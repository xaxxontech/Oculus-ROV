package oculus;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class State {

	private Settings settings = new Settings();

	public static final String user = "user";
	public static final String logintime = "logintime";
	public static final String userisconnected = "userisconnected";
	public static final String reboot = "reboot";
	public static final String developer = "developer";
	public static final String serialport = "serialport";
	public static final String lightport = "lightport";
	public static final String target = "target";
	public static final String boottime = "boottime";
	public static final String batterylife = "batterylife";
	public static final String batterystatus = "batterstatus";

	//public static final String sonarenabled = "sonarenabled";
	//public static final String sonardistance = "sonardistance";
	//public static final String sonardebug = "sonardebug";
	public static final String sonar = "sonar";

	public static final String sonarback = "sonarback";
	public static final String sonarright = "sonarright";
	public static final String sonarleft = "sonarleft";
	
	public static final String motionenabled = "motionenabled";
	public static final String dockdensity = "dockdensity";
	public static final String dockxpos = "dockxpos";
	public static final String dockypos = "dockypos";
	public static final String externaladdress = "externaladdress";
	public static final String localaddress = "localaddress";
	public static final String autodocktimeout = "autodocktimeout";
	public static final String autodocking = "autodocking";
	public static final String docking = "docking";
	public static final String dockxsize = "dockxsize";	
	public static final String dockysize = "dockysize";
	public static final String dockstatus = "dockstatus";
	public static final String timeout = "timeout";
	public static final String losttarget = "losttarget";
	public static final String docked = "docked";
	public static final String undocked = "undocked";
	public static final String undock = "undock";
//	public static final String holdservo = "holdservo";
	public static final String unknown = "unknown";	
	public static final String frameGrabBusy = "frameGrabBusy";

	public static final long ONE_DAY = 86400000;
	public static final long ONE_MINUTE = 60000;
	public static final long TWO_MINUTES = 60000;
	public static final long FIVE_MINUTES = 300000;
	public static final long TEN_MINUTES = 600000;
	public static final int ERROR = -1;




	/** notify these on change events */
	public Vector<Observer> observers = new Vector<Observer>();
	
	/** reference to this singleton class */
	private static State singleton = null;


	/** properties object to hold configuration */
	private Properties props = new Properties();
	
	public static State getReference() {
		if (singleton == null) {
			singleton = new State();
		}
		return singleton;
	}

	/** private constructor for this singleton class */
	private State() {
		props.put(boottime, String.valueOf(System.currentTimeMillis()));
		// not required
		// props.put(userisconnected, false);
		props.put(localaddress, Util.getLocalAddress());
		new Thread(new Runnable() {
			@Override
			public void run() {
				String ip = null; 
				while(ip==null){
					ip = Util.getExternalIPAddress();
					if(ip!=null)
						State.getReference().set(State.externaladdress, ip);
					else Util.delay(500);
				}
			}
		}).start();
	}
	
	/** */
	public Properties getProperties(){
		return (Properties) props.clone();
	}

	/** */
	public void addObserver(Observer obs){
		observers.add(obs);
	}
	
	/** test for string equality. any nulls will return false */ 
	public boolean equals(final String a, final String b){
		String aa = get(a);
		if(aa==null) return false; 
		if(b==null) return false; 
		if(aa.equals("")) return false;
		if(b.equals("")) return false;
		
		return aa.equalsIgnoreCase(b);
	}
	
	/** test for string equality against config file. any nulls will return false */ 
	public boolean equalsSetting(final String a, final String b){
		String aa = get(a);
		if(aa==null) return false; 
		if(aa.equals("")) return false;
				
		String bb = settings.readSetting(b);
		if(bb==null) return false;
		if(bb.equals("")) return false;
		
		return aa.equalsIgnoreCase(bb);
	}
	
	
	/** @param file is the properties file to configure the framework
	public void parseFile(final String path) {

		if (path == null) {
			System.err.println("called parseConfigFile() with null arg!");
			return;
		}

		try {

			FileInputStream propFile = new FileInputStream(path);
			props.load(propFile);
			propFile.close();

			// now be sure no white space is in any properties!
			Enumeration<Object> keys = props.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				String value = (String) props.get(key);
				props.put(key, value.trim());
			}
		} catch (Exception e) {
			System.out.println("can't parse config file [" + path + "], terminate.");
			return;
		}
	}*/
	
	/** debug */
	public void dump(){
		Enumeration<Object> keys = props.keys();
		while(keys.hasMoreElements()){
			String key = (String) keys.nextElement();
			String value = (String) props.getProperty(key);			
			System.out.println(key + " = " + value);
		}
	}
	
	/** */
	@Override
	public String toString(){
		String str = new String();
		Enumeration<Object> keys = props.keys();
		while(keys.hasMoreElements()){
			String key = (String) keys.nextElement();
			String value = (String) props.getProperty(key);					
			str += key + " : " + value + "\r\n";
		}	
		return str;
	}
	
	/**
	 * @param props is the list of values to send to disk 
	 * @param path, is the file to write the state value pairs too
 
	public static void writeFile(Properties props, String path){
		
		System.out.println("state writing to: " + path);
	
		try {
			
			FileWriter out = new FileWriter(path);
			
			//out.write("state as of: " + new Date().toString() + "\n");
			//out.write("state writing to: " + path + "\n");
			
			Enumeration<Object> keys = props.keys();
			while(keys.hasMoreElements()){
				String key = (String) keys.nextElement();
				String value = (String) props.getProperty(key);
				
				System.out.println(key + " " + value);
				out.write(key + " " + value + "\r\n");
			}
 			
			out.close();

		} catch (Exception e) {
			e.printStackTrace();
		} 		
	}*/
	
	/** Put a name/value pair into the configuration */
	public synchronized void set(final String key, final String value) {
		try {
			props.put(key.trim(), value.trim());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		for(int i = 0 ; i < observers.size() ; i++)
			observers.get(i).updated(key.trim());	
		
		if(State.getReference().getBoolean(State.developer))
			System.out.println("__state: " + key + " = " + value);
	}

	/** Put a name/value pair into the config */
	public void set(final String key, final long value) {
		set(key, Long.toString(value));
	}
	
	/** */
	public synchronized String get(final String key) {

		String ans = null;
		try {

			ans = props.getProperty(key.trim());

		} catch (Exception e) {
			System.err.println(e.getStackTrace());
			return null;
		}

		return ans;
	}
	
	/** */
	public synchronized boolean getBoolean(String key) {
		key = key.toLowerCase();
		boolean value = false;
		try {

			value = Boolean.parseBoolean(get(key));

		} catch (Exception e) {
			if(key.equals("yes")) return true;
			else return false;
		}

		return value;
	}

	/** */
	public synchronized int getInteger(final String key) {

		String ans = null;
		int value = ERROR;

		try {

			ans = get(key);
			value = Integer.parseInt(ans);

		} catch (Exception e) {
			return ERROR;
		}

		return value;
	}
	
	/** */
	public synchronized long getLong(final String key) {

		String ans = null;
		long value = ERROR;

		try {

			ans = get(key);
			value = Long.parseLong(ans);

		} catch (Exception e) {
			return ERROR;
		}

		return value;
	}
	
	/** @return the ms since last boot */
	public long getUpTime(){
		return System.currentTimeMillis() - getLong(boottime);
	}
	
	/** @return the ms since last user log in */
	public long getLoginSince(){
		return System.currentTimeMillis() - getLong(logintime);
	}

	/** */
	public synchronized void set(String key, boolean b) {
		if(b) props.put(key, "true");
		else props.put(key, "false");
	}

	/** */ 
	public synchronized void delete(String key) {
		props.remove(key);
		for(int i = 0 ; i < observers.size() ; i++)
			observers.get(i).updated(key);	
	}
	

	//  public static final String enable = "enable";
    //	public static final String disable = "disable";
	//  private boolean locked = false
	
	
	/*
	public synchronized void lock() {
		locked = true;
	}

	public synchronized void unlock() {
		locked = false;
	}

	public synchronized boolean isLocked() {
		return locked;
	}*/
}