package oculus;

import java.io.FileInputStream;
import java.util.Enumeration;
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
	// public static final String command = "command";
	// public static final String action = "action";
	// public static final String launch = "launch";
	// public static final String kill = "kill";
	
	public static final String serialport = "serialport";
	public static final String enable = "enable";
	public static final String disable = "disable";
	public static final String boottime = "boottime";
	public static final String autodocking = "autodocking";
	
	//public static final String noardunio = "noardunio";

	public static final long ONE_MINUTE = 60000;
	public static final long TWO_MINUTES = 60000;
	public static final long FIVE_MINUTES = 300000;
	public static final long TEN_MINUTES = 600000;
	public static final int ERROR = -1;
	
	
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

		props.put(boottime, System.currentTimeMillis());
		props.put(autodocking, "false");

		//props.put(home, System.getProperty("java.home"));
		//props.put(path, System.getProperty("java.class.path"));
		//props.put(loggingEnabled, "true");
		//props.put(enableWatchDog, "true");
	}

	/** @param file is the properties file to configure the framework	 */
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
	}

	/** @return a copy of the properties file */
	public Properties getProperties() {
		return (Properties) props.clone();
	}
	
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
	 * lookup values from props file
	 * 
	 * @param key
	 *            is the lookup value
	 * @return the matching value from properties file (or null if not found)
	 */
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

	/**
	 * lookup values from props file
	 * 
	 * @param key
	 *            is the lookup value
	 * @return the matching value from properties file (or false if not found)
	 */
	public boolean getBoolean(String key) {

		boolean value = false;

		try {

			value = Boolean.parseBoolean(get(key));

		} catch (Exception e) {
			return false;
		}

		return value;
	}

	/**
	 * lookup values from props file
	 * 
	 * @param key
	 *            is the lookup value
	 * @return the matching value from properties file (or zero if not found)
	 */
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