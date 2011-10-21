package oculus.commport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import oculus.Application;
import oculus.FactorySettings;
import oculus.OptionalSettings;
import oculus.Settings;
import oculus.State;
import oculus.Util;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;
import gnu.io.SerialPort;

public abstract class AbstractArduinoComm implements ArduioPort {

	protected Logger log = Red5LoggerFactory.getLogger(AbstractArduinoComm.class, "oculus");
	protected State state = State.getReference();
	protected SerialPort serialPort = null;
	protected InputStream in;
	protected OutputStream out;
	public String version = null;
	public byte[] buffer = new byte[32];
	public int buffSize = 0;
	public long lastSent = System.currentTimeMillis();
	public long lastRead = System.currentTimeMillis();
	public Settings settings = new Settings();
	public int speedslow = settings.getInteger("speedslow");
	public int speedmed = settings.getInteger("speedmed");
	public int camservohoriz = settings.getInteger("camservohoriz");
	public int camposmax = settings.getInteger("camposmax");
	public int camposmin = settings.getInteger("camposmin");
	public int nudgedelay = settings.getInteger("nudgedelay");
	public int maxclicknudgedelay = settings.getInteger("maxclicknudgedelay");
	public int maxclickcam = settings.getInteger("maxclickcam");
	public double clicknudgemomentummult = settings.getDouble("clicknudgemomentummult");
	public int steeringcomp = settings.getInteger("steeringcomp");
	public boolean sonarEnabled = settings.getBoolean(OptionalSettings.sonarenabled.toString());
	public boolean holdservo = settings.getBoolean(FactorySettings.holdservo.toString());
	public int camservodirection = 0;
	public int camservopos = camservohoriz;
	public int camwait = 400;
	public int camdelay = 50;
	public int speedfast = 255;
	public int turnspeed = 255;
	public int speed = speedfast;
	public String direction = null;
	public boolean moving = false;
	public volatile boolean sliding = false;
	public volatile boolean movingforward = false;
	public int tempspeed = 999;
	public int clicknudgedelay = 0;
	public String tempstring = null;
	public int tempint = 0;
	public volatile boolean isconnected = false;
	public Application application = null;

	public AbstractArduinoComm(Application app) {

		// call back to notify on reset events etc
		application = app;

		if (state.get(State.serialport) != null) {
			new Thread(new Runnable() {
				public void run() {

					connect();
					Util.delay(SETUP);
					byte[] cam = { CAM, (byte) camservopos };
					sendCommand(cam);
					Util.delay(camwait);
					sendCommand(CAMRELEASE);
				}
			}).start();
		}
	}

	/** inner class to send commands as a seperate thread each */
	class Sender extends Thread {
		private byte[] command = null;

		public Sender(final byte[] cmd) {
			// do connection check
			if (!isconnected)
				log.error("not connected");
			else {
				command = cmd;
				this.start();
			}
		}

		public void run() {
			sendCommand(command);
		}
	}

	/** inner class to check if getting responses in timely manor */
	public class WatchDog extends Thread {
		public WatchDog() {
			this.setDaemon(true);
		}

		public void run() {
			Util.delay(SETUP);
			while (true) {

				if (getReadDelta() > DEAD_TIME_OUT) {
					log.error("arduino watchdog time out");
					return; // die, no point living?
				}

					if (getReadDelta() > WATCHDOG_DELAY) {
						new Sender(GET_VERSION);
						Util.delay(WATCHDOG_DELAY);
					}
				}
			
		}
	}

	public abstract void connect();
	
	@Override
	public boolean isConnected() {
		return isconnected;
	}

