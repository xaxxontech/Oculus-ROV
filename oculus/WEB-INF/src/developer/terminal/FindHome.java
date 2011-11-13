package developer.terminal;

import java.io.*;
import java.net.*;

import developer.CommandServer;

//import oculus.State;
//import oculus.PlayerCommands;
import oculus.OptionalSettings;
import oculus.State;
import oculus.Util;

public class FindHome extends AbstractTerminal {

	long start = System.currentTimeMillis();
	boolean looking = true;

	public FindHome(String ip, String port, final String user, final String pass)
			throws NumberFormatException, UnknownHostException, IOException {
		super(ip, port, user, pass);
		execute();
	}

	/** wait on the camera and battery, dock */
	public void connect() {

		out.println("state");
		Util.delay(500);

		while (true) {
			if (state.get(State.batterystatus) == null) {
				out.println("battery");
				Util.delay(2000);
			} else
				break;
		}

		while (true) {
			if (state.get("publish") == null) {
				out.println("publish camera");
				Util.delay(2000);
			} else if (state.get("publish").equals("camera"))
				break;
		}

		if (state.get(State.dockstatus) != null) {
			if (state.get(State.dockstatus).equals(State.docked)) {
				
				// don't calibrate on dead battery! 
				if (state.getInteger(State.batterylife) < 50) {
					System.out.println("... this a bad move mitch, batttery = "
							+ state.get(State.batterylife));
					out.println("beep");
					out.println("beep");
					out.println("beep");
					out.println("beep");
					shutdown();
				}

				System.out.println(".. docked, so undocking.");
				out.println("undock");
				Util.delay(2500);
				out.println("move backward");
				Util.delay(1000);
				out.println("stop");
				Util.delay(1000);

				// turn round some
				
				int nudges = state.getInteger(OptionalSettings.offcenter.toString());
				if(nudges != State.ERROR) System.out.println("...found offest: " + nudges);
				
				// find perfect 360 time 
				out.println("move left");
				Util.delay(6300);
				out.println("stop");
				Util.delay(1000);
			}
		}
		
		// clear old failures
		if (state.getBoolean(State.losttarget)) {
			System.out.println("..... recovering from old attempt! ");
			out.println("state " + State.losttarget + " false");

		}
			
		out.println("settings nudgedelay 250");
		out.println("settings stopdelay 300");
		out.println("settings volume 100");	
		out.println("state foo true");

	}

	public void execute() {

		// gain bot's resources state
		connect();			

		int nudges = spinFind();
		
		out.println("settings " + OptionalSettings.offcenter.toString() +" "+ nudges);
		
		// hand over control, wait for docking... 
		out.println("dock");
		start = System.currentTimeMillis();
		while (true) {
			
			Util.delay(5000);
			
			if (state.get(State.dockstatus) != null)
				if (state.get(State.dockstatus).equals(State.docked))
					break;

			System.out.println("[" + (System.currentTimeMillis() - start) + "] ms into autodocking.");
			if ((System.currentTimeMillis() - start) > State.TWO_MINUTES) {
				System.out.println("FindHome, Aborting...");
				
				out.println("beep");
				out.println("beep");
				out.println("beep");
				shutdown();
			}
			

			// clear old failures
			if (state.getBoolean(State.losttarget)) {
				System.out.println("..... target lost again.. ");
				out.println("state " + State.losttarget);
				
				out.println("beep");
				out.println("beep");
				out.println("beep");
				shutdown();
			}
		}

		System.out.println("...now docked, took [" + ((System.currentTimeMillis() - start)/1000) + "] sec");

		out.println("battery");
		out.println("memory");
		
		// save scan setting 
		
		
		
		// out.println("restart")
		out.println("bye");

		Util.delay(500);
		System.out.println("find home closed its self??");
		Util.delay(500);
		shutdown();
	}
	
	/** spin until dock in view */ 
	public int spinFind(){
		int i = 0; 
		start = System.currentTimeMillis();
		while (looking) {

			System.out.println("[" + (System.currentTimeMillis() - start) + "] ms into docking.");

			if ((state.get(State.dockxpos) != null)) {
				if (state.getInteger(State.dockxpos) > 0) {

					System.out.println("... see dock, dock!");

					// three checks
					out.println("find");
					Util.delay(4000);
					if (state.getInteger(State.dockxpos) > 0) {

						out.println("nudge backward");
						Util.delay(3000);
						out.println("find");
						Util.delay(3000);

						// ok, dock lock must be good
						if (state.getInteger(State.dockxpos) > 0)
							looking = false;

					}
				}
			}
			
			if ((System.currentTimeMillis() - start) > State.FIVE_MINUTES) {
				System.out.println("FindHome(), looking is Aborting...");
				shutdown();
			}
			
			if(out==null) {
				System.out.println("errrorrrrr: socket closed, shutdown");
				shutdown();
			}
			
			// call every so often 
			if ((i++ % 5) == 0) {

				System.out.println(i + " : publish again, look back one" );
				out.println("nudge right");
				out.println("publish camera");
				out.println("memory");
				Util.delay(4000);
				state.dump();

			}
		
			if (looking) {

				out.println("nudge left");
				Util.delay(4000);
				out.println("find");
				Util.delay(4000);

			}
		}
		
		return i;
	}

	public void parseInput(final String str) {
		// System.out.println("_parse: " + str);
		String[] cmd = str.split(CommandServer.SEPERATOR);
		if (cmd.length == 2)
			state.set(cmd[0], cmd[1]);
	}

	/** parameters: ip, port, user name, password [commands] */
	public static void main(String args[]) throws IOException {
		new FindHome(args[0], args[1], args[2], args[3]);
	}
}