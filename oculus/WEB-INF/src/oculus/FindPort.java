package oculus;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

//import org.red5.logging.Red5LoggerFactory;
//import org.slf4j.Logger;

import gnu.io.*;

public class FindPort {

	/* serial port configuration parameters */
	public static final int BAUD_RATE = 19200;
	public static final int TIMEOUT = 2000;
	public static final int DATABITS = SerialPort.DATABITS_8;
	public static final int STOPBITS = SerialPort.STOPBITS_1;
	public static final int PARITY = SerialPort.PARITY_NONE;
	public static final int FLOWCONTROL = SerialPort.FLOWCONTROL_NONE;

	/* add known devices here, strings returned from the firmware */
	public static final String OCULUS_SERVO = "<oculusServo>";
	public static final String OCULUS_DC = "<oculusDC>";
	public static final String LIGHTS = "<lights>";
	private static final long RESPONCE_DELAY = 300;

	/* reference to the underlying serial port */
	private SerialPort serialPort = null;
	private InputStream inputStream = null;
	private OutputStream outputStream = null;

	/* list of all free ports */
	private Vector<String> ports = new Vector<String>();

	// private static Logger log = Red5LoggerFactory.getLogger(FindPort.class,
	// "oculus");

	/* constructor makes a list of available ports */
	public FindPort() {
		getAvailableSerialPorts();

		// setup thread to fire on shutdown
		/*
		 * Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
		 * public void run() { serialPort.close();
		 * System.out.println("closed by cleanup thread"); } }));
		 */

	}

	/** */
	private void getAvailableSerialPorts() {
		@SuppressWarnings("rawtypes")
		Enumeration thePorts = CommPortIdentifier.getPortIdentifiers();
		while (thePorts.hasMoreElements()) {
			CommPortIdentifier com = (CommPortIdentifier) thePorts.nextElement();
			if (com.getPortType() == CommPortIdentifier.PORT_SERIAL)
				ports.add(com.getName());
		}
	}

	/** connects on start up, return true is currently connected */
	private boolean connect(String address) {
		try {

			/* construct the serial port */
			serialPort = (SerialPort) CommPortIdentifier.getPortIdentifier(address).open("FindPort", TIMEOUT);

			/* configure the serial port */
			serialPort.setSerialPortParams(BAUD_RATE, DATABITS, STOPBITS, PARITY);
			serialPort.setFlowControlMode(FLOWCONTROL);

			/* extract the input and output streams from the serial port */
			inputStream = serialPort.getInputStream();
			outputStream = serialPort.getOutputStream();

		} catch (Exception e) {
			System.out.println("error connecting to: " + address);
			close();
			return false;
		}

		// be sure
		if (inputStream == null)
			return false;
		if (outputStream == null)
			return false;

		// System.out.println("connected to: " + address);
		return true;
	}

	/** Close the serial port streams */
	private void close() {
		if (serialPort != null) {
			// System.out.println("FindPort close(): " + serialPort.getName());
			serialPort.close();
			serialPort = null;
		}
		try {
			if (inputStream != null) {
				inputStream.close();
			}
		} catch (Exception e) {
			System.err.println("input stream close():" + e.getMessage());
		}
		try {
			if (outputStream != null)
				outputStream.close();
		} catch (Exception e) {
			System.err.println("output stream close():" + e.getMessage());
		}
	}

	/**
	 * Loop through all available serial ports and ask for product id's
	 * 
	 * @param target
	 *            is the device we are looking for on this host's serial ports
	 *            (ie: oculus|lights)
	 * @return the COMXX value of the given device
	 */
	public String search(String target) {
		for (int i = ports.size() - 1; i >= 0; i--) {
			if (connect(ports.get(i))) {
				try {
					Thread.sleep(TIMEOUT);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				String id = null;
				try {
					id = getProduct();
				} catch (Exception e) {
					System.out.println("couldn't get a product id on: " + serialPort.getName());
					e.printStackTrace();
				}
				if (id.equalsIgnoreCase(target)) {

					// be sure
					close();

					// found it
					return ports.get(i);
				}
			}

			// close on each loop
			close();
		}

		// error state
		return null;
	}

	public String getProduct() throws Exception {

		byte[] buffer = new byte[32];
		String device = "";

		// be sure there is no old bytes in our reply
		inputStream.skip(inputStream.available());
				
		// send command to arduino
		outputStream.write('x');
		outputStream.write(13);
		
		// wait for reply 
		Thread.sleep(RESPONCE_DELAY);

		// read it 
		int read = inputStream.read(buffer);
		for (int j = 0; j < read; j++)
			device += (char) buffer[j];

		// System.out.println("responce: " + device.trim());
		
		return device.trim();
	}

	/*
	 * public String getVersion(String port) throws Exception {
	 * 
	 * byte[] buffer = new byte[32]; String version = "";
	 * 
	 * if (connect(port)) { Thread.sleep(TIMEOUT);
	 * 
	 * // send command to arduino outputStream.write('y');
	 * Thread.sleep(RESPONCE_DELAY);
	 * 
	 * int read = inputStream.read(buffer); for (int j = 0; j < read; j++)
	 * version += (char) buffer[j];
	 * 
	 * close(); }
	 * 
	 * return version.trim(); }
	 */

	/**
	 * test driver
	 * 
	 * @throws Exception
	 */

	public static void main(String[] args) throws Exception {

		long start = System.currentTimeMillis();

		FindPort port = new FindPort();

		String oculus = port.search(OCULUS_DC);
		if (oculus != null) {
			System.out.println("found oculus on: " + oculus); 
		} else {
			System.out.println("oculus NOT found");
		}

		System.out.println("scan took: " + (System.currentTimeMillis() - start) + " ms");
	}
}