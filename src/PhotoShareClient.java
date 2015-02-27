
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Scanner;

public class PhotoShareClient {

	public static void main(String[] args) {

		Scanner sc = new Scanner(System.in);

		System.out.println("client");

		/*System.out.print("Server: ");
		String server = sc.next();
		System.out.print("Port: ");
		int port = sc.nextInt(); */

		PhotoShareClient client = new PhotoShareClient();

		// to enable interactivity, uncomment relevant section above and substitute literals
		client.startClient("127.0.0.1",23456, sc);

		sc.close();
	}

	public void startClient(String server, int port, Scanner sc) {
		Socket soc = null;

		try {
			soc = new Socket(server, port);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		try{
			ObjectOutputStream outStream = new ObjectOutputStream(soc.getOutputStream());
			ObjectInputStream inStream = new ObjectInputStream(soc.getInputStream());

			/*System.out.print("User: ");
			String user = sc.next();
			System.out.print("Password: ");
			String passwd = sc.next(); */

			// to enable interactivity, uncomment relevant section above and remove this
			String user = "admin";
			String passwd = "admin";

			outStream.writeObject(user);
			outStream.writeObject(passwd);

			boolean answer = (Boolean) inStream.readObject();

			//autenticado
			if(answer){
				System.out.println("Authentication succeeded");

				System.out.print("Insert file to send: ");
				String path = sc.nextLine();
				File f = new File(path);
				byte[] fileByteBuf = Files.readAllBytes(f.toPath());
				int fileSize = fileByteBuf.length;
				String filename = f.getName();

				outStream.writeObject(fileSize);
				outStream.writeObject(filename);
				System.out.println("<-- " + fileSize);
				outStream.write(fileByteBuf, 0, fileSize);
			}
			else
				System.out.println("Authentication failed");

		}catch(IOException e){
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

}