package oculus;

import java.util.Date;
import java.util.TimerTask;

public class SystemWatchdog {

	// private static Logger log =
	// Red5LoggerFactory.getLogger(SystemWatchdog.class, "oculus");

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

	public java.util.Timer timer = new java.util.Timer();

	public SystemWatchdog(Application app) {
		this.app = app;
		if (reboot){
			timer.scheduleAtFixedRate(new Task(), 5000, DELAY);
			System.out.println("system watchdog starting...");
		}	
	}

	private class Task extends TimerTask {
		@Override
		public void run() {

			if (debug)
				app.message("system watchdog : " + (state.getUpTime()/1000) + " sec", null, null);

			if (state.getUpTime() > STALE) {

				app.message("been awake since: " 
						+ new Date(state.getLong(State.boottime)).toString(), null, null);
				Util.delay(5000);
				app.message("rebooting now...", null, null);
				app.restart();
			}
		}
	}
}
