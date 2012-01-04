package oculus;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import com.jacob.com.EnumVariant;
import com.jacob.com.Variant;

public class BatteryLife {
	
	/**
	 * Determine how much battery life is left (in percent).
	 * 
	 * 
	 * [CA] originally found here: http://www.dreamincode.net/code/snippet3300.htm
	 */

	private String host;
	private String connectStr;
	private String query; 
	private ActiveXComponent axWMI;
	
	private static final String os  = System.getProperty("os.name"); 
	
	private boolean battcharging = false;
	private boolean batterypresent = false;
	private static Application app = null;
	private static BatteryLife singleton = null;
	private State state = State.getReference();


	/**
	 * @return a reference to this singleton class 
	 */
	public static BatteryLife getReference() {
		if (singleton  == null) {
			singleton = new BatteryLife();
		}
		return singleton;
	}

	/**
	 * @param parent this the multi threaded red5 application to call back 
	 */
	public void init(Application parent){
		
		System.out.println("battery init...");
			
		if(app == null){
			
			// only initialize once 
			app = parent;	
			
			if(os.startsWith("windows")){
						
				// Technically you should be able to connect to other hosts, but it takes setup
				host = "localhost"; 
				connectStr = String.format("winmgmts:\\\\%s\\root\\CIMV2", host);
				query = "Select * from Win32_Battery"; 
				axWMI = new ActiveXComponent(connectStr);
		
			} else /* if(.equals("linux")*/ {
				
				System.out.println("linux battery... ");
				
				// cat /proc/acpi/battery/BAT0/state 
				String result[] = Util.systemCallBlocking(
						"cat /proc/acpi/battery/BAT0/state | grep \"present\"").split(":");
			
				if(result[1].equals("yes")) batterypresent = true;			
				
			}
		} 
	}
	
	/** 
	 * private constructor, definition of singleton. 	  
	 */
	private BatteryLife() {}
	
	public boolean batteryPresent(){
		if( batteryStatus() == 999 ) batterypresent = false; 
		else batterypresent = true; 
			
		return batterypresent;
	}
	
	public boolean batteryCharging(){
		return battcharging;
	}
	
	/** threaded update, will set values in application via call back */
	public void battStats() { 
		
		if(app == null){
			System.out.println("app not yet configured");
			return;
		}
		
		if(batterypresent == false){
			System.out.println("no battery found");
			return;
		}
		
		new Thread(new Runnable() {
			public void run() {
			
				if (batterypresent == false) {
					System.out.println("no batery found in thread");
					return;
				}
				
				if ( ! state.equals(State.dockstatus, State.docking)){
								
					int batt[] = battStatsCombined();
					String life = Integer.toString(batt[0]);
					int s = batt[1];
					String status = Integer.toString(s); // in case its not 1 or 2
					String str;
					if (s == 1) {
						status = "draining";
						str = "battery " + life + "%," + status;
						state.set(State.batterystatus, status);
						if (!state.getBoolean(State.motionenabled)) {
							state.set(State.motionenabled, true);
							str += " motion enabled";
						}
						if (! state.equals(State.dockstatus, State.undocked)) {
							state.set(State.dockstatus, State.undocked);
							str += " dock un-docked";
						}
						battcharging = false;
						app.message(null, "multiple", str);
					}
					if (s == 2) {
						status = "CHARGING";
						if (life.equals("99") || life.equals("100")) {
							status = "CHARGED";
						}
						battcharging = true;
						str = "battery " + life + "%," + status;
						if (state.get(State.dockstatus) == null) {
							state.set(State.dockstatus, State.docked);
							str += " dock docked";
						}
						app.message(null, "multiple", str);
						state.set(State.batterystatus, "charging");
					}						
				
					state.set(State.batterylife, life);
				}
			}
		}).start();
	}
	
	
	/*
	public int chargeRemaining() {
		String host = "localhost"; //Technically you should be able to connect to other hosts, but it takes setup
		String connectStr = String.format("winmgmts:\\\\%s\\root\\CIMV2", host);
		String query = "Select * from Win32_Battery"; 
		int result = 999;
		
		ActiveXComponent axWMI = new ActiveXComponent(connectStr); 
		//Execute the query
		Variant vCollection = axWMI.invoke("ExecQuery", new Variant(query));
		
		//Our result is a collection, so we need to work though the collection.
		// (it is odd, but there may be more than one battery... think about multiple
		//   UPS on the system).
		EnumVariant enumVariant = new EnumVariant(vCollection.toDispatch());
		Dispatch item = null;
		while (enumVariant.hasMoreElements()) { 
			item = enumVariant.nextElement().toDispatch();
			int percentLife = Dispatch.call(item,"EstimatedChargeRemaining").getInt();
			result = percentLife;
		}
		return result;
	}
	*/

	
	/** @return the percentage of battery life, or 999 if no battery present */
	public int batteryStatus() {

		if(app == null){
			System.out.println("app not yet configured");
			return 999;
		}
	
		int result = 999;
		
		try {
			
			//Execute the query
			Variant vCollection = axWMI.invoke("ExecQuery", new Variant(query));
			
			//Our result is a collection, so we need to work though the collection.
			// (it is odd, but there may be more than one battery... think about multiple
			//   UPS on the system).
			EnumVariant enumVariant = new EnumVariant(vCollection.toDispatch());
			Dispatch item = null;
			while (enumVariant.hasMoreElements()) { 
				item = enumVariant.nextElement().toDispatch(); // throws errors sometimes
				int percentLife = Dispatch.call(item,"BatteryStatus").getInt();
				result = percentLife;
			}
			
		} catch (Exception e) {
			System.out.println(e.getMessage()); 
			// e.printStackTrace();
		}
	
		return result;
	}
	
	/**
	 * get battery info 
	 * 
	 * @return the charge remaining and status if found, null if not.  
	 */
	public int[] battStatsCombined() {

		if(app == null){
			System.out.println("app not yet configured");
			return null;
		}
		
		if(!batterypresent){
			System.out.println("no battery found");
			return null;
		}
	
		int[] result = { 999, 999 };
		Variant vCollection = axWMI.invoke("ExecQuery", new Variant(query));
		EnumVariant enumVariant = new EnumVariant(vCollection.toDispatch());
		Dispatch item = null;
		while (enumVariant.hasMoreElements()) { 
			item = enumVariant.nextElement().toDispatch();
			result[0] = Dispatch.call(item,"EstimatedChargeRemaining").getInt();
			result[1] = Dispatch.call(item,"BatteryStatus").getInt();
		}
		
		return result;
	}
}
