package developer.terminal;

import java.io.*;
import java.net.*;

import developer.CommandServer;

import oculus.OptionalSettings;
import oculus.State;
import oculus.Util;

public class FindHome extends AbstractTerminal {

	long start = System.currentTimeMillis();
	boolean looking = true;
	int fullspin = 605;

	public FindHome(String ip, String port, final String user, final String pass)
			throws NumberFormatException, UnknownHostException, IOException {
		super(ip, port, user, pass);
		execute();
	}

	/** wait on the camera and battery, dock */
	public void connect() {

		out.println("settings");
		out.println("state");
		Util.delay(2000);

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
				Util.delay(2300);
				out.println("move backward");
				Util.delay(700);
				out.println("stop");
				Util.delay(700);

				// turn round some
				
				int nudges = state.getInteger(OptionalSettings.offcenter.toString());
				int aboutface = state.getInteger(OptionalSettings.aboutface.toString());
				System.out.println("...found offest: " + nudges + " aboutface: " + aboutface);
				
				if(fullspin<600) fullspin = 600;
				
				out.println("move left");
				Util.delay(fullspin);
				out.println("stop");
				Util.delay(1000);
			}
		}
		
		// clear old failures
		if (state.getBoolean(State.losttarget)) {
			System.out.println("..... recovering from old attempt! ");
			out.println("state " + State.losttarget + " false");

		}
			
		out.println("settings nudgedelay 290");
		out.println("settings stopdelay 100");
		out.println("settings volume 90");	
		out.println("state foo true");

	}

	public void execute() {

		// gain bot's resources state
		connect();			

		int nudges = spinFind();
		System.out.println("spin time = " + (fullspin/2));
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
				// nudges = spinFind();
				
				/*
				out.println("beep");
				out.println("beep");
				out.println("beep");
			*/	shutdown();
				
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
		out.println("settings " + OptionalSettings.aboutface.toString() + " " + ((double)fullspin/2));
		
		Util.delay(500);
		// out.println("restart")
		out.println("bye");
		Util.delay(500);
		
		System.out.println("... find home closed its self??");
		Util.delay(5000);
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
				if(out.checkError()){
					System.out.println("errrorrrrr: socket closed, shutdown");
					shutdown();
				}
			}
			
			// call every so often 
			if ((i++ % 3) == 0) {

				System.out.println(i + " : publish again, look back one" );
				out.println("nudge right");
				out.println("publish camera");
				out.println("memory");
				Util.delay(4000);
				// state.dump();

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