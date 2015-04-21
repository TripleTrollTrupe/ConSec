import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;


/**
 * Class responsible for adding a user and his password to the server's log
 * @author Paulo
 *
 */
public class addUser {

	public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InvalidKeyException, IllegalStateException {
		File up = new File("." + File.separator + "shadow" + File.separator
				+ "up");
		Path upp = Paths.get(up.getPath());
		File upsha = new File("." + File.separator + "shadow" + File.separator
				+ "up.sha");
		Scanner scan = new Scanner(System.in);

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
			
		
		String user;
		String passwd;
		
		System.out.println("Enter User name:");
		user = scan.next();
		System.out.println("Enter password:");
		passwd=scan.next();
		
		

		BufferedWriter bw = new BufferedWriter( new FileWriter(up,true));

	
		bw.write(user + ":" + hashPassword(passwd) + "\r\n");
		System.out.println(user +" has been added to the registration file!");
		bw.close();
		generateNewMac(scan,up,upsha);
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
	
	public static boolean validateMac(Scanner scan, File up, File upsha) throws NoSuchAlgorithmException, InvalidKeyException, IllegalStateException, IOException{
	
		Mac mac =Mac.getInstance("HmacSHA256");
		System.out.println("Please insert password to validate MAC: ");
		byte[] pass= ((scan.next()).getBytes());
		SecretKey key = new SecretKeySpec(pass,"HmacSHA256");
		mac.init(key);	
		mac.update(getFileBytes(up));
		byte []code = mac.doFinal();
		String generatedCode= DatatypeConverter.printBase64Binary(code);
		String storedCode = getFileString(upsha);
		return generatedCode.equals(storedCode);
			
	}
	
	private static byte [] getFileBytes(File f) throws IOException{
		Path fp=Paths.get(f.getPath());
		byte [] data = Files.readAllBytes(fp);
		return data;
	}
	
	private static String getFileString(File f) throws IOException{
		FileReader fr = new FileReader(f);
		
		StringBuilder sb = new StringBuilder();
		int ch;
		while((ch=fr.read())!=-1)
			sb.append((char)ch);
		fr.close();
		return sb.toString();
	}
	
	private static void generateNewMac(Scanner scan,File up, File upsha) throws NoSuchAlgorithmException, InvalidKeyException, IllegalStateException, IOException{
		FileWriter fw = new FileWriter(upsha);
		Mac mac= Mac.getInstance("HmacSHA256");
		System.out.println("Please insert password to generate a new MAC: ");
		byte [] pass = ((scan.next().getBytes()));
		SecretKey key=new SecretKeySpec(pass,"HmacSHA256");
		mac.init(key);
		mac.update(getFileBytes(up));
		byte [] genmac = mac.doFinal();
		fw.write(DatatypeConverter.printBase64Binary(genmac));
		fw.close();
		System.out.println("A new MAC has been generated!");	
	}
	
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
