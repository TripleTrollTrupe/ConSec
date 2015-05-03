import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

/**
 * PhotoShareClient
 * @author SC001
 * @author fc41935 - Paulo Antunes
 * @author fc43273 - Ricardo Costa
 * @author fc44223 - Henrique Mendes
 * Class that runs the client for the PhotoShare application
 */
public class PhotoShareClient {

	public static void main(String[] args) {

		System.out.println("client");

		boolean validInput = true;

		// verify input
		if(!args[0].equals("-u") || !args[2].equals("-a") || !args[3].contains(":")){
			System.out.println("Incorrect arguments!");
			validInput = false;
		}

		String[] serverAddress = args[3].split(":");
		String server=serverAddress[0];

		if(validInput && !Pattern.matches("\\d+",serverAddress[1])){
			System.out.println("Invalid port format!");
			validInput = false;
		}

		int port = 0;

		if(validInput)
			port = Integer.parseInt(serverAddress[1]);

		if(validInput && port!=23456){
			System.out.println("Invalid port!");
			validInput = false;
		}

		if(validInput) {
			String userID = args[1];

			PhotoShareClient client = new PhotoShareClient();

			client.startClient(server, port, userID, Arrays.copyOfRange(args, 4, args.length));
		}
		else{
			System.out.println("Correct usage: PhotoShare -u <localUserId> -a <serverAddress> [ -p <photos> | -l <userId> " + 
					"| -g <userId> | -c <comment> <userId> <photo> | -f <followUserIds> | -n ]");
		}
	}


