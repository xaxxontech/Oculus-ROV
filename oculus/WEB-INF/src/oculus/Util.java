package oculus;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Date;
import oculus.commport.ArduioPort;
import developer.SendMail;

public class Util {

	private static final int PRECISION = 2;
	
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
		systemCall("nircmdc.exe beep 500 1000");
	}

	/** */ 
	public static void dockingTest(final Application app, final ArduioPort port, final Docker docker) {

	//	Logger log = Red5LoggerFactory.getLogger(XMLCommandManager.class, "oculus");
		State state = State.getReference();

		if (docker == null || port == null || app == null) {
		//	log.error("not configured");
			return;
		}

		if (state.getBoolean(State.autodocking)) {
		//	log.error("can't auto dock twice");
			return;
		}

		if( ! state.getBoolean("busy")){
			state.set("busy", true);
		} else {
		//	log.error("can't auto dock twice, busy");
			return;
		}
		
		new Thread(new Runnable() {

			@Override
			public void run() {
				State state = State.getReference();
				app.playerCallServer(PlayerCommands.chat.toString(), "command manager taking over");
				System.out.println("----camera on----");
				app.publish("camera");
				port.camHoriz();
				int min = new Settings().getInteger(FactorySettings.camposmin.toString()); 
				port.camToPos(min);
				port.camToPos(min);
				
				if (state.equals(State.dockstatus, State.docked)) {
					
					docker.dock(State.undock);
					Util.delay(3000);
					docker.dock(State.undock);
					Util.delay(3000);	

					/*
					port.turnRight();
					java.util.Random r = new java.util.Random();
					int d = r.nextInt(500)+300;
					Util.delay(d);
					port.stopGoing();
	*/
					
				}
				
				Util.delay(5000);				
				app.dockGrab();
				Util.delay(5000);

				// TODO: how many is 360??
				for (int i = 0; i < 25; i++) {
					if (state.getInteger(State.dockxsize) > 0) {
						if (state.getInteger(State.dockxsize) > 0) {
							
							docker.autoDock("go");
							state.delete("busy");
							app.message("command manager handing off to docker", null, null);
							return;
							
						} else app.dockGrab();

					} else {

						System.out.println("_can't see dock... " + i);
						Util.delay(2000);
						port.turnLeft();
						// TODO:TAKE THIS INFO FROM X Y 
						Util.delay(450);
						port.stopGoing();
						port.camToPos(min);
						Util.delay(3000);
						app.dockGrab();
					
					}
				}

				System.out.println("_+_+_ failure to find dock");
				new SendMail("Oculus Message", "failure to find dock", app);
				state.delete("busy");
				
			}
		}).start();
	}
}
