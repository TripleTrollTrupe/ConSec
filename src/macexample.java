import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;


public class macexample {

		public static void main(String args[]) throws NoSuchAlgorithmException, InvalidKeyException, IOException{
			FileOutputStream fos =new FileOutputStream("test");
			Mac mac=Mac.getInstance("HmacSHA256");
			KeyGenerator kg = KeyGenerator.getInstance("DESede");
			Key key = kg.generateKey();
			mac.init(key);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			String data= "This have I thought good to deliver thee, ....";
			byte buf[]=data.getBytes();
			mac.update(buf);
			oos.writeObject(data);
			oos.writeObject(mac.doFinal());
			fos.close();
		}
}
