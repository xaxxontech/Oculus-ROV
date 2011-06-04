#include <Servo.h>

// pins
const int pingPin = 7;       // Range finder  
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

// buffer the command in byte buffer 
const int MAX_BUFFER = 8;
int buffer[MAX_BUFFER];
int commandSize = 0;
boolean echo = false;

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
}

void sonar(){
  // establish variables for duration of the ping, 
  // and the distance result in inches and centimeters:
  long duration, cm;

  // The PING))) is triggered by a HIGH pulse of 2 or more microseconds.
  // Give a short LOW pulse beforehand to ensure a clean HIGH pulse:
  pinMode(pingPin, OUTPUT);
  digitalWrite(pingPin, LOW);
  delayMicroseconds(2);
  digitalWrite(pingPin, HIGH);
  delayMicroseconds(5);
  digitalWrite(pingPin, LOW);

  // The same pin is used to read the signal from the PING))): a HIGH
  // pulse whose duration is the time (in microseconds) from the sending
  // of the ping to the reception of its echo off of an object.
  pinMode(pingPin, INPUT);
  duration = pulseIn(pingPin, HIGH);

  // convert the time into a distance
  cm = duration / 29 / 2;
  Serial.println("<cm " + (String)cm + ">");
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
  } 
  else {

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
    Serial.println("<version:2>"); 
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
  else if(buffer[0] == 'd'){
    sonar();
  } 
  //else {
    // do in echo mode only ?
    //Serial.println("<error>");
  //}

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




