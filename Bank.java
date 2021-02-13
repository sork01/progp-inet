// *neode.command* setgo rm -f ./*.class; javac -Xlint -cp .:./yamlbeans-1.09/yamlbeans-1.09.jar Bank.java; java -cp .:./yamlbeans-1.09/yamlbeans-1.09.jar Bank; cat accounts.yml
import com.esotericsoftware.yamlbeans.*;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;

/**
 * Bank represents a (currency-centric) bank capable of handling
 * transactions in several accounts pseudo-concurrently.
 * 
 * @author Mikael Forsberg <miforsb@kth.se>
 * @author Robin Gunning <rgunning@kth.se>
 * @version 20151204T1700
 */
public class Bank
{
    /**
     * Bank.LoginToken represents a meaningless (from a user's
     * perspective) numeric token used to keep track of authenticated
     * users.
     */
    public static class LoginToken
    {
        /**
         * Create a new LoginToken.
         * 
         * @param value Numeric value of the token
         * @param ttl_seconds Lifetime (time of validity, in seconds)
         *        of the token
         */
        public LoginToken(long value, long ttl_seconds)
        {
            this.value = value;
            this.expires = (System.currentTimeMillis() / 1000L) + ttl_seconds;
        }
        
        /**
         * Is the token equal to some other token?
         * 
         * @param other Token to compare
         * @return True if the two tokens are equal, false otherwise
         */
        public boolean equals(LoginToken other)
        {
            return value == other.value;
        }
        
        /**
         * Has the token expired?
         * 
         * @return True if the token has expired, false otherwise
         */
        public boolean isExpired()
        {
            return (System.currentTimeMillis() / 1000L) >= this.expires;
        }
        
        /**
         * Numeric token value.
         */
        private long value = 0;
        
        /**
         * UNIX timestamp of token expiration.
         */
        private long expires;
    }
    
    /**
     * Create a new bank without loading any accounts.
     */
    public Bank()
    {
        
    }
    
    /**
     * Create a new bank, loading accounts from a given file.
     * 
     * @param accountsFile YAML database of accounts
     */
    public Bank(String accountsFile)
    {
        this.accountsFile = accountsFile;
        loadAccounts();
    }
    
    /**
     * Load accounts.
     */
    public boolean loadAccounts()
    {
        return loadAccounts(accountsFile);
    }
    
    /**
     * Load accounts from a given filename.
     * 
     * @param filename Name of file to load accounts from
     */
    public boolean loadAccounts(String filename)
    {
        accountsLock.lock(); // blocks until obtained
        
        try
        {
            YamlReader reader = new YamlReader(new FileReader(filename));
            accounts = reader.read(AccountList.class);
            reader.close();
        }
        catch (Exception e)
        {
            System.out.println(e);
            return false;
        }
        finally
        {
            accountsLock.unlock();
        }
        
        return true;
    }
    
    /**
     * File a new account with the bank.
     * 
     * @param acc New account to file
     * @return True if the account was added successfully,
     *         false otherwise
     */
    public boolean createAccount(Account acc)
    {
        accountsLock.lock(); // blocks until obtained
        
        accounts.add(acc);
        saveAccounts();
        
        accountsLock.unlock();
        
        return true;
    }
    
    /**
     * Save accounts to disk.
     * 
     * @return True if the accounts were written to disk
     *         successfully, false otherwise
     */
    public boolean saveAccounts()
    {
        return saveAccounts(accountsFile);
    }
    
    /**
     * Save accounts to a given filename.
     * 
     * @param filename Name of file to write accounts to
     * @return True if the accounts were written to disk
     *         successfully, false otherwise
     */
    public boolean saveAccounts(String filename)
    {
        accountsLock.lock(); // blocks until obtained
        
        try
        {
            YamlWriter writer = new YamlWriter(new FileWriter(filename));
            writer.write(accounts);
            writer.close();
        }
        catch (Exception e)
        {
            System.out.println(e);
            return false;
        }
        finally
        {
            accountsLock.unlock();
        }
        
        return true;
    }
    
