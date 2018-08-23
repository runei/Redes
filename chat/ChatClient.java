import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.nio.*;

public class ChatClient {

    // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
	JFrame frame = new JFrame("Chat Client");
	private JTextField chatBox = new JTextField();
	private JTextArea chatArea = new JTextArea();
    // --- Fim das variáveis relacionadas coma interface gráfica

    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui

	SocketChannel socketChannel;
	static private final Charset charset = Charset.forName("UTF8");
	static private final CharsetDecoder decoder = charset.newDecoder();

    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
	public void printMessage(final String message) {
		chatArea.append(message);
	}

    // Construtor
	public ChatClient(String server, int port) throws IOException {

        // Inicialização da interface gráfica --- * NÃO MODIFICAR *
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(chatBox);
		frame.setLayout(new BorderLayout());
		frame.add(panel, BorderLayout.SOUTH);
		frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
		frame.setSize(500, 300);
		frame.setVisible(true);
		chatArea.setEditable(false);
		chatBox.setEditable(true);
		chatBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					newMessage(chatBox.getText());
				} catch (IOException ex) {
				} finally {
					chatBox.setText("");
				}
			}
		});
        // --- Fim da inicialização da interface gráfica

        // Se for necessário adicionar código de inicialização ao
        // construtor, deve ser colocado aqui
		socketChannel = SocketChannel.open();
		socketChannel.connect(new InetSocketAddress(server, port));
		socketChannel.configureBlocking(false);
	}

    // Método invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
	public void newMessage(String message) throws IOException {
        // PREENCHER AQUI com código que envia a mensagem ao servidor
		ByteBuffer buffer = ByteBuffer.allocate( 16384 );
		buffer.clear();
		buffer.put(message.getBytes());
		buffer.flip();
		while (buffer.hasRemaining())
		{
			socketChannel.write(buffer);
		}      
	}

	public String processMessage(String message)
	{
		String[] recv_msg = message.split(" ", 3);
		if (recv_msg[0].equals("MESSAGE"))
		{
			return recv_msg[1] + ": " + recv_msg[2];
		}
		else if (recv_msg[0].equals("NEWNICK"))
		{
			return recv_msg[1] + " changed nickname to " + recv_msg[2];
		}
		else if (recv_msg[0].equals("JOINED"))
		{
			return recv_msg[1] + " joined the room ";
		}
		else if (recv_msg[0].equals("LEFT"))
		{
			return recv_msg[1] + " left the room ";
		}
		else if (recv_msg[0].equals("PRIVATE"))
		{
			return recv_msg[1] + " sent a private message: " + recv_msg[2];
		}
		return message;
	}

    // Método principal do objecto
	public void run() throws IOException {
        // PREENCHER AQUI
		ByteBuffer buffer = ByteBuffer.allocate( 16384 );
		while (true)
		{
			buffer.clear();
			socketChannel.read( buffer );
			buffer.flip();

			if (buffer.limit()==0) {
				continue;
			}

    // Decode and print the message to stdout
			String message = processMessage(decoder.decode(buffer).toString());
			printMessage(message + '\n');
		}

	}
	
    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
	public static void main(String[] args) throws IOException {
		ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
		client.run();
	}

}
