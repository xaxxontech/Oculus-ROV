package developer.terminal;

import java.io.*;
import java.net.*;

import developer.CommandServer;

//import oculus.State;
//import oculus.PlayerCommands;
import oculus.State;
import oculus.Util;

public class FindHome extends AbstractTerminal {

	public FindHome(String ip, String port, final String user, final String pass) 
		throws NumberFormatException, UnknownHostException, IOException {
		super(ip, port, user, pass);
		execute();
	}

	public void execute(){
		
		long start = System.currentTimeMillis();
		out.println("cam");
		out.println("settings");
		out.println("battery");
		out.println("state");
		out.println("state");
		Util.delay(3000);
		
		if(state.get(State.dockstatus)!=null){
		
			if(state.get(State.dockstatus).equals(State.docked)){

				System.out.println(".. docked, undocking.");

				out.println("undock");
				Util.delay(1500);
				out.println("undock");
				Util.delay(1500);
				out.println("undock");	
				Util.delay(5000);				
		
			}	
		}
		
		System.out.println("undocked...");
		System.out.println(state);
		
		Util.delay(5000);
		
		out.println("dock");
		out.println("dock");
		
		Util.delay(5000);

		// wait for docking to end 
		while(true){
			
			// docked now
			// if(state.getBoolean(State.autodocking) && ((System.currentTimeMillis()-start)>10000)) break;
			
			System.out.println((System.currentTimeMillis()-start) + " going... ");
			System.out.println("waiting... " + state.get(State.autodocking));
			
			// refresh all state values from robot 
			// out.println("state autodocking true");
			// out.println("state");	
			/// out.println("find");		

		
			if(state.getBoolean(State.losttarget)) {
				state.delete(State.losttarget);
				out.println("dock");
			}

			if(state.getBoolean("foo")) {	
				break;
			}
			
			if(state.get(State.autodocking)!=null){
				System.out.println("auto is on..");
			}
			
			Util.delay(30000);
			
			System.out.println("state local: " + state.toString());

		}
	
		
		// log out 
		out.println("state foo reset");
		Util.delay(1500);
		out.println("bye");
		
		// System.out.println("-- done --");
		// System.out.println(state);
	}
	

	public void parseInput(final String str){
		System.out.println(this.getClass().getName() + " parse: " + str);
		String[] cmd = str.split(CommandServer.SEPERATOR);
		if(cmd.length==2) state.set(cmd[0], cmd[1]);	
	}
	
	/** parameters: ip, port, user name, password [commands] */ 
	public static void main(String args[]) throws IOException {
		new FindHome(args[0], args[1], args[2], args[3]);
	}
}