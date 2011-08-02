package oculus;

import oculus.Observer;
import oculus.State;

/** Manage autodocking events like timeout and losttarget */
public class DockingObserver implements Observer {

	// private static Logger log = Red5LoggerFactory.getLogger(SonarObserver.class, "oculus");
	private State state = State.getReference();
	private Application app = null;
	
	/** register for state changes */
	public DockingObserver(Application a){
		app = a;	
		// System.out.println("docking observer started");
		state.addObserver(this);
	}

	@Override
	public void updated(final String key) {
		
	//	if (key.equals(State.dockstatus) || key.equals(State.timeout) || 
		//    key.equals(State.autodocktimeout) || key.equals(State.losttarget)){
				
			final String status = state.get(key);
			System.out.println("..dock observer: " + key + " " + status);
	
			if (key.equals(State.losttarget)){
				app.message("__losttarget has been managed?", null, null);
				state.delete(State.losttarget);
				state.dump();
			}
	
		//}
	}

	@Override
	public void removed(String key) {
		System.out.println("__dock observer remove: " + key);
	}
}