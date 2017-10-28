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
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
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
    private ProgressBar mProgressBar;
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
            public void onClick(@NonNull final View view) {
                if (mBluetoothAdapter == null) {
                    checkLocationPermission();
                } else if (!mScanning) {
                    startDeviceScan();
                }
            }
        });
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
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
        checkLocationPermission();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PERMISSION_GRANTED) {
                    checkLocationSetting();
                } else {
                    setError(getString(R.string.error_location_permission_not_enabled));
                }
            }
        }
    }

    private void checkLocationPermission() {
        setStatus(getString(R.string.checking_location_permission));
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
            checkLocationSetting();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_COARSE_LOCATION);
        }
    }

    private void checkLocationSetting() {
        setStatus(getString(R.string.checking_location_setting));
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
                                setError(getString(R.string.error_location_setting_not_enabled));
                            }
                        } catch (ApiException exception) {
                            setError(getString(R.string.error_location_other)
                                    + exception.getLocalizedMessage());
                        }
                    }
                });
    }

    private void startBluetooth() {
        setStatus(getString(R.string.starting_bluetooth));
        mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
                .getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    REQUEST_ENABLE_BT);
            setError(getString(R.string.error_bluetooth_not_enabled));
        } else {
            startDeviceScan();
        }
    }

    private void update() {
        mPreview.setBackgroundColor(Color.rgb(mRedSeekBar.getProgress(),
                mGreenSeekBar.getProgress(), mBlueSeekBar.getProgress()));
        setTextString(mRedTextView, R.string.red, mRedSeekBar);
        setTextString(mGreenTextView, R.string.green, mGreenSeekBar);
        setTextString(mBlueTextView, R.string.blue, mBlueSeekBar);
        if (mCharacteristic != null) {
            mCharacteristic.setValue(intToByteArray(mRedSeekBar.getProgress(),
                    mGreenSeekBar.getProgress(), mBlueSeekBar.getProgress()));
            mBluetoothGatt.writeCharacteristic(mCharacteristic);
        } else {
            Log.v(TAG, "Not connected");
        }
    }

    private void setTextString(@NonNull final TextView textView, @StringRes int labelRes,
                               @NonNull final SeekBar seekBar) {
        textView.setText(getString(labelRes, (seekBar.getProgress() + 1) * 100 / 256));
    }

    private void startDeviceScan() {
        setStatus(getString(R.string.scanning_for_devices));
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mScanning) {
                    stopScan();
                    setError(getString(R.string.error_could_not_find_device));
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

    private void setStatus(@NonNull final String statusText) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoading.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.setIndeterminate(true);
                mStatusTextView.setText(statusText);
            }
        });
    }

    private void hideStatus() {
        mLoading.setVisibility( View.GONE);
        update();
    }

    private void setError(@NonNull final String errorText) {
        mLoading.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.INVISIBLE);
        mStatusTextView.setText(errorText);
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d(TAG, "Connected to GATT server.");
                    } else if (newState == STATE_DISCONNECTED) {
                        Log.d(TAG, "Disconnected from GATT server.");
                        mCharacteristic = null;
                        startDeviceScan();
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
                        update();
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
                                hideStatus();
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
