import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;


public class Auth {
	/** checks if a given MAC is valid
	 * @param scan -scanner used to receive password to generate new MAC
	 * @param up   -file used to generate MAC
	 * @param upsha -file with previous mAC
	 * @return true - if the MAC is valid, false otherwise
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public static boolean validateMac(Scanner scan, File up, File upsha) throws NoSuchAlgorithmException, InvalidKeyException, IllegalStateException, IOException{
		
		Mac mac =Mac.getInstance("HmacSHA256");
		System.out.println("Please insert password to validate MAC: ");
		byte[] pass= ((scan.next()).getBytes());
		SecretKey key = new SecretKeySpec(pass,"AES"); // I think we're supposed to use AES
		mac.init(key);	
		mac.update(getFileBytes(up));
		byte []code = mac.doFinal();
		String generatedCode= DatatypeConverter.printBase64Binary(code);
		String storedCode = getFileString(upsha);
		return generatedCode.equals(storedCode);
			
	}
	
	/** Gets the bytes from a given file
	 * @param f - file to get the bytes from
	 * @return byte array with the bytes from f
	 * @throws IOException
	 */
	private static byte [] getFileBytes(File f) throws IOException{
		Path fp=Paths.get(f.getPath());
		byte [] data = Files.readAllBytes(fp);
		return data;
	}
	
	/**Converts the content of a file into a String
	 * @param f - file to the the String from
	 * @return String correspondent to the file's content
	 * @throws IOException
	 */
	public static String getFileString(File f) throws IOException{
		FileReader fr = new FileReader(f);
		
		StringBuilder sb = new StringBuilder();
		int ch;
		while((ch=fr.read())!=-1)
			sb.append((char)ch);
		fr.close();
		return sb.toString();
	}
	
	/**Generates a new MAC to protect a file
	 * @param scan - scanner used to scan the password for MAC generation
	 * @param up   - File to be protected by the MAC
	 * @param upsha- File where the MAC will be saved
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public static void generateNewMac(Scanner scan,File up, File upsha) throws NoSuchAlgorithmException, InvalidKeyException, IllegalStateException, IOException{
		FileWriter fw = new FileWriter(upsha);
		Mac mac= Mac.getInstance("HmacSHA256");
		System.out.println("Please insert password to generate a new MAC: ");
		byte [] pass = ((scan.next().getBytes()));
		SecretKey key=new SecretKeySpec(pass,"AES"); // I think we're supposed to use AES
		mac.init(key);
		mac.update(getFileBytes(up));
		byte [] genmac = mac.doFinal();
		fw.write(DatatypeConverter.printBase64Binary(genmac));
		fw.close();
		System.out.println("A new MAC has been generated!");	
	}
	
	/** Procedure used by the server to check if MAC exists, if it's valid and create one if not existing
	 * @param scan - scanner used to check and/or receive password
	 * @param up   - file where the data is stored and used to generate a MAC
	 * @param upsha - file where the MAC protecting up is, or will be saved
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public static void initialCheck(Scanner scan, File up, File upsha) throws InvalidKeyException, NoSuchAlgorithmException, IllegalStateException, IOException{
		Path upp = Paths.get(up.getPath());
		if(up.exists() && upsha.exists() && !validateMac(scan,up,upsha)){
			System.out.println("The generated MAC code does not correpond to the code stored! Shutting Down!");
			System.exit(0);
		}
		
		if (!up.exists()) {
			System.out.println("Registration File does not exist, creating a new one....");
			Files.createDirectories(upp.getParent());
			up.createNewFile();
		}
		if(!upsha.exists()){
			System.out.println("Registration File is not protected by a MAC, the server will shutdown if it is not generated now!");
			System.out.println("Do you want to generate a MAC to protect the file? y/n");
			String answer = scan.next();
			while(!answer.equals("y") && !answer.equals("n"))
				answer=scan.next();
			if(answer.equals("y")){
				generateNewMac(scan,up,upsha);
			}
			else {
				System.out.println("Shutting down server...");
				System.exit(0);
			}
		}
	}
}
