package oculus;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

public class EmailAlerts extends Thread {

	private static Logger log = Red5LoggerFactory.getLogger(EmailAlerts.class, "oculus");

	// how low of battery to warm user with email
	public static final int WARN_LEVEL = 90;

	// how often to check
	public static final int DELAY = 60000;

	// call back to message window 
	private Application app = null;
	
	// Application.getRefrence();
	
	public EmailAlerts(Application app){
		this.app = app;
	}
	
	@Override
	public void run() {
		
		BatteryLife battery = new BatteryLife();
		
		// wait for setup 
		Util.delay(DELAY);
		
		app.message("starting email alert thread", null, null);
		
		boolean alive = true;
		while (alive) {
			
			app.message("email thread checking: " + battery.hashCode(), null, null);

			if (battery.batteryStatus() < WARN_LEVEL) {
				app.message("battery low, sending email", null, null);

				if (!SendMail.sendMessage("Oculus Message", "battery low, now at: " + battery.batteryStatus())){
				
					app.message("<font color=\"red\">cound not send battery warming email</a>", null, null);
				
					log.error("turning off email alerts");
					
					alive = false;
				}
				
				// don't flood
				Util.delay(DELAY*10);
			}
			Util.delay(DELAY);
		}
	}

	
	
	
	/** test driver 
	public static void main(String[] args) {

		// must kill with control C
		new EmailAlerts().start();
	} */
}
