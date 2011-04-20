package oculus;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Set;

import javax.imageio.ImageIO;

import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IServiceCapableConnection;
import org.jasypt.util.password.*;
import org.red5.io.amf3.ByteArray;
import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

public class Application extends MultiThreadedApplicationAdapter {
	IConnection grabber;
	IConnection player = null;
	protected ArduinoCommDC comport = null; 
	protected Speech sayit = new Speech("kevin16");
	protected BatteryLife bl;
	Settings settings= new Settings();
	volatile boolean docking = false;
	protected boolean initialstatuscalled = false;
	boolean batterypresent;
	boolean motionenabled = false;
	private static String salt = "PSDLkfkljsdfas345usdofi";
	ConfigurablePasswordEncryptor passwordEncryptor = new ConfigurablePasswordEncryptor();
	String userconnected = null; 
	String pendinguserconnected = null; 
	String remember = null;
	IConnection pendingplayer = null;
	boolean pendingplayerisnull = true;
	String stream = "stop";
	// WifiConnection wifi; // = new WifiConnection();
	boolean battcharging;
	boolean admin = false;
	private static Logger log = Red5LoggerFactory.getLogger(Application.class, "oculus");
	String dockstatus = "";
	String httpPort; 
	boolean facegrabon = false;
	boolean autodocking = false;
	boolean autodockingcamctr = false;
	int autodockgrabattempts;
	int autodockctrattempts;
	String docktarget;
	protected String portstr = null;
	
	public Application() { 
		super();
		passwordEncryptor.setAlgorithm("SHA-1");
		passwordEncryptor.setPlainDigest(true);
		initialize();
	}

	public boolean appConnect(IConnection connection, Object[] params) { // override
		String logininfo[] = ((String) params[0]).split(" ");
		if ((connection.getRemoteAddress()).equals("127.0.0.1") && logininfo[0].equals("")) { // always accept local grabber
			return true;
		}
		
		if (logininfo.length == 1) { //test for cookie auth
			String username = logintest("",logininfo[0]);
			if (username != null) {
				pendinguserconnected = username;
				return true;
			}
		}
		if (logininfo.length > 1) { //test for user/pass/remember
			String encryptedPassword = (passwordEncryptor.encryptPassword(logininfo[0]+salt+logininfo[1])).trim();
			if (logintest(logininfo[0], encryptedPassword) != null) {
				if (logininfo[2].equals("remember")) {
					remember = encryptedPassword;
				}
				pendinguserconnected = logininfo[0];
				return true;
			}
		}
		String str = "login from: "+connection.getRemoteAddress()+" failed";
		log.info(str); messageGrabber(str,"");
		return false;
	}
	
	public void appDisconnect(IConnection connection) {
		if (connection.equals(player)) {
			String str = userconnected+" disconnected";
			log.info(str); 
			messageGrabber(str,"connection awaiting&nbsp;connection");
			if (userconnected.equals(settings.readSetting("user0"))) {
				admin = false;
			}
			userconnected = null;
			player = null;
			facegrabon = false;
			if (!autodocking) {
				if (!stream.equals("stop")) { publish("stop"); }
				if (comport.moving) { comport.stopGoing(); }
			}
		}
		if (connection.equals(grabber)) {
			grabber = null;
			// log.info("grabber disconnected");
			// wait a bit, see if still no grabber, THEN reload
			new Thread(new Runnable() {
				public void run() {
					try {
						Thread.sleep(8000);
						if (grabber==null) {
							initialize();							
						}
					} catch (Exception e) {
						e.printStackTrace();
					} 
				}
			}).start();
		}
	}

	public void grabbersignin() {
		grabber = Red5.getConnectionLocal();
		String str = "awaiting&nbsp;connection";
		if (userconnected != null) { str = userconnected+"&nbsp;connected"; }
		str += " stream "+stream;
		messageGrabber("connected to subsystem","connection "+ str);
		log.info("grabber signed in from "+grabber.getRemoteAddress());
		//System.out.println("grabbbersignin");
		stream = "stop";
		
		//eliminate any other grabbers
		int i = 0;
		Collection<Set<IConnection>> concollection = getConnections();
		for (Set<IConnection> cc : concollection) {
			for (IConnection con : cc) {
				if (con instanceof IServiceCapableConnection && con != grabber && con != player && (con.getRemoteAddress()).equals("127.0.0.1")) {
					con.close();
					i ++;
				}
			}
		}
	}

	public void initialize() {
		
		FindPort find = new FindPort();
		portstr = find.search(FindPort.OCULUS_DC);
		if (portstr != null) {
			// true for watch dog enabled 
			comport = new ArduinoCommDC(portstr, true, this);
			comport.connect();
			// not used 
			// initializeAfterDelay();	
		}
		
		httpPort = settings.readRed5Setting("http.port");
		if (settings.readSetting("skipsetup").equals("yes")) {
			grabber_launch();
		} else { 
			initialize_launch(); 
		}
		
		// watch for low battery, start on config flag??
		// new EmailAlerts().start();
		
		log.info("initialize");
	}
	
