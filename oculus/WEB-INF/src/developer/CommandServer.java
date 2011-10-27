package developer;

import java.io.*;
import java.net.*;
import java.util.Vector;

import org.jasypt.util.password.ConfigurablePasswordEncryptor;

import oculus.Application;
import oculus.BatteryLife;
import oculus.Docker;
import oculus.LoginRecords;
import oculus.Observer;
import oculus.OptionalSettings;
import oculus.PlayerCommands;
import oculus.Settings;
import oculus.State;
import oculus.Updater;
import oculus.Util;
import oculus.commport.ArduioPort;

/**
 * Start the chat server. Start new threads for a each connection on the given port
 * 
 * Created: 2007.11.3
 * 
 */
public class CommandServer {
	
	public static final String SEPERATOR = " : ";
	
	private static BatteryLife battery = BatteryLife.getReference();
	private static Docker docker = null;
	private static ArduioPort port = null;
	private static Application app = null;
	private static oculus.Settings settings = new Settings(); 
	private static ServerSocket serverSocket = null; 
	private static Vector<PrintWriter> printers = new Vector<PrintWriter>();
	private static LoginRecords records = new LoginRecords();

	/** Threaded client handler */
	class ConnectionHandler extends Thread implements Observer {
	
		private oculus.State state = oculus.State.getReference();
		private Socket clientSocket = null;
		private BufferedReader in = null;
		private PrintWriter out = null;

