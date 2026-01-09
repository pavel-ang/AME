package edu.urv.mobileembeded.bluetooth;

import android.bluetooth.BluetoothDevice;

public interface BluetoothListener {
    void onDeviceDiscovered(BluetoothDevice device);
    void onDataReceived(String data);
    void onDeviceConnected(String deviceName);
    void onDeviceDisconnected(String deviceName);
    void onError(String message);
}
