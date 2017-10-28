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
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "LightSword";
    private final static String DEVICE_NAME = "LightBean";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 846;

    private final static String SERVICE = "a495ff20-c5b1-4b44-b512-1370f02d74de";
    private final static String SCRATCH = "a495ff21-c5b1-4b44-b512-1370f02d74de";

    private final static int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000;
    BluetoothGatt mBluetoothGatt;
    int mConnectionState;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGattCharacteristic mCharacteristic;
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

                        BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE));
                        Log.w(TAG, "service: " + service.getUuid());
                        mCharacteristic = service.getCharacteristic(UUID.fromString(SCRATCH));
                        Log.w(TAG, "mCharacteristic: " + mCharacteristic.getUuid());
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
    private TextView mStatusTextView;
    private View mPreview;
    private SeekBar mRedSeekBar;
    private SeekBar mGreenSeekBar;
    private SeekBar mBlueSeekBar;
    private TextView mRedTextView;
    private TextView mGreenTextView;
    private TextView mBlueTextView;
    private boolean mScanning;
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
    private Handler mHandler = new Handler();
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Log.w(TAG, "onLeScan");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (device.getName() != null && device.getName().equalsIgnoreCase(DEVICE_NAME)) {
                                Log.w(TAG, "Connected to: " + DEVICE_NAME);
                                scanLeDevice(false);
                                connectToDevice(device);
                            } else {
                                Log.w(TAG, "Unknown device: " + device.getName() + " " + device.getAddress());
                            }
                        }
                    });
                }
            };

    public static byte[] intToByteArray(int red, int green, int blue) {
        byte[] result = new byte[] {
                (byte)((0xFF) & red),
                (byte)((0xFF) & green),
                (byte)((0xFF) & blue),
                (byte)(0)};

        Log.w(TAG, "intToByteArray: red:" + red + " Output: " + String.format("%8s", Integer.toBinaryString(result[0] & 0xFF)).replace(' ', '0'));
        Log.w(TAG, "intToByteArray: green:" + red + " Output: " + String.format("%8s", Integer.toBinaryString(result[1] & 0xFF)).replace(' ', '0'));
        Log.w(TAG, "intToByteArray: blue:" + red + " Output: " + String.format("%8s", Integer.toBinaryString(result[2] & 0xFF)).replace(' ', '0'));
        return result;
    }

    public static byte[] intToByteArrayOLD(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }

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

        update();
        checkPermission();
    }


    private void checkPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
        } else {
            startLocation();
        }
    }

    private void startLocation() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
                //.addLocationRequest(mLocationRequestHighAccuracy)
                //.addLocationRequest(mLocationRequestBalancedPowerAccuracy);
        Task<LocationSettingsResponse> task =
                LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());
        task.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    if (response.getLocationSettingsStates().isLocationUsable()) {
                        startBluetooth();
                    } else {
                        Log.w(TAG, "NO LOCATION!!");
                    }

                } catch (ApiException exception) {
                    Log.w(TAG, "startLocation ApiException " + exception);
                }
            }
        });

    }

    private void startBluetooth() {
        Log.w(TAG, "startBluetooth");
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            scanLeDevice(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.w(TAG, "onRequestPermissionsResult");
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                Log.w(TAG, "onRequestPermissionsResult PERMISSION_REQUEST_COARSE_LOCATION");
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, yay! Start the Bluetooth device scan.
                    Log.w(TAG, "Permission granted, yay! Start the Bluetooth device scan");
                    startLocation();
                } else {
                    // Alert the user that this application requires the location permission to perform the scan.
                    Log.w(TAG, "Alert the user that this application requires the location permission to perform the scan.");
                }
            }
        }
    }

    private void update() {
        if (mScanning) {
            mStatusTextView.setText("Scanning");
        } else if (mCharacteristic != null) {
            mStatusTextView.setText("Connected to device");
        } else {
            mStatusTextView.setText("No device found");
        }
        mPreview.setBackgroundColor(Color.rgb(mRedSeekBar.getProgress(), mGreenSeekBar.getProgress(), mBlueSeekBar.getProgress()));
        mRedTextView.setText("Red : " + mRedSeekBar.getProgress());
        mGreenTextView.setText("Green : " + mGreenSeekBar.getProgress());
        mBlueTextView.setText("Blue : " + mBlueSeekBar.getProgress());

        if (mCharacteristic != null) {
            Log.w(TAG, "set R " + mRedSeekBar.getProgress() + " G " + mGreenSeekBar.getProgress() + " B " + mBlueSeekBar.getProgress());
            mCharacteristic.setValue(intToByteArray(mRedSeekBar.getProgress(), mGreenSeekBar.getProgress(), mBlueSeekBar.getProgress()));
            mBluetoothGatt.writeCharacteristic(mCharacteristic);
        } else {
            Log.w(TAG, "NOT CONNECTED");
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

    private void connectToDevice(BluetoothDevice device) {
        Log.i(TAG, "connectToDevice.");
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
    }

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

