package developer;

import oculus.Application;
import oculus.ArduinoCommDC;
import oculus.AutoDock;
import oculus.Docker;
import oculus.LoginRecords;
import oculus.PlayerCommands;
//  oculus.PlayerCommands;
import oculus.Settings;
import oculus.State;
import oculus.Updater;
import oculus.Util;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

/** */
public class CommandManager {

	// TODO: make this reserved string in app.java?
	private static Logger log = Red5LoggerFactory.getLogger(CommandManager.class, "oculus");
	private static State state = State.getReference();
	private static String oculus = "oculus";
	private static String function = "function";
	private static String argument = "argument";
	private static Application app = null;
	private Docker docker = null;
	private static ArduinoCommDC port = null;
///	public static boolean busy = false;


	// private MulticastChannel channel = null;

	public CommandManager(Application a, ArduinoCommDC p) {
		app = a;
		port = p;

		System.out.println("command manager...");
		log.debug("command manager ready...");
	}

	public void setDocker(Docker d) {
		this.docker = d;
		System.out.println("CommandManager: new docker created.... ");
	}

	public /*synchronized*/ void dockingTest() {

		if (docker == null || port == null || app == null) {
			log.error("not configured");
			return;
		}

		if (state.getBoolean(State.autodocking)) {
			log.error("can't auto dock twice");
			return;
		}

		if( ! state.getBoolean("busy")){
			state.set("busy", true);
		} else {
			log.error("can't auto dock twice, busy");
			return;
		}
		
		new Thread(new Runnable() {

			@Override
			public void run() {
				
				app.playerCallServer(PlayerCommands.chat.toString(), "command manager taking over");
				System.out.println("----camera on----");
				app.publish("camera");
				port.camHoriz();
		
				
				if (state.equals(State.dockstatus, State.docked)) {
					docker.dock(State.undock);
					Util.delay(3000);
					docker.dock(State.undock);
					Util.delay(3000);	
				}
				
				
				//.out.println("----going backward");
				// port.nudge("backward");
		//		Util.delay(3000);				
			//	port.nudge("backward");
				Util.delay(3000);				
		//		port.slide("left");

		//		System.out.println("----calling grab");
				app.dockGrab();

				Util.delay(4000);
				// System.out.println("--state--");

				// state.dump();
				// System.out.println("-- now dock --");

				//Util.delay(7000);
				


				/* System.out.println("----camera on----");
					app.publish("camera");
					port.camHoriz();
					
					Util.delay(27000);

					if (state.equals(State.dockstatus, State.docked)) {
						System.out.println("---- undocking");
						docker.dock(State.undock);
						Util.delay(3000);
						port.nudge("backward");
						Util.delay(3000);
						port.turnRight();
						Random r = new Random();
						int d = r.nextInt(2000)+300;
						Util.delay(d);
						port.stopGoing();
						System.out.println("---- rand = " + d);
					}
					*/
				
				// TODO: how many is 360??
				for (int i = 0; i < 20; i++) {
					if (state.getInteger(State.dockxsize) > 0) {

						port.turnRight();
						Util.delay(300);
						port.stopGoing();
						port.nudge("forward");
						Util.delay(3000);

						if (state.getInteger(State.dockxsize) > 0) {
							// docker = new AutoDock(app, null, port, null);
							docker.autoDock("go");
							state.delete("busy");
							return;
						} else app.dockGrab();


					} else {

						System.out.println("can't see dock... " + i);
						Util.delay(1000);
						port.turnLeft();
						// TODO: TAKE THIS INFO FROM X Y 
						Util.delay(300);
						port.stopGoing();
						Util.delay(3000);
						app.dockGrab();
					
					}
				}

				System.out.println("_+_+_ failure to find dock");
				new SendMail("Oculus Message", "failure to find dock", app);
				state.delete("busy");
			}
		}).start();
	}

	/**
	 * send a command to the group public void send(Command command) {
	 * channel.write(command); }
	 */
	/** try to find dock by turning in a circle and scanning */
	public void home() {

		if (docker == null || port == null || app == null) {
			log.error("not configured");
			return;
		}

		/*
		 * new Thread(new Runnable() {
		 * 
		 * @Override public void run() {
		 * 
		 * System.out.println("...starting to find home....");
		 * 
		 * while (!state.equals(State.dockstatus, State.docked)) {
		 * 
		 * if (!state.getBoolean("motionenabled")) {
		 * System.out.println("motion enabled.. "); return; }
		 * 
		 * if (state.get("status").equalsIgnoreCase("docked")) {
		 * System.out.println("...home found...."); //
		 * app.commandManager("restart", null); return; }
		 * 
		 * // app.playerCallServer(PlayerCommands.nudge, "left");
		 * Util.delay(3000); // app.playerCallServer(PlayerCommands.move,
		 * "stop"); // app.playerCallServer(PlayerCommands.autodock, "go");
		 * 
		 * } }
		 */

	}

	/** send email to admin account */
	public void dump() {
		new Thread() {
			public void run() {

				System.out.println("--dump--");
				System.out.println("version: "
						+ new Updater().getCurrentVersion());
				System.out.println("passengers: "
						+ app.loginrecords.getPassengers());
				System.out.println("login records: "
						+ app.loginrecords.toString());
				state.dump();

				LoginRecords.save();

				// blocking send
				// new SendMail("Oculus State Dump", "debug dump attached...");
			}
		}.start();
	}

	/** take any message on this channel, look for an action */
	public void execute(Command command) {
		if (app == null) {
			System.out.println("CommandManager.execute(): not configured");
			log.error("execute(): not configured");
			return;
		}

		System.out.println("type: " + command.getType() + " cmd: "
				+ command.list());

		// only listen to <oculus> xml </oculus> messages
		if (command.getType().equalsIgnoreCase(oculus)) {

			final String fn = command.get(function);
			final String arg = command.get(argument);

			/*
			 * final String pass = command.get("salted");
			 * 
			 * if (pass == null) { System.out.println("no salted meats!"); //
			 * return; }
			 */

			if (fn == null)
				return;

			// if (fn.equalsIgnoreCase("home")) {
			/*
			 * new Thread() { public void run() { home(); } }.start();
			 */
			// } else

			if (fn.equalsIgnoreCase("salt")) {

				String salt = new Settings().readSetting("salt");
				if (salt != null) {

					System.out.println("salt: " + salt);
					// /app.playerCallServer(PlayerCommands.showlog, null);

					// Command cmd = new Command("kk");
					// cmd.add("salted", salt);
					// cmd.send();
					// System.out.println(cmd);

				} else
					System.err.println("error - no salt");

			} else if (fn.equalsIgnoreCase("dump")) {

				dump();

			} else if (fn.equalsIgnoreCase("sonar")) {
				if (arg != null) {
					if (arg.equals("debug"))
						state.set(State.sonardebug, true);
					if (arg.equals("reset"))
						state.set(State.sonardebug, false);
				}
			} else if (fn.equalsIgnoreCase("find")) {

				app.dockGrab();
				// playerCallServer(PlayerCommands.dockgrab, null);

			} else {

				// must be an application primitive try {
				app.playerCallServer(fn, arg);

			}
		}

		System.out.println("-----------------exit!");
	}
}
