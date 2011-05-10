package oculus;

import java.io.File;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class SystemWatchdog {

	// if reboot is "true" in config file
	private final Settings settings = new Settings();
	private final boolean reboot = settings.getBoolean(State.reboot);
	private final boolean debug = settings.getBoolean(State.developer);

	// check every hour
	public static final long DELAY = State.FIVE_MINUTES;

	// when is the system stale and need reboot
	public static final long STALE = State.ONE_DAY * 2;

	// shared state variables
	private State state = State.getReference();
	
	// do at set interval 
	private Timer timer = new Timer();

    /** */
	public SystemWatchdog() {
		if (reboot){
			
			timer.scheduleAtFixedRate(new Task(), 5000, DELAY);
			
			if(debug) System.out.println("system watchdog starting...");
		}	
	}
	
	private class Task extends TimerTask {
		public void run() {
			
			if (debug) 
				System.out.println("system watchdog : " + (state.getUpTime()/1000) + " sec");

			// only reboot is idle 
			if ((state.getUpTime() > STALE) && !state.getBoolean(State.userisconnected)){ 
				
				// redundant if no user connected??
				// && !app.motionenabled ){
				
				// reboot should be enough? 
				// timer.cancel();
				
				String boot = new Date(state.getLong(State.boottime)).toString();				
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
						
						// does not work 
						//new File(log).deleteOnExit();
					}
				} 
	
				Util.systemCall("shutdown -r -f -t 01", true);				
			}
		}
	}
}
