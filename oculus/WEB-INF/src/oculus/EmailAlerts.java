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
	
	// Application.getRefrence();
	
	public EmailAlerts(Application app){
		this.app = app;
	}
	
	@Override
	public void run() {
		
		BatteryLife battery = new BatteryLife();
		
		try {
			// wait for setup 
			//Util.delay(DELAY);
			Thread.sleep(DELAY);
			
			app.message("starting email alert thread", null, null);
			
			boolean alive = true;
			while (alive) {
				
				int batt[] = battery.battStatsCombined(); 
				String life = Integer.toString(batt[0]);
				int s = batt[1];
				String status = Integer.toString(s); // in case its not 1 or 2
				
				app.message("email thread checking: " + "battery "+life+"%,"+status, null, null);
	
				if (battery.batteryStatus() < WARN_LEVEL) {
					app.message("battery low, sending email", null, null);
	
					if (!SendMail.sendMessage("Oculus Message", "battery "+life+"%,"+status)){
					
						app.message("cound not send battery warning email", null, null);
					
						log.error("turning off email alerts");
						alive = false;
					}
					
					// don't flood
					//Util.delay(DELAY*10);
					Thread.sleep(DELAY*10);
				}
				//Util.delay(DELAY);
				Thread.sleep(DELAY);
			}
        } catch (Exception e) {e.printStackTrace(); }
	}

		
	/** test driver 
	public static void main(String[] args) {

		// must kill with control C
		new EmailAlerts().start();
	} */
}
