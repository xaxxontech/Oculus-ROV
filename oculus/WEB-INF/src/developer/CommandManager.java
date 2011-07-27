package developer;

import java.io.File;

import oculus.Application;
import oculus.SendMail;
import oculus.State;
import oculus.Util;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;


/** */
public class CommandManager {

	// TODO: make this reserved string in app.java?
	private static String oculus = "oculus";
	private static String function = "function";
	private static String argument = "argument";
	private static Logger log = Red5LoggerFactory.getLogger(CommandManager.class, oculus);
	private State state = State.getReference();
	private static Application app = null;
	private MulticastChannel channel = null;

	/** */
	public CommandManager(Application a){ 
		app = a;
		channel = new MulticastChannel(this);
		log.debug("command manager ready");
		System.out.println("command manager ready");
	}

	/** send a command to the group */
	public void send(Command command) {
		channel.write(command);
	}

	/** try to find dock by turning in a circle and scanning */
	public void home() {
		new Thread() {
			public void run() {

				System.out.println("...starting to find home....");
				
				
				//state.set(State.sona, State.timeout);
				while (!state.get("status").equalsIgnoreCase("docked")) {

					if (! state.getBoolean("motionenabled")){
						System.out.println("motion enabled.. ");
						return;
					}
					
					if (state.get("status").equalsIgnoreCase("docked")){
						System.out.println("...home found....");
						return;
					}
					
					app.playerCallServer(Application.playerCommands.nudge, "left");
					Util.delay(3000);
					app.playerCallServer(Application.playerCommands.move, "stop");
					app.playerCallServer(Application.playerCommands.autodock, "go");
				
				}
			}
		}.start();
	}

	/** send email to admin account */
	public void dump(){
		new Thread() {
			public void run() {

				final String temp = System.getenv("RED5_HOME")+"\\log\\debug.txt";
				final String move = System.getenv("RED5_HOME")+"\\log\\moves.log";

				// delete if exists from before 
				new File(temp).delete();
				
				// write current state to file
				state.writeFile(temp);			
										
				// blocking send 
				// new SendMail("Oculus State Dump", "debug dump attached", temp, true);
				// new SendMail("Oculus Move Log", "debug dump attached", move, true);

				// email'ed it, now delete it 
				new File(temp).delete();	
				// new File(move).delete();	

			}
		}.start();
	}
	
	/** take any message on this channel, look for an action */
	public void execute(Command command) {
		if (app == null) {
			System.out.println("execute(): not configured");
			log.error("execute(): not configured");
			return;
		}

		System.out.println("type: " + command.getType() + " cmd: " + command.list());

		// only listen to <oculus> xml </oculus> messages
		if (command.getType().equalsIgnoreCase(oculus)) {
			
			final String fn = command.get(function);
			final String arg = command.get(argument);

			if (fn != null) {
				if (fn.equalsIgnoreCase("home")) {

					home();

				} else if (fn.equalsIgnoreCase("dump")) {

					dump();
					
				} else if (fn.equalsIgnoreCase("sonar")) {
					if( arg != null ){
						if(arg.equals("debug")) state.set(State.sonarDebug, true);
						if(arg.equals("reset")) state.set(State.sonarDebug, false);
					}
				} else if (fn.equalsIgnoreCase("find")) {

					// System.out.println("forward.. to do, merge with exp");
					
					try {
						app.playerCallServer(Application.playerCommands.dockgrab, null);
					} catch (Exception e) {
						System.out.println("_app call error");
					}
			
					
				} else { // must be an application primitive
					try {
						app.playerCallServer(fn, arg);
					} catch (Exception e) {
						System.out.println("_app call error");
					}
				}
			}
		}
	}
}
