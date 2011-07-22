package developer;


/**
 * XML Parser implementation.
 * <p/>
 * Created: 2003.11.19
 * 
 * @author Brad Zdanivsky
 * @author Peter Brandt-Erichsen
 */
public final class XMLParser {

    /**
     * Constructs the parser with zero parameters.
     */
    public XMLParser() {}

    /**
     * Parses the specified xml string and fills the specified command with the name/value
     * pairs of each element.
     * 
     * @param xml
     *            contains the xml-formatted string to parse
     * @return a command filled with the element values
     */
    public Command parse(String xml) {

        if (xml == null || xml.equals(""))
            return null;

        // send back  command object 
        Command command = null;

        try {

            // find the outer most tag
            // the '<type>' of xml command   
            String type = getType(xml);
            if (type != null) {

                command = new Command(type);

                // cut out outer most 'type' tags  
                xml = removeOuterElementTags(xml);

            } else {

                // make a default, no-name command  
                command = new Command();
            }

            // parse the xml fragment
            for (;;) {

                /* end of parse? */
                if (xml.length() <= 0)
                    break;

                /* parse the element name and value */
                String elementName = getType(xml);
                String elementValue = get(elementName, xml);

                /* add the name/value pair to the command */
                command.add(elementName, elementValue);

                /* remove the parsed element from the xml fragment */
                xml = removeElement(elementName, xml);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* return the filled command */
        return command;
    }

    /**
     * Parses and returns the name of the first element contained in the specified xml
     * string.
     */
    public static String parseElementName(String xml) {

        int start = xml.indexOf("<") + 1;
        int end = xml.indexOf(">");
        return xml.substring(start, end);
    }

    /**
     * Parses and returns the value of the first element contained in the specified xml
     * string.
     */
    public static String parseElementValue(String xml) {

        int start = xml.indexOf(">") + 1;
        int end = xml.indexOf("</");
        return xml.substring(start, end);
    }

    /**
     * @param tag
     *            the element name to be removed
     * @param xml
     *            the xml data to chop the above tag out of
     * @return the remaining xml data
     */
    public static String removeElement(String tag, String xml) {

        try {

            int start = xml.indexOf("</" + tag + ">");
            start += 3 + tag.length();

            xml = xml.substring(start, xml.length());

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return xml;
    }

    /**
     * Removes the first element from the specified xml string. ie:
     * <control>value</control>
     */
    public static String removeElement(String xml) {

        // remove the element
        int start = xml.indexOf("</");
        start = xml.indexOf(">", start);

        return xml.substring((start + 1));
    }

    /**
     * Removes the outer element tags from the specified xml string. ie:
     * <command></command>
     */
    public static String removeOuterElementTags(String xml) {

        // remove the outer element start tag
        int start = xml.indexOf(">");
        xml = xml.substring((start + 1));

        // remove the outer element end tag, and return the result
        return removeOuterEndTag(xml);
    }

    /**
     * Removes the outer element end tag from the specified xml string. ie: </command>
     */
    public static String removeOuterEndTag(String xml) {

        try {
			int end = xml.lastIndexOf("</");
			return xml.substring(0, end);
		} catch (Exception e) {
			// ignore it
			return "";
		}
    }

    /**
     * </p> example: <tag><!--- return this payload ---></tag>
     * 
     * note: nested tags can fail, basic matching only
     * 
     */
    public static String get(String tag, String xml) {

        try {

            // find the first <tag/>
            int start = xml.indexOf(tag) + tag.length() + 1;

            // subtract off the '/>' chars
            int end = xml.indexOf(tag, start) - 2;

            // send back the chewy center
            return xml.substring(start, end);

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Return the first tag of this xml packet
     * 
     * 
     * </p>example: <command><data>xxxx</data></command> will return "command"
     * 
     */
    public static String getType(String xml) {

        try {

            // trim any whitespace out first
            // xml = xml.trim();

            if (xml == null || xml.equals(""))
                return null;

            // if not xml tag, get out of here
            if (xml.charAt(0) != '<') {
                return null;
            }

            // find first ">"
            int first = xml.indexOf(">");

            // chop out first tag, min is <x>y</x>
            return xml.substring(1, first);

        } catch (Exception e) {
            return null;
        }
    }
}
