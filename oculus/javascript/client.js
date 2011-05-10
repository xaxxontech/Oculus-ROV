var sentcmdcolor = "#777777";
var systimeoffset;
var enablekeyboard = false;
var lagtimer = 0;
var officiallagtimer = 0;
var maxlogbuffer = 50000;
var statusflashingarray= new Array();
var pinginterval = null; //timer
var pingcountdown = null; //timer
var pingintervalcountdowntimer; //timer
var pingcountdownaftercheck; //timer
var laststatuscheck = null;
var pingintervalamount = 5;
var pingtimetoworry = 5;
var battcheck = 0;
var battcheckinterval = 180;
var statuscheckreceived = false;
var missedstatuschecks = 0;
var cspulsatenum = 20;
var cspulsatenumdir = 1;
var cspulsate=false;
var cspulsatetimer = null; //timer
var ctroffset = 0; 
var ctroffsettemp;
var docklinewidth=60;
var videooverlaymouseposinterval = null; // timer
var videomouseaction = false;
var connected = false;
var logintimeout = 5000; 
var logintimer; // timer
var username;
var streammode = "stop";
var streamdetails = [];
var steeringmode;
var cameramoving = false;
var broadcasting = false;
var clicksteeron = true;
var maxmessagecontents = 150000;
var maxmessageboxsize = 4000;
var tempimage = new Image();
tempimage.src = 'images/eye.gif';
var tempimage2 = new Image();
tempimage2.src = 'images/ajax-loader.gif';
var admin = false;
var userlistcalledby;
var initialdockedmessage = false;
var html5 = true;
var faceboxtimer;
var facegrabon = false;
var tempdivtext;
var autodocking = false;
var sendcommandtimelast = 0;
var lastcommandsent;

function loaded() {
	if (clicksteeron) { clicksteer("on"); }
    overlay("on");
	resized();
	var b = new Image();
	b.src = 'images/steering_icon_selected.gif';
}

function resized() {
	docklineposition();
	videooverlayposition();
	overlay("");
	videologo("");
}

function flashloaded() {
	if (/auth=/.test(document.cookie)) { loginfromcookie(); }
	else { login(); }
	videologo("on");
}

function countdowntostatuscheck() {
	pinginterval = null;
	pingcountdown = setTimeout("statuscheck();",50);
}

function statuscheck() { 
	if (connected) {
		pingcountdown = null;
		str = "";
		battcheck += pingintervalamount;
		if (battcheck >= battcheckinterval) {
			battcheck = 0;
			str = "battcheck";
		}
		callServer("statuscheck",str); 
		var i = new Date().getTime();
		laststatuscheck = i; 
		officiallagtimer = i;
		pinginterval = setTimeout("countdowntostatuscheck()", pingintervalamount*1000);
		statuscheckreceived=false;
		setTimeout("checkforstatusreceived();",pingtimetoworry*1000);
		pingcountdownaftercheck = setTimeout("pingcountdownaftercheck = null", 10);
	}
}

function checkforstatusreceived() {
	if (statuscheckreceived==false) {
		missedstatuschecks += 1;
		clearTimeout(pinginterval);
		clearTimeout(pingcountdown);
		message("status request failed", sentcmdcolor);
		setstatus("lag","<span style='color: red;'>LARGE</span>");
		if (missedstatuschecks > 20) {
			setstatus("connection","<span style='color: #666666;'>RELOAD PAGE</span>");
		}
		else { countdowntostatuscheck(); }
	}
	else { 
		missedstatuschecks =0;
	}
}

function callServer(fn, str) {
	if (pingcountdownaftercheck != null) {
		message("command delayed 10ms", sentcmdcolor);
		setTimeout("callServer('"+fn+"','"+str+"');",10);
	}
	else {
		if (pingcountdown != null) {
			clearTimeout(pingcountdown);
			countdowntostatuscheck();
		}
		//var sendcommandtimelast = 0;
		//var lastcommandsent;

		
		var nowtime = new Date().getTime();
		if (!(lastcommandsent == fn+str && nowtime - sendcommandtimelast < 200)) {
			getFlashMovie("oculus_player").flashCallServer(fn,str);
		}
		else message("rapid succession command dropped",sentcmdcolor);
		sendcommandtimelast = nowtime;
		lastcommandsent = fn+str;
	}
}

function play(str) { // called by javascript only?
	streammode = str;
	var num = 1;
	if (streammode == "stop") { num =0 ; } 
	getFlashMovie("oculus_player").flashplay(num);
}

function getFlashMovie(movieName) {
	var isIE = navigator.appName.indexOf("Microsoft") != -1;
	return (isIE) ? window[movieName] : document[movieName];
}

function publish(str) {
	if (str=="broadcast") {
		if (broadcasting) { 
			callServer("playerbroadcast","off"); 
			broadcasting = false;
			message ("sending: playerbroadcast off",sentcmdcolor);
			clicksteer("on");
		}
		else { 
			callServer("playerbroadcast","on");
			broadcasting = true;
			message ("sending: playerbroadcast on",sentcmdcolor);
			clicksteer("off");
		}
	}
	else {
		message("sending command: publish " + str, sentcmdcolor);
		callServer("publish", str);
		lagtimer = new Date().getTime();
	}
}

function message(message, colour, status, value) {
	if (message != null) {
		var tempmessage = message;
		var d = new Date();
		

		if (message == "status check received") { 
			statuscheckreceived=true;
			if (officiallagtimer != 0) {
				var i = d.getTime() - officiallagtimer;
				setstatus("lag",i+"ms");
			}
		}
		else {
			
			
//		if (false) {
//		}
//		else {
//			if (message == "status check received") { 
//				message += ":"+status + ":"+value;
//				statuscheckreceived=true;
//				if (officiallagtimer != 0) {
//					var i = d.getTime() - officiallagtimer;
//					setstatus("lag",i+"ms");
//				}
//			}
			
			
			message = "<span style='color: #444444'>:</span><span style='color: "
					+ colour + "';>" + message + "</span>";
			messageboxping = "";
			hiddenmessageboxping = "";
			if (lagtimer != 0 && colour != sentcmdcolor) {
				var n = d.getTime() - lagtimer;
				messageboxping = " <span style='color: "+sentcmdcolor+"; font-size: 11px'>" + n + "ms</span>";
				hiddenmessageboxping = " <span style='color: orange'>" + n + "ms</span>";
			} 
			var a = document.getElementById('messagebox');
			var str = a.innerHTML;
			if (str.length > maxmessageboxsize) {
				str = str.slice(0, maxmessageboxsize);
				document.getElementById('messagemorelink').style.display = "";
			}
			else { document.getElementById('messagemorelink').style.display = "none"; }
			if (colour != sentcmdcolor) { 
				a.innerHTML = message + messageboxping + "<br>" + str; 
			}

			var b = document.getElementById('hiddenmessagebox');
			str = b.innerHTML;
			if (str.length > maxmessagecontents) {
				str = str.slice(0, maxmessagecontents);
			}
			var datetime = "<span style='color: #444444; font-size: 11px'>";
			datetime += d.getDate()+"-"+(d.getMonth()+1)+"-"+d.getFullYear();
			datetime += " "+d.getHours()+":"+d.getMinutes()+":"+d.getSeconds();
			datetime +="</span>";
			b.innerHTML = message + hiddenmessageboxping + " &nbsp; " + datetime + "<br>" + str + " ";
		}
		if (tempmessage == "playing stream") { videologo("off"); } 
		if (tempmessage == "stream stopped") { videologo("on"); docklinetoggle("off"); }
		lagtimer = 0;
	}
	if (status != null) {  //(!/\S/.test(d.value))
		if (status == "multiple") { setstatusmultiple(value); }
		else { setstatus(status, value); }
	}
}

