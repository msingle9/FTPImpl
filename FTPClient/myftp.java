package FTPClient;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


/**
 * @author Christine McGee
 *
 */
public class myftp {

	private static Socket socket = null;
	private static String hostName = null;
	private static int portNumber = 0;
	private static Future clientProgramFuture = null;

	/**
	 * @param args args[0] as host name and args[1] as port number
	 */
	public static void main(String[] args) throws Exception {
        
		ExecutorService threadPoolClient = Executors.newFixedThreadPool(1);
		

		if (args.length != 2) {
            	System.err.println("Pass the  machine name where the server resides and the port number");
            	return;            
        	}
        		
		String hostName = args[0];
		
		try {	        
			portNumber = Integer.parseInt(args[1]);
	    	} 		
		catch (NumberFormatException e) {
	        
			System.err.println("Command line argument" + args[0] + " must be a port integer."); //FIX
	        System.exit(-1);
	    }
		
		
		        
        try {

        	socket = new Socket(hostName, portNumber);
        	//socket = new Socket("localhost", 8015);

        	Runnable clientProgram = new FTPClientWorker(socket);
       		
        	clientProgramFuture = threadPoolClient.submit(clientProgram);
       		
       		if ((clientProgramFuture.get() == null) || (clientProgramFuture.isDone()) || (clientProgramFuture.isCancelled())) {
                
       			threadPoolClient.shutdown();
       			
       			try {
       		     // Wait a while for existing tasks to terminate
       		     if (!threadPoolClient.awaitTermination(60, TimeUnit.SECONDS)) {
       		    	threadPoolClient.shutdownNow(); // Cancel currently executing tasks
       		       // Wait a while for tasks to respond to being cancelled
       		       if (!threadPoolClient.awaitTermination(60, TimeUnit.SECONDS))
       		           System.err.println("Pool did not terminate");
       		     }
       		   } catch (InterruptedException ie) {
       		     // (Re-)Cancel if current thread also interrupted
       			threadPoolClient.shutdownNow();
       		     // Preserve interrupt status
       		     Thread.currentThread().interrupt();
       		   }

            }

        }
        catch (IOException e) {
            System.err.println("Input Output Exception: " + e.getMessage());
        }
        
        if(threadPoolClient.isShutdown() || threadPoolClient.isTerminated()) {
        	System.out.println("Connection closed");
        }
	}
	

		

}
