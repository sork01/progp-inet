//*neode.command* setgo javac -cp .:./yamlbeans-1.09/yamlbeans-1.09.jar ATMServerThread.java; java -cp .:./yamlbeans-1.09/yamlbeans-1.09.jar ATMServerThread
import com.esotericsoftware.yamlbeans.*;
import java.io.*;
import java.net.*;
import java.util.Map;

/**
 * ATMServerThread handles a single user ATM session according
 * to the UBERPUTER ATM SPECIFICATION DRAFT REV.1.
 * 
 * @author Viebrapadata
 * @author Mikael Forsberg <miforsb@kth.se>
 * @author Robin Gunning <rgunning@kth.se>
 * @version 20151204T1700
 */
public class ATMServerThread extends Thread
{
    /**
     * Create a new ATMServerThread.
     * 
     * @param bank Bank to represent
     * @param socket Client connection socket
     */
    public ATMServerThread(Bank bank, Socket socket) throws IOException
    {
        super("ATMServerThread");
        
        this.bank = bank;
        this.socket = socket;
        tx = new DataOutputStream(socket.getOutputStream());
        rx = new DataInputStream(socket.getInputStream());
    }
    
    /**
     * Print a debug message to the terminal.
     * 
     * @param msg Message to print
     */
    public void debug(String msg)
    {
        System.out.println(msg);
    }
    