	public abstract void execute(); /* {
		String response = "";
		for (int i = 0; i < buffSize; i++)
			response += (char) buffer[i];

		// System.out.println("in: " + response);

		// take action as arduino has just turned on
		if (response.equals("reset")) {

			// might have new firmware after reseting
			isconnected = true;
			version = null;
			new Sender(GET_VERSION);
			updateSteeringComp();

		} else if (response.startsWith("version:")) {
			if (version == null) {
				// get just the number
				version = response.substring(response.indexOf("version:") + 8,
						response.length());
				application.message("arduinoculus version: " + version, null,
						null);
			} else
				return;

			// don't bother showing watch dog pings to user screen
		} else if (response.charAt(0) != GET_VERSION[0]) {

			// if sonar enabled will get <sonar back|left|right xxx> as watchdog
			if (response.startsWith("sonar")) {
				final String[] param = response.split(" ");
				final int range = Integer.parseInt(param[2]);

				if (param[1].equals("back")) {
					if (Math.abs(range - state.getInteger(State.sonarback)) > 1)
						state.set(State.sonarback, range);
				} else if (param[1].equals("right")) {
					if (Math.abs(range - state.getInteger(State.sonarright)) > 1)
						state.set(State.sonarright, range);
				}

				// must be an echo
			} else
				application.message("arduinoculus: " + response, null, null);
		}
	}
*/
	
