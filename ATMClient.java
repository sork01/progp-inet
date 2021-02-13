//*neode.command* setgo javac -cp .:./yamlbeans-1.09/yamlbeans-1.09.jar ATMClient.java; java -cp .:./yamlbeans-1.09/yamlbeans-1.09.jar ATMClient
import com.esotericsoftware.yamlbeans.*;
import java.io.*;   
import java.net.*;  
import java.util.Scanner;
import java.util.Stack;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

/**
 * ATMClient is a client for an ATM server conforming to
 * the UBERPUTER ATM SPECIFICATION DRAFT REV.1
 * 
 * @author Mikael Forsberg <miforsb@kth.se>
 * @author Robin Gunning <rgunning@kth.se>
 * @version 20151204T1700
 */
public class ATMClient
{
    /**
     * ATMClient.Language represents a single user interface
     * language specified by a set of string->string key-value pairs.
     * 
     * @author Mikael Forsberg <miforsb@kth.se>
     * @author Robin Gunning <rgunning@kth.se>
     * @version 20151204T1700
     */
    public static class Language extends HashMap<String, String>
    {
        /**
         * Get an entry using an integer key.
         * 
         * @param key Key
         * @return Requested string, or null if there is no such key
         */
        public String get(int key)
        {
            return get(Integer.toString(key));
        }
        
        /**
         * Get an entry using an integer key.
         * 
         * @param key Key
         * @return Requested string, or null if there is no such key
         */
        public String get(Integer key)
        {
            return get(Integer.toString(key));
        }
    }
    
    /**
     * ATMClient.LanguageCollections represents a set of selectable
     * user interface languages.
     * 
     * @author Mikael Forsberg <miforsb@kth.se>
     * @author Robin Gunning <rgunning@kth.se>
     * @version 20151204T1700
     */
    public static class LanguageCollection extends HashMap<String, Language>
    {
        /**
         * Create a new LanguageCollection by loading data in the
         * YAML format.
         * 
         * @param ymlfile Name of file containing YAML data to load
         * @return LanguageCollection object, or null if the filename could not be read
         *         or parsed
         */
        public static LanguageCollection fromYaml(String ymlfile)
        {
            LanguageCollection langs = null;
            
            try
            {
                YamlReader reader = new YamlReader(new FileReader(ymlfile));
                langs = reader.read(LanguageCollection.class);
                reader.close();
            }
            catch (Exception e)
            {
                System.out.println(e);
                return null;
            }
            
            return langs;
        }
        
        /**
         * Write the LanguageCollection to disk in the YAML format.
         * 
         * @param ymlfile Name of file to write
         * @return True if the file was written successfully, false otherwise
         */
        public boolean toYaml(String ymlfile)
        {
            try
            {
                YamlWriter writer = new YamlWriter(new FileWriter(ymlfile));
                writer.write(this);
                writer.close();
            }
            catch (Exception e)
            {
                System.out.println(e);
                return false;
            }
            
            return true;
        }
    }
    
    /**
     * ATMClient.MenuItem represents one menu item as received from
     * the server.
     * 
     * @author Mikael Forsberg <miforsb@kth.se>
     * @author Robin Gunning <rgunning@kth.se>
     * @version 20151204T1700
     */
    public static class MenuItem
    {
        /**
         * Create a new MenuItem.
         *
         * @param lang Language to use
         * @param menutext_id Key of string to display in menu
         * @param screentext_id Key of string to display before executing
         *        the action
         * @param action_id Action ID to send when executing
         * @param type Action type bitfield
         */
        public MenuItem(Language lang, int menutext_id, int screentext_id, int action_id,
                            int type)
        {
            menutext = lang.get(menutext_id);
            screentext = lang.get(screentext_id);
            action = action_id;
            this.type = type;
        }
        
        /**
         * Text to display in menu.
         */
        public final String menutext;
        
        /**
         * Text to display before executing.
         */
        public final String screentext;
        
