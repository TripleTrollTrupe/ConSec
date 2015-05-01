import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class CypherAction {

	/** This method is not necessary and is only used for testing with cyphers
	 */
	public static void main(String args[]) throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, IOException, UnrecoverableKeyException, KeyStoreException, CertificateException, IllegalBlockSizeException, BadPaddingException {
		System.out.println(getPrivateKey());
		System.out.println(getPublicKey());
		//File f=new File("."+File.separator+"test"+File.separator+"test.jpg");
	//	cypherFile(f);
		//decypherFile(f);
	}
	// Cipher might be done while transferring no need to save the whole file beforehand
	public static void cypherFile(File f, int size, ObjectInputStream in) throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException, IOException, UnrecoverableKeyException, KeyStoreException, CertificateException, IllegalBlockSizeException, BadPaddingException {
		
		KeyGenerator kg = KeyGenerator.getInstance("AES"); //creates an instance of AES algorythm
		kg.init(128); //creates a key with 128 bytes
		SecretKey sessionkey = kg.generateKey(); //generates a random key
		PublicKey publickey = getPublicKey(); //gets public key from keystore (check aux method)
		
		Cipher c = Cipher.getInstance("AES"); //Used to cipher with randomly generated symettric key
		c.init(Cipher.ENCRYPT_MODE, sessionkey);
			

		FileOutputStream fos;
		CipherOutputStream cos;
		
		fos = new FileOutputStream(f.getPath() + ".cif"); //stream where the ciphered file will be written
			
		cos = new CipherOutputStream(fos, c);
		byte[] b = new byte[16];
		int i;
			while ((i = in.read(b)) != -1) {//reads from the stream
				cos.write(b, 0, i); //writes to the cipher file
		}
		
		c = Cipher.getInstance("RSA"); //Cipher used to cipher with servers public key
		c.init(Cipher.WRAP_MODE, publickey);
		byte[] keyWrapped = c.wrap(sessionkey); //secret key wrapped with public key
		File keydir = new File("."+File.separator +"keys"+File.separator +f.getParentFile()); //directory to store key
		if(!keydir.exists()){
			Files.createDirectories(Paths.get(keydir.getPath())); // creates directory to store key if are not be exists
		}
		FileOutputStream kos = new FileOutputStream("keys"+File.separator +f.getPath() + ".key");//file where the key will be stored
		ObjectOutputStream oos = new ObjectOutputStream(kos);
		oos.write(keyWrapped); //writes down the wrapped key
		oos.close();
		cos.close();
		System.out.println("Cipher Operation Concluded!");
	}
	public static void decypherFile(File f,ObjectOutputStream out, int filesize) throws IOException,
			InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, UnrecoverableKeyException, KeyStoreException, CertificateException {

		File fkey = new File(f.getPath().replace(".cif", ".key"));
		File skey = new File("keys"+File.separator+fkey);
		FileInputStream fiskey = new FileInputStream(skey);
		
		PrivateKey privateKey=getPrivateKey(); //gets privateKey !!check if working properly !!

		
		
		Cipher c = Cipher.getInstance("RSA");
		c.init(Cipher.UNWRAP_MODE, privateKey);
		
		ObjectInputStream oiskey = new ObjectInputStream(fiskey);
		byte[] keyEncoded = new byte[256]; //the key's size after wrapping
		oiskey.read(keyEncoded); //gets key from file
		oiskey.close();
		Key unwrappedKey = c.unwrap(keyEncoded, "AES", Cipher.SECRET_KEY); //unwraps the AES key inside
		
		c=Cipher.getInstance("AES");
		c.init(Cipher.DECRYPT_MODE, unwrappedKey); // SecretKeySpec é subclasse de		

		FileInputStream fiscif = new FileInputStream(f.getPath());
		
		CipherInputStream cis = new CipherInputStream(fiscif, c);
		byte[] bytebuf = new byte[1024];
		int n;
		while ((n=cis.read(bytebuf,0,1024))>0) {//reads cipher file
			out.write(bytebuf, 0, n); //writes to the stream
		}

		cis.close();
		fiscif.close();
		oiskey.close();
		
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
	}

	/*method that generates keys for the server, to be used exclusively within the server! 
	//it's probably becoming obsolete because of keytool usage
	public static void generateKeys() throws NoSuchAlgorithmException,
			IOException {

		File pubkey = new File("." + File.separator + "keys" + File.separator
				+ "public.key");
		File privkey = new File("." + File.separator + "keys" + File.separator
				+ "private.key");
		System.out.println("Generating public and private keys...");
		// creates file to store public key and directories

		Files.createDirectories(Paths.get(pubkey.getPath()).getParent());
		pubkey.createNewFile();

		// creates file to store private key and directories

		Files.createDirectories(Paths.get(privkey.getPath()).getParent());
		privkey.createNewFile();

		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				pubkey));
		ObjectOutputStream oos2 = new ObjectOutputStream(new FileOutputStream(
				privkey));
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(1024);
		KeyPair kp = kpg.generateKeyPair();

		PublicKey ku = kp.getPublic();
		byte[] kuEncoded = ku.getEncoded();
		oos.write(kuEncoded);
		oos.close();
		PrivateKey kr = kp.getPrivate();
		byte[] krEncoded = kr.getEncoded();
		oos2.write(krEncoded);
		oos2.close();
		System.out.println("Public and Private Keys have been generated!");
	}*/
	//can be used to check existence of keystorage
	public static boolean existsKeyStorage() {
		File keystorage = new File("." + File.separator + "keytool"+File.separator+"serverkeystore.jck");
		return keystorage.exists();
	}
	
	private static PrivateKey getPrivateKey() throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, IOException, CertificateException{
		
		File keystorage = new File("." + File.separator + "keytool"+File.separator+"serverkeystore.jks");
		FileInputStream fis= new FileInputStream(keystorage);
		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		keystore.load(fis, "requiem".toCharArray());
		String alias = "server";   //alias used to register keystore
		Key key = keystore.getKey(alias, "requiem".toCharArray()); //password used on keystore
		fis.close();
		return (PrivateKey) key;
	}
	
	private static PublicKey getPublicKey() throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, CertificateException, IOException{
		File keystorage = new File("." + File.separator + "keytool"+File.separator+"serverkeystore.jks");
		FileInputStream fis= new FileInputStream(keystorage);
		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		keystore.load(fis, "requiem".toCharArray());
		String alias = "server";   //alias used to register keystore
		PublicKey publickey = keystore.getCertificate(alias).getPublicKey();
		return publickey;
	}
	
	public static void cipherComment(String comment, File f) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnrecoverableKeyException, KeyStoreException, CertificateException, IOException, IllegalBlockSizeException{
		
		KeyGenerator kg = KeyGenerator.getInstance("AES"); //creates an instance of AES algorythm
		kg.init(128); //creates a key with 128 bytes
		SecretKey sessionkey = kg.generateKey(); //generates a random key
		PublicKey publickey = getPublicKey(); //gets public key from keystore (check aux method)
		
		Cipher c = Cipher.getInstance("AES"); //Used to cipher with randomly generated symettric key
		c.init(Cipher.ENCRYPT_MODE, sessionkey);
			

		FileOutputStream fos;
		CipherOutputStream cos;
		
		fos = new FileOutputStream(f.getPath() + ".cif"); //stream where the ciphered file will be written
			
		cos = new CipherOutputStream(fos, c);
		byte[] b = comment.getBytes();

		cos.write(b, 0, b.length); //writes to the cipher file
		
		
		c = Cipher.getInstance("RSA"); //Cipher used to cipher with servers public key
		c.init(Cipher.WRAP_MODE, publickey);
		byte[] keyWrapped = c.wrap(sessionkey); //secret key wrapped with public key
		File keydir = new File("."+File.separator +"keys"+File.separator +f.getParentFile()); //directory to store key
		if(!keydir.exists()){
			Files.createDirectories(Paths.get(keydir.getPath())); // creates directory to store key if are not be exists
		}
		FileOutputStream kos = new FileOutputStream("keys"+File.separator +f.getPath() + ".key");//file where the key will be stored
		ObjectOutputStream oos = new ObjectOutputStream(kos);
		oos.write(keyWrapped); //writes down the wrapped key
		oos.close();
		cos.close();
		System.out.println("Cipher Operation Concluded!");
	}


	public static void decipherComment(ObjectInputStream in){
		
	}

}