    /**
     * Authenticate with the bank.
     * 
     * @param cardNr Card number of account
     * @param pinCode PIN code of account
     * @return If the given credentials were correct,
     *         returns a session token valid for one hour.
     *         Otherwise, returns null.
     */
    public LoginToken getLoginToken(int cardNr, int pinCode)
    {
        String s_cardNr = String.format("%04d", cardNr);
        String s_pinCode = String.format("%04d", pinCode);
        
        // System.out.println("login attempt: " + s_cardNr + ", " + s_pinCode);
        
        purgeExpiredLoginTokens();
        accountsLock.lock();
        
        Account targetAcc = null;
        
        for (Account acc : accounts)
        {
            if (acc.getCardNr().equals(s_cardNr) && acc.getPinCode().equals(s_pinCode))
            {
                targetAcc = acc;
                break;
            }
            // else
            // {
                // System.out.println("mismatch for " + acc.getName() + ": " + acc.getCardNr() + ", " + acc.getPinCode());
            // }
        }
        
        if (targetAcc == null)
        {
            accountsLock.unlock();
            return null;
        }
        
        Random rand = new Random();
        
        LoginToken token = new LoginToken(rand.nextLong(), 3600);
        
        if (users.containsKey(token))
        {
            return null;
        }
        
        users.put(token, targetAcc);
        
        accountsLock.unlock();
        return token;
    }
    
    /**
     * Purge expired session tokens.
     */
    public void purgeExpiredLoginTokens()
    {
        for (LoginToken tok : users.keySet())
        {
            if (tok.isExpired())
            {
                users.remove(tok);
            }
        }
    }
    
    /**
     * Retrieve the current balance of the account represented
     * by a given session token.
     * 
     * @param token Session token
     * @return Current balance of account, or null if the session
     *         token was found to be invalid for some reason.
     */
    public Integer balance(LoginToken token)
    {
        purgeExpiredLoginTokens();
        
        Account targetAcc = users.get(token);
        
        if (targetAcc == null)
        {
            return null;
        }
        
        accountsLock.lock();
        int result = targetAcc.getBalance();
        accountsLock.unlock();
        
        return result;
    }
    
    /**
     * Deposit currency into the account represented by a
     * given session token.
     * 
     * @param token Session token
     * @param amount Amount of currency to deposit
     * @return True if the currency was deposited, false
     *         otherwise
     */
    public boolean deposit(LoginToken token, int amount)
    {
        purgeExpiredLoginTokens();
        
        if (amount <= 0)
        {
            return false;
        }
        
        Account targetAcc = users.get(token);
        
        if (targetAcc == null)
        {
            return false;
        }
        
        accountsLock.lock();
        targetAcc.setBalance(targetAcc.getBalance() + amount);
        boolean result = saveAccounts();
        accountsLock.unlock();
        
        return result;
    }
    
    /**
     * Withdraw currency from the account represented by a
     * given session token.
     * 
     * @param token Session token
     * @param otp Next one-time password for the account
     * @param amount Amount of currency to withdraw
     * @return True if the currency was withdrawn, false
     *         otherwise
     */
    public boolean withdraw(LoginToken token, int otp, int amount)
    {
        String s_otp = String.format("%02d", otp);
        
        // System.out.println("bank withdraw");
        
        purgeExpiredLoginTokens();
        
        if (amount <= 0)
        {
            // System.out.println("bad amount");
            return false;
        }
        
        Account targetAcc = users.get(token);
        
        if (targetAcc == null)
        {
            // System.out.println("bad token");
            return false;
        }
        
        accountsLock.lock();
        
        if (!targetAcc.getNextOtp().equals(s_otp))
        {
            // System.out.println("otp mismatch: " + targetAcc.getNextOtp() + ", " + s_otp);
            accountsLock.unlock();
            return false;
        }
        else
        {
            // System.out.println("otp success");
            targetAcc.setNextOtp(String.format("%02d", otp + 2));
        }
        
        targetAcc.setBalance(targetAcc.getBalance() - amount);
        boolean result = saveAccounts();
        accountsLock.unlock();
        
        return result;
    }
    
    /**
     * List of accounts.
     */
    private ArrayList<Account> accounts = new ArrayList<Account>();
    
    /**
     * Map of authenticated user sessions.
     */
    private HashMap<LoginToken, Account> users = new HashMap<LoginToken, Account>();
    
    /**
     * Filename of accounts database.
     */
    private String accountsFile = null;
    
    /**
     * Lock for protecting the accounts from concurrent modification.
     */
    private Lock accountsLock = new ReentrantLock();
}
