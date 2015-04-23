import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class CypherAction {

	public static void main(String args[]) throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, IOException {
		File f = new File("." + File.separator + "shadow" + File.separator
				+ "test.txt");
		System.out.println("Testing cypher operation....");
		cypherFile(f); // comment out to test decypher
		System.out.println("Operation succeeded!");
		System.out.println("Testing decypher operation...");
		decypherFile(f); // comment out to test cypher
		System.out.println("Operation Concluded!");
	/*obsolete due to keytool usage
	 * 	if (!existsKeys()) {
			System.out.println("There are no public or private keys!");
			generateKeys();
		}*/
	}

	public static void cypherFile(File f) throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException, IOException {
		KeyGenerator kg = KeyGenerator.getInstance("AES");
		kg.init(128);
		SecretKey key = kg.generateKey();

		Cipher c = Cipher.getInstance("AES");
		c.init(Cipher.ENCRYPT_MODE, key);

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
		byte[] keyEncoded = key.getEncoded();
		FileOutputStream kos = new FileOutputStream(f.getPath() + ".key");
		ObjectOutputStream oos = new ObjectOutputStream(kos);
		oos.write(keyEncoded);
		oos.close();
		fis.close();
		cos.close();
	}

	public static void decypherFile(File f) throws IOException,
			InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException {
		Cipher c = Cipher.getInstance("AES");
		FileInputStream fis = new FileInputStream(f.getPath() + ".key");
		ObjectInputStream ois = new ObjectInputStream(fis);
		byte[] keyEncoded = new byte[16];
		ois.read(keyEncoded);
		SecretKeySpec keySpec2 = new SecretKeySpec(keyEncoded, "AES");
		c.init(Cipher.DECRYPT_MODE, keySpec2); // SecretKeySpec � subclasse de
												// secretKey

		ois.close();

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

	//method that generates keys for the server, to be used exclusively within the server! 
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
	}
	//can be used to check existence of keystorage
	public static boolean existsKeyStorage() {
		File keystorage = new File("." + File.separator + "keytool"+File.separator+"serverkeystore.jck");
		/*obsolete due to keytool usage
		 * File pubkey = new File("." + File.separator + "keys" + File.separator
				+ "public.key");
		File privkey = new File("." + File.separator + "keys" + File.separator
				+ "private.key");
		return (pubkey.exists() && privkey.exists());*/
		return keystorage.exists();
	}

}
