package oculus;

// import java.util.TimerTask;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

/** */
public class CommandManager {
	
	private static Logger log = Red5LoggerFactory.getLogger(CommandManager.class, "oculus");
	private Application app = null;

	private static CommandManager singleton = null;

	/** */ 
	private CommandManager(){
		System.out.println("command manager ready...");
		log.debug("command manager ready...");
	}
	
	/** */ 
	public static CommandManager getReference() {
		if (singleton  == null) {
			singleton = new CommandManager();
		}
		return singleton;
	}
	
	/** */
	public void init(Application app) {
			
		if(app != null){
			log.debug("configured, don't call twice");
			System.out.println("configured, don't call twice");
			return;
		}
		
		this.app = app;
	}
	
	/** */
	public void execute(String command){
		
		if(app == null) {
			System.out.println("manager is not configured");
			return;
		}
		
		System.out.println("manager exec: " + command);
		log.info("manager exec: " + command);
		
		String cmd[] = command.split(" ");
		app.playerCallServer(cmd[0], cmd[1]);
		
		// app.message(command, null, null);
		
	}

	/*
	private class Task extends TimerTask {
		@Override
		public void run() {
			
			
			
		}
	}
	*/
	
}
