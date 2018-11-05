package com.example.cheon.audioanalyzer;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import ca.uol.aig.fftpack.RealDoubleFFT;

public class MainActivity extends AppCompatActivity {
    private String TAG = ".MainActivity";

    private int mAudioSource = MediaRecorder.AudioSource.MIC;
    private int mSampleRate = 44100;
    private int mChannelCount = AudioFormat.CHANNEL_IN_STEREO;
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int mBufferSize = AudioTrack.getMinBufferSize(mSampleRate, mChannelCount, mAudioFormat);
    private int playBufferSize;

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
    int frequency = 44100;
    int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;

    int blockSize = 1024;

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

    public void onGenerate(View v) {
        Intent genIntent = new Intent(
                getApplicationContext(),
                FreqGeneratorActivity.class);
        startActivity(genIntent);
    }

    private class RecordAudio extends AsyncTask<Void, double[], Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            if (isRecording && !isPlaying) {
                try {
                    int specBufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, mAudioFormat);
                    playBufferSize = specBufferSize;
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


                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmSS");

                    String time = dateFormat.format(cal.getTime());
                    String filename = time + "+44100.pcm";

                    File outFile = new File(mFilePath, filename);
                    FileOutputStream fos = null;

                    try {
                        fos = new FileOutputStream(outFile);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    DataOutputStream dos = new DataOutputStream(fos);


//                    boolean timer = false;
                    while (isRecording) {
//                        if(!timer) {
//                            new Timer().schedule(new TimerTask() {
//                                @Override
//                                public void run() {
//                                    runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            isRecording = false;
//                                            mBtnRecord.setText("Record");
//                                        }
//                                    });
//                                }
//                            }, 5000);
//
//                            timer = true;
//                        }
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

                        try {
                            byte[] mByteData = short2byte(mAudioData);
                            fos.write(mByteData, 0, mByteData.length);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

//                        try {
//                            for (int i = 0; i < mAudioData.length; i++) {
//                                dos.writeShort(mAudioData[i]);
//                            }
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
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

//                    File rawFile = new File(mFilePath, filename);
//                    File wavFile = new File(mFilePath, filename);
//                    try {
//                        rawToWave(rawFile, wavFile);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }

                } catch (Throwable t) {
                    Log.e("AudioRecord", "Recording Failed");
                }
            }
            else if (!isRecording && isPlaying) {
//                short[] writeData = new short[blockSize];
                byte[] writeData = new byte[blockSize*2];
                Log.d("playBufferSize", String.valueOf(playBufferSize));
                FileInputStream fis = null;

                Calendar cal = Calendar.getInstance();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmSS");

                String time = dateFormat.format(cal.getTime());
                String filename = time + "+44100.pcm";

                File inFile = new File(mFilePath, filename);

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
                        int ret = dis.read(writeData, 0, blockSize*2);

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

    private byte[] short2byte(short[] s) {
        int shortArrSize = s.length;
        byte[] b = new byte[shortArrSize * 2];

        for (int i = 0; i < shortArrSize; i++) {
            b[i * 2] = (byte) (s[i] & 0x00FF);

            b[(i * 2) + 1] = (byte) (s[i] >> 8);

            s[i] = 0;
        }

        return b;
    }

    private void rawToWave(final File rawFile, final File waveFile) throws IOException {

        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, 44100); // sample rate
            writeInt(output, 44100 * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }

            output.write(fullyReadFileToBytes(rawFile));
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }
    byte[] fullyReadFileToBytes(File f) throws IOException {
        int size = (int) f.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis= new FileInputStream(f);
        try {

            int read = fis.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        }  catch (IOException e){
            throw e;
        } finally {
            fis.close();
        }

        return bytes;
    }
    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
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