function setstatus(status, value) {
	var a;
	if (a= document.getElementById(status+"_status")) {
		if (true) { // (a.innerHTML.toUpperCase() != value.toUpperCase() || status == "lag") { //if value is changing from what it was, continue...
		// if (status != "keyboard" || (status == "keyboard" && a.innerHTML.toUpperCase() != value.toUpperCase())) {
			// if (status == "cameratilt" || status=="lag") { a.innerHTML = value; } // has &deg;    !
			// else { a.innerHTML = value.toUpperCase(); }
			if (status=="dock" && value == "docking") { 
				value += " <img src='images/ajax-loader.gif' style='vertical-align: bottom;'>";
			}
			a.innerHTML = value;
			if (status == "connection" && value == "closed") { 
				a.style.color = "red";
				connected = false;
				setstatusunknown();
				videologo("on");
			}
			var clr = a.style.color;
			var bclr = a.style.backgroundColor;
			b=document.getElementById(status+"_title");
			var tclr = b.style.color;
			var tbclr = "#1b1b1b";
			if (statusflashingarray[status]==null) {
				statusflashingarray[status]="flashing";
				a.style.color = bclr;
				a.style.backgroundColor = clr;
				b.style.color = "#ffffff";
				b.style.backgroundColor = tbclr;
				var str1 = "document.getElementById('"+status+"_status').style.color='"+clr+"'; ";
				str1 += "document.getElementById('"+status+"_status').style.backgroundColor='"+bclr+"';";
				str1 += "document.getElementById('"+status+"_title').style.backgroundColor='"+bclr+"';";
				var str2 = "document.getElementById('"+status+"_status').style.color='"+bclr+"'; ";
				str2 += "document.getElementById('"+status+"_status').style.backgroundColor='"+clr+"';";
				str2 += "document.getElementById('"+status+"_title').style.color='"+tclr+"';";
				setTimeout(str1, 100);
				setTimeout(str2, 150);
				setTimeout(str1+"statusflashingarray['"+status+"']=null;", 250);
			}
		}
	}
	if (status=="vidctroffset") { ctroffset = parseInt(value); }
	//if (status=="motion" && value=="disabled") { motionenabled = false; }
	if (value == "connected" && !connected) { // initialize
		overlay("off");
		countdowntostatuscheck(); 
		connected = true;
		keyboard("enable");
		setTimeout("videomouseaction = true;",30); // firefox screen glitch fix
		clearTimeout(logintimer);
	}
	if (status == "storecookie") {
		createCookie("auth",value,30); 
	}
	if (status == "someonealreadydriving") { someonealreadydriving(value); }
	if (status == "user") { username = value; }
	if (status == "hijacked") { window.location.reload(); }
	if (status == "stream" && (value.toUpperCase() != streammode.toUpperCase())) { play(value); }
	if (status == "admin" && value == "true") { admin = true; }
	if (status == "dock") {
		if (initialdockedmessage==false) {
			initialdockedmessage = true;
			if (value == "docked") {
				message("docked, charging","green");
			}
		}
		if (!/docking/i.test(value)) {
			docklinetoggle("off");
		}
	}
	if (status == "streamsettings") {
		streamdetails = value.split("_");
	}
	if (status=="facefound") { facefound(value); }
	if (status=="autodocklock") { autodocklock(value)}
	if (status=="autodockcancelled") { autodocking=false; autodock("cancel"); }
	if (status=="softwareupdate") {
		if (value=="downloadcomplete") { softwareupdate("downloadcomplete",""); }
		else { softwareupdate("available",value); }
	}
	if (status == "framegrabbed") { framegrabbed(); }

}

function setstatusmultiple(value) {
	var statusarray = new Array();
	statusarray = value.split(" ");
	for (var i = 0; i<statusarray.length; i+=2) {
		setstatus(statusarray[i], statusarray[i+1]);
	}
}

function setstatusunknown() {
	var statuses = "stream speed battery cameratilt motion lag keyboard user dock";
	str = statuses.split(" ");
	var a;
	var i;
	for (i in str) {
		a=document.getElementById(str[i]+"_status");
		a.style.color = "#666666";
	}
}

function hiddenmessageboxShow() {
	document.getElementById("overlaydefault").style.display = "none";
	document.getElementById("login").style.display = "none";
	document.getElementById("someonealreadydrivingbox").style.display = "none";
	document.getElementById("advancedmenubox").style.display = "none";
	document.getElementById("extrastuffcontainer").style.display = "none";
	document.getElementById("hiddenmessageboxcontainer").style.display = "";
	overlay("on");
}

function extrastuffboxShow() {
	document.getElementById("overlaydefault").style.display = "none";
	document.getElementById("login").style.display = "none";
	document.getElementById("someonealreadydrivingbox").style.display = "none";
	document.getElementById("advancedmenubox").style.display = "none";
	document.getElementById("hiddenmessageboxcontainer").style.display = "none";
	document.getElementById("extrastuffcontainer").style.display = "";
	overlay("on");
}

function keyBoardPressed(event) {
	if (enablekeyboard) {
		var keycode = event.keyCode;
		if (keycode == 32 || keycode == 83) { // space bar or S
			move("stop");
		}
		if (keycode == 38 || keycode == 87) { // up arrow or W
			move("forward");
		}
		if (keycode == 40 || keycode == 88) { // down arrow or X
			move("backward");
		}
		if (keycode == 37 || keycode == 81) { // left arrow
			move("left");
		}
		if (keycode == 39 || keycode == 69) { // right arrow
			move("right");
		}
		if (keycode == 65) { // A
			nudge("left");
		}
		if (keycode == 68) { // D
			nudge("right");
		}
		if (keycode == 49) { speedset('slow'); } // 1
		if (keycode == 50) { speedset('med'); } // 2
		if (keycode == 51) { speedset('fast'); } // 3
		if (keycode == 82) { camera('upabit'); } // R
		if (keycode == 70) { camera('horiz'); } // F
		if (keycode == 86) { camera('downabit'); } // V
		if (steeringmode == "forward") { document.getElementById("forward").style.backgroundImage = "none"; }
	}
}

function motionenabletoggle() {
	message("sending: motion enable/disable", sentcmdcolor);
	callServer("motionenabletoggle", "");
	lagtimer = new Date().getTime(); // has to be *after* message()
	overlay("off");
}

function move(str) {
	message("sending command: "+str, sentcmdcolor);
	callServer("move", str);
	lagtimer = new Date().getTime();
}

function nudge(direction) {
	message("sending command: nudge " + direction, sentcmdcolor);
	callServer("nudge", direction);
	lagtimer = new Date().getTime();
}

function slide(direction) {
	message("sending command: slide " + direction, sentcmdcolor);
	callServer("slide", direction);
	lagtimer = new Date().getTime();	
}

function docklinetoggle(str) {
	var a = document.getElementById("dockline");
	var b = document.getElementById("docklineleft");
	var c = document.getElementById("docklineright");
	if (str=="") {
		if (a.style.display == "") { str="off"; }
		else { str="on"; }
	}
	if (str=="off") { 
		document.getElementById('manualdockstart').style.display = "";
		document.getElementById('manualdockgocancel').style.display = "none";
		a.style.display="none"; 
		b.style.display="none";
		c.style.display="none";
	}
	if (str=="on" && streammode != "stop") {
		document.getElementById('manualdockstart').style.display = "none";
		document.getElementById('manualdockgocancel').style.display = "";
		clicksteer("on");
		a.style.display="";
		b.style.display="";
		c.style.display="";
		docklineposition();
	}
}

function docklineposition(n) {
	var i = ctroffset;
	if (n) { i = n; }
	var a = document.getElementById("dockline");
	var b = document.getElementById("video");
	var c = document.getElementById("docklineleft");
	var d = document.getElementById("docklineright");
	var top = b.offsetTop;
	var height = b.offsetHeight;
	var ctr = b.offsetLeft + b.offsetWidth/2;
	a.style.left = (ctr+i) +"px";
	c.style.left = (ctr+i-(docklinewidth/2))+"px";
	d.style.left = (ctr+i+(docklinewidth/2))+"px";
	a.style.top = top + "px";
	a.style.height = height + "px";
	c.style.top = (top + 60) + "px";
	c.style.height = (height/2-40) + "px";
	d.style.top = (top + 60) + "px";
	d.style.height = (height/2-40) + "px";
}

