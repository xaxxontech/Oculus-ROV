package oculus;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Encapsulates a command sent from the server to the device.
 * <p/>
 * <b>Syntax:</b> <code>&lt;specifier&gt; [&lt;modifier&gt;, ... ]</code>
 * <p/>
 * Both the specifier and the optional modifiers are called command elements.
 * <blockquoute>
 * 
 * <pre>
 * For example:
 *    load filename
 *      - load: is the command specifier
 *      - filename: is the command modifier
 * </pre>
 * </blockquoute>
 * <p>
 * Both <code>load</code> and <code>filename</code> are considered command elements.
 */
public class Command {

	private static final int LENGTH = 1024;

	/** holds the outer most tags, not part of the command */
	private String type = CommandGUI.oculus;

	/** the command holds each element in the command */
	private Hashtable<String, String> command = null;
	
	private MulticastChannel channel = null;


	/**
	 * Construct a Command of tag, value pairs nested in a 'type' tag 
	 * 
	 * @param type is the <'type'><xxx>...</xxx></'type'> on the outer most tags 
	 */
	public Command(String str) {

		if (str == null)
			return;

		if (str.equals(""))
			return;

		type = str;
		command = new Hashtable<String, String>();
	}

	/** Create a command with the default tag name */
	public Command() {
		command = new Hashtable<String, String>();

		// add time stamp? 
	}

	/** @return the element, specified by the key, from the command */
	public String get(String key) {
		return command.get(key);
	}

	/**
	 * Get the type field
	 * 
	 * @return the command type field
	 */
	public String getType() {
		return type;
	}

	/** Set the type field */
	public void setType(String type) {
		this.type = type;
	}
	
	/**
	 * Deletes the element, specified by the key, from the command.
	 */
	public void delete(String key) {
		command.remove(key);
	}

	/**
	 * Adds the specified key/element pair to the command.
	 * <p/>
	 * If the specified key is null, or an empty string, this method will fail,
	 * and do nothing.
	 * <p/>
	 * If the specified key is already contained in the command, the element
	 * will be updated
	 * 
	 * @param key
	 *            uniquely identifies the element
	 * @param element
	 *            is the value to add
	 */
	public void add(String key, String element) {

		/** sanity checks */
		if (key == null) {
			return;
		}
		if (key.equals("")) {
			return;
		}
		if (element == null) {
			return;
		}
		if (element.equals("")) {
			return;
		}

		/** put the key/element pair into the command */
		try {

			command.put(key, element);

		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	/** @return true if the command contains no elements, else false. */
	public boolean isEmpty() {
		return command.isEmpty();
	}

	/** @return how many elements are in the command. */
	public int size() {
		return command.size();
	}

	/**
	 * Flushes the command of all elements.
	 */
	public void flush() {
		command.clear();
	}

	/**
	 * Returns the contents of this command as an xml string without any
	 * whitespace
	 * 
	 * @return the contents of this command as a formatted xml pairs.
	 */
	public String toXML() {

		/** allocate a string buffer */
		StringBuffer buffer = new StringBuffer(LENGTH);

		/** nested in 'type' tag */
		buffer.append("<" + type + ">");

		/** assemble the name/value pairs */
		for (Enumeration<String> e = command.keys(); e.hasMoreElements();) {
			String element = e.nextElement();
			buffer.append("<" + element + ">");
			buffer.append(get(element));
			buffer.append("</" + element + ">");
		}

		/** outer nested tag */
		buffer.append("</" + type + ">");

		/** check for string buffer overflow 
		if (constants.getBoolean(ZephyrOpen.frameworkDebug))
			if (buffer.length() >= LENGTH) {
				constants.error("Command.toString(): specified size [" + LENGTH
						+ "], " + "actual buffer size [" + buffer.length()
						+ "]", this);
			} */

		/** return the xml fragment */
		return buffer.toString();
	}

	/**
	 * Returns the contents of this command as an xml string without any
	 * whitespace
	 * 
	 * @return the contents of this command as a formatted xml pairs.
	 */
	@Override
	public String toString() {
		return toXML();
	}

	/**
	 * Get the name value pairs directly
	 * 
	 * @return a string formated for a new hashTable
	 */
	public String list() {
		return command.toString();
	}

	public void send() {
		if(channel==null) channel = new MulticastChannel();
		channel.write(command.toString());
	}

	/**
	 * Get a list of values in ordered list, separated by commas
	 * 
	 * 
	 * @return the comma separated list as a string
	 */
	/*
	public String list(String[] commandPrototype) {

		// allocate a string buffer
		StringBuffer buffer = new StringBuffer(LENGTH);

		// index into the prototype
		int index = 0;
		for (; index < commandPrototype.length; index++) {

			// write the values in prototype order
			buffer.append(command.get(commandPrototype[index]));
			buffer.append(", ");
		}
		return buffer.toString();
	}*/
}
