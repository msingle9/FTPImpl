package FTPServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple server program takes a single command line parameter, 
 * which is the port number where the server will execute.
 * @author Christine McGee, Andrew Heywood, Matthew Singletary
 *
 */
public class myftpserver {

	// Variable Declaration
	private static ServerSocket serverSocket = null;	
	
	/**
	 * Begins execution of FTP Server program by verifying correct command
	 * line arguments and creating a server socket to accept incoming
	 * connections from clients.
	 * @param args args[0] as port number where server will execute
	 */
	public static void main(String[] args) {
		
		int portNumber = 0;		
		
		// Verify port number entered on command line
		// Print out error if not and exit. 
		if (args.length != 1) {
            System.err.println("Pass the port number where the server will execute");
            System.err.println("Usage: myftpserver PORTNUMBER");
            return;            
        }
		
		// If port number entered parse String to int from args[0]
		if (args.length == 1) {
    	    
    		try {
    	        
    			portNumber = Integer.parseInt(args[0]);
    	    } 
    		catch (NumberFormatException e) {
    	        
    			System.err.println("Command line argument" + args[0] + " must be a port integer."); //FIX
    	        System.exit(-1);
    	    }
    	}
		else {
			System.err.println("Command line argument" + args[0] + " must be a port integer.");  //FIX
	        System.exit(-1);
		}
		
		
		// Try creating server socket for server to accept incoming connections
		// using port number from command line. Create thread pool to enable multiple 
		// clients to connect. Once created start accepting new client connections.
		// Catch errors, print cause, and exit.
		try {
			
			serverSocket = new ServerSocket(portNumber);

			ExecutorService threadPoolServer = Executors.newFixedThreadPool(20);

			while (true) {
            	
            	threadPoolServer.execute(new FTPServerWorker(serverSocket.accept()));            	
            }
        }
		catch (IOException e) {
			System.err.println("IOException while trying to create server socket and thread pool: " 
								+ e.getMessage() + "\n System Terminating."); //FIX
	        System.exit(-1);
		}
	}
}