function relaunchgrabber() {
	callServer("relaunchgrabber", "");
	message("sending command: relaunch grabber", sentcmdcolor, 0);
}

function speech() {
	var a = document.getElementById('speechbox');
	var str = a.value;
	a.value = "";
	callServer("speech", str);
	message("sending command: say '" + str + "'", sentcmdcolor, 0);
	lagtimer = new Date().getTime();
}

function advancedmenu() {
	document.getElementById("overlaydefault").style.display = "none";
	document.getElementById("login").style.display = "none";
	document.getElementById("someonealreadydrivingbox").style.display = "none";
	document.getElementById("hiddenmessageboxcontainer").style.display = "none";
	document.getElementById("extrastuffcontainer").style.display = "none";
	document.getElementById("advancedmenubox").style.display = "";
	var a= document.getElementById("videosettingsmenu");
	if (admin) { 
		document.getElementById("admin_menu").style.display = "";
		document.getElementById("regular_menu").style.display = "none";
		document.getElementById("admin_vidmenu").innerHTML = a.innerHTML;
	}
	else { document.getElementById("regular_vidmenu").innerHTML = a.innerHTML; }
	overlay("on");
	streamdetailspopulate();
	resized();
}

function keypress(e) {
	var keynum;
	if (window.event) {
		keynum = e.keyCode;
	}// IE
	else if (e.which) {
		keynum = e.which;
	} // Netscape/Firefox/Opera
	return keynum;
}

function battStats() {
	message("sending command: battStats", sentcmdcolor);
	callServer("battStats", "");
	lagtimer = new Date().getTime();
}

function drivingsettings(state) {
	var a= document.getElementById("extendedsettingsbox");
	a.innerHTML = document.getElementById("drivingsettings").innerHTML;
	a.style.display = "";
	callServer("getdrivingsettings","");
	message("request driving settings values", sentcmdcolor);
	lagtimer = new Date().getTime(); // has to be *after* message()
	resized();
}

function drivingsettingsdisplay(str) { // called by server via flashplayer
	message("driving settings values received", "green");
	splitstr = str.split(" ");
	document.getElementById('slowoffset').value = splitstr[0];
	document.getElementById('medoffset').value = splitstr[1];
	document.getElementById('nudgedelay').value = splitstr[2];
	document.getElementById('maxclicknudgedelay').value = splitstr[3];
	document.getElementById('clicknudgemomentummult').value = splitstr[4];
	document.getElementById('steeringcomp').value = parseInt(splitstr[5]) - 128;
}

function drivingsettingssend() {
	str =  document.getElementById('slowoffset').value + " "
			+ document.getElementById('medoffset').value + " "
			+ document.getElementById('nudgedelay').value + " "
			+ document.getElementById('maxclicknudgedelay').value + " "
			+ document.getElementById('clicknudgemomentummult').value + " "
			+ (parseInt(document.getElementById('steeringcomp').value) + 128);	
	callServer("drivingsettingsupdate", str);
	message("sending driving settings values: " + str, sentcmdcolor);
	lagtimer = new Date().getTime(); // has to be *after* message()
}

function camera(fn) {
	if (!(fn=="stop" && !cameramoving)) {
		callServer("cameracommand", fn);
		message("sending tilt command: " + fn, sentcmdcolor);
		lagtimer = new Date().getTime(); // has to be *after* message()
		if (fn == "up" || fn=="down") { 
			cameramoving = true; 
		}
		else { cameramoving = false; }
	}
}

function tiltcontrols(state) {
	var a= document.getElementById("extendedsettingsbox");
	a.innerHTML = document.getElementById("tiltcontrols").innerHTML;
	a.style.display = "";
	callServer("gettiltsettings", "");
	message("request tilt settings values", sentcmdcolor);
	lagtimer = new Date().getTime(); // has to be *after* message()
	resized();
}

function tiltsettingssend() {
	str = document.getElementById('camhoriz').value + " "
			+ document.getElementById('cammax').value + " "
			+ document.getElementById('cammin').value + " "
			+ document.getElementById('maxclickcam').value;
	callServer("tiltsettingsupdate", str);
	message("sending tilt settings values: " + str, sentcmdcolor);
	lagtimer = new Date().getTime(); // has to be *after* message()
}

function tiltsettingsdisplay(str) {
	message("tilt setings values received", "green");
	splitstr = str.split(" ");
	document.getElementById('camhoriz').value = splitstr[0];
	document.getElementById('cammax').value = splitstr[1];
	document.getElementById('cammin').value = splitstr[2];
	document.getElementById('maxclickcam').value = splitstr[3];
}

function tilttest() {
	var str = document.getElementById('tilttestposition').value;
	callServer("tilttest", str);
	message("sending tilt test: " + str, sentcmdcolor);
	lagtimer = new Date().getTime(); // has to be *after* message()
}

function speedset(str) {
	callServer("speedset", str);
	message("sending speedset: " + str, sentcmdcolor);
	lagtimer = new Date().getTime(); // has to be *after* message()
}

function dock(str) {
	callServer("dock", str);
	message("sending: " + str, sentcmdcolor);
	lagtimer = new Date().getTime(); // has to be *after* message()
	if (steeringmode == "forward") { document.getElementById("forward").style.backgroundImage = "none"; }
}

function autodock(str) {
	if (str=="start" &! autodocking) {
		overlay("off");
		clicksteeron = false;
		document.getElementById("video").style.zIndex = "-1";
		videooverlayposition();
		//var a =document.getElementById("videooverlay");
	    //a.onclick = autodockclick;
	    var b = document.getElementById("docklinecalibratebox")
	    tempdivtext = b.innerHTML;
	    var str = "Dock with charger: <table><tr><td style='height: 7px'></td></tr></table>";
	    str+="Get the dock in view, within 2 meters"
    	str+="<table><tr><td style='height: 11px'></td></tr></table>";
	    
	    str+="<a href='javascript: autodock(&quot;go&quot;);'>"
	    str+= "<span class='cancelbox'>&radic;</span> GO</a> &nbsp; &nbsp;"
	    str+="<a href='javascript: autodock(&quot;cancel&quot;);'>"
	    str+= "<span class='cancelbox'>X</span> CANCEL</a><br>"
	    b.innerHTML = str;
	    b.style.display = "";
	    document.getElementById("docklinecalibrateoverlay").style.display = "";
	    docklinecalibrate('position');
	    setTimeout("docklinecalibrate('position');",10); // rendering fix
	}
	if (str=="cancel") {
		docklinetoggle("off");
	    var b = document.getElementById("docklinecalibratebox")
	    b.innerHTML = tempdivtext;
		b.style.display = "none";
		document.getElementById("docklinecalibrateoverlay").style.display = "none";
		clicksteer("on");
		if (autodocking) {
			callServer("autodock", "cancel");
			message("sending autodock cancel", sentcmdcolor);
			lagtimer = new Date().getTime(); // has to be *after* message()
		}
	}
	if (str=="calibrate") {
		overlay("off");
		clicksteeron = false;
		document.getElementById("video").style.zIndex = "-1";
		videooverlayposition();
		var a =document.getElementById("videooverlay");
	    a.onclick = autodockcalibrate;
	    var b = document.getElementById("docklinecalibratebox")
	    tempdivtext = b.innerHTML;
	    var str = "Auto-dock calibration: <table><tr><td style='height: 7px'></td></tr></table>";
	    str+="Place Oculus square and centered in charging dock, then click within white area of target"
    	str+="<table><tr><td style='height: 11px'></td></tr></table>";
	    str+="<a href='javascript: autodock(&quot;cancel&quot;);'>"
	    str+= "<span class='cancelbox'>X</span> CANCEL</a><br>"
	    b.innerHTML = str;
	    b.style.display = "";
	    document.getElementById("docklinecalibrateoverlay").style.display = "";
	    docklinecalibrate('position');
	    setTimeout("docklinecalibrate('position');",10); // rendering fix
	}
	if (str=="go") {
		callServer("autodock", "go");
		message("sending autodock-go", sentcmdcolor);
		lagtimer = new Date().getTime(); // has to be *after* message()
		autodocking = true;
		var b = document.getElementById("docklinecalibratebox")
	    var str = "Auto Dock: <table><tr><td style='height: 7px'></td></tr></table>";
	    str+="in progress... stand by"
		str+="<table><tr><td style='height: 11px'></td></tr></table>";
	    str+="<a href='javascript: autodock(&quot;cancel&quot;);'>"
	    str+= "<span class='cancelbox'>X</span> CANCEL</a><br>"
	    b.innerHTML = str;
	}
}

