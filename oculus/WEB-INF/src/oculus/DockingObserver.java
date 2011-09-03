package oculus;

import oculus.Observer;
import oculus.State;

/** Manage autodocking events like timeout and losttarget */
public class DockingObserver implements Observer {

	// private static Logger log = Red5LoggerFactory.getLogger(SonarObserver.class, "oculus");
	private State state = State.getReference();
	private Application app = null;
	
	private long start = 0;
//	private long end = 0;
	
	/** register for state changes */
	public DockingObserver(Application a){
		app = a;	
		// System.out.println("docking observer started");
		state.addObserver(this);
	}

	@Override
	public void updated(final String key) {
		
	if (key.equals(State.dockstatus) || key.equals(State.timeout) || 
	    key.equals(State.autodocktimeout) || key.equals(State.losttarget)
	    ||  key.equals(State.autodocking) ){
				
			final String status = state.get(key);
			System.out.println("_.__dock observer: " + key + " " + status);
	
			if(state.getBoolean(State.autodocking)){
				
				System.out.println("_.__started autodock");
				start = System.currentTimeMillis();
				
			}
			
			if (state.getBoolean(State.losttarget)){
				app.message("_.__losttarget has been managed?", null, null);
				state.delete(State.losttarget);
				state.dump();
			}
	
		}
	}
	
	/*
	@Override
	public void removed(String key) {
		System.out.println("__dock observer remove: " + key);
	}*/
	
	
}