package oculus;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

//import org.red5.logging.Red5LoggerFactory;
//import org.slf4j.Logger;

public class Util {

	private static final int PRECISION = 2;

	private static Settings settings = new Settings();
	// private final boolean debug = settings.getBoolean(Settings.developer);
	// private final boolean alerts = settings.getBoolean(Settings.emailalerts);
    // private static final boolean notify = settings.getBoolean(Settings.loginnotify);
	

	/**
	 * @param props is the list of values to send to disk 
	 * @param path, is the file to write the state value pairs too
	 */ 
	public static void writeFile(Properties props, final String path, final String comment){
		
		System.out.println("state writing to: " + path);
	
		try {
			
			FileWriter out = new FileWriter(path);
			out.append("# "+ comment + "\r\n");
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
	
	
	/**
	 * Delays program execution for the specified delay.
	 * 
	 * @param delay
	 *            is the specified time to delay program execution
	 *            (milliseconds).
	 */
	public static void delay(long delay) {
		try {
			Thread.sleep(delay);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Delays program execution for the specified delay.
	 * 
	 * @param delay
	 *            is the specified time to delay program execution
	 *            (milliseconds).
	 */
	public static void delay(int delay) {
		try {
			Thread.sleep(delay);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	/*
	 * 
	 */
	public static String getTime(long ms) {

		// Sat May 03 15:33:11 PDT 2008
		String date = new Date(ms).toString();

		int index1 = date.indexOf(' ', 0);
		int index2 = date.indexOf(' ', index1 + 1);
		int index3 = date.indexOf(' ', index2 + 1);
		int index4 = date.indexOf(' ', index3 + 1);

		// System.out.println("1: " + index1 + " 2: " + index2 + " 3: " + index3
		// + " 4: " + index4);

		String time = date.substring(index3 + 1, index4);

		return time;
	}

	/*
	 * 
	 */
	public static String getTime() {
		return getTime(System.currentTimeMillis());
	}

	/**
	 * Returns the specified double, formatted as a string, to n decimal places,
	 * as specified by precision.
	 * <p/>
	 * ie: formatFloat(1.1666, 1) -> 1.2 ie: formatFloat(3.1666, 2) -> 3.17 ie:
	 * formatFloat(3.1666, 3) -> 3.167
	 */
	public static String formatFloat(double number, int precision) {

		String text = Double.toString(number);
		if (precision >= text.length()) {
			return text;
		}

		int start = text.indexOf(".") + 1;
		if (start == 0)
			return text;

		// cut off all digits and the '.'
		//
		if (precision == 0) {
			return text.substring(0, start - 1);
		}

		if (start <= 0) {
			return text;
		} else if ((start + precision) <= text.length()) {
			return text.substring(0, (start + precision));
		} else {
			return text;
		}
	}

	/**
	 * Returns the specified double, formatted as a string, to n decimal places,
	 * as specified by precision.
	 * <p/>
	 * ie: formatFloat(1.1666, 1) -> 1.2 ie: formatFloat(3.1666, 2) -> 3.17 ie:
	 * formatFloat(3.1666, 3) -> 3.167
	 */
	public static String formatFloat(double number) {

		String text = Double.toString(number);
		if (PRECISION >= text.length()) {
			return text;
		}

		int start = text.indexOf(".") + 1;
		if (start == 0)
			return text;

		if (start <= 0) {
			return text;
		} else if ((start + PRECISION) <= text.length()) {
			return text.substring(0, (start + PRECISION));
		} else {
			return text;
		}
	}

	/**
	 * Returns the specified double, formatted as a string, to n decimal places,
	 * as specified by precision.
	 * <p/>
	 * ie: formatFloat(1.1666, 1) -> 1.2 ie: formatFloat(3.1666, 2) -> 3.17 ie:
	 * formatFloat(3.1666, 3) -> 3.167
	 */
	public static String formatString(String number, int precision) {

		String text = number;
		if (precision >= text.length()) {
			return text;
		}

		int start = text.indexOf(".") + 1;

		if (start == 0)
			return text;

		// System.out.println("format string - found dec point at index = " +
		// start );

		// cut off all digits and the '.'
		//
		if (precision == 0) {
			return text.substring(0, start - 1);
		}

		if (start <= 0) {
			return text;
		} else if ((start + precision) <= text.length()) {
			return text.substring(0, (start + precision));
		}

		return text;
	}

	public static boolean copyfile(String srFile, String dtFile) {
		try {
			
			File f1 = new File(srFile);
			File f2 = new File(dtFile);
			InputStream in = new FileInputStream(f1);

			// Append
			OutputStream out = new FileOutputStream(f2, true);

			// Overwrite
			// OutputStream out = new FileOutputStream(f2);

			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();

		} catch (Exception e) {
			System.out.println(e.getMessage());
			return false;
		}

		// file copied
		return true;
	}
	
	/**
	 * Run the given text string as a command on the windows host computer. 
	 * 
	 * @param str is the command to run, like: "restart
	 * 
	 */
	public static void systemCallBlocking(final String args) {
		try {	
			
			long start = System.currentTimeMillis();
			Process proc = Runtime.getRuntime().exec(args);
			BufferedReader procReader = new BufferedReader(
					new InputStreamReader(proc.getInputStream()));

			String line = null;
			System.out.println(proc.hashCode() + " exec(): " + args);
			while ((line = procReader.readLine()) != null)
				System.out.println(proc.hashCode() + " systemCallBlocking() : " + line);
			
			System.out.println(proc.hashCode() + " process exit value = " + proc.exitValue());
			System.out.println(proc.hashCode() + " bocking run time = " + (System.currentTimeMillis()-start) + " ms");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Run the given text string as a command on the windows host computer. 
	 * 
	 * @param str is the command to run, like: "restart
	 *  
	 */
	public static void systemCall(final String str){
		new Thread(new Runnable() { 
			public void run() {
				try {
			
					Process proc = Runtime.getRuntime().exec(str);
					BufferedReader procReader = new BufferedReader(
							new InputStreamReader(proc.getInputStream()));

					String line = null;
					System.out.println("process exit value = " + str);
					while ((line = procReader.readLine()) != null)
						System.out.println("systemCall() : " + line);
					
					System.out.println("process exit value = " + proc.exitValue());
				
				} catch (Exception e) {
					e.printStackTrace();
				}		
			} 	
		}).start();
	}


	/**
	 * @return this device's external IP address is via http lookup, or null if fails 
	 */ 
	public static String getExternalIPAddress(){

		String address = null;
		URL url = null;

		try {
			
			url = new URL("http://checkip.dyndns.org/");

			// read in file from the encoded url
			URLConnection connection = (URLConnection) url.openConnection();
			BufferedInputStream in = new BufferedInputStream(connection.getInputStream());

			int i;
			while ((i = in.read()) != -1) {
				address = address + (char) i;
			}
			in.close();

			// parse html file
			address = address.substring(address.indexOf(": ") + 2);
			address = address.substring(0, address.indexOf("</body>"));
			
		} catch (Exception e) {
			return null;
		}
		
		// all good 
		return address;
	}

    /**
     * @return the local host's IP, null on error
     */
    public static String getLocalAddress(){
            try {
                    return (InetAddress.getLocalHost()).getHostAddress();
            } catch (UnknownHostException e) {
                    return null;
            }
    }
	
	
//	/** @return a list of ip's for this local network */ 
//	public static String getLocalAddress() {
//		String address = "";
//		try {
//			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
//			if (interfaces != null)
//				while (interfaces.hasMoreElements()) {
//					NetworkInterface ni = (NetworkInterface) interfaces.nextElement();
//					if (!ni.isVirtual())
//						if (!ni.isLoopback())
//							if (ni.isUp()) {
//								Enumeration<InetAddress> addrs = ni.getInetAddresses();
//								while (addrs.hasMoreElements()) {
//									InetAddress a = (InetAddress) addrs.nextElement();
//									address += a.getHostAddress() + " ";
//								}
//							}
//				}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		String[] addrs = address.split(" ");
//		for(int i = 0 ; i < addrs.length ; i++){
//			if(!addrs[i].contains(":"))
//				return addrs[i];
//		}
//		
//		return null;
//	}

	/**
	 * write new value to user's screen and set it 
	 */
	public static void setSystemVolume(int percent, Application app){
		setSystemVolume(percent);
		app.message("ROV volume set to "+Integer.toString(percent)+"%", null, null);
	}
	
	/**
	 * 
	 * change the host computer's volume 
	 * 
	 * @param percent
	 */
	public static void setSystemVolume(int percent) {
		new Settings().writeSettings(Settings.volume, percent);
		float vol = (float) percent / 100 * 65535;
		String str = "nircmdc.exe setsysvolume "+ (int) vol;
		Util.systemCall(str);
	}

	/**
	 * If enabled in settings with "notify", this method will turn up volume to max,
	 * say the string, and then restore the volume to the original setting. 
	 * 
	 * 
	 * @param str
	 * 				is the phrase to turn from text to speech 
	 */
	public static void beep() {
		// read setting every time in case settings changed by client
		if (settings.getBoolean(Settings.loginnotify)) 
			systemCall("nircmdc.exe beep 500 1000");
	}
}