		public ConnectionHandler(Socket socket) {
		
			clientSocket = socket;
			
			try {
			
				in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())), true);
			
			} catch (IOException e) {
				
				System.out.println("fail aquire tcp streams: " + e.getMessage());
				
				try {
					
					if(in!=null) in.close();
					if(out!=null) out.close();
					if(clientSocket!=null) clientSocket.close();
					
				} catch (IOException e1) {
					System.out.println(e1.getMessage());
					return;
				}

				return;
			}

			// send banner 
			out.println("oculus version " + new Updater().getCurrentVersion() + " ready for login."); 

			// first thing better be user:pass
			try {
				
				final String inputstr = in.readLine();
				final String user = inputstr.substring(0, inputstr.indexOf(':')).trim();
				final String pass = inputstr.substring(inputstr.indexOf(':')+1, inputstr.length()).trim();
				
				sendToGroup(clientSocket.getInetAddress() + " attempt by " + user);

				if(app.logintest(user, pass)==null){
					
					// was plain text password?
					System.out.println("plain text pawwsord sent from: " + clientSocket.getInetAddress());
				    ConfigurablePasswordEncryptor passwordEncryptor = new ConfigurablePasswordEncryptor();
					passwordEncryptor.setAlgorithm("SHA-1");
					passwordEncryptor.setPlainDigest(true);
					String encryptedPassword = (passwordEncryptor
							.encryptPassword( user + settings.readSetting("salt") + pass)).trim();
					
					if(app.logintest(user, encryptedPassword)==null){
						sendToGroup("login failure : " + user);
						out.println("login failure, please drop dead");
						out.close();
						new Exception("login failure");
					}
				}
			} catch (Exception ex) {
				System.out.println(ex.getMessage());
				try {
					if(in!=null) in.close();
					if(out!=null) out.close();
					if(clientSocket!=null) clientSocket.close();
					return;
				} catch (IOException e) {
					System.out.println(e.getMessage());
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
			
			sendToGroup(printers.size() + " tcp connections active");
			
			try {

				// loop on input from the client
				while (true) {

					// been closed ?
					if(out.checkError()) {
						System.out.println("...output stream is closed, close client.");
						shutDown();
						break;
					}
					
					// blocking read from the client stream up to a '\n'
					String str = in.readLine();

					// client is terminating?
					if (str == null) break;
					
					// parse and run it 
					str = str.trim();
					System.out.println("address [" + clientSocket + "] message [" + str + "]");
					manageCommand(str);
					
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
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
				if(in!=null) in.close();
				if(out!=null) out.close();
				if(clientSocket!=null) clientSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}

			// show this users is no longer in the group
			System.out.println("currently [" + printers.size() + "] users are connected.");
			app.message(printers.size() + " tcp connections active.", null, null);
			sendToGroup(printers.size() + " tcp connections active.");
		}

		@Override
		/** send to socket on state change */ 
		public void updated(String key) {
			String value = state.get(key);
			if(value==null) out.println("deleted " + SEPERATOR + key);
			else out.println(key + SEPERATOR + value);
		}
		
		/** try to parse, look up, exec comand */
		public void playerCommand(final String str){
			final String[] cmd = str.split(" ");
			PlayerCommands ply = null;
			try {
				ply = PlayerCommands.valueOf(cmd[0]);
			} catch (Exception e) {
				System.out.println("Command Server, command not found:" + str);
				return;
			}
			if(ply!=null){
				
				// TODO: COLIN, ASK.... 
				System.out.println(".. cmd server, plyer cmd: " + cmd[0]);
			
				app.playerCallServer(ply, str);


				switch (ply) {
				
				case dockgrab: app.dockGrab(); break;
				
				case writesetting: 
					System.out.println("write setting: " + str);
					if(settings.readSetting(cmd[0]) == null) {
						settings.newSetting(cmd[0], cmd[1]);
					} else {
						settings.writeSettings(cmd[0], cmd[1]);
					}
					settings.writeFile();
					break;
						
				case publish:
					app.publish(str);
					break;
					

				// case disconnectotherconnections: app.disconnectOtherConnections(); break;
				case monitor: app.monitor(str); break;
				// case showlog: app.showlog(); break;
				case autodock: docker.autoDock(str); break;
				// case autodockcalibrate: docker.autoDock("calibrate " + str); break;
				case restart: app.restart(); break;
				// case softwareupdate: app.softwareUpdate(str); break;
				}
				
				
				//if(str.equals("restart")) app.restart();
				
			}
		}
		
		/** add extra commands, macros here */ 
		public void manageCommand(final String str){

			// try player commands first 
			playerCommand(str);
			
			// do exta commands below 
			final String[] cmd = str.split(" ");
			
			///if(str.equals("reboot")) Util.systemCall("shutdown -r -f -t 01");				
			
			//if(str.equals("restart")) app.restart();
			
			///if(str.startsWith("stop")) port.stopGoing();
	
			if(str.equals("settings")) out.println(settings.toString());
			
			if(str.equals("image")) {
				
				app.frameGrab();
				while(app.framgrabbusy){
					// out.println("waiting....");
					Util.delay(500);
				}
	
				out.println("...done...");
				System.out.println("...done...");
			}
			
			if(str.startsWith("nudge")) port.nudge(cmd[1]);
			
			if(str.startsWith("move")){
				if(cmd[1].equals("forward")) port.goForward();
				else if(cmd[1].equals("backwards")) port.goBackward();
				else if(cmd[1].equals("left")) port.turnLeft();
				else if(cmd[1].equals("right")) port.turnRight();
			}
			
			if(str.equals("cam")){
				app.publish("camera");
				port.camHoriz();
				port.camCommand("down");
				Util.delay(1500);
				port.camCommand("stop");
			}
			
			if(str.equals("bye")) shutDown();
						
			if(str.equals("find")) app.dockGrab();	
			
			if(str.equals("battery")) battery.battStats();
								
			if(str.equals("dock") && docker!=null) docker.autoDock("go");
			
			if(str.equals("undock") && docker!=null) docker.dock("undock");
									
			if(str.equals("stop")) port.stopGoing();
				
			if(str.equals("beep")) Util.beep();
			
			if(str.equals("tcp")) out.println("tcp connections : " + printers.size());
	
			if(str.equals("users")){
				out.println("active users : " + records.getActive());
				if(records.toString()!=null) out.println(records.toString());
			}

			if(str.equals("settings")) out.println(settings.toString());

			if(str.startsWith("state")) {
				if(cmd.length==3) state.set(cmd[1], cmd[2]);
				else out.println(state.toString());
			}		
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
	
	public void setDocker(Docker d) {
		docker = d;
		// this gets called often 
		///System.out.println("CommandManager: new docker created.... ");
	}
	
	/** */
	public CommandServer(oculus.Application a, ArduioPort p) {

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
				while(true) {
					battery.battStats();
					go();
				}
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
				new ConnectionHandler(serverSocket.accept());

			} catch (Exception e) {
				try {				
					serverSocket.close();
				} catch (IOException e1) {
					System.out.println("failed to open client socket: " + e1.getMessage());
					return;					
				}	
				
				System.out.println("failed to open client socket: " + e.getMessage());
				Util.delay(State.ONE_MINUTE);
			}
		}
	}
}
