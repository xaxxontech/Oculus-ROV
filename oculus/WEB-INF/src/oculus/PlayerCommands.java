package oculus;

public enum PlayerCommands {

	publish, move, battStats, docklineposupdate, autodock, autodockcalibrate,
	speech, getdrivingsettings, drivingsettingsupdate, gettiltsettings, cameracommand, tiltsettingsupdate,
	tilttest, speedset, slide, nudge, dock, relaunchgrabber, clicksteer, chat, statuscheck, systemcall, 
	streamsettingsset, streamsettingscustom, motionenabletoggle, playerexit, playerbroadcast, password_update,
	new_user_add, user_list, delete_user, extrauser_password_update, username_update,
	disconnectotherconnections, showlog, monitor, framegrab, emailgrab, assumecontrol, 
	softwareupdate, restart, arduinoecho, arduinoreset, setsystemvolume, beapassenger, muterovmiconmovetoggle,
	spotlightsetbrightness, floodlight, dockgrab, writesetting, holdservo;

	
	public enum playerOnlyCommands {

		// TODO: CHECK THESE 
		publish, move, battStats, docklineposupdate, autodock, autodockcalibrate,
		speech, getdrivingsettings, drivingsettingsupdate, gettiltsettings, cameracommand, tiltsettingsupdate,
		tilttest, speedset, slide, nudge, dock, clicksteer, chat, statuscheck, systemcall, 
		streamsettingsset, streamsettingscustom, motionenabletoggle, playerexit, playerbroadcast, password_update, 
		extrauser_password_update, username_update,
		disconnectotherconnections, monitor, framegrab, emailgrab, assumecontrol, 
		softwareupdate, setsystemvolume, beapassenger, 
			
	}
	

	/**
	 * @return true if given command is in the sub-set
	 */
	public static boolean requiresPlayer(PlayerCommands cmd){
		for(playerOnlyCommands admin : playerOnlyCommands.values()){
			if(admin.equals(cmd)) 
				return true;
		}
		
		return false;
	}
	

	public enum AdminCommands {
		
		//TODO: CHECK THESE 
		new_user_add, user_list, delete_user, extrauser_password_update, username_update,
		disconnectotherconnections, showlog
	}
	

	/**
	 * @return true if given command is in the sub-set
	 */
	public static boolean requiresAdmin(PlayerCommands cmd){
		for(AdminCommands admin : AdminCommands.values()){
			if(admin.equals(cmd)) 
				return true;
		}
		
		return false;
	}
	
	
	@Override 
	public String toString() {
		return super.toString();
	}
}
