package com.zedray.lightsword;

public class Normalizer {

    private int[] mMin;
    private int[] mMax;
    private int mRange;

    public Normalizer(int range) {
        mRange = range;
    }

    public int[] setValues(int[] values) {
        if (mMin == null) {
            mMin = new int[values.length];
            mMax = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                mMin[i] = Integer.MAX_VALUE;
            }
        }
        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            mMin[i] = Math.min(values[i], mMin[i]);
            mMax[i] = Math.max(values[i], mMax[i]);

            if (mMax[i] > mMin[i]) {
                result[i] = (values[i] - mMin[i]) * 255 / (mMax[i] - mMin[i]);
            } else {
                result[i] = 0;
            }
        }
        return result;
    }
}
