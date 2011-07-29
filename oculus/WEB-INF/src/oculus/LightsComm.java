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

public class LightsComm implements SerialPortEventListener {

	private Logger log = Red5LoggerFactory.getLogger(LightsComm.class, "oculus");

	// shared state variables 
	private State state = State.getReference();
	
	public static final int SETUP = 2000;
	//public static final int WATCHDOG_DELAY = 1500;

	public static final byte[] DIM = {'f'};
	public static final byte[] BRIGHTER = {'b'};
	public static final byte SET_PWM = 's';
	public static final byte[] GET_VERSION = {'y'};
	private static final byte[] ECHO_ON = {'e', '1'};
	private static final byte[] ECHO_OFF = {'e', '0'};
	
	// comm cannel 
	private SerialPort serialPort = null;
	private InputStream in;
	private OutputStream out;
	
	// will be discovered from the device 
	protected String version = null;

	// input buffer
	private byte[] buffer = new byte[32];
	private int buffSize = 0;

	// track write times
	private long lastSent = System.currentTimeMillis();
	private long lastRead = System.currentTimeMillis();

	// Settings settings = new Settings(); 
	
	// make sure all threads know if connected 
	private volatile boolean isconnected = false;
	
	public int lightLevel = 0;
	
	// call back
	private Application application = null;

	/**
	 * Constructor but call connect to configure
	 * 
	 * @param app 
	 * 			  is the main oculus application, we need to call it on
	 * 			Serial events like restet            
	 */
	public LightsComm(Application app) {
		
		// call back to notify on reset events etc
		application = app; 
		
		if( state.get(State.lightport) != null ){
			new Thread(new Runnable() { 
				public void run() {
					connect();				
					Util.delay(SETUP);
						
						// start with them off ??
						off();
						
						// setEcho(true);
						
						// check for lost connection
						// new WatchDog().start();	
				}	
			}).start();
		}	
	}
	
	/** open port, enable read and write, enable events */
	public void connect() {
		try {

			serialPort = (SerialPort)CommPortIdentifier.getPortIdentifier(
					state.get(State.lightport)).open(LightsComm.class.getName(), SETUP);
			serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

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
		
		//isconnected = true;
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
					// print() or println() from arduino code
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

	// act on feedback from arduino  
	private void execute() {
		String response = "";
		for(int i = 0 ; i < buffSize ; i++)
			response += (char)buffer[i];
		
		// take action as arduino has just turned on 
		if(response.equals("reset")){
			
			// might have new firmware after reseting 
			version = null;
			new Sender(GET_VERSION);
			isconnected = true;
			
		} else if(response.startsWith("version:")){
			
			// NOTE: watchdog will send a get version command if idle comm port  
			if(version == null){
				// get just the number 
				version = response.substring( 
						response.indexOf("version:")+8, response.length());
	
				application.message("lights version: " + version, null, null);
				
			} else return;
			// don't bother showing watchdog pings to user screen 
		} else if(response.charAt(0) != GET_VERSION[0])		
			application.message("light: " + response, null, null);
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
			if(isConnected())start();
		}
		public void run() {
			sendCommand(command);
		}
	}

	/** @param update is set to true to turn on echo'ing of serial commands */
	public void setEcho(boolean update){
		if(update) 
			new Sender(ECHO_ON);
		else 
			new Sender(ECHO_OFF);
	}
	
	/** inner class to check if getting responses in timely manor 
	private class WatchDog extends Thread {
		public WatchDog() {
			this.setDaemon(true);
		}
		public void run() {
			Util.delay(SETUP);
			application.message("starting watchdog thread", null, null);
			while (true) {
				if (getReadDelta() > DEAD_TIME_OUT) {
					if (isconnected) {
						reset(); 
						application.message("watchdog time out, resetting", null, null);
					}
				}

				// send ping to keep connection alive 
				if(getReadDelta() > (DEAD_TIME_OUT / 3))
					if(isconnected) new Sender(GET_VERSION);
			
				Util.delay(WATCHDOG_DELAY);
			}
		}
	}*/
	
	public void reset(){
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
		} catch (Exception e) {
			System.out.println("close(): " + e.getMessage());
		}
		serialPort.close();
	}

	/**
	 * Send a multi byte command to send the arduino 
	 * 
	 * @param command
	 *            is a byte array of messages to send
	 */
	private synchronized void sendCommand(final byte[] command) {
		
		if(!isconnected) return;
		
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

	/** set default level */
	public void on() {
		new Sender(new byte[]{SET_PWM, (byte) 255});
	}
	
	public void off(){
		new Sender(new byte[]{SET_PWM, 0});
	}
	
	public void setLevel(int target){
		int n = target*255/100;
		new Sender(new byte[]{SET_PWM, (byte) n});
		application.message("light level set to "+target+"%", null, null);
		lightLevel = target;
	}
}