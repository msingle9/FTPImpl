/**
 * 
 */
package FTPServer;

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
import java.util.Scanner;

/**
 * @author Christine McGee
 *
 */
public class FTPServerWorker implements Runnable {
	
	private Socket socket = null;
	private boolean quitCommand = false;		

	private String root;
	private String currentDirectory;
	private String sysFileSeparator;


	private DataInputStream inputFromClient = null;
	private BufferedReader inputFromClientBuffered = null;

	private PrintStream outputToClient = null;

	private FileOutputStream fileOutputStream = null;
	private BufferedOutputStream bufferedFileOutputStream = null;
	private FileInputStream fileInputStream = null;
	private BufferedInputStream bufferedFileInputStream = null;


	public FTPServerWorker(Socket socket) {

		this.socket = socket;
		this.currentDirectory = System.getProperty("user.dir");
		this.root = System.getProperty("user.home");
		this.sysFileSeparator = System.getProperty("file.separator");
	}


	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		System.out.println("Connected to client");
		try {

			inputFromClient = new DataInputStream(socket.getInputStream());
			inputFromClientBuffered = new BufferedReader(new InputStreamReader(inputFromClient));

			outputToClient = new PrintStream(socket.getOutputStream(), true);

		}
		catch(IOException e) {
			System.err.println("IOException:  " + e + "\n" + e.getMessage());
		}


		try {
			// Get the command and possible arguments from the client
			while (!quitCommand) {

				processCommand();

			}	  
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/**
		 * Close the input and output streams and the socket.
		 */

		try {
			if (inputFromClient != null) {
				inputFromClient.close();
			}
			if (outputToClient != null) {
				outputToClient.close();
			}
			if (inputFromClientBuffered != null) {
				inputFromClientBuffered.close();
			}
			if (socket != null) {
				socket.close();
			}
			System.out.println("Disconnected from client");
		} catch (IOException e) {
			System.err.println("IOException:  " + e);
		}

	}

	private void processCommand() throws IOException {

		String commands = receiveClientResponse();
		String command = null;
		String arguments = null;

		try (Scanner separateClientCommand = new Scanner(commands)) {

			if (separateClientCommand.hasNext()) {

				command = separateClientCommand.next().toUpperCase();


				if (separateClientCommand.hasNext()) {

					arguments = separateClientCommand.next();
				}

			}

			clientsCommand(command, arguments);
		}
	}



	private void clientsCommand(String command, String arguments) throws IOException {

		// Switch statement for clients different commands
		switch(command) {


		case "GET":                
			getCommand(arguments);
			break;

		case "PUT":
			putCommand(arguments);
			break;

		case "DELETE":
			deleteCommand(arguments);
			break;

		case "LS":
			lsCommand();
			break;

		case "CD":
			cdCommand(arguments);
			break;

		case "MKDIR":
			mkdirCommand(arguments);
			break;

		case "PWD":
			pwdCommand();
			break;

		case "QUIT":
			quitCommand();
			break;                   

		default:
			messageClient("Unknown command");
			break;
		}

	}



