package oculus;

import java.io.*;

public class Settings {
	
	public static final int ERROR = -1; //Integer.MIN_VALUE;
	
	// String filename = System.getenv("RED5_HOME")+"\\webapps\\oculus\\settings.txt";
	private static String filename = System.getenv("RED5_HOME")+"\\conf\\oculus_settings.txt";

	// put all constants here
	public static final String emailalerts = "emailalerts";
	public static final String volume = "volume";
	public static final String loginnotify = "loginnotify";
	public static final String skipsetup = "skipsetup";
	public static final String developer = "developer";
	
	
	/** put all settings here 
	public static enum consants { 
		
		volume, notify, skipsetup, ;
	
		@Override 
		public String toString() {
			return super.toString();
		}	
	} */
	
	/**
	 * lookup values from props file
	 * 
	 * @param key
	 *            is the lookup value
	 * @return the matching value from properties file (or false if not found)
	 */
	public boolean getBoolean(String key){
		if(key==null) return false;
		String str = readSetting(key);
		if(str==null) return false;
		if(str.toUpperCase().equals("YES")) return true;
		else if(str.toUpperCase().equals("TRUE")) return true;		
		return false;
	}

	/**
	 * lookup values from props file
	 * 
	 * @param key
	 *            is the lookup value
	 * @return the matching value from properties file (or zero if not found)
	 */
	public int getInteger(String key) {

		String ans = null;
		int value = ERROR;

		try {

			ans = readSetting(key);
			value = Integer.parseInt(ans);

		} catch (Exception e) {
			return ERROR;
		}

		return value;
	}

	/**
	 * lookup values from props file
	 * 
	 * @param key
	 *            is the lookup value
	 * @return the matching value from properties file (or zero if not found)
	 */
	public double getDouble(String key) {

		String ans = null;
		double value = ERROR;

		try {

			ans = readSetting(key);
			value = Double.parseDouble(ans);

		} catch (Exception e) {
			return ERROR;
		}

		return value;
	}

	
	/**
	 *
	 * read through whole file line by line, extract result
	 * 
	 * @param str this parameter we are looking for 
	 * @return a String value for this given parameter, or
	 *  null if not found
	 */
	public String readSetting(String str) {
		FileInputStream filein;	
		String result=null;
		try{
			
			filein = new FileInputStream(filename);
			BufferedReader reader = new BufferedReader(new InputStreamReader(filein));
			String line = "";
		    while ((line = reader.readLine()) != null) {
		    	String items[] = line.split(" ");
		    	if ((items[0].toUpperCase()).equals(str.toUpperCase())) { result = items[1]; }		        
		    }
		    filein.close();		
		}
		catch (Exception e) { e.printStackTrace(); }
		return result;
	}
	
	/**
	 * modify value of existing settings file 
	 * 
	 * @param setting 
	 * 				is the key to be written to file  
	 * @param value
	 * 				is the integer to parse into a string before being written to file
	 */
	public void writeSettings(String setting, int value) {
	
		String str = null; 
		
		try {
			str = Integer.toString(value);
		} catch (Exception e) {
			return;
		}
	
		if(str != null)
			writeSettings(setting, str);
	}
	