function autodockclick(ev) { // TODO: unused if autodock("go") used above, instead
	ev = ev || window.event;
	if (ev.pageX) {
		var x = ev.pageX;
		var y = ev.pageY;
	}
	else {
		var x = ev.clientX + document.body.scrollLeft - document.body.clientLeft;
		var y = ev.clientY + document.body.scrollTop - document.body.clientTop;
	}
	var v = document.getElementById("video");
	x -= v.offsetLeft;
	y -= v.offsetTop;
	//alert(v.offsetLeft);
	
	var b = document.getElementById("docklinecalibratebox")
    var str = "Auto Dock: <table><tr><td style='height: 7px'></td></tr></table>";
    str+="in progress... stand by"
	str+="<table><tr><td style='height: 11px'></td></tr></table>";
    str+="<a href='javascript: autodock(&quot;cancel&quot;);'>"
    str+= "<span class='cancelbox'>X</span> CANCEL</a><br>"
    b.innerHTML = str;
	
	callServer("autodockgo", x+" "+y);
	message("sending autodock-go: " + x+" "+y, sentcmdcolor);
	lagtimer = new Date().getTime(); // has to be *after* message()
	autodocking = true;
}

function autodocklock(str) {
	// clicksteer("on");
	splitstr = str.split(" ");
	//x,y,width,height
	videooverlayposition();
	var scale =2;
	var video = document.getElementById("video");
	var box = document.getElementById("facebox");
	splitstr = str.split(" "); // left top width height
	box.style.width = (splitstr[2]*scale)+"px";
	box.style.height = (splitstr[3]*scale)+"px";
	box.style.left = (video.offsetLeft + (splitstr[0]*scale)) + "px";
	box.style.top = (video.offsetTop + (splitstr[1]*scale)) + "px";
	box.style.display = "";
	setTimeout("document.getElementById('facebox').style.display='none';",500);
}

function autodockcalibrate(ev) {
	ev = ev || window.event;
	if (ev.pageX) {
		var x = ev.pageX;
		var y = ev.pageY;
	}
	else {
		var x = ev.clientX + document.body.scrollLeft - document.body.clientLeft;
		var y = ev.clientY + document.body.scrollTop - document.body.clientTop;
	}
	var video = document.getElementById("video");
	x -= video.offsetLeft;
	y -= video.offsetTop;
	callServer("autodockcalibrate", x+" "+y);
	message("sending autodock-calibrate: " + x+" "+y, sentcmdcolor);
	lagtimer = new Date().getTime(); // has to be *after* message()
	autodock("cancel");
}

function keyboard(str) {
	if (str=="enable") {
		setstatus("keyboard","enabled");
		enablekeyboard = true;
		// changecss(".keycmds", "color", "#777777");
	}
	else { 
		setstatus("keyboard","disabled");
		enablekeyboard = false; 
		// changecss(".keycmds", "color", "#222222");
	}
}

/*
function changecss(theClass, element, value) {
	// Last Updated on June 23, 2009
	// documentation for this script at
	// http://www.shawnolson.net/a/503/altering-css-class-attributes-with-javascript.html
	var cssRules;
	var added = false;
	for ( var S = 0; S < document.styleSheets.length; S++) {

		if (document.styleSheets[S]['rules']) {
			cssRules = 'rules';
		} else if (document.styleSheets[S]['cssRules']) {
			cssRules = 'cssRules';
		} else {
			// no rules found... browser unknown 
		}

		for ( var R = 0; R < document.styleSheets[S][cssRules].length; R++) {
			if (document.styleSheets[S][cssRules][R].selectorText == theClass) {
				if (document.styleSheets[S][cssRules][R].style[element]) {
					document.styleSheets[S][cssRules][R].style[element] = value;
					added = true;
					break;
				}
			}
		}
		if (!added) {
			if (document.styleSheets[S].insertRule) {
				document.styleSheets[S].insertRule(theClass + ' { ' + element
						+ ': ' + value + '; }',
						document.styleSheets[S][cssRules].length);
			} else if (document.styleSheets[S].addRule) {
				document.styleSheets[S].addRule(theClass, element + ': '
						+ value + ';');
			}
		}
	}
}
*/

function crosshairs(state) {
    crosshairsposition(); 
    if (state == "on") { //turn on
    	document.getElementById("crosshair_top").style.display = "";
        document.getElementById("crosshair_right").style.display = "";
        document.getElementById("crosshair_bottom").style.display = "";
        document.getElementById("crosshair_left").style.display = "";
        cspulsate = true;
        crosshairspulsate();
    }
    else { //turn off
    	document.getElementById("crosshair_top").style.display = "none";
        document.getElementById("crosshair_right").style.display = "none";
        document.getElementById("crosshair_bottom").style.display = "none";
        document.getElementById("crosshair_left").style.display = "none";
        cspulsate = false;
        clearTimeout(cspulsatetimer);
    }
}

function crosshairsposition() {
    var hfromctr=cspulsatenum;
    var vfromctr=cspulsatenum;
    var video = document.getElementById('video');
    var videow = video.offsetWidth; // + ctroffset*2;
    var videoh = video.offsetHeight;

    var a=document.getElementById("crosshair_top");
    a.style.left = (videow/2 + video.offsetLeft) + "px";
    a.style.top = (video.offsetTop + videoh/2 - vfromctr - parseInt(a.style.height)) + "px";

    var b=document.getElementById("crosshair_right");
    b.style.left = (videow/2 + hfromctr + video.offsetLeft) + "px";
    b.style.top = (videoh/2 + video.offsetTop) + "px";

    var c=document.getElementById("crosshair_bottom");
    c.style.left = (videow/2 + video.offsetLeft) + "px";
    c.style.top = (videoh/2 + vfromctr + video.offsetTop) + "px";

    var d=document.getElementById("crosshair_left");
	d.style.left = (videow/2 - hfromctr - parseInt(d.style.width) + video.offsetLeft) + "px";
	d.style.top = (videoh/2 + video.offsetTop) + "px";
}

function crosshairspulsate() {
	if (cspulsate == true) {
		cspulsatenum += cspulsatenumdir	;
		if (cspulsatenum >25 ) { cspulsatenum=25; cspulsatenumdir = -1; }
		if (cspulsatenum <20 ) { cspulsatenum=20; cspulsatenumdir = 1; }
		crosshairsposition();
		cspulsatetimer = setTimeout("crosshairspulsate();",100);
	}
}

function systemcall(str,conf) {
	overlay("off");
	if (str=="") {
		var a = document.getElementById('usersyscommand');
		str=a.value;
		a.value = "";
		overlay('off');
		message("sending system command: "+str,sentcmdcolor);
	}
	if (conf=="y") {
		if (confirm("execute:\n'"+str+"'\nare you sure?")) { 
			message("sending system command: "+str,sentcmdcolor);
			callServer("systemcall",str); 
		}
		else { message("sytem command aborted", sentcmdcolor); }
	}
	else { callServer("systemcall",str); }
}

function usersyscommanddivHide() {
	document.getElementById('usersyscommanddiv').style.display='none';
}

function arduinoReset() {
	message("resetting arduino ",sentcmdcolor);
	callServer('arduinoreset');
	overlay('off');
}

function arduinoEcho(value){
	message("firmware command echo" + value, sentcmdcolor);
	
	if(value=='on')	callServer("arduinoecho", "on");
	if(value=='off') callServer("arduinoecho", "off");
	//lagtimer = new Date().getTime(); // has to be *after* message()
	overlay('off');
}

function restart() {
	  message("sending system command: "+str,sentcmdcolor);
	  callServer('restart','');
	  overlay('off');
}

