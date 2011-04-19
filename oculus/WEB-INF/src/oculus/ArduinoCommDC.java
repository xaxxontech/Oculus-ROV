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

	public static final int TIME_OUT = 2000;
	public static final int RESPOND_DELAY = 100;
	public static final long DEAD_TIME_OUT = 8000;

	// add commands here
	public static byte STOP = 's';
	public static byte FORWARD = 'f';
	public static byte BACKWARD = 'b';
	public static byte LEFT = 'l';
	public static byte RIGHT = 'r';
	public static byte COMP = 'c';
	public static byte CAM = 'v';

	// just do on reset instead?
	public final byte[] GET_PRODUCT = { 'x' };
	public final byte[] GET_VERSION = { 'y' };

	private String portName = null;
	private SerialPort serialPort = null;
	private InputStream in;
	private OutputStream out;

	// input buffer
	private byte[] buffer = new byte[64];
	private int buffSize = 0;

	// track write times
	private long lastSent = System.currentTimeMillis();
	private long lastRead = System.currentTimeMillis();

	Settings settings = new Settings();

	protected int speedslow = Integer.parseInt(settings
			.readSetting("speedslow"));
	protected int speedmed = Integer.parseInt(settings.readSetting("speedmed"));
	protected int camservohoriz = Integer.parseInt(settings
			.readSetting("camservohoriz"));
	protected int camposmax = Integer.parseInt(settings
			.readSetting("camposmax"));
	protected int camposmin = Integer.parseInt(settings
			.readSetting("camposmin"));
	protected int nudgedelay = Integer.parseInt(settings
			.readSetting("nudgedelay"));
	protected int maxclicknudgedelay = Integer.parseInt(settings
			.readSetting("maxclicknudgedelay"));
	protected int maxclickcam = Integer.parseInt(settings
			.readSetting("maxclickcam"));
	protected double clicknudgemomentummult = Double.parseDouble(settings
			.readSetting("clicknudgemomentummult"));
	protected int steeringcomp = Integer.parseInt(settings
			.readSetting("steeringcomp"));

	protected int camservodirection = 0;
	protected int camservopos = camservohoriz;
	protected int camwait = 400;
	protected int camdelay = 50;
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
	long nextcommandtime = 0;
	boolean isconnected = false;

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
	public ArduinoCommDC(String str, boolean watchdog) {

		// keep port name, need to reconnect
		portName = str;

		// check for lost connection
		if (watchdog)
			new WatchDog().start();
	}

	/** open port, enable read and write, enable events */
	public void connect() {
		try {

			serialPort = (SerialPort) CommPortIdentifier.getPortIdentifier(
					portName).open(ArduinoCommDC.class.getName(), TIME_OUT);
			serialPort.setSerialPortParams(19200, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			// open streams
			out = serialPort.getOutputStream();
			in = serialPort.getInputStream();

			// register for serial events
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);

		} catch (Exception e) {
			log.error(e.getMessage());
			return;
		}

		// setup delay, give arduino chance to get ready
		/*try {
			Thread.sleep(TIME_OUT);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}*/

		// all good, ready for commands
		isconnected = true;
		log.info("connected to: " + portName);
	}

	@Override
	/** buffer input on event and trigger parse on '>' charater  
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
					if ((input[j] == '>') || (input[j] == 13)
							|| (input[j] == 10)) {

						print();

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
				System.out.println("event : " + e.getLocalizedMessage());
			}
		}
	}

	// TODO: store commands sent
	private void print() {

		if (buffSize == 0)
			return;

		System.out.print("read[" + getReadDelta() + "]\twrite["
				+ getWriteDelta() + "]\tsize[" + buffSize + "] ");
		String responce = "";
		for (int i = 0; i < buffSize; i++)
			responce += (char) buffer[i];

		System.out.println(responce.trim());
	}

	/** @return the time since last write() operation */
	public long getWriteDelta() {
		return System.currentTimeMillis() - lastSent;
	}

	/** @return the time since last read operation */
	public long getReadDelta() {
		return System.currentTimeMillis() - lastRead;
	}

	/** inner class to send commands */
	private class Sender extends Thread {

		// exec the given command
		byte[] command = null;

		public Sender(final byte[] cmd) {
			this.command = cmd;
			this.start();
		}

		public void run() {
			sendCommand(command);

			// TODO: Check the reply, send error if not there
		}
	}

	/** inner class to check if getting responses */
	private class WatchDog extends Thread {

		public WatchDog() {
			System.out.println("starting watchdog thread");
			this.setDaemon(true);
		}

		public void run() {
			while (true) {
				if (getReadDelta() > DEAD_TIME_OUT) {
					if (isconnected) {

						System.out.println("in delta: "
								+ (System.currentTimeMillis() - lastRead));
						System.err
								.println("no info coming back from arduino, resting: "
										+ portName);

						// reset
						disconnect();
						connect();

						if (isconnected) {
							System.out.println("re-connected, stopping bot..");
							// TODO: send current state, speed, comp etc ??/
							stopGoing();
						}
					}
				}

				try {
					// check often
					Thread.sleep(RESPOND_DELAY + 100);
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
	 * Send a multi byte command to the arduino with protection for trying to
	 * send too quickly
	 * 
	 * @param command
	 *            is a finalized byte array of messages to send
	 */
	private synchronized void sendCommand(final byte[] command) {

		/*
		if (!isconnected) {
			System.err.println("not connected, try connecting...");
			connect();
			return;
		}
*/
		
		if (getWriteDelta() < RESPOND_DELAY) {
			//try {

				System.out.println("sending too fast: " + getWriteDelta());
				return;
				
				// only wait as long as needed to make next time slot
				// Thread.sleep((RESPOND_DELAY - getWriteDelta()));

				// System.out.println("delta now: " + getWriteDelta());

			//} catch (InterruptedException e) {
			//	e.printStackTrace();
			//}
		}

		try {

			// send bytes
			out.write(command);

		} catch (Exception e) {
			e.printStackTrace();
		}

		// track last write
		lastSent = System.currentTimeMillis();
	}

	/** */
	public void stopGoing() {
		
		if(moving){
			final byte[] command = { STOP, '\n' };
			new Sender(command);
		}
		
		moving = false;
		movingforward = false;

	}

	/** */
	public void goForward() {

		final byte[] command = { FORWARD, (byte) speed, '\n' };
		new Sender(command);

		moving = true;
		movingforward = true;
	}

	/** */
	public void goBackward() {

		final byte[] command = { BACKWARD, (byte) speed, '\n' };
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

		// send it
		final byte[] command = { RIGHT, (byte) tmpspeed, '\n' };
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

		// send it
		final byte[] command = { LEFT, (byte) tmpspeed, '\n' };
		new Sender(command);

		moving = true;
	}

	/** TODO: not sure how this steers */
	public void camGo() {
		while (camservodirection != 0) {
			camset(camservopos, camdelay);
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

			camset(camservopos, camwait);

		}
		if (str.equals("upabit")) {
			camservopos += 5;
			if (camservopos > camposmax) {
				camservopos = camposmax;
			}

			camset(camservopos, camdelay);
		}
	}

	/** level the camera servo */
	public void camHoriz() {
		camservopos = camservohoriz;
		camset(camservopos, camwait);
	}

	/** set the cam servo to a new target, and ms to delay after setting pwm pin */
	private void camset(final int target, final int delay) {

		// TODO: error check the new target ?

		if (delay < camdelay) {
			System.err.println("skipping, cam delay too small!");
			return;
		}

		final byte[] command = { CAM, (byte) target, (byte) delay, '\n' };
		new Sender(command);
	}

	/** set the cam servo to a new target, and use default delay */
	public void camset(final int target) {
		camset(target, camwait);
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

		// TODO: check with colin... why return if we have access to this??
		camset(camservopos, camwait + clicknudgedelay);
		return camservopos;
	}

	/** send steering compensation values to the arduino */
	public void updateSteeringComp() {
		// log.info("set steering comp");
		// new Thread(new Runnable() {
		//	public void run() {
				final byte[] command = { COMP, (byte) steeringcomp, '\n' };
				new Sender(command);
			//}
		//});
	}

	/* test driver
	public static void main(String[] args) throws Exception {

		FindPort find = new FindPort();
		String portstr = find.search(FindPort.OCULUS_DC);
		if (portstr != null) {

			ArduinoCommDC dc = new ArduinoCommDC(portstr, false);

			dc.connect();
			if (!dc.isconnected) {
				System.out.println("can't connect to: " + portstr);
				System.exit(0);
			}

			// System.out.println("connected oculus on: " + portstr);

			dc.updateSteeringComp();
			dc.goBackward();
			dc.camset(170);
			dc.goBackward();

			Thread.sleep(7700);

			dc.turnLeft();

			Thread.sleep(3900);

			dc.turnRight();
			dc.camset(0);

			Thread.sleep(4760);

			dc.stopGoing();
			dc.camset(100);

			// test watchdog
			// Thread.sleep(30000);
		}

		System.out.println(".. done");

		// force exit
		System.exit(0);
	}*/
	
}
