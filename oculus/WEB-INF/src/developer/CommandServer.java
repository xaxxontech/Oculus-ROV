package developer;

import java.io.*;
import java.net.*;
import java.util.Vector;

import oculus.ArduinoCommDC;
import oculus.Docker;
import oculus.LoginRecords;
import oculus.Observer;
import oculus.OptionalSettings;
import oculus.Settings;
import oculus.State;
import oculus.Util;

/**
 * Start the chat server. Start new threads for a each connection on the given
 * port
 * 
 * Created: 2007.11.3
 * 
 * @author Brad Zdanivsky
 */
public class CommandServer {
	
	private static Docker docker = null;
	private static ArduinoCommDC port = null;
	private static oculus.Application app = null;
	private static State state = State.getReference();
	private static oculus.Settings settings = new Settings(); 
	private static ServerSocket serverSocket = null; 
	private static Vector<PrintWriter> printers = new Vector<PrintWriter>();
	private static LoginRecords records = new LoginRecords();

	/** Threaded client handler */
	class ConnectionHandler extends Thread implements Observer {
	
		private Socket clientSocket = null;
		private BufferedReader in = null;
		private PrintWriter out = null;

		public ConnectionHandler(Socket socket) {
			super();
			clientSocket = socket;
			try {
				in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())), true);
			} catch (IOException e) {
				System.out.println("fail aquire tcp streams: " + e.getMessage());
				try {
					in.close();
					out.close();
					clientSocket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
					return;
				}
				return;
			}

			// first thing better be user:pass
			try {
				final String salted = in.readLine();
				final String user = salted.substring(0, salted.indexOf(':'));
				final String pass = salted.substring(salted.indexOf(':')+1, salted.length());
				
				sendToGroup(clientSocket.getInetAddress() + " attempt by " + user);

				if(app.logintest(user, pass)==null){
					System.out.println(".....login failure:"+user);
					in.close();
					out.close();
					clientSocket.close();
					return;
				}
		
				// tell new user what is up 
				out.println(state.toString());
				app.message("tcp connection " + user + clientSocket.getInetAddress(), null, null);

			} catch (Exception ex) {
				System.out.println(ex.getMessage());
				try {
					clientSocket.close();
					return;
				} catch (IOException e) {
					return;
				}
			}
			
			// keep track of all other user sockets output streams
			printers.add(out);	
			state.addObserver(this);
			this.start();
		}

		/** do the client thread */
		@Override
		public void run() {
			
			System.out.println(clientSocket.getInetAddress() + " connected");
			sendToGroup(clientSocket.getInetAddress() + " connected");
			sendToGroup(printers.size() + " tcp connections active");
			
			try {

				// loop on input from the client
				while (true) {

					// blocking read from the client stream up to a '\n'
					String str = in.readLine();

					// client is terminating?
					if (str == null) {
						shutDown();
						break;
					}
						
					str = str.trim();
					System.out.println("address [" + clientSocket + "] message [" + str + "]");
					app.message(clientSocket.getInetAddress() + " " + str, null, null);
			
					if(str.equals("factory")) app.factoryReset();
					
					if(str.equals("tcp")) out.println("tcp connections: " + printers.size());

					if(str.equals("reboot")) app.restart();
					
					if(str.equals("cam")){
						app.publish("camera");
						port.camHoriz();
						port.camCommand("down");
						Util.delay(1500);
						port.camCommand("stop");
					}
					
					/*
					if(str.equals("dump")) {
						for (int c = 0; c < printers.size(); c++) printers.get(c).close();
						printers.clear();
					}
					*/
					
					if(str.equals("find")) app.dockGrab();	
					
					if(str.equals("state")) out.println(state.toString());
										
					if(str.equals("dock") && docker!=null) docker.autoDock("go");
					
					if(str.equals("undock") && docker!=null) docker.dock("undock");
					
					if(str.equals("home") && docker!=null) Util.dockingTest(app, port, docker);
										
					if(str.equals("users")){
						//String logs = records.toString();
						//if(logs==null) return;
						//else System.out.println(logs);
						
						out.println("active users: " + records.getActive());
						
						/*
						if(records.getActive()>0){
							String[] list = records.getActiveList();
							System.out.println("------------------"+ list.length);
							for(int i = 0; i < list.length ; i++)
								System.out.println(i + " " + list[i] + " ");
							
						}*/
						
					}
				}
			} catch (Exception e) {
				shutDown();
			}
		}

		// close resources
		private void shutDown() {

			// log to console, and notify other users of leaving
			System.out.println("server: closing socket [" + clientSocket + "]");
			sendToGroup(clientSocket.getInetAddress() + " has left the group.");

			try {
				// close resources
				printers.remove(out);
				in.close();
				out.close();
				clientSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			// show this users is no longer in the group
			System.out.println("currently [" + printers.size() + "] users are connected.");
			app.message(printers.size() + " tcp users are connected.", null, null);
			sendToGroup(printers.size() + " tcp connections active.");
		}

		@Override
		public void updated(String key) {
			String value = state.get(key);
			if(value==null) out.println(key + " was deleted");
			else out.println(key + " : " + value);
		}
	}
		
	/** send input back to all the clients currently connected */
	public void sendToGroup(String str) {
		PrintWriter pw = null;
		for (int c = 0; c < printers.size(); c++) {
			pw = printers.get(c);
			if (pw.checkError()) {		
				printers.remove(pw);
				pw.close();
			} else pw.println(str);
		}
	}
		
	public void chat(String str){
		sendToGroup(str);
	}
	
	public void setDocker(Docker d) {
		docker = d;
		System.out.println("CommandManager: new docker created.... ");
	}
	
	/** */
	public CommandServer(oculus.Application a, ArduinoCommDC p) {

		if(app != null) {
			System.out.println("allready configured");
			return;
		}
		
		app = a;
		port = p;
		
		/** register shutdown hook */
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if(serverSocket!=null)
						serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}));
		
		// do long time
		new Thread(new Runnable() {
			@Override
			public void run() {
				//while(true)
					go();
			}
		}).start();
	}
	
	/** do forever */ 
	public void go(){
		
		try {
			serverSocket = new ServerSocket(settings.getInteger(OptionalSettings.commandport.toString()));
		} catch (Exception e) {
			System.out.println("server sock error: " + e.getMessage());
			return;
		} 
		
		System.out.println("listening with socket [" + serverSocket + "] ");
		
		// serve new connections until killed
		
		while (true) {
			try {

				// new user has connected
				Socket socket = serverSocket.accept();
				System.out.println("server: connection accepted [" + socket + "]");
				new ConnectionHandler(socket);

			} catch (Exception e) {
				
				System.out.println("failed to open client socket: " + e.getMessage());
				
				try {				
					serverSocket.close();
				} catch (IOException e1) {
					System.out.println("failed to open client socket: " + e1.getMessage());
					return;					
				}				
			}
		}
	}
}