function softwareupdate(command,value) {
	if (command=="check") { 
		callServer("softwareupdate","check");
		message("sending software update request",sentcmdcolor);
		lagtimer = new Date().getTime(); // has to be *after* message()
		overlay('off');
	}
	if (command=="available") {
		if (confirm(value)) {
			message("sending install software download request",sentcmdcolor);
			lagtimer = new Date().getTime(); // has to be *after* message()
			callServer("softwareupdate","download");
		}
		else { message("software update declined",sentcmdcolor); }
	}
	if (command=="downloadcomplete") {
		var str = "Software update download complete.\n";
		str += "Update will take effect on next server restart.\n\n";
		str += "Do you want to restart server now?";
		if (confirm(str)) { restart(); }
	}
	if (command =="version") {
		message("sending software version request",sentcmdcolor);
		lagtimer = new Date().getTime(); // has to be *after* message()
		callServer("softwareupdate","versiononly");
		overlay("off");
	}
}

function clicksteer(str) {
	if (str=="" && !clicksteeron) { str = "on"; }
	if (str == "on") { 
		document.getElementById("video").style.zIndex = "-1";
		clicksteeron = true;
		videooverlayposition();
		var a =document.getElementById("videooverlay");

		a.onmouseover = videoOverlayMouseOver;
	    a.onmouseout = videoOverlayMouseOut;
	    a.onclick = videoOverlayClick;
	    // above event function assign WORKs in ie9. Must be something else, like 0 size window?
	    
	    a.style.cursor="crosshair";
	}
	else {
		document.getElementById("video").style.zIndex = "5";
		clicksteeron = false;
		//str = 'off'
	}
	overlay('off');
	//message("clicksteer "+str,"orange");
}

function videooverlayposition() {
	var a = document.getElementById("videooverlay");
    var video = document.getElementById("video");
    a.style.width = video.offsetWidth + "px";
    a.style.height = video.offsetHeight + "px";
    a.style.left = video.offsetLeft + "px";
    a.style.top = video.offsetTop + "px";
}

function videoOverlayMouseOver() {
	if (videomouseaction && clicksteeron) {
		videooverlayposition();
		videoOverlaySetMousePosGrabber();
		document.getElementById("videocursor_top").style.display="";
		document.getElementById("videocursor_bottom").style.display="";
		document.getElementById("videocursor_left").style.display="";
		document.getElementById("videocursor_right").style.display="";
		crosshairs("on");
		var a=document.getElementById("videocursor_ctr"); // <canvas>
		a.style.display = "";
		try {

			var ctx = a.getContext("2d");
			ctx.strokeStyle = "#34ae2b"; // "#45F239";
			ctx.beginPath();
			ctx.arc(25,25,24,0,Math.PI*2,true);
			ctx.stroke();
		}
		catch(err) { 
			html5=false;
		} // some non html5 browsers
		if (html5==false) {
			a.style.display = "none";
			a=document.getElementById("videocursor_ctr_html4");
			a.style.display = "";

		}
	}
}

function videoOverlaySetMousePosGrabber() {
	document.onmousemove = videoOverlayGetMousePos;
}
	
function videoOverlayGetMousePos(ev) {
	ev = ev || window.event;
	if (ev.pageX || ev.pageY) {
		var x = ev.pageX;
		var y = ev.pageY;
	}
	else {
		var x = ev.clientX + document.body.scrollLeft - document.body.clientLeft;
		var y = ev.clientY + document.body.scrollTop - document.body.clientTop;
	}
//	alert(x+" "+y);
	var b = document.getElementById("videocursor_top");
	b.style.left = x + "px";
    var video = document.getElementById("video");
    b.style.top = video.offsetTop + "px";
    var th = y - video.offsetTop -25; if (th<0) { th=0; } 
    b.style.height = th + "px";
	var c = document.getElementById("videocursor_bottom");
	c.style.left = x + "px";
    c.style.top = (y + 25) + "px";
    var bh = video.offsetTop + video.offsetHeight - y -25; if (bh<0) { bh=0; }
    c.style.height = bh + "px";
    var d = document.getElementById("videocursor_left");
    d.style.left = video.offsetLeft + "px";
    d.style.top = y + "px";
    var lw = x - video.offsetLeft - 25; if (lw<0) { lw=0; }
    d.style.width = lw + "px";
    var e = document.getElementById("videocursor_right");
    e.style.top = y + "px";
    e.style.left = (x +25) + "px";
    var rw = video.offsetLeft + video.offsetWidth - x -25; if (rw<0) { rw=0; }
    e.style.width = rw + "px";
    if (html5) { var f = document.getElementById("videocursor_ctr"); }
    else { var f = document.getElementById("videocursor_ctr_html4"); }
	f.style.display = "";
	f.style.left = (x-25) + "px";
	f.style.top = (y-25) + "px";
	document.onmousemove = null;
	videooverlaymouseposinterval = setTimeout("videoOverlaySetMousePosGrabber();", 20)
}

function videoOverlayClick(ev) {
	if (videomouseaction && clicksteeron) {
		ev = ev || window.event;
		if (ev.pageX || ev.pageY) {
				var x = ev.pageX;
				var y = ev.pageY;
		}
		else {
			var x = ev.clientX + document.body.scrollLeft - document.body.clientLeft;
			var y = ev.clientY + document.body.scrollTop - document.body.clientTop;
		}
		var video = document.getElementById("video");
		var xctroffset = x - (video.offsetLeft + (video.offsetWidth/2)); // converts to 320x240
		var yctroffset = y - (video.offsetTop + (video.offsetHeight/2)); // converts to 320x240
		if (Math.abs(xctroffset) < 30 ) { xctroffset = 0; }
		else { flashvidcursor("videocursor_top"); flashvidcursor("videocursor_bottom"); }
		if (Math.abs(yctroffset) < 15) { yctroffset = 0; }
		else { flashvidcursor("videocursor_left"); flashvidcursor("videocursor_right"); }
		if (!(xctroffset ==0 && yctroffset==0)) {
			var str = xctroffset+" "+yctroffset;
			callServer("clicksteer",str);
			message("sending: clicksteer "+str, sentcmdcolor);
			lagtimer = new Date().getTime(); // has to be *after* message()
		}
	}
}

function videoOverlayMouseOut() {
	if (videomouseaction && clicksteeron) {
		document.onmousemove = null;
		clearTimeout(videooverlaymouseposinterval);
		document.getElementById("videocursor_top").style.display="none";
		document.getElementById("videocursor_bottom").style.display="none";
		document.getElementById("videocursor_left").style.display="none";
		document.getElementById("videocursor_right").style.display="none";
		document.getElementById("videocursor_ctr").style.display="none";
		document.getElementById("videocursor_ctr_html4").style.display="none";
		crosshairs("off");
	}
}

function flashvidcursor(id) {
	var a =document.getElementById(id);
	var clr1 = "#ffffff";
	var clr2 = "#45F239";
	var str1 = "document.getElementById('"+id+"').style.borderColor='"+clr1+"';";
	str1 += "document.getElementById('"+id+"').style.borderWidth='2px';";
	var str2 = "document.getElementById('"+id+"').style.borderColor='"+clr2+"';";
	str2 += "document.getElementById('"+id+"').style.borderWidth='1px';";
	eval(str1);
	setTimeout(str2, 100);
	setTimeout(str1, 200);
	setTimeout(str2, 300);
}

function overlay(str) {
	var a=document.getElementById("overlay");
	var c=document.getElementById("overlaycontents");
	if (str=="on") { a.style.display = ""; c.style.display = ""; resized(); }
	if (str=="off") { a.style.display = "none"; c.style.display = "none"; resized(); }
	
	/*
	var b = document.body;
	if (b.scrollHeight > b.offsetHeight) {
		a.style.height = b.scrollHeight + "px";
		c.style.height = b.scrollHeight + "px";
	}
	else { 
		a.style.height = b.offsetHeight + "px";
		c.style.height = b.offsetHeight + "px";
	}
	*/
}

function createCookie(name,value,days) {
	if (days) {
		var date = new Date();
		date.setTime(date.getTime()+(days*24*60*60*1000));
		var expires = "; expires="+date.toGMTString();
	}
	else var expires = "";
	document.cookie = name+"="+value+expires+"; path=/";
}