	/**
	 * @param server - IP of the server to connect to
	 * @param port   - port to establish connection
	 * @param userID - ID that represents current user
	 * @param optionArgs - arguments from the console, relative to operations
	 * Starts up the connection and executes the operations specified in the console parameters
	 */
	public void startClient(String server, int port, String userID, String [] optionArgs) {
		System.setProperty("javax.net.ssl.trustStore",("."+File.separator+"clientkeystore.jks"));
		SocketFactory sf = SSLSocketFactory.getDefault();
		Socket soc = null;
		ObjectOutputStream outStream = null;
		ObjectInputStream inStream = null;

		boolean connected = false;

		try {
			soc = sf.createSocket(server, port);
			outStream = new ObjectOutputStream(
					soc.getOutputStream());
			inStream = new ObjectInputStream(
					soc.getInputStream());
			connected = true;
		} catch (IOException e) {
			//System.err.println(e.getMessage());
			connected = false;
		}

		if(connected) {
			try {


				Scanner sc = new Scanner(System.in);
				System.out.print(userID + " password: "); String passwd = sc.next();
				sc.close();

				outStream.writeObject(userID);
				outStream.writeObject(passwd);

				// get authentication answer
				if ( (Boolean) inStream.readObject() ) {

					System.out.println("Authentication succeeded");

					switch(optionArgs[0]){

					case "-p": 

						if(optionArgs.length >= 2){
							boolean noError = true;

							for(int i = 1; i < optionArgs.length && noError; i++){
								noError = sendFile(outStream, inStream, optionArgs[i]);
							}
							if(!noError)
								System.out.println("File already exists in server or is empty!");
						}
						else
							System.out.println("Insufficient arguments!");
						break;

					case "-l":

						if(optionArgs.length >= 2)
							getPhotoInfo(outStream,inStream,optionArgs[1]);
						else
							System.out.println("Insufficient arguments!");
						break;

					case "-g":

						if(optionArgs.length >= 2){
							if(getUserData(inStream, outStream, optionArgs[1]))
								System.out.println("User " + optionArgs[1] + " data successfully copied from server");
							else
								System.out.println("Must be subscribed to user " + optionArgs[1] + "!");
						}
						else
							System.out.println("Insufficient arguments!");
						break;

					case "-c":

						if(optionArgs.length >= 4){
							outStream.writeObject("-c");
							outStream.writeObject(optionArgs[1]);
							outStream.writeObject(optionArgs[2]);
							outStream.writeObject(optionArgs[3]);
							if((Boolean)inStream.readObject())
								System.out.println("Commented successfully");
							else
								System.out.println("Comment failed, target photo's owner is not followed by this user!");
						}
						else
							System.out.println("Insufficient arguments!");
						break;

					case "-f":
						if(optionArgs.length >= 2){
							for(int i = 1; i < optionArgs.length; i++){
								if(userID.equals(optionArgs[i]))
									System.out.println("User cannot follow himself/herself!");
								else{
									outStream.writeObject("-f");
									outStream.writeObject(optionArgs[i]);
									if((Boolean)inStream.readObject())
										System.out.println("User " + optionArgs[i] + " subscribed " + userID + " with success");
									else
										System.out.println("User " + optionArgs[i] + " is non existent or already subscribed!");
								}
							}
						}
						else
							System.out.println("Insufficient arguments!");
						break;

					case "-n":
						if(getSubsLatest(inStream, outStream))
							System.out.println("All latest photos and comments from followed users received");
						else
							System.out.println("User " + userID + " isn't following anyone!");
						break;
					}
				} else
					System.out.println("Authentication failed!");
			} catch (FileNotFoundException e) {
				System.out.println("File not found!");
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				try {
					outStream.writeObject("-t");
					if(outStream != null) outStream.close();
					if(inStream != null) inStream.close();
					if(soc != null) soc.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		else
			System.out.println("Server Down!");

		System.out.println("end of execution");
	}

	/**
	 * @param outStream - Stream used to send objects to server
	 * @param inStream  - Stream used to receive objects from server
	 * @param file      - File to send to the server
	 * @return true - if there is no error during the operation / false - if an error interrupts the operation
	 * @throws IOException - If there's an unexpected issue with outStream
	 * @throws ClassNotFoundException - If there's an unexpected issue with inStream
	 * Sends files to the connnected server, sends size first, then file name and finally sends the bytes of the file
	 */
	private boolean sendFile(ObjectOutputStream outStream, ObjectInputStream inStream, String file) throws IOException, ClassNotFoundException {

		boolean noError = true;

		outStream.writeObject("-p");
		File f = new File(file);

		byte[] fileByteBuf = new byte [1024];
		int fileSize = (int) f.length();
		String filename = f.getName();

		outStream.writeObject(fileSize);
		outStream.writeObject(filename);
		System.out.println("<-- " + fileSize);
		System.out.println(filename);

		noError = (Boolean) inStream.readObject();

		if(noError){
			// send packets of max 1024 bytes
			int n;
			FileInputStream fin = new FileInputStream(f);
			while ((n=fin.read(fileByteBuf, 0, 1024))>0) { 
				outStream.write(fileByteBuf, 0, n);
			}
			System.out.println("File transfer completed!");
			fin.close();
		}

		return noError;
	}

	/**
	 * @param outStream - Stream used to send objects to server
	 * @param inStream  - Stream used to receive objects from server
	 * @param userId    - User to get information from
	 * @throws IOException - If there's an unexpected issue with outStream
	 * @throws ClassNotFoundException - If there's an unexpected issue with inStream
	 * Requests information about the activity of a certain user
	 */
	private void getPhotoInfo(ObjectOutputStream outStream, ObjectInputStream inStream, String userId) throws IOException, ClassNotFoundException{
		boolean keepReading = true;

		outStream.writeObject("-l");
		outStream.writeObject(userId);
		if(!(Boolean)inStream.readObject()){ // if user doens't exist or is not subscribed to
			System.out.println("Not an existing or subscribed user");
			keepReading = false;
		}
		while(keepReading){
			System.out.println((String) inStream.readObject()); // prints date and photo name
			keepReading= (Boolean) inStream.readObject(); // checks if there still is info to receive
		}
	}

	/**
	 * @param inStream  - Stream used to receive objects from server
	 * @param outStream - Stream used to send objects to server
	 * @param user      - User to get data from
	 * @throws IOException - If there's an unexpected issue with outStream
	 * @throws ClassNotFoundException - If there's an unexpected issue with inStream
	 * Requests a copy of the user's photos
	 */
	private boolean getUserData(ObjectInputStream inStream, ObjectOutputStream outStream, String user) throws IOException, ClassNotFoundException {

		outStream.writeObject("-g");
		outStream.writeObject(user);

		if(!(Boolean)inStream.readObject())
			return false;

		boolean receiving = true;

		// receive photos
		while(receiving){
			switch((String)inStream.readObject()){
			case "-p":
				receiveFile(inStream, outStream, user, "photos");
				break;
			case "-t":
				receiving = false;
				break;
			}
		}

		// receive comments
		receiving = true;
		while(receiving){
			switch((String)inStream.readObject()){
			case "-p":
				receiveFile(inStream, outStream, user, "comments");
				break;
			case "-t":
				receiving = false;
				break;
			}
		}
		return true;
	}

	/**
	 * @param inStream  - Stream used to receive objects from server
	 * @param outStream - Stream used to send objects to server
	 * @return true - if operation is succesful / false - if the operation is interrupted
	 * @throws IOException - If there's an unexpected issue with outStream
	 * @throws ClassNotFoundException - If there's an unexpected issue with inStream
	 * Requests latest activity from each of the users subscribed to
	 */
	private boolean getSubsLatest(ObjectInputStream inStream, ObjectOutputStream outStream) throws IOException, ClassNotFoundException {

		outStream.writeObject("-n");

		boolean receiving = (Boolean)inStream.readObject();

		if(!receiving)
			return false;

		String received = "";
		String currentUser = "";

		while(receiving){
			switch((received = (String)inStream.readObject())){
			case "-p":
				receiveFile(inStream, outStream, currentUser, "photos");
				break;
			case "-c":
				inStream.readObject(); //consume "-p" of the sendFile method
				receiveFile(inStream, outStream, currentUser, "comments");
				break;
			case "-t":
				receiving = false;
				break;
			default:
				currentUser = received;
			}
		}
		return true;
	}

	/**
	 * @param inStream  - Stream used to receive objects from server
	 * @param outStream - Stream used to send objects to server
	 * @param user      - ID of the current user
	 * @param dir       - Directory in which the files are to be saved
	 * @return true - if operation is successful / false - if the operation is interrupted
	 * @throws IOException - If there's an unexpected issue with outStream
	 * @throws ClassNotFoundException - If there's an unexpected issue with inStream
	 * Receives files sent from the server, receives size first, then proceeds to receive the bytes of the file
	 */
	private boolean receiveFile(ObjectInputStream inStream, ObjectOutputStream outStream, String user, String dir) throws IOException, ClassNotFoundException {

		FileOutputStream fos = null;

		try{
			int size = 0;
			String filename = "";


			size = (Integer) inStream.readObject();
			filename = (String) inStream.readObject();

			System.out.println("--> " + size);
			System.out.println(filename);

			// create file and directories if non existing
			Path fpath = Paths.get("." + File.separator + user + File.separator + dir + File.separator + filename);
			File f = new File("." + File.separator + user + File.separator + dir + File.separator + filename);
			if(!f.exists()){
				Files.createDirectories(fpath.getParent());
				f.createNewFile();
			}

			outStream.writeObject(true);

			byte[] bytebuf = new byte[1024];
			int bytesRead=0;
			fos = new FileOutputStream(f);

			while (bytesRead < size) {	
				int count = inStream.read(bytebuf, 0, 1024);
				if (count == -1) {
					throw new IOException("Expected file size: " + size
							+ "\nRead size: " + bytesRead);
				}

				fos.write(bytebuf, 0, count);
				bytesRead += count;
			}
			
			System.out.println("File transfer completed!");

		} finally {
			if(fos != null)
				fos.close();
		}
		return true;
	}
}
