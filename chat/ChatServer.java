import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

public class ChatServer
{

	private enum State
	{
		init, outside, inside;
	}

	private static class User
	{
		public String room;
		public String name;
		public State state;

		public User(User user)
		{
			this.room = user.room;
			this.name = user.name;
			this.state = user.state;
		}

		public User()
		{
			this.room = "";
			this.name = "";
			this.state = State.init;
		}
	}

	private static final class ServerMessages
	{
		public static final String OK = "OK";
		public static final String ERROR = "ERROR";
		public static final String MESSAGE = "MESSAGE";
		public static final String NEWNICK = "NEWNICK";
		public static final String JOINED = "JOINED";
		public static final String LEFT = "LEFT";
		public static final String BYE = "BYE";
		public static final String PRIVATE = "PRIVATE";
	} 

	private static class Command 
	{
		public String command = "";
		public String parameter = "";

		public Command(String message)
		{
			String[] strs = message.split(" ", 2);
			this.command = strs[0].substring(1);
			if (strs.length > 1)
			{
				this.parameter += strs[1];
			}
		}
	}

  // A pre-allocated buffer for the received data
	static private final ByteBuffer buffer = ByteBuffer.allocate( 16384 );

  // Decoder for incoming text -- assume UTF-8
	static private final Charset charset = Charset.forName("UTF8");
	static private final CharsetDecoder decoder = charset.newDecoder();

	static private Map<SocketChannel, User> users = new HashMap<SocketChannel, User>();

	static public void main( String args[] ) throws Exception {
    // Parse port from command line
		int port = Integer.parseInt( args[0] );

		try {
      // Instead of creating a ServerSocket, create a ServerSocketChannel
			ServerSocketChannel ssc = ServerSocketChannel.open();

      // Set it to non-blocking, so we can use select
			ssc.configureBlocking( false );

      // Get the Socket connected to this channel, and bind it to the
      // listening port
			ServerSocket ss = ssc.socket();
			InetSocketAddress isa = new InetSocketAddress( port );
			ss.bind( isa );

      // Create a new Selector for selecting
			Selector selector = Selector.open();

      // Register the ServerSocketChannel, so we can listen for incoming
      // connections
			ssc.register( selector, SelectionKey.OP_ACCEPT );
			System.out.println( "Listening on port " + port );

			while (true) {
        // See if we've had any activity -- either an incoming connection,
        // or incoming data on an existing connection
				int num = selector.select();

        // If we don't have any activity, loop around and wait again
				if (num == 0) {
					continue;
				}

        // Get the keys corresponding to the activity that has been
        // detected, and process them one by one
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> it = keys.iterator();
				while (it.hasNext()) {
          // Get a key representing one of bits of I/O activity
					SelectionKey key = it.next();

          // What kind of activity is it?
					if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {

            // It's an incoming connection.  Register this socket with
            // the Selector so we can listen for input on it
						Socket s = ss.accept();
						System.out.println( "Got connection from " + s );

            // Make sure to make it non-blocking, so we can use a selector
            // on it.
						SocketChannel sc = s.getChannel();
						users.put(sc, new User());
						sc.configureBlocking( false );

            // Register it with the selector, for reading
						sc.register( selector, SelectionKey.OP_READ );

					} else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {

						SocketChannel sc = null;

						try {

              // It's incoming data on a connection -- process it
							sc = (SocketChannel)key.channel();
							boolean ok = processInput( sc );

              // If the connection is dead, remove it from the selector
              // and close it
							if (!ok) {
								key.cancel();

								Socket s = null;
								try {
									s = sc.socket();
									System.out.println( "Closing connection to " + s );
									users.remove(sc);
									s.close();
								} catch( IOException ie ) {
									System.err.println( "Error closing socket " + s + ": " + ie );
								}
							}

						} catch( IOException ie ) {

              // On exception, remove this channel from the selector
							key.cancel();

							try {
								sc.close();
							} catch( IOException ie2 ) { System.out.println( ie2 ); }

							System.out.println( "Closed " + sc );
						}
					}
				}

        // We remove the selected keys, because we've dealt with them.
				keys.clear();
			}
		} catch( IOException ie ) {
			System.err.println( ie );
		}
	}

	static private void newMessage(SocketChannel sc, String message) throws IOException {
        // PREENCHER AQUI com código que envia a mensagem ao servidor
		buffer.clear();
		buffer.put(message.getBytes());
		buffer.flip();
		while (buffer.hasRemaining())
		{
			sc.write(buffer);
		}       

	}

	static private boolean isCommand(String message)
	{
		if (message.charAt(0) == '/' && message.charAt(1) != '/')
		{
			return true;
		}
		return false;
	}

	static private boolean isNameAvailable(String name)
	{
		for (User user : users.values())
		{
			if (user.name.equals(name))
			{
				return false;
			}
		}
		return true;
	}

	static private void sendMsgToRoom(User sender, String message) throws IOException
	{
		for (Map.Entry<SocketChannel, User> entry : users.entrySet())
		{
			User user = entry.getValue();
			if (user.room.equals(sender.room) && !user.name.equals(sender.name))
			{
				newMessage(entry.getKey(), message);
			}
		}
	}