function readCookie(name) {
	var nameEQ = name + "=";
	var ca = document.cookie.split(';');
	for(var i=0;i < ca.length;i++) {
		var c = ca[i];
		while (c.charAt(0)==' ') c = c.substring(1,c.length);
		if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
	}
	return null;
}

function eraseCookie(name) {
	createCookie(name,"",-1);
}

function loginfromcookie() {
	var str = ""; 
	str = readCookie("auth");
	getFlashMovie("oculus_player").connect(str);
	logintimer = setTimeout("eraseCookie('auth'); window.location.reload()", logintimeout);
}

function login() {
	document.getElementById("overlaydefault").style.display = "none";
	document.getElementById("login").style.display = "";
	document.getElementById("user").focus();	
}

function loginsend() {
	document.getElementById("overlaydefault").style.display = "";
	document.getElementById("login").style.display = "none";
	var str1= document.getElementById("user").value;
	var str2= document.getElementById("pass").value;
	var str3= document.getElementById("user_remember").checked;
	if (str3 == true) { str3="remember"; }
	else { eraseCookie("auth"); }
	getFlashMovie("oculus_player").connect(str1+" "+str2+" "+str3+" ");
	logintimer = setTimeout("window.location.reload()", logintimeout);
}

function logout() {
	eraseCookie("auth");
	window.location.reload();
}

function someonealreadydriving(value) {
	clearTimeout(logintimer);
	overlay("on");
	document.getElementById("overlaydefault").style.display = "none";
	document.getElementById("someonealreadydrivingbox").style.display = "";
	document.getElementById("usernamealreadydrivingbox").innerHTML = value.toUpperCase();
}

function beapassenger() {
	callServer("beapassenger", username);
	overlay("off");
	setstatus("connection","PASSENGER");
}

function assumecontrol() {
	callServer("assumecontrol", username);
}

function playerexit() {
	callServer("playerexit","");
}

function steeringmouseover(id, str) {
	if (!(id == "forward" && steeringmode == "forward")) {
		document.getElementById(id).style.backgroundImage = "url(images/steering_icon_highlight.gif)";

	}
	document.getElementById("steering_textbox").innerHTML = id.toUpperCase();
	if (str) {
		document.getElementById("steeringkeytextbox").innerHTML = str; 
	}
}

function steeringmouseout(id) {
	if (!(id == "forward" && steeringmode == "forward")) {
		document.getElementById(id).style.backgroundImage = "none";
		if (
			(id == "backward" && steeringmode == "backward") || 
			(id == "rotate right" && steeringmode == "rotate right") ||
			(id == "rotate left" && steeringmode == "rotate left") ||
			(id == "bear right" && steeringmode == "bear right") ||
			(id == "bear left" && steeringmode == "bear left") ||
			(id == "bear right backward" && steeringmode == "bear right backward") ||
			(id == "bear left backward" && steeringmode == "bear leftbackward") 
			) {
			move("stop");
			steeringmode="stop";
		}
	}
	if ( id == "camera up" || id=="camera down") { camera('stop'); }
	document.getElementById("steering_textbox").innerHTML = "";
	document.getElementById("steeringkeytextbox").innerHTML = "";
}

function steeringmousedown(id) {
	if (id != "forward" && id != "nudge left" && id != "nudge right" 
		&& id != "camera up" && id != "camera down" && id != "camera horizontal"
		&& id != "speed slow" && id != "speed medium" && id != "speed fast"
		&& steeringmode == "forward") {
		document.getElementById("forward").style.backgroundImage = "none";
	}
	/*
	if (id == "forward") {
		if (steeringmode == "forward") {
			move("stop");
			steeringmode="stop";
			id = null;
			document.getElementById(id).style.backgroundImage = "url(images/steering_icon_highlight.gif)";
		}
		else { move("forward"); }
	}
	*/
	document.getElementById(id).style.backgroundImage = "url(images/steering_icon_selected.gif)";
	if (id == "forward") { move("forward"); } // comment out if above uncommented
	if (id == "backward") { move("backward"); }
	if (id == "rotate right") { move("right"); }
	if (id == "rotate left") { move("left"); }
	if (id == "slide right") { slide("right"); }
	if (id == "slide left") { slide("left"); }
	if (id == "nudge right") { nudge("right"); id = null; }
	if (id == "nudge left") { nudge("left"); id = null; }
	if (id == "nudge forward") { nudge("forward"); }
	if (id == "nudge backward") { nudge("backward"); }
	if (id == "stop") { move("stop"); }
	if (id == "bear left") { move("bear_left"); }
	if (id == "bear right") { move("bear_right"); }
	if (id == "bear left backward") { move("bear_left_bwd"); }
	if (id == "bear right backward") { move("bear_right_bwd"); }
	if (id == "camera up") { camera("up"); id=null; }
	if (id == "camera down") { camera("down"); id=null; }
	if (id == "camera horizontal") { camera("horiz"); id=null; }
	if (id == "speed slow") { speedset("slow"); id=null; }
	if (id == "speed medium") { speedset("med"); id=null; }
	if (id == "speed fast") { speedset("fast"); id=null; }
	if (id) {
		steeringmode = id;
	}
}

function steeringmouseup(id) {
	if (steeringmode != "forward" || (id == "nudge left" || id == "nudge right"
		|| id=="camera up" || id=="camera down" || id=="camera horizontal"
		|| id=="speed slow" || id=="speed medium" || id=="speed fast" )) {
		document.getElementById(id).style.backgroundImage = "url(images/steering_icon_highlight.gif)";
		if (
			(id == "backward" && steeringmode == "backward") || 
			(id == "rotate right" && steeringmode == "rotate right") ||
			(id == "rotate left" && steeringmode == "rotate left") ||
			(id == "bear right" && steeringmode == "bear right") ||
			(id == "bear left" && steeringmode == "bear left") ||
			(id == "bear right backward" && steeringmode == "bear right backward") ||
			(id == "bear left backward" && steeringmode == "bear left backward") 
			) {
			move("stop");
			steeringmode="stop";
		}
		if ( id == "camera up" || id=="camera down") { camera('stop'); }
	}
}

function videologo(state) {
	// pass "" as state to reposition only
	var i = document.getElementById("videologo");
    var video = document.getElementById("video");
	if (state=="on") { i.style.display = ""; }
	if (state=="off") { i.style.display = "none"; }
    var x = video.offsetLeft + (video.offsetWidth/2) - (i.width/2);
    var y = video.offsetTop + (video.offsetHeight/2) - (i.height/2);
    i.style.left = x + "px";
    i.style.top = y + "px";
}

function docklinecalibrate(str) {
	if (str == "start") {
		overlay("off");
		clicksteeron = false;
		document.getElementById("video").style.zIndex = "-1";
		videooverlayposition();
		var a =document.getElementById("videooverlay");
	    a.onclick = docklineclick; // function() { docklineclick; }
	    ctroffsettemp = ctroffset;
	    document.getElementById("dockline").style.display = "";
	    document.getElementById("docklineleft").style.display = "";
	    document.getElementById("docklineright").style.display = "";
	    docklineposition();
	    document.getElementById("docklinecalibratebox").style.display = "";
	    document.getElementById("docklinecalibrateoverlay").style.display = "";
	    docklinecalibrate('position');
	    setTimeout("docklinecalibrate('position');",10); // rendering fix
	}
	if (str=="position") {
	    var b = document.getElementById("docklinecalibratebox");
	    var c = document.getElementById("docklinecalibrateoverlay");
	    var video = document.getElementById("video");
	    b.style.left = (video.offsetLeft + 20) + "px";
	    b.style.top = (video.offsetTop + 20) + "px";
	    c.style.left = (video.offsetLeft + 10) + "px";
	    c.style.top = (video.offsetTop + 10) + "px";
	    c.style.height = (b.offsetHeight + 20) + "px";
	}
	if (str == "save") {
		docklinetoggle("off");
		ctroffset = ctroffsettemp;
		document.getElementById("docklinecalibratebox").style.display = "none";
		document.getElementById("docklinecalibrateoverlay").style.display = "none";
		clicksteer("on");
		message("sending dockline position: " + ctroffset, sentcmdcolor);
		callServer("docklineposupdate", ctroffset);
		lagtimer = new Date().getTime();
	}
	if (str == "cancel") {
		docklinetoggle("off");
		document.getElementById("docklinecalibratebox").style.display = "none";
		document.getElementById("docklinecalibrateoverlay").style.display = "none";
		clicksteer("on");
	}
}

