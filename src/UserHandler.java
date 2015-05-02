import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;




/** Class that handles user related operations
 * @author Paulo
 *
 */
public class UserHandler {

	/**
	 * Sets a user to subscribe to another user.
	 * @param subscribingUser the user that is to subscribe
	 * @param subscribedUser the user that is to be subscribed to
	 * @return true if subscribingUser is set to subscribe subscribingUser successfully, false otherwise
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 * @throws SignatureException 
	 * @throws CertificateException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyStoreException 
	 * @throws InvalidKeyException 
	 * @throws UnrecoverableKeyException 
	 */
	public static boolean subscribe(String subscribingUser, String subscribedUser) throws IOException, UnrecoverableKeyException, InvalidKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, SignatureException, ClassNotFoundException {

		if(!userExists(subscribingUser) || isSubscribed(subscribingUser,subscribedUser))
			return false;

		File f = new File("." + File.separator + "data" + File.separator + subscribingUser + File.separator + "subscriptions");
		Path fpath = Paths.get("." + File.separator + "data" + File.separator + subscribingUser + File.separator + "subscriptions");

		if(!f.exists()){
			Files.createDirectories(fpath.getParent());
			f.createNewFile();
		} else {
			CipherAction.verifySignature(f);
		}
		
		BufferedWriter bw = new BufferedWriter( new FileWriter(f,true));
		bw.write(subscribedUser + "\r\n");
		bw.newLine();
		bw.flush();
		bw.close();
		CipherAction.generateSignature(f);
		return true;
	}

	/**
	 * Checks if a user exists.
	 * @param userID the user to check
	 * @return true if the user is registered in the system, false otherwise
	 * @throws IOException 
	 */
	public static boolean userExists(String userID) throws IOException{

		File up = new File("." + File.separator + "shadow" + File.separator + "up");
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(up)));
		boolean exists = false;

		if (userID.length() != 0){
			String line;
			while((line = br.readLine()) != null && !exists){
				exists = line.startsWith(userID + ":");
			}
		}
		br.close();

		return exists;
	}
	
	/**
	 * Checks if a user is subscribed to another user.
	 * @param subscribingUser the subscribing user
	 * @param subscribedUser the subscribed user
	 * @return true if the subscribingUser is subscribed to the subscribedUser, false otherwise
	 * @throws IOException
	 */
	public static boolean isSubscribed(String subscribingUser, String subscribedUser) throws IOException {

		File f = new File("." + File.separator + "data" + File.separator + subscribingUser + File.separator + "subscriptions");

		if(!f.exists())
			return false;

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		boolean isSubscribed = false;

		String line;
		while((line = br.readLine()) != null && !isSubscribed){
			isSubscribed = (subscribedUser).equals(line);
		}
		br.close();

		return isSubscribed;
	}
		
	/** Method that adds a user to another user's follower list
	 * @param followingUser - the user that will be set as a follower
	 * @param followedUser  - the user that will be followed
	 * @return true if the followingUser is set as a follower of the followedUser, false otherwise
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 * @throws SignatureException 
	 * @throws CertificateException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyStoreException 
	 * @throws InvalidKeyException 
	 * @throws UnrecoverableKeyException 
	 */
	public static boolean follow(String followingUser, String followedUser) throws IOException, UnrecoverableKeyException, InvalidKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, SignatureException, ClassNotFoundException{
		if(!userExists(followingUser) || isFollowing(followingUser,followedUser))
			return false;
		
		File f = new File("." + File.separator + "data" + File.separator + followedUser + File.separator + "followers");
		Path fpath = Paths.get("." + File.separator + "data" + File.separator + followedUser + File.separator + "followers");
		
		if(!f.exists()){
			Files.createDirectories(fpath.getParent());
			f.createNewFile();
		} else {
			CipherAction.verifySignature(f);
		}
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(f,true));
		bw.write(followingUser + "\r\n");
		bw.newLine();
		bw.flush();
		bw.close();
		CipherAction.generateSignature(f);
		subscribe(followingUser,followedUser);
		return true;
	}

	/** Checks is a user is already following another user
	 * @param followingUser - user to check if he is following followedUser
	 * @param followedUser  - user to check if he is followed by followingUser
	 * @return true if followingUser is a part of followedUser's follower list
	 * @throws IOException
	 */
	public static boolean isFollowing(String followingUser, String followedUser) throws IOException {
		File f = new File("." + File.separator + "data" + File.separator + followedUser + File.separator + "followers");
		
		if(!f.exists())
			return false;
		
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		boolean follows = false;
		
		String line;
		while((line = br.readLine()) !=null && !follows){
			follows = (followingUser).equals(line);
		}
		br.close();
		
		return follows;
		
	}
	

}
