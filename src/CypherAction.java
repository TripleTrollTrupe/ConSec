import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import javax.xml.bind.DatatypeConverter;

public class CypherAction {

	/** This method is not necessary and is only used for testing with cyphers
	 */
	public static void main(String args[]) throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, IOException, UnrecoverableKeyException, KeyStoreException, CertificateException, IllegalBlockSizeException, BadPaddingException {
		System.out.println(getPrivateKey());
		System.out.println(getPublicKey());
		File f=new File("."+File.separator+"test"+File.separator+"test.txt");
		cypherFile(f);
		decypherFile(f);
	}

	public static void cypherFile(File f) throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException, IOException, UnrecoverableKeyException, KeyStoreException, CertificateException, IllegalBlockSizeException, BadPaddingException {
		KeyGenerator kg = KeyGenerator.getInstance("AES"); //creates an instance of AES algorythm
		kg.init(128); //creates a key with 128 bytes
		SecretKey sessionkey = kg.generateKey(); //generates a random key
		PublicKey publickey = getPublicKey();
		String testkey = DatatypeConverter.printBase64Binary(publickey.getEncoded());
		System.out.println(testkey.length());
		
		Cipher c = Cipher.getInstance("AES"); //Used to cipher with randomly generated symettric key
		c.init(Cipher.ENCRYPT_MODE, sessionkey);
		//byte [] secretkey=c.doFinal(sessionkey.getEncoded()); gets secret key in byte array form 
			

		FileInputStream fis;
		FileOutputStream fos;
		CipherOutputStream cos;

		fis = new FileInputStream(f);
		fos = new FileOutputStream(f.getPath() + ".cif");

		cos = new CipherOutputStream(fos, c);
		byte[] b = new byte[16];
		int i;
		while ((i = fis.read(b)) != -1) {
			cos.write(b, 0, i);
		}
		c = Cipher.getInstance("RSA"); //Cipher used to cipher with servers public key
		c.init(Cipher.WRAP_MODE, publickey);
		byte[] keyWrapped = c.wrap(sessionkey);
		System.out.println(keyWrapped.length);
		FileOutputStream kos = new FileOutputStream(f.getPath() + ".key");
		ObjectOutputStream oos = new ObjectOutputStream(kos);
		oos.write(keyWrapped);
		oos.close();
		fis.close();
		cos.close();
		System.out.println("Cipher Operation Concluded!");
	}

	public static void decypherFile(File f) throws IOException,
			InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, UnrecoverableKeyException, KeyStoreException, CertificateException {

		FileInputStream fis = new FileInputStream(f.getPath() + ".key");
		
		PrivateKey privateKey=getPrivateKey(); //gets privateKey !!check if working properly !!
		String testkey= DatatypeConverter.printBase64Binary(privateKey.getEncoded());
		System.out.println("private key size: "+ testkey.length());
		//Key unwrappedKey = c.unwrap(wrappedKey, "DESede", Cipher.SECRET_KEY);
		
		
		Cipher c = Cipher.getInstance("RSA");
		c.init(Cipher.UNWRAP_MODE, privateKey);
		
		ObjectInputStream ois = new ObjectInputStream(fis);
		byte[] keyEncoded = new byte[256]; //this size probably because of wrapping
		ois.read(keyEncoded); //gets key from file
		ois.close();
		Key unwrappedKey = c.unwrap(keyEncoded, "AES", Cipher.SECRET_KEY); //unwraps the AES key inside
		
		c=Cipher.getInstance("AES");
		c.init(Cipher.DECRYPT_MODE, unwrappedKey); // SecretKeySpec é subclasse de

		

		FileInputStream fin = new FileInputStream(f.getPath() + ".cif");
		FileOutputStream fout = new FileOutputStream(f.getPath());

		CipherInputStream cis = new CipherInputStream(fin, c);
		byte[] d = new byte[16];
		int j = cis.read(d);
		while (j != -1) {
			fout.write(d, 0, j);
			j = cis.read(d);
		}

		fout.close();
		cis.close();
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


}
