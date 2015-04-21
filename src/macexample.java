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


public class macexample {

	public static void main(String args[]) throws NoSuchAlgorithmException, InvalidKeyException, IOException, ClassNotFoundException{			
		File up = new File("." + File.separator + "shadow" + File.separator + "up"); // ficheiro com pass sem MAC
		File upsha = new File("." + File.separator + "shadow" + File.separator + "up.sha"); // ficheiro onde e guardado o MAC
		Path upp = Paths.get(up.getPath());
		FileWriter fos =new FileWriter(upsha);

		

		byte [] content = Files.readAllBytes(upp);

		Scanner scan = new Scanner(System.in);
		Mac mac=Mac.getInstance("HmacSHA256");

		System.out.println("Please insert password to generate MAC:");
		byte[] pass = (scan.next()).getBytes();
		SecretKey key = new SecretKeySpec(pass,"HmacSHA256");

		mac.init(key);
		mac.update(content);
		byte []getmac =mac.doFinal(); 	
		//checking the MAC in the storage
		System.out.println("mac used for verification :: "+ DatatypeConverter.printBase64Binary(getmac));

		fos.write(DatatypeConverter.printBase64Binary(getmac));	
		fos.close();
		scan.close();
		verifyMac(getmac);

	}
	
	//usar FileWriter e FileReader

	public static void verifyMac(byte []check) throws IOException, NoSuchAlgorithmException, InvalidKeyException, ClassNotFoundException{
		File upsha = new File("." + File.separator + "shadow" + File.separator + "up.sha"); // ficheiro onde e guardado o MAC
		FileReader fr = new FileReader(upsha);
		
		StringBuilder sb = new StringBuilder();
		int ch;
		while((ch=fr.read())!=-1)
			sb.append((char)ch);
		
		System.out.println("mac to check :: " + DatatypeConverter.printBase64Binary(check));
		System.out.println("mac in the storage :: " + sb.toString());
		fr.close();

	}
}
