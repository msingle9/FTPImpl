/**
 * 
 */
package FTPServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Christine McGee
 *
 */
public class myftpserver {

	private static ServerSocket serverSocket = null;
	public static ServerSocket serverDataSocket = null;
		
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		int portNumber = 0;
		
		
		
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
		
		
		
		try {
			
			serverSocket = new ServerSocket(portNumber);

			ExecutorService threadPoolServer = Executors.newFixedThreadPool(20);

			while (true) {
            	
            	threadPoolServer.execute(new FTPServerWorker(serverSocket.accept()));            	
            }
        }
		catch (IOException e) {
			System.err.println("IOException: " + e.getMessage() + "\n System Terminating."); //FIX
	        System.exit(-1);
		}
	}
}
