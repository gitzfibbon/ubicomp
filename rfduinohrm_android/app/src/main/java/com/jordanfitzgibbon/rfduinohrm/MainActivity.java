package com.jordanfitzgibbon.rfduinohrm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.UUID;


public class MainActivity extends ActionBarActivity implements BluetoothAdapter.LeScanCallback {

    final private String TAG = "HRM_MainActivity";

    private HeartRateMonitor heartRateMonitor;
    private PlotManager plotManager;

    private int sampleRateHz = 10; // The rate at which the rfduino is sampling

    // For keeping track of time
    long lastUpdateTime;

    // How often to refresh the heart rate
    private final int refreshIntervalMs = 2000;

    // The current HR
    private int heartRate = 0;

    private boolean bleConnected = false;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;

    private RFduinoService rfduinoService;
    private ServiceConnection rfduinoServiceConnection;

    private String samplesText = "no data";
    private TextView dataTextView;
    private TextView connectionTextView;
    private EditText connectEditText;
    private TextView textViewPeakDetectionHr;

    private Button scanButton;
    private Button connectButton;
    private Button disconnectButton;


    private Switch switchSamplingRate;


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
        plotManager.ConfigureFilteredPlot();


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
        dataTextView = (TextView) findViewById(R.id.textViewData);
        connectionTextView = (TextView) findViewById(R.id.textViewConnection);
        textViewPeakDetectionHr = (TextView) findViewById(R.id.textViewPeakDetectionHr);

        switchSamplingRate = (Switch) findViewById(R.id.switchSamplingRate);
        switchSamplingRate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sendSamplingRate(isChecked);
            }

        });

        scanButton = (Button) findViewById(R.id.buttonScan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                connectButton.setEnabled(false);
                connectionTextView.setText("");

//                scanStarted = true;
                bluetoothAdapter.startLeScan(
                        new UUID[]{RFduinoService.UUID_SERVICE},
                        MainActivity.this);

            }
        });


        connectButton = (Button) findViewById(R.id.buttonConnect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
                switchSamplingRate.setEnabled(true);
                switchSamplingRate.setChecked(true);
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

    }

    private void sendSamplingRate(boolean isChecked) {
        if (rfduinoService == null) {
            return;
        }
        if (isChecked == false) {
            // low sampling rate
            rfduinoService.send(new byte[] {(byte)0});
        }
        else {
            // high sampling rate
            rfduinoService.send(new byte[] {(byte)1});
        }
    }

    private void disconnect(){

        if(rfduinoService != null) {
            rfduinoService.disconnect();
            rfduinoService = null;
        }

        if(rfduinoServiceConnection != null) {
            getApplicationContext().unbindService(rfduinoServiceConnection);
            serviceBound = false;
        }


        disconnectButton.setEnabled(false);
        switchSamplingRate.setEnabled(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(rfduinoReceiver, RFduinoService.getIntentFilter());

    }

    @Override
    protected  void onResume() {
        super.onResume();

        // Set up a timer for recalculating heart rate
        this.lastUpdateTime = System.currentTimeMillis();
    }

    @Override
    public void onLeScan(BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        bluetoothAdapter.stopLeScan(this);

        String deviceName = device.getName();
        String infoText = BluetoothHelper.getDeviceInfoText(device, rssi, scanRecord);
        String targetDevice = connectEditText.getText().toString().substring(0, Math.min(14, connectEditText.getText().toString().length()));
        if (deviceName.equals(targetDevice)) {
            bluetoothDevice = device;
            connectButton.setEnabled(true);
        }
        else
        {
            infoText = "Did not find " + targetDevice + ". Only found " + deviceName;
        }

        final String finalInfoText = infoText;
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionTextView.setText(finalInfoText);
            }
        });
    }

    private ServiceConnection genServiceConnection() {
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {

                serviceBound = true;
                rfduinoService = ((RFduinoService.LocalBinder) service).getService();

                    if (rfduinoService.initialize()) {

                        if (rfduinoService.connect(bluetoothDevice.getAddress())) {

                        }
                    }


                bleConnected = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                rfduinoService = null;
            }
        };
    }



    private final BroadcastReceiver rfduinoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

                AddNewData(intent.getByteArrayExtra(RFduinoService.EXTRA_DATA));
        }
    };

    // Handle new data received from the device
    private void AddNewData(byte[] data) {

        if (null == data) {
            return;
        }

        if (data.length == 4) {
            // It's an int - must be the sample interval in ms
            int intValue = BluetoothHelper.bytesToInt(data);
            this.sampleRateHz = 1000 / intValue;
        }

        // This is the standard case
        ArrayList<Float> values = BluetoothHelper.bytesToFloatArray(data);

        for (int i = 0; i < values.size(); i++) {

            Float floatValue = values.get(i);

            // Append and trim this display text
            samplesText = samplesText + "|" + floatValue.toString();
            dataTextView.setText(samplesText);

            // Add this new data sample to hr calculation
            this.AddNewSample(floatValue);
        }

        samplesText = samplesText + "\n";
        int maxLength = 1500;
        if (samplesText.length() >= maxLength) {
            samplesText = "";
        }
    }

    // Update HR calculation with this new sample
    public int AddNewSample(Float sample) {

        // Initialize HeartRateMonitor class
        if (this.heartRateMonitor == null) {
            this.heartRateMonitor = new HeartRateMonitor(sample);
        }

        // Store the sample
        this.heartRateMonitor.AddNewSample(sample);

        // Plot the de-meaned mean RGB values, median filtered mean RGB values and peaks
        boolean isPeak = this.heartRateMonitor.GetSecondFromLastPeak();
        Float deMeanedSample = this.heartRateMonitor.GetLastDeMeanedSample();
        plotManager.UpdateFilteredPlot(deMeanedSample, isPeak);

        // Check if the current interval is over
        long millisTime = System.currentTimeMillis();
        long timeDiff = millisTime - lastUpdateTime;
        if (timeDiff >= this.refreshIntervalMs) {

            // Reset the last updated time
            this.lastUpdateTime = System.currentTimeMillis();

            // Get heart rate using a window of this many seconds
            int useWindowInSeconds = 6;

            this.heartRate = heartRateMonitor.GetHeartRate(useWindowInSeconds, this.sampleRateHz);
            Log.d(TAG, "Heart Rate: " + heartRate);

            // Update the UI with the heart rate
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (heartRate < 40 || heartRate > 250) {
                        textViewPeakDetectionHr.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
                    }
                    else {
                        textViewPeakDetectionHr.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 60);
                    }
                    textViewPeakDetectionHr.setText(Integer.toString(heartRate));
                }
            });
        }

        // Return the newly calculated heart rate
        return heartRate;
    }
}
