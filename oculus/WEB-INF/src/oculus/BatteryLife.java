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
	
	String host;
	String connectStr;
	String query; 
	ActiveXComponent axWMI;
	
	public BatteryLife() {
		host = "localhost"; //Technically you should be able to connect to other hosts, but it takes setup
		connectStr = String.format("winmgmts:\\\\%s\\root\\CIMV2", host);
		query = "Select * from Win32_Battery"; 
		axWMI = new ActiveXComponent(connectStr);
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
