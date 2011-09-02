package oculus;

import java.io.FileWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class State {

	private Settings settings = new Settings();

	/** these settings must be available in basic configuration */
	public enum factoryDefaults {
		
		skipsetup, speedslow, speedmed, steeringcomp, camservohoriz, camposmax, camposmin, nudgedelay, 
		docktarget, vidctroffset, vlow, vmed, vhigh, vfull, vcustom, vset, maxclicknudgedelay,
		clicknudgedelaymomentumfactor, clicknudgemomentummult, maxclickcam,
		volume, mute_rov_on_move, videoscale;
		
		@Override
		public String toString() {
			return super.toString();
		}
	}
	
	/** place extensions to settings here */
	public enum optionalSettings {

		emailalerts, emailaddress, emailpassword, developer, reboot, loginnotify, holdservo;
		
		@Override
		public String toString() {
			return super.toString();
		}
	}
	
	
	//  public static final String enable = "enable";
    //	public static final String disable = "disable";
	//  private boolean locked = false
	
	public static final String user = "user";
	public static final String logintime = "logintime";
	public static final String userisconnected = "userisconnected";
	public static final String reboot = "reboot";
	public static final String developer = "developer";
	public static final String serialport = "serialport";
	public static final String lightport = "lightport";
	public static final String target = "target";
	public static final String boottime = "boottime";

	public static final String sonarenabled = "sonarenabled";
	public static final String sonardistance = "sonardistance";
	public static final String sonardebug = "sonardebug";
	public static final String sonar = "sonar";

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
	public static final String holdservo = "holdservo";
	public static final String unknown = "unknown";	

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
		props.put(userisconnected, false);
		props.put(localaddress, Util.getLocalAddress());
		new Thread(new Runnable() {
			@Override
			public void run() {
				String ip = Util.getExternalIPAddress();
				if(ip!=null) State.getReference().set(State.externaladdress, ip);
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
		
		// for(int i = 0 ; i < observers.size() ; i++)
			// System.out.println("_+__[" + i + "] " + observers.get(i).getClass().getName());
	}
	
	
	
	/** get basic settings */
	public static Properties createDeaults(){
		Properties config = new Properties();
		config.setProperty(factoryDefaults.skipsetup.toString(), "false");
		config.setProperty(factoryDefaults.speedslow.toString(), "115");
		config.setProperty(factoryDefaults.speedmed.toString(), "180");
		config.setProperty(factoryDefaults.steeringcomp.toString(), "128");
		config.setProperty(factoryDefaults.camservohoriz.toString(), "68");
		config.setProperty(factoryDefaults.camposmax.toString(), "89");
		config.setProperty(factoryDefaults.camposmin.toString(), "58");
		config.setProperty(factoryDefaults.nudgedelay.toString(), "150");
		config.setProperty(factoryDefaults.docktarget.toString(), "1.194_0.23209_0.17985_0.22649_129_116_80_67_-0.045455");
		config.setProperty(factoryDefaults.vidctroffset.toString(), "0");
		config.setProperty(factoryDefaults.vlow.toString(), "320_240_4_85");
		config.setProperty(factoryDefaults.vmed.toString(), "320_240_8_95");
		config.setProperty(factoryDefaults.vhigh.toString(), "640_480_8_85");
		config.setProperty(factoryDefaults.vfull.toString(), "640_480_8_95");
		config.setProperty(factoryDefaults.vcustom.toString(), "1024_768_8_85");
		config.setProperty(factoryDefaults.vset.toString(), "vmed");
		config.setProperty(factoryDefaults.maxclicknudgedelay.toString(), "580");
		config.setProperty(factoryDefaults.clicknudgedelaymomentumfactor.toString(), "0.7");
		config.setProperty(factoryDefaults.clicknudgemomentummult.toString(), "0.7");
		config.setProperty(factoryDefaults.maxclickcam.toString(), "14");
		config.setProperty(factoryDefaults.volume.toString(), "20");
		config.setProperty(factoryDefaults.mute_rov_on_move.toString(), "true"); // was "yes"
		config.setProperty(factoryDefaults.videoscale.toString(), "100");
		return config;
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
	
	/**
	 * debug
	 */
	public void dump(){
		Enumeration<Object> keys = props.keys();
		while(keys.hasMoreElements()){
			String key = (String) keys.nextElement();
			String value = (String) props.getProperty(key);			
			System.out.println(key + " = " + value);
		}
	}
	
	/**
	 * @param props is the list of values to send to disk 
	 * @param path, is the file to write the state value pairs too
	 */ 
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
	}
	
	/** Put a name/value pair into the configuration */
	public synchronized void set(final String key, final String value) {
		try {
			props.put(key.trim(), value.trim());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		for(int i = 0 ; i < observers.size() ; i++)
			observers.get(i).updated(key.trim());	
	}

	/** 
	private void notifyObservers(String updated) {
		for(int i = 0 ; i < observers.size() ; i++)
			observers.get(i).updated(updated);
	}*/

	/** Put a name/value pair into the config */
	public /* synchronized */ void set(final String key, final long value) {
		//try {
			set(key, Long.toString(value));
			// props.put(key.trim(), Long.toString(value));
			// notifyObservers(key.trim());
	//	} catch (Exception e) {
	//		e.printStackTrace();
	//	}
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
		//System.out.println("_+___state.delete(): " + key);
		//for(int i = 0 ; i < observers.size() ; i++)
		//	observers.get(i).updated(key.trim());	
		
		props.remove(key);
	}
	
	
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