        /**
         * Action ID to send when executing.
         */
        public final int action;
        
        /**
         * Action type bitfield.
         */
        public final int type;
    }
    
    /**
     * Main entry point for running the client program.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) throws IOException
    {
        ATMClient clt = null;
        
        String host = "127.0.0.1";
        int port = 8086;
        
        try
        {
            host = args[0];
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            System.err.println("Usage: java ATMClient <host> <port>");
            System.exit(1);
        }
        
        try 
        {
            port = Integer.parseInt(args[1]);
            
            if (port < 1 || port > 65535)
            {
                System.err.println("Usage: java ATMClient <host> <port>");
                System.exit(1);
            }
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            System.err.println("Usage: java ATMClient <host> <port>");
            System.exit(1);
        }
        
        try
        {
            clt = new ATMClient(host, port);
        }
        catch (UnknownHostException e)
        {
            System.err.println("Unknown host: " + host + ":" + port);
            System.exit(1);
        }
        catch (IOException e)
        {
            System.err.println("Couldn't open connection to " + host + ":" + port);
            System.exit(1);
        }
        
        try
        {
            while (clt.isConnected())
            {
                clt.runOnce();
            }
        }
        catch (Exception e)
        {
            System.out.println(e);
            e.printStackTrace();
        }
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
     * Create a new ATMClient.
     * 
     * @param host Hostname or IP-address for network connection
     * @param port Port for network connection
     */
    public ATMClient(String host, int port) throws UnknownHostException, IOException
    {
        socket = new Socket(host, port);
        
        tx = new DataOutputStream(socket.getOutputStream());
        rx = new DataInputStream(socket.getInputStream());
        
        connected = true;
        
        languages = LanguageCollection.fromYaml("languages-client.yml");
        current_language = languages.get("English");
        
        state.push(ST_REQUEST_MENU);
    }
    
    /**
     * Run the ATMClient until it is disconnected for some reason.
     */
    public void run() throws IOException
    {
        while (isConnected())
        {
            runOnce();
        }
    }
    
    /**
     * Retrieve one of the "useful" strings from the current language.
     * 
     * @param which Pseudo-English name of useful string
     * @return String in current language, or the given parameter if the
     *         current language did not contain the corresponding key
     */
    private String getUsefulString(String which)
    {
        if (!languages.containsKey("_Useful") || !languages.get("_Useful").containsKey(which))
        {
            // if (!languages.containsKey("_Useful"))
            // {
                // debug("getUsefulString: dont even have the list!");
            // }
            
            // if (!languages.get("_Useful").containsKey(which))
            // {
                // debug("getUsefulString: requested string \"" + which + "\" does not exist");
            // }
            
            return which;
        }
        
        String key = languages.get("_Useful").get(which);
        
        if (!current_language.containsKey(key))
        {
            // debug("getUsefulString: current language doesnt have the key");
            
            return which;
        }
        
        return current_language.get(key);
    }
    
