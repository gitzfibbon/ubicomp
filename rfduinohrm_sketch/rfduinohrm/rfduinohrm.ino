
#include <RFduinoBLE.h>

int pulseSensor = 2;
int value = 0;

void setup() {

  // Configure and start BLE
  RFduinoBLE.advertisementData = "JordanFitzgibbon";
  RFduinoBLE.begin();

  pinMode(pulseSensor, INPUT);
  
  Serial.begin(9600);
  
}

int i=0;
const int numSamples = 1;
int values[1];

void loop() {
  
  RFduino_ULPDelay( SECONDS(1) );
  
  values[i] = analogRead(pulseSensor);
  i++;

  if (i >= numSamples) {
    i = 0;
    
    // Get an average
    double avg = 0;
    for (int j=0; j<numSamples; j++) {
      avg += values[numSamples-1-j];
    }
    avg = avg/numSamples;
    
    Serial.println();
    Serial.println(avg);
    //RFduinoBLE.send(9);
    RFduinoBLE.sendFloat(avg);
  }

}

void RFduinoBLE_onReceive(char *data, int len) {
  // display the first recieved byte
  Serial.println(data[0]);
}

