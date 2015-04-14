import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;


/**
 * Class responsible for adding a user and his password to the server's log
 * @author Paulo
 *
 */
public class addUser {

	public static void main(String[] args) throws IOException {
		String user;
		int hashpwd;
		Scanner scan = new Scanner(System.in);
		System.out.println("Enter User name:");
		user = scan.next();
		System.out.println("Enter password:");
		hashpwd = (scan.next()).hashCode();
		File f = new File("." + File.separator + "shadow" + File.separator
				+ "up");
		Path fpath = Paths.get("." + File.separator + "shadow" + File.separator
				+ "up");
		if (!f.exists()) {
			Files.createDirectories(fpath.getParent());
			f.createNewFile();
		}
		BufferedWriter bw = new BufferedWriter( new FileWriter(f,true));

		bw.write(user + ":" + hashpwd + "\r\n");
		bw.close();
		scan.close();

	}
}
