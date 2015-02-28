import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

//Servidor do servico PhotoShareServer

public class PhotoShareServer {

	public static void main(String[] args) {
		System.out.println("server: main");
		PhotoShareServer server = new PhotoShareServer();
		server.startServer();
	}

	public void startServer() {
		ServerSocket sSoc = null;

		try {
			sSoc = new ServerSocket(23456);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		while (true) {
			try {
				Socket inSoc = sSoc.accept();
				ServerThread newServerThread = new ServerThread(inSoc);
				newServerThread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// sSoc.close();
	}

	// Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("thread: created");
		}

		public void run() {
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(
						socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(
						socket.getInputStream());

				String user = null;
				String passwd = null;

				try {
					user = (String) inStream.readObject();
					passwd = (String) inStream.readObject();
					System.out.println("thread: received user and password: "
							+ user + ":" + passwd);
				} catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				}

				// TODO: refazer
				// este codigo apenas exemplifica a comunicacao entre o cliente
				// e o servidor
				// nao faz qualquer tipo de autenticacao
				if (user.length() != 0) {
					outStream.writeObject(new Boolean(true));
				} else {
					outStream.writeObject(new Boolean(false));
				}

				int size = 0;
				FileOutputStream fos = null;
				// C:\Users\Utilizador\Desktop\a.png
				try {
					size = (Integer) inStream.readObject();
					String filename = (String) inStream.readObject();

					System.out.println("--> " + size);
					System.out.println(filename);

					byte[] fileByteBuf = new byte[size];
					System.out.println("fileByteBuf length: "
							+ fileByteBuf.length);

					//TODO Kaze was here and he actually made it work.
					
					int bytesRead = 0; // bytes jah lidos
					fos = new FileOutputStream(".\\" + filename);

					while (bytesRead < size) { // enquanto o total dos bytes
						// lidos forem menor que o
						// tamanho do ficheiro
						int count = inStream.read(fileByteBuf);
						if (count == -1) {
							throw new IOException("Expected file size: " + size
									+ "read size: " + bytesRead);
						}

						fos.write(fileByteBuf, 0, count);
						bytesRead += count;
						System.out.println("total received: " + bytesRead
								+ " out of " + size);
					}
					System.out.println("File transfer completed!");
					fos.close();

				} catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				} catch (SocketException e2) {
					e2.printStackTrace();
				}

				outStream.close();
				inStream.close();

				socket.close();
				System.out.println("thread: dead");

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}