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
		
		if (key.equals(State.dockstatus) || 
		    key.equals(State.timeout) || 
		    key.equals(State.autodocktimeout) ||
		    key.equals(State.losttarget)){
				
			final String status = state.get(key);
			System.out.println("..dock observer: " + key + " " + status);
	
			if (key.equals(State.autodocktimeout)){
				app.message("_timeout has been managed?", null, null);
				state.delete(State.autodocktimeout);
				state.dump();
			}
			
		}
	}

	@Override
	public void removed(String key) {
		System.out.println("__dock observer remove: " + key);
	}
}