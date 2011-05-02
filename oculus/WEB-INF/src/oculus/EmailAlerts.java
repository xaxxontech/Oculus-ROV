package oculus;

import java.util.TimerTask;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

public class EmailAlerts {

	private static Logger log = Red5LoggerFactory.getLogger(EmailAlerts.class,"oculus");

	// how low of battery to warm user with email
	public static final int WARN_LEVEL = 65;

	// how often to check, ten minutes 
	public static final int DELAY = 600; //000;

	// call back to message window
	private Application app = null;
	
	// set to "true" is config file
	private final boolean debug = new Settings().getBoolean("developer");
	private final boolean alerts = new Settings().getBoolean("emailalerts");

	public java.util.Timer timer = new java.util.Timer();

	public EmailAlerts(Application app) {
		this.app = app;
		if (alerts && app.batterypresent){
			timer.scheduleAtFixedRate(new Task(), 5000, DELAY);
			System.out.println("starting email alerts...");
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

						if (new SendMail().sendMessage("Oculus Message", 
								"battery " + Integer.toString(life)
										+ "% and is draining! ")) {

							// TODO: trigger auto dock
							// app.autodock();

							// only send single email
							timer.cancel();

						} else {
							app.message("cound not send battery warning email", null, null);
							log.error("failed to send warning email, check settings");
						}
					}
				}
			}
		}
	}
}
