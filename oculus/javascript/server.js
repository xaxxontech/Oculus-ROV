var maxlogbuffer = 10000;
var stream = "stop";
var reloadinterval = 600000; // 10 min
var initialize = false;

function server_loaded() {
}

function initialize_loaded() {
	// window.open('server.html','_self'); // bypass init
	initialize = true;
}

function flashloaded() {
	if (!initialize) { setTimeout("reload();", reloadinterval); }
}

function reload() {
	if (stream == "stop") {
		message("refreshing page", null);
		window.location.reload();
	}
	else { setTimeout("reload();", 10000); } //10 sec, keep checking until stream stopped by user
}

function message(message,status) {
	if (/^populatevalues/.test(message)) { populatevalues(message); message=""; }
	if (/^<CHAT>/.test(message)) {
		message = "<span style='font-size: 20px'>"+message.slice(6)+"</span>";
	}
	
	if (message != "") {
		var a = document.getElementById("messagebox");
		var str = a.innerHTML;
		if (str.length > maxlogbuffer) {
			str = str.slice(0, maxlogbuffer);
		}
		var datetime="";
		if (!initialize) {
			var d = new Date();
			datetime += "<span style='font-size: 11px; color: #666666;'>";
			datetime += d.getDate()+"-"+(d.getMonth()+1)+"-"+d.getFullYear();
			datetime += " "+d.getHours()+":"+d.getMinutes()+":"+d.getSeconds();
			datetime +="</span>";
		}
		a.innerHTML = "&bull; "+message+" " + datetime + "<br/>" + str;
	}
	
	if (/^connected/.test(message)) { // some things work better if down here -wtf???
		init(); 
	}
	if (message=="launch server") { window.open('server.html','_self'); }
	if (/^streaming/.test(message)) { 
		var b = message.split(" ");
		stream = b[1]; 
	}
	if (message=="shutdown") { shutdownwindow(); } 
	if (status != null && !initialize) { setstatus(status); } 
}

function setstatus(status) {
	var s=status.split(" ");
	var a;
	for (var n=0; n<s.length; n=n+2) {
		if (a= document.getElementById(s[n]+"_status")) {
			a.innerHTML = s[n+1];
		}
	}
}

function getFlashMovie(movieName) {
	var isIE = navigator.appName.indexOf("Microsoft") != -1;
	return (isIE) ? window[movieName] : document[movieName];
}

function callServer(fn, str) {
	getFlashMovie("oculus_grabber").flashCallServer(fn,str);
} 

function saveandlaunch() {
	str = "";
	var s;
	var oktosend = true;
	var msg = "";
	//user password
	var user = document.getElementById("newusername").value;
	if (user != "") {
		var pass = document.getElementById("userpass").value;
		var passagain = document.getElementById("userpassagain").value;
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
		str += "user " + user + " password " + pass + " ";  
	}
	//battery
//	if (document.getElementById("battery").checked) { str += "battery yes "; }
//	else { str += "battery no "; }

	//httpport
	s = document.getElementById("httpport").value;
	if (s=="") { 
		message += "http port is blank ";
		oktosend = false; 
	}
	else { str += "httpport "+s+" "; }
	
	//rtmpport
	s = document.getElementById("rtmpport").value;
	if (s=="") { 
		message += "rtmp port is blank ";
		oktosend = false; 
	}
	else { str += "rtmpport "+s+" "; }	
	
	//skipsetup
	if (document.getElementById("skipsetup").checked) {str += "skipsetup yes "; }
	else { str += "skipsetup no "; }
	
	if (msg != "") { message(msg); }
	if (oktosend) {
		message("submitting info",null);
		callServer("saveandlaunch",str);
	}
}

function init() {
	if (initialize) {
		getFlashMovie("oculus_grabber").playlocal();
		callServer("populatesettings","");
	}
	else { callServer("autodock","getdocktarget"); };
}

function populatevalues(values) {
	splitstr = values.split(" ");
	var username = false
	for (var n=1; n<splitstr.length; n=n+2) { // username battery comport httpport rtmpport
		if (splitstr[n] == "username") {  
			username = true;
			document.getElementById("username").innerHTML = "<b>"+splitstr[n+1]+"</b>"; 
		}
		if (splitstr[n]== "battery") {
			var a=document.getElementById("battery");
			var str = splitstr[n+1];
			if (str == "nil") { a.innerHTML="not found"; }
			else { a.innerHTML = "present"; }
		}
		if (splitstr[n]=="comport") {
			a = document.getElementById("comport");
			var str = splitstr[n+1];
			if (str == "nil") { a.innerHTML="not found"; }
			else { a.innerHTML = "found on "+str; }
		}
		if (splitstr[n]=="lightport") {
			a = document.getElementById("lightport");
			var str = splitstr[n+1];
			if (str == "nil") { a.innerHTML="not found"; }
			else { a.innerHTML = "found on "+str; }
		}
		if (splitstr[n]=="httpport") { document.getElementById("httpport").value = splitstr[n+1]; }
		if (splitstr[n]=="rtmpport") { document.getElementById("rtmpport").value = splitstr[n+1]; }
	}
	if (!username) { 
		document.getElementById("username").innerHTML = "<b>"+splitstr[n+1]+"</b>"; 
		userbox("show");
	}
}

function userbox(state) {
	if (state=="show") {
		document.getElementById('changeuserbox').style.display='';
		document.getElementById('changeuserboxlink').style.display='none';
	}
	else {
		document.getElementById('changeuserbox').style.display='none';
		document.getElementById('changeuserboxlink').style.display='';
	}
}

function ifnotshow() {
	document.getElementById('ifnot').style.display='';
}

function quit() {
	callServer('systemcall','red5-shutdown.bat');
	message("shutdown",null);
}

function restart() {
	message("shutdown",null);
	callServer('restart','')
}

function shutdownwindow() {
	window.open('about:blank','_self');
}

function chat() {
	var a = document.getElementById('chatbox_input');
	var str = a.value;
	a.value = "";
	if (str != "") {
		callServer("chat", "<i>OCULUS</i>: "+str);
	}
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