    /**
     * Have the client do a limited amount of work.
     */
    public void runOnce() throws IOException
    {
        Scanner scanner = new Scanner(System.in);
        
        switch (state.peek())
        {
            case ST_REQUEST_MENU:
                tryUpdate();
                menuitems.clear();
                // debug("requesting menu");
                state.push(ST_RECV_MENU_NUM_ITEMS);
                sendMenuRequest();
                break;
            
            case ST_RECV_MENU_NUM_ITEMS:
                // debug("trying to receive menu size");
                state.pop();
                
                if (receive(ATM.MSG_MENU_NUM_ITEMS, true))
                {
                    menuitems_remain = rx.readUnsignedByte();
                    
                    // debug("got menu size: " + Integer.toString(menuitems_remain));
                    
                    state.push(ST_RECV_MENU_ITEMS);
                }
                break;
            
            case ST_RECV_MENU_ITEMS:
                if (receiveMenuItem())
                {
                    --menuitems_remain;
                    
                    // debug("got a menu item \"" + menuitems.getLast().menutext + "\", remaining: "
                        //+ Integer.toString(menuitems_remain));
                    
                    if (menuitems_remain < 1)
                    {
                        state.pop();
                        state.push(ST_SHOW_MENU);
                    }
                }
                break;
            
            case ST_SHOW_MENU:
                // debug("showing the menu");
                
                int choice = 0;
                
                System.out.println("-------------");
                
                if (printBanner())
                {
                    System.out.println("-------------");
                }
                
                while (choice < 1 || choice > menuitems.size() + 1)
                {
                    int i = 1;
                    
                    for (MenuItem item : menuitems)
                    {
                        System.out.println(Integer.toString(i) + ": " + item.menutext);
                        ++i;
                    }
                    
                    System.out.println(Integer.toString(i) + ": "
                        + getUsefulString("change_language"));
                    
                    choice = scanner.nextInt();
                }
                
                if (choice == menuitems.size() + 1)
                {
                    state.pop();
                    state.push(ST_CHANGE_LANGUAGE);
                }
                else
                {
                    menuitem_selected = menuitems.get(choice - 1);
                    
                    state.pop();
                    state.push(ST_SHOW_ACTION);
                }
                
                break;
            
            case ST_SHOW_ACTION:
                state.pop();
                ++transaction_counter;
                
                int arg = -1;
                
                // do we need to supply an uint32 parameter?
                if ((menuitem_selected.type & ATM.TYPE_SND_UINT32) != 0)
                {
                    while (arg < 0)
                    {
                        System.out.println(menuitem_selected.screentext); // todo: fixa formatter?
                        arg = scanner.nextInt();
                    }
                }
                else
                {
                    System.out.println(menuitem_selected.screentext); // todo: fixa formatter?
                }
                
                // execute the action
                tx.writeByte(ATM.MSG_ACTION);
                tx.writeByte(menuitem_selected.action);
                tx.writeInt(arg);
                tx.writeShort(transaction_counter);
                
                // do we need to receive a particular response?
                if ((menuitem_selected.type & ATM.TYPE_RECV_UINT32) != 0)
                {
                    if (receive(ATM.MSG_RESPONSE, true))
                    {
                        rx.skipBytes(2);
                        System.out.println(rx.readInt());
                    }
                }
                else // just receive OK or FAIL
                {
                    int recv = receive(new int[]{ATM.MSG_OK, ATM.MSG_FAIL}, true);
                    
                    switch (recv)
                    {
                        case ATM.MSG_OK:
                            // debug("got ok");
                            rx.skipBytes(2);
                            break;
                        
                        case ATM.MSG_FAIL:
                            // debug("got fail");
                            rx.skipBytes(2);
                            int errstr_id = rx.readUnsignedByte();
                            // debug("error id: " + Integer.toString(errstr_id));
                            
                            String errstr = current_language.get(Integer.toString(errstr_id));
                            
                            // debug("emptying stdin");
                            while (System.in.available() > 0)
                            {
                                // debug("looping");
                                System.in.skip(System.in.available());
                            }
                            // debug("done emptying");
                            
                            // debug("print error string");
                            System.out.println(errstr);
                            
                            // debug("wait for carriage return");
                            // System.in.read();
                            
                            break;
                        
                        default:
                            break;
                    }
                }
                
                // debug("before followups");
                // for (MenuItem m : menuitems)
                // {
                    // debug(Integer.toString(m.action));
                // }
                
                // do we need to receive a follow-up action?
                if ((menuitem_selected.type & ATM.TYPE_RECV_FOLLOWUP) != 0)
                {
                    // debug("reading followup");
                    
                    if (receiveMenuItem())
                    {
                        // for (MenuItem m : menuitems)
                        // {
                            // debug(Integer.toString(m.action));
                        // }
                        
                        state.push(ST_SHOW_ACTION);
                        menuitem_selected = menuitems.removeLast();
                        // debug("followup menuitem action "
                            //+ Integer.toString(menuitem_selected.action));
                    }
                }
                
                break;
            
            case ST_CHANGE_LANGUAGE:
                int i = 0;
                
                System.out.println("-------------");
                
                for (String langname : languages.keySet())
                {
                    if (langname.startsWith("_"))
                    {
                        continue;
                    }
                    
                    System.out.print(++i);
                    System.out.println(": " + langname);
                }
                
                choice = 0;
                
                while (choice < 1 || choice > i)
                {
                    choice = scanner.nextInt();
                }
                
                i = 0;
                
                for (String langname : languages.keySet())
                {
                    if (langname.startsWith("_"))
                    {
                        continue;
                    }
                    
                    if (++i == choice)
                    {
                        current_language = languages.get(langname);
                    }
                }
                
                state.pop();
                break;
            
            default:
                debug("unhandled state, quit");
                System.exit(1);
                break;
        }
    }
    
