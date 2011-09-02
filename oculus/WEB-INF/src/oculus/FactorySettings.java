package oculus;

import java.util.Properties;

public enum FactorySettings {

	/** these settings must be available in basic configuration */

	skipsetup, speedslow, speedmed, steeringcomp, camservohoriz, camposmax, camposmin, nudgedelay, 
	docktarget, vidctroffset, vlow, vmed, vhigh, vfull, vcustom, vset, maxclicknudgedelay, 
	clicknudgedelaymomentumfactor, clicknudgemomentummult, maxclickcam, volume, mute_rov_on_move, videoscale;


	/** get basic settings */
	public static Properties createDeaults(){
		Properties config = new Properties();
		config.setProperty(skipsetup.toString(), "false");
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
		config.setProperty(clicknudgedelaymomentumfactor.toString(), "0.7");
		config.setProperty(clicknudgemomentummult.toString(), "0.7");
		config.setProperty(maxclickcam.toString(), "14");
		config.setProperty(volume.toString(), "20");
		config.setProperty(mute_rov_on_move.toString(), "true"); // was "yes"
		config.setProperty(videoscale.toString(), "100");
		return config;
	}
	
	@Override
	public String toString() {
		return super.toString();
	}

}
