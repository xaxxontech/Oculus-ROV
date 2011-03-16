package oculus;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

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
	public static final String OCULUS_SERVO = "oculusServo";
	public static final String OCULUS_DC = "oculusDC";
	public static final String LIGHTS = "lights";

	/* reference to the underlying serial port */
	private SerialPort serialPort = null;
	private InputStream inputStream = null;
	private OutputStream outputStream = null;

	/* list of all free ports */
	private Vector<String> ports = new Vector<String>();

	/** */
	public void getAvailableSerialPorts() {

		@SuppressWarnings("rawtypes")
		Enumeration thePorts = CommPortIdentifier.getPortIdentifiers();
		while (thePorts.hasMoreElements()) {
			CommPortIdentifier com = (CommPortIdentifier) thePorts.nextElement();
			switch (com.getPortType()) {
			case CommPortIdentifier.PORT_SERIAL:
				try {
					CommPort thePort = com.open("FindPort", TIMEOUT);
					thePort.close();
					ports.add(com.getName());
				} catch (PortInUseException e) {
					System.out.println(com.getName() + ", is in use by: " + com.getCurrentOwner());
				} catch (Exception e) {
					System.err.println("Failed to open port " + com.getName());
					e.printStackTrace();
				}
			}
		}
	}

	/* constructor makes a list of available ports */
	public FindPort() {
		getAvailableSerialPorts();
	}

	/** connects on start up, return true is currently connected */
	private boolean connect(String address) {

		try {

			/* construct the serial port */
			serialPort = (SerialPort) CommPortIdentifier.getPortIdentifier(
					address).open(this.getClass().getName(), TIMEOUT);

		} catch (Exception e) {
			System.out.println("error ininitalizing port: " + address);
		}

		try {

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

		if (inputStream == null)
			return false;

		if (outputStream == null)
			return false;

		// connected
		// System.out.println("connected to: " + address);
		return true;
	}

	/** Close the serial port streams */
	public void close() {
		try {
			if (inputStream != null) {
				inputStream.close(); }
		} catch (Exception e) {
			System.err.println("close() :" + e.getMessage());
		}

		try {
			if (outputStream != null)
				outputStream.close();
		} catch (Exception e) {
			System.err.println("close() :" + e.getMessage());
		}
		
		if (serialPort != null) {
			serialPort.close();
		}
	}

	/**
	 * Loop through all available serial ports and ask for product id's
	 * 
	 * @param target
	 *            is the device we are looking for on this host's serial ports
	 *            (ie: oculus|lights)
	 * @return the COMXX value of the given device
	 * @throws Exception
	 */
	public String search(String target) throws Exception {
		for (int i = ports.size() - 1; i >= 0; i--) {
			if (connect(ports.get(i))) {
				Thread.sleep(TIMEOUT);
				String id = getProduct();
				if (id.equalsIgnoreCase(target)) {
					close();
					return ports.get(i);
				}	
			}
			close();
		}
		
		// error state 
		return null;
	}

	private String getProduct() throws Exception {

		byte[] buffer = new byte[32];
		String device = "";
		
		// send command to arduino 
		outputStream.write('x');
		Thread.sleep(100);

		int read = inputStream.read(buffer); 
		for(int j = 0 ; j < read ; j++)
			device += (char) buffer[j];
		
		device = device.replaceAll("\\s+$", "");
		return device;
	}

	public String getVersion(String port) throws Exception {
		
		String version = "";
		byte[] buffer = new byte[32];
		
		if(connect(port)){
			Thread.sleep(TIMEOUT);
			
			// send command to arduino 
			outputStream.write('y');
			Thread.sleep(100);
			
			int read = inputStream.read(buffer); 
			for(int j = 0 ; j < read ; j++)
				version += (char) buffer[j];
			
			close();
		}
		
		// Thread.sleep(300);
		return version.trim();
	}

	/**
	 * test driver
	 * 
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		FindPort port = new FindPort();

		String oculus = port.search(OCULUS_DC);
		if(oculus != null){
			System.out.println("found oculus on: " + oculus);
			String version = port.getVersion(oculus);
			if(version != null)
				System.out.println("version: " + version);	
		} else System.out.println("oculus NOT found");
		
		String lights = port.search(LIGHTS);
		if(lights != null){
			System.out.println("found lights on: " + oculus);
			String version = port.getVersion(oculus);
			if(version != null)
				System.out.println("version: " + version);	
		} else System.out.println("ligths NOT found");
		
		port.close();
	}
}
