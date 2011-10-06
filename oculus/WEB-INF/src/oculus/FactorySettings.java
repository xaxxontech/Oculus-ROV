package oculus;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public enum FactorySettings {

	/** these settings must be available in basic configuration */

	skipsetup, speedslow, speedmed, steeringcomp, camservohoriz, camposmax, camposmin, nudgedelay, 
	docktarget, vidctroffset, vlow, vmed, vhigh, vfull, vcustom, vset, maxclicknudgedelay, 
	clicknudgedelaymomentumfactor, clicknudgemomentummult, maxclickcam, mute_rov_on_move, 
	videoscale, volume, holdservo, loginnotify;

	/** get basic settings */
	public static Properties createDeaults() {
		Properties config = new Properties();
		config.setProperty(skipsetup.toString(), "no");
		config.setProperty(speedslow.toString(), "115");
		config.setProperty(speedmed.toString(), "180");
		config.setProperty(steeringcomp.toString(), "128");
		config.setProperty(camservohoriz.toString(), "68");
		config.setProperty(camposmax.toString(), "89");
		config.setProperty(camposmin.toString(), "58");
		config.setProperty(nudgedelay.toString(), "150");
		config.setProperty(docktarget.toString(), "1.194_0.23209_0.17985_0.22649_129_116_80_67_-0.045455");
		config.setProperty(vidctroffset.toString(), "0");
		config.setProperty(vlow.toString(), "320_240_4_85");
		config.setProperty(vmed.toString(), "320_240_8_95");
		config.setProperty(vhigh.toString(), "640_480_8_85");
		config.setProperty(vfull.toString(), "640_480_8_95");
		config.setProperty(vcustom.toString(), "1024_768_8_85");
		config.setProperty(vset.toString(), "vmed");
		config.setProperty(maxclicknudgedelay.toString(), "580");
		config.setProperty(clicknudgemomentummult.toString(), "0.7");
		config.setProperty(maxclickcam.toString(), "14");
		config.setProperty(volume.toString(), "20");
		config.setProperty(mute_rov_on_move.toString(), "true"); 
		config.setProperty(videoscale.toString(), "100");
		config.setProperty(holdservo.toString(), "false");
		config.setProperty(loginnotify.toString(), "false");
		return config;
	}

	/** @returns true if all settings are in properties */
	public static boolean validate(Properties conf) {
		Settings fromfile = new Settings();
		String value = null;
		for (FactorySettings settings : FactorySettings.values()) {
			value = fromfile.readSetting(settings.toString());
			if (value == null) {
				System.out.println(conf.toString());
				System.out.println("settings file missing: " + settings);
				return false;
			}
		}
		return true;
	}

	/** write to file in the order set in enum */
	public synchronized static void createFile() {

		// kill if exists
		
		FileWriter fw = null;
		try {
			fw = new FileWriter(new File(Settings.filename+".tmp"));
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		
		try {
			
			fw.append("# FACTORY RESET on: " + new java.util.Date().toString() + "\r\n");
			final Properties props = createDeaults();
			for (FactorySettings factory : FactorySettings.values()) {
				fw.append(factory.toString() + " "
						+ props.getProperty(factory.toString()) + "\r\n");
			}

			fw.close();
			
		} catch (IOException e) {
			try {
				fw.close();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
		
		if (new File(Settings.filename).exists()) new File(Settings.filename).delete();

		new File(Settings.filename+".tmp").renameTo(new File(Settings.filename));
	}

	@Override
	public String toString() {
		return super.toString();
	}

}
