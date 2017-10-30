package com.zedray.lightsword;

import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.zedray.lightsword.bluetooth.BluetoothStack;
import com.zedray.lightsword.setup.SetupStack;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    Visualizer mVisualizer;
    int[] mValues;
    int[] mMin;
    int[] mMax;
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
    private Button mMediaButton;
    private MediaPlayer mMediaPlayer;
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

        mMediaButton = (Button) findViewById(R.id.button_media);

        mRedSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mGreenSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mBlueSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);

        mMediaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    stopMedia();
                } else {
                    playMedia();
                }
            }
        });

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

    private void playMedia() {
        try {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                stopMedia();
            }
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            if (mMediaPlayer == null) {
                mMediaPlayer = MediaPlayer.create(this, R.raw.flash);
            }

            // Create the Visualizer object and attach it to our media player.
            mVisualizer = new Visualizer(mMediaPlayer.getAudioSessionId());
            int range = Visualizer.getCaptureSizeRange()[0]; // 0=128  1=1024
            mVisualizer.setCaptureSize(range);
            mMin = new int[3];
            mMax = new int[3];
            for (int i = 0; i < 2; i++) {
                mMin[i] = Integer.MAX_VALUE;
            }

            mVisualizer.setDataCaptureListener(
                    new Visualizer.OnDataCaptureListener() {
                        public void onWaveFormDataCapture(Visualizer visualizer,
                                                          byte[] bytes, int samplingRate) {
                            //mVisualizerView.updateVisualizer(bytes);
                            useBytes(bytes);
                        }

                        public void onFftDataCapture(Visualizer visualizer,
                                                     byte[] bytes, int samplingRate) {
                            //useBytes(bytes);
                        }
                    }, Visualizer.getMaxCaptureRate(), true, false);

            //mMediaPlayer.prepare();
            //mMediaPlayer.setVolume(1f, 1f);
            //mMediaPlayer.setLooping(false);
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mVisualizer.setEnabled(false);
                }
            });

            mVisualizer.setEnabled(true);
            mMediaPlayer.start();


            mMediaButton.setText(R.string.stop);
        } catch (Exception e) {
            Log.e(TAG, "playMedia", e);
        }
    }

    private void useBytes(byte[] bytes) {
        if (bytes == null) {
            return;
        }
        int[] rgbValues = new int[3];
        rgbValues[0] = getAverage(bytes, 0, 60);
        rgbValues[1] = getAverage(bytes, 30, 90);
        rgbValues[2] = getAverage(bytes, 70, 128);
        //Log.d(TAG, "values R" + rgbValues[0] + " G" + rgbValues[1]+ " B" + rgbValues[2]);
        mRedSeekBar.setProgress(rgbValues[0]);
        mGreenSeekBar.setProgress(rgbValues[1]);
        mBlueSeekBar.setProgress(rgbValues[2]);
    }

    private int getAverage(byte[] bytes, int start, int end) {
        int sum = 0;
        for (int i = start; i < end; i++) {
            sum = (bytes[i] & 0xFF);
        }
        int average = sum * 1000 / (end - start);
        mMin[0] = Math.min(average, mMin[0]);
        mMax[0] = Math.max(average, mMax[0]);
        if (mMax[0] - mMin[0] > 0) {
            return (average - mMin[0]) * 255 / (mMax[0] - mMin[0]);
        } else {
            return 0;
        }
    }

    private void stopMedia() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mMediaButton.setText(R.string.play);
        }
        if (mVisualizer != null) {
            mVisualizer.release();
            mVisualizer = null;
        }
    }
}
