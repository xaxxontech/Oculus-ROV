package developer;

import oculus.Application;
import oculus.Observer;
import oculus.State;

/** Manage autodocking events like timeout and losttarget */
public class DockingObserver implements Observer {

	// private static Logger log =
	// Red5LoggerFactory.getLogger(SonarObserver.class, "oculus");
	private State state = State.getReference();
	private Application app = null;

	private long start = 0;
	private long end = 0;
	private boolean docking = false;

	/** register for state changes */
	public DockingObserver(Application a) {
		app = a;
		System.out.println("docking observer started...");
		state.addObserver(this);
	}

	@Override
	public void updated(final String key) {

		if (state.getBoolean(State.autodocking)) {
			if (!docking) {
				docking = true;
				System.out.println("__started autodocking...");
				start = System.currentTimeMillis();
			}
		}

		if (state.get(State.dockstatus) != null) {
			if (state.get(State.dockstatus).equals(State.docked)) {

				// System.out.println("-.--- done docking");
				end = System.currentTimeMillis();
				
				if((end - start)>State.TEN_MINUTES){
					System.out.println("docking booting still?");
					return;
				}
				
				app.message("docking took " + ((end - start) / 1000) + " seconds" , null, null);
				System.out.println("docking took: " + ((end - start) / 1000) + " seconds");
				docking = false;
				state.dump();

			}
		}
	}

	/*
	 * @Override public void removed(String key) {
	 * System.out.println("__dock observer remove: " + key); }
	 */

}