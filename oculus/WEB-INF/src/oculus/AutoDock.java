package oculus;

import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.IConnection;
import org.red5.server.api.service.IServiceCapableConnection;
import org.slf4j.Logger;

public class AutoDock {

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

	private static Logger log = Red5LoggerFactory.getLogger(Application.class, "oculus");
	private State state = State.getReference();
	private BatteryLife life = BatteryLife.getReference();
	private Settings settings = new Settings();
	private IConnection grabber = null;
	private String docktarget = null;
	private ArduinoCommDC comport = null; 
	private Application app = null;
	private boolean docking = false;
	private boolean autodockingcamctr = false;
	private int autodockgrabattempts;
	private int autodockctrattempts;
	
	public AutoDock(Application theapp, IConnection thegrab, ArduinoCommDC com){
		this.app = theapp;
		this.grabber = thegrab;
		this.comport = com;
	}
	
	// TODO: Move to state! 
	public boolean isDocking(){
		return docking;
	}
	
	public void cancel(){
		docking = false;
	}

	public void autoDock(String str) {
		
		log.debug("autodock: " + str);
		System.out.println("autodock: " + str);
		
		String cmd[] = str.split(" ");
		
		if (cmd[0].equals("cancel")) {
			
			// app.autodocking = false;
			state.set(State.autodocking, "false");
			
			app.message("auto-dock ended","multiple","cameratilt " +app.camTiltPos()+" autodockcancelled blank motion stopped");
			log.info("autodock cancelled");
			IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
			sc.invoke("dockgrab", new Object[] {0,0,"cancel"});
		}
		if (cmd[0].equals("go")) {
			if (app.motionenabled == true) {
				//int x = Integer.parseInt(cmd[1])/2; //assuming 320x240
				//int y = Integer.parseInt(cmd[2])/2; //assuming 320x240
				IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
				//sc.invoke("dockgrab", new Object[] {x,y,"findfromxy"});
				sc.invoke("dockgrab", new Object[] {0,0,"start"}); // sends xy, but they're unused
				
				state.set(State.autodocking, "true");
				autodockingcamctr = false;
				autodockgrabattempts = 0;
				autodockctrattempts = 0;
				app.message("auto-dock in progress","motion", "moving");
				log.info("autodock started");
			}
			else { app.message("motion disabled","autodockcancelled", null); }
		}
		if (cmd[0].equals("dockgrabbed")) { // RESULTS FROM GRABBER: calibrate, findfromxy, find
			if ((cmd[1].equals("find") || cmd[1].equals("findfromxy")) && state.getBoolean(State.autodocking)) { // x,y,width,height,slope
				String s = cmd[2]+" "+cmd[3]+" "+cmd[4]+" "+cmd[5]+" "+cmd[6];
				if (cmd[4].equals("0")) { // width==0, failed to find target
					if (autodockgrabattempts < 0) { // TODO: remove this condition if unused
						autodockgrabattempts ++;
						IServiceCapableConnection sc = (IServiceCapableConnection) grabber;
						sc.invoke("dockgrab", new Object[] {0,0,"find"}); // sends xy, but they're unused
					}
					else { 
						//failed, give up
						state.set(State.autodocking, "false");
						
						app.message("auto-dock target not found, try again","multiple", /*"cameratilt "+app.camTiltPos()+ */" autodockcancelled blank");
						log.info("target lost");
					}
				}
				else {
					app.message(null,"autodocklock",s);
					autoDockNav(Integer.parseInt(cmd[2]),Integer.parseInt(cmd[3]),Integer.parseInt(cmd[4]),
						Integer.parseInt(cmd[5]),new Float(cmd[6]));
					autodockgrabattempts ++;
				}
			}
			if (cmd[1].equals("calibrate")) { // x,y,width,height,slope,lastBlobRatio,lastTopRatio,lastMidRatio,lastBottomRatio
				docktarget = cmd[7]+"_"+cmd[8]+"_"+cmd[9]+"_"+cmd[10]+"_"+cmd[2]+"_"+cmd[3]+"_"+cmd[4]+"_"+cmd[5]+"_"+cmd[6];
				settings.writeSettings("docktarget", docktarget); 
				String s = cmd[2]+" "+cmd[3]+" "+cmd[4]+" "+cmd[5]+" "+cmd[6];
				//messageplayer("dock"+cmd[1]+": "+s,"autodocklock",s);
				app.message("auto-dock calibrated","autodocklock",s);
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
			app.messageGrabber("docksettings", docktarget);
			
			log.info("got dock target: " + docktarget);
		}
	}
	
	

	void dock(String str) {
		if (str.equals("dock") && !docking) {
			if (app.motionenabled == true) {
				if (!life.batteryCharging()) {
					
					app.message("docking initiated", "multiple", "speed slow motion moving dock docking");

					// need to set this because speedset calls goForward also if true
					comport.movingforward = false; 
					
					comport.speedset("slow");
					//comport.goForward();
					docking = true;
					app.dockstatus = "docking";
					//Date d = new Date();
					//dockstarttime = d.getTime();
					new Thread(new Runnable() {
						public void run() {
							int counter = 0;
							int n;
							while (docking == true) {
								n = 500;
								if (counter <= 3) { n += 500; }
								if (counter > 0) { app.message(null,"motion","moving"); }
								comport.goForward();
								try {
									Thread.sleep(n);
								} catch (Exception e) {
									e.printStackTrace();
								}
								comport.stopGoing();
								app.message(null,"motion","stopped");
								if (life.batteryStatus() == 2) {
									docking = false;
									String str = "";
									if (state.getBoolean(State.autodocking)) {
										state.set(State.autodocking, "false");
										str += " cameratilt "+app.camTiltPos()+" autodockcancelled blank";
										if (!app.stream.equals("stop") && app.userconnected==null) { 
											app.publish("stop"); 
										}
									}
									app.message("docked successfully", "multiple", "motion disabled dock docked battery charging"+str);
									log.info(app.userconnected +" docked successfully");
									app.motionenabled = false;
									app.dockstatus = "docked"; // needs to be before battStats()
									life.battStats(); 
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
									app.message("docking timed out", "multiple", s);
									log.info(app.userconnected +" docking timed out");
									app.dockstatus = "un-docked";
									if (state.getBoolean(State.autodocking)) {
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
				else { app.message("**battery indicating charging, auto-dock unavailable**", null, null); }
			}
			else { app.message("motion disabled", null, null); }
		}
		if (str.equals("undock")) {
			comport.speedset("slow");
			comport.goBackward();
			app.motionenabled = true;
			app.message("un-docking", "multiple", "speed fast motion moving dock un-docked");
			app.dockstatus = "un-docked";
			new Thread(new Runnable() {
				public void run() {
					try {
						Thread.sleep(2000);
						comport.speedset("fast");
						comport.goBackward();
						Thread.sleep(750);
						comport.stopGoing();
						app.message("disengaged from dock", "motion", "stopped");
						log.info(app.userconnected + " un-docked");
						life.battStats();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
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

