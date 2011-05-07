package oculus;

import java.util.TimerTask;

/** */
public class EmailAlerts {

	// how low of battery to warm user with email
	public static final int WARN_LEVEL = 30;

	// how often to check, ten minutes 
	public static final long DELAY = State.TEN_MINUTES;

	// call back to message window
	private Application app = null;
	
	// set to "true" is config file
	private final boolean debug = new Settings().getBoolean("developer");
	private final boolean alerts = new Settings().getBoolean("emailalerts");

	public java.util.Timer timer = new java.util.Timer();

	public EmailAlerts(Application app) {
		if (alerts && app.batterypresent){
			this.app = app;
			timer.scheduleAtFixedRate(new Task(), 10000, DELAY);
			if(debug) System.out.println("starting email alerts...");
		}
	}

	private class Task extends TimerTask {
		@Override
		public void run() {
			if (app.battery != null) {

				int batt[] = app.battery.battStatsCombined();
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
