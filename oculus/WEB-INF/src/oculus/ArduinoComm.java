package oculus;

import java.io.OutputStream;
import gnu.io.*;

public class ArduinoComm {
	protected OutputStream out;
	protected CommPort commPort;
	Settings settings= new Settings();

	protected int leftzero = Integer.parseInt(settings.readSetting("leftzero"));  
	protected int rightzero = Integer.parseInt(settings.readSetting("rightzero"));
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
	protected int bearturnspeed = Integer.parseInt(settings.readSetting("bearturnspeed"));
	
	protected int camservodirection = 0;
	protected int camservopos = camservohoriz;
	protected int speedfast = 256;
	protected int speedoffset = speedfast; // set default to max
	protected int arduinodelay = 4;
	protected String direction = null;
	protected boolean moving = false;
	volatile boolean sliding = false;
	volatile boolean movingforward = false;
	int tempspeedoffset = 999;
	int clicknudgedelay = 0;
	String tempstring = null;
	int tempint = 0;
	long nextcommandtime = 0;
	Boolean isconnected = false;
	
	public void connect(String str) throws Exception { // open port, enable write, initialize
		CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(str);
		/*
		if (portIdentifier.isCurrentlyOwned()) {
			System.out.println("Error: Port is currently in use");
		} else {
		*/
			commPort = portIdentifier.open(this.getClass().getName(), 2000);
			SerialPort serialPort = (SerialPort) commPort;
			serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			out = serialPort.getOutputStream();
			isconnected = true;
		//}
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
		if (isconnected) {
			if (nextcommandtime ==0) { nextcommandtime = System.currentTimeMillis(); }
			int n=0;
			long now = System.currentTimeMillis();
			if (now < nextcommandtime) {
				n = (int) (nextcommandtime - now );
			}
			if (n<0) { n=0; }
			nextcommandtime = now + n + arduinodelay;
			if (command2 != -1) { nextcommandtime += arduinodelay; }
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
					int n;
					n = leftzero - speedoffset;
					if (n<0) { n=0; }
					sendcommand(5,n);
					n = rightzero + speedoffset;
					if (n>180) { n=180; }
					sendcommand(6,n);
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
					int n;
					n = leftzero + speedoffset;
					if (n>180) { n=180; }
					sendcommand(5,n);
					n = rightzero - speedoffset;
					if (n<0) { n=0; }
					sendcommand(6,n);
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
					int n;
					int tmpoffset = turnspeed;
					if (speedoffset < turnspeed ) { tmpoffset = speedoffset; }
					n = leftzero - tmpoffset;
					if (n<0) { n=0; }
					sendcommand(5,n);
					n = rightzero- tmpoffset;
					if (n<0) { n=0; }
					sendcommand(6,n);
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
					int n;
					int tmpoffset = turnspeed;
					if (speedoffset < turnspeed) { tmpoffset = speedoffset; }
					n = leftzero + tmpoffset;
					if (n>180) { n=180; }
					sendcommand(5,n);
					n = rightzero + tmpoffset;
					if (n>180) { n=180; }
					sendcommand(6,n);
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
			camservodirection = -1;
			camGo();
		}
		if (str.equals("down")) {
			camservodirection = 1;
			camGo();
		}
		if (str.equals("horiz")) {
			camHoriz();
		}
		if (str.equals("upabit")) {
			camservopos -= 5;
			if (camservopos < camposmin) {
				camservopos = camposmin;
			}
			new Thread(new Runnable() {
				public void run() {
					try {
						//arduinoDelay(-1); // testing this here
						sendcommand(4,camservopos);
						Thread.sleep(200);
						sendcommand(8,-1); // release
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
		if (str.equals("downabit")) {
			camservopos += 5;
			if (camservopos > camposmax) {
				camservopos = camposmax;
			}
			new Thread(new Runnable() {
				public void run() {
					try {
						// arduinoDelay(-1); // testing this here
						sendcommand(4,camservopos);
						Thread.sleep(200);
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
					Thread.sleep(200);
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
		if (str.equals("slow")) { speedoffset = speedslow; }
		if (str.equals("med")) { speedoffset = speedmed; }
		if (str.equals("fast")) { speedoffset = speedfast; }
		if (movingforward) { goForward(); }
	}
	
	public void driveMotorsEnable(String str) { //for zero testing
		new Thread(new Runnable() {
			public void run() {
				try {
				sendcommand(5,leftzero);
				sendcommand(6,rightzero);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
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
					//turnspeed = tempturnspeed;
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
			tempspeedoffset = 999;
			new Thread(new Runnable() {
				public void run() {
					try {
						int distance = 300;
						int turntime = 500;
						tempspeedoffset = speedoffset;
						speedoffset = speedfast;
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
										speedoffset = tempspeedoffset;									
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
			if (tempspeedoffset != 999) {
				speedoffset = tempspeedoffset;
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
							Thread.sleep(250);
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
		new Thread(new Runnable() {
			public void run() {
				try {
					tempspeedoffset = speedoffset;
					speedoffset = speedfast;
					if (direction.equals("right")) { turnRight(); }
					else { turnLeft(); }
					Thread.sleep(clicknudgedelay);
					speedoffset = tempspeedoffset;
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
		camservopos += n;
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
					Thread.sleep(200);
					sendcommand(8,-1); // release
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		return camservopos;
	}
	
	/*
	public void bear(String dir) {
		direction = dir;
		new Thread(new Runnable() {
			public void run() {
				try {
					int n;
					if (direction.equals("left")) {
						n = leftzero - bearturnspeed;
						if (n<0) { n=0; }
						sendcommand(5,n);
						sendcommand(6,180);
					}
					if (direction.equals("right")) {
						n = rightzero + bearturnspeed;
						if (n>180) { n=180; }
						sendcommand(5,0);
						sendcommand(6,n);
					}
					if (direction.equals("left_bwd")) {
						n = leftzero + bearturnspeed;
						if (n>180) { n=180; }
						sendcommand(5,n);
						sendcommand(6,0);
					}
					if (direction.equals("right_bwd")) {
						n = rightzero - bearturnspeed;
						if (n<0) { n=0; }
						sendcommand(5,180);
						sendcommand(6,n);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		moving = true;
	}
	*/

}