	static private boolean changeNick(SocketChannel sc, Command command) throws IOException
	{
		if (command.command.equals("nick") && isNameAvailable(command.parameter))
		{
			users.get(sc).name = command.parameter;
			newMessage(sc, ServerMessages.OK);
			return true;
		}
		return false;
	}

	static private boolean bye(SocketChannel sc, Command command) throws IOException
	{
		if (command.command.equals("bye"))
		{
			newMessage(sc, ServerMessages.BYE);
			return true;
		}
		return false;
	}

	static private boolean join(SocketChannel sc, Command command) throws IOException
	{
		if (command.command.equals("join") && !command.parameter.equals(""))
		{
			User user = users.get(sc);
			user.room = command.parameter;
			newMessage(sc, ServerMessages.OK);
			sendMsgToRoom(user, ServerMessages.JOINED + " " + user.name);
			return true;
		}
		return false;
	}

	static private boolean leave(SocketChannel sc, Command command) throws IOException
	{
		if (command.command.equals("leave"))
		{
			User user = users.get(sc);
			newMessage(sc, ServerMessages.OK);
			sendMsgToRoom(user, ServerMessages.LEFT + " " + user.name);
			user.room = "";
			return true;			
		}
		return false;
	}

	static private boolean priv(SocketChannel sc, Command command) throws IOException
	{
		if (command.command.equals("priv"))
		{
			String[] params = command.parameter.split(" ", 2);
			if (params.length >= 2)
			{
				User sender = users.get(sc);
				for (Map.Entry<SocketChannel, User> entry : users.entrySet())
				{
					User  user = entry.getValue();
					if (user.name.equals(params[0]) && user != sender)
					{
						newMessage(sc, ServerMessages.OK);
						newMessage(entry.getKey(), ServerMessages.PRIVATE + " " + sender.name + " " + params[1]);
						return true;
					}
				}
			}
		}
		return false;
	}

	static private boolean processInit(SocketChannel sc, String message) throws IOException
	{
		if (isCommand(message))
		{
			Command command = new Command(message);
			if (changeNick(sc, command))
			{
				users.get(sc).state = State.outside;
				return true;
			}
			else if (bye(sc, command))
			{
				return false;
			}
		}
		newMessage(sc, ServerMessages.ERROR);
		return true;
	}

	static private boolean processOustide(SocketChannel sc, String message) throws IOException
	{
		if (isCommand(message))
		{
			Command command = new Command(message);
			if (join(sc, command))
			{
				users.get(sc).state = State.inside;
				return true;
			}
			else if (changeNick(sc, command))
			{
				return true;
			}
			else if (bye(sc, command))
			{
				return false;
			}
			else if (priv(sc, command))
			{
				return true;
			}
		}
		newMessage(sc, ServerMessages.ERROR);
		return true;	
	}

	static private boolean processInside(SocketChannel sc, String message) throws IOException
	{
		User user = users.get(sc);
		if (isCommand(message))
		{
			Command command = new Command(message);
			User old_user = new User(user);
			if (changeNick(sc, command))
			{
				// verificar retorno user.name
				sendMsgToRoom(user, ServerMessages.NEWNICK + " " + old_user.name + " " + user.name);
				return true;
			}
			else if (join(sc, command))
			{
				sendMsgToRoom(old_user, ServerMessages.LEFT + " " + old_user.name);
				return true;
			}
			else if (leave(sc, command))
			{
				user.state = State.outside;
				return true;
			}
			else if (bye(sc, command))
			{
				sendMsgToRoom(user, ServerMessages.LEFT + " " + user.name);
				return false;			
			}
			else if (priv(sc, command))
			{
				return true;
			}
			newMessage(sc, ServerMessages.ERROR);
			return true;
		}
		String send_message = message;
		if (send_message.substring(0, 2).equals("//"))
		{
			send_message = send_message.substring(1);
		}
		send_message = ServerMessages.MESSAGE + " " + user.name + " " + send_message;
		newMessage(sc, send_message);
		sendMsgToRoom(user, send_message);
		return true;
	}

	static private boolean processEvent(SocketChannel sc, String message) throws IOException
	{
		User user = users.get(sc);
		if (user.state == State.init)
		{
			return processInit(sc, message);
		}
		else if (user.state == State.outside)
		{
			return processOustide(sc, message);		
		}
		else if (user.state == State.inside)
		{
			return processInside(sc, message);
		}
		return true;
	}

  // Just read the message from the socket and send it to stdout
	static private boolean processInput( SocketChannel sc ) throws IOException {
    // Read the message to the buffer
		buffer.clear();
		sc.read( buffer );
		buffer.flip();

    // If no data, close the connection
		if (buffer.limit() == 0) {
			return false;
		}

    // Decode and print the message to stdout
		String message = decoder.decode(buffer).toString();
		
		if (!processEvent(sc, message))
		{
			return false;
		}
		//System.out.print( message + '\n' );
		//newMessage(sc, message);
		return true;
	}
}