package oculus;

import java.io.OutputStream;
import gnu.io.*;

public class ArduinoCommDC {
	protected OutputStream out;
	protected CommPort commPort;
	Settings settings= new Settings();

	protected int speedslow = Integer.parseInt(settings.readSetting("speedslow"));
	protected int speedmed = Integer.parseInt(settings.readSetting("speedmed"));
	protected int turnspeed = Integer.parseInt(settings.readSetting("turnspeed"));
	protected int camservohoriz = Integer.parseInt(settings.readSetting("camservohoriz"));
	protected int camposmax = Integer.parseInt(settings.readSetting("camposmax"));
	protected int camposmin = Integer.parseInt(settings.readSetting("camposmin"));
	protected int camdelay = Integer.parseInt(settings.readSetting("camdelay"));
	protected int nudgedelay = Integer.parseInt(settings.readSetting("nudgedelay"));
	protected int maxclicknudgedelay = Integer.parseInt(settings.readSetting("maxclicknudgedelay"));
	protected int maxclickcam = Integer.parseInt(settings.readSetting("maxclickcam"));
	protected double clicknudgemomentummult  = Double.parseDouble(settings.readSetting("clicknudgemomentummult"));
	protected int steeringcomp = Integer.parseInt(settings.readSetting("steeringcomp")); 
	
