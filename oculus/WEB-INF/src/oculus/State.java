package oculus;

import java.io.FileWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

///import org.red5.logging.Red5LoggerFactory;
///import org.slf4j.Logger;

/**
 * @author <a href="mailto:brad.zdanivsky@gmail.com">Brad Zdanivsky</a>
 */
public class State {

	// private static Logger log = Red5LoggerFactory.getLogger(State.class, "oculus");
	
	public static final String user = "user";
	public static final String logintime = "logintime";
	public static final String userisconnected = "userisconnected";
	public static final String reboot = "reboot";
	public static final String developer = "developer";
	public static final String serialport = "serialport";
	public static final String lightport = "lightport";
	//  public static final String enable = "enable";
    //	public static final String disable = "disable";
	public static final String boottime = "boottime";
	public static final String autodocking = "autodocking";
	public static final String docking = "docking";
	public static final String motionenabled = "motionenabled";
	
	// these in settings now, don't change often 
	//blic static final String notify = "notify";
	//public static final String emailalerts = "emailalerts";
	// public static final String developer = "developer";

	public static final String dockx = "dockx";
	public static final String docky = "docky";
	public static final String sonar = "sonar";

	
	public static final long ONE_DAY = 86400000;
	public static final long ONE_MINUTE = 60000;
	public static final long TWO_MINUTES = 60000;
	public static final long FIVE_MINUTES = 300000;
	public static final long TEN_MINUTES = 600000;
	public static final int ERROR = -1;
	
	// comand line interface 
	// MulticastChannel channel = MulticastChannel.getReference();

	/** reference to this singleton class */
	private static State singleton = null;
	
	/** properties object to hold configuration */
	private Properties props = new Properties();
	
	// private boolean locked = false;

	public static State getReference() {
		if (singleton == null) {
			singleton = new State();
		}
		return singleton;
	}

	/** private constructor for this singleton class */
	private State() {

		props.put(boottime, String.valueOf(System.currentTimeMillis()));
		props.put(userisconnected, "false");		
		//props.put(autodocking, "false");

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
	 * @param path, is the file to write the state value pairs too
	 */
	public void writeFile(String path){
		
		System.out.println("state writing to: " + path);
	
		try {
			
			FileWriter out = new FileWriter(path);
			out.write("state as of: " + new Date().toString() + "\n");
			out.write("state writing to: " + path + "\n");
			
			Enumeration<Object> keys = props.keys();
			while(keys.hasMoreElements()){
				String key = (String) keys.nextElement();
				String value = (String) props.getProperty(key);
				
				System.out.println(key + " = " + value);

				out.write(key + " = " + value + "\r\n");
			}
 			
			out.close();

		} catch (Exception e) {
			e.printStackTrace();
		} 		
	}
	
	/** @return a copy of the properties file
	public Properties getProperties() {
		return (Properties) props.clone();
	}*/
	
	/**
	 * Put a name/value pair into the configuration
	 * 
	 * @param key
	 * @param value
	 */
	public synchronized void set(String key, String value) {

		// if(key==null || value==null) return;
	
//		System.out.println("refreshing property for: " + key + " = " + value);
		props.put(key.trim(), value.trim());
	}

	/**
	 * Put a name/value pair into the configuration
	 * 
	 * @param key
	 * @param value
	 */
	public synchronized void set(String key, long value) {

		// if(key==null) return;
		
//		System.out.println("refreshing property for: " + key + " = " + value);
    
		props.put(key.trim(), Long.toString(value));
	}

	
	/** */
	public synchronized String get(String key) {

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
	public boolean getBoolean(String key) {

		boolean value = false;

		try {

			value = Boolean.parseBoolean(get(key));

		} catch (Exception e) {
			System.err.println(e.getStackTrace());
			return false;
		}

		return value;
	}

	/** */
	public int getInteger(String key) {

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
	public long getLong(String key) {

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

	public void set(String key, boolean b) {
		if(b) set(key, "true");
		else set(key, "false");
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