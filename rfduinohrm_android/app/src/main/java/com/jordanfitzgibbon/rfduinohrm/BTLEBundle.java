package com.jordanfitzgibbon.rfduinohrm;

import android.bluetooth.BluetoothDevice;
import android.content.ServiceConnection;

import com.lannbox.rfduinotest.RFduinoService;

public class BTLEBundle
{
    public ServiceConnection connection;
    public RFduinoService service;
    public BluetoothDevice device;
    public int state_;
    public boolean isBound, scanStarted, scanning;

    public BTLEBundle(){
        connection = null;
        service = null;
        device = null;
        state_ = 0;
        isBound = false;
        scanStarted = false;
        scanning = false;
    }

}
