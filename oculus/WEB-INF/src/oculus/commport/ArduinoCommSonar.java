package oculus.commport;

import java.io.IOException;

import oculus.Application;
import oculus.Discovery;
import oculus.State;
import oculus.Util;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

public class ArduinoCommSonar extends AbstractArduinoComm implements
		SerialPortEventListener, ArduioPort {

	public static final int SONAR_DELAY = 1300;

	public ArduinoCommSonar(Application app) {
		super(app);

		// check for lost connection
		new WatchDog().start();
	}

	/** inner class to check if getting responses in timely manor */
	private class WatchDog extends Thread {
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

				if (getReadDelta() > SONAR_DELAY) {
					new Sender(SONAR);
					Util.delay(SONAR_DELAY);
				}

			}
		}
	}
	
	public void serialEvent(SerialPortEvent event) {
		if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
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
	}

	@Override
	public void execute() {
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
				application.message(getFirmware() + " v: " + version, null,
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
				application.message(Discovery.OCULUS_SONAR + " : " + response,
						null, null);
		}
	}

	@Override
	public String getFirmware() {
		return Discovery.OCULUS_SONAR;
	}

	@Override
	public void connect() {
		try {

			serialPort = (SerialPort) CommPortIdentifier.getPortIdentifier(
					state.get(State.serialport)).open(
					ArduinoCommSonar.class.getName(), SETUP);
			serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8,
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
	}
}