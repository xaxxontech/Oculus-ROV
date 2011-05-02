package oculus;

import java.io.File;
import java.util.Date;
import java.util.TimerTask;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

public class SystemWatchdog {

	// private static Logger log = Red5LoggerFactory.getLogger(SystemWatchdog.class, "oculus");

	// if reboot is "true" in config file
	private final boolean reboot = new Settings().getBoolean("reboot");
	private final boolean debug = new Settings().getBoolean("developer");

	// check every hour
	public static final int DELAY = 3600 * 1000;

	// when is the system stale and need reboot
	// seconds in day = 86400
	public static final int STALE = (86400 * 2) * 1000;

	// shared state variables
	private State state = State.getReference();

	// call back to message window
	private Application app = null;

	public SystemWatchdog(Application app) {
		this.app = app;
		if (reboot){
			java.util.Timer timer = new java.util.Timer();
			timer.scheduleAtFixedRate(new Task(), 5000, DELAY);
			System.out.println("system watchdog starting...");
		}	
	}

	private class Task extends TimerTask {
		@Override
		public void run() {

			if (debug)
				app.message("system watchdog : " + (state.getUpTime()/1000) + " sec", null, null);

			// only reboot is idle 
			if ((state.getUpTime() > STALE) && app.motionenabled ){ // && (app.userconnected==null)) {
				
				String boot = new Date(state.getLong(State.boottime)).toString();
				app.message("been awake since: " + boot, null, null);
				
				/*
				if(debug){
				
					
					state.set(State.emailbusy, true);
					
				String filename = System.getenv("RED5_HOME")+"\\log\\oculus.log"; //jvm.stdout";
				
				if(!new SendMail().sendMessage("Oculus Reboot", "been awake since: " + boot, filename))
					app.message("failed to send email", null, null); // value)
				
				// wait for email to send 
				state.set(State.emailbusy, true);
				
				long start = System.currentTimeMillis();
				while(state.getBoolean(State.emailbusy) && (System.currentTimeMillis() - start > 60000)){
					System.out.println("waiting..." + (System.currentTimeMillis() - start));
					Util.delay(1000);
				}
				
				new File(filename).deleteOnExit();
				
				app.message("rebooting now...", null, null);
				
				// System.exit(0);
			
				app.restart();
				System.exit(0);
				
				*/
				
				app.systemCall("shutdown -r -f -t 01");				
			}
		}
	}
}
