package oculus;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

public class ArduinoCommDC implements SerialPortEventListener {

	private Logger log = Red5LoggerFactory.getLogger(ArduinoCommDC.class, "oculus");

	// if watchdog'n, re-connect if not seen input since this long ago 
	public static final long DEAD_TIME_OUT = 10000;
	public static final int SETUP = 2000;
	public static final int WATCHDOG_DELAY = 1000;

	// add commands here
	public static final byte FORWARD = 'f';
	public static final byte BACKWARD = 'b';
	public static final byte LEFT = 'l';
	public static final byte RIGHT = 'r';
	public static final byte COMP = 'c';
	public static final byte CAM = 'v';
	public static final byte ECHO = 'z';
	
	public static final byte[] STOP = {'s'};
	// public static final byte[] GET_PRODUCT = { 'x' };
	public static final byte[] GET_VERSION = { 'y' };
	public static final byte[] CAMRELEASE = {'w'};
	
	private String portName = null;
	private SerialPort serialPort = null;
	private InputStream in;
	private OutputStream out;
	
	// add a 'z' to commands 
	protected boolean echo = true;
	protected String version = null;

	// input buffer
	private byte[] buffer = new byte[32];
	private int buffSize = 0;

	// track write times
	private long lastSent = System.currentTimeMillis();
	private long lastRead = System.currentTimeMillis();

	Settings settings = new Settings();

	protected int speedslow = Integer.parseInt(settings.readSetting("speedslow"));
	protected int speedmed = Integer.parseInt(settings.readSetting("speedmed"));
	protected int camservohoriz = Integer.parseInt(settings.readSetting("camservohoriz"));
	protected int camposmax = Integer.parseInt(settings.readSetting("camposmax"));
	protected int camposmin = Integer.parseInt(settings.readSetting("camposmin"));
	protected int nudgedelay = Integer.parseInt(settings.readSetting("nudgedelay"));
	protected int maxclicknudgedelay = Integer.parseInt(settings.readSetting("maxclicknudgedelay"));
	protected int maxclickcam = Integer.parseInt(settings.readSetting("maxclickcam"));
	protected double clicknudgemomentummult = Double.parseDouble(settings.readSetting("clicknudgemomentummult"));
	protected int steeringcomp = Integer.parseInt(settings.readSetting("steeringcomp"));

	protected int camservodirection = 0;
	protected int camservopos = camservohoriz;
	protected int camwait = 400;
	protected int camdelay = 50; // for smooth continuous motion
	protected int speedfast = 255;
	protected int turnspeed = 255;
	protected int speed = speedfast; // set default to max

	protected String direction = null;
	protected boolean moving = false;
	volatile boolean sliding = false;
	volatile boolean movingforward = false;
	int tempspeed = 999;
	int clicknudgedelay = 0;
	String tempstring = null;
	int tempint = 0;
	
	// make sure all threads know if connected 
	private volatile boolean isconnected = false;
	
	// call back
	private Application application = null;

	/**
	 * Constructor but call connect to configure
	 * 
	 * @param str
	 *            is the name of the serial port on the host computer
	 * 
	 * @param watchdog
	 *            set to true to enable an internal thread to watch for a
	 *            non-responsive arduino. Will try to disconnect() and then
	 *            connect() again.
	 */
	public ArduinoCommDC(String str, boolean watchdog, Application app) {

		// keep port name, need it to re-connect
		portName = str;

		// call back to notify on reset events etc
		application  = app;
		
		// check for lost connection
		if (watchdog)
			new WatchDog().start();
	}

	/** open port, enable read and write, enable events */
	public void connect() {
		try {

			serialPort = (SerialPort) CommPortIdentifier.getPortIdentifier(portName).open(ArduinoCommDC.class.getName(), SETUP);
			serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			// open streams
			out = serialPort.getOutputStream();
			in = serialPort.getInputStream();

			// register for serial events
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
			
			// setup delay 
			Thread.sleep(SETUP);

		} catch (Exception e) {
			log.error(e.getMessage());
			return;
		}
	}

	/** @return True if the serial port is open */
	public boolean isConnected(){
		return isconnected;
	}
	
