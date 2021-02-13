/**
 * Account represents a single user bank account.
 * 
 * @author Mikael Forsberg <miforsb@kth.se>
 * @author Robin Gunning <rgunning@kth.se>
 * @version 20151204T1700
 */
public class Account implements java.io.Serializable
{
    /**
     * A version UID is required for java.io.Serializable.
     * This should be changed when any changes have been
     * made to the public API of this class.
     */
    static final long serialVersionUID = 8169274829459213L;
    
    /**
     * Create a new empty account.
     */
    public Account()
    {
        name = "Unnamed";
        balance = -1;
        cardNr = "-1";
        pinCode = "-1";
        nextOtp = "-1";
    }
    
    /**
     * Create a new account.
     * 
     * @param name Name of account holder
     * @param balance Initial balance of the account
     * @param cardNr Card number for the account
     * @param pinCode PIN code for the card number
     * @param nextOtp Initial OTP password for withdrawals
     */
    public Account(String name, int balance, String cardNr, String pinCode, String nextOtp)
    {
        this.name = name;
        this.balance = balance;
        this.cardNr = cardNr;
        this.pinCode = pinCode;
        this.nextOtp = nextOtp;
    }
    
    /**
     * Returns a summarizing string representation of the account.
     * 
     * @return String representation of the account
     */
    public String toString()
    {
        return String.format("[\"%s\", Card: %s, PIN: %s, Balance: %d]",
            name,
            String.format("%04d", cardNr),
            String.format("%04d", pinCode),
            balance);
    }
    
    /**
     * Returns the name of the account holder.
     * 
     * @return Name of account holder
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * Sets the name of the account holder.
     * 
     * @param name Name to set
     */
    public void setName(String name)
    {
        this.name = name;
    }
    
    /**
     * Gets the current balance of the account.
     * 
     * @return Current balance of account
     */
    public int getBalance()
    {
        return balance;
    }
    
    /**
     * Sets the current balance of the account.
     * 
     * @param balance Balance to set
     */
    public void setBalance(int balance)
    {
        this.balance = balance;
    }
    
    /**
     * Gets the card number of the account.
     * 
     * @return Card number of the account
     */
    public String getCardNr()
    {
        return cardNr;
    }
    
    /**
     * Sets the card number of the account.
     * 
     * @param cardNr Card number to set
     */
    public void setCardNr(String cardNr)
    {
        this.cardNr = cardNr;
    }
    
    /**
     * Gets the PIN code of the account.
     * 
     * @return PIN code of the account
     */
    public String getPinCode()
    {
        return pinCode;
    }
    
    /**
     * Sets the PIN code of the account.
     * 
     * @param pinCode PIN code to set
     */
    public void setPinCode(String pinCode)
    {
        this.pinCode = pinCode;
    }
    
    /**
     * Gets the next OTP password for the account.
     * 
     * @return Next OTP password for the account
     */
    public String getNextOtp()
    {
        return nextOtp;
    }
    
    /**
     * Sets the next OTP password for the account.
     * 
     * @param nextOtp OTP password to set
     */
    public void setNextOtp(String nextOtp)
    {
        this.nextOtp = nextOtp;
    }
    
    /**
     * Name of the account holder.
     */
    private String name;
    
    /**
     * Current balance of the account.
     */
    private int balance;
    
    /**
     * Card number of the account.
     */
    private String cardNr;
    
    /**
     * PIN code of the account.
     */
    private String pinCode;
    
    /**
     * Next OTP password for the account.
     */
    private String nextOtp;
}
