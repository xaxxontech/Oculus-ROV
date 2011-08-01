package developer.sonar;

import oculus.Application;
import oculus.ArduinoCommDC;
import oculus.Observer;
import oculus.State;

/**
 * Stop the robot before hitting the wall 
 */
public class SonarSteeringObserver implements Observer {

	// private static Logger log = Red5LoggerFactory.getLogger(SonarObserver.class, "oculus");
	private State state = State.getReference();
	// private java.util.Timer timer = new Timer();
	// private Settings settings = new Settings();
	private Application app = null;
	private ArduinoCommDC comm = null;

	/** try to configure FTP parameters */
	public SonarSteeringObserver(Application a, ArduinoCommDC port) {
		app = a;
		comm = port;
		
		System.out.println("_+_sonar observer started");

		// register for state changes
		state.addObserver(this);

		// refresh on timer too
		// timer.scheduleAtFixedRate(new FtptTask(), State.ONE_MINUTE,
		// State.FIVE_MINUTES);

	}

	@Override
	public void updated(final String key) {

		if (!key.equals(State.sonardistance)) return;
		if (state.getBoolean(State.autodocking)) return;
		if (!comm.movingforward) return;

		//final String value = state.get(key);
		//if (value != null) {

		//	System.out.println("___sonar: " + key + " = " + value);	
			if (state.getInteger(State.sonardistance) < 95)
				app.playerCallServer(Application.playerCommands.move, "stop");
			
		//}
	}

	@Override
	public void removed(String key) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * run on timer private class SonarTask extends TimerTask {
	 * 
	 * @Override public void run() {
	 * 
	 *           app.message("sonar task update to: " , null, null);
	 * 
	 *           } }
	 */
}