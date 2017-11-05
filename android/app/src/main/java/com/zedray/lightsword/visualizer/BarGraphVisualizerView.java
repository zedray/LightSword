package com.zedray.lightsword.visualizer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.zedray.lightsword.MainActivity;
import com.zedray.lightsword.Normalizer;

public class BarGraphVisualizerView extends View {

    private static final String TAG = "BarGraphVisualizerView";
    private static final int DIVISIONS = 4;
    private static final int RED = 1;
    private static final int GREEN = 6;
    private static final int BLUE = 12;

    private Rect mRect = new Rect();
    private Paint mBackgroundPaint = new Paint();
    private Paint mRedPaint = new Paint();
    private Paint mGreenPaint = new Paint();
    private Paint mBluePaint = new Paint();
    private Paint mForegroundPaint = new Paint();
    private int[] mDbValues;
    private Normalizer mNormalizer = new Normalizer(255);
    private MainActivity mMainActivity;

    public BarGraphVisualizerView(Context context) {
        super(context);
        init();
    }

    public BarGraphVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BarGraphVisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mBackgroundPaint.setColor(Color.BLUE);
        mRedPaint.setColor(Color.RED);
        mGreenPaint.setColor(Color.GREEN);
        mBluePaint.setColor(Color.BLUE);
        mForegroundPaint.setColor(Color.BLACK);
        mForegroundPaint.setStrokeWidth(3);
        mRect.set(0, 0, getWidth(), getHeight());
    }

    public void updateVisualizer(byte[] bytes) {
        mDbValues = new int[bytes.length / DIVISIONS];
        int[] values = new int[bytes.length / DIVISIONS];
        for (int i = 0; i < bytes.length / DIVISIONS; i++) {
            byte rfk = bytes[DIVISIONS * i];
            byte ifk = bytes[DIVISIONS * i + 1];
            float magnitude = (rfk * rfk + ifk * ifk);
            int dbValue = (int) (10 * Math.log10(magnitude));
            if (dbValue < -100000) {
                values[i] = 0;
            } else {
                values[i] = dbValue;
            }
        }

        mDbValues = mNormalizer.setValues(values);
        mMainActivity.setValues(mDbValues[RED],  mDbValues[GREEN],  mDbValues[BLUE]);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mDbValues == null) {
            return;
        }

        canvas.drawRect(mRect, mBackgroundPaint);
        int width = getWidth() / mDbValues.length;
        for (int i = 0; i < mDbValues.length; i++) {
            int left = i * width;
            int top = getHeight() - mDbValues[i];
            int right = (i + 1) * width;
            int bottom = getHeight();
            canvas.drawRect(left, top, right, bottom, getPaint(i));
        }
        canvas.drawRect(0, 0, getWidth(), 5, mForegroundPaint);
    }

    private Paint getPaint(int i) {
        switch (i) {
            case RED:
                return mRedPaint;
            case GREEN:
                return mGreenPaint;
            case BLUE:
                return mBluePaint;
            default:
                return mForegroundPaint;
        }
    }

    public void setActivity(MainActivity mainActivity) {
        mMainActivity = mainActivity;
    }
}