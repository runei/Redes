import java.io.*;
import java.net.*;

class TCPServer {

	public static void main(String argv[]) throws Exception
	{
		ServerSocket welcomeSocket = new ServerSocket(6789);
		while(true) 
		{
			Socket connectionSocket = welcomeSocket.accept();
			ClientThread ct = new ClientThread(connectionSocket);
			new Thread(ct).start();
		}
	}

}

class ClientThread implements Runnable
{

	private Socket connectionSocket;

	public ClientThread(Socket connectionSocket)
	{
		this.connectionSocket = connectionSocket;
	}

	public void run()
	{
		try {
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			String clientSentence = inFromClient.readLine();
			String capitalizedSentence = clientSentence.toUpperCase() + '\n';
			outToClient.writeBytes(capitalizedSentence);
			connectionSocket.close();
		} catch (IOException e) {
			
		}
	}

}