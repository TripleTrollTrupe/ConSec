import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
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

public class CipherAction {

	/** This method is not necessary and is only used for testing with ciphers
	 * @throws SignatureException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String args[]) throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, IOException, UnrecoverableKeyException, KeyStoreException, CertificateException, IllegalBlockSizeException, BadPaddingException, SignatureException, ClassNotFoundException {
	//	System.out.println(getPrivateKey());
	//	System.out.println(getPublicKey());
	//	File f=new File("."+File.separator+"data"+File.separator+"getpics"+File.separator+"subscriptions");
	//	System.out.println(cipherContent(f).toString());
	//	StringBuilder sb=cipherContent(f);
	//	addToCipher(f,"leldone");
	//	System.out.println(cipherContent(f).toString());
	}
	// Cipher might be done while transferring no need to save the whole file beforehand
	public static void cipherFile(File f, int size, ObjectInputStream in) throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException, IOException, UnrecoverableKeyException, KeyStoreException, CertificateException, IllegalBlockSizeException, BadPaddingException {
		
		KeyGenerator kg = KeyGenerator.getInstance("AES"); //creates an instance of AES algorythm
		kg.init(128); //creates a key with 128 bytes
		SecretKey sessionkey = kg.generateKey(); //generates a random key
		PublicKey publickey = getPublicKey(); //gets public key from keystore (check aux method)
		
		Cipher c = Cipher.getInstance("AES"); //Used to cipher with randomly generated symettric key
		c.init(Cipher.ENCRYPT_MODE, sessionkey);
			

		FileOutputStream fos;
		CipherOutputStream cos;
		
		if(!f.exists()){
			Files.createDirectories(Paths.get(f.getParent()));
		}
		
		fos = new FileOutputStream(f.getPath()); //stream where the ciphered file will be written
	
		cos = new CipherOutputStream(fos,c);
		byte[] bytebuf = new byte[16];
		int bytesRead=0;
		while((bytesRead=in.read(bytebuf))!=-1){
			cos.write(bytebuf,0,bytesRead);
		}
		
	
		cos.close();
		
		c = Cipher.getInstance("RSA"); //Cipher used to cipher with servers public key
		c.init(Cipher.WRAP_MODE, publickey);
		byte[] keyWrapped = c.wrap(sessionkey); //secret key wrapped with public key
		File keydir = new File("."+File.separator +"keys"+File.separator +f.getParentFile()); //directory to store key
		if(!keydir.exists()){
			Files.createDirectories(Paths.get(keydir.getPath())); // creates directory to store key if are not be exists
		}
		FileOutputStream kos = new FileOutputStream("keys"+File.separator +f.getPath().replace("cif",".key"));//file where the key will be stored
		ObjectOutputStream oos = new ObjectOutputStream(kos);
		oos.write(keyWrapped); //writes down the wrapped key
		oos.close();
		
		System.out.println("Cipher Operation Concluded!");

	}
	
	public static void decipherFile(File f,ObjectOutputStream out, int filesize) throws IOException,
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
		byte[] bytebuf = new byte[16];
		int bytesRead=0;

		while((bytesRead=cis.read(bytebuf))!=-1){
			out.write(bytebuf,0,bytesRead);
		}

		cis.close();
		fiscif.close();
		oiskey.close();
		
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
		fis.close();
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
		
		if(!f.exists()){
			Files.createDirectories(Paths.get(f.getParent()));
		}
		
		
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
		System.out.println("Comment Cipher Operation Concluded!");
	}
	
	public static void cypherSize(int size, File f) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, UnrecoverableKeyException, KeyStoreException, CertificateException, IllegalBlockSizeException{
		KeyGenerator kg = KeyGenerator.getInstance("AES"); //creates an instance of AES algorythm
		kg.init(128); //creates a key with 128 bytes
		SecretKey sessionkey = kg.generateKey(); //generates a random key
		PublicKey publickey = getPublicKey(); //gets public key from keystore (check aux method)
		
		Cipher c = Cipher.getInstance("AES"); //Used to cipher with randomly generated symettric key
		c.init(Cipher.ENCRYPT_MODE, sessionkey);
			

		FileOutputStream fos;
		CipherOutputStream cos;
		//creates directory to store sizes if not existing
		File sizedir = new File("."+File.separator +"sizes"+File.separator +f.getParentFile()); //directory to store key
		if(!sizedir.exists()){
			Files.createDirectories(Paths.get(sizedir.getPath())); // creates directory to store key if are not be exists
		}
		
		fos = new FileOutputStream("."+File.separator +"sizes" + File.separator +f.getPath() + ".size.cif"); //stream where the ciphered file will be written
			
		cos = new CipherOutputStream(fos, c);
		byte[] b = ByteBuffer.allocate(4).putInt(size).array();

		cos.write(b, 0, b.length); //writes to the cipher file
		
		
		c = Cipher.getInstance("RSA"); //Cipher used to cipher with servers public key
		c.init(Cipher.WRAP_MODE, publickey);
		byte[] keyWrapped = c.wrap(sessionkey); //secret key wrapped with public key
		File keydir = new File("."+File.separator +"keys"+File.separator + "sizes" + File.separator +f.getParentFile()); //directory to store key
		if(!keydir.exists()){
			Files.createDirectories(Paths.get(keydir.getPath())); // creates directory to store key if are not be exists
		}
		FileOutputStream kos = new FileOutputStream("keys"+File.separator +"sizes" +File.separator+f.getPath() + ".size.key");//file where the key will be stored
		ObjectOutputStream oos = new ObjectOutputStream(kos);
		oos.write(keyWrapped); //writes down the wrapped key
		oos.close();
		cos.close();
		System.out.println("Size Cipher Operation Concluded!");
	}
	
	//need to think of way to do this
	public static int getOriginalSize(File f) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnrecoverableKeyException, KeyStoreException, CertificateException{
		File fkey = new File("sizes"+f.getPath()+".size.key");
		File skey = new File("keys"+File.separator+fkey.getPath());
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

		FileInputStream fiscif = new FileInputStream("sizes"+File.separator+f.getPath()+".size.cif");
		
		CipherInputStream cis = new CipherInputStream(fiscif, c);	
		byte []size = new byte [4];
		cis.read(size);
		cis.close();
		return ByteBuffer.wrap(size).getInt();
	
	}
	
	
	public static void generateSignature(File f) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, SignatureException, InvalidKeyException, NoSuchPaddingException{
		FileInputStream fis= new FileInputStream(f);
		//ObjectInputStream ois= new ObjectInputStream(fis);
		FileOutputStream sigfos= new FileOutputStream(f.getPath().replace(".cif",".sig"));
		ObjectOutputStream sigoos = new ObjectOutputStream(sigfos);
		PrivateKey privatekey=getPrivateKey();
		Signature s = Signature.getInstance("MD5withRSA");
		s.initSign(privatekey);
		//byte buf[]=new byte[16];
		//while((fis.read(buf,0,16))>0){
		s.update(cipherContent(f).toString().getBytes());
		//}
		sigoos.write(s.sign());
		sigoos.close();
		fis.close();
		sigfos.close();
		System.out.println("Generated a new signature for:" +f.getPath());
	}
	
	public static void verifySignature(File f) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, InvalidKeyException, SignatureException, ClassNotFoundException, NoSuchPaddingException{		
	//	FileInputStream fis = new FileInputStream(f);
		File fsig = new File(f.getPath().replace(".cif",".sig"));
		System.out.println(fsig.getPath());
		//Path fsigp = Paths.get(fsig.getPath());
		FileInputStream fissig = new FileInputStream(fsig);
		ObjectInputStream oissig = new ObjectInputStream(fissig);
		byte sig[] = new byte [256];
		System.out.println("signature is here"+fsig.getPath());
		oissig.read(sig); // ver melhor como ler assinatura
		fissig.close();
		oissig.close();
		PublicKey publickey=getPublicKey(); 
		Signature s = Signature.getInstance("MD5withRSA");
		
		s.initVerify(publickey);
		//byte buf[]=new byte[16];
		//while((fis.read(buf,0,16))>0){
		s.update(cipherContent(f).toString().getBytes());
		//}
		if (s.verify(sig)){
			System.out.println("Signature Valid");
		} else{
			System.out.println("Signature Invalid");
		}
	//	fis.close();
	}
	
	//return a StringBuilder with the ciphered file's content
	public static StringBuilder cipherContent(File f) throws NoSuchAlgorithmException, NoSuchPaddingException, UnrecoverableKeyException, KeyStoreException, CertificateException, IOException, InvalidKeyException{
		PrivateKey privatekey=getPrivateKey();
		StringBuilder sb = new StringBuilder();
		FileInputStream fiscif = new FileInputStream(f);
		ObjectInputStream oiscif = new ObjectInputStream(fiscif);
		
		Cipher c = Cipher.getInstance("RSA");
		c.init(Cipher.UNWRAP_MODE, privatekey);
		

		
		FileInputStream fiskey = new FileInputStream("keys"+File.separator+f.getPath().replace(".cif",".key"));
		ObjectInputStream oiskey = new ObjectInputStream(fiskey);
		byte[] keyEncoded = new byte[256]; //the key's size after wrapping
		oiskey.read(keyEncoded); //gets key from file
		oiskey.close();
		Key unwrappedKey = c.unwrap(keyEncoded, "AES", Cipher.SECRET_KEY); //unwraps the AES key inside
		
		c=Cipher.getInstance("AES");
		c.init(Cipher.DECRYPT_MODE, unwrappedKey); // SecretKeySpec é subclasse de		

		CipherInputStream cis = new CipherInputStream(oiscif,c);
		
		byte[] bytebuf = new byte[1]; //so we don't have to deal with padding on the stringbuilder 
	
		while((cis.read(bytebuf))!=-1){
			System.out.println(sb.toString());
			sb.append(new String (bytebuf));
		}

		oiscif.close();
		cis.close();
		return sb;
	}
	
	public static void cipherFile(File f) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnrecoverableKeyException, KeyStoreException, CertificateException, IllegalBlockSizeException{
		KeyGenerator kg = KeyGenerator.getInstance("AES"); //creates an instance of AES algorythm
		kg.init(128); //creates a key with 128 bytes
		SecretKey sessionkey = kg.generateKey(); //generates a random key
		PublicKey publickey = getPublicKey(); //gets public key from keystore (check aux method)
		
		Cipher c = Cipher.getInstance("AES"); //Used to cipher with randomly generated symettric key
		c.init(Cipher.ENCRYPT_MODE, sessionkey);
			

		FileOutputStream fos;
		CipherOutputStream cos;
		FileInputStream fis = new FileInputStream(f);
		if(!f.exists()){
			Files.createDirectories(Paths.get(f.getParent()));
		}
		
		fos = new FileOutputStream(f.getPath()); //stream where the ciphered file will be written
		ObjectOutputStream oos= new ObjectOutputStream(fos);
		cos = new CipherOutputStream(oos,c);
		byte[] bytebuf = new byte[16];
		int bytesRead=0;
		while((bytesRead=fis.read(bytebuf))!=-1){
			cos.write(bytebuf,0,bytesRead);
		}
		fis.close();
		
	
		cos.close();
		
		c = Cipher.getInstance("RSA"); //Cipher used to cipher with servers public key
		c.init(Cipher.WRAP_MODE, publickey);
		byte[] keyWrapped = c.wrap(sessionkey); //secret key wrapped with public key
		File keydir = new File("."+File.separator +"keys"+File.separator +f.getParentFile()); //directory to store key
		if(!keydir.exists()){
			Files.createDirectories(Paths.get(keydir.getPath())); // creates directory to store key if are not be exists
		}
		FileOutputStream kos = new FileOutputStream("keys"+File.separator +f.getPath().replace(".cif", ".key"));//file where the key will be stored
		ObjectOutputStream koos = new ObjectOutputStream(kos);
		koos.write(keyWrapped); //writes down the wrapped key
		koos.close();
		
		System.out.println("Cipher Operation Concluded!");
		
	}
	
	private static void addToCipherAux(StringBuilder sb, String s, File f) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, NoSuchPaddingException, InvalidKeyException{
		sb.append("\n"+s);
		String toAdd = sb.toString();
		File fkey = new File(f.getPath().replace(".cif",".key"));
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
		c.init(Cipher.ENCRYPT_MODE, unwrappedKey); // SecretKeySpec é subclasse de
		FileOutputStream fos= new FileOutputStream(f);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		CipherOutputStream cos = new CipherOutputStream(oos,c);
		cos.write(toAdd.getBytes());
		cos.close();
	}
	
	public static void addToCipher(File f,String s) throws UnrecoverableKeyException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, KeyStoreException, CertificateException, IOException{
		StringBuilder content= cipherContent(f);
		addToCipherAux(content,s,f);
	}
	
	public static void startingCipher(File f, String s) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, UnrecoverableKeyException, KeyStoreException, CertificateException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
		KeyGenerator kg = KeyGenerator.getInstance("AES"); //creates an instance of AES algorythm
		kg.init(128); //creates a key with 128 bytes
		SecretKey sessionkey = kg.generateKey(); //generates a random key
		PublicKey publickey = getPublicKey(); //gets public key from keystore (check aux method)
		
		Cipher c = Cipher.getInstance("AES"); //Used to cipher with randomly generated symettric key
		c.init(Cipher.ENCRYPT_MODE, sessionkey);
			

		FileOutputStream fos;
		CipherOutputStream cos;
		FileInputStream fis = new FileInputStream(f);
		if(!f.exists()){
			Files.createDirectories(Paths.get(f.getParent()));
		}
		
		fos = new FileOutputStream(f.getPath()); //stream where the ciphered file will be written
		ObjectOutputStream oos= new ObjectOutputStream(fos);
		cos = new CipherOutputStream(oos,c);
		byte[] bytebuf = (s.getBytes("UTF-8"));
		cos.write(bytebuf);
	
		fis.close();
		
	
		cos.close();
		
		c = Cipher.getInstance("RSA"); //Cipher used to cipher with servers public key
		c.init(Cipher.WRAP_MODE, publickey);
		byte[] keyWrapped = c.wrap(sessionkey); //secret key wrapped with public key
		File keydir = new File("."+File.separator +"keys"+File.separator +f.getParentFile()); //directory to store key
		if(!keydir.exists()){
			Files.createDirectories(Paths.get(keydir.getPath())); // creates directory to store key if are not be exists
		}
		FileOutputStream kos = new FileOutputStream("keys"+File.separator +f.getPath().replace(".cif", ".key"));//file where the key will be stored
		ObjectOutputStream koos = new ObjectOutputStream(kos);
		koos.write(keyWrapped); //writes down the wrapped key
		koos.close();
		
		System.out.println("Cipher Operation Concluded!");
	}
	

}
