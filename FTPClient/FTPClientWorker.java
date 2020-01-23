/**
 * 
 */
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
 * @author Christine McGee
 *
 */
public class FTPClientWorker implements Runnable{
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

	
	public FTPClientWorker(Socket clientsSocket){
        this.clientsSocket = clientsSocket;
        this.currentDirectory = System.getProperty("user.dir");
		this.sysFileSeparator = System.getProperty("file.separator");
    }
	
	
	public void run(){
		
		userInputScanner = new Scanner(System.in);
		
		try {					
			
			inputFromServer = new DataInputStream(clientsSocket.getInputStream());
			inputFromServerBuffered = new BufferedReader(new InputStreamReader(inputFromServer));
			
			outputToServer = new PrintStream(clientsSocket.getOutputStream(), true);
			
			while (!quitCommand) {
		       	try {
					commands();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		
		}
		catch(IOException e) {
			System.err.println("IOException:  " + e + "\n" + e.getMessage());
		}
		finally {
			
			
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
				System.err.println("IOException:  " + e + "\n" + e.getMessage());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
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
	 
	private void getCommand(String command, String arguments) {
		
		messageServer(command + " " + arguments);
					
		// If file is not found on server inform user and return   		
		if ((receiveServerResponse()).toUpperCase().equals("NOT FOUND")) {
			System.out.println("File not found.");
			return;
		}

		String filesLengthString = receiveServerResponse();
		Integer filesLength = Integer.parseInt(filesLengthString);


		String filePath = (currentDirectory + sysFileSeparator + arguments);
		
		File newFile = new File(filePath);

		try {
        
   			fileOutputStream = new FileOutputStream(newFile);
            bufferedFileOutputStream = new BufferedOutputStream(fileOutputStream);
            
            byte[] buffer = new byte[filesLength];
            
            inputFromServer.read(buffer,0, filesLength);     

            
            bufferedFileOutputStream.write(buffer, 0 , filesLength);
            bufferedFileOutputStream.flush();
            
            System.out.println("File " + arguments + " retrieving complete."); 
            
        } 
    	catch (IOException e) {
            System.err.println("IOException: " + e);
        }     		
		
	}
	
	private void putCommand(String command, String arguments) {
		
		File fileToSend = new File(arguments);
        
		if (!fileToSend.exists()) {
            System.out.println("File not found");
            return;
        }
		
		messageServer(command + " " + arguments);  

        String fileLengthString = Long.toString(fileToSend.length());
        
        messageServer(fileLengthString);
        
        try {
        	
            File fileReference = new File(fileToSend.getPath());
            
            byte[] buffer = new byte[(int) fileReference.length()];
            
            fileInputStream = new FileInputStream(fileReference);
            bufferedFileInputStream = new BufferedInputStream(fileInputStream);
            bufferedFileInputStream.read(buffer,0,buffer.length);
            
            outputToServer.write(buffer,0,buffer.length);
            outputToServer.flush();
            
            System.out.println("File " + arguments + " sending complete.");
        } 
        catch (FileNotFoundException e) {
            System.err.println("FileNotFoundException: " + e);
        } 
        catch (IOException e) {
            System.err.println("IOException: " + e);
        } 
        finally {
            if (fileInputStream != null) {
            	try {
					fileInputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            if (bufferedFileInputStream != null) {
            	try {
					bufferedFileInputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        }       
	}
	
	private void lsCommand(String commands) {
		
		messageServer(commands);
		
		
		String numberOfFilesString = receiveServerResponse();
		
		int numberOfFiles = Integer.parseInt(numberOfFilesString);
		
		List<String> fileList = new ArrayList<String>();
		
		for(int index = 0; index < numberOfFiles; index ++) {
			fileList.add(receiveServerResponse());
		}
		
		for(String file : fileList) {        			
			System.out.println(file);
		}	
		
		/*  											DOES NOT WORK RIGHT
		int maxLength = 0;
		
		for(String file : fileList) {
			  maxLength = (file.length() > maxLength) ? file.length() : maxLength;
		}
		
		
		for(String file : fileList) {        			
			System.out.printf("%-" + (maxLength) + "s\t\t", file);
		}
		 */
	}
	
	private void remainingCommands(String commands) {
		messageServer(commands); 
		System.out.print(receiveServerResponse());
	}
	
	private void quitCommand(String commands) {
		messageServer(commands);
		quitCommand = true;
	}
	
	private void messageServer(String message) {
		outputToServer.println(message);
		outputToServer.flush();
	}
	
	private String receiveServerResponse() {
		String serverResponse = null;
		try {
			serverResponse = inputFromServerBuffered.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return serverResponse;
	}
}

