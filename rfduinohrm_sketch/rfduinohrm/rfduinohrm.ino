
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


  RFduinoBLE.advertisementInterval = 1000;

}

int i=0;
const int batchSize = 10;
int values[batchSize];

const int highSampleRateInMs = 100;
const int lowSampleRateInMs = 200;
int samplingDelayMs = highSampleRateInMs;
boolean sendSamplingRate = false;

boolean isConnected = false;

void loop() {
  
  if (isConnected == false) {
    
    // Sleep for a bit
    int sleepTime = 1;
    Serial.print("Sleeping for "); Serial.print(sleepTime); Serial.println(" seconds");
    RFduino_ULPDelay(SECONDS(sleepTime));
  }
  else {

    if (sendSamplingRate == true) {
      Serial.print("Sampling interval in ms is "); Serial.println(samplingDelayMs);
      RFduinoBLE.sendInt(samplingDelayMs);
      sendSamplingRate = false;
    }
    
    // Sample at the specified sampling rate
    
    RFduino_ULPDelay( MILLISECONDS(samplingDelayMs) );
  
    float sensorValue = analogRead(pulseSensor);
    values[i] = (int)round(sensorValue);
    Serial.print(i); Serial.print(": "); Serial.println(values[i]);
    
    if (i >= batchSize - 1) {
  
      // reset i
      i = 0;
      
      // Convert the batch to char array and send it
      char buf[20];
      for (int i = 0; i < batchSize; i++) {
   
        buf[2*i] = (char)highByte(values[i]);
        buf[2*i+1] = (char)lowByte(values[i]);
      }
      
      // This is for robust sending taken from https://github.com/RFduino/RFduino/blob/master/libraries/RFduinoBLE/examples/BulkDataTransfer/BulkDataTransfer.ino#L84
      Serial.println("Sending a batch");
      while (! RFduinoBLE.send(buf, 20))
        ;  // all tx buffers in use (can't send - try again later
  
    }
    else {
      // Increment i
      i++;  
    }
    
  }

}

void RFduinoBLE_onReceive(char *data, int len) {
  if (data != NULL && data[0] == 1) {
    Serial.println("Received request to sample at a high rate");
    samplingDelayMs = highSampleRateInMs;
    sendSamplingRate = true;
  }
  else if (data != NULL && data[0] == 0) {
    Serial.println("Received request to sample at a low rate");
    samplingDelayMs = lowSampleRateInMs;
    sendSamplingRate = true;
  }
  else if (data != NULL) {
    Serial.println(data[0]);
  }

}

void RFduinoBLE_onConnect() {
  // Re-initialize a bunch of variables
  isConnected = true;
  i=0;
  samplingDelayMs = highSampleRateInMs;
  sendSamplingRate = true;
  Serial.println("Connected!");
}

void RFduinoBLE_onDisconnect() {
  isConnected = false;
  Serial.println("Disconnected :(");
  
}


