package com.zedray.lightsword;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import com.punchthrough.bean.sdk.Bean;
import com.punchthrough.bean.sdk.BeanDiscoveryListener;
import com.punchthrough.bean.sdk.BeanListener;
import com.punchthrough.bean.sdk.BeanManager;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.ScratchBank;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Locale;


/**************************************
 * Message Definitions
 *************************************/
enum Command {
    SET_LEDS
}


/**************************************
 * Global State
 *************************************/
enum LedState {
    ON,
    OFF,
}


public class BeanLEDStrip {

    private final byte START_FRAME = 0x77;  // Random start frame

    private static final String TAG = "BeanLEDStrip";
    private static final String BEAN_NAME = "PrototypeBean";

    private final Context mContext;
    private final TextView mConnectStatus;
    private Bean mLedStrip;
    private LedState mLedState;

    public BeanLEDStrip(Context context, TextView connectStatus) {
        this.mContext = context;
        this.mConnectStatus = connectStatus;
        this.mLedStrip = null;
        this.mLedState = LedState.OFF;
    }

    public Boolean isConnected() {
        return mLedStrip.isConnected();
    }

    public void connect() {
        Log.d(TAG, "Starting connection attempt...");
        if (mLedStrip == null) {
            Log.d(TAG, "Starting Bean discovery...");
            BeanManager beanManager = BeanManager.getInstance();
            beanManager.setScanTimeout(20);
            beanManager.startDiscovery(new BeanDiscoveryListener() {
                @Override
                public void onBeanDiscovered(Bean bean, int rssi) {
                    Log.d(TAG, "Bean found");

                    if (bean.getDevice().getName().equals(BEAN_NAME)) {
                        Log.d(TAG, "LED strip found!");
                        mLedStrip = bean;

                        Log.d(TAG, "Starting to connect to bean named " + BEAN_NAME);
                        mLedStrip.connect(mContext, beanListener);
                    }
                }

                @Override
                public void onDiscoveryComplete() {
                    Log.d(TAG, "Bean Discovery Complete");
                }
            });
        }
    }

    public void reset() {
        if (mLedStrip != null) {
            mLedStrip.disconnect();
            mLedStrip = null;
            mLedState = LedState.OFF;
        }
        mConnectStatus.setText("Disconnected");
        connect();
    }

    private BeanListener beanListener = new BeanListener() {

        @Override
        public void onConnected() {
            Log.d(TAG, "Connected");
            mConnectStatus.setText("Connected");
            setLeds(0, 0, 0);
        }

        @Override
        public void onConnectionFailed() {
            Log.d(TAG, "Connection Failed");
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "Disconnected");
        }

        @Override
        public void onSerialMessageReceived(byte[] bytes) {
            Log.d(TAG, "Serial Message Received");
        }

        @Override
        public void onScratchValueChanged(ScratchBank scratchBank, byte[] bytes) {
            Log.d(TAG, "Scratch Value Changed");
        }

        @Override
        public void onError(BeanError beanError) {
            Log.d(TAG, "Bean Error");
        }

        @Override
        public void onReadRemoteRssi(int rssi) {
            Log.d(TAG, "onReadRemoteRssi " + rssi);
        }
    };

    private byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }

    public void setLeds(int red, int green, int blue) {
        byte[] buffer = new byte[5];
        buffer[0] = START_FRAME;
        buffer[1] = (byte) Command.SET_LEDS.ordinal();
        buffer[2] = (byte) red;
        buffer[3] = (byte) green;
        buffer[4] = (byte) blue;
        Log.d(TAG, String.format(Locale.US, "Setting RGB (%d, %d, %d)", red, green, blue));
        mLedStrip.sendSerialMessage(buffer);
    }

    public void setLedsOld(int red, int green, int blue)  {

        // Header and command ID
        byte[] buffer = new byte[2];
        buffer[0] = START_FRAME;
        buffer[1] = (byte) Command.SET_LEDS.ordinal();

        // Payload
        byte[] redBytes = intToByteArray(red);
        byte[] greenBytes = intToByteArray(green);
        byte[] blueBytes = intToByteArray(blue);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(buffer);
            outputStream.write(redBytes);
            outputStream.write(greenBytes);
            outputStream.write(blueBytes);
            byte[] allbytes = outputStream.toByteArray();
            mLedStrip.sendSerialMessage(allbytes);
            Log.d(TAG, "Sending serial message of length: " + allbytes.length);
            byte [] tmp = Arrays.copyOfRange(allbytes, 2, 6);
            System.out.println("Original red: " + red);
            System.out.println(new BigInteger(tmp).intValue());
            System.out.println("----");
            byte x = (byte) 255;
            System.out.println(x & 0xFF);
        } catch (IOException e) {
            Log.e(TAG, "FAIL: setLeds", e);
        }
    }
}
