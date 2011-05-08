package oculus;

import java.io.File;
import java.util.Date;
import java.util.TimerTask;

public class SystemWatchdog {

	// if reboot is "true" in config file
	private final boolean reboot = new Settings().getBoolean(State.reboot);
	private final boolean debug = new Settings().getBoolean(State.developer);

	// check every hour
	public static final long DELAY = State.FIVE_MINUTES;

	// when is the system stale and need reboot
	public static final long STALE = State.ONE_DAY * 2;

	// shared state variables
	private State state = State.getReference();

	// call back to message window
	private Application app = null;

	public SystemWatchdog(Application app) {
		if (reboot){
			this.app = app;
			java.util.Timer timer = new java.util.Timer();
			timer.scheduleAtFixedRate(new Task(), 5000, DELAY);
			if(debug) System.out.println("system watchdog starting...");
		}	
	}
	
	private class Task extends TimerTask {
		public void run() {
			
			if (debug) {
				System.out.println("system watchdog : " + (state.getUpTime()/1000) + " sec");
				app.message("system watchdog : " + (state.getUpTime()/1000) + " sec", null, null);
			}

			// only reboot is idle 
			if ((state.getUpTime() > STALE) && !state.getBoolean(State.userisconnected)){ // && !app.motionenabled ){
				
				String boot = new Date(state.getLong(State.boottime)).toString();
				app.message("last boot: " + boot, null, null);
				
				System.out.println("rebboting, last was: " + boot);
				
				if(debug){
					
					// copy std out and email it 
					String log = System.getenv("RED5_HOME")+"\\log\\jvm.stdout";
					String temp = System.getenv("RED5_HOME")+"\\log\\debug.txt";
			
					// delete if exists from before 
					new File(temp).delete();
					if(Util.copyfile(log, temp)){
					
						// blocking send 
						new SendMail("Oculus Rebooting", "been awake since: " + boot, temp, true);
				
						// emailed it, now delete it 
						new File(temp).delete();
						new File(log).deleteOnExit();
					}
				} 
				
				app.message("rebooting now...", null, null);				
				app.systemCall("shutdown -r -f -t 01");				
			}
		}
	}
}