	private void getCommand(String argument) throws IOException {


		String fileNameClientWants = argument;

		File fileClientWants = new File(currentDirectory + sysFileSeparator + fileNameClientWants);

		if (!fileClientWants.exists()) {
			messageClient("NOT FOUND");
			return;
		} else {
			messageClient("EXISTS");
		}

		messageClient(Long.toString(fileClientWants.length()));

		try {

			File fileReference = new File(fileClientWants.getPath());

			byte[] buffer = new byte[(int) fileReference.length()];

			fileInputStream = new FileInputStream(fileReference);
			bufferedFileInputStream = new BufferedInputStream(fileInputStream);
			bufferedFileInputStream.read(buffer,0,buffer.length);

			outputToClient.write(buffer,0,buffer.length);
			outputToClient.flush();

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

	private void putCommand(String argument) throws IOException {

		File fileToCreate = new File("Server" + argument);					//REMOVE SERVER for real use

		String fileLengthFromClient = receiveClientResponse();

		Integer filesLength = Integer.parseInt(fileLengthFromClient);

		try {

			fileOutputStream = new FileOutputStream(fileToCreate);
			bufferedFileOutputStream = new BufferedOutputStream(fileOutputStream);

			byte[] buffer = new byte[filesLength];

			inputFromClient.read(buffer,0, filesLength);     


			bufferedFileOutputStream.write(buffer, 0 , filesLength);
			bufferedFileOutputStream.flush();

		} 
		catch (IOException e) {
			System.err.println("IOException: " + e);
		}    



	}

	/**
	 * Delete file in directory with name given by client
	 * @param argument
	 */
	private void deleteCommand(String argument) {
		try {         
			File fileToDelete = new File(currentDirectory, argument);            

			if(fileToDelete.exists()) {
				if(fileToDelete.delete()) {  
					messageClient("Removed " + fileToDelete.getName());
				} 
				else {  
					messageClient("Deletion of " + fileToDelete.getName() + " Failed");  
				}  
			}
			else {
				messageClient("File does not exist.");
			}
		}  
		catch(Exception e) {  
			e.printStackTrace();  
		}  

	}


	/**
	 * Retrieves list of files from current directory and sends to Client
	 */
	private void lsCommand() {

		File directoryFile = new File(determineCurrentDirectory());

		String directoryFiles[] = directoryFile.list();

		messageClient(Integer.toString(directoryFiles.length));

		for(String file: directoryFiles){

			messageClient(file);

		}

	}


	/**
	 * Takes Client arguments and determines which directory to change to then
	 * changes current directory client is in.
	 * @param argument The argument from the client
	 */
	private void cdCommand(String argument) {

		String directory = determineCurrentDirectory();
		String splitUpArgument[] = argument.split("sysFileSeparator");


		for(int index = 0; index < splitUpArgument.length; index++) {

			if ((splitUpArgument[index].equals("..")) && (directory.length() > root.length())) {

				int indexOfLastSeparator = directory.lastIndexOf(sysFileSeparator);

				if (indexOfLastSeparator > 0) {

					directory = directory.substring(0, indexOfLastSeparator);

				}
			}
			else if ((argument != null) && (!argument.equals("."))) {

				File directoryToCheck = new File(currentDirectory, argument);

				if(directoryToCheck.exists() && directoryToCheck.isDirectory()) {
					directory = directory + sysFileSeparator + argument;
				}
				else {
					messageClient("No such file or directory.");
					return;
				}
			}

		}

		this.currentDirectory = directory;
		//System.setProperty("user.dir", directory);
		messageClient("");
	}


	/**
	 * Makes a directory in current directory as named by Client
	 * @param argument The arguments from the Client
	 */
	private void mkdirCommand(String argument) {
		if (argument != null) {

			String newDirectoryPath = (currentDirectory + sysFileSeparator + argument);

			File directoryToMake = new File(newDirectoryPath);

			if(!directoryToMake.mkdir()) {

				messageClient("New Directory creation failed");

			}
		}
		else {

			messageClient("Directory name must not be blank");

		}

		messageClient("");
	}


	/**
	 * Sends the absolute path of the remote current working directory
	 * to the Client
	 */
	private void pwdCommand() {
		determineCurrentDirectory();
		messageClient("Remote working directory: " + currentDirectory);
	}


	/**
	 * Terminates the while loop to close the thread socket connection
	 */
	private void quitCommand() {
		quitCommand = true;        	
	}


	/**
	 * Determines the remote current directory from the System
	 * @return currentDirectory String representing the current working directory
	 */
	private String determineCurrentDirectory() {
		
		return this.currentDirectory;
	}


	/**
	 * Outputs desired message to client via output stream
	 * @param message The message for the client
	 */
	private void messageClient(String message) {
		outputToClient.println(message);
		outputToClient.flush();
	}


	/**
	 * Receives servers response and returns its string representation
	 * @return String representation of server's response
	 */
	private String receiveClientResponse() {
		String clientResponse = null;
		try {
			clientResponse = inputFromClientBuffered.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return clientResponse;
	}


}
