package oculus;

import java.util.TimerTask;

/** */
public class CommandManager {

	private Application app = null;
	
	// private final boolean debug = new Settings().getBoolean("developer");

	private static CommandManager singleton = null;

	// private MulticastChannel channel = MulticastChannel.getReference();
		
	private CommandManager(){
		
		
	}
	
	public static CommandManager getReference() {
		if (singleton  == null) {
			singleton = new CommandManager();
		}
		return singleton;
	}
	
	public void init(Application app) {
			
		if(app != null){
			System.out.println("configured, don't call twice");
			return;
		}
		
		this.app = app;
	}
	
	public void execute(String command){
		
		if(app == null) {
			System.out.println("manager is not configured");
			return;
		}
		
		System.out.println("manager exec: " + command);
		
		String cmd[] = command.split(" ");
		app.playerCallServer(cmd[0], cmd[1]);
		
		// app.message(command, null, null);
		
	}

	private class Task extends TimerTask {
		@Override
		public void run() {
			
			
			
		}
	}
}
