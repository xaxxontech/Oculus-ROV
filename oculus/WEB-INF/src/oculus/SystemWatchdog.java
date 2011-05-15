package oculus;

import java.io.File;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

public class SystemWatchdog {
	
	private static Logger log = Red5LoggerFactory.getLogger(SystemWatchdog.class, "oculus");

	// if reboot is "true" in config file
	private final Settings settings = new Settings();
	private final boolean reboot = settings.getBoolean(State.reboot);
	private final boolean debug = settings.getBoolean(State.developer);

	// check every hour
	public static final long DELAY = State.TWO_MINUTES;

	// when is the system stale and need reboot
	public static final long STALE = State.ONE_DAY * 2; 
	
	// shared state variables
	private State state = State.getReference();
	
    /** Constructor */
	public SystemWatchdog() {
		if (reboot){
			Timer timer = new Timer();
			timer.scheduleAtFixedRate(new Task(), State.ONE_MINUTE, DELAY);
			if(debug) log.info("system watchdog starting...");
		}	
	}
	
	private class Task extends TimerTask {
		public void run() {
			
			if (debug) log.info("system watchdog : " + (state.getUpTime()/1000) + " sec");

			// only reboot is idle 
			if ((state.getUpTime() > STALE) && !state.getBoolean(State.userisconnected)){ 
				
				String boot = new Date(state.getLong(State.boottime)).toString();				
				log.info("rebooting, last was: " + boot);
				log.info("user logged in for: " + state.getLoginSince() + " ms");
			
				if(debug){
					
					// copy stdout log and email it 
					String oculus = System.getenv("RED5_HOME")+"\\log\\oculus.log";
					String logfile = System.getenv("RED5_HOME")+"\\log\\jvm.stdout";
					String temp = System.getenv("RED5_HOME")+"\\log\\debug.txt";
			
					// delete if exists from before 
					new File(temp).delete();
					
					// write current state to file
					state.writeFile(temp);
					
					if(Util.copyfile(logfile, temp)){
						
						if(Util.copyfile(oculus, temp)){
											
						// blocking send 
						new SendMail("Oculus Rebooting", "been awake since: " + boot, temp, true);
						
						// emailed it, now delete it 
						new File(temp).delete();
						
						// does not work 
						// new File(log).deleteOnExit();
					
						} System.out.println("error on file copy: " + oculus);
					} System.out.println("error on file copy: " + logfile);
				} 
	
				System.exit(-1);
				
				// Util.systemCall("shutdown -r -f -t 01", true);				
			}
		}
	}
}
