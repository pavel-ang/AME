package edu.urv.mobileembeded.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothManager {

    private static final UUID HM10_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID HM10_CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Context context;
    private final BluetoothListener listener;

    private BluetoothGatt bluetoothGatt;
    private final List<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private boolean scanning;

    public BluetoothManager(Context context, BluetoothListener listener) {
        this.context = context;
        this.listener = listener;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            this.bluetoothLeScanner = null;
            if (listener != null) {
                listener.onError("Bluetooth is not supported on this device.");
            }
        } else {
            this.bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public List<BluetoothDevice> getPairedDevices() {
        if (bluetoothAdapter == null) {
            return new ArrayList<>();
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (listener != null) {
                listener.onError("Bluetooth connect permission not granted");
            }
            return new ArrayList<>();
        }
        return new ArrayList<>(bluetoothAdapter.getBondedDevices());
    }

    public void startScan() {
        if (scanning) {
            return;
        }
        if (bluetoothLeScanner == null) {
            if (listener != null) {
                listener.onError("Bluetooth is not supported on this device.");
            }
            return;
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            if (listener != null) {
                listener.onError("Bluetooth scan permission not granted");
            }
            return;
        }
        discoveredDevices.clear();
        scanning = true;
        // Stop scanning after a predefined scan period.
        handler.postDelayed(this::stopScan, 10000); // 10 seconds
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        bluetoothLeScanner.startScan(null, scanSettings, leScanCallback);
    }

    public void stopScan() {
        if (!scanning) {
            return;
        }
        if (bluetoothLeScanner == null) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            if (listener != null) {
                listener.onError("Bluetooth scan permission not granted");
            }
            return;
        }
        scanning = false;
        bluetoothLeScanner.stopScan(leScanCallback);
    }

    public List<BluetoothDevice> getDiscoveredDevices() {
        return discoveredDevices;
    }

    public void connectToDevice(BluetoothDevice device) {
        if (device == null) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (listener != null) {
                listener.onError("Bluetooth connect permission not granted");
            }
            return;
        }
        stopScan();
        bluetoothGatt = device.connectGatt(context, false, gattCallback);
    }

    public void closeConnection() {
        if (bluetoothGatt == null) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (listener != null) {
                listener.onError("Bluetooth connect permission not granted");
            }
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    public void writeData(String data) {
        if (bluetoothGatt == null) {
            return;
        }
        BluetoothGattService service = bluetoothGatt.getService(HM10_SERVICE_UUID);
        if (service == null) {
            return;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(HM10_CHARACTERISTIC_UUID);
        if (characteristic == null) {
            return;
        }
        characteristic.setValue(data.getBytes());
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothGatt.writeCharacteristic(characteristic);
    }

    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            if (device != null && !discoveredDevices.contains(device)) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                discoveredDevices.add(device);
                if (listener != null) {
                    listener.onDeviceDiscovered(device);
                }
            }
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (listener != null) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    listener.onDeviceConnected(gatt.getDevice().getName());
                }
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (listener != null) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    listener.onDeviceDisconnected(gatt.getDevice().getName());
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(HM10_SERVICE_UUID);
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(HM10_CHARACTERISTIC_UUID);
                    if (characteristic != null) {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        gatt.setCharacteristicNotification(characteristic, true);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (HM10_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                String data = new String(characteristic.getValue());
                if (listener != null) {
                    listener.onDataReceived(data);
                }
            }
        }
    };
}