	public void writeSettings(String setting, String value) { // modify value of existing setting
		// read whole file, replace line while you're at it, write whole file
		value = value.replaceAll("\\s+$", ""); // remove trailing whitespace
		FileInputStream filein;
		String[] lines = new String[999];
		try{
			
			filein = new FileInputStream(filename);
			BufferedReader reader = new BufferedReader(new InputStreamReader(filein));			
			int i=0;
		    while ((lines[i] = reader.readLine()) != null) { 
		    	String items[] = lines[i].split(" ");
		    	if ((items[0].toUpperCase()).equals(setting.toUpperCase())) {
		    		lines[i] = setting+" "+value;
		    	}
		    	i++;
		    }
		    filein.close();
		}
		catch (Exception e) { e.printStackTrace(); }
		
		FileOutputStream fileout;
		try{
			fileout = new FileOutputStream (filename);
			for (int n=0; n<lines.length; n++) {
				if (lines[n] != null) {
					new PrintStream(fileout).println (lines[n]);
				}
			}
		    fileout.close();		
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	/**
	 * read whole file, add single line, write whole file
	 * 
	 * @param setting
	 * @param value
	 */
	public void newSetting(String setting, String value) {

		setting = setting.trim();// setting.replaceAll("\\s+$", ""); // remove trailing whitespace
		value = value.trim();  // value.replaceAll("\\s+$", ""); 
		
		FileInputStream filein;
		String[] lines = new String[999];
		try
		{
			filein = new FileInputStream(filename);
			BufferedReader reader = new BufferedReader(new InputStreamReader(filein));			
			int i=0;
		    while ((lines[i] = reader.readLine()) != null) { 
	    		lines[i] = lines[i].replaceAll("\\s+$", ""); 
	    		if (!lines[i].equals("")) { i++; }
		    }
		    filein.close();
		}
		catch (Exception e) { e.printStackTrace(); }
		
		FileOutputStream fileout;
		try
		{
			fileout = new FileOutputStream (filename);
			for (int n=0; n<lines.length; n++) {
				if (lines[n] != null) {
					new PrintStream(fileout).println (lines[n]);
				}
			}
			new PrintStream(fileout).println (setting+" "+value);
		    fileout.close();		
		}
		catch (Exception e) { e.printStackTrace(); }
	}

	public void deleteSetting(String setting) {
		//read whole file, remove offending line, write whole file
		setting = setting.replaceAll("\\s+$", ""); // remove trailing whitespace
		FileInputStream filein;
		String[] lines = new String[999];
		try
		{
			filein = new FileInputStream(filename);
			BufferedReader reader = new BufferedReader(new InputStreamReader(filein));			
			int i=0;
		    while ((lines[i] = reader.readLine()) != null) { 
		    	String items[] = lines[i].split(" ");
		    	if ((items[0].toUpperCase()).equals(setting.toUpperCase())) {
		    		lines[i] = null;
		    	} 
	    		i++;
		    }
		    filein.close();
		}
		catch (Exception e) { e.printStackTrace(); }
		
		FileOutputStream fileout;
		try
		{
			fileout = new FileOutputStream (filename);
			for (int n=0; n<lines.length; n++) {
				if (lines[n] != null) {
					new PrintStream(fileout).println (lines[n]);
				}
			}
		    fileout.close();		
		}
		catch (Exception e) { e.printStackTrace(); }
	}
	
	public String readRed5Setting(String str) {
		String filenm = System.getenv("RED5_HOME")+"\\conf\\red5.properties";
		FileInputStream filein;	
		String result=null;
		try
		{
			filein = new FileInputStream(filenm);
			BufferedReader reader = new BufferedReader(new InputStreamReader(filein));
			String line = "";
		    while ((line = reader.readLine()) != null) {
				String s[] = line.split("=");
				if (s[0].equals(str)) { result = s[1]; }
		    }
		    filein.close();
		}
		catch (Exception e) { e.printStackTrace(); }
		return result;
	}
	
	public void writeRed5Setting(String setting, String value) { // modify value of existing setting
		// read whole file, replace line while you're at it, write whole file
		String filenm = System.getenv("RED5_HOME")+"\\conf\\red5.properties";
		value = value.replaceAll("\\s+$", ""); // remove trailing whitespace
		FileInputStream filein;
		String[] lines = new String[999];
		try
		{
			filein = new FileInputStream(filenm);
			BufferedReader reader = new BufferedReader(new InputStreamReader(filein));			
			int i=0;
		    while ((lines[i] = reader.readLine()) != null) { 
		    	String items[] = lines[i].split("=");
		    	if ((items[0].toUpperCase()).equals(setting.toUpperCase())) {
		    		lines[i] = setting+"="+value;
		    	}
		    	i++;
		    }
		    filein.close();
		}
		catch (Exception e) { e.printStackTrace(); }
		
		FileOutputStream fileout;
		try
		{
			fileout = new FileOutputStream (filenm);
			for (int n=0; n<lines.length; n++) {
				if (lines[n] != null) {
					new PrintStream(fileout).println (lines[n]);
				}
			}
		    fileout.close();		
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}