	public void manageInput(){
		try {
			byte[] input = new byte[32];
			int read = in.read(input);
			for (int j = 0; j < read; j++) {
				// print() or println() from arduino code
				if ((input[j] == '>') || (input[j] == 13)
						|| (input[j] == 10)) {
					// do what ever is in buffer
					if (buffSize > 0)
						execute();
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
	
	@Override
	public long getWriteDelta() {
		return System.currentTimeMillis() - lastSent;
	}

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#getVersion()
	 */
	@Override
	public String getVersion() {
		return version;
	}

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#getReadDelta()
	 */
	@Override
	public long getReadDelta() {
		return System.currentTimeMillis() - lastRead;
	}

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#setEcho(boolean)
	 */
	@Override
	public void setEcho(boolean update) {
		if (update)
			new Sender(ECHO_ON);
		else
			new Sender(ECHO_OFF);
	}

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#reset()
	 */
	@Override
	public void reset() {
		if (isconnected) {
			new Thread(new Runnable() {
				public void run() {
					disconnect();
					connect();
				}
			}).start();
		}
	}

	/** shutdown serial port */
	protected void disconnect() {
		try {
			in.close();
			out.close();
			isconnected = false;
			version = null;
		} catch (Exception e) {
			log.error("disconnect(): " + e.getMessage());
		}
		serialPort.close();
	}

	/**
	 * Send a multi byte command to send the arduino
	 * 
	 * @param command
	 *            is a byte array of messages to send
	 */
	protected synchronized void sendCommand(final byte[] command) {

		if (!isconnected)
			return;

		try {

			// send
			out.write(command);

			// end of command
			out.write(13);

		} catch (Exception e) {
			reset();
			log.error(e.getMessage());
		}

		// track last write
		lastSent = System.currentTimeMillis();
	}

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#stopGoing()
	 */
	@Override
	public void stopGoing() {

		if (application.muteROVonMove && moving) {
			application.unmuteROVMic();
		}

		// TODO: BRAD BREAKING
		/*
		 * if(movingforward) {
		 * 
		 * 
		 * new Sender(STOP); new Sender(new byte[] { BACKWARD, (byte)180 });
		 * Util.delay(40);
		 * 
		 * } else {
		 * 
		 * 
		 * 
		 * 
		 * }
		 */

		// nudge("backward");
		// else nudge("forward");

		new Sender(STOP);
		moving = false;
		movingforward = false;

	}

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#goForward()
	 */
	@Override
	public void goForward() {
		new Sender(new byte[] { FORWARD, (byte) speed });
		moving = true;
		movingforward = true;

		if (application.muteROVonMove) {
			application.muteROVMic();
		}
	}

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#pollSensor()
	 */
	@Override
	public void pollSensor() {
		if (sonarEnabled)
			new Sender(SONAR);
	}

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#goBackward()
	 */
	@Override
	public void goBackward() {
		new Sender(new byte[] { BACKWARD, (byte) speed });
		moving = true;
		movingforward = false;

		if (application.muteROVonMove) {
			application.muteROVMic();
		}
	}

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#turnRight()
	 */
	@Override
	public void turnRight() {
		int tmpspeed = turnspeed;
		int boost = 10;
		if (speed < turnspeed && (speed + boost) < speedfast)
			tmpspeed = speed + boost;

		new Sender(new byte[] { RIGHT, (byte) tmpspeed });
		moving = true;

		if (application.muteROVonMove) {
			application.muteROVMic();
		}
	}

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#turnLeft()
	 */
	@Override
	public void turnLeft() {
		int tmpspeed = turnspeed;
		int boost = 10;
		if (speed < turnspeed && (speed + boost) < speedfast)
			tmpspeed = speed + boost;

		new Sender(new byte[] { LEFT, (byte) tmpspeed });
		moving = true;

		if (application.muteROVonMove) {
			application.muteROVMic();
		}
	}

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#camGo()
	 */
	@Override
	public void camGo() {
		new Thread(new Runnable() {
			public void run() {
				while (camservodirection != 0) {
					sendCommand(new byte[] { CAM, (byte) camservopos });
					Util.delay(camdelay);
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

				checkForHoldServo();
			}
		}).start();
	}

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#camCommand(java.lang.String)
	 */
	@Override
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
			new Thread(new Runnable() {
				public void run() {
					sendCommand(new byte[] { CAM, (byte) camservopos });
					checkForHoldServo();
				}
			}).start();
		} else if (str.equals("upabit")) {
			camservopos += 5;
			if (camservopos > camposmax) {
				camservopos = camposmax;
			}
			new Thread(new Runnable() {
				public void run() {
					sendCommand(new byte[] { CAM, (byte) camservopos });
					checkForHoldServo();
				}
			}).start();
		}
		// else if (str.equals("hold")) {
		// new Thread(new Runnable() { public void run() {
		// sendCommand(new byte[] { CAM, (byte) camservopos });
		// } }).start();
		// }
	}

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#camHoriz()
	 */
	@Override
	public void camHoriz() {
		camservopos = camservohoriz;
		new Thread(new Runnable() {
			public void run() {
				try {
					byte[] cam = { CAM, (byte) camservopos };
					sendCommand(cam);
					checkForHoldServo();

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#camToPos(java.lang.Integer)
	 */
	@Override
	public void camToPos(Integer n) {
		camservopos = n;
		new Thread(new Runnable() {
			public void run() {
				try {
					sendCommand(new byte[] { CAM, (byte) camservopos });
					checkForHoldServo();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#speedset(java.lang.String)
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#nudge(java.lang.String)
	 */
	@Override
	public void nudge(String dir) {
		direction = dir;
		new Thread(new Runnable() {
			public void run() {
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

				Util.delay(n);

				if (movingforward == true) {
					goForward();
				} else {
					stopGoing();
				}
			}
		}).start();
	}

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#slide(java.lang.String)
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#slidecancel()
	 */
	@Override
	public void slidecancel() {
		if (sliding == true) {
			if (tempspeed != 999) {
				speed = tempspeed;
				sliding = false;
			}
		}
	}

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#clickSteer(java.lang.String)
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#clickNudge(java.lang.Integer)
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#clickCam(java.lang.Integer)
	 */
	@Override
	public Integer clickCam(Integer y) {
		Integer n = maxclickcam * y / 240;
		camservopos -= n;
		if (camservopos > camposmax) {
			camservopos = camposmax;
		}
		if (camservopos < camposmin) {
			camservopos = camposmin;
		}

		new Thread(new Runnable() {
			public void run() {
				try {
					byte[] command = { CAM, (byte) camservopos };
					sendCommand(command);
					checkForHoldServo();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		return camservopos;
	}

	/* (non-Javadoc)
	 * @see oculus.ArduioPort#releaseCameraServo()
	 */
	@Override
	public void releaseCameraServo() {
		new Thread(new Runnable() {
			public void run() {
				try {
					sendCommand(CAMRELEASE);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	public void checkForHoldServo() {

		if (application.stream == null) return;

		if (!holdservo || application.stream.equals("stop")
				|| application.stream.equals("mic")
				|| application.stream == null) {
			Util.delay(camwait);
			sendCommand(CAMRELEASE);
		}
	}

	@Override
	public void updateSteeringComp() {
		byte[] command = { COMP, (byte) steeringcomp };
		new Sender(command);
	}

}