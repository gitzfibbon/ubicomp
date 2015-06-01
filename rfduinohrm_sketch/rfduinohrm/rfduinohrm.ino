
// This code will sample the pulse sensor values at some interval defined below.
// The pulse sensor values are usually in the hundreds so I convert them from float to int without losing much data.
// I collect values in batches of 10 ints at a time and then send them in batches over BLE.
// The batches of ints must be converted to char array and send in 20 byte batches.
// Depending on the setting that are chosen this can be reduced to one transmission every couple of seconds.


#include <RFduinoBLE.h>
#include <math.h>

int pulseSensor = 2;

void setup() {

  Serial.begin(9600);

  pinMode(pulseSensor, INPUT);

  // Configure and start BLE
  RFduinoBLE.deviceName = "jordanfitzgibbon";
  RFduinoBLE.advertisementData = "1234567890";
  RFduinoBLE.begin();  
  
}

int i=0;
const int batchSize = 10;
int values[batchSize];
int delayMs = 50;

void loop() {
    
  RFduino_ULPDelay( MILLISECONDS(delayMs) );

  float sensorValue = analogRead(pulseSensor);
  values[i] = (int)round(sensorValue);
  Serial.println(i + ": " + values[i]);
  
  if (i >= batchSize - 1) {

    // reset i
    i = 0;
    
    // Convert the batch to char array and send it
    char buf[20];
    for (int i = 0; i < batchSize; i++) {
 
      buf[2*i] = (char)highByte(values[i]);
      buf[2*i+1] = (char)lowByte(values[i]);
      
      //for (int j = 0; j < 2; j++){
      //  buf[2*i+j]=(char)(values[i] >> (8*j) & 0xFF);
      //}
    }
    
    // This is for robust sending taken from https://github.com/RFduino/RFduino/blob/master/libraries/RFduinoBLE/examples/BulkDataTransfer/BulkDataTransfer.ino#L84
    while (! RFduinoBLE.send(buf, 20))
      ;  // all tx buffers in use (can't send - try again later


    Serial.println("Sending batch");
    // RFduinoBLE.sendFloat(analogRead(pulseSensor));
  }
  else {
    // Increment i
    i++;  
  }
  

}

void RFduinoBLE_onReceive(char *data, int len) {
  // display the first recieved byte
  Serial.println(data[0]);
}

