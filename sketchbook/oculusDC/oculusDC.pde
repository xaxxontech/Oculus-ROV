// #include <Math.h>
#include <Servo.h>

// pins
// const int analogInPin = 0;  

const int motorA1Pin = 4;    // H-bridge pin 2         LEFT motor
const int motorA2Pin = 2;    // H-bridge pin 7         LEFT motor
const int motorB1Pin = 7;    // H-bridge pin 10        RIGHT motor
const int motorB2Pin = 8;    // H-bridge pin 15        RIGHT motor
const int enablePinA = 3;    // H-bridge enable pin 9  LEFT motor
const int enablePinB = 11;   // H-bridge enable pin 1  RIGHT motor
const int camservopin = 6;  

Servo camservo; // tilt

// motor compensation 
int acomp = 0;
int bcomp = 0;

boolean echo = true;

// how long to wait before sending a rudundant packet 
// const int MIN_DELAY = 7000;

// how much change is enough to be worthy of sending 
// const int A2D_THRESHOLD = 2;

// buffer the command in byte buffer 
const int MAX_BUFFER = 8;
int buffer[MAX_BUFFER];
int commandSize = 0;

// int sensorValue = 0;        
// int outputValue = 0;     
// long start, delta, last = 0; 

void setup() { 
  pinMode(motorA1Pin, OUTPUT); 
  pinMode(motorA2Pin, OUTPUT); 
  pinMode(enablePinA, OUTPUT);
  pinMode(motorB1Pin, OUTPUT); 
  pinMode(motorB2Pin, OUTPUT); 
  pinMode(enablePinB, OUTPUT);
  TCCR2A = _BV(COM2A1) | _BV(COM2B1) | _BV(WGM20); // phase correct (1/2 freq)
  //TCCR2A = _BV(COM2A1) | _BV(COM2B1) | _BV(WGM21) | _BV(WGM20); // 'fast pwm' (1x freq)
  TCCR2B = _BV(CS22) | _BV(CS21) | _BV(CS20); // divide by 1024 
  //TCCR2B = _BV(CS22) | _BV(CS20); // divide by 128 
  //TCCR2B = _BV(CS21) | _BV(CS20); // divide by 8 
  OCR2A = 0;
  OCR2B = 0;

  Serial.begin(115200);
  Serial.println("<reset>");
}

void loop() {
  if( Serial.available() > 0 ){
    // commands take priority 
    manageCommand(); 
  } 

  // if not busy doing a command  
  // interupts();
  // pollSensors();

  // toggle debug led 
  // if(commandSize == 0) digitalWrite(13, LOW);
  // else digitalWrite(13, HIGH);
}

// look at i/o lines for changes in state 
/*
 void pollSensors(){
 
 sensorValue = analogRead(analogInPin);  
 outputValue = map(sensorValue, 0, 1024, 1, 255);  
 
 // new value detected 
 if( abs(last - outputValue) >= A2D_THRESHOLD ){ 
 
 start = millis(); 
 send();
 
 // track last new value
 last = outputValue;
 
 } 
 else if( delta >= MIN_DELAY ){
 
 // clear timer, send old value to keep refreshed on host 
 start = millis();
 send();
 } 
 
 // track time if looping on no changes
 delta = millis() - start;
 }
 */

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

  // always set speed on each move command 
  if((buffer[0] == 'f') || (buffer[0] == 'b') || (buffer[0] == 'l') || (buffer[0] == 'r')){
    OCR2A =  buffer[1] - acomp*( (float) buffer[1] / 254.0);
    OCR2B =  buffer[1] - bcomp*( (float) buffer[1] / 254.0);    
    // Serial.println("<speed " + (String)buffer[1] + ">");
  } 

  if (buffer[0] == 'f') {
    digitalWrite(motorA1Pin, HIGH);   
    digitalWrite(motorA2Pin, LOW);  
    digitalWrite(motorB1Pin, HIGH); 
    digitalWrite(motorB2Pin, LOW);
//    Serial.println("<forward>");  
  }
  else if (buffer[0] == 'b') {
    digitalWrite(motorA1Pin, LOW);  
    digitalWrite(motorA2Pin, HIGH); 
    digitalWrite(motorB1Pin, LOW);  
    digitalWrite(motorB2Pin, HIGH);
 //   Serial.println("<backward>"); 
  }
  else if (buffer[0] == 'r') {
    digitalWrite(motorA1Pin, HIGH);   
    digitalWrite(motorA2Pin, LOW); 
    digitalWrite(motorB1Pin, LOW);  
    digitalWrite(motorB2Pin, HIGH);
   // Serial.println("<right>"); 
  }
  else if (buffer[0] == 'l') {
    digitalWrite(motorA1Pin, LOW);  
    digitalWrite(motorA2Pin, HIGH); 
    digitalWrite(motorB1Pin, HIGH); 
    digitalWrite(motorB2Pin, LOW);
   // Serial.println("<left>");
  } 
  if(buffer[0] == 'x'){
    Serial.println("<id:oculusDC>");
  }   
  else if(buffer[0] == 'y'){
    Serial.println("<version:0.5.1>"); 
  }   
  else if (buffer[0] == 's') {
    OCR2A = 0;
    OCR2B = 0;
    //Serial.println("<stop>");
  }
  else if(buffer[0] == 'v'){
    camservo.attach(camservopin);
    camservo.write(buffer[1]);
   // Serial.println("<camTilt " + (String)buffer[1] + ">");
  }
  else if(buffer[0]== 'w'){
    camservo.detach();
   // Serial.println("<camRelease>");
  }
  else if(buffer[0] == 'c'){
    // 128 = 0, > 128 = acomp, < 128 = bcomp
    if (buffer[1] == 128) {
      acomp = 0;
      bcomp = 0;
    }
    if (buffer[1] > 128) {
      bcomp = 0;
      acomp = (buffer[1]-128)*2;
    }
    if (buffer[1] < 128) {
      acomp = 0;
      bcomp = (128-buffer[1])*2;
    }
    // Serial.println("<setComp " + (String)buffer[1] + ">"); 
  } 
  else if(buffer[0] == 'e'){
    if(buffer[1] == '1')
      echo = true;
    if(buffer[1] == '0')
      echo = false ;
  } 

  // echo the command back 
  // if(echo) { 
  if(buffer[commandSize-1] == 'z'){ 
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

// send back each i/o line or sensor's values on one line 
// void send(){
//  Serial.println("<sonar " + (String) outputValue + ">");  
//
