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

public class PhotoShareClient {

	public static void main(String[] args) {

		System.out.println("client");

		boolean validInput = true;

		// TODO arg testing code
		System.out.println("args:");
		for(int jk = 0; jk < args.length; jk++)
			System.out.print(jk + " " + args[jk] +" ");
		System.out.println();

		// verify input
		if(!args[0].equals("-u") || !args[2].equals("-a")){
			System.out.println("Incorrect arguments!");
			validInput = false;
		}

		String[] serverAddress = args[3].split(":");
		String server=serverAddress[0];

		if(validInput && !Pattern.matches("\\d+",serverAddress[1])){
			System.out.println("Invalid port format!");
			validInput = false;
		}

		int port = Integer.parseInt(serverAddress[1]);

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

	public void startClient(String server, int port, String userID, String [] optionArgs) {

		Socket soc = null;
		ObjectOutputStream outStream = null;
		ObjectInputStream inStream = null;

		boolean connected = false;

		try {
			soc = new Socket(server, port);
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

						boolean noError = true;

						for(int i = 1; i < optionArgs.length && noError; i++){
							noError = sendFile(outStream, inStream, optionArgs[i]);
						}
						if(!noError)
							System.out.println("File already exists in server!");
						break;

					case "-l":

						getPhotoInfo(outStream,inStream,optionArgs[1]);
						break;

					case "-g":
						if(getUserData(inStream, outStream, optionArgs[1]))
							System.out.println("User " + optionArgs[1] + " data successfully copied from server");
						else
							System.out.println("Must be subscribed to user " + optionArgs[1] + "!");
						break;

					case "-c":
						outStream.writeObject("-c");
						outStream.writeObject(optionArgs[1]);
						outStream.writeObject(optionArgs[2]);
						outStream.writeObject(optionArgs[3]);
						if((Boolean)inStream.readObject())
							System.out.println("Commented successfully");
						else
							System.out.println("Comment failed, target photo's owner is not followed by this user!");
						break;

					case "-f":
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

						break;

					case "-n":
						outStream.writeObject("-n");
						// TODO
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

	private void getPhotoInfo(ObjectOutputStream outStream, ObjectInputStream inStream, String userId) throws IOException, ClassNotFoundException{
		boolean keepReading = true;

		outStream.writeObject("-l");
		outStream.writeObject(userId);
		if(!(Boolean)inStream.readObject()){ // se nao pertencer ou nao existir
			System.out.println("Not an existing or subscribed user");
			return; // nothing else to do, exits method
		}
		while(keepReading){
			System.out.println((String) inStream.readObject()); // prints date and photo name
			keepReading= (Boolean) inStream.readObject(); // checks if there still is info to receive
		}


	}
	
	private boolean getUserData(ObjectInputStream inStream, ObjectOutputStream outStream, String user) throws IOException, ClassNotFoundException {
		
		outStream.writeObject("-g");
		outStream.writeObject(user);
		
		if(!(Boolean)inStream.readObject())
			return false;
		
		boolean receiving = true;
		
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

	// receive a file from inStream, receives size first and then the bytes
	private boolean receiveFile(ObjectInputStream inStream, ObjectOutputStream outStream, String user, String dir) throws IOException {

		FileOutputStream fos = null;

		try{
			int size = 0;
			String filename = "";

			try {
				size = (Integer) inStream.readObject();
				filename = (String) inStream.readObject();
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			}		


			System.out.println("--> " + size);
			System.out.println(filename);

			// create file and directories if non existing TODO Change if necessary
			Path fpath = Paths.get("." + File.separator + user + File.separator + dir + File.separator + filename);
			File f = new File("." + File.separator + user + File.separator + dir + File.separator + filename);
			if(!f.exists()){
				Files.createDirectories(fpath.getParent());
				f.createNewFile();
			}

			outStream.writeObject(true);

			byte[] fileByteBuf = new byte[1024];
			int bytesRead = 0; // bytes jah lidos
			fos = new FileOutputStream(f);

			while (bytesRead < size) {	
				int count = inStream.read(fileByteBuf, 0, 1024);
				if (count == -1) {
					throw new IOException("Expected file size: " + size
							+ "\nRead size: " + bytesRead);
				}

				fos.write(fileByteBuf, 0, count);
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
