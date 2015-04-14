import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
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
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PhotoShareServer
 * @author SC001
 * @author fc41935 - Paulo Antunes
 * @author fc43273 - Ricardo Costa
 * @author fc44223 - Henrique Mendes
 * Class that runs a server, implemented to support multiple client connections via a thread pool.
 */
public class PhotoShareServer {

	// the port running the listening socket
	private int serverPort;
	// the size of the thread pool
	private int tpSize;

	// Starts the server with listening socket on port specified by first argument and thread pool size 20
	// and runs it
	public static void main(String[] args) {
		System.out.println("server: main");

		if(args.length == 1){
			PhotoShareServer server = new PhotoShareServer(Integer.parseInt((args[0])),20);
			server.startServer();
		}
		else{
			System.out.println("Incorrect use!");
			System.out.println("Correct usage: PhotoShareServer <port>");
		}
	}

	/**
	 * Constructor method for PhotoShareServer class.
	 * @param port the port for the server's listening socket
	 */
	public PhotoShareServer(int port,int numThreads) {
		this.serverPort = port;
		this.tpSize = numThreads;
	}



	/**
	 * Runs the server.
	 * Creates the listening socket and the thread pool, then runs for indefinite time while accepting 
	 * new connections incomming in the listening socket and assigning threads from the thread pool to 
	 * serve those requests.
	 */
	// Warning suppressed because server is never stopped in this implementation
	// (only when the process is killed)
	@SuppressWarnings("resource")
	public void startServer() {
		ServerSocket sSoc = null;

		try {
			sSoc = new ServerSocket(this.serverPort);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		ExecutorService threadPool = Executors.newFixedThreadPool(this.tpSize);

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

	/**
	 * ServerThread
	 * @author SC001
	 * @author fc41935 - Paulo Antunes
	 * @author fc43273 - Ricardo Costa
	 * @author fc44223 - Henrique Mendes
	 * Nested class that represents a thread running on the server.
	 * Includes methods for the server operations needed.
	 */
	class ServerThread extends Thread {

		// the socket for communicating with the client whose request is being processed
		private Socket socket = null;


		/**
		 * Constructor method for ServerThread class.
		 * @param inSoc the socket for communicating with the client being served
		 */
		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("thread: created");
		}

		/* 
		 * Serves the request made by the client assigned to the present thread.
		 * @see java.lang.Thread#run()
		 */
		public void run() {
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(
						socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(
						socket.getInputStream());

				String user = "";
				String rawpasswd = "";
				int passwd;   //TODO Kaze's lazy way to adapt to hashCode
				
				// get user and password
				user = (String) inStream.readObject();
				rawpasswd = (String) inStream.readObject();
				passwd = rawpasswd.hashCode();
				// auhenticates user
				boolean auth = authenticate(user, passwd);
				outStream.writeObject(auth);

				if(auth){

					String option = "";
					boolean working = true;

					// get and proccess client requests
					while(working) {

						option = (String) inStream.readObject();

						// differentiate between different types of requests
						switch(option) {

						case "-p":
							working = receiveFile(inStream, outStream, user);
							break;

						case "-l":
							String listedUser = (String) inStream.readObject();

							fetchPhotoInfo(outStream, user, listedUser);
							break;

						case "-g":
							String subsID = "";
							subsID = (String) inStream.readObject();
							UpdateFollower(user, subsID, outStream, inStream);
							break;

						case "-c":
							String comment = "";
							String commentTargetUser = "";
							String filename = "";

							comment = (String) inStream.readObject();
							commentTargetUser = (String) inStream.readObject();
							filename = (String) inStream.readObject();

							// output operation outcome to client 
							outStream.writeObject(comment(user, comment, commentTargetUser, filename));
							break;

						case "-f":
							String subscribingUser = (String) inStream.readObject();
							outStream.writeObject(UserHandler.subscribe(subscribingUser, user));
							break;

						case "-n":
							getSubsLatest(outStream, inStream, user);
							break;

						case "-t":
							System.out.println("Finished processing user " + user + " request");
							working = false;
							break;

						default:
							System.out.println("Invalid request!");
							working = false;
							break;
						}
					}
				} else
					System.out.println("Invalid Credentials!");


				outStream.close();
				inStream.close();
				socket.close();
				System.out.println("thread: dead");

			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Authenticates the user and password from local users and passwords file.
		 * @param user the user to authenticate
		 * @param passwd the password to authenticate the user with
		 * @return true if the user and password comination is valid, false otherwise
		 * @throws IOException
		 */
		private boolean authenticate(String user, int passwd) throws IOException { //TODO Kaze changed to int 'cause hashCode

			File up = new File("." + File.separator + "shadow" + File.separator + "up");
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(up)));
			boolean auth = false;

			if (user.length() != 0){
				String line;
				while((line = br.readLine()) != null && !auth){
					auth = (user + ":" + passwd).equals(line);
				}
			}
			br.close();

			return auth;
		}

		/**
		 * Receive a file from an {@link ObjectInputStream}, receive the size first and then the corresponding bytes.
		 * @param inStream the ObjectInputStream from which the files are received
		 * @param outStream the ObjectOutputStream to validate the transfer to the client
		 * @param user the user uploading the file
		 * @return true if the transfer was successful, false otherwise
		 * @requires authenticated user
		 * @throws IOException
		 * @throws ClassNotFoundException 
		 */
		private boolean receiveFile(ObjectInputStream inStream, ObjectOutputStream outStream, String user) throws IOException, ClassNotFoundException {

			FileOutputStream fos = null;

			try{
				int size = 0;
				String filename = "";

				size = (Integer) inStream.readObject();
				filename = (String) inStream.readObject();

				System.out.println("--> " + size);
				System.out.println(filename);

				Path fpath = Paths.get("." + File.separator + "data" + File.separator + user + File.separator + "photos" + File.separator + filename);
				File f = new File("." + File.separator + "data" + File.separator + user + File.separator + "photos" + File.separator + filename);

				// check if there's already a photo with the same name owned by the same user or if empty file
				if(!f.exists() && size != 0){
					// create file and directories if non existing
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
				int bytesRead = 0;
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


		/**
		 * Creates a comment for an existing photo from a user that follows the photo's owner.
		 * @param subscribingUser the commenting user
		 * @param comment the commenting user's comment
		 * @param subscribedUser the photo's owner
		 * @param filename the photo's name
		 * @return true if the comment is created successfully, false otherwise Note that for the comment operation
		 * to be successful the photo must exist and follows(followingUser,subscribedUser) must be true
		 * @throws IOException
		 */
		private boolean comment(String subscribingUser, String comment, String subscribedUser, String filename) throws IOException{

			// create file (and directories) if non existing
			File f = new File("." + File.separator + "data" + File.separator + subscribedUser + File.separator + "photos" + File.separator + filename);
			File fc = new File("." + File.separator + "data" + File.separator + subscribedUser + File.separator + "comments" + File.separator + filename + ".comment");
			Path fpath = Paths.get("." + File.separator + "data" + File.separator + subscribedUser + File.separator + "comments" + File.separator + filename);

			if(f.exists() && (UserHandler.isSubscribed(subscribingUser, subscribedUser) || subscribingUser.equals(subscribedUser))){
				if(!fc.exists()){
					Files.createDirectories(fpath.getParent());
					fc.createNewFile();
				}

				BufferedWriter bw = new BufferedWriter( new FileWriter(fc,true));
				bw.write(subscribingUser + ": " + comment + "\r\n");
				bw.close();

				return true;
			}
			else
				return false;

		}

		/**
		 * Sends all photos and comments of a followed user.
		 * @param followingUser the user wishing to download the data
		 * @param subscribedUser the user that owns the information
		 * @param outStream the ObjectOutputStream sending the data
		 * @param inStream the ObjectInputStream used for validations while sending the data
		 * @return true if the operation succeeds, false otherwise
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private boolean UpdateFollower(String followingUser,String subscribedUser,ObjectOutputStream outStream,ObjectInputStream inStream) throws IOException, ClassNotFoundException{

			if(!UserHandler.userExists(followingUser) || !UserHandler.userExists(subscribedUser)){
				outStream.writeObject(false);
				return false;
			}
			if(!UserHandler.isSubscribed(followingUser,subscribedUser) && !(followingUser.equals(subscribedUser))){
				outStream.writeObject(false);
				return false;
			}
			// validated operation, start sending
			outStream.writeObject(true);

			File photoDir =new File("." + File.separator + "data" + File.separator + subscribedUser+ File.separator + "photos");
			String[] photos= photoDir.list();
			File commentsDir =new File("." + File.separator + "data" + File.separator + subscribedUser + File.separator + "comments");
			String[] comments = commentsDir.list();

			if(photos != null)
				for(int i = 0; i < photos.length; i++){
					sendFile(outStream, inStream, "." + File.separator + "data" + File.separator + subscribedUser+ File.separator + "photos" + File.separator + photos[i]);
				}

			outStream.writeObject("-t");

			if(comments != null)
				for(int i = 0; i < comments.length; i++){
					sendFile(outStream, inStream, "." + File.separator + "data" + File.separator + subscribedUser+ File.separator + "comments" + File.separator + comments[i]);
				}

			outStream.writeObject("-t");

			return true;
		}

		/**
		 * Sends a file.
		 * @param outStream the ObjectOutputStream used for sending the file
		 * @param inStream the ObjectInputStream for validation
		 * @param file the file to be sent
		 * @return true if no error occurs, false otherwise
		 * @throws IOException
		 * @throws ClassNotFoundException
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
		 * Sends all information about all the photos of a certain user.
		 * @param outStream the ObjectOutputStream used for sending
		 * @param followingUser the user getting the information of the other user's photos
		 * @param subscribedUser the user that owns the photos
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private void fetchPhotoInfo(ObjectOutputStream outStream, String followingUser, String subscribedUser) throws IOException, ClassNotFoundException{

			if(!(UserHandler.isSubscribed(followingUser,subscribedUser)||(followingUser.equals(subscribedUser)))){
				System.out.println("User does not exist or is not followed");
				outStream.writeObject(false);
			}
			else{
				outStream.writeObject(true); // if the the target user is followed or is himself
				File folder = new File("." + File.separator + "data"+ File.separator + subscribedUser + File.separator + "photos");

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

		/**
		 * Send the most recent photo and respective comments of each of the users followed by a user.
		 * @param outStream the ObjectOutputStream used for sending 
		 * @param inStream the ObjectInputStream for validation
		 * @param user the user getting the data
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private void getSubsLatest(ObjectOutputStream outStream, ObjectInputStream inStream, String user) throws IOException, ClassNotFoundException {

			ArrayList<String> subs = subs(user);

			if(subs != null && !subs.isEmpty()){
				outStream.writeObject(true);
				for(String subbedUser : subs){
					outStream.writeObject(subbedUser);

					File photo = lastFileModified("." + File.separator + "data"+ File.separator + subbedUser + File.separator + "photos");
					if(photo != null){
						sendFile(outStream, inStream, "." + File.separator + "data"+ File.separator + subbedUser +
								File.separator + "photos" + File.separator + photo.getName());

						File comment = new File("." + File.separator + "data" + File.separator + subbedUser +
								File.separator + "comments" + File.separator + photo.getName() + ".comment");

						if(comment.exists() && !comment.isDirectory()){
							outStream.writeObject("-c");
							sendFile(outStream, inStream, "." + File.separator + "data" + File.separator + subbedUser +
									File.separator + "comments" + File.separator + photo.getName() + ".comment");
						}
					}
				}
				outStream.writeObject("-t");
			} else
				outStream.writeObject(false);
		}

		/**
		 * Compiles a user's followed users into a list.
		 * @param followingUser the user following other users
		 * @return an ArrayList containing all the users followed by followingUser, null if no users are
		 * followed by followingUser
		 * @throws IOException
		 */
		private ArrayList<String> subs(String followingUser) throws IOException {

			File subFile = new File("." + File.separator + "data" + File.separator + followingUser + File.separator + "subscriptions");

			if(!subFile.exists())
				return null;                                                            

			ArrayList<String> subList = new ArrayList<String>();

			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(subFile)));

			if (followingUser.length() != 0){

				String line;
				while((line = br.readLine()) != null){
					if(!line.isEmpty())
						subList.add(line);
				}
			}
			br.close();

			return subList;
		}

		/**
		 * Gets the last modified file of a directory.
		 * @param dir the directory to get the last modified file of
		 * @return the last modified file of directory dir, null if dir is not a directory
		 */
		private File lastFileModified(String dir) {
			File fl = new File(dir);

			if(fl.isDirectory()){
				System.out.println("here");
				File[] files = fl.listFiles(new FileFilter() {          
					public boolean accept(File file) {
						return file.isFile();
					}
				});
				long lastMod = Long.MIN_VALUE;
				File choice = null;
				if(files != null)
					for (File file : files) {
						if (file.lastModified() > lastMod) {
							choice = file;
							lastMod = file.lastModified();
						}
					}
				return choice;
			}
			return null;
		}
	}
}