    /**
     * Implementation of Thread.run()
     */
    public void run()
    {
        try
        {
            mainLoop();
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
    }
    
    /**
     * Runs the main session loop.
     */
    public void mainLoop() throws IOException
    {
        Bank.LoginToken token = null;
        
        int transact_id = 0;
        int cardno = 0;
        int pin = 0;
        int state = 1000; // default waiting state
        int sum = 0;
        
        while (true)
        {
            switch(state)
            {
                case 1000:
                    state = rx.readUnsignedByte();
                    // System.out.println("här är state " + state);
                    break;
                
                case ATM.MSG_MENU_REQUEST:
                    if (token == null)
                    {
                        tx.writeByte(ATM.MSG_MENU_NUM_ITEMS);
                        tx.writeByte(1);
                        
                        tx.writeByte(ATM.MSG_MENU_ITEM);
                        tx.writeByte(ATM.S_LOGIN);
                        tx.writeByte(ATM.S_LOGINTEXT_CARD);
                        tx.writeByte(ATM.ACT_LOGIN_CARDNO);
                        tx.writeByte(ATM.TYPE_SND_UINT32 | ATM.TYPE_RECV_FOLLOWUP);
                    }
                    else
                    {
                        tx.writeByte(ATM.MSG_MENU_NUM_ITEMS);
                        tx.writeByte(4);
                        
                        tx.writeByte(ATM.MSG_MENU_ITEM);
                        tx.writeByte(ATM.S_BALANCE);
                        tx.writeByte(ATM.S_BALANCETEXT);
                        tx.writeByte(ATM.ACT_BALANCE);
                        tx.writeByte(ATM.TYPE_RECV_UINT32);
                        
                        tx.writeByte(ATM.MSG_MENU_ITEM);
                        tx.writeByte(ATM.S_DEPOSIT);
                        tx.writeByte(ATM.S_DEPOSITTEXT);
                        tx.writeByte(ATM.ACT_DEPOSIT);
                        tx.writeByte(ATM.TYPE_SND_UINT32 | ATM.TYPE_RECV_FOLLOWUP);
                        
                        tx.writeByte(ATM.MSG_MENU_ITEM);
                        tx.writeByte(ATM.S_WITHDRAW);
                        tx.writeByte(ATM.S_WITHDRAWTEXT);
                        tx.writeByte(ATM.ACT_WITHDRAW);
                        tx.writeByte(ATM.TYPE_SND_UINT32 | ATM.TYPE_RECV_FOLLOWUP);
                        
                        tx.writeByte(ATM.MSG_MENU_ITEM);
                        tx.writeByte(ATM.S_LOGOUT);
                        tx.writeByte(ATM.S_LOGOUTTEXT);
                        tx.writeByte(ATM.ACT_LOGOUT);
                        tx.writeByte(0);
                    }
                    
                    state = 1000;
                    break;
                
                case ATM.MSG_ACTION:
                    int action_id = rx.readUnsignedByte();
                    
                    switch(action_id)
                    {
                        case ATM.ACT_BALANCE:
                            rx.skipBytes(4);
                            transact_id = rx.readUnsignedShort();
                            
                            tx.writeByte(ATM.MSG_RESPONSE);
                            tx.writeShort(transact_id);
                            tx.writeInt(bank.balance(token));
                            
                            break;
                        
                        case ATM.ACT_DEPOSIT:
                            if (token == null)
                            {
                                throw new RuntimeException("Deposit from unauthenticated user");
                            }
                            
                            sum = rx.readInt();
                            transact_id = rx.readUnsignedShort();
                            
                            bank.deposit(token, sum);
                            
                            tx.writeByte(ATM.MSG_OK);
                            tx.writeShort(transact_id);
                            
                            tx.writeByte(ATM.MSG_MENU_ITEM);
                            tx.writeByte(ATM.S_BALANCE);
                            tx.writeByte(ATM.S_BALANCETEXT);
                            tx.writeByte(ATM.ACT_BALANCE);
                            tx.writeByte(ATM.TYPE_RECV_UINT32);
                            
                            sum = 0;
                            
                            break;
                        
                        case ATM.ACT_WITHDRAW:
                            if (token == null)
                            {
                                throw new RuntimeException("Withdraw from unauthenticated user");
                            }
                            
                            sum = rx.readInt();
                            transact_id = rx.readUnsignedShort();
                            
                            tx.writeByte(ATM.MSG_OK);
                            tx.writeShort(transact_id);
                            
                            tx.writeByte(ATM.MSG_MENU_ITEM);
                            tx.writeByte(ATM.S_NONE);
                            tx.writeByte(ATM.S_OTPTEXT);
                            tx.writeByte(ATM.ACT_OTP_WITHDRAW);
                            tx.writeByte(ATM.TYPE_SND_UINT32 | ATM.TYPE_RECV_FOLLOWUP);
                            
                            break;
                        
                        case ATM.ACT_OTP_WITHDRAW:
                            if (token == null)
                            {
                                throw new RuntimeException("OTP from unauthenticated user");
                            }
                            
                            int otp = rx.readInt();
                            transact_id = rx.readUnsignedShort();
                            
                            if (bank.balance(token) < sum)
                            {
                                // debug("insuff funds");
                                tx.writeByte(ATM.MSG_FAIL);
                                tx.writeShort(transact_id);
                                tx.writeByte(ATM.S_AMNTERROR);
                            }
                            else
                            {
                                if (bank.withdraw(token, otp, sum))
                                {
                                    // debug("withdraw ok");
                                    tx.writeByte(ATM.MSG_OK);
                                    tx.writeShort(transact_id);
                                }
                                else
                                {
                                    // debug("withdraw fail");
                                    tx.write(ATM.MSG_FAIL);
                                    tx.writeShort(transact_id);
                                    tx.write(ATM.S_ERRORTEXT);
                                }
                            }
                            
                            tx.writeByte(ATM.MSG_MENU_ITEM);
                            tx.writeByte(ATM.S_NONE);
                            tx.writeByte(ATM.S_BALANCETEXT);
                            tx.writeByte(ATM.ACT_BALANCE);
                            tx.writeByte(ATM.TYPE_RECV_UINT32);
                            
                            break;
                        
                        case ATM.ACT_LOGIN_CARDNO:
                            if (token != null)
                            {
                                throw new RuntimeException("Login CARD from already authenticated user");
                            }
                            
                            cardno = rx.readInt();
                            transact_id = rx.readUnsignedShort();
                            
                            tx.write(ATM.MSG_OK);
                            tx.writeShort(transact_id);
                            
                            tx.writeByte(ATM.MSG_MENU_ITEM);
                            tx.writeByte(ATM.S_LOGIN);
                            tx.writeByte(ATM.S_LOGINTEXT_PIN);
                            tx.writeByte(ATM.ACT_LOGIN_PIN);
                            tx.writeByte(ATM.TYPE_SND_UINT32);
                            
                            break;
                        
                        case ATM.ACT_LOGIN_PIN:
                            if (token != null)
                            {
                                throw new RuntimeException("Login PIN from already authenticated user");
                            }
                            
                            pin = rx.readInt();
                            transact_id = rx.readUnsignedShort();
                            
                            token = bank.getLoginToken(cardno, pin);
                            
                            if (token == null)
                            {
                                tx.writeByte(ATM.MSG_FAIL);
                                tx.writeShort(transact_id);
                                tx.writeByte(ATM.S_ERRORTEXT);
                            }
                            else
                            {
                                tx.writeByte(ATM.MSG_OK);
                                tx.writeShort(transact_id);
                            }
                            
                            break;
                        
                        case ATM.ACT_LOGOUT:
                            if (token == null)
                            {
                                throw new RuntimeException("Logout from unauthenticated user");
                            }
                            
                            rx.skipBytes(4);
                            transact_id = rx.readShort();
                            
                            token = null;
                            
                            tx.writeByte(ATM.MSG_OK);
                            tx.writeShort(transact_id);
                            break;
                        
                        default:
                            throw new RuntimeException("Unrecognized action");
                            // break;
                    }
                    
                    state = 1000;
                    break;
                
                case ATM.MSG_UPDATE_REQUEST:
                    // debug("got update request");
                    int version = rx.readInt();
                    int myversion = getLanguageDataVersion();
                    
                    // debug("version: " + Integer.toString(version) + ", my version: " + Integer.toString(myversion));
                    
                    if (version != myversion)
                    {
                        File file = new File("languages.yml");
                        FileInputStream in = new FileInputStream(file);
                        
                        byte[] data = new byte[(int)file.length()];
                        
                        in.read(data);
                        in.close();
                        
                        // debug("sending " + Integer.toString(data.length) + " bytes of language data");
                        
                        tx.write(ATM.MSG_UPDATE);
                        tx.write((data.length & 0xff0000) >> 16);
                        tx.write((data.length & 0x00ff00) >> 8);
                        tx.write(data.length & 0x0000ff);
                        tx.write(data);
                    }
                    else
                    {
                        // debug("no update necessary");
                        tx.write(ATM.MSG_OK);
                        tx.writeShort(0);
                    }
                    
                    state = 1000;
                    break;
                
                default:
                    state = 1000;
                    break;
            }
        }
    }
    
    /**
     * Gets the value of the "_Version" field in the language file.
     * 
     * @return Value of the "_Version" field in the language file
     */
    private int getLanguageDataVersion()
    {
        try
        {
            YamlReader reader = new YamlReader(new FileReader("languages.yml"));
            Map map = (Map) reader.read();
            
            String version = (String) map.get("_Version");
            
            if (version != null)
            {
                return Integer.parseInt(version);
            }
            else
            {
                throw new RuntimeException();
            }
        }
        catch (Exception e)
        {
            debug("could not get lang data version");
            return 0;
        }
    }
    
    /**
     * Client connection socket.
     */
    private Socket socket = null;
    
    /**
     * Client socket input data stream.
     */
    private DataInputStream rx;
    
    /**
     * Client socket output data stream.
     */
    private DataOutputStream tx;
    
    /**
     * Bank being represented.
     */
    private Bank bank;
}
