import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

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
	public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, IllegalStateException, IOException {
		System.setProperty("javax.net.ssl.keyStore",("."+File.separator+"serverkeystore.jks"));
		System.setProperty("javax.net.ssl.keyStorePassword","requiem");
		System.out.println("server: main");
		File up = new File("." + File.separator + "shadow" + File.separator
				+ "up");
		File upsha = new File("." + File.separator + "shadow" + File.separator
				+ "up.sha");
		Scanner scan = new Scanner(System.in);
		Auth.initialCheck(scan,up,upsha); // Verify integrity of user registration files
		System.out.println("Generated MAC is correct, booting up server!");

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
		ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
		ServerSocket sSoc = null;

		try {
			sSoc = ssf.createServerSocket(this.serverPort);
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
				// get user and password
				user = (String) inStream.readObject();
				rawpasswd = (String) inStream.readObject();
				// authenticates user
				boolean auth = authenticate(user, hashPassword(rawpasswd));
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
							updateFollower(user, subsID, outStream, inStream);
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
							String followingUser = (String) inStream.readObject();
							outStream.writeObject(UserHandler.follow(followingUser, user));
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
			} catch (NoSuchAlgorithmException e) {
	
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnrecoverableKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (KeyStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CertificateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SignatureException e) {
				// TODO Auto-generated catch block
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
		private boolean authenticate(String user, String passwd) throws IOException { //TODO Kaze changed to int 'cause hashCode

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
		 * @throws BadPaddingException 
		 * @throws IllegalBlockSizeException 
		 * @throws CertificateException 
		 * @throws KeyStoreException 
		 * @throws NoSuchPaddingException 
		 * @throws NoSuchAlgorithmException 
		 * @throws UnrecoverableKeyException 
		 * @throws InvalidKeyException 
		 */
		private boolean receiveFile(ObjectInputStream inStream, ObjectOutputStream outStream, String user) throws IOException, ClassNotFoundException, InvalidKeyException, UnrecoverableKeyException, NoSuchAlgorithmException, NoSuchPaddingException, KeyStoreException, CertificateException, IllegalBlockSizeException, BadPaddingException {

			FileOutputStream fos = null;

			try{
				int size = 0;
				String filename = "";

				size = (Integer) inStream.readObject();
				filename = (String) inStream.readObject();

				System.out.println("--> " + size);
				System.out.println(filename);

				
				File f = new File("." + File.separator + "data" + File.separator + user + File.separator + "photos" + File.separator + filename);
				File fcif = new File(f.getPath()+".cif");
				File fkey = new File("keys"+f.getPath()+".key");
				// check if there's already a photo with the same name owned by the same user or if empty file
				System.out.println(fcif.getPath());
				System.out.println(fkey.getPath());
				if((fcif.exists() && fkey.exists()) || size==0){
					System.out.println("Already existing file or empty file!");
					outStream.writeObject(false);
					return false;
				}

				outStream.writeObject(true);

			/*	byte[] fileByteBuf = new byte[1024];
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

				}*/
				CipherAction.cypherSize(size, f);
				CipherAction.cipherFile(f, size, inStream);
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
		 * @throws IllegalBlockSizeException 
		 * @throws CertificateException 
		 * @throws KeyStoreException 
		 * @throws NoSuchPaddingException 
		 * @throws NoSuchAlgorithmException 
		 * @throws UnrecoverableKeyException 
		 * @throws InvalidKeyException 
		 */
		private boolean comment(String follower, String comment, String followed, String filename) throws IOException, InvalidKeyException, UnrecoverableKeyException, NoSuchAlgorithmException, NoSuchPaddingException, KeyStoreException, CertificateException, IllegalBlockSizeException{

			// create file (and directories) if non existing
			String timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
			File f = new File("." + File.separator + "data" + File.separator + followed + File.separator + "photos" + File.separator + filename);
			File fc = new File("." + File.separator + "data" + File.separator + followed + File.separator + "comments" + File.separator + filename +"_"+timestamp+ ".comment");
			Path fpath = Paths.get("." + File.separator + "data" + File.separator + followed + File.separator + "comments" + File.separator + filename);
			if(f.exists() && (UserHandler.isFollowing(follower, followed) || follower.equals(followed))){
				if(!f.exists()){
					Files.createDirectories(fpath.getParent());				
				}
				CipherAction.cypherSize(comment.length(), fc);
				CipherAction.cipherComment(comment, fc);

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
		 * @throws CertificateException 
		 * @throws KeyStoreException 
		 * @throws NoSuchPaddingException 
		 * @throws NoSuchAlgorithmException 
		 * @throws UnrecoverableKeyException 
		 * @throws InvalidKeyException 
		 */
		private boolean updateFollower(String follower,String followed,ObjectOutputStream outStream,ObjectInputStream inStream) throws IOException, ClassNotFoundException, InvalidKeyException, UnrecoverableKeyException, NoSuchAlgorithmException, NoSuchPaddingException, KeyStoreException, CertificateException{

			if(!UserHandler.userExists(follower) || !UserHandler.userExists(followed)){
				outStream.writeObject(false);
				return false;
			}
			if(!UserHandler.isFollowing(follower,followed) && !(follower.equals(followed))){
				outStream.writeObject(false);
				return false;
			}
			// validated operation, start sending
			outStream.writeObject(true);

			File photoDir =new File("." + File.separator + "data" + File.separator + followed+ File.separator + "photos");
			String[] photos= photoDir.list();
			File commentsDir =new File("." + File.separator + "data" + File.separator + followed + File.separator + "comments");
			String[] comments = commentsDir.list();

			if(photos != null)
				for(int i = 0; i < photos.length; i++){
					sendFile(outStream, inStream, "." + File.separator + "data" + File.separator + followed+ File.separator + "photos" + File.separator + photos[i]);
				}

			outStream.writeObject("-t");

			if(comments != null)
				for(int i = 0; i < comments.length; i++){
					sendFile(outStream, inStream, "." + File.separator + "data" + File.separator + followed+ File.separator + "comments" + File.separator + comments[i]);
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
		 * @throws CertificateException 
		 * @throws KeyStoreException 
		 * @throws NoSuchPaddingException 
		 * @throws NoSuchAlgorithmException 
		 * @throws UnrecoverableKeyException 
		 * @throws InvalidKeyException 
		 */
		private boolean sendFile(ObjectOutputStream outStream, ObjectInputStream inStream, String file) throws IOException, ClassNotFoundException, InvalidKeyException, UnrecoverableKeyException, NoSuchAlgorithmException, NoSuchPaddingException, KeyStoreException, CertificateException {

			boolean noError = true;

			outStream.writeObject("-p");
			File f = new File(file);
			File fcif = new File(f.getName().replace(".cif", ""));
			File fsize = new File(f.getPath().replace(".cif",".size.cif"));
			int fileSize = CipherAction.getOriginalSize(fsize); //get the previous file size
			String filename = fcif.getName();

			outStream.writeObject(fileSize);
			outStream.writeObject(filename);
			System.out.println("<-- " + fileSize);
			System.out.println(filename);

			noError = (Boolean) inStream.readObject();

			if(noError){
				CipherAction.decipherFile(f, outStream,fileSize);
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
		 * @throws CertificateException 
		 * @throws KeyStoreException 
		 * @throws NoSuchPaddingException 
		 * @throws NoSuchAlgorithmException 
		 * @throws InvalidKeyException 
		 * @throws UnrecoverableKeyException 
		 */
		private void fetchPhotoInfo(ObjectOutputStream outStream, String follower, String followed) throws IOException, ClassNotFoundException, UnrecoverableKeyException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, KeyStoreException, CertificateException{

			if(!(UserHandler.isFollowing(follower,followed)||(follower.equals(followed)))){
				System.out.println("User does not exist or is not followed");
				outStream.writeObject(false);
			}
			else{
				outStream.writeObject(true); // if the the target user is followed or is himself
				File folder = new File("." + File.separator + "data"+ File.separator + followed + File.separator + "photos");

				File[] list = folder.listFiles();
				if(list==null){
					outStream.writeObject("There are no photos!");
					outStream.writeObject(false);
				} else {
				for(int i =0;i<list.length;i++){
					SimpleDateFormat date = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
					outStream.writeObject("Photo Name: " + list[i].getName() + " Upload Date: " + date.format(list[i].lastModified()).replace(".cif",""));
					outStream.writeObject(true);
				}
				outStream.writeObject("No more photos");
				outStream.writeObject(false);
			}
			}
		}

		/**
		 * Send the most recent photo and respective comments of each of the users followed by a user.
		 * @param outStream the ObjectOutputStream used for sending 
		 * @param inStream the ObjectInputStream for validation
		 * @param user the user getting the data
		 * @throws IOException
		 * @throws ClassNotFoundException
		 * @throws CertificateException 
		 * @throws KeyStoreException 
		 * @throws NoSuchPaddingException 
		 * @throws NoSuchAlgorithmException 
		 * @throws UnrecoverableKeyException 
		 * @throws InvalidKeyException 
		 * @throws SignatureException 
		 */
		private void getSubsLatest(ObjectOutputStream outStream, ObjectInputStream inStream, String user) throws IOException, ClassNotFoundException, InvalidKeyException, UnrecoverableKeyException, NoSuchAlgorithmException, NoSuchPaddingException, KeyStoreException, CertificateException, SignatureException {
			File subFile = new File("." + File.separator + "data" + File.separator + user + File.separator + "subscriptions.cif");
			if(!subFile.exists()){
				outStream.writeObject(false);
			} else{
			
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
		}

		/**
		 * Compiles a user's followed users into a list.
		 * @param followingUser the user following other users
		 * @return an ArrayList containing all the users followed by followingUser, null if no users are
		 * followed by followingUser
		 * @throws IOException
		 * @throws ClassNotFoundException 
		 * @throws SignatureException 
		 * @throws CertificateException 
		 * @throws NoSuchAlgorithmException 
		 * @throws KeyStoreException 
		 * @throws InvalidKeyException 
		 * @throws UnrecoverableKeyException 
		 * @throws NoSuchPaddingException 
		 */
		private ArrayList<String> subs(String followingUser) throws IOException, UnrecoverableKeyException, InvalidKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, SignatureException, ClassNotFoundException, NoSuchPaddingException {

			File subFile = new File("." + File.separator + "data" + File.separator + followingUser + File.separator + "subscriptions.cif");
			CipherAction.verifySignature(subFile);
			StringBuilder sb =CipherAction.cipherContent(subFile);
			if(!subFile.exists())
				return null;                                                            
			System.out.println(sb.toString());
			ArrayList<String> subList = new ArrayList<String>(Arrays.asList(sb.toString().split("\n")));


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
				//System.out.println("here");
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
		private String hashPassword(String rawpasswd) throws NoSuchAlgorithmException{
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(rawpasswd.getBytes());
			byte [] digest=md.digest();
			
			//converts digest to a StringBuffer
			StringBuffer sb = new StringBuffer();
			for(int i=0; i<digest.length;i++){
				sb.append(Integer.toString((digest[i] & 0xFF)+ 0x100,16).substring(1));
			}
			return sb.toString();
			
		}
	}
	
}
