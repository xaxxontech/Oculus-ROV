/*
COMMANDS: 
 1 = forward both wheels, ready for next command (speed) 
 2 = backward both wheels, ready for next command (speed)
 3 = stop, disable wheels
 4 = attach camservo, ready for next command (cam tilt degrees)
 5 = turn right (right wheel reverse, left wheel forward), ready for next command (speed)
 6 = turn left (right wheel forward, left wheel reverse), ready for next command (speed)
 8 = release camservo
 120 ('x' ascii)  = send back product id number 
 121 ('y' asccii) = send back version number 
 NOTE: fwd/bkwd combined commands like above don't allow for drift comp
 */

#include <Servo.h> 

const int forward = 1;
const int backward = 2;
// stop a reserved word?
const int stop = 3;
const int camtilt = 4;
const int right = 5;
const int left = 6;
const int releasecam = 8;

const int motorA1Pin = 4;    // H-bridge pin 2    LEFT motor
const int motorA2Pin = 2;    // H-bridge pin 7     LEFT motor
const int motorB1Pin = 7;    // H-bridge pin 10    RIGHT motor
const int motorB2Pin = 8;    // H-bridge pin 15     RIGHT motor
const int enablePinA = 3;    // H-bridge enable pin 9  LEFT motor
const int enablePinB = 11;    // H-bridge enable pin 1  RIGHT motor
const int camservopin = 6;  // pins 9,10,11 make weird things happen on start...

Servo camservo; // tilt
boolean waitingForCommand = true; // false=waiting for follow up to previous command 
int lastcommand = 0;
//int lastcommandtime;

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
  //lastcommandtime == millis();
}    

void sendID(){
 Serial.println("oculusDC");
 Serial.write(13);
}

void sendVersion(){
 Serial.println("v0.1");
 Serial.write(13);
}

void loop() { 
  if (Serial.available() > 0 ) {
    int i = Serial.read();

    // echo Serial.write(i);

    //int time = millis();
    //if ((time - lastcommandtime) > 10) { waitingForCommand = true; }
    if (waitingForCommand == true) {
      if (i==forward || i==backward || i==right || i==left || i==camtilt) {
        lastcommand = i;
        waitingForCommand = false;
        //lastcommandtime = millis();
      }
      else if (i==stop) {
        //digitalWrite(enablePinA, LOW); 
        //digitalWrite(enablePinB, LOW);
        OCR2A = 0;
        OCR2B = 0;
        //analogWrite(enablePinA, 0);
        //analogWrite(enablePinB, 0);
      }
      else if (i==releasecam) {
        camservo.detach();
      }

      // ascii 'x'
      else if(i == 120){
        sendID();
      }

      // ascii 'y'
      else if(i == 121){
        sendVersion();
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
        /*
				if (i==255) {
         					digitalWrite(enablePinA, HIGH);
         					digitalWrite(enablePinB, HIGH);
         				}
         				else {
         					analogWrite(enablePinA, i); 
         					analogWrite(enablePinB, i);
         				}
         				*/
        OCR2A = i;
        OCR2B = i;
      }
      if (lastcommand == camtilt) {
        // why attach here? save power by turning it off later? 
        camservo.attach(camservopin);
        camservo.write(i);	
      }
    }
  }
}







