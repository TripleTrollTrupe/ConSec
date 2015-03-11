import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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
						outStream.writeObject("-l");
						outStream.writeObject(optionArgs[5]);
						System.out.println((String) inStream.readObject());
						System.out.println("end of execution");
						break;

					case "-g":
						outStream.writeObject("-g");
						outStream.writeObject(optionArgs[5]);
						//TODO what are comments?
						break;

					case "-c":
						outStream.writeObject("-c");
						outStream.writeObject(optionArgs[5]);
						outStream.writeObject(optionArgs[6]);
						outStream.writeObject(optionArgs[7]);
						System.out.println("end of execution");
						break;

					case "-f":
						outStream.writeObject("-f");
						outStream.writeObject(optionArgs[5]);
						System.out.println("end of execution");
						break;

					case "-n":
						outStream.writeObject("-n");
						//TODO: receber as cenas do servidor too layzee ATM sry
						System.out.println("end of execution");
					}
				} else
					System.out.println("Authentication failed");
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
}
