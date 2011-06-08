var ctroffset = 0; //295
var connected = false;
var logintimeout = 10000; 
var logintimer; // timer
var username;
var streammode = "stop";
var steeringmode;

function loaded() {

}

function resized() {
	
}

function flashloaded() {
	//getFlashMovie("oculus_player").connect("colin asdfds"); //loginsend
	var user = window.OCULUSANDROID.getUser();
	var pass = window.OCULUSANDROID.getPass();
	loginsend(user,pass);
}

function callServer(fn, str) {
	getFlashMovie("oculus_android").flashCallServer(fn,str);
}

function play(str) {
	streammode = str;
	var num = 1;
	if (streammode == "stop") { num =0 ; } 
	getFlashMovie("oculus_android").flashplay(num);
}

function getFlashMovie(movieName) {
	var isIE = navigator.appName.indexOf("Microsoft") != -1;
	return (isIE) ? window[movieName] : document[movieName];
}

function publish(str) {
	callServer("publish", str);
}

function message(message, colour, status, value) {
	if (status != null) {  //(!/\S/.test(d.value))
		if (status == "multiple") { setstatusmultiple(value); }
		else { setstatus(status, value); }
	}
	
	/*
	var msgwin = document.getElementById("messagewindow");
	var str = msgwin.innerHTML;
	msgwin.innerHTML = message + " " + status + " " + value + "<br>"+str;
	*/
}

function setstatus(status, value) {
	if (status=="vidctroffset") { ctroffset = parseInt(value); }
	//if (status=="motion" && value=="disabled") { motionenabled = false; }
	if (value.toUpperCase() == "CONNECTED" && !connected) { // initialize
		// countdowntostatuscheck(); 
		connected = true;
		publish("camera");
	}
	if (status.toUpperCase() == "STORECOOKIE") {
		createCookie("auth",value,30); 
	}
	if (status == "someonealreadydriving") { someonealreadydriving(value); }
	if (status == "user") { username = value; }
	if (status == "hijacked") { window.location.reload(); }
	if (status == "stream" && (value.toUpperCase() != streammode.toUpperCase())) { play(value); }
	if (status == "address") { document.title = "Cyclops "+value; }
}

function setstatusmultiple(value) {
	var statusarray = new Array();
	statusarray = value.split(" ");
	for (var i = 0; i<statusarray.length; i+=2) {
		setstatus(statusarray[i], statusarray[i+1]);
	}
}

function loginsend(str1, str2) {
	/*
	var str1= document.getElementById("user").value;
	var str2= document.getElementById("pass").value;
	var str3= document.getElementById("user_remember").checked;
	if (str3 == true) { str3="remember"; }
	else { eraseCookie("auth"); }
	*/
	getFlashMovie("oculus_android").connect(str1+" "+str2+" false");
	// logintimer = setTimeout("window.location.reload()", logintimeout);
}

function logout() {
	window.location.reload();
}

function motionenabletoggle() {
	callServer("motionenabletoggle", "");
}

function move(str) {
	callServer("move", str);
}

function nudge(direction) {
	callServer("nudge", direction);
}

function slide(direction) {
	callServer("slide", direction);
}



function speech() {
	var a = document.getElementById('speechbox');
	var str = a.value;
	a.value = "";
	callServer("speech", str);
}


function camera(fn) {
	callServer("cameracommand", fn);
}

function speedset(str) {
	callServer("speedset", str);
}

function dock(str) {
	callServer("dock", str);
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

function steeringmousedown(id) {
	document.getElementById(id).style.backgroundColor = "#45F239";
	setTimeout("document.getElementById('"+id+"').style.backgroundColor='transparent';",200);
	/*
	if (steeringmode == id) {
		move("stop");
		steeringmode="stop";
		id = null;
	}
	*/
	if (id == "forward") { move("forward"); }
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
	if (id == "camera up") { camera("upabit"); id=null; }
	if (id == "camera down") { camera("downabit"); id=null; }
	if (id == "camera horizontal") { camera("horiz"); id=null; }
	if (id == "speed slow") { speedset("slow"); id=null; }
	if (id == "speed medium") { speedset("med"); id=null; }
	if (id == "speed fast") { speedset("fast"); id=null; }
	if (id == "menu") { move("stop"); menu(); id=null; }
	if (id) {
		steeringmode = id;
	}
}

function menu() {
	overlay("on");
	document.getElementById("overlaydefault").style.display="none";   
	document.getElementById("login").style.display="none";
	document.getElementById("someonealreadydrivingbox").style.display="none";
	document.getElementById("menubox").style.display="";
	
}
