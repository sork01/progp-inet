import java.net.*;
import java.io.*;

/**
 * ATMServer creates a server socket and spawns server threads
 * for each new client connection.
 * 
 * @author Viebrapadata
 * @author Mikael Forsberg <miforsb@kth.se>
 * @author Robin Gunning <rgunning@kth.se>
 * @version 20151204T1700
 */
public class ATMServer
{
    /**
     * Main entry point for running the ATMServer program.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) throws IOException
    {
        // setting port using command line
        int connectionPort = 0;
        
        ServerSocket serverSocket = null;
       
        boolean listening = true;
        
        // connect to the bank
        Bank bank = new Bank("accounts.yml");
        
        try
        {
            connectionPort = Integer.parseInt(args[0]);
            
            if (connectionPort < 1 || connectionPort > 65535)
            {
                System.err.println("Invalid port");
                System.exit(1);
            }
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            System.err.println("Missing argument port");
            System.exit(1);
        }
        
        try
        {
            serverSocket = new ServerSocket(connectionPort);
        }
        catch (IOException e)
        {
            System.err.println("Could not listen on port: " + connectionPort);
            System.exit(1);
        }
        
        System.out.println("Bank started listening on port: " + connectionPort);
        
        while (listening)
        {
            new ATMServerThread(bank, serverSocket.accept()).start();
        }
        
        serverSocket.close();
    }
}
