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
	
	private static final int STREAM_CONNECT_DELAY = 2000;
	
	private IConnection grabber = null;
	private IConnection player = null;

	private ArduinoCommDC comport = null; 
	private LightsComm light = null;
	
	// optionally log all moves if a developer 
	private LogManager moves = new LogManager();
	
	private BatteryLife battery = null;
	private Settings settings = new Settings();
	protected boolean initialstatuscalled = false;
	private static String salt = "PSDLkfkljsdfas345usdofi";
	ConfigurablePasswordEncryptor passwordEncryptor = new ConfigurablePasswordEncryptor();
	String userconnected = null; 
	String pendinguserconnected = null; 
	String remember = null;
	IConnection pendingplayer = null;
	boolean pendingplayerisnull = true;
	protected String stream = "stop";
	// WifiConnection wifi; // = new WifiConnection();
	boolean admin = false;
	private static Logger log = Red5LoggerFactory.getLogger(Application.class, "oculus");
	String dockstatus = "";
	String httpPort; 
	boolean facegrabon = false;
	private boolean emailgrab = false;
	private AutoDock docker = null;
	private State state = State.getReference();
	private Speech speech = new Speech();

	public Application() { 
		super();
		passwordEncryptor.setAlgorithm("SHA-1");
		passwordEncryptor.setPlainDigest(true);
		initialize();
	}
	
	@Override
	public boolean appConnect(IConnection connection, Object[] params) { 
		
		String logininfo[] = ((String) params[0]).split(" ");
		
		// always accept local grabber
		if ((connection.getRemoteAddress()).equals("127.0.0.1") && logininfo[0].equals("")) { 
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
	
	@Override
	public void appDisconnect(IConnection connection) {
		if (connection.equals(player)) {
			String str = userconnected+" disconnected";
			log.info(str); 
			messageGrabber(str,"connection awaiting&nbsp;connection");
			if (userconnected.equals(settings.readSetting("user0"))) {
				admin = false;
			}
			userconnected = null;
			state.set(State.userisconnected, false);
			
			player = null;
			facegrabon = false;
			if (!state.getBoolean(State.autodocking)) {
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
							grabberInitialize();							
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
		
		docker = new AutoDock(this, grabber, comport);
	}

	public void initialize() {
		
		// must be blocking search of all ports, but only once!  
		new Discovery().search();
		System.out.println("discovery done...");
		 
		comport = new ArduinoCommDC(this);
		light = new LightsComm(this);
		
		httpPort = settings.readRed5Setting("http.port");
		
		
		// checks setting for flag before starting 
		new SystemWatchdog();
        new EmailAlerts(this);
		
		//if(settings.getBoolean(State.developer))			
			//CommandManager.getReference().init(this);
		
		String str = settings.readSetting(Settings.volume);
		if (str != null) {
			Util.setSystemVolume(Integer.parseInt(str));
			
			// if not found, add it 
			// TODO: this wont' work if 'volume' isn't already there,
			// should be similar check for ALL settings, + read value from default file.  
			// Also required for automatic software update when changes to settings keys
		} else settings.writeSettings(Settings.volume, "0");
		
		grabberInitialize();
		
		battery = BatteryLife.getReference();
		
		if(settings.getBoolean(Settings.developer))
			moves.open(System.getenv("RED5_HOME")+"\\log\\moves.log");

		log.info("initialize");
	}
	
	/**
	 * battery init steps separated here since battery has to be called after delay 
	 * and delay can't be in main app, is in server.js instead
	 * 
	 * @param mode is this required, not really used? 
	 */
	private void checkForBattery(String mode) {
		if (mode.equals("init")) {
			battery.init(this);
		} else {
			new Thread(new Runnable() { 
				public void run() {
					if (battery.batteryPresent())
						messageGrabber("populatevalues battery yes", null);
					else 
						messageGrabber("populatevalues battery nil",null);
				}
			}).start();
		}
	}
	
	private void grabberInitialize() {
		if (settings.getBoolean(Settings.skipsetup)) {
			grabber_launch();
		} else { 
			initialize_launch();
		}
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
		} else {
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
		
			if (userconnected.equals(settings.readSetting("user0"))) {
				admin = true;
			}
			else { admin = false; }
			// System.out.println(userconnected+" connected");
			// log.info(userconnected+" connected");
			str = userconnected + " connected from: "+player.getRemoteAddress();
			log.info(str); 
			messageGrabber(str,"connection "+userconnected+"&nbsp;connected");
			Util.beep();
		}
		
		state.set(State.userisconnected, "true");
		state.set(State.logintime, System.currentTimeMillis());
		state.set(State.user, userconnected);
	}

	/** put all commands here */
	public enum playerCommands { publish, move, battStats, docklineposupdate, autodock, autodockcalibrate,
		speech, getdrivingsettings, drivingsettingsupdate, gettiltsettings, cameracommand, tiltsettingsupdate,
		tilttest, speedset, slide, nudge, dock, relaunchgrabber, clicksteer, chat, statuscheck, systemcall, 
		streamsettingsset, streamsettingscustom, motionenabletoggle, playerexit, playerbroadcast, password_update,
		new_user_add, pasword_update, user_list, delete_user, extrauser_password_update, username_update,
		disconnectotherconnections, showlog, monitor, framegrab, emailgrab, facegrab, assumecontrol, 
		softwareupdate, restart, arduinoecho, arduinoreset, setsystemvolume, beapassenger;
	
		@Override 
		public String toString() {
			return super.toString();
		}	
	}
	
	/**
	 * distribute commands from player
	 * 
	 * @param fn
	 * 				is the function to call
	 * 
	 * @param str
	 * 				is the parameter to pass onto the function
	 */
	public void playerCallServer(String fn, String str) { 
		
		playerCommands cmd = playerCommands.valueOf(fn);
		if (Red5.getConnectionLocal() == player) {

			switch (cmd) {
			
			case publish:
				publish(str);
				messageplayer("command received: publish " + str, null, null);
				break;
			
			case speedset: 
				comport.speedset(str);
				messageplayer("speed set: " + str, "speed", str.toUpperCase());
				break; 
			
			case slide:
				if (!state.getBoolean(State.motionenabled)){
					messageplayer("motion disabled", "motion", "disabled");
					break;
				}
				moveMacroCancel();
				comport.slide(str);
				messageplayer("command received: " + fn + str, null, null);
				break;
		
			case systemcall:	
				// System.out.println("received: " + str);
				messageplayer("system command received", null, null); 
				Util.systemCall(str, admin); 
				break;
				
			case relaunchgrabber: 
				if (admin) {
					grabber_launch();
					messageplayer("relaunching grabber", null, null);
				}
				break;
				
			case docklineposupdate: 
				if(admin){
					settings.writeSettings("vidctroffset", str);
					messageplayer("vidctroffset set to : " + str, null, null);
				}
				break;
				
			case framegrab:
				if (grabber instanceof IServiceCapableConnection) {
					IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
					sc.invoke("framegrab", new Object[] { });
					messageplayer("framegrab command received", null, null);
				}
				break;
				
			case emailgrab:	
				if (grabber instanceof IServiceCapableConnection) {
					IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
					sc.invoke("framegrab", new Object[] { });
					messageplayer("framegrab command received", null, null);
				}
				emailgrab = true;
				break;
				
			case arduinoecho: 
				if(str.equals("on")) comport.setEcho(true); 
				else comport.setEcho(false);
				messageplayer("echo set to: "+str, null, null);
				break;
			
			case arduinoreset: 
				comport.reset();
				messageplayer("resetting arduino", null, null);
				break;
				
			case move: move(str); break;
			case nudge: nudge(str); break;
			case speech: saySpeech(str); break;
			case dock: moves.append("docking"); docker.dock(str); break;
			case battStats: battery.battStats(); break;
			case cameracommand: cameraCommand(str); break;
			case gettiltsettings: getTiltSettings(); break;
			case getdrivingsettings: getDrivingSettings(); break;	
			case motionenabletoggle: motionEnableToggle(); break;
			case drivingsettingsupdate: drivingSettingsUpdate(str); break;
			case tiltsettingsupdate: tiltSettingsUpdate(str); break;
			case tilttest: tiltTest(str); break;
			case clicksteer: clickSteer(str); break;
			case streamsettingscustom: streamSettingsCustom(str); break;
			case streamsettingsset: streamSettingsSet(str); break;
			case statuscheck: statusCheck(str); break;
			case playerexit: appDisconnect(player); break;
			case playerbroadcast: playerBroadCast(str); break;
			case password_update: account("password_update", str); break;
			case new_user_add: account("new_user_add",str); break;
			case pasword_update: account("pasword_update",str); break;
			case user_list: account("user_list",""); break;
			case delete_user: account("delete_user",str); break; 
			case extrauser_password_update: account("extrauser_password_update", str); break;
			case username_update: account("username_update",str); break;
			case disconnectotherconnections: disconnectOtherConnections(); break;
			case monitor: monitor(str); break; 
			case showlog: showlog(); break; 
			case facegrab: faceGrab(str); break;
			case autodock: moves.append("autodock " + str); docker.autoDock(str); break;
			case autodockcalibrate: docker.autoDock("calibrate "+str); break;
			case restart: 
				restart(); 
				System.out.println("restart command received from player");
				break;
			case softwareupdate: softwareUpdate(str); break;
			case setsystemvolume: Util.setSystemVolume(Integer.parseInt(str), this); break;
			
			default:
			//	System.out.println("command not found: " + cmd.toString());
			//	for( playerCommands command : playerCommands.values() )
			//		System.out.println(command.ordinal() + " = " + command.toString());
				break;
			}
		
			// if (fn.equals("autodockgo")) { docker.autoDock("go "+str); } // TODO: unused if autoDock("go") used w/o xy, delete
			// if (fn.equals("autodock")) { docker.autoDock(str); }
			// if (fn.equals("autodockcalibrate")) { docker.autoDock("calibrate "+str); } // eliminate, combine into 'autodock
			
		}	
		
		switch (cmd) {
			case chat: chat(str); break;
			case beapassenger: beAPassenger(str); break;
			case assumecontrol: assumeControl(str); break;
		}

 	}
	
	/** put all commands here */
	public enum gabberCommands { streammode, saveandlaunch, populatesettings, 
		systemcall, chat, facerect, dockgrabbed, autodock, restart, checkforbattery;
	
		@Override 
		public String toString() {
			return super.toString();
		}	
	}
	
	/**
	 * distribute commands from grabber
	 * 
	 * @param fn is the funct ion to call 
	 * @param str is the parameters to pass on to the function.
	 */
	public void grabberCallServer(/*Application.GabberCommands cmd*/ String fn, String str){
		
		// turn string input to id 
		gabberCommands cmd = gabberCommands.valueOf(fn);
		
		switch (cmd) {	
		case streammode: grabberSetStream(str);
			break;

		case saveandlaunch: saveAndLaunch(str);
			break;
		
		case populatesettings: populateSettings();
			break;
			
		case systemcall: Util.systemCall(str, true);
			break;
			
		case chat: chat(str);
			break;
			
		case facerect: messageplayer(null, "facefound", str);
			break; 
	
		case restart: 
			admin=true;
			System.out.println("restart command received from grabber");
			restart();
			break;
			
		case dockgrabbed: docker.autoDock("dockgrabbed "+str); break;
			
		case autodock: docker.autoDock(str); break;
			
		case checkforbattery: checkForBattery(str); break;
			
		
		default: 
		//	System.out.println("command not found: " + cmd.toString());
		//	for( gabberCommands command : gabberCommands.values() )
		//		System.out.println(command.ordinal() + " = " + command.toString());
			break;
		}
	}
	
	private void grabberSetStream(String str) {
		stream = str;
		//messageplayer("streaming "+str,"stream",stream);
		messageGrabber("streaming "+stream,"stream "+stream);
		log.info("streaming "+stream);
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(STREAM_CONNECT_DELAY);
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

	public void publish(String str) {
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
		//String str= "frame grabbed <a href='images/framegrab.png' target='_blank'>view</a>";
		//String str= "frame grabbed <a href='javascript: framegrabbed();'>view</a>";
		messageplayer(null, "framegrabbed", null);
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
				String file = System.getenv("RED5_HOME")+"\\webapps\\oculus\\images\\framegrab.png";	
				if(JavaImage != null) { 
					// If you sent a jpeg to the server, just change PNG to JPEG and Red5ScreenShot.png to .jpeg
					ImageIO.write(JavaImage, "PNG", new File(file));
					if (emailgrab) {
						emailgrab = false;
						new SendMail("Oculus Screen Shot", "image attached", file, this);
					}
				}
			} catch(IOException e) {log.info("Save_ScreenShot: Writing of screenshot failed " + e); 
			System.out.println("IO Error " + e);}
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
		speech.mluv(str);
		//Util.systemCall("nircmdc.exe speak text \""+str+"\"", true);
		log.info("voice synth: '"+str+"'");				
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
	
	// TODO... THIS IS NEEDED... BUT WHY?
	public void message(String str, String status, String value){
		messageplayer(str, status, value);
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

	private void moveMacroCancel() {
		if (state.getBoolean(State.docking) == true) {
			String str = "";
			if (!dockstatus.equals("docked")) {
				dockstatus = "un-docked";
				str += "dock un-docked";
			}
			messageplayer("docking cancelled by movement", "multiple", str);
			docker.cancel();
		}
		if (comport.sliding == true) {
			comport.slidecancel();
		}
		if (state.getBoolean(State.autodocking)) {
			docker.autoDock("cancel");
		}
	}
	
	private void cameraCommand(String str) {
		comport.camCommand(str);
		messageplayer("tilt command received: " + str, null, null);
		if (!str.equals("up") && !str.equals("down") && !str.equals("horiz")) {
			messageplayer(null,"cameratilt",camTiltPos());
		}
		if (str.equals("horiz")) { 	messageplayer(null,"cameratilt","0&deg;"); }
	}
	
	protected String camTiltPos() {
		int n = comport.camservohoriz - comport.camservopos;
		n *= -1;
		String s = "";
		if (n>0) { s="+"; }
		return s+Integer.toString(n)+"&deg;";
	}
	
	private void statusCheck(String s) {
		if (initialstatuscalled==false) {
			initialstatuscalled=true; 
			battery.battStats();
			// signalStrength();
			String str = "";
			if (comport != null) {
				String spd = "FAST";
				if (comport.speed == comport.speedmed) { spd = "MED"; }
				if (comport.speed== comport.speedslow) { spd = "SLOW"; }
				String mov = "STOPPED";
				if (!state.getBoolean(State.motionenabled))/*(!motionenabled)*/ { mov = "DISABLED"; }
				if (comport.moving == true) { mov = "MOVING"; }
				str += " speed "+spd+" cameratilt "+camTiltPos()+" motion "+mov;
			}
			str += " vidctroffset "+Integer.parseInt(settings.readSetting("vidctroffset"));
			str += " rovvolume "+Integer.parseInt(settings.readSetting("volume"));
			str += " stream "+stream+" selfstream stop";
			//str += " address "+settings.readSetting("address");
			//str += " wifi "+wifi.wifiSignalStrength();
			if (admin) {
				str += " admin true";
			}
			if (!dockstatus.equals("")) { 
				str += " dock "+dockstatus;
			}
			messageplayer("status check received","multiple",str.trim());
		}
		else {
//			String str = " stream "+stream;
//			messageplayer("status check received","multiple",str.trim());
			messageplayer("status check received",null,null);
		}
		if (s.equals("battcheck")) { 
			battery.battStats();
		}
	}
	
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
	
	
	public void restart() {
		if (admin) {
			messageplayer("restarting server application", null, null);
			messageGrabber("restarting server application", null);
			File f;
			f=new File(System.getenv("RED5_HOME")+"\\restart");
			try {
				if(!f.exists()){ f.createNewFile(); }
				Runtime.getRuntime().exec("red5-shutdown.bat");
			} catch (Exception e) {
				e.printStackTrace();
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
		if (state.getBoolean(State.motionenabled)){ 
			if (str.equals("forward")) { comport.goForward();}
			if (str.equals("backward")) { comport.goBackward();}
			if (str.equals("right")) { comport.turnRight();}
			if (str.equals("left")) { comport.turnLeft();}
			moveMacroCancel();
			if (s.equals("")) { s = "MOVING"; }
			msg = "command received: "+str;
		}
		else {
			s = "DISABLED";
		}
		messageplayer(msg, "motion", s);
		
		if(state.get(State.dockx) == null) moves.append(str);
		else moves.append(str + " " + state.get(State.dockx) + " " + state.get(State.docky));
	}
	
	private void nudge(String str) {
		if (state.getBoolean(State.motionenabled)){ 
			comport.nudge(str);
			
			messageplayer("command received: nudge" + str, null, null);
			
			if(state.get(State.dockx) == null) moves.append(str);
			else moves.append(str + " " + state.get(State.dockx) + " " + state.get(State.docky));
	
			if (state.getBoolean(State.docking)
					|| state.getBoolean(State.autodocking)) moveMacroCancel(); 
		}
		else {
			messageplayer("motion disabled", "motion", "disabled");
		}
	}
	
	private void motionEnableToggle() {
		if (state.getBoolean(State.motionenabled)) { 
			state.set(State.motionenabled, "false");
			messageplayer("motion disabled", "motion", "disabled");
		}
		else {
			state.set(State.motionenabled, "true");
			messageplayer("motion enabled", "motion", "enabled");
		}
	}
	
	private void clickSteer(String str) {
		if (state.getBoolean(State.motionenabled)){
			int n = comport.clickSteer(str);
			
			moves.append("clicksteer " + str);
			
			if (n != 999) {
				messageplayer("received: clicksteer " + str, "cameratilt", camTiltPos());
			} else {
				messageplayer("received: clicksteer " + str, null, null);
			}
			
			moveMacroCancel();
		}
		else { messageplayer("motion disabled", "motion", "disabled"); }
	}
	
	public void messageGrabber(String str, String status) {
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
		if (userconnected.equals(settings.readSetting("user0"))) admin = true;
		else admin = false; 
		
		state.set(State.userisconnected, "true");
		state.set(State.logintime, System.currentTimeMillis());
		state.set(State.user, userconnected);
		
		// Util.announce("hijacked by " + userconnected);
		Util.beep();
	}
	
	private void beAPassenger(String user) {
		pendingplayerisnull = true;
		String str = user+ " added as passenger";
		messageplayer(str, null, null);
		// Util.announce(str);
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
		Util.beep();
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
			if (!str.equals("off")) {
				sc.invoke("publish", new Object[] { str, 160, 120, 8, 85 });
				new Thread(new Runnable() {
					public void run() {
						try { Thread.sleep(STREAM_CONNECT_DELAY); }
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
		if (fn.equals("password_update")) {
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
						message += "ERROR: user name already exists ";
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
		String httpport = null;
		String rtmpport = null;
		String skipsetup = null;		
		String s[] = str.split(" ");
		for (int n=0; n<s.length; n=n+2) { 
			// user password comport httpport rtmpport skipsetup
			if (s[n].equals("user")) { user = s[n+1]; }
			if (s[n].equals("password")) { password = s[n+1]; }
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
		if (state.get(State.serialport) == null) result += "comport nil ";
		else result += "comport "+state.get(State.serialport)+" ";

		// lights
		if (state.get(State.lightport) == null) result += "lightport nil ";
		else result += "lightport "+state.get(State.lightport)+" ";
		
		// battery
		// result += "battery " + settings.readSetting("batterypresent") + " ";
		// result += "battery"
		
//		if (battery != null) {
//			if (battery.batteryPresent()) result += "battery yes ";
//			else result += "battery nil ";
//		}
		
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
				messageplayer("downloading software update...", null, null);
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
	
	/*
	private void setSystemVolume(int percent) {
		settings.writeSettings("volume", Integer.toString(percent));
		float vol = (float) percent / 100 * 65535;
		String str = "nircmdc.exe setsysvolume "+ (int) vol;
		Util.systemCall(str, true);
		messageplayer("ROV volume set to "+Integer.toString(percent)+"%", null, null);
	}*/
}