package developer.terminal;

import java.io.*;
import java.net.*;

//import oculus.State;
import oculus.Util;

public class TestMoves extends AbstractTerminal {

	public TestMoves(String ip, String port, final String user, final String pass) 
		throws NumberFormatException, UnknownHostException, IOException {
		super(ip, port, user, pass);
	}

	public void execute(){
			
		// send commands to get ready 
		out.println("state motionenabled true");
		out.println("publish camera");
		out.println("battery");
		out.println("undock");
		Util.delay(1000);
		out.println("undock");	
		Util.delay(5000);
		out.println("dock");
		out.println("state");
		out.println("settings");

		
		/*
		// wait for docking to end 
		while(true){
			
			// docked now
			if(state.getBoolean(State.autodocking) && ((System.currentTimeMillis()-start)>10000)) break;
			
			System.out.println("waiting... " + state.get(State.autodocking));
			Util.delay(3000);
			
			// refresh all state values from robot 
			out.println("state");
		}
			
		*/
		
		// log out 
		Util.delay(3000);
		out.println("bye");
	}
	
	/** parameters: ip, port, user name, password [commands] */ 
	public static void main(String args[]) throws IOException {
		new TestMoves(args[0], args[1], args[2], args[3]);
	}
}