function docklineclick(ev) {
	ev = ev || window.event;
	if (ev.pageX) {
		var x = ev.pageX;
	}
	else {
		var x = ev.clientX + document.body.scrollLeft - document.body.clientLeft;
	} 
	// var x = ev.clientX + document.body.scrollLeft - document.body.clientLeft;
	var video = document.getElementById("video");
	ctroffsettemp  = x - (video.offsetLeft + (video.offsetWidth/2));
	docklineposition(ctroffsettemp);
}

function account(str) { // change_password, password_update  DONE
	// // change_other_pass, add_user, delete_user, newuser_add, change_username
	if (str == "change_password") {
		var a= document.getElementById("extendedsettingsbox");
		a.innerHTML = document.getElementById("changepassword").innerHTML;
		a.style.display = "";
		document.getElementById('user_name').innerHTML = username;
		resized();
	}
	if (str == "password_update") {
		overlay("off");
		var pass = document.getElementById('userpass').value;
		var passagain = document.getElementById('userpass_again').value;
		if (pass != passagain || pass=="") { message("*error: passwords didn't match, try again", sentcmdcolor); }
		else {
			message("sending new password", sentcmdcolor);
			callServer("password_update", pass);
			lagtimer = new Date().getTime();
			document.getElementById("extendedsettingsbox").style.display = "none";
		}
	}
	if (str=="add_user") {
		var a= document.getElementById("extendedsettingsbox");
		a.innerHTML = document.getElementById("adduser").innerHTML;
		a.style.display = "";
		resized();
	}
	if (str=="newuser_add") {
		overlay("off");
		var user = document.getElementById('newusername').value;
		var pass = document.getElementById('newuserpass').value;
		var passagain = document.getElementById('newuserpass_again').value;
		var oktosend = true;
		var msg = "";
		if (pass != passagain || pass=="") {
			oktosend = false;
			msg += "*error: passwords didn't match, try again "; 
		}
		if (/\s+/.test(user)) { 
			oktosend = false;
			msg += "*error: no spaces allowed in user name "; 
		} 
		if (/\s+/.test(pass)) { 
			oktosend = false;
			msg += "*error: no spaces allowed in password "; 
		}
		if (msg != "") { message(msg, sentcmdcolor); }
		if (oktosend) {
			message("sending new user info", sentcmdcolor);
			callServer("new_user_add", user + " " + pass);
			lagtimer = new Date().getTime();
			document.getElementById("extendedsettingsbox").style.display = "none";
		}
	}	
	if (str=="delete_user") {
		//deleteuser deluserlist 
		var a= document.getElementById("extendedsettingsbox");
		a.innerHTML = document.getElementById("deleteuser").innerHTML;
		a.style.display = "";
		userlistcalledby = "deluser";
		callServer("user_list","");
		message("request user list", sentcmdcolor);
		lagtimer = new Date().getTime();
		resized();
	}
	if (str=="change_extra_pass") {  
		var a= document.getElementById("extendedsettingsbox");
		a.innerHTML = document.getElementById("changeextrapassword").innerHTML;
		a.style.display = "";
		userlistcalledby = "changeextrapass";
		callServer("user_list","");
		message("request user list", sentcmdcolor);
		lagtimer = new Date().getTime();
		resized();
	}
	if (str=="change_username") {
		var a= document.getElementById("extendedsettingsbox");
		a.innerHTML = document.getElementById("changeusername").innerHTML;
		a.style.display = "";
		document.getElementById('user_name2').innerHTML = username;
		resized();
	}
	if (str=="username_update") { //
		var user = document.getElementById('usernamechanged').value;
		var pass = document.getElementById('usernamechangedpass').value;
		if (user!="") {
			message("sending new username", sentcmdcolor);
			callServer("username_update", user+" "+pass);
			lagtimer = new Date().getTime();
			overlay("off");
			document.getElementById("extendedsettingsbox").style.display = "none";
		}
	}
}

function userlistpopulate(list) {
	message("user list received", "green");
	users = list.split(" ");
	if (userlistcalledby == "deluser") {
		userlistcalledby = null;
		var a= document.getElementById("deluserlist");
		if (users.length == 0) { a.innerHTML = "no extra users"; }
		else {
			var str="";
			var i;
			for (i in users) {
				if (users[i] != "") {
					str += "<a class='blackbg' href='javascript: deluserconf(&quot;"+users[i]+"&quot;);'>"
						+users[i]+"</a><br>";
				}
			}
			a.innerHTML = str;
		}
	}
	if (userlistcalledby=="changeextrapass") {
		userlistcalledby=null;
		var a= document.getElementById("changepassuserlist");
		if (users.length == 0) { a.innerHTML = "no extra users"; }
		else {
			var str="";
			var i;
			for (i in users) {
				if (users[i] != "") {
					str += "<a class='blackbg' href='javascript: openbox(&quot;passfield_"
						+users[i]+"&quot;);'>"+users[i]+"</a><br>";
					str += "<div id='passfield_"+users[i]+"' style='padding-left: 5px; display: none; padding-bottom: 7px;'>";
					str += "new password: ";
					str += "<input id='extrauserpass_"+users[i]+"' class='inputbox' type='password' name='password' size='10'"; 
					str += "onfocus='keyboard(&quot;disable&quot;); this.style.backgroundColor=&quot;#000000&quot;'"; 
					str += "onblur='keyboard(&quot;enable&quot;); this.style.backgroundColor=&quot;#262626&quot;'><br/>";
					str += "re-enter new password: ";
					str += "<input id='extrauserpassagain_"+users[i]+"' class='inputbox' type='password' name='password' size='10'"; 
					str += "onfocus='keyboard(&quot;disable&quot;); this.style.backgroundColor=&quot;#000000&quot;'"; 
					str += "onblur='keyboard(&quot;enable&quot;); this.style.backgroundColor=&quot;#262626&quot;'><br/>";
					str += "<a class='blackbg' href='javascript: updateextrapass(&quot;"+users[i]+"&quot;);'>";
					str += "<span class='cancelbox'>&radic;</span> update</a> &nbsp;";
					str += "<a class='blackbg' href='javascript: closebox(&quot;passfield_"+users[i]+"&quot;);'>";
					str += "<span class='cancelbox'>X</span> <span style='font-size: 11px'>CANCEL</span></a>";
					str += "</div>";
				}
			}
			a.innerHTML = str;
		}
	}
	resized();
} 

function deluserconf(str) {
	if (confirm("delete: "+str+"\nare you sure?")) {
		document.getElementById("extendedsettingsbox").style.display = "none";
		overlay("off");
		message("request delete user: "+str, sentcmdcolor);
		callServer("delete_user", str);
		lagtimer = new Date().getTime();
	}
}

function updateextrapass(str) {
	overlay("off");
	var pass = document.getElementById('extrauserpass_'+str).value;
	var passagain = document.getElementById('extrauserpassagain_'+str).value;
	var oktosend = true;
	var msg = "";
	if (pass != passagain || pass=="") {
		oktosend = false;
		msg += "*error: passwords didn't match, try again "; 
	}
	if (/\s+/.test(pass)) { 
		oktosend = false;
		msg += "*error: no spaces allowed in password "; 
	}
	if (msg != "") { message(msg, sentcmdcolor); }
	if (oktosend) {
		message("sending new password", sentcmdcolor);
		callServer("extrauser_password_update", str+" "+pass);
		lagtimer = new Date().getTime();
		document.getElementById("extendedsettingsbox").style.display = "none";
	}
}

function closebox(str) {
	document.getElementById(str).style.display = "none";
}

