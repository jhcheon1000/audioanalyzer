package com.example.cheon.audioanalyzer;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

public class FreqGeneratorActivity extends AppCompatActivity {

    private int duration = 3;
    private int sampleRate = 8000;
    private int numSamples = duration * sampleRate;
//    private double sample[] = new double[numSamples];
    private double sample[] = null;
    private double freqOfTone = 440;

//    private byte generatedSnd[] = new byte[2 * numSamples];
    private byte generatedSnd[] = null;
    Handler handler = new Handler();

    SeekBar durationBar, rateBar, freqBar;

    TextView durationValue, rateValue, freqValue;

    EditText durationEdit, rateEdit, freqEdit;

    Button playButton, analyzer, setButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.generator);
        durationBar = findViewById(R.id.duration);
        rateBar = findViewById(R.id.sampleRate);
        freqBar = findViewById(R.id.frequency);

        durationValue = findViewById(R.id.duration_value);
        rateValue = findViewById(R.id.sampleRate_value);
        freqValue = findViewById(R.id.freq_value);

        durationEdit = findViewById(R.id.duration_edit);
        rateEdit = findViewById(R.id.sampleRate_edit);
        freqEdit = findViewById(R.id.frequency_edit);

        playButton = findViewById(R.id.gen_play);
        analyzer = findViewById(R.id.analyzer);
        setButton = findViewById(R.id.set_btn);
    }

    @Override
    protected void onStart() {
        super.onStart();


    }

    @Override
    protected void onResume() {
        super.onResume();

        durationBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                durationValue.setText(String.valueOf(progress));
                duration = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                numSamples = duration * sampleRate;
//                sample = new double[numSamples];
//                generatedSnd = new byte[2 * numSamples];
            }
        });

        rateBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                rateValue.setText(String.valueOf(progress));
                sampleRate = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                numSamples = duration * sampleRate;
//                sample = new double[numSamples];
//                generatedSnd = new byte[2 * numSamples];
            }
        });

        freqBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                freqValue.setText(String.valueOf(progress));
                freqOfTone = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                numSamples = duration * sampleRate;
//                sample = new double[numSamples];
//                generatedSnd = new byte[2 * numSamples];
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                genTone();
                playSound();
            }
        });

        analyzer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        getApplicationContext(),
                        MainActivity.class
                );
                startActivity(intent);
            }
        });

        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                duration = Integer.parseInt("" + durationEdit.getText());
                sampleRate = Integer.parseInt("" + rateEdit.getText());
                freqOfTone = Integer.parseInt("" + freqEdit.getText());

                durationValue.setText(String.valueOf(duration));
                rateValue.setText(String.valueOf(sampleRate));
                freqValue.setText(String.valueOf(freqOfTone));
            }
        });

    }

    void genTone() {
        init();
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/freqOfTone));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }

    void playSound(){
        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.play();
    }

    public void init() {
        numSamples = duration * sampleRate;
        sample = new double[numSamples];
        generatedSnd = new byte[2 * numSamples];
    }
}
