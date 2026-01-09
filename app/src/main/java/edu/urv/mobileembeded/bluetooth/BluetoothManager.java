package edu.urv.mobileembeded.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothManager {

    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private Thread workerThread;
    private BluetoothListener listener;

    public BluetoothManager(BluetoothListener listener) {
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.listener = listener;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public List<BluetoothDevice> getPairedDevices() {
        if (ActivityCompat.checkSelfPermission(null, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
             if (listener != null) {
                listener.onError("Bluetooth permission not granted");
            }
            return new ArrayList<>();
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        return new ArrayList<>(pairedDevices);
    }

    public void connectToDevice(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(null, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (listener != null) {
                listener.onError("Bluetooth permission not granted");
            }
            return;
        }
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);
            bluetoothSocket.connect();
            inputStream = bluetoothSocket.getInputStream();
            beginListenForData();
            if (listener != null) {
                listener.onDeviceConnected(device.getName());
            }
        } catch (IOException e) {
            if (listener != null) {
                listener.onError("Connection failed: " + e.getMessage());
            }
        }
    }

    private void beginListenForData() {
        workerThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    bytes = inputStream.read(buffer);
                    String data = new String(buffer, 0, bytes);
                    if (listener != null) {
                        listener.onDataReceived(data);
                    }
                } catch (IOException e) {
                    if (listener != null) {
                        listener.onError("Error reading from device: " + e.getMessage());
                    }
                    break;
                }
            }
        });
        workerThread.start();
    }

    public void closeConnection() {
        try {
            if (workerThread != null) {
                workerThread.interrupt();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            if (listener != null) {
                listener.onError("Error closing connection: " + e.getMessage());
            }
        }
    }
}
