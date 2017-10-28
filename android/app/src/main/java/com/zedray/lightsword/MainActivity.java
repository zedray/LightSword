package com.zedray.lightsword;

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

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "LightSword";
    private static final String DEVICE_NAME = "LightBean";
    private static final String SERVICE = "a495ff20-c5b1-4b44-b512-1370f02d74de";
    private static final String SCRATCH = "a495ff21-c5b1-4b44-b512-1370f02d74de";
    private static final long SCAN_PERIOD = 10000;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 846;

    private BluetoothGatt mBluetoothGatt;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGattCharacteristic mCharacteristic;
    private Handler mHandler = new Handler();

    private View mLoading;
    private TextView mStatusTextView;
    private View mPreview;
    private SeekBar mRedSeekBar;
    private SeekBar mGreenSeekBar;
    private SeekBar mBlueSeekBar;
    private TextView mRedTextView;
    private TextView mGreenTextView;
    private TextView mBlueTextView;
    private boolean mScanning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mLoading = findViewById(R.id.loading);
        mLoading.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mScanning) {
                    startDeviceScan();
                }
            }
        });
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
        checkLocationPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PERMISSION_GRANTED) {
                    checkLocationSetting();
                } else {
                    setStatus(getString(R.string.error_location_permission_not_enabled), true);
                }
            }
        }
    }

    private void checkLocationPermission() {
        setStatus(getString(R.string.checking_location_permission), true);
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
            checkLocationSetting();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_COARSE_LOCATION);
        }
    }

    private void checkLocationSetting() {
        setStatus(getString(R.string.checking_location_setting), true);
        LocationServices.getSettingsClient(this)
                .checkLocationSettings(new LocationSettingsRequest.Builder().build())
                .addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
                    @Override
                    public void onComplete(@NonNull final Task<LocationSettingsResponse> task) {
                        try {
                            LocationSettingsResponse response = task.getResult(ApiException.class);
                            if (response.getLocationSettingsStates().isLocationUsable()) {
                                startBluetooth();
                            } else {
                                setStatus(getString(R.string.error_location_setting_not_enabled), true);
                            }
                        } catch (ApiException exception) {
                            setStatus(getString(R.string.error_location_other)
                                    + exception.getLocalizedMessage(), true);
                        }
                    }
                });
    }

    private void startBluetooth() {
        setStatus(getString(R.string.starting_bluetooth), true);
        mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
                .getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    REQUEST_ENABLE_BT);
            setStatus(getString(R.string.error_bluetooth_not_enabled), true);
        } else {
            startDeviceScan();
        }
    }

    private void update() {
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

    private void startDeviceScan() {
        setStatus(getString(R.string.scanning_for_devices), true);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mScanning) {
                    stopScan();
                    setStatus(getString(R.string.scanning_for_devices), true);
                }
            }
        }, SCAN_PERIOD);
        mScanning = true;
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        update();
    }

    private void stopScan() {
        mScanning = false;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        update();
    }

    private static byte[] intToByteArray(int red, int green, int blue) {
        return new byte[]{
                (byte) ((0xFF) & red),
                (byte) ((0xFF) & green),
                (byte) ((0xFF) & blue),
                (byte) (0)};
    }

    private void setStatus(@NonNull final String statusText, final boolean isVisible) {
        mLoading.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        mStatusTextView.setText(statusText);
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d(TAG, "Connected to GATT server.");
                        Log.d(TAG, "Attempting to start service discovery:" +
                                mBluetoothGatt.discoverServices());

                    } else if (newState == STATE_DISCONNECTED) {
                        Log.d(TAG, "Disconnected from GATT server.");
                    }
                }

                @Override
                // New services discovered
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
                    Log.d(TAG, "onCharacteristicWrite characteristic: " + characteristic.getUuid() + " " + status);
                }
            };

    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(final SeekBar seekBar, int progress, boolean fromUser) {
            update();
        }

        @Override
        public void onStartTrackingTouch(final SeekBar seekBar) {
            // Do nothing.
        }

        @Override
        public void onStopTrackingTouch(final SeekBar seekBar) {
            // Do nothing.
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (device.getName() != null && device.getName().equalsIgnoreCase(DEVICE_NAME)) {
                                setStatus(getString(R.string.connected_to_device), true);
                                Log.d(TAG, "Connected to: " + DEVICE_NAME);
                                stopScan();
                                mBluetoothGatt = device.connectGatt(MainActivity.this, false, mGattCallback);
                            } else {
                                Log.d(TAG, "Unknown device: " + device.getName() + " " + device.getAddress());
                            }
                        }
                    });
                }
            };
}
