#include <Servo.h>

// pins
const int ledPin = 13; 

Servo led; 

int bright = 100;
boolean echo = false;

// buffer the command in byte buffer 
const int MAX_BUFFER = 8;
int buffer[MAX_BUFFER];
int commandSize = 0;

void setup() { 
  led.attach(ledPin);
  led.write(bright);
  Serial.begin(115200);
  Serial.println("<reset>");
}

void loop() {
  if( Serial.available() > 0 ){
    // commands take priority 
    manageCommand(); 
  } 

  // if not busy doing a command  
  // pollSensors();
}

// buffer and/or execute commands from host controller 
void manageCommand(){

  int input = Serial.read();

  // end of command -> exec buffered commands 
  if((input == 13) || (input == 10)){
    if(commandSize > 0){
      parseCommand();
      commandSize = 0; 
    }
  } else {

    // buffer it 
    buffer[commandSize++] = input;

    // protect buffer
    if(commandSize >= MAX_BUFFER){
      commandSize = 0;
      // Serial.println("<overflow>");
    }
  }
}

// do multi byte 
void parseCommand(){

  if (buffer[0] == 'b') {
    
    // max
    if(bright >= 220) return;
    
    bright = bright + 10;
    led.write(bright);
   
    // Serial.println("<bright>");  
  }
  else if (buffer[0] == 'd') {
    
    // min 
    if(bright <= 50) return;
    
    bright = bright + 10;
    led.write(bright);
    
    // Serial.println("<dim>"); 
  }  
  else if (buffer[0] == 's') {
  
    bright = buffer[1];
    led.write(bright);
    
    // Serial.println("<brigth " + (String)buffer[1] + ">");      
  }  
  else if(buffer[0] == 'x'){
    Serial.println("<id:oculusLights>");
  }   
  else if(buffer[0] == 'y'){
    Serial.println("<version:0.1.1>"); 
  }   
  else if(buffer[0] == 'e'){
    if(buffer[1] == '1')
      echo = true;
    if(buffer[1] == '0')
      echo = false ;
  } 

  // echo the command back 
  if(echo) { 
    Serial.print("<");
    Serial.print((char)buffer[0]);

    if(commandSize > 1)
      Serial.print(',');    

    for(int b = 1 ; b < commandSize ; b++){
      Serial.print((String)buffer[b]);  
      if(b<(commandSize-1)) 
        Serial.print(',');    
    } 
    Serial.println(">");
 }
}
