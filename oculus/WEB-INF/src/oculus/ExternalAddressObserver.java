package oculus;

import java.util.Timer;
import java.util.TimerTask;

import oculus.Application;
import oculus.Observer;
import oculus.State;

/**
 * Manage FTP configuration and connections. Start new threads for each FTP
 * transaction
 * 
 * @author <a href="mailto:brad.zdanivsky@gmail.com">Brad Zdanivsky</a>
 */
public class ExternalAddressObserver implements Observer {

	// private static Logger log =
	// Red5LoggerFactory.getLogger(SonarObserver.class, "oculus");
	private Application app = null;
	private State state = State.getReference();
	private java.util.Timer timer = new Timer();

	/** try to configure FTP parameters */
	public ExternalAddressObserver(Application a) {

		app = a;

		// System.out.println("external address observer started");

		// register for state changes
		state.addObserver(this);
		timer.scheduleAtFixedRate(new UpdateTask(), 0, State.ONE_DAY);
	}

	@Override
	public void updated(final String key) {

		if (!key.equals(State.externaladdress)) return;
		
		//System.out.println("ip: " + state.get(State.externaladdress));
		app.message("wan: " + state.get(State.externaladdress), null, null);
		app.populateSettings();

	}

	/** keep checking */
	private class UpdateTask extends TimerTask {

		@Override
		public void run() {

			String ip = Util.getExternalIPAddress();
			state.set(State.externaladdress, ip);

		}
	}
}