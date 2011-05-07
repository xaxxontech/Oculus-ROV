package oculus;

import java.util.Properties;

/**
 * <p>
 * Holds the framework configuration parameters. Used as a shared resource for
 * all other classes to initialize from. The defaults are enough to send
 * commands and register API's for incoming XML events.
 * 
 * @author <a href="mailto:brad.zdanivsky@gmail.com">Brad Zdanivsky</a>
 */
public class State {

	// public static final String DEFAULT_PORT = "4444";
	// public static final String DEFAULT_ADDRESS = "230.0.0.1";
	// public static final String shutdown = "shutdown";
	
	public static final String user = "user";
	public static final String logintime = "logintime";
	public static final String userconnected = "userconnected";
	public static final String reboot = "reboot";
	public static final String developer = "developer";
	public static final String serialport = "serialport";
	public static final String lightport = "lightport";
	
	public static final String enable = "enable";
	public static final String disable = "disable";
	public static final String boottime = "boottime";
	public static final String emailbusy = "emailbusy";

	
	//public static final String autodocking = "autodocking";
	//public static final String noardunio = "noardunio";

	public static final long ONE_DAY = 86400000;
	public static final long ONE_MINUTE = 60000;
	public static final long TWO_MINUTES = 60000;
	public static final long FIVE_MINUTES = 300000;
	public static final long TEN_MINUTES = 600000;
	public static final int ERROR = -1;
	
	//
	MulticastChannel channel = MulticastChannel.getReference();
	
	/** reference to this singleton class */
	private static State singleton = null;

	/** properties object to hold configuration */
	private Properties props = new Properties();
	private boolean locked = false;

	public static State getReference() {
		if (singleton == null) {
			singleton = new State();
		}
		return singleton;
	}

	/** private constructor for this singleton class */
	private State() {

		props.put(boottime, String.valueOf(System.currentTimeMillis()));
		props.put(userconnected, false);
		
		
		//props.put(autodocking, "false");
		//props.put(home, System.getProperty("java.home"));
	
		//Command test = new Command();
		//test.add(boottime, get(boottime));
		//channel.write(test);
		
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

		if (locked) {
			System.out.println(" state locked, can't put(): " + key);
			return;
		}

		if (props.containsKey(key))
			System.out.println("refreshing property for: " + key + " = " + value);

		props.put(key.trim(), value.trim());
	}


	/**
	 * Put a name/value pair into the configuration
	 * 
	 * @param key
	 * @param value
	 */
	public synchronized void set(String key, boolean value) {

		if (locked) {
			System.out.println(" state locked, can't put(): " + key);
			return;
		}

		if (props.containsKey(key))
			System.out.println("refreshing property for: " + key + " = " + value);

		props.put(key.trim(), Boolean.toString(value));
	}


	/**
	 * Put a name/value pair into the configuration
	 * 
	 * @param key
	 * @param value
	 */
	public synchronized void set(String key, long value) {

		if (locked) {
			System.out.println(" state locked, can't put(): " + key);
			return;
		}

		if (props.containsKey(key))
			System.out.println("refreshing property for: " + key + " = " + value);

		props.put(key.trim(), Long.toString(value));
	}

	
	/** */
	public synchronized String get(String key) {

		String ans = null;

		try {

			ans = props.getProperty(key.trim());

		} catch (Exception e) {
			System.err.println(e.getMessage());
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
	
	/**@return the ms since last boot */
	public long getUpTime(){
		return System.currentTimeMillis() - getLong(boottime);
	}
	
	public synchronized void lock() {
		locked = true;
	}

	public synchronized void unlock() {
		locked = false;
	}

	public synchronized boolean isLocked() {
		return locked;
	}
}