    /**
     * Is the client connected to a server?
     * 
     * @return True if the client is connected to a server, false otherwise
     */
    public boolean isConnected()
    {
        return connected;
    }
    
    /**
     * Stop the client and tear down the socket connection.
     */
    public void stop() throws IOException
    {
        rx.close();
        tx.close();
        socket.close();
    }
    
    /**
     * Try to receive a certain type of message.
     * 
     * @param msg Type of message required
     * @param drain_on_failure Whether or not to drain the socket input on failure
     * @return True if the required message was received, false otherwise
     */
    private boolean receive(int msg, boolean drain_on_failure) throws IOException
    {
        int recv = rx.readUnsignedByte();
        
        if (recv == msg)
        {
            return true;
        }
        else
        {
            if (drain_on_failure)
            {
                drain();
            }
            
            return false;
        }
    }
    
    /**
     * Try to receive one of a set of certain types of message.
     * 
     * @param msg Types of messages allowed
     * @param drain_on_failure Whether or not to drain the socket input on failure
     * @return The positive 8-bit ID of the received message, or -1 if no matching
     *         message was received
     */
    private int receive(int[] msg, boolean drain_on_failure) throws IOException
    {
        int recv = rx.readUnsignedByte();
        
        for (int m : msg)
        {
            if (recv == m)
            {
                return recv;
            }
        }
        
        if (drain_on_failure)
        {
            drain();
        }
        
        return -1;
    }
    
    /**
     * Drain the socket input.
     */
    private void drain() throws IOException
    {
        rx.skipBytes(rx.available());
    }
    
    /**
     * Request the menu from the server.
     */
    private void sendMenuRequest() throws IOException
    {
        tx.writeByte(ATM.MSG_MENU_REQUEST);
    }
    
