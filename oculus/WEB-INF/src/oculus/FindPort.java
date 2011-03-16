package oculus;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import sun.security.x509.AVA;

import gnu.io.*;

public class FindPort {

	/* serial port configuration parameters */
	private static final int BAUD_RATE = 19200;
	private static final int TIMEOUT = 2000;
	private static final int DATABITS = SerialPort.DATABITS_8;
	private static final int STOPBITS = SerialPort.STOPBITS_1;
	private static final int PARITY = SerialPort.PARITY_NONE;
	private static final int FLOWCONTROL = SerialPort.FLOWCONTROL_NONE;

	/* add known devices here, strings returned from the firmware */
	public static final String OCULUS = "oculusDC";
	public static final String LIGHTS = "lights";

	protected InputStream inputStream = null;
	protected OutputStream outputStream = null;

	/* reference to the underlying serial port */
	private SerialPort serialPort = null;

	/* list of all free ports */
	private Vector<String> ports = new Vector<String>();

	/** */
	public void getAvailableSerialPorts() {

		@SuppressWarnings("rawtypes")
		Enumeration thePorts = CommPortIdentifier.getPortIdentifiers();
		while (thePorts.hasMoreElements()) {
			CommPortIdentifier com = (CommPortIdentifier) thePorts
					.nextElement();
			switch (com.getPortType()) {
			case CommPortIdentifier.PORT_SERIAL:
				try {
					CommPort thePort = com.open("CommUtil", 50);
					thePort.close();
					ports.add(com.getName());
				} catch (PortInUseException e) {
					System.out.println("Port, " + com.getName()
							+ ", is in use by: " + com.getCurrentOwner());
				} catch (Exception e) {
					System.err.println("Failed to open port " + com.getName());
					e.printStackTrace();
				}
			}
		}
	}

	/** get list of ports available on this particular computer */
	private void initPorts() {

		ports.clear();

		@SuppressWarnings("rawtypes")
		Enumeration pList = CommPortIdentifier.getPortIdentifiers();
		while (pList.hasMoreElements()) {
			CommPortIdentifier cpi = (CommPortIdentifier) pList.nextElement();
			if (cpi.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				if (cpi.isCurrentlyOwned()) {

					System.out.println("taken : " + cpi.getName());

				} else {

					// System.out.println(cpi.getName());

					ports.add(cpi.getName());

				}
			}
		}
	}

	/* constructor tests each port */
	public FindPort() {

		// initPorts();
		getAvailableSerialPorts();

		// for (int i = ports.size()-1 ; i >= 0; i--)
		// System.out.println("port : " + ports.get(i));
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
			serialPort.setSerialPortParams(BAUD_RATE, DATABITS, STOPBITS,
					PARITY);
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
		System.out.println("connected to: " + address);
		return true;
	}

	/** Close the serial port profile's streams */
	public void close() {
		// System.out.println("closing ...");
		try {
			if (inputStream != null) {
				inputStream.close(); }
		} catch (Exception e) {
			System.out.println("close() :" + e.getMessage());
		}

		try {
			if (outputStream != null)
				outputStream.close();
		} catch (Exception e) {
			System.out.println("close() :" + e.getMessage());
		}
		
		if (serialPort != null) {
			serialPort.close();
		}
	}

	/**
	 * Loop through all available serail ports and ask for product id's
	 * 
	 * @param target
	 *            is the device we are looking for on this host's serial ports
	 *            (ie: oculus}lights)
	 * @return the COMXX value of the given device
	 * @throws Exception
	 */
	public String search(String target) throws Exception {

		String portNumber = null;

		for (int i = ports.size() - 1; i >= 0; i--) {

			System.out.println("search: " + ports.get(i));

			if (connect(ports.get(i))) {
				Thread.sleep(2000);

				String id = getProduct();
				if (id.equalsIgnoreCase(target)) {
					portNumber = ports.get(i);
					close();
					break;
				}
				close();
			}
		}
		return portNumber;
	}

	private String getProduct() throws Exception {

		byte[] buffer = new byte[32];
		String device = "";
		// ascii 'x'
		outputStream.write('x');

		// outputStream.write(13);

		Thread.sleep(100);

		System.out.println("avail : " + inputStream.available());

		int read = inputStream.read(buffer); 
		System.out.println("read: " + read); 
		for(int j = 0 ; j < read	 ; j++){
			device += (char) buffer[j];
		}
		device = device.replaceAll("\\s+$", "");
		Thread.sleep(300);

		return device;
	}

	public String getVersion(String port) throws Exception {

		// yte[] buffer = new byte[32];
		String version = "";

		// Thread.sleep(300);

		// ascii 'y' = 121
		outputStream.write('y');

		// outputStream.write(13);

		Thread.sleep(1000);

		System.out.println("avail : " + inputStream.available());

		/*
		 * int read = inputStream.read(buffer); System.out.println("read: " +
		 * read); for(int j = 0 ; j > read ; j++){
		 * System.out.println(buffer[j]); device += buffer[j]; }
		 */

		return version;
	}

	/**
	 * test driver
	 * 
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		System.out.println("port test ...");

		FindPort port = new FindPort();

		// System.out.println("found oculus on: " + port.search(OCULUS));

		// System.out.println("version : " +
		// port.getVersion(port.search(OCULUS)));

		//System.out.println("found lights on: " + port.search(LIGHTS));
		System.out.println("found oculus on: " + port.search(OCULUS));

		// System.out.println("version : " +
		// port.getVersion(port.search(LIGHTS)));

		port.close();
		// System.out.println("... done");
	}
}
