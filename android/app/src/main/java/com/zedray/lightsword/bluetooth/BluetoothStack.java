package com.zedray.lightsword.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.zedray.lightsword.MainActivity;
import com.zedray.lightsword.R;

import java.util.UUID;

import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

public class BluetoothStack {

    private static final String TAG = "BluetoothStack";

    private static final String DEVICE_NAME = "LightBean";
    private static final String SERVICE = "a495ff20-c5b1-4b44-b512-1370f02d74de";
    private static final String SCRATCH = "a495ff21-c5b1-4b44-b512-1370f02d74de";

    private static final long SCAN_PERIOD = 10000;
    //private static final long SCAN_PERIOD = 100;
    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothGatt mBluetoothGatt;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGattCharacteristic mCharacteristic;
    private MainActivity mMainActivity;
    private boolean mScanning;
    private Handler mHandler;

    public BluetoothStack(@NonNull MainActivity mainActivity) {
        mHandler = new Handler();
        mMainActivity = mainActivity;
    }

    public void refresh() {
        if (mBluetoothAdapter == null) {
            mMainActivity.checkLocationPermission();
        } else if (!mScanning) {
            startDeviceScan();
        }
    }

    public void update(int red, int green, int blue) {
        if (mCharacteristic != null) {
            mCharacteristic.setValue(intToByteArray(red, green, blue));
            mBluetoothGatt.writeCharacteristic(mCharacteristic);
        } else {
            Log.v(TAG, "Not connected");
        }
    }

    public void onPause() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    public void startBluetooth() {
        mMainActivity.setStatus(mMainActivity.getString(R.string.starting_bluetooth));
        mBluetoothAdapter = ((BluetoothManager) mMainActivity.getSystemService(Context.BLUETOOTH_SERVICE))
                .getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            mMainActivity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    REQUEST_ENABLE_BT);
            mMainActivity.setError(mMainActivity.getString(R.string.error_bluetooth_not_enabled));
        } else {
            startDeviceScan();
        }
    }

    private void startDeviceScan() {
        mMainActivity.setStatus(mMainActivity.getString(R.string.scanning_for_devices));
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mScanning) {
                    stopScan();
                    mMainActivity.setError(mMainActivity.getString(R.string.error_could_not_find_device));
                }
            }
        }, SCAN_PERIOD);
        mScanning = true;
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        mMainActivity.update();
    }

    private void stopScan() {
        mScanning = false;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        mMainActivity.update();
    }

    private static byte[] intToByteArray(int red, int green, int blue) {
        return new byte[]{
                (byte) ((0xFF) & red),
                (byte) ((0xFF) & green),
                (byte) ((0xFF) & blue),
                (byte) (0)};
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server.");
                mMainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothGatt.discoverServices();
                    }
                });
            } else if (newState == STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server.");
                mCharacteristic = null;
                startDeviceScan();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (BluetoothGattService service : gatt.getServices()) {
                    Log.d(TAG, "Service: " + service.getUuid());
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        Log.d(TAG, "Characteristic: " + characteristic.getUuid() + "  " + characteristic.getInstanceId());
                        if (characteristic.getValue() == null) {
                            Log.d(TAG, "value is null");
                        } else {
                            int format = -1;
                            final int value = characteristic.getIntValue(format, 1);
                            Log.d(TAG, "value: " + value);
                        }
                    }
                }

                BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE));
                Log.d(TAG, "service: " + service.getUuid());
                mCharacteristic = service.getCharacteristic(UUID.fromString(SCRATCH));
                Log.d(TAG, "mCharacteristic: " + mCharacteristic.getUuid());
                mMainActivity.update();
            } else {
                Log.d(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.d(TAG, "onCharacteristicRead status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onCharacteristicRead received: " + characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d("onCharacteristicWrite", "Failed write, retrying");
                gatt.writeCharacteristic(characteristic);
            }
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    mMainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (device.getName() != null && device.getName().equalsIgnoreCase(DEVICE_NAME)) {
                                mMainActivity.hideStatus();
                                Log.d(TAG, "Connecting to: " + DEVICE_NAME);
                                stopScan();
                                mBluetoothGatt = device.connectGatt(mMainActivity, false, mGattCallback);
                            } else {
                                Log.d(TAG, "Unknown device: " + device.getName() + " " + device.getAddress());
                            }
                        }
                    });
                }
            };
}