	protected int camservodirection = 0;
	protected int camservopos = camservohoriz;
	protected int camwait= 400;
	protected int speedfast = 255;
	protected int speed = speedfast; // set default to max
	protected int arduinodelay = 4;
	protected String direction = null;
	protected boolean moving = false;
	volatile boolean sliding = false;
	volatile boolean movingforward = false;
	int tempspeed = 999;
	int clicknudgedelay = 0;
	String tempstring = null;
	int tempint = 0;
	long nextcommandtime = 0;
	Boolean isconnected = false;
	
	
	public void connect(String str) throws Exception { // open port, enable write, initialize
		CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(str);
		commPort = portIdentifier.open(this.getClass().getName(), 2000);
		SerialPort serialPort = (SerialPort) commPort;
		serialPort.setSerialPortParams(19200, SerialPort.DATABITS_8,
				SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		out = serialPort.getOutputStream();
		isconnected = true;
	}

	protected void disconnect() {
		try {
			out.close();
			isconnected = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		commPort.close();
	}

	public void sendcommand(int command1, int command2) {
		// System.out.println(command1+" and "+command2);
		if (isconnected) {
			if (nextcommandtime ==0) { nextcommandtime = System.currentTimeMillis(); }
			int n=0;
			long now = System.currentTimeMillis();
			if (now < nextcommandtime) {
				n = (int) (nextcommandtime - now + 1); // may not need +1
			}
			if (n<0) { n=0; } 
			nextcommandtime = now + n + arduinodelay;
			if (command2 != -1) { nextcommandtime += arduinodelay +1; } // may not need +1
			try {
				Thread.sleep(n);
				out.write(command1);
				if (command2 != -1) {
					n= arduinodelay;
					Thread.sleep(n);
					out.write(command2);
				}
			}
			catch (Exception e) { e.printStackTrace(); }
		}
	}
	
	public void stopGoing() { 
		moving = false;
		movingforward = false;
		new Thread(new Runnable() {
			public void run() {
				try {
					sendcommand(3,-1);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	public void goForward() {
		new Thread(new Runnable() {
			public void run() {
				try {
					sendcommand(1,speed);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		moving = true;
		movingforward = true;
	}
	
	public void goBackward() {
		new Thread(new Runnable() {
			public void run() {
				try {
					sendcommand(2,speed);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		moving = true;
		movingforward = false;
	}

	public void turnRight() {
		new Thread(new Runnable() {
			public void run() {
				try {
					int tmpspeed = turnspeed;
					int boost = 10;
					if (speed < turnspeed && (speed+boost)<speedfast) { tmpspeed = speed + boost; }
					sendcommand(5,tmpspeed);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		moving = true;
	}
	
	public void turnLeft() {
		new Thread(new Runnable() {
			public void run() {
				try {
					int tmpspeed = turnspeed;
					int boost = 10;
					if (speed < turnspeed && (speed+boost)<speedfast) { tmpspeed = speed + boost; }
					sendcommand(6,tmpspeed);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		moving = true;
	}
	
	public void camGo() {
		new Thread(new Runnable() {
			public void run() {
				try {
					while (camservodirection != 0) {
						sendcommand(4,camservopos);
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
					sendcommand(8,-1); // release
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	public void camCommand(String str) {
		if (str.equals("stop")) {
			camservodirection = 0;
		}
		if (str.equals("up")) {
			camservodirection = 1;
			camGo();
		}
		if (str.equals("down")) {
			camservodirection = -1;
			camGo();
		}
		if (str.equals("horiz")) {
			camHoriz();
		}
		if (str.equals("downabit")) {
			camservopos -= 5;
			if (camservopos < camposmin) {
				camservopos = camposmin;
			}
			new Thread(new Runnable() {
				public void run() {
					try {
						//arduinoDelay(-1); // testing this here
						sendcommand(4,camservopos);
						Thread.sleep(camwait);
						sendcommand(8,-1); // release
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
		if (str.equals("upabit")) {
			camservopos += 5;
			if (camservopos > camposmax) {
				camservopos = camposmax;
			}
			new Thread(new Runnable() {
				public void run() {
					try {
						// arduinoDelay(-1); // testing this here
						sendcommand(4,camservopos);
						Thread.sleep(camwait);
						sendcommand(8,-1); // release
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}
	
	public void camHoriz() {

		new Thread(new Runnable() {
			public void run() {
				try {
					// arduinoDelay(-1); // testing this here
					sendcommand(4,camservohoriz);
					camservopos = camservohoriz;
					Thread.sleep(camwait);
					sendcommand(8,-1); // release
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		
	}
	
	public void camHoldStill() { // unused
		new Thread(new Runnable() {
			public void run() {
				try {
					sendcommand(4,camservopos);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	public void speedset(String str) {
		if (str.equals("slow")) { speed = speedslow; }
		if (str.equals("med")) { speed = speedmed; }
		if (str.equals("fast")) { speed = speedfast; }
		if (movingforward) { goForward(); }
	}
	
	
	public void nudge(String dir) {
		direction = dir;
		new Thread(new Runnable() {
			public void run() {
				try {
					int n = nudgedelay;
					if (direction.equals("right")) { turnRight(); }
					if (direction.equals("left")) { turnLeft(); }
					if (direction.equals("forward")) { goForward(); movingforward = false; n *= 4; }
					if (direction.equals("backward")) { goBackward(); n *= 4; }
					Thread.sleep(n);
					if (movingforward == true) { goForward(); } 
					else { stopGoing(); } 
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
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
						}
						else {
							turnRight();
						}
						Thread.sleep(turntime);
						if (sliding == true) {
							goBackward();
							Thread.sleep(distance);
							if (sliding == true ) {
								if (direction.equals("right")) {
									turnRight();
								}
								else {
									turnLeft();
								}
								Thread.sleep(turntime);
								if (sliding == true) {
									goForward();
									Thread.sleep(distance);
									if (sliding == true ) {
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
		if (Integer.parseInt(xy[1]) != 0) { tempint = clickCam(Integer.parseInt(xy[1])); }
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
		if (x>0) { direction = "right"; }
		else { direction = "left"; }
		clicknudgedelay = maxclicknudgedelay*(Math.abs(x))/320;  
		/*
		 * multiply clicknudgedelay by multiplier
		 * multiplier increases to CONST(eg 2) as x approaches 0, 1 as approaches 320
		 *  ((320-Math.abs(x))/320)*1+1 
		 */
		double mult = Math.pow(((320.0-(Math.abs(x)))/320.0),3)*clicknudgemomentummult+1.0;
		//System.out.println("clicknudgedelay-before: "+clicknudgedelay);
		clicknudgedelay = (int) (clicknudgedelay * mult);
		//System.out.println("n: "+clicknudgemomentummult+" mult: "+mult+" clicknudgedelay-after: "+clicknudgedelay);
		new Thread(new Runnable() {
			public void run() {
				try {
					tempspeed = speed;
					speed = speedfast;
					if (direction.equals("right")) { turnRight(); }
					else { turnLeft(); }
					Thread.sleep(clicknudgedelay);
					speed = tempspeed;
					if (movingforward == true) { goForward(); }
					else { stopGoing(); }
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	public Integer clickCam(Integer y) {
		Integer n = maxclickcam*y/240;
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
					sendcommand(4,camservopos);
					Thread.sleep(camwait+clicknudgedelay);
					sendcommand(8,-1); 
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		return camservopos;
	}
	
	public void camToPos(Integer n) {
		camservopos = n;
		new Thread(new Runnable() {
			public void run() {
				try {
					sendcommand(4,camservopos);
					Thread.sleep(camwait);
					sendcommand(8,-1); // release
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	public void updateSteeringComp() {
		new Thread(new Runnable() {
			public void run() {
				try {
					sendcommand(9,steeringcomp);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}
