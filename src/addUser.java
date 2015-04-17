import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;


/**
 * Class responsible for adding a user and his password to the server's log
 * @author Paulo
 *
 */
public class addUser {

	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		
		String user;
		String passwd;
		Scanner scan = new Scanner(System.in);
		System.out.println("Enter User name:");
		user = scan.next();
		System.out.println("Enter password:");
		passwd=scan.next();
		
		
		File f = new File("." + File.separator + "shadow" + File.separator
				+ "up");
		Path fpath = Paths.get("." + File.separator + "shadow" + File.separator
				+ "up");
		if (!f.exists()) {
			Files.createDirectories(fpath.getParent());
			f.createNewFile();
		}
		BufferedWriter bw = new BufferedWriter( new FileWriter(f,true));

	
		bw.write(user + ":" + hashPassword(passwd) + "\r\n");
		bw.close();
		scan.close();

	}
	//converts a password to a hashed password
	private static String hashPassword(String rawpasswd) throws NoSuchAlgorithmException{
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
