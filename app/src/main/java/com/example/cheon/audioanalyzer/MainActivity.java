package com.example.cheon.audioanalyzer;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import ca.uol.aig.fftpack.RealDoubleFFT;

public class MainActivity extends AppCompatActivity {
    private String TAG = ".MainActivity";

    private int mAudioSource = MediaRecorder.AudioSource.MIC;
    private int mSampleRate = 44100;
    private int mChannelCount = AudioFormat.CHANNEL_IN_STEREO;
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int mBufferSize = AudioTrack.getMinBufferSize(mSampleRate, mChannelCount, mAudioFormat);

    private short[] mAudioData;
    private String mDecibelFormat;

    private WaveformView mWaveformView;
    private TextView mDecibelView;


    public AudioRecord mAudioRecord = null;
    public boolean isRecording = false;

    public AudioTrack mAudioTrack = null;
    public boolean isPlaying = false;

    Button mBtnRecord;
    Button mBtnPlay;

    String mFilePath;

    //==================================================================================================
//for fftpack

    private RealDoubleFFT transformer;
    int frequency = 8000;
    int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;

    int blockSize = 256;

    RecordAudio recordTask;

    ImageView imageView;
    Bitmap bitmap;
    Canvas canvas;
    Paint paint;


//==================================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//      for fft (fast fourier transform)
        transformer = new RealDoubleFFT(blockSize);
        imageView = (ImageView) findViewById(R.id.fft_graph);
        bitmap = Bitmap.createBitmap((int) 256, (int) 100, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        paint.setColor(Color.GREEN);
        imageView.setImageBitmap(bitmap);
        mAudioData = new short[blockSize];

//      for getting waveform
        mBtnRecord = (Button) findViewById(R.id.record_button);
        mBtnPlay = (Button) findViewById(R.id.play_button);
        mDecibelFormat = getResources().getString(R.string.decibel_format);
        mWaveformView = (WaveformView) findViewById(R.id.waveform_view);
        mDecibelView = (TextView) findViewById(R.id.decibel_view);

        requestRecordAudioPermission();


    }

    public void onRecord(View v) {
        if (isRecording == true) {
            isRecording = false;
            mBtnRecord.setText("Record");

            recordTask.cancel(true);
        } else {
            isRecording = true;
            mBtnRecord.setText("Stop");

            recordTask = new RecordAudio();
            recordTask.execute();
        }
    }

    public void onPlay(View v) {
        if (isPlaying == true) {
            isPlaying = false;
            mBtnPlay.setText("Play");

            recordTask.cancel(true);
        } else {
            isPlaying = true;
            mBtnPlay.setText("Stop");

            if (mAudioTrack == null) {
                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate, mChannelCount, mAudioFormat, mBufferSize, AudioTrack.MODE_STREAM);
            }

            recordTask = new RecordAudio();
            recordTask.execute();
        }
    }

    private class RecordAudio extends AsyncTask<Void, double[], Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            if (isRecording && !isPlaying) {
                try {
                    int specBufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, mAudioFormat);
                    Log.d("specBufferSize", String.valueOf(specBufferSize));
                    mAudioRecord = new AudioRecord(mAudioSource, frequency, channelConfiguration, mAudioFormat, specBufferSize);

                    mAudioData = new short[blockSize];
                    double[] toTransform = new double[blockSize];

                    mAudioRecord.startRecording();

                    mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recordtest";
                    File path = new File(mFilePath);
                    if (!path.exists()) {
                        path.mkdirs();
                    }

                    File outFile = new File(mFilePath, "record.pcm");
                    FileOutputStream fos = null;

                    try {
                        fos = new FileOutputStream(outFile);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    DataOutputStream dos = new DataOutputStream(fos);

                    while (isRecording) {
                        int ret = mAudioRecord.read(mAudioData, 0, blockSize);

//                      update waveform and decibel value
                        mWaveformView.updateAudioData(mAudioData);
                        updateDecibelLevel();

                        for (int i = 0; i < blockSize && i < ret; i++) {
                            toTransform[i] = (double) mAudioData[i] / Short.MAX_VALUE;
                        }

                        transformer.ft(toTransform);
                        publishProgress(toTransform);

                        Log.d(TAG, "read bytes is " + ret);

//                        put byte array instead of mAudioData (short array)
//                        try {
//                            fos.write(mAudioData, 0, mBufferSize);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }

                        try {
                            Log.d("DATA LENGTH", String.valueOf(mAudioData.length));
                            for (int i = 0; i < mAudioData.length; i++) {
                                dos.writeShort(mAudioData[i]);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    mAudioRecord.stop();
                    mAudioRecord.release();
                    mAudioRecord = null;
                    mAudioData = null;

                    recordTask = null;

                    try {
                        dos.close();
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } catch (Throwable t) {
                    Log.e("AudioRecord", "Recording Failed");
                }
            }
            else if (!isRecording && isPlaying) {
                int playBufferSize = AudioTrack.getMinBufferSize(frequency, AudioFormat.CHANNEL_OUT_MONO, mAudioFormat);
//                short[] writeData = new short[blockSize];
                byte[] writeData = new byte[blockSize * 2];
                Log.d("playBufferSize", String.valueOf(playBufferSize));
                FileInputStream fis = null;
                File inFile = new File(mFilePath, "record.pcm");

                try {
                    fis = new FileInputStream(inFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                DataInputStream dis = new DataInputStream(fis);

                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, frequency, AudioFormat.CHANNEL_OUT_MONO, mAudioFormat, playBufferSize, AudioTrack.MODE_STREAM);
                mAudioTrack.play();

                while (isPlaying) {
                    try {
                        int ret = dis.read(writeData, 0, blockSize * 2);

//                        for (int i = 0; i < 256; i++) {
//                            writeData[i] = dis.readShort();
//                        }

                        if (ret <= 0) {
                            (MainActivity.this).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    isPlaying = false;
                                    mBtnPlay.setText("Play");
                                }
                            });
                            break;
                        }

                        mAudioTrack.write(writeData, 0, ret);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                mAudioTrack.stop();
                mAudioTrack.release();
                mAudioTrack = null;

                recordTask = null;

                try {
                    dis.close();
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(double[]... toTransform) {
            super.onProgressUpdate(toTransform);

            canvas.drawColor(Color.BLACK);

            for (int i = 0; i < toTransform[0].length; i++) {
                int x = i;
                int downy = (int) (100 - (toTransform[0][i] * 10));
                int upy = 100;

                canvas.drawLine(x, downy, x, upy, paint);
            }

            imageView.invalidate();
        }
    }


    private void updateDecibelLevel() {
        double sum = 0;

        for (short rawSample : mAudioData) {
            double sample = rawSample / 32768.0;
            sum += sample * sample;
        }

        double rms = Math.sqrt(sum / mAudioData.length);
        final double db = 20 * Math.log10(rms);

        mDecibelView.post(new Runnable() {
            @Override
            public void run() {
                mDecibelView.setText(String.format(mDecibelFormat, db));
            }
        });
    }

    private void requestRecordAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Permission to access the microphone, external storage is required for this app to record audio and save the .pcm file.")
                        .setTitle("Permission required");

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
            }
        } else {
            recordTask = new RecordAudio();
            recordTask.execute();
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == 101 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults.length > 0) {
            Log.i(TAG, "Permission has been granted by user");

            recordTask = new RecordAudio();
            recordTask.execute();
        } else {
            Log.i(TAG, "Permission has been denied by user");
        }

    }
}
