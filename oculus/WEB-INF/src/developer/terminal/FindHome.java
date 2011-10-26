package developer.terminal;

import java.io.*;
import java.net.*;

import oculus.State;
import oculus.Util;

/**
 * Bare bones terminal client for Oculus. 
 */
public class FindHome extends AbstractTerminal {

	private long start = System.currentTimeMillis();
	
	/** */ 
	public FindHome(String ip, String port, final String user, final String pass) 
		throws NumberFormatException, UnknownHostException, IOException {
		super(ip, port, user, pass);
	}

	public void execute(){
		
		out.println("state motionenabled true");
		out.println("publish camera");
		out.println("battery");
		out.println("undock");
		Util.delay(1000);
		out.println("undock");
		
		/*
		
		Util.delay(5000);
		out.println("dock");
		
		while(true){
			
			// docked now
			if(state.getBoolean(State.autodocking) && ((System.currentTimeMillis()-start)>10000)) break;
			
			System.out.println("waiting... " + state.get(State.autodocking));
			Util.delay(3000);
			
		}
			
		*/
		
		// log out 
		out.println("bye");
		
		// System.out.println("-- done --");
		// System.out.println(state.toString());
		
	}
	
	
	/** parameters: ip, port, user name, password [commands] */ 
	public static void main(String args[]) throws IOException {
		new FindHome(args[0], args[1], args[2], args[3]);
	}
}