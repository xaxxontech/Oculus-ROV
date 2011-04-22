package oculus;

import java.util.Date;

public class Util {

    private static final int PRECISION = 2;

	/**
     * Delays program execution for the specified delay.
     * 
     * @param delay
     *            is the specified time to delay program execution (milliseconds).
     */
    public static void delay(long delay) {

        try {
            Thread.sleep(delay);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Delays program execution for the specified delay.
     * 
     * @param delay
     *            is the specified time to delay program execution (milliseconds).
     */
    public static void delay(int delay) {

        try {
            Thread.sleep(delay);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /*
	 * 
	 */
    public static String getTime(long ms) {

        //  Sat May 03 15:33:11 PDT 2008
        String date = new Date(ms).toString();

        int index1 = date.indexOf(' ', 0);
        int index2 = date.indexOf(' ', index1 + 1);
        int index3 = date.indexOf(' ', index2 + 1);
        int index4 = date.indexOf(' ', index3 + 1);

        //System.out.println("1: " + index1 + " 2: " + index2 + " 3: " + index3 + " 4: " + index4);

        String time = date.substring(index3 + 1, index4);

        return time;
    }

    /*
	 * 
	 */
    public static String getTime() {
        return getTime(System.currentTimeMillis());
    }

    /**
     * Returns the specified double, formatted as a string, to n decimal places, as
     * specified by precision.
     * <p/>
     * ie: formatFloat(1.1666, 1) -> 1.2 ie: formatFloat(3.1666, 2) -> 3.17 ie:
     * formatFloat(3.1666, 3) -> 3.167
     */
    public static String formatFloat(double number, int precision) {

        String text = Double.toString(number);
        if (precision >= text.length()) {
            return text;
        }

        int start = text.indexOf(".") + 1;
        if (start == 0)
            return text;

        //  cut off all digits and the '.'
        //
        if (precision == 0) {
            return text.substring(0, start - 1);
        }

        if (start <= 0) {
            return text;
        } else if ((start + precision) <= text.length()) {
            return text.substring(0, (start + precision));
        } else {
            return text;
        }
    }

    /**
     * Returns the specified double, formatted as a string, to n decimal places, as
     * specified by precision.
     * <p/>
     * ie: formatFloat(1.1666, 1) -> 1.2 ie: formatFloat(3.1666, 2) -> 3.17 ie:
     * formatFloat(3.1666, 3) -> 3.167
     */
    public static String formatFloat(double number) {

        String text = Double.toString(number);
        if (PRECISION >= text.length()) {
            return text;
        }

        int start = text.indexOf(".") + 1;
        if (start == 0)
            return text;

        if (start <= 0) {
            return text;
        } else if ((start + PRECISION) <= text.length()) {
            return text.substring(0, (start + PRECISION));
        } else {
            return text;
        }
    }
    
    /**
     * Returns the specified double, formatted as a string, to n decimal places, as
     * specified by precision.
     * <p/>
     * ie: formatFloat(1.1666, 1) -> 1.2 ie: formatFloat(3.1666, 2) -> 3.17 ie:
     * formatFloat(3.1666, 3) -> 3.167
     */
    public static String formatString(String number, int precision) {

        String text = number;
        if (precision >= text.length()) {
            return text;
        }

        int start = text.indexOf(".") + 1;

        if (start == 0)
            return text;

        // System.out.println("format string - found dec point at index = " + start );

        //  cut off all digits and the '.'
        //
        if (precision == 0) {
            return text.substring(0, start - 1);
        }

        if (start <= 0) {
            return text;
        } else if ((start + precision) <= text.length()) {
            return text.substring(0, (start + precision));
        }

        return text;
    }

}
