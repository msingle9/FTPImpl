package FTPClient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Displays a prompt “mytftp>” to the user and then accepts and executes commands
 * by relaying the commands to the server and displaying the results and 
 * error messages. Runs until user enters the “quit” command.
 * @author Christine McGee, Andrew Heywood, Matthew Singletary
 *
 */
public class FTPClientWorker implements Runnable{
	
	// Variable Declarations
	private Socket clientsSocket = null;
	
	private Scanner userInputScanner = null;
	private DataInputStream inputFromServer = null;
	private BufferedReader inputFromServerBuffered = null;
    private PrintStream outputToServer = null;
	
    private FileOutputStream fileOutputStream = null;
    private BufferedOutputStream bufferedFileOutputStream = null;

    private FileInputStream fileInputStream = null;
    private BufferedInputStream bufferedFileInputStream = null;
        
	boolean quitCommand = false;	
	
	private String currentDirectory;
	private String sysFileSeparator;

	
	/**
	 * Initializes newly created FTPClientWorker object before use.
	 * Determines the directory the server resides in and the operating
	 * system's file separator.
	 * @param clientsSocket Socket created in myftp class
	 */
	public FTPClientWorker(Socket clientsSocket){
        this.clientsSocket = clientsSocket;
        this.currentDirectory = System.getProperty("user.dir");
		this.sysFileSeparator = System.getProperty("file.separator");
    }	
	
	/* 
	 * Overrides the run() method from Runnable class and assigns the input
	 * and output streams to variables. Creates loop to accept user commands
	 * until quit is entered.
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run(){
		
		// Takes user command
		userInputScanner = new Scanner(System.in);
		
		// Try assigning input and output streams and starting while loop
		// catch possible errors and inform user. Finally close all streams
		// and socket.
		try {					
			
			inputFromServer = new DataInputStream(clientsSocket.getInputStream());
			inputFromServerBuffered = new BufferedReader(new InputStreamReader(inputFromServer));
			
			outputToServer = new PrintStream(clientsSocket.getOutputStream(), true);
			
			while (!quitCommand) {
		       	try {
					commands();
				} catch (IOException e) {
					System.err.println("IOException for command while loop:  " + e + "\n" + e.getMessage());					
				}
			}
		
		}
		catch(IOException e) {
			System.err.println("Stream creation failed:  " + e + "\n" + e.getMessage());
		}
		finally {
			
			// Close the input and output streams and the socket.
			try {
				
				Thread.sleep(1000);
				
				if (inputFromServer != null) {
					inputFromServer.close();
				}

				if (inputFromServerBuffered != null) {
					inputFromServerBuffered.close();
				}

				if (outputToServer != null) {
					outputToServer.close();
				}

				if (clientsSocket != null) {
					clientsSocket.close();
				}

			} 
			catch (IOException e) {
				System.err.println("IOException while trying to close streams:  " + e + "\n" + e.getMessage());
			} catch (InterruptedException e) {
				System.err.println("Interrupted thread Exception while trying to close thread:  " 
									+ e + "\n" + e.getMessage());
			}
		}
	}	
	
	/**
	 * Method to display prompt to user and take user input to determine
	 * the command entered. Continue until user enters quit.
	 * @throws IOException
	 */
	private void commands() throws IOException {
		
		System.out.print("myftp> ");
        
    	while (userInputScanner.hasNextLine()) {
    		
            
        	String commands = userInputScanner.nextLine();
        	String command = null;
        	String arguments = null;
        	
        	
        	try (Scanner separateCommand = new Scanner(commands)) {

        		if (separateCommand.hasNext()) {
        			
        			command = separateCommand.next();
        		}
        		else {
            		System.out.print("myftp> ");
            		continue;              		  
        		}

        		if (separateCommand.hasNext()) {
        		  
        			arguments = separateCommand.next();
        		}
        	}

        	// If else block to route the command
        	if (command.toUpperCase().equals("GET")) {

        		getCommand(command, arguments);        		

        	}
        	else if (command.toUpperCase().equals("PUT")) {	
        		
        		putCommand(command, arguments);
        		
        	}
        	else if (command.toUpperCase().equals("LS")) {
        		
        		lsCommand(commands);
        	
        	}
        	else if (command.toUpperCase().equals("QUIT")) {
        		
        		quitCommand(commands);
        		break;
        	
        	}
        	else {
        		
        		remainingCommands(commands);
        	}

            
            System.out.print("\nmyftp> ");
            
        }		
    
    }  
	 
