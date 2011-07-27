package developer;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * @author Brad Zdanivsky
 */
public class MulticastChannel implements Runnable {

	public static final String DEFAULT_PORT = "4444";
	public static final String DEFAULT_ADDRESS = "230.0.0.1";
	public static final int BUFFER_SIZE = 1024;

	private MulticastSocket serverSocket = null;
	private InetAddress groupAddress = null;
	private int groupPort = 0;
	private String local = null;
	private Thread server = null;
	private boolean loopback = true;

	private XMLParser parse = new XMLParser();
	private boolean running = true;
	private/* static */CommandManager manager = null;

	/** Constructor */
	MulticastChannel(CommandManager cm) {

		manager = cm;
		String address = DEFAULT_ADDRESS;
		String port = DEFAULT_PORT;

		try {

			groupPort = Integer.parseInt(port);

			/** get group IP */
			groupAddress = InetAddress.getByName(address);

			/** construct the server socket */
			serverSocket = new MulticastSocket(groupPort);

			/** join this group */
			serverSocket.joinGroup(groupAddress);

			/** find our ip */
			local = (InetAddress.getLocalHost()).getHostAddress();

		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		/** start thread, block wait on input from socket */
		server = new Thread(this);
		server.setDaemon(true);
		server.start();
	}

	/** Constructor, use where not needing a call back hook */
	MulticastChannel() {

		String address = DEFAULT_ADDRESS;
		String port = DEFAULT_PORT;

		try {

			groupPort = Integer.parseInt(port);

			/** get group IP */
			groupAddress = InetAddress.getByName(address);

			/** construct the server socket */
			serverSocket = new MulticastSocket(groupPort);

			/** join this group */
			serverSocket.joinGroup(groupAddress);

			/** find our ip */
			local = (InetAddress.getLocalHost()).getHostAddress();

		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	/** @return the address of this node */
	public String getLocalAddress() {
		return local;
	}

	/**
	 * @param enable
	 *            will enable or disable loop back if we want to talk to
	 *            ourselves
	 */
	public void setLoopback(boolean enable) {
		loopback = enable;
	}

	public void close() {
		running = false;
		System.out.println("....closing multicast channel");
	}

	/** Executes this thread */
	public void run() {
		try {
			/** loop until system termination */
			while (running) {

				byte[] buf = new byte[BUFFER_SIZE];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);

				/** block on command input */
				serverSocket.receive(packet);

				String input = new String(packet.getData()).trim();

				/** get sending node's addr */
				String sendersIp = ((InetAddress) packet.getAddress()).getHostAddress();

				/** test the input */
				if (valid(input, sendersIp)) {
					// System.out.println("multicast: " + input);
					Command cmd = parse.parse(input);

					/** dispatch the command */
					if (manager != null) {
						if (cmd != null) {
							try {
								manager.execute(cmd);
							} catch (Exception e) {
								System.out.println("manager.execute(): " + e.getMessage());
							}
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Is this a valid xml command to be dispatch? and ARE WE IN LOOPBACK MODE?
	 * 
	 * @param data
	 *            is the packet to check
	 * @param ip
	 *            is the sender's IP
	 * @return true if this packet should be parsed and dispatched
	 */
	private boolean valid(String data, String ip) {

		/** ignore messages coming from us? */
		if (!loopback) {
			if (local.equals(ip)) {
				return false;
			}
		}

		/** sanity test */
		if (data == null)
			return false;
		if (data.equals(""))
			return false;

		return true;
	}

	/**
	 * @param out
	 *            is a string to write to the socket
	 */
	public void write(String out) {

		try {

			/** dump it into the socket */
			serverSocket.send(new DatagramPacket(out.getBytes(), out.length(),
					groupAddress, groupPort));

		} catch (Exception e) {
			System.err.println("unable to write to socket");
			System.err.println(e.getMessage());
		}
	}

	/**
	 * @param command
	 *            to send to the channel
	 */
	public void write(Command command) {

		// if( constants.getBoolean(ZephyrOpen.showLAN))
		// command.add(ZephyrOpen.localAddress,
		// constants.get(ZephyrOpen.localAddress) );

		// if( constants.getBoolean(ZephyrOpen.externalLookup))
		// command.add(ZephyrOpen.externalAddress,
		// constants.get(ZephyrOpen.externalAddress));

		// if(command.get(ZephyrOpen.userName) == null)
		// command.add(ZephyrOpen.userName, constants.get(ZephyrOpen.userName));

		write(command.toString());
	}
}
