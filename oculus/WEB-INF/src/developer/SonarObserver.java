package developer;


//import java.util.Date;
//import java.util.Timer;
//mport java.util.TimerTask;

import oculus.Application;
import oculus.ArduinoCommDC;
import oculus.Observer;
import oculus.Settings;
import oculus.State;
import oculus.Util;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

/**
 * Manage FTP configuration and connections. Start new threads for each FTP transaction
 * 
 * @author <a href="mailto:brad.zdanivsky@gmail.com">Brad Zdanivsky</a>
 */
public class SonarObserver implements Observer {
	
	// private static Logger log = Red5LoggerFactory.getLogger(SonarObserver.class, "oculus");
	private State state = State.getReference();
	//private java.util.Timer timer = new Timer();
	//private Settings settings = new Settings();
	//private boolean configured = true;
	private Application app = null;
	private ArduinoCommDC comm = null;
	
	/** try to configure FTP parameters */
	public SonarObserver(Application a, ArduinoCommDC port) {
		app = a;
		comm = port;
		
		System.out.println("_+_sonar observer started");
		//if(configured){
		
			// register for state changes
			state.addObserver(this);
			
			// refresh on timer too 
			// timer.scheduleAtFixedRate(new FtptTask(), State.ONE_MINUTE, State.FIVE_MINUTES);
		//}
	}
	

	@Override
	public void updated(final String key) {
		
		if(! (key.equalsIgnoreCase(State.sonar) || key.equalsIgnoreCase(State.sonardistance))) return;
		
		final String value = state.get(key);
		if(value!=null){

			System.out.println("___sonar: " + key + " = " + value);
			if( ! state.getBoolean(State.autodocking)){
				if((comm.movingforward) && (state.getInteger(State.sonardistance) < 90))
					app.playerCallServer(Application.playerCommands.move, "stop");
			}
		}
		
	}
	
	/** run on timer 
	private class SonarTask extends TimerTask {
		
		@Override
		public void run() {
			
			app.message("sonar task update to: " , null, null);

		}
	}*/
}