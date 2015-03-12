import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

						case "-l":
							fetchPhotoInfo(inStream, outStream, user);
							break;

						case "-g":
							String subsID = "";
							subsID = (String) inStream.readObject();
							UpdateFollower(user, subsID, outStream, inStream);
							break;

						case "-c":
							String comment = "";
							String commentingUser = "";
							String filename = "";

							comment = (String) inStream.readObject();
							commentingUser = (String) inStream.readObject();
							filename = (String) inStream.readObject();

							// output operation outcome 
							outStream.writeObject(comment(user, comment, commentingUser, filename));

							break;

						case "-f":

							String subscribingUser = (String) inStream.readObject();
							outStream.writeObject(follow(subscribingUser, user));
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

			} catch (ClassNotFoundException e) {
				e.printStackTrace();
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
					auth = (user + ":" + passwd).equals(line);
				}
				outStream.writeObject(auth);
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
				Path fpath = Paths.get("." + File.separator + "data" + File.separator + user + File.separator + "photos" + File.separator + filename);
				File f = new File("." + File.separator + "data" + File.separator + user + File.separator + "photos" + File.separator + filename);
				if(!f.exists()){
					Files.createDirectories(fpath.getParent());
					f.createNewFile();
				}
				else{
					System.out.println("Already existing file!");
					outStream.writeObject(false);
					return false;
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

		private boolean follows(String user, String userID) throws IOException {

			File f = new File("." + File.separator + "data" + File.separator + user + File.separator + "subscriptions");

			if(!f.exists())
				return false;

			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
			boolean follows = false;

			String line;
			while((line = br.readLine()) != null && !follows){
				follows = (userID).equals(line);
			}
			br.close();

			return follows;
		}

		private boolean comment(String user, String comment, String userID, String filename) throws IOException{

			// create file (and directories) if non existing
			File f = new File("." + File.separator + "data" + File.separator + userID + File.separator + "photos" + File.separator + filename);
			File fc = new File("." + File.separator + "data" + File.separator + userID + File.separator + "comments" + File.separator + filename + ".comment");
			Path fpath = Paths.get("." + File.separator + "data" + File.separator + userID + File.separator + "comments" + File.separator + filename);

			if(f.exists() && follows(user, userID)){
				if(!fc.exists()){
					Files.createDirectories(fpath.getParent());
					fc.createNewFile();
				}

				BufferedWriter bw = new BufferedWriter( new FileWriter(fc,true));
				bw.write(user + ": " + comment + "\r\n");
				bw.close();

				return true;
			}
			else
				return false;

		}

		// set userID to follow user
		private boolean follow(String userID, String user) throws IOException {

			if(!userExists(userID) || follows(userID,user))
				return false;

			File f = new File("." + File.separator + "data" + File.separator + userID + File.separator + "subscriptions");

			if(!f.exists())
				f.createNewFile();

			BufferedWriter bw = new BufferedWriter( new FileWriter(f,true));
			bw.write(user + "\r\n");
			bw.newLine();
			bw.flush();
			bw.close();

			return true;
		}

		private boolean userExists(String userID){
			File f = new File("." + File.separator + "data" + File.separator + userID);

			if(f.exists() && f.isDirectory())
				return true;
			else
				return false;
		}

		private boolean UpdateFollower(String userID,String subs,ObjectOutputStream outStream,ObjectInputStream inStream) throws IOException, ClassNotFoundException{

			if(!userExists(userID) || !userExists(subs)){
				outStream.writeObject(false);
				return false;
			}
			if(!follows(userID,subs) && !(userID.equals(subs))){
				outStream.writeObject(false);
				return false;
			}
			// validated operation, start sending
			outStream.writeObject(true);

			File photoDir =new File("." + File.separator + "data" + File.separator + subs+ File.separator + "photos");
			String[] photos= photoDir.list();
			File commentsDir =new File("." + File.separator + "data" + File.separator + subs + File.separator + "comments");
			String[] comments = commentsDir.list();

			if(photos != null)
				for(int i = 0; i < photos.length; i++){
					sendFile(outStream, inStream, "." + File.separator + "data" + File.separator + subs+ File.separator + "photos" + File.separator + photos[i]);
				}

			outStream.writeObject("-t");

			if(comments != null)
				for(int i = 0; i < comments.length; i++){
					sendFile(outStream, inStream, "." + File.separator + "data" + File.separator + subs+ File.separator + "comments" + File.separator + comments[i]);
				}

			outStream.writeObject("-t");

			return true;
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


		//get all comments from a certain subscriber
		private void fetchPhotoInfo(ObjectInputStream inStream, ObjectOutputStream outStream, String user) throws IOException, ClassNotFoundException{


			String userID = (String) inStream.readObject();

			outStream.writeObject(follows(user,userID)||(user.equals(userID))); // if the the target user is followed or is himself
			File folder = new File("." + File.separator + "data"+ File.separator + userID + File.separator + "photos");

			File[] list = folder.listFiles();
			for(int i =0;i<list.length;i++){
				SimpleDateFormat date = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
				outStream.writeObject("Photo Name: " + list[i].getName() + " Upload Date: " + date.format(list[i].lastModified()));
				outStream.writeObject(true);
			}
			outStream.writeObject("No more photos");
			outStream.writeObject(false);
		}
	}
}
