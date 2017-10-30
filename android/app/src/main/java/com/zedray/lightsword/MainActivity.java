package com.zedray.lightsword;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.zedray.lightsword.bluetooth.BluetoothStack;
import com.zedray.lightsword.setup.SetupStack;

public class MainActivity extends AppCompatActivity {

    private BluetoothStack mBluetoothStack;
    private SetupStack mSetupStack;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mBluetoothStack = new BluetoothStack(this);
        mSetupStack = new SetupStack(this);

        mLoading = findViewById(R.id.loading);
        mLoading.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NonNull final View view) {
                mBluetoothStack.refresh();
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
        mSetupStack.checkLocationPermission();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBluetoothStack.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mSetupStack.onRequestPermissionsResult(requestCode, grantResults);
    }

    public void setStatus(@NonNull final String statusText) {
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

    public void hideStatus() {
        mLoading.setVisibility(View.GONE);
        update();
    }

    public void setError(@NonNull final String errorText) {
        mLoading.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.INVISIBLE);
        mStatusTextView.setText(errorText);
    }

    public void update() {
        update(mRedSeekBar.getProgress(), mGreenSeekBar.getProgress(),
                mBlueSeekBar.getProgress());
        mBluetoothStack.update(mRedSeekBar.getProgress(), mGreenSeekBar.getProgress(),
                mBlueSeekBar.getProgress());
    }

    public void startBluetooth() {
        mBluetoothStack.startBluetooth();
    }

    public void checkLocationPermission() {
        mSetupStack.checkLocationPermission();
    }

    private void update(int red, int green, int blue) {
        mPreview.setBackgroundColor(Color.rgb(red, green, blue));
        setTextString(mRedTextView, R.string.red, red);
        setTextString(mGreenTextView, R.string.green, green);
        setTextString(mBlueTextView, R.string.blue, blue);
    }

    private void setTextString(@NonNull final TextView textView, @StringRes int labelRes,
                               final int value) {
        textView.setText(getString(labelRes, value));
    }

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
}