	@Override
	/** buffer input on event and trigger parse on '>' charter  
	 * 
	 * Note, all feedback must be in single xml tags like: <feedback 123>
	 */
	public void serialEvent(SerialPortEvent event) {
		if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {
				byte[] input = new byte[32];
				int read = in.read(input);
				for (int j = 0; j < read; j++) {

					// print() or println() from ardunio code
					if ((input[j] == '>') || (input[j] == 13) || (input[j] == 10)) {

						// do what ever is in buffer 
						if(buffSize > 0) execute();

						// reset
						buffSize = 0;

						// track input from arduino
						lastRead = System.currentTimeMillis();

					} else if (input[j] == '<') {

						// start of message
						buffSize = 0;

					} else {

						// buffer until ready to parse
						buffer[buffSize++] = input[j];
					}
				}
			} catch (IOException e) {
				log.error("event : " + e.getMessage());
			}
		}
	}

	// TODO: store commands sent if echo'ing.. record errors and dropped commands 
	private void execute() {

		// copy out of buffer 
		String responce = "";
		for(int i = 0 ; i < buffSize ; i++)
			responce += (char)buffer[i];
		responce = responce.trim();
		
		// take action as ardiuno has just turned on 
		if(responce.equals("reset")){
			
			application.wasReset();
			isconnected = true;
			updateSteeringComp();
			camHoriz();
			
			// might have new firmware after reseting? 
			version = null;
			new Sender(GET_VERSION);
		
		} else if(responce.startsWith("version:")){
			
			// NOTE: watchdog will send a get version command if idle comport 
			if(version == null) 
				version = responce.substring(
						responce.indexOf("version:")+8, responce.length());
	
			// don't flood std.out 
			return;
			
		} else if(responce.equals("overflow")){
			log.error("Sending too fast: " + getWriteDelta());
			disconnect();
		}
		
		// act on feedback
		if(responce.charAt(0) == STOP[0]){
			movingforward = false;
			moving = false;
		} else if(responce.charAt(0) != GET_VERSION[0]){
			// don't bother showing watchdog pings 
			System.out.println("ardunio: " + responce);
		}
	}

	/** @return the time since last write() operation */
	public long getWriteDelta() {
		return System.currentTimeMillis() - lastSent;
	}

	/** @return this device's firmware version */
	public String getVersion(){
		return version;
	}
	
	/** @return the time since last read operation */
	public long getReadDelta() {
		return System.currentTimeMillis() - lastRead;
	}

	/** inner class to send commands */
	private class Sender extends Thread {
		private byte[] command = null;
		public Sender(final byte[] cmd) {
			command = cmd;
			start();
		}
		public void run() {
			sendCommand(command);
		}
	}

	/** @param update is set to true to turn on echo'ing of serial commands */
	public void setEcho(boolean update){
		echo = update;
	}
	
	/** inner class to check if getting responses */
	private class WatchDog extends Thread {
		public WatchDog() {
			System.out.println("starting watchdog thread");
			this.setDaemon(true);
		}
		public void run() {
			try {
				Thread.sleep(SETUP);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			while (true) {
				if (getReadDelta() > DEAD_TIME_OUT) {
					if (isconnected) {

						// System.out.println("in delta: "	+ getReadDelta());
						System.err.println("disconnecting, no data coming in on port: " + portName);

						// reset arduino
						disconnect();
						
						// try again 
						connect();
					}
				}

				// send ping to keep connection alive 
				if(getReadDelta() > (DEAD_TIME_OUT / 3))
					new Sender(GET_VERSION);
				
				try {
					// check often
					Thread.sleep(WATCHDOG_DELAY);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/** shutdown serial port */
	protected void disconnect() {
		try {
			in.close();
			out.close();
			isconnected = false;
		} catch (Exception e) {
			System.out.println("close(): " + e.getMessage());
		}
		serialPort.close();
	}

	/**
	 * Send a multi byte command to send the arduino 
	 * 
	 * @param command
	 *            is a finalized byte array of messages to send
	 */
	private synchronized void sendCommand(final byte[] command) {
		
		if(!isconnected) return;
		
		try {
				
			// send 
			out.write(command);
			
			// add flag for echo 
			if(echo)
				out.write(ECHO);
			
			// end of command 
			out.write(13);
			
		} catch (Exception e) {
			disconnect();
			log.error(e.getMessage());
		}

		// track last write
		lastSent = System.currentTimeMillis();
	}

	/** */
	public void stopGoing() {

		// TODO: only send if really going?
		// if (moving) { 
		// byte[] command = { STOP };
		
		new Sender(STOP);
		
		moving = false;
		movingforward = false;
	}

	/** */
	public void goForward() {

		 byte[] command = { FORWARD, (byte) speed };
		new Sender(command);

		moving = true;
		movingforward = true;
	}

	/** */
	public void goBackward() {

		 byte[] command = { BACKWARD, (byte) speed };
		new Sender(command);

		moving = true;
		movingforward = false;
	}

	/** */
	public void turnRight() {

		// set new speed
		int tmpspeed = turnspeed;
		int boost = 10;
		if (speed < turnspeed && (speed + boost) < speedfast)
			tmpspeed = speed + boost;

		byte[] command = { RIGHT, (byte) tmpspeed };
		new Sender(command);
		moving = true;
	}

	/** */
	public void turnLeft() {

		// set new speed
		int tmpspeed = turnspeed;
		int boost = 10;
		if (speed < turnspeed && (speed + boost) < speedfast)
			tmpspeed = speed + boost;

		byte[] command = { LEFT, (byte) tmpspeed };
		new Sender(command);
		moving = true;
	}

	public void camGo() {
		new Thread(new Runnable() { public void run() {
			try {
				while (camservodirection != 0) {
					byte[] command = { CAM, (byte) camservopos };
					sendCommand(command);
					Thread.sleep(camdelay);
					camservopos += camservodirection;
					if (camservopos > camposmax) {
						camservopos = camposmax;
						camservodirection = 0;
					}
					if (camservopos < camposmin) {
						camservopos = camposmin;
						camservodirection = 0;
					}
				}
				Thread.sleep(250);
				// [] command = { CAMRELEASE };
				sendCommand(CAMRELEASE);
	        } catch (Exception e) {e.printStackTrace(); }
		} }).start();
	}

	public void camCommand(String str) {
		if (str.equals("stop")) {
			camservodirection = 0;
		} else if (str.equals("up")) {
			camservodirection = 1;
			camGo();
		} else if (str.equals("down")) {
			camservodirection = -1;
			camGo();
		} else if (str.equals("horiz")) {
			camHoriz();
		} else if (str.equals("downabit")) {
			camservopos -= 5;
			if (camservopos < camposmin) {
				camservopos = camposmin;
			}
			new Thread(new Runnable() { public void run() {
				try {
					byte[] command = { CAM, (byte) camservopos };
					sendCommand(command);
					Thread.sleep(camwait);
					// byte[] command1 = { CAMRELEASE };
					sendCommand(CAMRELEASE);
				} catch (Exception e) {e.printStackTrace(); }
			} }).start();
		} else  if (str.equals("upabit")) {
			camservopos += 5;
			if (camservopos > camposmax) {
				camservopos = camposmax;
			}
			new Thread(new Runnable() { public void run() {
				try {
					byte[] command = { CAM, (byte) camservopos };
					sendCommand(command);
					Thread.sleep(camwait);
					//byte[] command1 = { CAMRELEASE };
					sendCommand(CAMRELEASE);
				} catch (Exception e) {e.printStackTrace(); }
			} }).start();
		}
	}

	/** level the camera servo */
	public void camHoriz() {
		camservopos = camservohoriz;
		new Thread(new Runnable() { public void run() {
			try {
				 			 
				byte[] cam = { CAM, (byte) camservopos };
	            sendCommand(cam);
	            Thread.sleep(camwait);
	            // byte[] release = { CAMRELEASE };
	            sendCommand(CAMRELEASE);
	            	
			} catch (Exception e) {e.printStackTrace(); }
		} }).start();
	}

	
	public void camToPos(Integer n) {
        camservopos = n;
        // System.out.println("cam to: " + camservopos);
        new Thread(new Runnable() {public void run() {
            try {
            	
            	byte[] cam = { CAM, (byte) camservopos };
            	sendCommand(cam);
            	Thread.sleep(camwait);
            	// byte[] release = { CAMRELEASE };
            	sendCommand(CAMRELEASE);
            
            } catch (Exception e) {
            	e.printStackTrace();
            }
        } }).start();
        
        //System.out.println("_cam to: " + camservopos);   
    }
    
	/** Set the speed on the bot */
	public void speedset(String str) {
		if (str.equals("slow")) {
			speed = speedslow;
		}
		if (str.equals("med")) {
			speed = speedmed;
		}
		if (str.equals("fast")) {
			speed = speedfast;
		}
		if (movingforward) {
			goForward();
		}
	}

	public void nudge(String dir) {
		direction = dir;
		int n = nudgedelay;
		if (direction.equals("right")) {
			turnRight();
		}
		if (direction.equals("left")) {
			turnLeft();
		}
		if (direction.equals("forward")) {
			goForward();
			movingforward = false;
			n *= 4;
		}
		if (direction.equals("backward")) {
			goBackward();
			n *= 4;
		}

		try {
			Thread.sleep(n);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (movingforward == true) {
			goForward();
		} else {
			stopGoing();
		}
	}

	public void slide(String dir) {
		if (sliding == false) {
			sliding = true;
			direction = dir;
			tempspeed = 999;
			new Thread(new Runnable() {
				public void run() {
					try {
						int distance = 300;
						int turntime = 500;
						tempspeed = speed;
						speed = speedfast;
						if (direction.equals("right")) {
							turnLeft();
						} else {
							turnRight();
						}
						Thread.sleep(turntime);
						if (sliding == true) {
							goBackward();
							Thread.sleep(distance);
							if (sliding == true) {
								if (direction.equals("right")) {
									turnRight();
								} else {
									turnLeft();
								}
								Thread.sleep(turntime);
								if (sliding == true) {
									goForward();
									Thread.sleep(distance);
									if (sliding == true) {
										stopGoing();
										sliding = false;
										speed = tempspeed;
									}
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}

	public void slidecancel() {
		if (sliding == true) {
			if (tempspeed != 999) {
				speed = tempspeed;
				sliding = false;
			}
		}
	}

	public Integer clickSteer(String str) {
		tempstring = str;
		tempint = 999;
		String xy[] = tempstring.split(" ");
		if (Integer.parseInt(xy[1]) != 0) {
			tempint = clickCam(Integer.parseInt(xy[1]));
		}
		new Thread(new Runnable() {
			public void run() {
				try {
					String xy[] = tempstring.split(" ");
					if (Integer.parseInt(xy[0]) != 0) {
						if (Integer.parseInt(xy[1]) != 0) {
							Thread.sleep(camwait);
						}
						clickNudge(Integer.parseInt(xy[0]));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		return tempint;
	}

	public void clickNudge(Integer x) {
		if (x > 0) {
			direction = "right";
		} else {
			direction = "left";
		}
		clicknudgedelay = maxclicknudgedelay * (Math.abs(x)) / 320;
		/*
		 * multiply clicknudgedelay by multiplier multiplier increases to
		 * CONST(eg 2) as x approaches 0, 1 as approaches 320
		 * ((320-Math.abs(x))/320)*1+1
		 */
		double mult = Math.pow(((320.0 - (Math.abs(x))) / 320.0), 3)
				* clicknudgemomentummult + 1.0;
		// System.out.println("clicknudgedelay-before: "+clicknudgedelay);
		clicknudgedelay = (int) (clicknudgedelay * mult);
		// System.out.println("n: "+clicknudgemomentummult+" mult: "+mult+" clicknudgedelay-after: "+clicknudgedelay);
		new Thread(new Runnable() {
			public void run() {
				try {
					tempspeed = speed;
					speed = speedfast;
					if (direction.equals("right")) {
						turnRight();
					} else {
						turnLeft();
					}
					Thread.sleep(clicknudgedelay);
					speed = tempspeed;
					if (movingforward == true) {
						goForward();
					} else {
						stopGoing();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	public Integer clickCam(Integer y) {
		Integer n = maxclickcam * y / 240;
		camservopos -= n;
		if (camservopos > camposmax) {
			camservopos = camposmax;
		}
		if (camservopos < camposmin) {
			camservopos = camposmin;
		}

		new Thread(new Runnable() { public void run() {
			try {
				byte[] command = { CAM, (byte) camservopos };
				sendCommand(command);
				Thread.sleep(camwait + clicknudgedelay);
				// byte[] command1 = { CAMRELEASE };
				sendCommand(CAMRELEASE);
			} catch (Exception e) {e.printStackTrace(); }
		} }).start();
		return camservopos;
	}

	/** send steering compensation values to the arduino */
	public void updateSteeringComp() {
		byte[] command = { COMP, (byte) steeringcomp };
		new Sender(command);
	}
	
	/*
	 * test driver public static void main(String[] args) throws Exception {
	 * 
	 * FindPort find = new FindPort(); String portstr =
	 * find.search(FindPort.OCULUS_DC); if (portstr != null) {
	 * 
	 * ArduinoCommDC dc = new ArduinoCommDC(portstr, false);
	 * 
	 * dc.connect(); if (!dc.isconnected) {
	 * System.out.println("can't connect to: " + portstr); System.exit(0); }
	 * 
	 * // System.out.println("connected oculus on: " + portstr);
	 * 
	 * dc.updateSteeringComp();
	 */

}
