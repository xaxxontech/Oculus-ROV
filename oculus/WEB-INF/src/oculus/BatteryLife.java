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
	private boolean battcharging = false;
	boolean batterypresent = false;
	private static Application app = null;
	private Settings settings = new Settings();

	
	private static BatteryLife singleton = null;
	public static BatteryLife getReference() {
		if (singleton  == null) {
			singleton = new BatteryLife();
		}
		return singleton;
	}

	public static void init(Application parent){
		//if(app!=null) {
			System.out.println("battery init...");
		//	return;
		//}else{
			//System.out.println("battery can't re-init!");
			app = parent;
		//}
	}
	
	private BatteryLife() {
		host = "localhost"; //Technically you should be able to connect to other hosts, but it takes setup
		connectStr = String.format("winmgmts:\\\\%s\\root\\CIMV2", host);
		query = "Select * from Win32_Battery"; 
		axWMI = new ActiveXComponent(connectStr);
		
		if (((settings.readSetting("batterypresent")).toUpperCase()).equals("YES")) {
			batterypresent = true; 
		} else { 
			batterypresent = false; 
			app.motionenabled = true;
		}
	}
	
	public boolean batteryPresent(){
		return batterypresent;
	}
	
	public boolean batteryCharging(){
		return battcharging;
	}
	
	/** threaded update */
	public void battStats() { 
		
		if(app == null){
			System.out.println("not yet configured");
			return;
		}
		
		if(batterypresent == false){
			System.out.println("no battery found");
			return;
		}
		
		new Thread(new Runnable() {
			public void run() {
				// TODO: HHHHHHHHHHHHHHHHHHH
				if (batterypresent == true && !app.dockstatus.equals("docking")) {
					
					int batt[] = battStatsCombined();
					String life = Integer.toString(batt[0]);
					int s = batt[1];
					String status = Integer.toString(s); // in case its not 1 or 2
					String str;
					if (s == 1) {
						status = "draining";
						str = "battery "+life+"%,"+status;
						if (app.motionenabled== false) {
							app.motionenabled = true;
							str += " motion enabled";
						}
						if (!app.dockstatus.equals("un-docked")) {
							app.dockstatus = "un-docked";
							str += " dock un-docked";
						}
						battcharging = false;
						app.message(null, "multiple", str);
					}
					if (s == 2) {
						status = "CHARGING";
						// app.motionenabled = false ;
						if (life.equals("99") || life.equals("100")) {
							status = "CHARGED";
						}
						battcharging = true;
						str="battery "+life+"%,"+status;
						if (app.dockstatus.equals("")) {
							app.dockstatus = "docked";
							str += " dock docked";
						}
						app.message(null, "multiple", str);
					}			
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
	
	public int batteryStatus() {

		
		if(app == null){
			System.out.println("not yet configured");
			return 999;
		}
		
		if(batterypresent == false){
			System.out.println("no battery found");
			return 999;
		}
		
		int result = 999;
		
		//Execute the query
		Variant vCollection = axWMI.invoke("ExecQuery", new Variant(query));
		
		//Our result is a collection, so we need to work though the collection.
		// (it is odd, but there may be more than one battery... think about multiple
		//   UPS on the system).
		EnumVariant enumVariant = new EnumVariant(vCollection.toDispatch());
		Dispatch item = null;
		while (enumVariant.hasMoreElements()) { 
			item = enumVariant.nextElement().toDispatch();
			int percentLife = Dispatch.call(item,"BatteryStatus").getInt();
			result = percentLife;
		}
		return result;
	}
	
	public int[] battStatsCombined() {
	
		if(app == null){
			System.out.println("not yet configured");
			return null;
		}
		
		if(batterypresent == false){
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
			result [1] = Dispatch.call(item,"BatteryStatus").getInt();
		}
		return result;
	}
}
