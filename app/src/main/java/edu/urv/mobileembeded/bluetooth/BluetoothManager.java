package edu.urv.mobileembeded.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothManager {

    private static final String TAG = "BT_DEBUG";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter bluetoothAdapter;
    private final Context context;
    private final BluetoothListener listener;

    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    public BluetoothManager(Context context, BluetoothListener listener) {
        this.context = context;
        this.listener = listener;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public void startDiscovery() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            if (listener != null) listener.onError("Bluetooth scan permission not granted");
            return;
        }
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
        Log.d(TAG, "Discovery started.");
    }

    public List<BluetoothDevice> getPairedDevices() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (listener != null) {
                listener.onError("Bluetooth connect permission not granted");
            }
            return new ArrayList<>();
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        return new ArrayList<>(pairedDevices);
    }

    public synchronized void connectToDevice(BluetoothDevice device) {
        Log.d(TAG, "Connecting to: " + device.getAddress());
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
        }

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();
    }

    public synchronized void closeConnection() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
    }

    public void writeData(String data) {
        ConnectedThread r;
        synchronized (this) {
            if (connectedThread == null) return;
            r = connectedThread;
        }
        r.write(data.getBytes());
    }

    private synchronized void manageConnectedSocket(BluetoothSocket socket) {
        if (listener != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            listener.onDeviceConnected(socket.getRemoteDevice().getName());
        }
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                if (listener != null) listener.onError("Bluetooth connect permission not granted");
            } else {
                try {
                    tmp = device.createRfcommSocketToServiceRecord(SPP_UUID);
                } catch (IOException e) {
                    Log.e(TAG, "Socket's create() method failed", e);
                    if (listener != null) {
                        listener.onError("Socket creation failed: " + e.getMessage());
                    }
                }
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "BEGIN connectThread");
            if (mmSocket == null) {
                return; // Socket creation failed, error already reported.
            }
            try {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mmSocket.connect();
            } catch (IOException connectException) {
                Log.e(TAG, "Unable to connect; closing socket", connectException);
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                if(listener != null) listener.onError("Connection failed: " + connectException.getMessage());
                return;
            }

            synchronized (BluetoothManager.this) {
                connectThread = null;
            }

            manageConnectedSocket(mmSocket);
        }

        public void cancel() {
            try {
                if (mmSocket != null) {
                    mmSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String data = new String(buffer, 0, bytes);
                    if (listener != null) {
                        listener.onDataReceived(data);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Input stream was disconnected", e);
                    if (listener != null) {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        listener.onDeviceDisconnected(mmSocket.getRemoteDevice().getName());
                    }
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
