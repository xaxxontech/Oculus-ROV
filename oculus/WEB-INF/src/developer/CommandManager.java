package developer;

import oculus.Application;
import oculus.LoginRecords;
import oculus.PlayerCommands;
import oculus.Settings;
import oculus.State;
import oculus.Updater;
import oculus.Util;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

/** */
public class CommandManager {

	// TODO: make this reserved string in app.java?
	private static String oculus = "oculus";
	private static String function = "function";
	private static String argument = "argument";
	private static Logger log = Red5LoggerFactory.getLogger(
			CommandManager.class, oculus);
	private State state = State.getReference();
	private static Application app = null;

	//private MulticastChannel channel = null;

	/** */
	public CommandManager(Application a) {
		app = a;
		//channel = new MulticastChannel(this);
		log.debug("command manager ready");
		System.out.println("command manager ready");
	}

	/**
	 * send a command to the group public void send(Command command) {
	 * channel.write(command); }
	 */
	/** try to find dock by turning in a circle and scanning */
	public void home() {

		System.out.println("...starting to find home....");

		while (!state.equals(State.dockstatus, State.docked)) {

			if (!state.getBoolean("motionenabled")) {
				System.out.println("motion enabled.. ");
				return;
			}

			if (state.get("status").equalsIgnoreCase("docked")) {
				System.out.println("...home found....");
				return;
			}

			// app.playerCallServer(PlayerCommands.nudge, "left");
			Util.delay(3000);
			// app.playerCallServer(PlayerCommands.move, "stop");
			// app.playerCallServer(PlayerCommands.autodock, "go");

		}

	}

	/** send email to admin account */
	public void dump() {
		new Thread() {
			public void run() {

				System.out.println("--dump--");
				System.out.println("version: " + new Updater().getCurrentVersion());
				System.out.println("passengers: " + app.loginrecords.getPassengers());
				System.out.println("login records: " + app.loginrecords.toString());
				state.dump();
				
				new LoginRecords().save();
				
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

		System.out.println("type: " + command.getType() + " cmd: " + command.list());

		// only listen to <oculus> xml </oculus> messages
		if (command.getType().equalsIgnoreCase(oculus)) {

			final String fn = command.get(function);
			final String arg = command.get(argument);

			if (fn == null) return;

			// if (fn.equalsIgnoreCase("home")) {
			/*
			 * new Thread() { public void run() { home(); } }.start();
			 */
			// } else

			if (fn.equalsIgnoreCase("salt")) {

				String salt = new Settings().readSetting("salt");
				if (salt != null) {

					System.out.println("salt: " + salt);

					// Command cmd = new Command("kk");
					// cmd.add("salted", salt);
					// cmd.send();
					// System.out.println(cmd);

				} else System.err.println("error - no salt");

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

				app.playerCallServer(PlayerCommands.dockgrab, null);
				
			} else {
				
				// must be an application primitive try {
				app.playerCallServer(fn, arg);

			}
		}
	}
}
