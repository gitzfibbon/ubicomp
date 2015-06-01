package com.jordanfitzgibbon.rfduinohrm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;


public class MainActivity extends ActionBarActivity implements BluetoothAdapter.LeScanCallback {

    final private String TAG = "HRM_MainActivity";

    private HeartRateMonitor heartRateMonitor;
    private PlotManager plotManager;

    private String samplesText = "no data";

//    // State machine
//    final private static int STATE_BLUETOOTH_OFF = 1;
//    final private static int STATE_DISCONNECTED = 2;
//    final private static int STATE_CONNECTING = 3;
//    final private static int STATE_CONNECTED = 4;

    private int state;

    private boolean scanStarted;
    private boolean scanning;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;

    private RFduinoService rfduinoService;
    private ServiceConnection rfduinoServiceConnection;

    private TextView dataTextView;
    private EditText connectEditText;
    private Button enableBluetoothButton;
    private TextView scanStatusText;
    private Button scanButton;
    private TextView deviceInfoText;
    private TextView connectionStatusText;
    private Button connectButton;
    private Button disconnectButton;
    //private EditData valueEdit;
    private Button sendZeroButton;
    private Button sendValueButton;
    private Button clearButton;
    private LinearLayout dataLayout;

    //private RetainedFragment dataFragment;
    private boolean serviceBound;
    private boolean connectionIsOld = false;
    private boolean fromNotification = false;
    private boolean serviceInForeground = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the plots
        plotManager = new PlotManager(this);
        plotManager.ConfigureRawPlot();
        plotManager.ConfigureFilteredPlot();
        //plotManager.ConfigureFFTPlot(HeartRateMonitor.FFT_SIZE);

        Intent inti = getIntent();
        int flags = inti.getFlags();
        if((inti.getAction().equals("RFduinoTest_CallToMain")) || (serviceInForeground))//&& ((flags & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0))
        {
            Log.w("Main", "Return from notifictation");
            Intent stopForegroundIntent = new Intent(getApplicationContext(), RFduinoService.class);
            stopForegroundIntent.setAction("RFduinoService_StopForeground");
            getApplicationContext().startService(stopForegroundIntent);
            serviceInForeground = false;
            // Saving to sharedPreferences that the service is running in foreground now
            //SharedPreferences.Editor editor = sharedPref.edit();
            //editor.putBoolean("foregroundServiceRunning", serviceInForeground);
            //editor.commit();
            fromNotification = true;
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // rebind to service if it currently isn't
        if(!serviceBound) {
            rfduinoServiceConnection = genServiceConnection();
        }

        if(fromNotification) {
            Intent rfduinoIntent = new Intent(getApplicationContext(), RFduinoService.class);
            getApplicationContext().bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);
        }

        connectEditText = (EditText) findViewById(R.id.editTextConnect);