    /**
     * Try to receive a single menu item.
     * 
     * @return True if a menu item was received, false otherwise
     */
    private boolean receiveMenuItem() throws IOException
    {
        try
        {
            if (!receive(ATM.MSG_MENU_ITEM, true))
            {
                return false;
            }
            
            menuitems.add(new MenuItem(current_language,
                rx.readUnsignedByte(),
                rx.readUnsignedByte(),
                rx.readUnsignedByte(),
                rx.readUnsignedByte()));
            
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }
    
    /**
     * Print the bank's news banner.
     * 
     * @return True if the current language contained the banner (and
     *         it has been printed to the terminal), false otherwise.
     */
    private boolean printBanner()
    {
        if (!current_language.containsKey("1"))
        {
            return false;
        }
        
        System.out.println(current_language.get("1"));
        return true;
    }
    
    /**
     * Try to update the language file.
     */
    private void tryUpdate() throws IOException
    {
        // send an update request
        tx.write(ATM.MSG_UPDATE_REQUEST);
        
        if (!languages.containsKey("_Version") || !languages.get("_Version").containsKey("0"))
        {
            tx.writeInt(0);
        }
        else
        {
            tx.writeInt(Integer.parseInt(languages.get("_Version").get("0")));
        }
        
        // get a response
        int recv = receive(new int[]{ATM.MSG_OK, ATM.MSG_UPDATE}, true);
        
        // do we get an update?
        if (recv == ATM.MSG_UPDATE)
        {
            // numbytes is sent as a 24-bit unsigned integer
            int numbytes = rx.readUnsignedByte() << 16 | rx.readUnsignedByte() << 8
                            | rx.readUnsignedByte();
            
            int numread = 0;
            
            byte[] bytes = new byte[numbytes];
            
            // debug("update, numbytes: " + Integer.toString(numbytes));
            
            // receive the updated data
            while (numbytes > 0)
            {
                numread += rx.read(bytes, numread, numbytes);
                numbytes -= numread;
            }
            
            // parse the data
            String yaml_str = new String(bytes, java.nio.charset.Charset.forName("UTF-8"));
            
            try
            {
                YamlReader reader = new YamlReader(new StringReader(yaml_str));
                Map map = (Map) reader.read();
                
                languages.clear();
                
                // handle the special "_Version" field
                Language ver = new Language();
                ver.put("0", (String) map.get("_Version"));
                languages.put("_Version", ver);
                
                // handle the other languages, including "_Useful"
                for (Object o : map.keySet())
                {
                    String langname = (String) o;
                    
                    if (langname.equals("_Version"))
                    {
                        continue;
                    }
                    
                    // debug(langname);
                    
                    Map langmap = (Map) map.get(langname);
                    Language lang = new Language();
                    
                    for (Object oo : langmap.keySet())
                    {
                        lang.put((String) oo, (String) langmap.get(oo));
                    }
                    
                    languages.put(langname, lang);
                }
                
                // save the data
                languages.toYaml("languages-client.yml");
                current_language = languages.get("English");
            }
            catch (Exception e)
            {
                System.out.println(e);
                e.printStackTrace();
                debug("broken yaml");
                System.exit(1);
            }
        }
        else if (recv == ATM.MSG_OK)
        {
            // debug("no update available");
            rx.skipBytes(2);
        }
    }
    
    /**
     * Connection status of the client.
     */
    private boolean connected = false;
    
    /**
     * Stack of states for the client state machine.
     */
    private Stack<Integer> state = new Stack<Integer>();
    
    /**
     * Menu items remaining to be received.
     */
    private int menuitems_remain = 0;
    
    /**
     * Menu items received.
     */
    private LinkedList<MenuItem> menuitems = new LinkedList<MenuItem>();
    
    /**
     * Menu item selected.
     */
    private MenuItem menuitem_selected = null;
    
    /**
     * Main network connection socket.
     */
    private Socket socket;
    
    /**
     * Network socket input data stream.
     */
    private DataInputStream rx;
    
    /**
     * Network socket output data stream.
     */
    private DataOutputStream tx;
    
    /**
     * Set of selectable user interface languages.
     */
    private LanguageCollection languages;
    
    /**
     * Currently selected user interface language.
     */
    private Language current_language;
    
    /**
     * Network transaction id counter.
     */
    private int transaction_counter = 0;
    
    /**
     * Enumeration of states for the client state machine.
     * This state: request the menu.
     */
    private static final int ST_REQUEST_MENU = 0;
    
    /**
     * Enumeration of states for the client state machine.
     * This state: receive the number of expected menu items.
     */
    private static final int ST_RECV_MENU_NUM_ITEMS = 1;
    
    /**
     * Enumeration of states for the client state machine.
     * This state: receive actual menu items.
     */
    private static final int ST_RECV_MENU_ITEMS = 2;
    
    /**
     * Enumeration of states for the client state machine.
     * This state: show the menu.
     */
    private static final int ST_SHOW_MENU = 3;
    
    /**
     * Enumeration of states for the client state machine.
     * This state: show / perform an action.
     */
    private static final int ST_SHOW_ACTION = 4;
    
    /**
     * Enumeration of states for the client state machine.
     * This state: change user interface language.
     */
    private static final int ST_CHANGE_LANGUAGE = 5;
}
