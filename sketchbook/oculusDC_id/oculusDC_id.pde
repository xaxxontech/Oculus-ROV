#include <Servo.h> 

// commands
const int forward = 1;  // forward both wheels, ready for next command (speed) 
const int backward = 2;  // backward both wheels, ready for next command (speed) 
const int stop = 3;  // stop, disable wheels
const int camtilt = 4;  // attach camservo, ready for next command (cam tilt degrees)
const int right = 5;  // turn right (right wheel reverse, left wheel forward), ready for next command (speed)
const int left = 6;  // turn left (right wheel forward, left wheel reverse), ready for next command (speed)
const int releasecam = 8;  // release camservo
const int setComp = 9;
const int sendID = 120;  // ('x' ascii)  = send back product id number
const int sendVersion = 121;  // ('y' asccii) = send back version number

// pins
const int motorA1Pin = 4;    // H-bridge pin 2    LEFT motor
const int motorA2Pin = 2;    // H-bridge pin 7     LEFT motor
const int motorB1Pin = 7;    // H-bridge pin 10    RIGHT motor
const int motorB2Pin = 8;    // H-bridge pin 15     RIGHT motor
const int enablePinA = 3;    // H-bridge enable pin 9  LEFT motor
const int enablePinB = 11;    // H-bridge enable pin 1  RIGHT motor
const int camservopin = 6;  

Servo camservo; // tilt
boolean waitingForCommand = true; // false = waiting for follow up to previous command 
int lastcommand = 0;
int acomp = 0;
int bcomp = 0;

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
  Serial.begin(19200);
}    

void loop() { 
  if (Serial.available() > 0 ) {
    int i = Serial.read();
    if (waitingForCommand == true) {
      if (i==forward || i==backward || i==right || i==left || i==camtilt || i==setComp) {
        lastcommand = i;
        waitingForCommand = false;
      }
      else if (i==stop) {
        OCR2A = 0;
        OCR2B = 0;
      }
      else if (i==releasecam) {
        camservo.detach();
      }
      else if(i == sendID){
				Serial.println("oculusDC");
      }

      else if(i == sendVersion){
				Serial.println("v0.2");
      }
    }
    else {
      waitingForCommand = true;
      if (lastcommand == forward) {
        digitalWrite(motorA1Pin, HIGH);   
        digitalWrite(motorA2Pin, LOW);  
        digitalWrite(motorB1Pin, HIGH); 
        digitalWrite(motorB2Pin, LOW);  
      }
      if (lastcommand == backward) {
        digitalWrite(motorA1Pin, LOW);  
        digitalWrite(motorA2Pin, HIGH); 
        digitalWrite(motorB1Pin, LOW);  
        digitalWrite(motorB2Pin, HIGH); 
      }
      if (lastcommand == right) {
        digitalWrite(motorA1Pin, HIGH);   
        digitalWrite(motorA2Pin, LOW); 
        digitalWrite(motorB1Pin, LOW);  
        digitalWrite(motorB2Pin, HIGH); 
      }
      if (lastcommand == left) {
        digitalWrite(motorA1Pin, LOW);  
        digitalWrite(motorA2Pin, HIGH); 
        digitalWrite(motorB1Pin, HIGH); 
        digitalWrite(motorB2Pin, LOW);
      }
      if (lastcommand == forward || lastcommand == backward || lastcommand == right || lastcommand == left) {
				if (i == 255) { // comp only applies to full speed
					OCR2A = i - acomp;
					OCR2B = i - bcomp;
				}
				else {
					OCR2A = i;
					OCR2B = i;
				}
      }
      if (lastcommand == camtilt) {
        camservo.attach(camservopin);
        camservo.write(i);	
      }
			if (lastcommand == setComp) {
				// 128 = 0, > 128 = acomp, < 128 = bcomp
				if (i == 128) {
					acomp = 0;
					bcomp = 0;
				}
				if (i > 128) {
					bcomp = 0;
					acomp = (i-128)*2;
				}
				if (i < 128) {
					acomp = 0;
					bcomp = (128-i)*2;
				}
			}
    }
  }
}

