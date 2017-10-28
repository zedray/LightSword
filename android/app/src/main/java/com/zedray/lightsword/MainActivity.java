package com.zedray.lightsword;

import android.Manifest;
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
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "LightSword";
    private final static String DEVICE_NAME = "LightBean";

    private final static String SERVICE = "a495ff20-c5b1-4b44-b512-1370f02d74de";
    private final static String SCRATCH1 = "a495ff21-c5b1-4b44-b512-1370f02d74de";
    private final static String SCRATCH2 = "a495ff22-c5b1-4b44-b512-1370f02d74de";
    private final static String SCRATCH3 = "a495ff23-c5b1-4b44-b512-1370f02d74de";

    private final static int REQUEST_ENABLE_BT = 1;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGattService mService;
    private BluetoothGattCharacteristic mRedCharacteristic;
    private BluetoothGattCharacteristic mGreenCharacteristic;
    private BluetoothGattCharacteristic mBlueCharacteristic;

    private TextView mStatusTextView;
    private View mPreview;
    private SeekBar mRedSeekBar;
    private SeekBar mGreenSeekBar;
    private SeekBar mBlueSeekBar;
    private TextView mRedTextView;
    private TextView mGreenTextView;
    private TextView mBlueTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStatusTextView = (TextView) findViewById(R.id.textView_status);
        mPreview = findViewById(R.id.preview);
        mRedSeekBar = (SeekBar) findViewById(R.id.seekBar_red);
        mGreenSeekBar = (SeekBar) findViewById(R.id.seekBar_green);
        mBlueSeekBar = (SeekBar) findViewById(R.id.seekBar_blue);
        mRedTextView = (TextView) findViewById(R.id.textView_red);
        mGreenTextView = (TextView) findViewById(R.id.textView_green);
        mBlueTextView = (TextView) findViewById(R.id.textView_blue);
        mRedSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mGreenSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mBlueSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);


        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!hasBlePermissions() ||
                !getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "ble_not_supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            if(hasBlePermissions()) {
                mHandler = new Handler();

                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                    Toast.makeText(this, "ble_not_supported", Toast.LENGTH_SHORT).show();
                    finish();
                }

                final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                mBluetoothAdapter = bluetoothManager.getAdapter();

                scanLeDevice(true);
            }
        }

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();


        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        scanLeDevice(true);
        update();
    }

    public static byte[] intToByteArray(int value) {
        return ByteBuffer.allocate(8).putInt(value).array();
    }

    SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            update();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    public boolean hasBlePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            return true;
        }
    }

    private void update() {
        if (mScanning) {
            mStatusTextView.setText("Scanning");
        } else if (mRedCharacteristic != null) {
            mStatusTextView.setText("Connected to ");
        } else {
            mStatusTextView.setText("No device found");
        }
        mPreview.setBackgroundColor(Color.rgb(mRedSeekBar.getProgress(), mGreenSeekBar.getProgress(), mBlueSeekBar.getProgress()));
        mRedTextView.setText("Red : " + mRedSeekBar.getProgress());
        mGreenTextView.setText("Green : " + mGreenSeekBar.getProgress());
        mBlueTextView.setText("Blue : " + mBlueSeekBar.getProgress());

        if (mRedCharacteristic != null) {
            Log.w(TAG, "set R " + mRedSeekBar.getProgress() + " G " + mGreenSeekBar.getProgress() + " B " + mBlueSeekBar.getProgress());
            mRedCharacteristic.setValue(intToByteArray(mRedSeekBar.getProgress()));
            mGreenCharacteristic.setValue(intToByteArray(mGreenSeekBar.getProgress()));
            mBlueCharacteristic.setValue(intToByteArray(mBlueSeekBar.getProgress()));
            mBluetoothGatt.writeCharacteristic(mRedCharacteristic);
            mBluetoothGatt.writeCharacteristic(mGreenCharacteristic);
            mBluetoothGatt.writeCharacteristic(mBlueCharacteristic);
        } else {
            Log.w(TAG, "NOT CONNECTED set R " + mRedSeekBar.getProgress() + " G " + mGreenSeekBar.getProgress() + " B " + mBlueSeekBar.getProgress());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    private boolean mScanning;
    private Handler mHandler = new Handler();
    private static final long SCAN_PERIOD = 10000;

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.w(TAG, "Device: " + device.getName() + " " + device.getAddress());

                            if (device.getName() != null && device.getName().equalsIgnoreCase(DEVICE_NAME)) {

                                mScanning = false;
                                mBluetoothAdapter.stopLeScan(mLeScanCallback);

                                connectToDevice(device);
                            }
                        }
                    });
                }
            };

    BluetoothGatt mBluetoothGatt;
    int mConnectionState;

    private void connectToDevice(BluetoothDevice device) {
        Log.i(TAG, "connectToDevice.");
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
    }

    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        mConnectionState = STATE_CONNECTED;
                        Log.i(TAG, "Connected to GATT server.");
                        Log.i(TAG, "Attempting to start service discovery:" +
                                mBluetoothGatt.discoverServices());

                    } else if (newState == STATE_DISCONNECTED) {
                        mConnectionState = STATE_DISCONNECTED;
                        Log.i(TAG, "Disconnected from GATT server.");
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.w(TAG, "onServicesDiscovered GATT_SUCCESS");
                        for (BluetoothGattService service : gatt.getServices()) {
                            Log.w(TAG, "Service: " + service.getUuid());
                            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                                Log.w(TAG, "Characteristic: " + characteristic.getUuid() + "  " + characteristic.getInstanceId());
                                if (characteristic.getValue() == null) {
                                    Log.w(TAG, "value is null");
                                } else {
                                    int format = -1;
                                    final int value = characteristic.getIntValue(format, 1);
                                    Log.w(TAG, "value: " + value);
                                }
                            }
                        }

                        mService = gatt.getService(UUID.fromString(SERVICE));
                        Log.w(TAG, "mService: " + mService.getUuid());
                        mRedCharacteristic = mService.getCharacteristic(UUID.fromString(SCRATCH1));
                        Log.w(TAG, "mRedCharacteristic: " + mRedCharacteristic.getUuid());
                        mGreenCharacteristic = mService.getCharacteristic(UUID.fromString(SCRATCH2));
                        Log.w(TAG, "mGreenCharacteristic: " + mGreenCharacteristic.getUuid());
                        mBlueCharacteristic = mService.getCharacteristic(UUID.fromString(SCRATCH3));
                        Log.w(TAG, "mBlueCharacteristic: " + mBlueCharacteristic.getUuid());
                    } else {
                        Log.w(TAG, "onServicesDiscovered received: " + status);
                    }
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    Log.w(TAG, "onCharacteristicRead status: " + status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.w(TAG, "onCharacteristicRead received: " + characteristic);
                        //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                    }
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        Log.d("onCharacteristicWrite", "Failed write, retrying");
                        gatt.writeCharacteristic(characteristic);
                    }
                    Log.w(TAG, "onCharacteristicWrite characteristic: " + characteristic.getUuid() + " " + status);
                }
            };

    private void scanLeDevice(final boolean enable) {
        Log.w(TAG, "scanLeDevice enable " + enable);
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mScanning) {
                        mScanning = false;
                        Log.w(TAG, "scanLeDevice stopLeScan after " + SCAN_PERIOD + "ms");
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    }
                    update();
                }
            }, SCAN_PERIOD);
            mScanning = true;
            Log.w(TAG, "scanLeDevice startLeScan");
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            Log.w(TAG, "scanLeDevice stopLeScan");
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        update();
    }
}

