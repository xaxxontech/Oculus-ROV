package oculus;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

public class EmailAlerts extends Thread {

	private static Logger log = Red5LoggerFactory.getLogger(EmailAlerts.class, "oculus");

	// how low of battery to warm user with email
	public static final int WARN_LEVEL = 30;

	// how often to check
	public static final int DELAY = 60000;

	// call back to message window
	private Application app = null;

	public EmailAlerts(Application app) {
		this.app = app;
	}

	@Override
	public void run() {
		Util.delay(DELAY);
		app.message("starting email alert thread", null, null);
		boolean alive = true;
		while (alive) {

			if (app.battery != null) {

				int batt[] = app.battery.battStatsCombined();
				String life_str = Integer.toString(batt[0]);
				int life = batt[0];
				int status = batt[1];

				// if draining only 
				if (status == 1) {

					// debug only, remove 
					app.message("email thread checking: " + "battery " + life_str+ "%", null, null);
					if (life < WARN_LEVEL) {
					
						app.message("battery low, sending email", null, null);
						if (!new SendMail().sendMessage("Oculus Message",
								"battery " + Integer.toString(life) + "% and is draining")) {

							app.message("<font color=\"purple\">cound not send battery warning email</font>", null, null);
							log.error("failed to send, turning off email alerts");
							alive = false;
						}

						// only send single email
						alive = false;
						// don't flood
						// Util.delay(DELAY * 10);
					}
				}
			}
			Util.delay(DELAY);
			// TODO: add condition to only send ONE email if below life% threshold?
		}
	}
}