function openbox(str) {
	document.getElementById(str).style.display = "";
}

function disconnectOtherConnections() {
	message("request eliminate passengers: "+str, sentcmdcolor);
	callServer("disconnectotherconnections", "");
	lagtimer = new Date().getTime();
	overlay("off");
}

function speakchat(command,id) {
	var links = document.getElementById("speakchatlinks");
	var over = document.getElementById(id);
	var under = document.getElementById("popoutbox_under");
	var linksinput = document.getElementById(id+"_input");
	if (command=='show') {
		var xy = findpos(links);
		over.style.display = "";
		over.style.left = xy[0] + "px";
		over.style.top = xy[1] + "px";
		under.style.display = "";
		under.style.left = (xy[0] - 5) + "px";
		under.style.top = (xy[1] -5) + "px";
		under.style.width = (over.offsetWidth + 12) + "px";
		under.style.height = (over.offsetHeight + 10) + "px";
		linksinput.focus();
		keyboard('disable');
	}
	else {
		over.style.display = "none";
		under.style.display = "none";
		keyboard('enable');
	}
}

function findpos(obj) { // derived from http://bytes.com/groups/javascript/148568-css-javascript-find-absolute-position-element
	var left = 0;
	var top = 0;
	var ll = 0;
	var tt = 0;
	while(obj) {
		lb = parseInt(obj.style.borderLeftWidth);
		if (lb > 0) { ll += lb }
		tb = parseInt(obj.style.borderTopWidth);
		if (tb > 0) { tt += tb; }
		left += obj.offsetLeft;
		top += obj.offsetTop;
		obj = obj.offsetParent;
	}
	left += ll;
	top += tt;
	return [left,top];
}

function speech() {
	var a = document.getElementById('speechbox_input');
	var str = a.value;
	a.value = "";
	if (str != "") {
		callServer("speech", str);
		message("sending command: say '" + str + "'", sentcmdcolor);
		lagtimer = new Date().getTime();
	}
}

function chat() {
	var a = document.getElementById('chatbox_input');
	var str = a.value;
	a.value = "";
	if (str != "") {
		callServer("chat", "<i>"+username.toUpperCase()+"</i>: "+str);
		message("sending command: textchat '" + str + "'", sentcmdcolor);
		lagtimer = new Date().getTime();
	}
}

function monitor(str) {
	callServer("monitor",str);
	message("sending monitor: " + str, sentcmdcolor);
	lagtimer = new Date().getTime();
	overlay('off');
}

function serverlog() {
	callServer("showlog","");
	message("request server log",sentcmdcolor);
	lagtimer = new Date().getTime();
}

function showserverlog(str) {
	var a=document.getElementById("extrastuffbox");
	a.style.width = "700px";
	extrastuffboxShow();
	a.innerHTML = str;
}

function camiconbutton(str,id) {
	var a=document.getElementById(id);
	if (str == "over") { a.style.color = "#ffffff"; }
	if (str == "out") { a.style.color = "#4c56fe"; }
	if (str == "click") {
		if (id=="pubstop") { id="stop"; }
		publish(id); 
	}
}

function streamset(str) {
	if (str=="setcustom") {
		var a= document.getElementById("extendedsettingsbox");
		a.innerHTML = document.getElementById("customstreamsettings").innerHTML;
		a.style.display = "";
		document.getElementById('vwidth').value = streamdetails[17];
		document.getElementById('vheight').value = streamdetails[18];
		document.getElementById('vfps').value = streamdetails[19];
		document.getElementById('vquality').value = streamdetails[20];
		resized();
	}
	else if (str=="customupdate") {
		streamdetails[0] = "vcustom";
		streamdetails[17] = document.getElementById('vwidth').value;
		streamdetails[18] = document.getElementById('vheight').value;
		streamdetails[19] = document.getElementById('vfps').value;
		streamdetails[20] = document.getElementById('vquality').value;
		if (parseInt(streamdetails[20]) > 100) { streamdetails[20] = "100"; }
		if (parseInt(streamdetails[20]) < 0) { streamdetails[20] = "0"; }
		if (parseInt(streamdetails[19]) < 1) { streamdetails[19] = "1"; }
		if (parseInt(streamdetails[18]) < 1) { streamdetails[18] = "1"; }
		if (parseInt(streamdetails[17]) < 1) { streamdetails[17] = "1"; }
		var s = streamdetails[17] +"_"+ streamdetails[18] +"_"+ streamdetails[19] +"_"+ streamdetails[20];
		callServer("streamsettingscustom", s);
		message("sending custom stream values: " + s, sentcmdcolor);
		lagtimer = new Date().getTime(); // has to be *after* message()
		// s ="size:"+streamdetails[17]+"x"+streamdetails[18]+" fps:"+streamdetails[19]+" quality:"+streamdetails[20];
		// document.getElementById("custom_specs").innerHTML = s;
		// var modes = "low med high full custom".split(" ");
		// for (i in modes) { document.getElementById(modes[i]+"_bull").style.visibility="hidden"; }
		// document.getElementById("custom_bull").style.visibility="visible";
		document.getElementById("extendedsettingsbox").style.display = "none";
		overlay("off");
	}
	else {
		/* var modes = "low med high full custom".split(" ");
		var i;	
		for (i in modes) { document.getElementById(modes[i]+"_bull").style.visibility="hidden"; }
		document.getElementById(str+"_bull").style.visibility="visible";
		*/
		streamdetails[0] = "v"+str;
		callServer("streamsettingsset", str);
		message("sending stream setting " + str, sentcmdcolor);
		lagtimer = new Date().getTime(); // has to be *after* message()
		document.getElementById("extendedsettingsbox").style.display = "none";
		overlay("off");
	}
}

function streamdetailspopulate() {
	if (streamdetails.length > 0) {
		var s = [];
		s = streamdetails;
		var i = 1;
		var a= document.getElementById("low_specs");
		a.innerHTML = "size:"+s[i]+"x"+s[i+1]+" fps:"+s[i+2]+" quality:"+s[i+3];
		i = 5;
		a= document.getElementById("med_specs");
		a.innerHTML = "size:"+s[i]+"x"+s[i+1]+" fps:"+s[i+2]+" quality:"+s[i+3];
		i = 9;
		a= document.getElementById("high_specs");
		a.innerHTML = "size:"+s[i]+"x"+s[i+1]+" fps:"+s[i+2]+" quality:"+s[i+3];
		i = 13;
		a= document.getElementById("full_specs");
		a.innerHTML = "size:"+s[i]+"x"+s[i+1]+" fps:"+s[i+2]+" quality:"+s[i+3];
		i = 17;
		a= document.getElementById("custom_specs");
		a.innerHTML = "size:"+s[i]+"x"+s[i+1]+" fps:"+s[i+2]+" quality:"+s[i+3];
		a = document.getElementById(streamdetails[0].slice(1)+"_bull");
		a.style.visibility="visible";
		resized();
	}
}

function facegrab() {
	var str;
	if (facegrabon) { 
		str="off"; 
		facegrabon=false; }
	else {
		facegrabon = true;
		str="on"; }
	callServer("facegrab",str);
	overlay("off");
}


function emailgrab() {
	callServer("emailgrab", null);
	overlay("off");
}


function facefound(str) {
	if (clicksteeron) {
		clearTimeout(faceboxtimer);
		videooverlayposition();
		var scale =2;
		var video = document.getElementById("video");
		var box = document.getElementById("facebox");
		splitstr = str.split(" "); // left top width height
		box.style.width = (splitstr[2]*scale)+"px";
		box.style.height = (splitstr[3]*scale)+"px";
		box.style.left = (video.offsetLeft + (splitstr[0]*scale)) + "px";
		box.style.top = (video.offsetTop + (splitstr[1]*scale)) + "px";
		box.style.display = "";
		faceboxtimer = setTimeout("document.getElementById('facebox').style.display='none';",1000);
	}
}

function framegrabbed() {
	document.getElementById("framegrabbox").style.display = "";
	document.getElementById('framegrabpic').src = 'images/framegrab.png'+ '?' + (new Date()).getTime();
}