        scanButton = (Button) findViewById(R.id.buttonScan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                scanStarted = true;
                bluetoothAdapter.startLeScan(
                        new UUID[]{ RFduinoService.UUID_SERVICE },
                        MainActivity.this);

            }
        });


        connectButton = (Button) findViewById(R.id.buttonConnect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //v.setEnabled(false);
                //connectionStatusText.setText("Connecting...");
                // if device was rotated we need to set up a new service connection with this activity
                if (connectionIsOld) {
                    Log.w("Main", "Rebuilding connection after rotation");
                    connectionIsOld = false;
                    rfduinoServiceConnection = genServiceConnection();
                }
                if (serviceBound) {
                    if (rfduinoService.initialize()) {
                        if (rfduinoService.connect(bluetoothDevice.getAddress())) {
                            //upgradeState(STATE_CONNECTING);
                        }
                    }
                } else {
                    Intent rfduinoIntent = new Intent(getApplicationContext(), RFduinoService.class);
                    getApplicationContext().bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);
                }

            }
        });

        // Disconnect Device
        disconnectButton = (Button) findViewById(R.id.buttonDisconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });

        dataTextView = (TextView) findViewById(R.id.textViewData);

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.w("Main","onStart called");
        registerReceiver(scanModeReceiver, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
        registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(rfduinoReceiver, RFduinoService.getIntentFilter());

//        if(state <= STATE_DISCONNECTED) {
//            //updateState(bluetoothAdapter.isEnabled() ? STATE_DISCONNECTED : STATE_BLUETOOTH_OFF);
//        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected  void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onLeScan(BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        bluetoothAdapter.stopLeScan(this);
        scanning = false;

        String deviceName = device.getName();
        final String infoText = BluetoothHelper.getDeviceInfoText(device, rssi, scanRecord);
        String targetDevice = connectEditText.getText().toString().substring(0, Math.min(14, connectEditText.getText().toString().length()));
        if (deviceName.equals(targetDevice)) {
            bluetoothDevice = device;
        }
        else
        {
            Log.d(TAG, "Looking for " + targetDevice + " but found " + deviceName);
        }

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dataTextView.setText(infoText);
            }
        });
    }
    private ServiceConnection genServiceConnection() {
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                serviceBound = true;
                rfduinoService = ((RFduinoService.LocalBinder) service).getService();
                Log.w("Main","onServiceConnected called, service = "+ rfduinoService.toString());
                if(fromNotification) {
                    BTLEBundle bundle = rfduinoService.restoreData();
                    if(bundle != null) {
                        state = bundle.state_;
                        bluetoothDevice = bundle.device;
                        scanStarted = bundle.scanStarted;
                        scanning = bundle.scanning;
                        Log.w("Main","State restored from service, state: "+ state);
                    }
                    Log.w("Main","Stopping service before unbinding");
                    Intent stopIntent = new Intent(getApplicationContext(),RFduinoService.class);
                    getApplicationContext().stopService(stopIntent);
                    fromNotification = false;
//                    if(state<STATE_CONNECTED) {
//                        disconnect();
//                    }
                    //updateUi();
                }
                else{
                    if (rfduinoService.initialize()) {

                        if (rfduinoService.connect(bluetoothDevice.getAddress())) {
                            //upgradeState(STATE_CONNECTING);
                        }
                    }
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.w("Main","onServiceDisconnected called");
                rfduinoService = null;
                //downgradeState(STATE_DISCONNECTED);
            }
        };
    }
    private void disconnect(){
        if(rfduinoService != null) {
            rfduinoService.disconnect();
            rfduinoService = null;
        }
        else {Log.w("Main","Service empty");}
        if(rfduinoServiceConnection != null) {
            getApplicationContext().unbindService(rfduinoServiceConnection);
            serviceBound = false;
        }
        else{ Log.w("Main","ServiceConnection empty");}
    }

    private Float bytesToFloat(byte[] data)
    {
        Float value = -3.0f;
        if (data.length == 4) {
            value = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        }
        return value;
    }


    private final BroadcastReceiver rfduinoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.w("Main","rfduinoReceiver called with " + action);
            if (RFduinoService.ACTION_CONNECTED.equals(action)) {
                //upgradeState(STATE_CONNECTED);
            } else if (RFduinoService.ACTION_DISCONNECTED.equals(action)) {
                //downgradeState(STATE_DISCONNECTED);
            } else if (RFduinoService.ACTION_DATA_AVAILABLE.equals(action)) {
                addData(intent.getByteArrayExtra(RFduinoService.EXTRA_DATA));
            }
        }
    };

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            if (state == BluetoothAdapter.STATE_ON) {
                //upgradeState(STATE_DISCONNECTED);
            } else if (state == BluetoothAdapter.STATE_OFF) {
                //downgradeState(STATE_BLUETOOTH_OFF);
            }
        }
    };

    private final BroadcastReceiver scanModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scanning = (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_NONE);
            scanStarted &= scanning;
            //updateUi();
        }
    };


    @Override
    public void  onNewIntent(Intent intent) {
        Log.w("Main", "onNewintent called");
    }

    private void addData(byte[] data) {

        Float floatValue = bytesToFloat(data);

        // Prepend and trim this display text
        int maxLength = 200;
        if (samplesText.length() >= maxLength) {
            samplesText = floatValue.toString();
        }
        else {
            samplesText = samplesText + "|" + floatValue.toString();
        }

        // Make sure to trim down the text size to maxLength
        samplesText = samplesText.substring(0, Math.min(maxLength, samplesText.length()));

        dataTextView.setText(samplesText);
        this.NewSample(floatValue);
    }

    public int NewSample(Float sample) {

        // Increment the sample count. We use it to calculate current FPS.
        //this.sampleCount++;

        // Initialize HeartRateMonitor class
        if (this.heartRateMonitor == null) {
            this.heartRateMonitor = new HeartRateMonitor(sample);
        }

        // Store the sample
        this.heartRateMonitor.AddNewSample(sample);

        // Update the raw plot with the latest sample
        Float lastSample = this.heartRateMonitor.GetLastSample();
        plotManager.UpdateRawPlot(lastSample);

//        // Plot the de-meaned mean RGB values, median filtered mean RGB values and peaks
//        boolean isPeak = heartRateMonitor.DetectPeak();
//        Scalar deMeanedMean = this.heartRateMonitor.GetLastMeanDeMeaned();
//        Scalar medianFiltered = this.heartRateMonitor.GetLastMedianFiltered();
//        plotManager.UpdateFilteredPlot(
//                deMeanedMean.val[0], deMeanedMean.val[1], deMeanedMean.val[2],
//                medianFiltered.val[0], medianFiltered.val[1], medianFiltered.val[2],
//                isPeak);

//        // Check if the current interval is over
//        long nanoTime = System.nanoTime();
//        if (this.ConvertNanoToMs(nanoTime - lastUpdateTime) >= this.refreshIntervalMs) {
//
//            // Reset the last updated time
//            this.lastUpdateTime = nanoTime;
//
//            // Update the FPS variable with the average of the actual FPS of the current interval and the FPS of the previous interval
//            double previousFPS = this.FPS;
//            this.FPS = (this.sampleCount / (double)(this.refreshIntervalMs / 1000) + previousFPS) / 2;
//            this.sampleCount = 0;
//            Log.d(TAG, "FPS: " + this.FPS);
//
//            // Update the FFT plot
//            int fftWindowInSeconds = 10;
//            float[] fftMags = this.heartRateMonitor.FFT(fftWindowInSeconds, (int)Math.round(this.FPS));
//            this.plotManager.UpdateFFTPlot(fftMags);
//
//            // Get heart rate
//            this.heartRate = heartRateMonitor.GetHeartRate(this.FPS);
//            Log.d(TAG, "Heart Rate: " + heartRate);
//
//            // Update the UI with the heart rate
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    textViewHeartRate.setText("Heart Rate: " + heartRate);
//                }
//            });
//        }

        // Return the newly calculated heart rate
        int heartRate = 13;
        return heartRate;
    }

    private int ConvertNanoToMs(long nanoTime) {
        return (int) nanoTime / 1000000;
    }

}
