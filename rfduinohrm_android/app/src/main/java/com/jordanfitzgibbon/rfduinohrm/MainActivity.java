package com.jordanfitzgibbon.rfduinohrm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

//import com.lannbox.rfduinotest.BTLEBundle;
//import com.lannbox.rfduinotest.EditData;
//import com.lannbox.rfduinotest.RetainedFragment;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class MainActivity extends ActionBarActivity implements BluetoothAdapter.LeScanCallback {

    final private String TAG = "HRM";

    // State machine
    final private static int STATE_BLUETOOTH_OFF = 1;
    final private static int STATE_DISCONNECTED = 2;
    final private static int STATE_CONNECTING = 3;
    final private static int STATE_CONNECTED = 4;

    private int state;

    private boolean scanStarted;
    private boolean scanning;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;

    private RFduinoService rfduinoService;
    private ServiceConnection rfduinoServiceConnection;

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

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Device Info
        // deviceInfoText = (TextView) findViewById(R.id.deviceInfo);

        // Connect Device
        // connectionStatusText = (TextView) findViewById(R.id.connectionStatus);

        //connectButton = (Button) findViewById(R.id.connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent rfduinoIntent = new Intent(getApplicationContext(), RFduinoService.class);
                getApplicationContext().bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);
            }
        });

        // Disconnect Device
        //disconnectButton = (Button) findViewById(R.id.disconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });

        //dataLayout = (LinearLayout) findViewById(R.id.dataLayout);

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
        bluetoothDevice = device;
        scanning = false;

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceInfoText.setText(
                        BluetoothHelper.getDeviceInfoText(bluetoothDevice, rssi, scanRecord));
                //updateUi();
            }
        });
    }

    private ServiceConnection genServiceConnection() {
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                serviceBound = true;
                rfduinoService = ((RFduinoService.LocalBinder) service).getService();
                Log.w("Main", "onServiceConnected called, service = " + rfduinoService.toString());
                if (fromNotification) {
                    BTLEBundle bundle = rfduinoService.restoreData();
                    if (bundle != null) {
                        state = bundle.state_;
                        bluetoothDevice = bundle.device;
                        scanStarted = bundle.scanStarted;
                        scanning = bundle.scanning;
                        Log.w("Main", "State restored from service, state: " + state);
                    }
                    Log.w("Main", "Stopping service before unbinding");
                    Intent stopIntent = new Intent(getApplicationContext(), RFduinoService.class);
                    getApplicationContext().stopService(stopIntent);
                    fromNotification = false;
                    if (state < STATE_CONNECTED) {
                        disconnect();
                    }
                    //updateUi();
                } else {
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

    private String bytesToFloat(byte[] data)
    {
        Float value = -3.0f;
        if (data.length == 4) {
            value = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        }
        return value.toString();
    }


}
