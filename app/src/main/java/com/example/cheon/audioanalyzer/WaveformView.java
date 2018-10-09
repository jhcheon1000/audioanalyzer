package com.example.cheon.audioanalyzer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceView;

import java.util.LinkedList;

public class WaveformView extends SurfaceView {

    private static final int HISTORY_SIZE = 6;

    private static final float MAX_AMPLITUDE_TO_DRAW = 8192.0f;

    private final LinkedList<short[]> mAudioData;

    private short[] mSingleAudioData;

    private final Paint mPaint;


    public WaveformView(Context context) {
        this(context, null, 0);
    }

    public WaveformView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveformView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mAudioData = new LinkedList<short[]>();


        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(0);
        mPaint.setAntiAlias(true);
    }

    public synchronized void updateAudioData(short[] buffer) {
        short[] newBuffer;

//        if (mAudioData.size() == HISTORY_SIZE) {
//            newBuffer = mAudioData.removeFirst();
//            System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
//        }
//        else {
//            newBuffer = buffer.clone();
//        }
//
//        mAudioData.addLast(newBuffer);
//
//        Canvas canvas = getHolder().lockCanvas();
//        if (canvas != null) {
//            drawWaveform(canvas);
//            getHolder().unlockCanvasAndPost(canvas);
//        }

        mSingleAudioData = buffer.clone();

        Canvas canvas = getHolder().lockCanvas();
        if (canvas != null) {
            drawWaveform(canvas);
            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    private void drawWaveform(Canvas canvas) {
        canvas.drawColor(Color.BLACK);

        float width = getWidth();
        float height = getHeight();
        float centerY = height / 2;

        int colorDelta = 255 / (HISTORY_SIZE + 1);
        int brightness = colorDelta;
        brightness = brightness * 6;

        mPaint.setColor(Color.argb(brightness, 128, 255, 192));

        float lastX = -1;
        float lastY = -1;

        for (int x = 0; x < width; x++) {
            int index = (int) ((x / width) * mSingleAudioData.length);
            short sample = mSingleAudioData[index];
            float y = (sample / MAX_AMPLITUDE_TO_DRAW) * centerY + centerY;

            if (lastX != -1) {
                canvas.drawLine(lastX, lastY, x, y, mPaint);
            }

            lastX = x;
            lastY = y;
        }


//        for (short[] buffer : mAudioData) {
//            mPaint.setColor(Color.argb(brightness, 128, 255, 192));
//
//            float lastX = -1;
//            float lastY = -1;
//
//            for (int x = 0; x < width; x++) {
//                int index = (int) ((x / width) * buffer.length);
//                short sample = buffer[index];
//                float y = (sample / MAX_AMPLITUDE_TO_DRAW) * centerY + centerY;
//
//                if (lastX != -1) {
//                    canvas.drawLine(lastX, lastY, x, y, mPaint);
//                }
//
//                lastX = x;
//                lastY = y;
//            }
//
//            brightness += colorDelta;
//        }
    }
}