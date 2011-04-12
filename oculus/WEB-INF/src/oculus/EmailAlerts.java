package oculus;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

public class EmailAlerts extends Thread {

	private static Logger log = Red5LoggerFactory.getLogger(EmailAlerts.class, "oculus");

	// how low of battery to warm user with email
	public static final int WARN_LEVEL = 30;

	// how often to check
	public static final int DELAY = 120000;

	@Override
	public void run() {

		BatteryLife battery = new BatteryLife();
		while (true) {

			if (battery.batteryStatus() < WARN_LEVEL) {
				log.info("battery low, sending battery warning email");

				if (!SendMail.sendMessage("Oculus Message", "battery low, now at: " + battery.batteryStatus()))
					log.debug("cound not send battery warming email");
			}

			try {
				Thread.sleep(DELAY);
			} catch (InterruptedException e) {
				log.error("Thread delay error, closing.");
				break;
			}
		}
	}

	/** test driver */
	public static void main(String[] args) {

		// must kill with control C
		new EmailAlerts().start();
	}
}
