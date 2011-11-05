package developer.terminal;

import java.io.*;
import java.net.*;

import oculus.State;
import oculus.Util;

public abstract class AbstractTerminal  {
	
	State state = State.getReference();

	/** share on read, write threads */
	Socket socket;
	BufferedReader in;
	PrintWriter out;
	
	/** */ 
	public AbstractTerminal(String ip, String port, final String user, final String pass) 
		throws NumberFormatException, UnknownHostException, IOException {
	
		socket = new Socket(ip, Integer.parseInt(port));
		if(socket != null){
			
			out = new PrintWriter(new BufferedWriter(
				new OutputStreamWriter(socket.getOutputStream())), true);
			
			// login on connect 
			out.println(user + ":" + pass);
			startReader(socket);
		}
	}
	
	public abstract void execute();
	
	public abstract void parseInput(final String str);
	
	public void startReader(final Socket socket) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				} catch (Exception e) {
					return;
				}
			
				String input = null;
				while(true){
					try {
											
						input = in.readLine();
						if(input==null) break;
						else parseInput(input);

						if(out.checkError()){
							System.out.println("..write end closed.");
							break;
						}
					} catch (IOException e) {
						System.out.println(e.getMessage());
						break;
					}
				}				
				try {
					in.close();
					out.close();
					socket.close();
					System.out.println("-- exit --");
					System.out.println(state.toString());
					Util.delay(300);
					System.exit(-1);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}
