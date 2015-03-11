import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.file.Files;

// PhotoShareServer

public class PhotoShareServer {

	protected int serverPort;

	public static void main(String[] args) {
		System.out.println("server: main");

		if(args.length == 1){
			PhotoShareServer server = new PhotoShareServer(Integer.parseInt((args[0])));
			server.startServer();
		}
		else{
			System.out.println("Incorrect use!");
			System.out.println("Correct usage: PhotoShareServer <port>");
		}
	}

	public PhotoShareServer(int port) {
		this.serverPort = port;
	}

	// Because server is never stopped in this implementation
	@SuppressWarnings("resource")
	public void startServer() {
		ServerSocket sSoc = null;

		try {
			sSoc = new ServerSocket(this.serverPort);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		ExecutorService threadPool = Executors.newFixedThreadPool(20);

		while (true) {
			try {
				Socket inSoc = sSoc.accept();
				threadPool.execute(new ServerThread(inSoc));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// sSoc.close();
	}

	// threads
	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("thread: created");
		}

		public void run() {
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(
						socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(
						socket.getInputStream());

				String user = "";
				String passwd = "";

				// get user and password
				try {
					user = (String) inStream.readObject();
					passwd = (String) inStream.readObject();
				} catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				}				

				// auhenticates user
				if(authenticate(outStream, user, passwd)){

					String option = "";
					boolean working = true;

					// get and proccess client requests
					while(working) {
						try {
							option = (String) inStream.readObject();
						} catch(ClassNotFoundException e1) {
							e1.printStackTrace();
						}

						switch(option) {

						case "-p":
							working = receiveFile(inStream, outStream, user);
							break;

						case "-t":
							System.out.println("Finished processing user " + user + " request");
							working = false;
							break;
						}
					}
					System.out.println("thread: dead");
				} else
					System.out.println("Invalid Credentials!");


				outStream.close();
				inStream.close();

				socket.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// authenticate user and password from local file 
		private boolean authenticate(ObjectOutputStream outStream, String user, String passwd) throws IOException {

			File up = new File("." + File.separator + "shadow" + File.separator + "up");
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(up)));
			boolean auth = false;

			if (user.length() != 0){

				String line;
				while((line = br.readLine()) != null && !auth){
					auth = (user + ":" + passwd).equalsIgnoreCase(line);
				}
				outStream.writeObject(new Boolean(auth));
			}
			br.close();

			return auth;
		}

		// receive a file from inStream, receives size first and then the bytes
		private boolean receiveFile(ObjectInputStream inStream, ObjectOutputStream outStream, String user) throws IOException {

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

				// create file and directories if non existing
				Path fpath = Paths.get("." + File.separator + "data" + File.separator + user + File.separator + filename);
				File f = new File("." + File.separator + "data" + File.separator + user + File.separator + filename);
				if(!f.exists()){
					Files.createDirectories(fpath.getParent());
					f.createNewFile();
				}
				else{
					System.out.println("Already existing file!");
					outStream.writeObject(new Boolean(false));
					return false;
				}
				
				outStream.writeObject(new Boolean(true));

				byte[] fileByteBuf = new byte[1024];
				int bytesRead = 0; // bytes jah lidos
				fos = new FileOutputStream(f);

				// TODO display dynamic progress
				// int lastLineLength = 0;

				while (bytesRead < size) {	
					int count = inStream.read(fileByteBuf, 0, 1024);
					if (count == -1) {
						throw new IOException("Expected file size: " + size
								+ "\nRead size: " + bytesRead);
					}

					fos.write(fileByteBuf, 0, count);
					bytesRead += count;

					/* TODO display dynamic progress
					lastLineLength = ("total received: " + bytesRead + " out of " + size).length();

					for(int i = 0; i < lastLineLength; i++)
						System.out.print("\b");

						// UNCOMMENT BELOW
					System.out.print("total received: " + bytesRead + " out of " + size);
					 */

				}
				//System.out.println();
				System.out.println("File transfer completed!");

			} finally {
				if(fos != null)
					fos.close();
			}

			return true;
		}


	}
	
	@SuppressWarnings("unused")
	private Boolean updateSubscriber(String user,String name){
		BufferedReader read = null;
		String Lines;
		Path fpath = Paths.get("." + File.separator + "data" + File.separator + user + File.separator + "subscriptions.txt");
		try {
			read = new BufferedReader(new FileReader(fpath.toString()));
			Lines = read.readLine();
			while(!Lines.equals(name) && Lines != null){
				Lines = read.readLine();
			}
			
			if(Lines == null)
				return false;
			else{
				
				// usar path para ir para a diretoria do outro utilizador?
				// depois como pegar em cada ficheiro e mandar
				
				
				
			}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;

	}
}
