package developer.sonar;

import oculus.Application;
import oculus.Observer;
import oculus.State;
import oculus.commport.AbstractArduinoComm;

/** Stop the robot before hitting the wall */
public class SonarSteeringObserver implements Observer {

	// private static Logger log = Red5LoggerFactory.getLogger(SonarObserver.class, "oculus");
	
	private static final int TOO_CLOSE = 42; // cm 
	private State state = State.getReference();
	//private java.util.Timer timer = new Timer();
	// private Settings settings = new Settings();
	private Application app = null;
	private AbstractArduinoComm comm = null;

	/** register for state changes */
	public SonarSteeringObserver(Application a, AbstractArduinoComm port) {
		app = a;
		comm = port;
		state.addObserver(this);

		// refresh on timer too
		// timer.scheduleAtFixedRate(new SonarTast(), State.ONE_MINUTE, 1800);
	}
	
	@Override
	public void updated(final String key) {
		if (key.equals(State.sonarright)) {
			
			if (state.getBoolean(State.autodocking)) return;

			final int value = state.getInteger(key);
			
			System.out.println("obv_sonar: " + key + " = " + value);	
			
			if (!comm.movingforward) return;
			
			if (value < TOO_CLOSE) { 
				comm.stopGoing();
				app.message("carefull, sonar is: " + value, null, null);
			}
		}
	}
}