package developer.terminal;

import java.io.*;
import java.net.*;

public class Terminal {

	boolean running = true;

	public Terminal(String ip, String port, final String user, final String pass, final String[] commands) 
		throws NumberFormatException, UnknownHostException, IOException {
	
		Socket socket = new Socket(ip, Integer.parseInt(port));
		if(socket != null){
			// startReader(socket);
			String input = null;
			BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(socket.getOutputStream())), true);
			
			// login on connect 
			out.println(user + ":" + pass);
				
			System.out.println("logging in...");
			
			for(int i = 0 ; i < commands.length ; i++){
				out.println(commands[i]);
				if (commands[i].equalsIgnoreCase("bye") || commands[i].equalsIgnoreCase("quit")) 
					running=false;
			}
			
			// boolean running = true;
			while (running) {
				try {
	
					input = stdin.readLine();
					if (input == null) running=false;
					if (out == null) running=false;
					if (input.equalsIgnoreCase("bye") || input.equalsIgnoreCase("quit")) running=false;

					out.println(input);
	
				} catch (Exception e) {
					System.out.println(e.getMessage());
					running = false;
				}
			}
			
			out.close();
			stdin.close();
			socket.close();
		}
	}

	public void startReader(final Socket socket) {
		new Thread(new Runnable() {
		
			@Override
			public void run() {
		
				BufferedReader in = null;
				
				try {
					in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				} catch (Exception e) {
					System.out.println(e.getMessage());
					System.exit(-1);
				}
			
				String input = null;
			
				try {
										
					input = in.readLine();
					
				} catch (IOException e) {
					try {
						in.close();
					} catch (IOException e1) {
						System.out.println(e.getMessage());
					}

					// return;
					System.exit(-1);
				}

				if (input == null) System.exit(-1);
				else System.out.println(input);
				
			}
		}).start();
	}

	// driver
	public static void main(String args[]) throws IOException {

		String[] cmd = new String[args.length-4];
		for(int i = 0 ; i < (args.length-4); i++)
			cmd[i] = args[i+4];
		
		// Terminal terminal = 
		
		new Terminal(args[0], args[1], args[2], args[3], cmd);
		
	}
}