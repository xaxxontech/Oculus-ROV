package developer.terminal;

import java.io.*;
import java.net.*;

import developer.CommandServer;

//import oculus.Observer;
import oculus.State;
import oculus.Util;

/**
 * Bare bones terminal client for Oculus. 
 */
public class FindHome {

	//State state = State.getReference();
	private long start = System.currentTimeMillis();

	/** share on read, write threads */
	Socket socket = null;
	PrintWriter out = null;
	
	/** */ 
	public FindHome(String ip, String port, final String user, final String pass) 
		throws NumberFormatException, UnknownHostException, IOException {

		socket = new Socket(ip, Integer.parseInt(port));
		if(socket != null){
	
			out = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(socket.getOutputStream())), true);
			
			out.println(user + ":" + pass);
			startReader(socket);
			
			// state.addObserver(this);
			
			out.println("state motionenabled true");
			out.println("publish");
			out.println("battery");
			
			//if(state.get(State.dockstatus).equals(State.docked))
			
			out.println("undock");
			
			Util.delay(5000);
			
			out.println("undock");

			//while(state.getInteger(State.dockxpos)==0){
			//	out.println("find");
			//	Util.delay(1200);
			//}
			
			Util.delay(6000);
			out.println("dock");

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
					return;
				}
			
				String input = null;
				while(true){
					try {			
						input = in.readLine();
						if(input==null) break;
						else parseCommand(input);	
					} catch (IOException e) {
						break;
					}
				}
				
				// System.out.println(".. server closed socket, logged out.");
				
				try {
					in.close();
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				// die hard, kill process 
				//System.exit(-1);
			}
		}).start();
	}

	/** */ 
	private void parseCommand(String input) {
		
		if(out.checkError()){
			System.out.println("..write end closed.");
			try {
				socket.close();
				System.exit(-1);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if(input==null) return;
		else input = input.trim();
		
		if(input.length()==0) return;
		
		// System.out.println("parse length: " + input.length());
		// System.out.println("parse: " + input);
		
		if(input.contains(CommandServer.SEPERATOR)){
			String[] cmd = input.split(CommandServer.SEPERATOR);
			
			
			// state.set(cmd[0], cmd[1]);
			
			/*
			if(cmd[0].equals(State.dockdensity)){
											
				System.out.println("got possss : " + cmd[1]);
				
				if(cmd[1].startsWith("0.")){
					
					System.out.println("dock target is zero density..");
					
					if(state.getInteger(State.dockxpos)==0){
						out.println("nudge left");
						out.println("nudge backwards");
						Util.delay(600);						
						out.println("dock");
					}
				}
			}
			*/
			
			if(cmd[0].equals(State.dockstatus)){
				if(cmd[1].equals(State.docked)){
					
					if((System.currentTimeMillis()-start)>15000){
						out.println("battery");

						Util.delay(300);
						//out.println("state");
						
						System.out.println("........docked!");
					
						//if(cmd[0].equals(State.batterystatus)){
						//	.equals("charging")){
						
						//	out.println("battery");
						//	System.out.println("testing battery... ");
						//	Util.delay(2500);
						//}
							
						System.out.println("--------done!");
						out.println("bye");
						//Util.delay(500);
						//state.dump();
						
					}
				}
			}
		}
	}
	
	
	/** parameters: ip, port, user name, password [commands] */ 
	public static void main(String args[]) throws IOException {
		new FindHome(args[0], args[1], args[2], args[3]);
	}
}