
// This code is taken from https://github.com/Uni-Freiburg-Wearable-Devices-WS14/RFDuinoTest

package com.jordanfitzgibbon.rfduinohrm;

import android.bluetooth.BluetoothDevice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

public class BluetoothHelper {
    public static String shortUuidFormat = "0000%04X-0000-1000-8000-00805F9B34FB";

    public static UUID sixteenBitUuid(long shortUuid) {
        assert shortUuid >= 0 && shortUuid <= 0xFFFF;
        return UUID.fromString(String.format(shortUuidFormat, shortUuid & 0xFFFF));
    }

    public static String getDeviceInfoText(BluetoothDevice device, int rssi, byte[] scanRecord) {
        return new StringBuilder()
                .append("Name: ").append(device.getName())
                .append("\nMAC: ").append(device.getAddress())
                .append("\nRSSI: ").append(rssi)
                .append("\nScan Record:").append(parseScanRecord(scanRecord))
                .toString();
    }

    public static Integer bytesToInt(byte[] data) {
        int value = 0;
        if (data.length == 4) {
            value = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt();
        }
        return value;
    }

    public static Float bytesToFloat(byte[] data) {
        Float value = 0f;
        if (data.length == 4) {
            value = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        }
        return value;
    }

    // Assumes a 20 byte char array of ints and converts it to 10 floats
    public static ArrayList<Float> bytesToFloatArray(byte[] data) {

        ArrayList<Float> list = new ArrayList<>(Collections.nCopies(10, 0f));

        if (data.length == 20) {
            for (int i=0; i < 10; i++) {
                byte[] bytes = Arrays.copyOfRange(data, 2*i, 2*i+2);
                int value = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getShort();
                list.set(i, (float)value);
            }
        }

        return list;
    }


    public static String getAdvertisementData(BluetoothDevice device, byte[] scanRecord) {
        StringBuilder output = new StringBuilder();
        int i = 0;
        while (i < scanRecord.length) {
            int len = scanRecord[i++] & 0xFF;
            if (len == 0) break;
            switch (scanRecord[i] & 0xFF) {
               case 0xFF: // Manufacturer Specific data (RFduinoBLE.advertisementData)
                    String ascii = HexAsciiHelper.bytesToAsciiMaybe(scanRecord, i + 3, len);
                    if (ascii != null) {
                        output.append(ascii);
                    }
                    break;
            }
            i += len;
        }
        return output.toString();
    }

    // Bluetooth Spec V4.0 - Vol 3, Part C, section 8
    private static String parseScanRecord(byte[] scanRecord) {
        StringBuilder output = new StringBuilder();
        int i = 0;
        while (i < scanRecord.length) {
            int len = scanRecord[i++] & 0xFF;
            if (len == 0) break;
            switch (scanRecord[i] & 0xFF) {
                // https://www.bluetooth.org/en-us/specification/assigned-numbers/generic-access-profile
                case 0x0A: // Tx Power
                    output.append("\n  Tx Power: ").append(scanRecord[i+1]);
                    break;
                case 0xFF: // Manufacturer Specific data (RFduinoBLE.advertisementData)
                    output.append("\n  Advertisement Data: ")
                            .append(HexAsciiHelper.bytesToHex(scanRecord, i + 3, len));

                    String ascii = HexAsciiHelper.bytesToAsciiMaybe(scanRecord, i + 3, len);
                    if (ascii != null) {
                        output.append(" (\"").append(ascii).append("\")");
                    }
                    break;
            }
            i += len;
        }
        return output.toString();
    }
}