	public void initialize_launch() {
		new Thread(new Runnable() {
			public void run() {
				try {
					String address="127.0.0.1:"+ httpPort;
					Runtime.getRuntime().exec("cmd.exe /c start http://" + address+ "/oculus/initialize.html");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	public void grabber_launch() {
		if (((settings.readSetting("batterypresent")).toUpperCase()).equals("YES")) {
			batterypresent = true; 
		} else { 
			batterypresent = false;
			motionenabled = true;
		}
		
		new Thread(new Runnable() {
			public void run() {
				try {
					//String address = settings.readSetting("address") + ":"+ settings.readSetting("http_port");
					String address="127.0.0.1:"+ httpPort;
					Runtime.getRuntime().exec("cmd.exe /c start http://" + address+ "/oculus/server.html");
					// Runtime.getRuntime().exec("cmd.exe /c start /MIN http://" + address+ "/oculus/grabber.html");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	public void playersignin() {
		if (player != null) { 
			pendingplayer = Red5.getConnectionLocal();
			pendingplayerisnull = false;
			if (pendingplayer instanceof IServiceCapableConnection) {
				IServiceCapableConnection sc = (IServiceCapableConnection) pendingplayer;
				String str = "connection PENDING user "+pendinguserconnected;
				if (remember != null) { 
					//System.out.println("sending store cookie");
					str += " storecookie "+remember; 
					remember = null;
				}
				str += " someonealreadydriving "+userconnected; // this has to be last to above variables are already set in javascript
				sc.invoke("message", new Object[] { null, "green", "multiple", str});
				str = pendinguserconnected + " pending connection from: "+pendingplayer.getRemoteAddress();
				log.info(str); messageGrabber(str,null);
			}
		}
		else {
			player = Red5.getConnectionLocal();
			userconnected = pendinguserconnected;
			String str = "connection connected user "+userconnected;
			if (remember != null) { 
				str += " storecookie "+remember; 
				remember = null;
			}
			str += " streamsettings "+streamSettings();
			messageplayer(userconnected + " connected to OCULUS", "multiple", str);
			initialstatuscalled = false;
			if (batterypresent) { bl = new BatteryLife(); }
			// if (((settings.readSetting("wifienabled")).toUpperCase()).equals("YES")) {
			//	wifi = new WifiConnection();  }
			if (userconnected.equals(settings.readSetting("user0"))) {
				admin = true;
			}
			else { admin = false; }
			// System.out.println(userconnected+" connected");
			//log.info(userconnected+" connected");
			str = userconnected + " connected from: "+player.getRemoteAddress();
			log.info(str); 
			messageGrabber(str,"connection "+userconnected+"&nbsp;connected");
		}
	}

	public void playerCallServer(String fn, String str) { // distribute commands from player
		if (Red5.getConnectionLocal() == player) {
			if (fn.equals("publish")) { 
				publish(str);
				messageplayer("command received: publish " + str, null, null);
			}
			if (fn.equals("move")) {
				move(str);
			}
			if (fn.equals("speech")) {
				saySpeech(str);
			}
			if (fn.equals("battStats")) {
				battStats();
			}
			if (fn.equals("getdrivingsettings")) {
				getDrivingSettings();
			}
			if (fn.equals("drivingsettingsupdate")) {
				drivingSettingsUpdate(str);
			}
			if (fn.equals("cameracommand")) {
				cameraCommand(str);
			}
			if (fn.equals("gettiltsettings")) {
				getTiltSettings();
			}
			if (fn.equals("tiltsettingsupdate")) {
				tiltSettingsUpdate(str);
			}
			if (fn.equals("tilttest")) {
				tiltTest(str);
			}
			if (fn.equals("speedset")) {
				comport.speedset(str);
				messageplayer("speed set: " + str, "speed", str.toUpperCase());
			}
			if (fn.equals("nudge")) {
				nudge(str);
			}
			
			if (fn.equals("slide")) {
				if (motionenabled == true) {
					moveMacroCancel();
					comport.slide(str);
					messageplayer("command received: " + fn + str, null, null);
				}
				else {
					messageplayer("motion disabled", "motion", "disabled");
				}
			}
			
			if (fn.equals("dock")) {
				dock(str);
			}
			if (fn.equals("relaunchgrabber")) {
				if (admin) {
					grabber_launch();
					messageplayer("relaunching grabber", null, null);
				}
			}
			if (fn.equals("statuscheck")) { statusCheck(str); }
			if (fn.equals("streamsettingscustom")) { streamSettingsCustom(str); }
			if (fn.equals("streamsettingsset")) { streamSettingsSet(str); } 
			if (fn.equals("systemcall")) { messageplayer("system command received", null, null); systemCall(str); }
			if (fn.equals("clicksteer")) { clickSteer(str); }
			if (fn.equals("motionenabletoggle")) { motionEnableToggle(); }
			if (fn.equals("playerexit")) { appDisconnect(player); } //  System.out.println("onunload called"); }
			/*
			if (fn.equals("wifisignalstrength")) {
				String s = wifi.wifiSignalStrength();
				messageplayer("signal: "+s, "wifi", s); 
			}
			*/
			if (fn.equals("playerbroadcast")) { playerBroadCast(str); }
			if (fn.equals("docklineposupdate") && admin) { 
				settings.writeSettings("vidctroffset", str);
				messageplayer("vidctroffset set to : " + str, null, null);
			}
			if (fn.equals("password_update")) { account("pasword_update",str); }
			if (fn.equals("new_user_add")) { account("new_user_add",str); }
			if (fn.equals("user_list")) { account("user_list",""); }
			if (fn.equals("delete_user")) { account("delete_user",str); }
			if (fn.equals("extrauser_password_update")) { account("extrauser_password_update", str); }
			if (fn.equals("username_update")) { account("username_update",str); }
			if (fn.equals("disconnectotherconnections")) { disconnectOtherConnections(); }
			if (fn.equals("monitor")) { monitor(str); }
			if (fn.equals("showlog")) { showlog(); }
			if (fn.equals("framegrab")) {
				if (grabber instanceof IServiceCapableConnection) {
					IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
					sc.invoke("framegrab", new Object[] { });
					messageplayer("framegrab command received", null, null);
				}
			}
			if (fn.equals("facegrab")) { faceGrab(str); }
			if (fn.equals("autodockgo")) { autoDock("go "+str); }
			if (fn.equals("autodock")) { autoDock(str); }
			if (fn.equals("autodockcalibrate")) { autoDock("calibrate "+str); } // eliminate, combine into 'autodock'
			if (fn.equals("restart")) { restart(); }
			if (fn.equals("softwareupdate")) { softwareUpdate(str); }
		}
		if (fn.equals("assumecontrol")) { assumeControl(str); }
		if (fn.equals("beapassenger")) { beAPassenger(str); }
		if (fn.equals("chat")) { chat(str); }
 	}
	
	public void grabberCallServer(String fn, String str) { // distribute commands from grabber
		if (fn.equals("streammode")) { grabberSetStream(str); }
		if (fn.equals("saveandlaunch")) { saveAndLaunch(str); }
		if (fn.equals("populatesettings")) { populateSettings(); }
		if (fn.equals("systemcall")) {
			str = str.trim();
			try {
				Runtime.getRuntime().exec(str);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (fn.equals("chat")) { chat(str); }
		if (fn.equals("facerect")) { messageplayer(null, "facefound", str); }
		if (fn.equals("dockgrabbed")) { autoDock("dockgrabbed "+str); }
		if (fn.equals("autodock")) { autoDock(str); }
		if (fn.equals("restart")) { restart(); }
	}
	
	private void grabberSetStream(String str) {
		stream = str;
		//messageplayer("streaming "+str,"stream",stream);
		messageGrabber("streaming "+stream,"stream "+stream);
		log.info("streaming "+stream);
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(2000);
					Collection<Set<IConnection>> concollection = getConnections();
					for (Set<IConnection> cc : concollection) {
						for (IConnection con : cc) {
							if (con instanceof IServiceCapableConnection && con != grabber && !(con == pendingplayer && !pendingplayerisnull)) {
									IServiceCapableConnection n = (IServiceCapableConnection) con;
									n.invoke("message", new Object[] { "streaming "+stream, "green", "stream",stream });
							}
						}
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();		
	}

	private void publish(String str) {
		if (grabber instanceof IServiceCapableConnection) {
			IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
			String current = settings.readSetting("vset");
			String vals[] = (settings.readSetting(current)).split("_");
			int width = Integer.parseInt(vals[0]);
			int height = Integer.parseInt(vals[1]);
			int fps = Integer.parseInt(vals[2]);
			int quality = Integer.parseInt(vals[3]);
			sc.invoke("publish", new Object[] { str, width, height, fps, quality });
			//messageGrabber("stream "+str);
		}
	}
	
	public void frameGrabbed(ByteArray _RAWBitmapImage) {
		String str= "frame grabbed <a href='images/framegrab.png' target='_blank'>view</a>";
		messageplayer(str, null, null);
		// Use functionality in org.red5.io.amf3.ByteArray to get parameters of the ByteArray
		int BCurrentlyAvailable = _RAWBitmapImage.bytesAvailable();
		int BWholeSize = _RAWBitmapImage.length(); // Put the Red5 ByteArray into a standard Java array of bytes
		byte c[] = new byte[BWholeSize];
		_RAWBitmapImage.readBytes(c);

		// Transform the byte array into a java buffered image
		ByteArrayInputStream db = new ByteArrayInputStream(c);

		if(BCurrentlyAvailable > 0) {
		System.out.println("The byte Array currently has " + BCurrentlyAvailable + " bytes. The Buffer has " + db.available());
		try{
		BufferedImage JavaImage = ImageIO.read(db);
		// Now lets try and write the buffered image out to a file
		if(JavaImage != null) { // If you sent a jpeg to the server, just change PNG to JPEG and Red5ScreenShot.png to .jpeg
		ImageIO.write(JavaImage, "PNG", new File(System.getenv("RED5_HOME")+"\\webapps\\oculus\\images\\framegrab.png"));
		}

		} catch(IOException e) {log.info("Save_ScreenShot: Writing of screenshot failed " + e); System.out.println("IO Error " + e);}
		}
	}
		
	private void messageplayer(String str, String status, String value) {
		if (player instanceof IServiceCapableConnection) {
			IServiceCapableConnection sc = (IServiceCapableConnection) player;
			sc.invoke("message", new Object[] { str, "green", status, value });
		}
	}

	private void sendplayerfunction(String fn, String params) {
		if (player instanceof IServiceCapableConnection) {
			IServiceCapableConnection sc = (IServiceCapableConnection) player;
			sc.invoke("playerfunction", new Object[] { fn, params });
		}
	}

	private void saySpeech(String str) {
		messageplayer("synth voice: "+str, null, null);
		messageGrabber("synth voice: "+str,null);
		sayit.mluv(str);
		log.info("voice synth: '"+str+"'");
	}

	private void battStats() { 
		new Thread(new Runnable() {
			public void run() {
				if (batterypresent == true && !dockstatus.equals("docking")) {
					int batt[] = bl.battStatsCombined();
					String life = Integer.toString(batt[0]);
					int s = batt[1];
					String status = Integer.toString(s); // in case its not 1 or 2
					String str;
					if (s == 1) {
						status = "draining";
						str = "battery "+life+"%,"+status;
						if (motionenabled== false) {
							motionenabled = true;
							str += " motion enabled";
						}
						if (!dockstatus.equals("un-docked")) {
							dockstatus = "un-docked";
							str += " dock un-docked";
						}
						battcharging = false;
						messageplayer(null, "multiple", str);
					}
					if (s == 2) {
						status = "CHARGING";
						// motionenabled = false ;
						if (life.equals("99") || life.equals("100")) {
							status = "CHARGED";
						}
						battcharging = true;
						str="battery "+life+"%,"+status;
						if (dockstatus.equals("")) {
							dockstatus = "docked";
							str += " dock docked";
						}
						messageplayer(null, "multiple", str);
					}			
				}
			}
		}).start();
	}
	
	private void getDrivingSettings() {
		if (admin) {
			String str = comport.speedslow + " " + comport.speedmed + " " 
					+ comport.nudgedelay + " " + comport.maxclicknudgedelay + " " 
					+ comport.clicknudgemomentummult + " " + comport.steeringcomp; 
			sendplayerfunction("drivingsettingsdisplay", str);
		}
	}

	private void drivingSettingsUpdate(String str) {
		if (admin) {
			String comps[] = str.split(" ");
			comport.speedslow = Integer.parseInt(comps[0]);
			settings.writeSettings("speedslow", Integer.toString(comport.speedslow));
			comport.speedmed = Integer.parseInt(comps[1]);
			settings.writeSettings("speedmed", Integer.toString(comport.speedmed));
			comport.nudgedelay = Integer.parseInt(comps[2]);
			settings.writeSettings("nudgedelay", Integer.toString(comport.nudgedelay));
			comport.maxclicknudgedelay = Integer.parseInt(comps[3]);
			settings.writeSettings("maxclicknudgedelay", Integer.toString(comport.maxclicknudgedelay));
			comport.clicknudgemomentummult = Double.parseDouble(comps[4]);
			settings.writeSettings("clicknudgemomentummult", Double.toString(comport.clicknudgemomentummult));
			int n = Integer.parseInt(comps[5]);
			if (n > 255) { n=255; }
			if (n < 0) { n=0; }
			comport.steeringcomp = n;
			settings.writeSettings("steeringcomp", Integer.toString(comport.steeringcomp));
			comport.updateSteeringComp();
			String s = comport.speedslow + " " + comport.speedmed + " "
					+ comport.nudgedelay + " " + comport.maxclicknudgedelay + " " 
					+ comport.clicknudgemomentummult + " " + (comport.steeringcomp -128);
			messageplayer("driving settings set to: " + s, null, null);
		}
	}

	public void wasReset(){
		messageplayer("firmware reseting", null, null);
		new Thread(new Runnable() { public void run() {
			try {
				Thread.sleep(300);
				messageplayer("firmware version: " + comport.getVersion(), null, null);
			}catch (Exception e) {
				log.error(e.getMessage());
			}
		}}).start();
	}
	
	private void getTiltSettings() {
		if (admin) {
			String str = comport.camservohoriz + " " + comport.camposmax + " "
					+ comport.camposmin + " " + comport.maxclickcam;
			sendplayerfunction("tiltsettingsdisplay", str);
		}
	}

	private void tiltSettingsUpdate(String str) {
		if (admin) {
			String comps[] = str.split(" ");
			comport.camservohoriz = Integer.parseInt(comps[0]);
			settings.writeSettings("camservohoriz", Integer.toString(comport.camservohoriz));
			comport.camposmax = Integer.parseInt(comps[1]);
			settings.writeSettings("camposmax", Integer.toString(comport.camposmax));
			comport.camposmin = Integer.parseInt(comps[2]);
			settings.writeSettings("camposmin", Integer.toString(comport.camposmin));
			comport.maxclickcam = Integer.parseInt(comps[3]);
			settings.writeSettings("maxclickcam", Integer.toString(comport.maxclickcam));
			String s = comport.camservohoriz + " " + comport.camposmax + " "
					+ comport.camposmin + " " + comport.maxclickcam;
			messageplayer("cam tilt set to: " + s, null, null);
		}
	}
	
	private void tiltTest(String str) {
		comport.camToPos(Integer.parseInt(str));
		messageplayer("cam position: " + str, null, null);
	}

	private void dock(String str) {
		if (str.equals("dock") && !docking) {
			if (motionenabled == true) {
				if (!battcharging) {
					messageplayer("docking initiated", "multiple", "speed slow motion moving dock docking");
					comport.movingforward = false; // need to set this because speedset calls goForward also if true
					comport.speedset("slow");
					//comport.goForward();
					docking = true;
					dockstatus = "docking";
					//Date d = new Date();
					//dockstarttime = d.getTime();
					new Thread(new Runnable() {
						public void run() {
							int counter = 0;
							int n;
							while (docking == true) {
								n = 500;
								if (counter <= 3) { n += 500; }
								if (counter > 0) { messageplayer(null,"motion","moving"); }
								comport.goForward();
								try {
									Thread.sleep(n);
								} catch (Exception e) {
									e.printStackTrace();
								}
								comport.stopGoing();
								messageplayer(null,"motion","stopped");
								if (bl.batteryStatus() == 2) {
									docking = false;
									String str = "";
									if (autodocking) {
										autodocking = false;
										str += " cameratilt "+camTiltPos()+" autodockcancelled blank";
										if (!stream.equals("stop") && userconnected==null) { publish("stop"); }
									}
									messageplayer("docked successfully", "multiple", "motion disabled dock docked battery charging"+str);
									log.info(userconnected +" docked successfully");
									motionenabled = false;
									dockstatus = "docked"; // needs to be before battStats()
									battStats(); 
									break;
								}
								counter += 1;
								if (counter >12) { // failed
									docking = false;
									String s = "dock un-docked";
									if (comport.moving) { 
										comport.stopGoing();
										s += " motion stopped";
									} 
									messageplayer("docking timed out", "multiple", s);
									log.info(userconnected +" docking timed out");
									dockstatus = "un-docked";
									if (autodocking) {
										new Thread(new Runnable() { public void run() { try {
											comport.speedset("slow");
											comport.goBackward();
											Thread.sleep(2000);
											comport.speedset("fast");
											comport.goBackward();
											Thread.sleep(750);
											comport.stopGoing();
											IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
											sc.invoke("dockgrab", new Object[] {0,0,"find"}); // sends xy, but they're unused
										} catch (Exception e) { e.printStackTrace(); } } }).start();
									}
									
									break;
								}
							}
						}
					}).start();
				}
				else { messageplayer("**battery indicating charging, auto-dock unavailable**", null, null); }
			}
			else { messageplayer("motion disabled", null, null); }
		}
		if (str.equals("undock")) {
			comport.speedset("slow");
			comport.goBackward();
			motionenabled = true;
			messageplayer("un-docking", "multiple", "speed fast motion moving dock un-docked");
			dockstatus = "un-docked";
			new Thread(new Runnable() {
				public void run() {
					try {
						Thread.sleep(2000);
						comport.speedset("fast");
						comport.goBackward();
						Thread.sleep(750);
						comport.stopGoing();
						messageplayer("disengaged from dock", "motion", "stopped");
						log.info(userconnected + " un-docked");
						battStats();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}

	private void moveMacroCancel() {
		if (docking == true) {
			String str = "";
			if (!dockstatus.equals("docked")) {
				dockstatus = "un-docked";
				str += "dock un-docked";
			}
			// if (!comport.moving) { str += " motion stopped"; }
			messageplayer("docking cancelled by movement", "multiple", str);
			docking = false; 
		}
		if (comport.sliding == true) {
			comport.slidecancel();
		}
		if (autodocking == true) {
			autoDock("cancel");
		}
	}
	
	/*
	private void initializeAfterDelay() {
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(3000);
					comport.updateSteeringComp();
					Thread.sleep(25);
					comport.camHoriz();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}*/
	
	private void cameraCommand(String str) {
		comport.camCommand(str);
		messageplayer("tilt command received: " + str, null, null);
		if (!str.equals("up") && !str.equals("down") && !str.equals("horiz")) {
			messageplayer(null,"cameratilt",camTiltPos());
		}
		if (str.equals("horiz")) { 	messageplayer(null,"cameratilt","0&deg;"); }
	}
	
	private String camTiltPos() {
		int n = comport.camservohoriz - comport.camservopos;
		n *= -1;
		String s = "";
		if (n>0) { s="+"; }
		return s+Integer.toString(n)+"&deg;";
	}
	
	private void statusCheck(String s) {
		if (initialstatuscalled==false) {
			initialstatuscalled=true; 
			battStats();
			// signalStrength();
			String spd = "FAST";
			if (comport.speed == comport.speedmed) { spd = "MED"; }
			if (comport.speed== comport.speedslow) { spd = "SLOW"; }
			String mov = "STOPPED";
			if (!motionenabled) { mov = "DISABLED"; }
			if (comport.moving == true) { mov = "MOVING"; }
			String str = "speed "+spd+" cameratilt "+camTiltPos()+" motion "+mov;
			str += " vidctroffset "+Integer.parseInt(settings.readSetting("vidctroffset"));
			str += " stream "+stream;
			//str += " address "+settings.readSetting("address");
			//str += " wifi "+wifi.wifiSignalStrength();
			if (admin) {
				str += " admin true";
			}
			if (!dockstatus.equals("")) { 
				str += " dock "+dockstatus;
			}
			messageplayer("status check received","multiple",str);
		}
		else {
			String str = " stream "+stream;
			// signalStrength();
			messageplayer("status check received","multiple",str);
		}
		if (s.equals("battcheck")) { 
			battStats();
		}
	}
	
	/*
	private void getStreamSettings() {
		String str = settings.readSetting("vwidth") + " " + settings.readSetting("vheight") + " "
				+ settings.readSetting("vfps")+ " " + settings.readSetting("vquality");
		sendplayerfunction("streamsettingsdisplay", str);
	}
	*/
	
	/*
	private void streamSettingsUpdate(String str) {
		String comps[] = str.split(" ");
		settings.writeSettings("vwidth", comps[0]);
		settings.writeSettings("vheight", comps[1]);
		settings.writeSettings("vfps", comps[2]);
		settings.writeSettings("vquality", comps[3]);
		messageplayer("stream settings set to: " + str, null, null);
	}
	*/
	
	private void streamSettingsCustom(String str) {
		settings.writeSettings("vset","vcustom");
		settings.writeSettings("vcustom",str);
		String s = "custom stream set to: "+str;
		if (!stream.equals("stop")) { 
			publish(stream);
			s += "<br>restarting stream";
		}
		messageplayer(s,null,null);
		log.info("stream changed to "+str);
	}
	
	private void streamSettingsSet(String str) {
		settings.writeSettings("vset", "v"+str);
		String s = "stream set to: "+str;
		if (!stream.equals("stop")) { 
			publish(stream);
			s += "<br>restarting stream";
		}
		messageplayer(s,null,null);
		log.info("stream changed to "+str);
	}
	
	private String streamSettings() {
		String result = "";
		result += settings.readSetting("vset")+"_";
		result += settings.readSetting("vlow")+"_"+settings.readSetting("vmed")+"_";
		result += settings.readSetting("vhigh")+"_"+settings.readSetting("vfull")+"_";
		result += settings.readSetting("vcustom");
		return result;
	}
	
	private void systemCall(String str) {
		if (admin) {
			str = str.trim();
			try {
				Runtime.getRuntime().exec(str);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void restart() {
		if (admin) {
			messageplayer("restarting server application", null, null);
			File f;
			f=new File(System.getenv("RED5_HOME")+"\\restart");
			if(!f.exists()){
				try {
					f.createNewFile();
					Runtime.getRuntime().exec("red5-shutdown.bat");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void monitor(String str) {
		// uses nircmd.exe from http://www.nirsoft.net/utils/nircmd.html
		messageplayer("monitor "+str,null,null);
		// String rfh = System.getenv("RED5_HOME");
		// str = rfh +"\\webapps\\oculus\\nircmdc.exe monitor "+str;
		// str = "nircmdc.exe monitor "+str;
		str = str.trim();
		if (str.equals("on")) { str = "cmd.exe /c start monitoron.bat"; }
		else { str = "nircmdc.exe monitor async_off"; }
		try {
			Runtime.getRuntime().exec(str);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void move(String str) {
		String s = "";
		String msg = "motion disabled (try un-dock)";
		if (str.equals("stop")) { 
			comport.stopGoing(); 
			s = "STOPPED"; 
			msg = "command received: "+str;
		}
		if (motionenabled == true) {
			if (str.equals("forward")) { comport.goForward(); }
			if (str.equals("backward")) { comport.goBackward(); }
			if (str.equals("right")) { comport.turnRight(); }
			if (str.equals("left")) { comport.turnLeft(); }
			moveMacroCancel();
			if (s.equals("")) { s = "MOVING"; }
			msg = "command received: "+str;
		}
		else {
			s = "DISABLED";
			// msg set above
		}
		messageplayer(msg, "motion", s);
	}
	
	private void nudge(String str) {
		if (motionenabled == true) {
			comport.nudge(str);
			messageplayer("command received: nudge" + str, null, null);
			if (!docking) { moveMacroCancel(); }
		}
		else {
			messageplayer("motion disabled", "motion", "disabled");
		}
	}
	
	private void motionEnableToggle() {
		if (motionenabled == true) {
			motionenabled = false;
			messageplayer("motion disabled", "motion", "disabled");
		}
		else {
			motionenabled = true;
			messageplayer("motion enabled", "motion", "enabled");
		}
	}
	
	private void clickSteer(String str) {
		if (motionenabled == true) {
			int n = comport.clickSteer(str);
			if (n != 999) {
				messageplayer("received: clicksteer " + str, "cameratilt", camTiltPos());
			}
			else {
				messageplayer("received: clicksteer " + str, null, null);
			}
			moveMacroCancel();
		}
		else { messageplayer("motion disabled", "motion", "disabled"); }
	}
	
	private void messageGrabber(String str, String status) {
		// System.out.println(str);
		if (grabber instanceof IServiceCapableConnection) {
			IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
			sc.invoke("message", new Object[] { str, status });
		}
		// else { System.out.println("not iservicecapableconnection"); }
	}
	
	private String logintest(String user,String pass) {
		int i;
		String value="";
		String returnvalue = null;
		if (user.equals("")) {
			i=0;
			while(true) {
				value = settings.readSetting("pass"+i);
				if (value==null) { break; }
				else {
					if (value.equals(pass)) {
						returnvalue = settings.readSetting("user"+i);
						break;
					}
				}
				i++;
			}
		}
		else  {
			i=0;
			while(true) {
				value = settings.readSetting("user"+i);
				//System.out.println(value);
				if (value==null) { break; }
				else {
					if (value.equals(user)) {
						if ((settings.readSetting("pass"+i)).equals(pass)) {
							returnvalue = user;
						}
						break;
					}
				}
				i++;
			}
		}
		return returnvalue;
	}
	
	private void assumeControl(String user) {
		messageplayer("controls hijacked", "hijacked", user);
		IConnection tmp = player;
		player = pendingplayer;
		pendingplayer = tmp;
		userconnected = user;
		String str = "connection connected streamsettings "+streamSettings();
		messageplayer(userconnected + " connected to OCULUS", "multiple", str);			
		str = userconnected + " connected from: "+player.getRemoteAddress();
		log.info(str); messageGrabber(str,null);
		initialstatuscalled = false;
		pendingplayerisnull = true;
		if (userconnected.equals(settings.readSetting("user0"))) {
			admin = true;
		}
		else { admin = false; }
	}
	
	private void beAPassenger(String user) {
		pendingplayerisnull = true;
		String str = user+ " added as passenger";
		messageplayer(str, null, null);
		log.info(str); messageGrabber(str,null);
		if (!stream.equals("stop")) {
			Collection<Set<IConnection>> concollection = getConnections();
			for (Set<IConnection> cc : concollection) {
				for (IConnection con : cc) {
					if (con instanceof IServiceCapableConnection && con != grabber && con != player) {
						IServiceCapableConnection sc = (IServiceCapableConnection) con;
						//sc.invoke("play", new Object[] { 1 });
						sc.invoke("message", new Object[] { "streaming "+stream, "green", "stream",stream });
					}
				}
			}
		}
	}
	
	/*// causes slowness => JNI conflicts with OS?
	private void signalStrength() {
		if (((settings.readSetting("wifienabled")).toUpperCase()).equals("YES")) {
			new Thread(new Runnable() {
				public void run() {
					String str = wifi.wifiSignalStrength();
					messageplayer(null, "wifi", str);
				}
			}).start();
		}
	}
	*/
	
	private void playerBroadCast(String str) {
		if (player instanceof IServiceCapableConnection) {
			IServiceCapableConnection sc = (IServiceCapableConnection) player;
			if (str.equals("on")) {
				sc.invoke("publish", new Object[] { "on", 160, 120, 8, 85 });
				new Thread(new Runnable() {
					public void run() {
						try { Thread.sleep(2000); }
						catch (Exception e) { e.printStackTrace(); }
						grabberPlayPlayer(1);
					}
				}).start();
				log.info("player broadcast start");
			}
			else { 
				sc.invoke("publish", new Object[] { "stop", null, null, null, null });
				grabberPlayPlayer(0);
				log.info("player broadcast stop");
			}
		}
	}
	
	private void grabberPlayPlayer(int nostreams) {
		if (grabber instanceof IServiceCapableConnection) {
			IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
			sc.invoke("play", new Object[] { nostreams });
		}
	}

	private void account(String fn, String str) { 
		if (fn.equals("pasword_update")) {
			passwordChange(userconnected, str);
		}
		if (admin) {
			if (fn.equals("new_user_add")) {
				String message = "";
				Boolean oktoadd = true;
				String u[] = str.split(" ");
				if(!u[0].matches("\\w+")) {
					message += "error: username must be letters/numbers only ";
					oktoadd = false;
				}
				if(!u[1].matches("\\w+")) {
					message += "error: password must be letters/numbers only ";
					oktoadd = false;
				}
				int i=0;
				String s;
				while(true) {
					s = settings.readSetting("user"+i);
					if (s==null) { break; }
					if ((s.toUpperCase()).equals((u[0]).toUpperCase())) {
						message += "error: user name already exists ";
						oktoadd = false;
					}
					i++;
				}
				//add check for existing user, user loop below to get i while you're at it
				if (oktoadd) {
					message += "added user "+u[0];
					settings.newSetting("user"+i, u[0]);
					String p= u[0]+salt+u[1];
					String encryptedPassword = passwordEncryptor.encryptPassword(p);
					settings.newSetting("pass"+i, encryptedPassword);
				}
				messageplayer(message, null, null);
			}
			if (fn.equals("user_list")) {
				int i=1;
				String users = "";
				String u;
				while(true) {
					u = settings.readSetting("user"+i);
					if (u==null) { break; }
					else { users += u + " "; }
					i++;
				}
				sendplayerfunction("userlistpopulate", users);
			}
			if (fn.equals("delete_user")) {
				int i=1;
				int usernum = -1;
				int maxusernum = -1;
				String[] allusers = new String[999];
				String[] allpasswords = new String[999];
				String u;
				while(true) { // read & store all users+passwords, note number to be deleted, and max number
					u = settings.readSetting("user"+i);
					if (u==null) { 
						maxusernum = i-1;
						break; 
					}
					if (u.equals(str)) {
						usernum = i;
					}
					allusers[i] = u;
					allpasswords[i] = settings.readSetting("pass"+i);
					i++;
				}
				if (usernum>0) {
					i = usernum;
					while(i <= maxusernum) { // delete user to be delted + all after
						settings.deleteSetting("user"+i);
						settings.deleteSetting("pass"+i);
						i ++;
					}
					i = usernum+1;
					while(i<=maxusernum) { // shuffle remaining past deleted one, down one
						settings.newSetting("user"+(i-1), allusers[i]);
						settings.newSetting("pass"+(i-1), allpasswords[i]);
						i ++;
					}
				}
				messageplayer(str+" deleted.", null, null);
			}
			if (fn.equals("extrauser_password_update")) {
				String s[] = str.split(" ");
				passwordChange(s[0],s[1]);
			}
			if (fn.equals("username_update")) {
				String u[] = str.split(" ");
				String message = "";
				Boolean oktoadd = true;
				if(!u[0].matches("\\w+")) {
					message += "error: username must be letters/numbers only ";
					oktoadd = false;
				}
				int i=1;
				String s;
				while(true) {
					s = settings.readSetting("user"+i);
					if (s==null) { break; }
					if ((s.toUpperCase()).equals(u[0].toUpperCase())) {
						message += "error: user name already exists ";
						oktoadd = false;
					}
					i++;
				}
				String encryptedPassword = (passwordEncryptor.encryptPassword(userconnected+salt+u[1])).trim();
				if (logintest(userconnected, encryptedPassword) == null) {
					message += "error: wrong password";
					oktoadd = false;
				}
				if (oktoadd) {
					message += "username changed to: "+u[0];
					messageplayer("username changed to: "+u[0], "user", u[0]);
					settings.writeSettings("user0", u[0]);
					userconnected = u[0];
					String p= u[0]+salt+u[1];
					encryptedPassword = passwordEncryptor.encryptPassword(p);
					settings.writeSettings("pass0", encryptedPassword);
				}
				else { messageplayer(message, null, null); }
			}
		}
	}
	
	private void passwordChange(String user, String pass) {
		String message = "password updated";
		//pass = pass.replaceAll("\\s+$", "");
		if (pass.matches("\\w+")) {
			String p= user+salt+pass;
			String encryptedPassword = passwordEncryptor.encryptPassword(p);
			int i=0;
			String u;
			while(true) {
				u = settings.readSetting("user"+i);
				if (u==null) { break; }
				else {
					if (u.equals(user)) {
						settings.writeSettings("pass"+i, encryptedPassword);
						break;
					}
				}
				i++;
			}
		}
		else {
			message = "error: password must be alpha-numeric with no spaces";
		}
		messageplayer(message, null, null);
	}
	
	private void disconnectOtherConnections() {
		if (admin) {
			int i = 0;
			Collection<Set<IConnection>> concollection = getConnections();
			for (Set<IConnection> cc : concollection) {
				for (IConnection con : cc) {
					if (con instanceof IServiceCapableConnection && con != grabber && con != player) {
						con.close();
						i ++;
					}
				}
			}
			messageplayer(i+" passengers eliminated", null, null);
		}
	}
	
	private void chat(String str) {
		Collection<Set<IConnection>> concollection = getConnections();
		for (Set<IConnection> cc : concollection) {
			for (IConnection con : cc) {
				if (con instanceof IServiceCapableConnection && con != grabber && !(con == pendingplayer && !pendingplayerisnull)) {
					IServiceCapableConnection n = (IServiceCapableConnection) con;
					n.invoke("message", new Object[] { str, "yellow", null, null});
				}
			}
		}
		log.info("(chat) "+str); 
		messageGrabber("<CHAT>"+str ,null); 
	}
	
	private void showlog() {
		if (admin) {
			String filename = System.getenv("RED5_HOME")+"\\log\\oculus.log";
			FileInputStream filein;	
			String str="";
			try
			{
				filein = new FileInputStream(filename);
				BufferedReader reader = new BufferedReader(new InputStreamReader(filein));
				String line = "";
			    while ((line = reader.readLine()) != null) {
			    	str = "&bull; " + line + "<br>" + str;
			    }
			    filein.close();		
			    sendplayerfunction("showserverlog", str);
			}
			catch (Exception e) { e.printStackTrace(); }
		}
	}
	
	private void saveAndLaunch(String str) {
		//System.out.println(str);
		String message = "";
		Boolean oktoadd = true;
		String user = null;
		String password = null;
		String battery = null;
		String httpport = null;
		String rtmpport = null;
		String skipsetup = null;		
		String s[] = str.split(" ");
		for (int n=0; n<s.length; n=n+2) { // user password battery comport httpport rtmpport skipsetup
			if (s[n].equals("user")) { user = s[n+1]; }
			if (s[n].equals("password")) { password = s[n+1]; }
			if (s[n].equals("battery")) { battery = s[n+1]; }
			//if (s[n].equals("comport")) { com_port = s[n+1]; }
			if (s[n].equals("httpport")) { httpport = s[n+1]; }
			if (s[n].equals("rtmpport")) { rtmpport = s[n+1]; }
			if (s[n].equals("skipsetup")) { skipsetup = s[n+1]; }
		}
		//user & password
		if (user != null) {
			if (!user.matches("\\w+")) {
				message += "Error: username must be letters/numbers only ";
				oktoadd = false;
			}
			if (!password.matches("\\w+")) {
				message += "Error: password must be letters/numbers only ";
				oktoadd = false;
			}
			int i=0;
			String name;
			while(true) {
				name = settings.readSetting("user"+i);
				if (name==null) { break; }
				if ((name.toUpperCase()).equals((user).toUpperCase())) {
					message += "Error: user name already exists ";
					oktoadd = false;
				}
				i++;
			}
			if (oktoadd) {
				String p= user+salt+password;
				String encryptedPassword = passwordEncryptor.encryptPassword(p);
				if (settings.readSetting("user0") == null) {
					settings.newSetting("user0", user);
					settings.newSetting("pass0", encryptedPassword);
				}
				else {
					settings.writeSettings("user0", user);
					settings.writeSettings("pass0", encryptedPassword);					
				}
			}
		} else {
			if (settings.readSetting("user0") == null) {
				oktoadd=false;
				message += "Error: admin user not defined ";
			}
		}
		// battery
		if (battery != null) { 
			if (battery.equals("yes")) {
				batterypresent = true;
			} else { 
				batterypresent = false;
				motionenabled = true;
			}	
			settings.writeSettings("batterypresent", battery); 
		}
		// httpport
		if (httpport != null) { settings.writeRed5Setting("http.port", httpport); }
		// rtmpport
		if (rtmpport != null) { settings.writeRed5Setting("rtmp.port", rtmpport); }
		// skipsetup
		if (skipsetup != null) { settings.writeSettings("skipsetup", skipsetup); }
		
		if (oktoadd) {
			message = "launch server";
		}		
		messageGrabber(message,null);
	}
	
	private void populateSettings() {
		settings.writeSettings("skipsetup", "no");
		String result = "populatevalues ";
		// username
		String str = settings.readSetting("user0");
		if (str != null) { result += "username " + str +" "; }
		// comport
		if (portstr == null) { portstr = "nil"; }
		result += "comport " + portstr + " ";
		// battery
		result += "battery " + settings.readSetting("batterypresent") + " ";
		// http port
		result += "httpport " + settings.readRed5Setting("http.port") + " ";
		// rtmp port
		result += "rtmpport " + settings.readRed5Setting("rtmp.port") + " ";
		messageGrabber(result,null);
	}
	
	private void faceGrab(String str) {
		messageplayer("facegrab "+str+" received", null, null);
		if (str.equals("off")) { facegrabon = false; }
		else {
			facegrabon = true;
			new Thread(new Runnable() {
				public void run() {
					try {
						while(facegrabon) {
							if (grabber instanceof IServiceCapableConnection) {
								IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
								sc.invoke("facegrab", new Object[] { });
							}
							Thread.sleep(1000);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}
	
	private void softwareUpdate(String str) {
		if (admin) {
			if (str.equals("check")) {
				messageplayer("checking for new software...", null, null);
				Updater updater = new Updater();
				int currver = updater.getCurrentVersion();
				String fileurl  = updater.checkForUpdateFile();
				int newver = updater.versionNum(fileurl); 
				if (newver > currver) {
					String message = "New version available: v."+newver+"\n";
					if (currver == -1) { 
						message +=  "Current software version unknown\n";
					}
					else {
						message +=  "Current software is v."+currver+"\n";
					}
					message += "Do you want to download and install?";
					messageplayer("new version available", "softwareupdate", message);
				}
				else {
					messageplayer("no new version available", null, null);
				}
			}
			if (str.equals("download")) {
				messageplayer("downloading software update", null, null);
				new Thread(new Runnable() {  public void run() {
					String fileurl  = new Updater().checkForUpdateFile();
					System.out.println(fileurl);
					Downloader dl =new Downloader(); 
					if (dl.FileDownload(fileurl,"update.zip", "webapps")) {
						messageplayer("update download complete, unzipping...", null, null);
						if (!dl.unzipFolder("webapps\\update.zip", "webapps")) {
							dl.deleteDir(new File("webapps\\update"));
							dl.deleteFile("webapps\\update.zip");
							messageplayer("unable to unzip package, corrupted? Try again.", null, null);
						}
						else {
							dl.deleteFile("webapps\\update.zip");
							messageplayer("done.", "softwareupdate", "downloadcomplete");
						}
					}
					else {
						messageplayer("update download failed",null,null);
					}
				} }).start();
			}
			if (str.equals("versiononly")) {
				int currver = new Updater().getCurrentVersion();
				String msg = "";
				if (currver == -1) { msg="version unknown"; }
				else { msg="version: v."+currver; }
				messageplayer(msg, null, null);
			}
		}
	}
	
	private void autoDock(String str) {
		/* notes 
		 * 
		 * 	boolean autodocking = false;
		 *	String docktarget; // calibration values
		 *     s[] = 0 lastBlobRatio,1 lastTopRatio,2 lastMidRatio,3 lastBottomRatio,4 x,5 y,6 width,7 height,8 slope
		 *     UP CLOSE 85x70  1.2143_0.23563_0.16605_0.22992_124_126_85_70_0.00000
		 *     FAR AWAY 18x16   1.125_0.22917_0.19792_0.28819_144_124_18_16_0.00000

		 *  
		 * 
		 * 1st go click: dockgrab_findfromxy
		 *  MODE1 if autodocking = true:
		 * 		if size <= S1, 
		 * 	 		if not centered: clicksteer to center, dockgrab_find [BREAK]
		 *     		else go forward CONST time, dockgrab_find [BREAK]
 		 * 		if size > S1 && size <=S2	 
		 * 			determine N based on slope and blobsize magnitude
		 *  		if not centered +- N: clicksteer to center +/- N, dockgrab_find [BREAK]
		 *   		go forward N time
		 * 		if size > S2 
		 *   		if slope and XY not within target:
		 *   			backup, dockgrab_find
		 *   		else :
		 *      		dock
		 *  END MODE1 
		 * 
		 * events: 
		 *   dockgrabbed_find => enter MODE1
		 *   dockgrabbed_findfromxy => enter MODE1
		 * 
		 */
		String cmd[] = str.split(" ");
		
		if (cmd[0].equals("cancel")) {
			autodocking = false;
			messageplayer("auto-dock ended","multiple","cameratilt "+camTiltPos()+" autodockcancelled blank motion stopped");
			log.info("autodock cancelled");
			//IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
			//sc.invoke("dockgrab", new Object[] {0,0,"cancel"});
		}
		if (cmd[0].equals("go")) {
			if (motionenabled == true) {
				int x = Integer.parseInt(cmd[1])/2; //assuming 320x240
				int y = Integer.parseInt(cmd[2])/2; //assuming 320x240
				IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
				sc.invoke("dockgrab", new Object[] {x,y,"findfromxy"});
				autodocking = true;
				autodockingcamctr = false;
				autodockgrabattempts = 0;
				autodockctrattempts = 0;
				messageplayer("auto-dock in progress","motion", "moving");
				log.info("autodock started");
			}
			else {  messageplayer("motion disabled","autodockcancelled", null); }
		}
		if (cmd[0].equals("dockgrabbed")) { // RESULTS FROM GRABBER: calibrate, findfromxy, find
			if ((cmd[1].equals("find") || cmd[1].equals("findfromxy")) && autodocking) { // x,y,width,height,slope
				String s = cmd[2]+" "+cmd[3]+" "+cmd[4]+" "+cmd[5]+" "+cmd[6];
				if (cmd[4].equals("0")) { // width==0, failed to find target
					if (autodockgrabattempts < 0) { // TODO: remove this condition if unused
						autodockgrabattempts ++;
						IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
						sc.invoke("dockgrab", new Object[] {0,0,"find"}); // sends xy, but they're unused
					}
					else { 
						//failed, give up
						autodocking = false;
						messageplayer("auto-dock target lost, try again","multiple","cameratilt "+camTiltPos()+" autodockcancelled blank");
						log.info("target lost");
					}
				}
				else {
					messageplayer(null,"autodocklock",s);
					autoDockNav(Integer.parseInt(cmd[2]),Integer.parseInt(cmd[3]),Integer.parseInt(cmd[4]),
						Integer.parseInt(cmd[5]),new Float(cmd[6]));
					autodockgrabattempts ++;
				}
			}
			if (cmd[1].equals("calibrate")) { // x,y,width,height,slope,lastBlobRatio,lastTopRatio,lastMidRatio,lastBottomRatio
				docktarget = cmd[7]+"_"+cmd[8]+"_"+cmd[9]+"_"+cmd[10]+"_"+cmd[2]+"_"+cmd[3]+"_"+cmd[4]+"_"+cmd[5]+"_"+cmd[6];
				settings.writeSettings("docktarget",docktarget); 
				String s = cmd[2]+" "+cmd[3]+" "+cmd[4]+" "+cmd[5]+" "+cmd[6];
				//messageplayer("dock"+cmd[1]+": "+s,"autodocklock",s);
				messageplayer("auto-dock calibrated","autodocklock",s);
			}
		}
		if (cmd[0].equals("calibrate")) {
			int x = Integer.parseInt(cmd[1])/2; //assuming 320x240
			int y = Integer.parseInt(cmd[2])/2; //assuming 320x240
			if (grabber instanceof IServiceCapableConnection) {
				IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
				sc.invoke("dockgrab", new Object[] {x,y,"calibrate"});
			}
		}
		if (cmd[0].equals("getdocktarget")) {
			docktarget = settings.readSetting("docktarget");
			messageGrabber("docksettings",docktarget);
		}

	}
	
	private void autoDockNav(int x, int y, int w, int h, float slope) {
		x =x+(w/2); //convert to center from upper left
		y=y+(h/2);  //convert to center from upper left
		String s[] = docktarget.split("_");
		// s[] = 0 lastBlobRatio,1 lastTopRatio,2 lastMidRatio,3 lastBottomRatio,4 x,5 y,6 width,7 height,8 slope
		// 0.71053_0.27940_0.16028_0.31579_123_93_81_114_0.014493
		// neg slope = approaching from left
		int rescomp = 2;
		int dockw = Integer.parseInt(s[6]);
		int dockh = Integer.parseInt(s[7]);
		int dockx = Integer.parseInt(s[4]) + dockw/2;
		float dockslope = new Float(s[8]);
		float slopedeg = (float) ((180 / Math.PI) * Math.atan(slope));
		float dockslopedeg = (float) ((180 / Math.PI) * Math.atan(dockslope));
		int s1 = dockw*dockh * 20/100 *  w/h; // was 15/100 w/ taller marker
		int s2 = (int) (dockw*dockh * 65.5/100 * w/h);   // was 92/100 w/ taller marker
		// System.out.println(dockslopedeg+" "+slopedeg);
		if (w*h < s1) { 
			if (Math.abs(x-160) > 10 || Math.abs(y-120) > 50) { // clicksteer and go
				comport.clickSteer((x-160)*rescomp+" "+(y-120)*rescomp);
				new Thread(new Runnable() { public void run() { try {
					Thread.sleep(1500); // was 1500 w/ dockgrab following
					comport.speedset("fast");
					comport.goForward();
					Thread.sleep(1500);
					comport.stopGoing();
					Thread.sleep(500); // let deaccelerate
					IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
					sc.invoke("dockgrab", new Object[] {0,0,"find"}); // sends xy, but they're unused
				} catch (Exception e) { e.printStackTrace(); } } }).start();
			}
			else { // go only 
				new Thread(new Runnable() { public void run() { try {
					comport.speedset("fast");
					comport.goForward();
					Thread.sleep(1500);
					comport.stopGoing();
					Thread.sleep(500); // let deaccelerate
					IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
					sc.invoke("dockgrab", new Object[] {0,0,"find"}); // sends xy, but they're unused
				} catch (Exception e) { e.printStackTrace(); } } }).start();
			}
		} // end of S1 check
		if (w*h >= s1 && w*h < s2) {
			if (autodockingcamctr) { // if cam centered do check and comps below
				autodockingcamctr = false;
				int autodockcompdir = 0;
				if (Math.abs(slopedeg-dockslopedeg) > 1.7) {
					autodockcompdir = (int) (160 -(w*1.0) -20 -Math.abs(160 -x));  // was 160 - w - 25 - Math.abs(160-x)
				}
				if (slope > dockslope) { autodockcompdir *= -1; } // approaching from left
				autodockcompdir += x + (dockx - 160);
				//System.out.println("comp: "+autodockcompdir);
				if (Math.abs(autodockcompdir-dockx) > 10 || Math.abs(y-120) > 30) { // steer and go
					comport.clickSteer((autodockcompdir-dockx)*rescomp+" "+(y-120)*rescomp); 
					new Thread(new Runnable() { public void run() { try {
						Thread.sleep(1500); 
						comport.speedset("fast");
						comport.goForward();
						Thread.sleep(450);
						comport.stopGoing();
						Thread.sleep(500); // let deaccelerate
						IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
						sc.invoke("dockgrab", new Object[] {0,0,"find"}); // sends xy, but they're unused
					} catch (Exception e) { e.printStackTrace(); } } }).start();
				}
				else { // go only 
					new Thread(new Runnable() { public void run() { try {
						comport.speedset("fast");
						comport.goForward();
						Thread.sleep(500);
						comport.stopGoing();
						Thread.sleep(500); // let deaccelerate
						IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
						sc.invoke("dockgrab", new Object[] {0,0,"find"}); // sends xy, but they're unused
					} catch (Exception e) { e.printStackTrace(); } } }).start();
				}
			}
			else { // !autodockingcamctr
				autodockingcamctr = true;
				if (Math.abs(x-dockx) > 10 || Math.abs(y-120) > 30) {
					comport.clickSteer((x-dockx)*rescomp+" "+(y-120)*rescomp);
					new Thread(new Runnable() { public void run() { try {
						Thread.sleep(1500);
						IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
						sc.invoke("dockgrab", new Object[] {0,0,"find"}); // sends xy, but they're unused
					} catch (Exception e) { e.printStackTrace(); } } }).start();
				}
				else {
					IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
					sc.invoke("dockgrab", new Object[] {0,0,"find"}); // sends xy, but they're unused
				}
			}
		}
		if (w*h >= s2) {
			if ((Math.abs(x-dockx) > 5) && autodockctrattempts <= 10) {
				autodockctrattempts ++;
				comport.clickSteer((x-dockx)*rescomp+" "+(y-120)*rescomp);
				new Thread(new Runnable() { public void run() { try {
					Thread.sleep(1500);
					IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
					sc.invoke("dockgrab", new Object[] {0,0,"find"}); // sends xy, but they're unused
				} catch (Exception e) { e.printStackTrace(); } } }).start();
			}
			else {
				if (Math.abs(slopedeg-dockslopedeg) > 1.6 || autodockctrattempts >10) { // backup and try again
					System.out.println("backup "+dockslopedeg+" "+slopedeg+" ctrattempts:"+autodockctrattempts);
					autodockctrattempts = 0; 
					int comp = 80;
					if (slope < dockslope) { comp = -80; }
					x += comp;
					comport.clickSteer((x-dockx)*rescomp+" "+(y-120)*rescomp);
					new Thread(new Runnable() { public void run() { try {
						Thread.sleep(1500);
						comport.speedset("fast");
						comport.goBackward();
						Thread.sleep(1500); 
						comport.stopGoing();
						Thread.sleep(500); // let deaccelerate
						IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
						sc.invoke("dockgrab", new Object[] {0,0,"find"}); // sends xy, but they're unused
					} catch (Exception e) { e.printStackTrace(); } } }).start();
					log.info("autodock backup");
				}
				else { 
					System.out.println("dock "+dockslopedeg+" "+slopedeg);
					new Thread(new Runnable() { public void run() { try {
						Thread.sleep(100);
						dock("dock"); 
					} catch (Exception e) { e.printStackTrace(); } } }).start();
				}
			}
		}
	}
}
