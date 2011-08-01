package oculus;

import oculus.Observer;
import oculus.State;

public class DockingObserver implements Observer {

	// private static Logger log = Red5LoggerFactory.getLogger(SonarObserver.class, "oculus");
	private State state = State.getReference();
	private Application app = null;
	
	/** try to configure FTP parameters */
	public DockingObserver(Application a){
		app = a;
		
		System.out.println("docking observer started");

		// register for state changes
		state.addObserver(this);
	}

	@Override
	public void updated(final String key) {
		
		if( state.getBoolean(State.dockstatus) || state.getBoolean(State.losttarget) 
				|| state.getBoolean(State.timeout)){
				
		final String status = state.get(key);
		System.out.println("dk: " + key + " " + status);

		if (state.getBoolean(State.autodocktimeout)){
			state.delete(State.autodocktimeout);
			state.dump();
		}
		
		}
		
	/*	if(status.equals(State.timeout)){
			
			app.playerCallServer(playerCommands.nudge, "left");
			Util.delay(3000);
			app.playerCallServer(playerCommands.autodock, "go");
			
		}
		*/
		
		
		//}
	}
}