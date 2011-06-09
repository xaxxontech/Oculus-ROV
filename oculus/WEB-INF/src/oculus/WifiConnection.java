/* 
 * unused, was causing lockup over time
 */

package oculus;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import com.jacob.com.EnumVariant;
import com.jacob.com.Variant;
import java.io.*; 

public class WifiConnection {
	ActiveXComponent waxWMI;
	
	public WifiConnection() {
		String wconnectStr = String.format("winmgmts:\\\\%s\\root\\WMI", "localhost"); 
		waxWMI = new ActiveXComponent(wconnectStr);
	}
	
	public String wifiSignalStrength() { 
		int result = 0;
		String wquery = "Select * from MSNdis_80211_ReceivedSignalStrength where active=true";  //works for xp only
		Variant vCollection = waxWMI.invoke("ExecQuery", new Variant(wquery));
 		EnumVariant enumVariant = new EnumVariant(vCollection.toDispatch());
		Dispatch item = null;
		try {
			while (enumVariant.hasMoreElements()) { 
				item = enumVariant.nextElement().toDispatch();
				result = Dispatch.call(item,"NDIS80211ReceivedSignalStrength").getInt();
				result = (result + 95)*10/6;
			}
		}
		catch (Exception e) { // MSNdis_80211_ReceivedSignalStrength returned null, SO <= XPsp2  
			try {
				Process p = Runtime.getRuntime().exec("cmd.exe /c netsh wlan show interfaces");
				//p.waitFor(); // causes hang...
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line = reader.readLine();
				while (line != null) {
					String str[] = line.split("\\s+");
					if (str.length>=4 ) {
						if (str[1].equals("Signal")) {
							result = Integer.parseInt((str[3].split("%"))[0]);
						}
					}
					
					line = reader.readLine();
				}
			}
			catch(Exception z) { result=0; } 
		}
		if (result < 0) { result = 0; }
		if (result > 100) { result = 100; }
		return result+"%";
	}
}
