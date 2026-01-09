package edu.urv.mobileembeded.bluetooth;

public interface BluetoothListener {
    void onDataReceived(String data);
    void onDeviceConnected(String deviceName);
    void onError(String message);
}
