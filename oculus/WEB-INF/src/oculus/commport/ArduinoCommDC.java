package oculus.commport;

import java.io.IOException;

import oculus.Application;
import oculus.Discovery;
import oculus.State;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

public class ArduinoCommDC extends AbstractArduinoComm implements SerialPortEventListener, ArduioPort {
	
	public ArduinoCommDC(Application app) {
		super(app);

		// check for lost connection
		new WatchDog().start();
	}

	public void connect(){
		try {

			serialPort = (SerialPort) CommPortIdentifier.getPortIdentifier(
					state.get(State.serialport)).open(
					AbstractArduinoComm.class.getName(), SETUP);
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
				version = response.substring(response.indexOf("version:") + 8, response.length());
				application.message(Discovery.OCULUS_DC + " : " + version, null, null);
			} else return;

			// don't bother showing watch dog pings to user screen
		} else if (response.charAt(0) != GET_VERSION[0]) {
			application.message(Discovery.OCULUS_DC + " : " + response, null, null);
		}
	}

	@Override
	public String getFirmware() {
		return Discovery.OCULUS_DC;
	}
}