	/**
	 * Command get retrieves a file from the server and copy
	 * it to the client.
	 * @param command String representation of command entered by user
	 * @param arguments String representation of file name entered by user
	 */
	private void getCommand(String command, String arguments) {
		
		// Send command and file name to server
		messageServer(command + " " + arguments);
					
		// If file is not found on server inform user and return from method   		
		if ((receiveServerResponse()).toUpperCase().equals("NOT FOUND")) {
			System.out.println("File not found.");
			return;
		}

		// Get file length from server to setup for transfer
		String filesLengthString = receiveServerResponse();
		Integer filesLength = Integer.parseInt(filesLengthString);

		// String representation of full path name for file to be received
		String filePath = (currentDirectory + sysFileSeparator + arguments);
		
		// Create new file at specified path name
		File newFile = new File(filePath);

		// Try to create a file output stream and byte buffer the size of the file length.
		// Read input from server into byte buffer then proceed to use 
		// (buffered) file output stream to write from byte buffer to the created file.
		// Inform user upon file transfer completion.
		// Catch possible errors.
		try {
        
   			fileOutputStream = new FileOutputStream(newFile);
            bufferedFileOutputStream = new BufferedOutputStream(fileOutputStream);
            
            byte[] buffer = new byte[filesLength];
            
            inputFromServer.read(buffer,0, filesLength);     

            
            bufferedFileOutputStream.write(buffer, 0 , filesLength);
            bufferedFileOutputStream.flush();
            
            newFile.setReadable(true, false);
            newFile.setWritable(true, false);
            
            System.out.println("File " + arguments + " retrieving complete."); 
            
        } 
    	catch (IOException e) {
            System.err.println("IOException creating file output streams: " + e + "\n" + e.getMessage());
        }     		
		finally {
            if (fileOutputStream != null) {
            	try {
            		fileOutputStream.close();
				} catch (IOException e) {
					System.err.println("Closing file output stream failed.");
				}
            }
            if (bufferedFileOutputStream != null) {
            	try {
            		bufferedFileOutputStream.close();
				} catch (IOException e) {
					System.err.println("Closing buffered file output stream failed.");
				}
            }
        }   
	}	
	
	/**
	 * Command put sends a file to the server from the client.
	 * @param command command String representation of command entered by user
	 * @param arguments String representation of file name entered by user
	 */
	private void putCommand(String command, String arguments) {
		
		// Setup File object to prepare to send to server
		File fileToSend = new File(arguments);
        
		// If file name entered by user does not exist inform user
		// and return from method.
		if (!fileToSend.exists()) {
            System.out.println("File not found");
            return;
        }
		
		// Send put command and filename to server
		messageServer(command + " " + arguments);  

		// Parse Long of file length to a String to send to server
        String fileLengthString = Long.toString(fileToSend.length());
        
        // Send file length to server
        messageServer(fileLengthString);
        
		// Try to create a byte buffer the size of the file length.
		// Read from file into file input stream then proceed to read
        // from (buffered) file input stream into byte buffer. Write from
        // the byte buffer to the output stream to the Server.
		// Inform user upon file transfer completion.
		// Catch possible errors.
        try {
            
            byte[] buffer = new byte[(int) fileToSend.length()];
            
            fileInputStream = new FileInputStream(fileToSend);
            bufferedFileInputStream = new BufferedInputStream(fileInputStream);
            bufferedFileInputStream.read(buffer,0,buffer.length);
            
            outputToServer.write(buffer,0,buffer.length);
            outputToServer.flush();
            
            System.out.println("File " + arguments + " sending complete.");
        } 
        catch (FileNotFoundException e) {
            System.err.println("FileNotFoundException: " + e + "\n" + e.getMessage());
        } 
        catch (IOException e) {
            System.err.println("IOException: " + e + "\n" + e.getMessage());
        } 
        finally {
            if (fileInputStream != null) {
            	try {
					fileInputStream.close();
				} catch (IOException e) {
					System.err.println("IOException closing file input stream:  " + e + "\n" + e.getMessage());
				}
            }
            if (bufferedFileInputStream != null) {
            	try {
					bufferedFileInputStream.close();
				} catch (IOException e) {
					System.err.println("IOException closing buffered input stream:  " + e + "\n" + e.getMessage());
				}
            }            
        }       
	}	
	
	/**
	 * Command ls retrieves list of files and directories in the current
	 * directory on the server.
	 * @param commands String representation of the ls command entered by the user
	 */
	private void lsCommand(String commands) {
		
		// Send ls command to user
		messageServer(commands);
		
		// Receive as a string the number of filenames the server will be sending
		String numberOfFilesString = receiveServerResponse();
		
		// Parse the String to an int
		int numberOfFiles = Integer.parseInt(numberOfFilesString);
		
		// Create a List to store the filenames
		List<String> fileList = new ArrayList<String>();
		
		// Iterate for the number of filenames to be received 
		// and add each filename to the List
		for(int index = 0; index < numberOfFiles; index ++) {
			fileList.add(receiveServerResponse());
		}
		
		// Iterate over the List and print the filenames for the user
		for(String file : fileList) {        			
			System.out.println(file);
		}		
	}	
	
	/**
	 * Sends delete, cd, mkdir, or pwd command to the server.
	 * Receives server's response and prints for user.
	 * @param commands String representation of command entered by user
	 */
	private void remainingCommands(String commands) {
		messageServer(commands); 
		System.out.print(receiveServerResponse());
	}	
	
	/**
	 * Sends quit command to server and sets quitCommand boolean
	 * to true to exit loop.
	 * @param commands String representation of quit command
	 */
	private void quitCommand(String commands) {
		messageServer(commands);
		quitCommand = true;
	}	
	
	/**
	 * Sends messages to the server and flushes the stream.
	 * @param message String representation of message to send to server
	 */
	private void messageServer(String message) {
		outputToServer.println(message);
		outputToServer.flush();
	}	
	
	/**
	 * Receives server's response.
	 * @return String representation of server's response
	 */
	private String receiveServerResponse() {
		String serverResponse = null;
		
		// Tries to receive server's response and assign to String
		// and catches possible errors.
		try {
			serverResponse = inputFromServerBuffered.readLine();
		} catch (IOException e) {
			System.err.println("IOException:  " + e + "\n" + e.getMessage());
		}
		return serverResponse;
	}
}

