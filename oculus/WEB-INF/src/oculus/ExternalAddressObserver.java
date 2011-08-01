package oculus;

import java.util.Timer;
import java.util.TimerTask;

import oculus.Application;
import oculus.Observer;
import oculus.State;

/** look up external address */
public class ExternalAddressObserver implements Observer {

	// private static Logger log = Red5LoggerFactory.getLogger(SonarObserver.class, "oculus");
	private Application app = null;
	private State state = State.getReference();
	private java.util.Timer timer = new Timer();

	/** register for state changes */
	public ExternalAddressObserver(Application a) {
		app = a;
		state.addObserver(this);
		timer.scheduleAtFixedRate(new UpdateTask(), 0, State.ONE_DAY);
	}

	@Override
	public void updated(final String key) {
		if (key.equals(State.externaladdress))
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

	@Override
	public void removed(String key) {}
}