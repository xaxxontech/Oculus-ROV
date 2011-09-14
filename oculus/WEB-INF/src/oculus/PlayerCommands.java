package oculus;

public enum PlayerCommands {

	publish, move, battStats, docklineposupdate, autodock, autodockcalibrate,
	speech, getdrivingsettings, drivingsettingsupdate, gettiltsettings, cameracommand, tiltsettingsupdate,
	tilttest, speedset, slide, nudge, dock, relaunchgrabber, clicksteer, chat, statuscheck, systemcall, 
	streamsettingsset, streamsettingscustom, motionenabletoggle, playerexit, playerbroadcast, password_update,
	new_user_add, user_list, delete_user, extrauser_password_update, username_update,
	disconnectotherconnections, showlog, monitor, framegrab, emailgrab, assumecontrol, 
	softwareupdate, restart, arduinoecho, arduinoreset, setsystemvolume, beapassenger, muterovmiconmovetoggle,
	spotlightsetbrightness, floodlight, dockgrab, writesetting;

	@Override 
	public String toString() {
		return super.toString();
	}
}
