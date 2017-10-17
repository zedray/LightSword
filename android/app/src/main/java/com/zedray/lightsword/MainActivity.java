package com.zedray.lightsword;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.larswerkman.holocolorpicker.ColorPicker;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private BeanLEDStrip mBeanLedStrip;
    private ToggleButton mPowerButton;
    private ColorPicker mColorPicker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPowerButton = (ToggleButton) findViewById(R.id.powerButton);
        mColorPicker = (ColorPicker) findViewById(R.id.picker);
        mColorPicker.setOnColorChangedListener(colorChangedListener);
        mPowerButton.setOnCheckedChangeListener(powerChangeListener);
        mBeanLedStrip = new BeanLEDStrip(this, (TextView) findViewById(R.id.connectStatusText));
        mBeanLedStrip.connect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh_beans_setting) {
            mBeanLedStrip.reset();
            mPowerButton.setChecked(false);
        }

        return super.onOptionsItemSelected(item);
    }

    private void setLedColor(int rgb) {
        int red = rgb >> 16 & 0xff;
        int green = rgb >> 8 & 0xff;
        int blue = rgb & 0xff;
        mBeanLedStrip.setLeds(red, green, blue);
    }

    private CompoundButton.OnCheckedChangeListener powerChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Log.d(TAG, "Power button changed to " + isChecked);
            if (isChecked) {
                setLedColor(mColorPicker.getColor());
            } else {
                mBeanLedStrip.setLeds(0, 0, 0);
            }
        }
    };

    private ColorPicker.OnColorChangedListener colorChangedListener = new ColorPicker.OnColorChangedListener() {
        public void onColorChanged(int color) {
            setLedColor(mColorPicker.getColor());
        }
    };
}
