package com.zedray.lightsword.setup;

import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.zedray.lightsword.MainActivity;
import com.zedray.lightsword.R;
import com.zedray.lightsword.bluetooth.BluetoothStack;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class SetupStack {

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 846;

    private MainActivity mMainActivity;

    public SetupStack(@NonNull MainActivity mainActivity) {
        mMainActivity = mainActivity;
    }

    public void checkLocationPermission() {
        mMainActivity.setStatus(mMainActivity.getString(R.string.checking_location_permission));
        if (ContextCompat.checkSelfPermission(mMainActivity, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
            checkLocationSetting();
        } else {
            ActivityCompat.requestPermissions(mMainActivity, new String[]{ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_COARSE_LOCATION);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PERMISSION_GRANTED) {
                    checkLocationSetting();
                } else {
                    mMainActivity.setError(mMainActivity.getString(R.string.error_location_permission_not_enabled));
                }
            }
        }
    }

    private void checkLocationSetting() {
        mMainActivity.setStatus(mMainActivity.getString(R.string.checking_location_setting));
        LocationServices.getSettingsClient(mMainActivity)
                .checkLocationSettings(new LocationSettingsRequest.Builder().build())
                .addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
                    @Override
                    public void onComplete(@NonNull final Task<LocationSettingsResponse> task) {
                        try {
                            LocationSettingsResponse response = task.getResult(ApiException.class);
                            if (response.getLocationSettingsStates().isLocationUsable()) {
                                mMainActivity.startBluetooth();
                            } else {
                                mMainActivity.setError(mMainActivity.getString(R.string.error_location_setting_not_enabled));
                            }
                        } catch (ApiException exception) {
                            mMainActivity.setError(mMainActivity.getString(R.string.error_location_other)
                                    + exception.getLocalizedMessage());
                        }
                    }
                });
    }
}
