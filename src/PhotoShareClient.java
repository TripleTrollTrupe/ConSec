import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.regex.Pattern;

public class PhotoShareClient {

	public static void main(String[] args) {



		System.out.println("client");

		//verifica se existe os argumentos -u e -a como sao obrigatorios se nao existirem a chamada esta mal formatada
		if(args[0].compareTo("-u")!=0 || args[2].compareTo("-a")!=0){
			System.out.println("Argumentos incorrectos");
			System.exit(-1);			
		}

		//vai buscar o endereco IP e porto do servidor aos argumentos
		String[] serverAddress = args[3].split(":");
		String server=serverAddress[0];

		//efectua verificao se os caracteres do porto sao todos digitos, para certificar que se pode fazer parseInt
		if(Pattern.matches("//d+",serverAddress[1])){
			System.out.println("O porto dado e invalido, por favor verifique");
			System.exit(-1);
		}

		int port = Integer.parseInt(serverAddress[1]);

		//verifica se o porto e correcto		
		if(port!=23456){
			System.out.println("Porto invalido");
			System.exit(-1);
		}

		//nao tenho a certeza desta parte mas suponho que o utilizador seja dado na forma : <user:pwd>
		String[] userInfo = args[1].split(":");
		String user = userInfo[0];
		String passwd = userInfo[1];


		PhotoShareClient client = new PhotoShareClient();

		// to enable interactivity, uncomment relevant section above and
		// substitute literals
		client.startClient(server, port, user, passwd,args);


	}

	public void startClient(String server, int port, String user, String passwd, String [] args) {
		Socket soc = null;

		try {
			soc = new Socket(server, port);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		try {
			ObjectOutputStream outStream = new ObjectOutputStream(
					soc.getOutputStream());
			ObjectInputStream inStream = new ObjectInputStream(
					soc.getInputStream());


			outStream.writeObject(user);
			outStream.writeObject(passwd);

			boolean answer = (Boolean) inStream.readObject();

			// autenticado
			if (answer) {
				System.out.println("Authentication succeeded");

				
				//TODO: querem por o servidor a mandar uma mensagem de confirmacao das operacoes?, if so temos que adicionar
				// um check deste lado tambem
				switch(args[4]){
				case "-p": 
					try{
						outStream.writeObject("-p");
						//funciona para uma fotografia apenas, how do with more?
						File f = new File(args[5]);

						byte[] fileByteBuf = new byte [1024];
						int fileSize = (int) f.length();
						String filename = f.getName();

						outStream.writeObject(fileSize);
						outStream.writeObject(filename);
						System.out.println("<-- " + fileSize);

						// enquanto puder ser fragmentado em pacotes de 1024 bytes
						int n;
						FileInputStream fin = new FileInputStream(f);
						while ((n=fin.read(fileByteBuf, 0, 1024))>0) { 

							// o read so le 1024 bytes de cada vez, nao vale a pena mandar mais de cada vez
							outStream.write(fileByteBuf, 0, n);
						}
						System.out.println("Transfer completed! Closing all connections!");
						fin.close();
						outStream.close();
						inStream.close();
						soc.close();
						System.out.println("end of execution");
						break;
					} catch (IOException e){
						e.printStackTrace();
					}

				case "-l":
					try{
						outStream.writeObject("-l");
						outStream.writeObject(args[5]);
						System.out.println((String) inStream.readObject());
						outStream.close();
						inStream.close();
						soc.close();
						System.out.println("end of execution");
						break;
					} catch (IOException e){
						e.printStackTrace();
					}

				case "-g":
					try{
						outStream.writeObject("-g");
						outStream.writeObject(args[5]);
						//TODO : receber cenas do servidor too layzee ATM sry
						break;
					} catch (IOException e){
						e.printStackTrace();
					}

				case "-c":
					try{
						outStream.writeObject("-c");
						outStream.writeObject(args[5]);
						outStream.writeObject(args[6]);
						outStream.writeObject(args[7]);
						outStream.close();
						inStream.close();
						soc.close();
						System.out.println("end of execution");
						break;
					} catch(IOException e){
						e.printStackTrace();
					}

				case "-f":
					try{
						outStream.writeObject("-f");
						outStream.writeObject(args[5]);
						outStream.close();
						inStream.close();
						soc.close();
						System.out.println("end of execution");
						break;

					} catch(IOException e){
						e.printStackTrace();
					}
				case "-n":
					try{
						outStream.writeObject("-n");
						//TODO: receber as cenas do servidor too layzee ATM sry
						outStream.close();
						inStream.close();
						soc.close();
						System.out.println("end of execution");
					} catch(IOException e){
						e.printStackTrace();
					}
				}
			} else
				System.out.println("Authentication failed");

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

}
