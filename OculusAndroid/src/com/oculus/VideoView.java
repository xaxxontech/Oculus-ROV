package com.oculus;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.WindowManager.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebSettings.PluginState;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class VideoView extends Activity implements SensorEventListener {
	private WebView mWebView;
	private String user;
	private String pass;
	private String domainText;
	private String httpPortText;
	private Bundle b;
    private WindowManager mWindowManager;
    private LinearLayout mDrivingControls;
    private Menu mMenu;
//    private long lastTime = 0;
    private int VARIABLE_TAP_DELAY = 100;
    private static final int TAP_DELAY = 120;
    private static final int HIDE_CONTROLS_DELAY = 4000;
    private static final int AFTER_PRESS_HIDE_CONTROLS_DELAY = 500;
    private static final int BUTTON_FLASH_DELAY = 140;
//    private boolean drivingControlsAreVisible = false;

    private HideDrivingControls mHideDrivingControls = new HideDrivingControls();
    private CheckTap checkTap = new CheckTap();
    private ButtonFlashDone buttonFlashDone  = new ButtonFlashDone(); 
    Handler mHandler = new Handler();
    public int lastButtonId = -1; // unset
    public int flashingButtonId = -1; // unset
    private Button mMenuButton;
    private Button mForwardButton;
    private Button mBackwardButton;
    private Button mStopButton;
    private Button mLeftButton;
    private Button mRightButton;
    private boolean moving = false;
    private boolean movingforward = false;
    private boolean turning = false;
    
    private Button mSpeedSlowButton;
    private Button mSpeedMedButton;
    private Button mSpeedFastButton;
    
    private Button mCamUpButton;
    private Button mCamHorizButton;
    private Button mCamDownButton;
    private boolean cameramoving = false;
    
    /*
     * sensor stuff below
     */
    private boolean tiltEnabled = false;
    private boolean tiltCalibrated = false;
    private int tiltCalibrationAvgCount;
    private String tiltSteerMode = "";
    float stopRoll;
    float stopPitch;
    static final int ROLL_THRESHHOLD = 10;
    static final int PITCH_THRESHHOLD = 10;


    /* sensor data */
    SensorManager m_sensorManager;
    float []m_lastMagFields;
    float []m_lastAccels;
    private float[] m_rotationMatrix = new float[16];
    private float[] m_remappedR = new float[16];
    private float[] m_orientation = new float[4];

    /* fix random noise by averaging tilt values */
    final static int AVERAGE_BUFFER = 30;
    float []m_prevPitch = new float[AVERAGE_BUFFER];
    float m_lastPitch = 0.f;
//    float m_lastYaw = 0.f;
    /* current index int m_prevEasts */
    int m_pitchIndex = 0;

    float []m_prevRoll = new float[AVERAGE_BUFFER];
    float m_lastRoll = 0.f;
    /* current index into m_prevTilts */
    int m_rollIndex = 0;

    /* center of the rotation */
//    private float m_tiltCentreX = 0.f;
//    private float m_tiltCentreY = 0.f;
//    private float m_tiltCentreZ = 0.f;    
    
    
    
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
	    // full screen
	    getWindow().requestFeature(Window.FEATURE_NO_TITLE);
	    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    
		setContentView(R.layout.video);

	    b = getIntent().getExtras();
	    if (b != null) {
		    domainText = b.getString("domainText");
		    httpPortText = b.getString("httpPortText");
		    user = b.getString("userNameText");
		    pass = b.getString("passwordText");
	    }
	    
	    mWebView = (WebView) findViewById(R.id.webview);
	    mWebView.getSettings().setJavaScriptEnabled(true);
	    mWebView.getSettings().setPluginState(PluginState.ON);
	    
	    mWebView.addJavascriptInterface(new  AndroidOculusJavaScriptInterface(), "OCULUSANDROID");
	    
	    mWebView.loadUrl("http://" + domainText + ":" + httpPortText + "/oculus/android.html");
	    mWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY); // delete scrollbar void on right
	    mWebView.setOnTouchListener(mWebViewOnTouchListener);
	    
        LayoutInflater inflate = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mDrivingControls = (LinearLayout) inflate.inflate(R.layout.drivingcontrols, null);
        mWindowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
                LayoutParams.TYPE_APPLICATION, LayoutParams.FLAG_FULLSCREEN, PixelFormat.TRANSLUCENT);
        mWindowManager.addView(mDrivingControls, lp);
        mDrivingControls.setOnTouchListener(mDrivingControlsListener);
        drivingControlsSetVisible(0);
        
        mMenuButton = (Button) mDrivingControls.findViewById(R.id.drvctrls_menubutton);
        mMenuButton.setOnTouchListener(mMenuButtonListener);
        

        mForwardButton = (Button) mDrivingControls.findViewById(R.id.drvctrls_forward);
        mForwardButton.setOnTouchListener(mMoveButtonListener);

        mBackwardButton = (Button) mDrivingControls.findViewById(R.id.drvctrls_backward);
        mBackwardButton.setOnTouchListener(mMoveButtonListener);

        mStopButton = (Button) mDrivingControls.findViewById(R.id.drvctrls_stop);
        mStopButton.setOnTouchListener(mMoveButtonListener);

        mRightButton = (Button) mDrivingControls.findViewById(R.id.drvctrls_right);
        mRightButton.setOnTouchListener(mMoveButtonListener);

        mLeftButton = (Button) mDrivingControls.findViewById(R.id.drvctrls_left);
        mLeftButton.setOnTouchListener(mMoveButtonListener);
        
        
        mSpeedSlowButton = (Button) mDrivingControls.findViewById(R.id.drvctrls_slow);
        mSpeedSlowButton.setOnTouchListener(mSpeedButtonListener);

        mSpeedMedButton = (Button) mDrivingControls.findViewById(R.id.drvctrls_med);
        mSpeedMedButton.setOnTouchListener(mSpeedButtonListener);
        
        mSpeedFastButton = (Button) mDrivingControls.findViewById(R.id.drvctrls_fast);
        mSpeedFastButton.setOnTouchListener(mSpeedButtonListener);
        
        
        mCamUpButton = (Button) mDrivingControls.findViewById(R.id.drvctrls_camup);
        mCamUpButton.setOnTouchListener(mCamButtonListener);

        mCamHorizButton = (Button) mDrivingControls.findViewById(R.id.drvctrls_camhoriz);
        mCamHorizButton.setOnTouchListener(mCamButtonListener);

        mCamDownButton = (Button) mDrivingControls.findViewById(R.id.drvctrls_camdown);
        mCamDownButton.setOnTouchListener(mCamButtonListener);
        
        m_sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        
    }
    
    private void drivingControlsSetVisible(int visLevel) {
    	int views[] = {R.id.drvctrlsgroup1, R.id.drvctrlsgroup2, R.id.drvctrlsgroup3,
    			R.id.drvctrlsgroup4, R.id.drvctrlsgroup5, R.id.drvctrlsgroup6};
    	for (int n : views) {
	    	ViewGroup drivingControlsGroup = (ViewGroup) mDrivingControls.findViewById(n);
	    	int numChildren =  drivingControlsGroup.getChildCount();
	    	for (int i=0; i<numChildren; i++) {
	    		Button drivingControlsChild = (Button) drivingControlsGroup.getChildAt(i);
	    		if (visLevel==2) {
	    			drivingControlsChild.setTextColor(0xFFFFFFFF);
	    			drivingControlsChild.setBackgroundColor(0x99222244);
	    		}
	    		if (visLevel==1) {
	    			drivingControlsChild.setTextColor(0x66FFFFFF);
	    			drivingControlsChild.setBackgroundColor(0x22222244);
	    		}
	    		if (visLevel==0) {
	    			drivingControlsChild.setTextColor(0x00000000);
	    			drivingControlsChild.setBackgroundColor(0x00000000);
	    		}
	    	}
    	}
    	

    }
    
    OnTouchListener mWebViewOnTouchListener = new OnTouchListener() {

		public boolean onTouch(View arg0, MotionEvent arg1) {
			if (arg1.getAction()==MotionEvent.ACTION_DOWN) {
				mDrivingControls.setVisibility(View.VISIBLE);
				drivingControlsDown(arg0); 
			}
			return false;
		}
    	
    };
    
    OnTouchListener mDrivingControlsListener = new OnTouchListener() {
		public boolean onTouch(View arg0, MotionEvent arg1) {
			if (arg1.getAction()==MotionEvent.ACTION_DOWN) {
				drivingControlsDown(arg0);
			}
			return false;
		}    	
    };
    
    private void drivingControlsDown(View arg0) {
        drivingControlsSetVisible(1);
		lastButtonId = arg0.getId();
        mHandler.removeCallbacks(mHideDrivingControls);
        mHandler.postDelayed(mHideDrivingControls, HIDE_CONTROLS_DELAY);
        mHandler.removeCallbacks(checkTap);
        mHandler.postDelayed(checkTap, VARIABLE_TAP_DELAY);
    }
    
    private void openMenu(){
        this.getWindow().openPanel(Window.FEATURE_OPTIONS_PANEL, 
        		new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MENU));
        mDrivingControls.setVisibility(View.INVISIBLE); //invisible
    }
    
    private void appClose() {
    	appMessage("connection failed or closed");
        mWebView.loadUrl("about:blank");  // flush flash plugin, disconnect from oculus server
        finish();
    }
    
	public void appMessage(String str) {
		Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
	}
    
    OnTouchListener mMenuButtonListener = new OnTouchListener() {
		public boolean onTouch(View arg0, MotionEvent arg1) {
			if (arg1.getAction()==MotionEvent.ACTION_DOWN) {
				drivingControlsDown(arg0);
				return true;
			}
			if (arg1.getAction()==MotionEvent.ACTION_UP) {
				lastButtonId = -1;
				return true;
			}
			return false;
		}
    };
    
    OnTouchListener mMoveButtonListener = new OnTouchListener() {
		public boolean onTouch(View arg0, MotionEvent arg1) {
			if (arg1.getAction()==MotionEvent.ACTION_DOWN) {
				drivingControlsDown(arg0);
				return true;
			}
			if (arg1.getAction()==MotionEvent.ACTION_UP) {
				lastButtonId = -1;
				if (moving) {
					if (movingforward && turning) {
						turning = false;
						move("forward");
					}
					else { move("stop"); } 
				}
				return true;
			}
			return false;
		}
    };
    
    OnTouchListener mCamButtonListener = new OnTouchListener() {
		public boolean onTouch(View arg0, MotionEvent arg1) {
			if (arg1.getAction()==MotionEvent.ACTION_DOWN) {
				drivingControlsDown(arg0);
				return true;
			}
			if (arg1.getAction()==MotionEvent.ACTION_UP) {
				if (cameramoving && lastButtonId != mCamHorizButton.getId()) { cameramove("stop"); }
				lastButtonId = -1;
				return true;
			}
			return false;
		}
    };

    OnTouchListener mSpeedButtonListener = new OnTouchListener() {
		public boolean onTouch(View arg0, MotionEvent arg1) {
			if (arg1.getAction()==MotionEvent.ACTION_DOWN) {
				drivingControlsDown(arg0);
				return true;
			}
			if (arg1.getAction()==MotionEvent.ACTION_UP) {
				lastButtonId = -1;
				return true;
			}
			return false;
		}
    };

    @Override
    protected void onPause() {
        super.onPause();
        mWebView.loadUrl("about:blank");  // flush flash plugin, disconnect from oculus server
        mWindowManager.removeView(mDrivingControls);
        if (tiltEnabled) {
            unregisterSensorListeners();
        }
        finish();
    }
    
    @Override
    public void onDestroy() {
        if (tiltEnabled) {
        	unregisterSensorListeners();
        }
        super.onDestroy();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	mMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }
    
    @Override
    public void onBackPressed() {
    	appClose();
    	return;
    }
    
    private void move(String str) {
    	if (str=="stop") { 
        	if (movingforward) {
        		if (turning) { 
        			turning = false;
        			str = "forward";
    			}
        		else { str = null; } 
    		}
        	else  { moving= false; turning= false; movingforward= false; }
		}
    	if (str != null) {
    		mWebView.loadUrl("javascript: callServer('move','"+str+"');");
    	}
    }
    
    private void cameramove(String str) {
    	if (str=="stop") { cameramoving = false; }
    	else { cameramoving = true; }
    	mWebView.loadUrl("javascript: callServer('cameracommand','"+str+"');");
    }
        
    private final class HideDrivingControls implements Runnable {
        public void run() {
        	if (VARIABLE_TAP_DELAY ==0) {VARIABLE_TAP_DELAY = TAP_DELAY; }
        	drivingControlsSetVisible(0);
        }
    }
    
    private final class CheckTap implements Runnable {
        public void run() {
        	boolean buttonPressed = false;
        	if (VARIABLE_TAP_DELAY == 0) { VARIABLE_TAP_DELAY = TAP_DELAY; }
        	if (lastButtonId == mMenuButton.getId()) {
        		openMenu();
        		buttonPressed=true;
        	}
        	
        	if (lastButtonId == mForwardButton.getId()) {
        		moving = true;
        		if (movingforward) {
        			movingforward = false;
            		turning = false;
        			move("stop");
        		}
        		else { 
	        		turning = false;
	        		movingforward = true;
	        		move("forward");
        		}
        		buttonPressed=true;
        	}
        	if (lastButtonId == mBackwardButton.getId()) {
        		moving = true;
        		movingforward = false;
        		turning = false;
        		buttonPressed=true;
        		move("backward");
        	}
        	if (lastButtonId == mStopButton.getId()) {
        		moving = false;
        		movingforward = false;
        		turning = false;
        		buttonPressed=true;
        		if (tiltEnabled) { disableTilt(); }
        		move("stop");
        	}
        	if (lastButtonId == mLeftButton.getId()) {
        		moving = true;
        		turning = true;
        		buttonPressed=true;
        		move("left");
        	}
        	if (lastButtonId == mRightButton.getId()) {
        		moving = true;
        		turning = true;
        		buttonPressed=true;
        		move("right");
        	}
        	
        	if (lastButtonId == mSpeedSlowButton.getId()) {
        		mWebView.loadUrl("javascript: callServer('speedset','slow');");
        		buttonPressed=true;
        	}
        	if (lastButtonId == mSpeedMedButton.getId()) {
        		mWebView.loadUrl("javascript: callServer('speedset','med');");
        		buttonPressed=true;
        	}
        	if (lastButtonId == mSpeedFastButton.getId()) {
        		mWebView.loadUrl("javascript: callServer('speedset','fast');");
        		buttonPressed=true;
        	}
        	
        	if (lastButtonId == mCamUpButton.getId()) {
        		buttonPressed=true;
        		cameramove("up");
        	}
        	if (lastButtonId == mCamDownButton.getId()) {
        		buttonPressed=true;
        		cameramove("down");
        	}
        	if (lastButtonId == mCamHorizButton.getId()) {
        		buttonPressed=true;
        		cameramove("horiz");
        	}
        	
        	if (buttonPressed) {
	            mHandler.removeCallbacks(mHideDrivingControls);
	            mHandler.postDelayed(mHideDrivingControls, AFTER_PRESS_HIDE_CONTROLS_DELAY);
	            Button lastButton = (Button) mDrivingControls.findViewById(lastButtonId);
    			lastButton.setBackgroundColor(0xFFFFFFFF);
    			flashingButtonId =lastButton.getId();
    			mHandler.removeCallbacks(buttonFlashDone);
    			mHandler.postDelayed(buttonFlashDone, BUTTON_FLASH_DELAY);
        	}
        	else {
        		drivingControlsSetVisible(2);
        		VARIABLE_TAP_DELAY = 0;
        	}
        }
    }
    
    private final class ButtonFlashDone implements Runnable {
        public void run() {
        	Button lastButton = (Button) mDrivingControls.findViewById(flashingButtonId);
			lastButton.setBackgroundColor(0x22FFFFFF);
        	flashingButtonId = -1;
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        mMenu.close();
//        mDrivingControls.setVisibility(View.VISIBLE); //invisible
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.menu_cam_mic_on:
        	mWebView.loadUrl("javascript: publish('camandmic');");
        	return true;
    	case R.id.menu_cam_on:
        	mWebView.loadUrl("javascript: publish('camera');");
        	return true;
        case R.id.menu_cam_mic_off:
        	mWebView.loadUrl("javascript: publish('stop');");
        	return true;
        case R.id.menu_dock:
        	mWebView.loadUrl("javascript: callServer('autodock','go');");
        	if (tiltEnabled) { disableTilt(); }
        	return true;
        case R.id.menu_undock:
        	mWebView.loadUrl("javascript: callServer('dock','undock');");
        	return true;
        case R.id.enable_tilt:
        	if (tiltEnabled) { disableTilt(); }
        	else { enableTilt(); }
        	return true;
        case R.id.light_on:
        	mWebView.loadUrl("javascript: callServer('lightsetlevel','100');");
        	return true;
        case R.id.light_off:
        	mWebView.loadUrl("javascript: callServer('lightsetlevel','0');");
        	return true;
    	case R.id.manual_dock_go:
        	mWebView.loadUrl("javascript: callServer('dock','dock');");
        	return true;
    	case R.id.menu_logout:
        	appClose();
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    } 
    
    @Override
    public void onOptionsMenuClosed(Menu menu) {
    	mDrivingControls.setVisibility(View.VISIBLE); 
    }
    
	class AndroidOculusJavaScriptInterface {		
		public String getUser() {
			return user;
		}
		public String getPass() {
			return pass;
//			return "asdfds";
		}
		public void connectionClosed() { // flash message and exit
			appClose();
		}
		public void message(String str) {
			appMessage(str);
		}
		public void lightPresent() {
			
		}
	}


	/*
	 * sensor stuff below
	 */
	
	private void enableTilt() {
        registerSensorListeners();
    	tiltEnabled= true;
    	tiltCalibrated = false;
    	tiltCalibrationAvgCount = 0;
    	tiltSteerMode = "";
//    	appMessage("Tilt enabled"); //, STOP set at current orientation");
    	MenuItem item = (MenuItem) mMenu.findItem(R.id.enable_tilt);
		item.setTitle("Disable Tilt Controls");
	}
	
	private void disableTilt() {
		unregisterSensorListeners();
		tiltEnabled= false;
    	appMessage("Tilt controls disabled");
    	MenuItem item = (MenuItem) mMenu.findItem(R.id.enable_tilt);
		item.setTitle("Enable Tilt Controls");
	}
	
    private void registerSensorListeners() {
        m_sensorManager.registerListener(this, m_sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
        m_sensorManager.registerListener(this, m_sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
    }

    private void unregisterSensorListeners() {
        m_sensorManager.unregisterListener(this);
    }
    
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

	public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accel(event);
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mag(event);
        }		
	}
	
    private void accel(SensorEvent event) {
        if (m_lastAccels == null) {
            m_lastAccels = new float[3];
        }

        System.arraycopy(event.values, 0, m_lastAccels, 0, 3);

        /*if (m_lastMagFields != null) {
            computeOrientation();
        }*/
    }

    private void mag(SensorEvent event) {
        if (m_lastMagFields == null) {
            m_lastMagFields = new float[3];
        }

        System.arraycopy(event.values, 0, m_lastMagFields, 0, 3);

        if (m_lastAccels != null) {
            computeOrientation();
        }
    }

    Filter [] m_filters = { new Filter(), new Filter(), new Filter() };

    private class Filter {
        static final int AVERAGE_BUFFER = 10;
        float []m_arr = new float[AVERAGE_BUFFER];
        int m_idx = 0;

        public float append(float val) {
            m_arr[m_idx] = val;
            m_idx++;
            if (m_idx == AVERAGE_BUFFER)
                m_idx = 0;
            return avg();
        }
        public float avg() {
            float sum = 0;
            for (float x: m_arr)
                sum += x;
            return sum / AVERAGE_BUFFER;
        }

    }
    
    private void computeOrientation() {
        if (SensorManager.getRotationMatrix(m_rotationMatrix, null, m_lastAccels, m_lastMagFields)) {
            SensorManager.getOrientation(m_rotationMatrix, m_orientation);

            /* 1 radian = 57.2957795 degrees */
            /* [0] : yaw, rotation around z axis
             * [1] : pitch, rotation around x axis
             * [2] : roll, rotation around y axis */
            float yaw = m_orientation[0] * 57.2957795f;
            float pitch = m_orientation[1] * 57.2957795f;
            float roll = m_orientation[2] * 57.2957795f;

//            m_lastYaw = m_filters[0].append(yaw);
            m_lastPitch = m_filters[1].append(pitch);
            m_lastRoll = m_filters[2].append(roll);
            
            if (!tiltCalibrated) {
//    			stopRoll =  m_lastRoll;
//            	stopPitch =  m_lastPitch;
//            	tiltCalibrated = true;
            	if (tiltCalibrationAvgCount > 20) {
        			stopRoll = m_lastRoll;
	            	stopPitch = 0; // m_lastPitch;
            		tiltCalibrated = true;
            		appMessage("Tilt enabled, STOP set at current orientation");
//	            	System.out.println("calibrated roll y: " + m_lastRoll);
//					System.out.println("calibrated pitch x: " + m_lastPitch);
            	}
            	tiltCalibrationAvgCount ++;
            	
//            	if (tiltCalibrationAvgCount <3) {
//            		if (tiltCalibrationAvgCount ==0) {
//            			stopRoll = m_lastRoll;
//    	            	stopPitch = m_lastPitch;
//            		}
//            		else {
//            			stopRoll = stopRoll + m_lastRoll;
//		            	stopPitch = stopPitch + m_lastPitch;
//            		}
//					System.out.println("pitch x: " + m_lastPitch);
//					System.out.println("roll y: " + m_lastRoll);
//					tiltCalibrationAvgCount ++;
//            	}
//            	else {
//            		stopRoll = stopRoll/tiltCalibrationAvgCount;
//            		stopPitch = stopPitch/tiltCalibrationAvgCount;
//	            	tiltCalibrated = true;
//					System.out.println("calibrated azi z: " + m_lastYaw);
//					System.out.println("calibrated pitch x: " + m_lastPitch);
//            	}
            	
            }

//			System.out.println("azi z: " + m_lastYaw);
//			System.out.println("pitch x: " + m_lastPitch);
//			System.out.println("roll y: " + m_lastRoll);
            
	        if (tiltCalibrated) {
//		        if (Math.abs(m_lastPitch - stopPitch) <= PITCH_THRESHHOLD && moving == false) {
		            if (m_lastRoll - stopRoll < -ROLL_THRESHHOLD &! tiltSteerMode.equals("backward")) { 
		            	move("backward");
		            	moving = true;
		            	tiltSteerMode = "backward";
		        	}
		            if (m_lastRoll - stopRoll > ROLL_THRESHHOLD &! tiltSteerMode.equals("forward")) { 
		            	move("forward");
		            	moving = true;
		            	tiltSteerMode = "forward";		        	
	            	}
//		        }
		        
		        if (Math.abs(m_lastRoll - stopRoll) <= ROLL_THRESHHOLD && 
		        		Math.abs(m_lastPitch - stopPitch) <= PITCH_THRESHHOLD && moving == true) {
		        	move("stop");
		        	moving = false;
		        	tiltSteerMode = "";
		        }
		
//		        if (Math.abs(m_lastRoll - stopRoll) <= ROLL_THRESHHOLD && moving == false) {
		            if (m_lastPitch - stopPitch < -PITCH_THRESHHOLD &! tiltSteerMode.equals("right")) { 
		            	move("right");
		            	moving = true;
		            	tiltSteerMode = "right";		        	
	            	}
		            if (m_lastPitch - stopPitch > PITCH_THRESHHOLD &! tiltSteerMode.equals("left")) { 
		            	move("left");
		            	moving = true;
		            	tiltSteerMode = "left";
	            	}
//		        }
	        }

        }
    }
    
}
