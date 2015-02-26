
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;

public class PhotoShareClient {

	public static void main(String[] args) {
		System.out.println("client");
		PhotoShareClient client = new PhotoShareClient();
		client.startClient();
	}

	public void startClient() {
		Socket soc = null;

		try {
			soc = new Socket("127.0.0.1", 23456);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		try{
			ObjectOutputStream outStream = new ObjectOutputStream(soc.getOutputStream());
			ObjectInputStream inStream = new ObjectInputStream(soc.getInputStream());

			String user = "lol";
			String passwd = "lel";

			outStream.writeObject(user);
			outStream.writeObject(passwd);

			boolean answer = (Boolean) inStream.readObject();

			//autenticado
			if(answer){
				System.out.println("y");

				File f = new File(""); //TODO add directory path
				byte[] fileByteBuf = Files.readAllBytes(f.toPath());
				
				//TODO send buf size
				outStream.writeObject(fileByteBuf.length);
				
				outStream.write(fileByteBuf, 0, fileByteBuf.length);
			}
			else
				System.out.println("no");

		}catch(IOException e){
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}