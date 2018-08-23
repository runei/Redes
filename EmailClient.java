class EmailClient 
{
	
	public static void main(String args[])
	{
		if (args.length != 2)
		{
			System.out.println("Modelo Compilar: java EmailClient server porta");
			return;
		}

		String server = args[0];
		int porta = Integer.parseInt(args[1]);

		Socket socket = new Socket(server, porta);

	}

}