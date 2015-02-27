
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

//Servidor do servico PhotoShareServer

public class PhotoShareServer {

	public static void main(String[] args) {
		System.out.println("servidor: main");
		PhotoShareServer server = new PhotoShareServer();
		server.startServer();
	}

	public void startServer (){
		ServerSocket sSoc = null;

		try {
			sSoc = new ServerSocket(23456);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		while(true) {
			try {
				Socket inSoc = sSoc.accept();
				ServerThread newServerThread = new ServerThread(inSoc);
				newServerThread.start();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		//sSoc.close();
	}


	//Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("thread do server para cada cliente");
		}

		public void run(){
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

				String user = null;
				String passwd = null;

				try {
					user = (String)inStream.readObject();
					passwd = (String)inStream.readObject();
					System.out.println("thread: depois de receber user e password: " + user + ":" + passwd);
				}catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				}

				//TODO: refazer
				//este codigo apenas exemplifica a comunicacao entre o cliente e o servidor
				//nao faz qualquer tipo de autenticacao
				if (user.length() != 0){
					outStream.writeObject(new Boolean(true));
				}
				else {
					outStream.writeObject(new Boolean(false));
				}

				int size = 0;
				byte [] fileByteBuf = null;
				//C:\Users\Utilizador\Desktop\atkHighUpper0011.png
				try {
					size = (Integer) inStream.readObject();
					String filename = (String) inStream.readObject();


					fileByteBuf = new byte[size];
					inStream.read(fileByteBuf, 0, size);
					if(size != -1){
						System.out.println("size received: " + size);

						//TODO find out why images created are invalid(same size)

						FileOutputStream fos = new FileOutputStream(".\\" + filename);
						fos.write(fileByteBuf);
						fos.close();
					}
					else
						System.out.println("! read size exceeds real size");
				}catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				}

				outStream.close();
				inStream.close();

				socket.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}