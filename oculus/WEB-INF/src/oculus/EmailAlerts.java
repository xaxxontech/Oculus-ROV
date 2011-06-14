package oculus;

import java.util.Timer;
import java.util.TimerTask;

/** */
public class EmailAlerts {

	// how low of battery to warm user with email
	// any lower and my dell will go into 
	// low power mode, need time to park it   
	public static final int WARN_LEVEL = 35;

	// how often to check, ten minutes 
	public static final long DELAY = State.FIVE_MINUTES;

	// call back to message window
	private Application app = null;
	private Timer timer = new java.util.Timer();

	// configuration 
	private Settings settings = new Settings();
	private final boolean debug = settings.getBoolean(Settings.developer);
	private final boolean alerts = settings.getBoolean(Settings.emailalerts);
	private BatteryLife life = BatteryLife.getReference();
	
	/** Constructor */
	public EmailAlerts(Application parent) {
		app = parent;
		
		if (alerts){
			timer.scheduleAtFixedRate(new Task(), State.ONE_MINUTE, DELAY);
			if(debug) System.out.println("starting email alerts...");
		}
	}

	/** run on timer */
	private class Task extends TimerTask {
		@Override
		public void run() {
			
			// not needed? 
			if (life.batteryPresent()) {

				int batt[] = life.battStatsCombined();
				if(batt == null) {
					System.out.println("batery not ready, email alerts");
					return;
				}
				
				// String lifestr = Integer.toString(batt[0]);
				int life = batt[0];
				int status = batt[1];
				
				// if draining only
				if (status == 1) {

					//if (debug)
						//app.message("checking battery: " + "battery " + lifestr + "%", null, null);

					if (life < WARN_LEVEL) {
		
						app.message("battery low, sending email", null, null);
						
						String msg = "The battery " + Integer.toString(life) 
						+ "% and is draining!"; 
										
						// add the link back to the user screen 
						msg += "\n\nPlease find the dock, log in here: http://" 
							+ Util.getExternalIPAddress() 
							+ ":" + settings.readRed5Setting("http.port") 
							+ "/oculus/";
						
						// send email 
						new SendMail("Oculus Message", msg, app); 

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
