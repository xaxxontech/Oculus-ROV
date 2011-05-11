package oculus;

import java.util.Timer;
import java.util.TimerTask;

/** */
public class EmailAlerts {

	// how low of battery to warm user with email
	public static final int WARN_LEVEL = 35;

	// how often to check, ten minutes 
	public static final long DELAY = State.FIVE_MINUTES;

	// call back to message window
	private Application app = null;
	private Timer timer = new java.util.Timer();

	// configuration 
	private Settings settings = new Settings();
	private final boolean debug = settings.getBoolean("developer");
	private final boolean alerts = settings.getBoolean("emailalerts");
	private BatteryLife life = BatteryLife.getReference();
	
	/** Constructor */
	public EmailAlerts(Application app) {
		if (alerts && life.batteryPresent()){
			this.app = app;
			timer.scheduleAtFixedRate(new Task(), State.ONE_DAY, DELAY);
			if(debug) System.out.println("starting email alerts...");
		}
	}

	/** run on timer */
	private class Task extends TimerTask {
		@Override
		public void run() {
			if (life.batteryPresent()) {

				int batt[] = life.battStatsCombined();
				String lifestr = Integer.toString(batt[0]);
				int life = batt[0];
				int status = batt[1];
				
				// if draining only
				if (status == 1) {

					if (debug)
						app.message("checking battery: " + "battery " + lifestr + "%", null, null);

					if (life < WARN_LEVEL) {
		
						app.message("battery low, sending email", null, null);
						new SendMail("Oculus Message", "battery " + Integer.toString(life) 
								+ "% and is draining!", app); 

						// TODO: trigger auto dock
						// app.autodock();

						// only send single email
						timer.cancel();
					}
				}
			}
		}
	}
}
