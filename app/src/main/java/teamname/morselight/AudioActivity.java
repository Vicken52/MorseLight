/*
http://developer.android.com/guide/topics/media/audio-capture.html
 */
package teamname.morselight;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

import java.util.Calendar;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class AudioActivity extends ActionBarActivity {

    private String fileName;
    private Button recordButton, playButton, detectButton;

    private MediaRecorder recorder;
    private MediaPlayer player;
    private boolean isRecording, isPlaying;

    /*==================================================
    * Detecting beep sound using TarsosDSP
    *
     ====================================================*/
    long startTime, endTime, currentTime, duration, silenceStart, silenceDuration;
    float pitch, threshold;
    boolean gotSound, isDetecting, needUIUpdate;
    AudioDispatcher dispatcher;
    PitchDetectionHandler pdh;
    AudioProcessor p;
    private  void onRecord(){
        if (isRecording) {
            recordButton.setText("Start Recording");
            stopRecording();
        }else {
            recordButton.setText("Stop Recording");
            startRecording();
        }
        isRecording = !isRecording;
    }

    private void onPlaying(){
        if (isPlaying) {
            playButton.setText("Start Playing");
            stopPlaying();
        }else {
            playButton.setText("Stop Playing");
            startPlaying();
        }
        isPlaying = !isPlaying;
    }
    private void startRecording(){
        // create MediaRecorder to get the audio
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
        }catch (IOException e){
            Log.e("", "recorder prepare failed");
        }
        recorder.start();
    }

    private void stopRecording(){
        if (!isRecording)
            return;
        recorder.stop();
        recorder.release();
        recorder = null;
    }


    private void startPlaying(){
        player = new MediaPlayer();
        try{
            player.setDataSource(fileName);
            player.prepare();
            player.start();
        }catch (IOException e){
            Log.e("", "startPlaying error at setDataSource()");
        }
    }

    private void stopPlaying(){
        if (!isPlaying)
            return;
        player.release();
        player = null;
    }

    private void onDetecting(){
        if (!isDetecting) {
            detectButton.setText("STOP DETECTING");
            isDetecting = true;
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);
            pitch = -1;
            threshold = 900;
            duration = 0;
            gotSound = false;
            silenceStart = Calendar.getInstance().getTimeInMillis();
            if (pdh == null) {
                pdh = new PitchDetectionHandler() {
                    @Override
                    public void handlePitch(PitchDetectionResult result, AudioEvent e) {
                        final float pitchInHz = result.getPitch();
                        if ((pitchInHz > threshold) && !gotSound) {
                            startTime = Calendar.getInstance().getTimeInMillis();
                            silenceDuration = startTime - silenceStart;
                            gotSound = true;
                            needUIUpdate = true;
                        } else if ((pitchInHz < threshold) && gotSound) {
                            currentTime = Calendar.getInstance().getTimeInMillis();
                            duration = currentTime - startTime;
                            startTime = currentTime;
                            gotSound = false;
                            silenceStart = currentTime;
                            needUIUpdate = true;
                        }
                        if (needUIUpdate) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TextView text = (TextView) findViewById(R.id.pitchTv);
                                    text.setText("Pitch (Hz): " + pitchInHz);
                                    if (!gotSound) {
                                        TextView tv = (TextView) findViewById(R.id.soundTv);
                                        tv.setText("Sound duration: " + duration);
                                        EditText morseEt = (EditText) findViewById(R.id.morseEt);
                                        morseEt.append(beepToMorse(duration));
                                    } else {
                                        TextView tv = (TextView) findViewById(R.id.silenceTv);
                                        tv.setText("Silence duration" + silenceDuration);
                                        if (isNewChar(silenceDuration))
                                            tv.append(" ");
                                        else if (isNewWord(silenceDuration))
                                            tv.append(" / ");
                                    }
                                    needUIUpdate = false;
                                }
                            });
                        }
                    }
                };
            }
            if (p == null)
                p = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, pdh);
            dispatcher.addAudioProcessor(p);
            new Thread(dispatcher, "Audio Dispatcher").start();
        }else{
            isDetecting = false;
            detectButton.setText("START DETECTING");
            if (dispatcher != null) {
                dispatcher.removeAudioProcessor(p);
                dispatcher.stop();  // this will stop audio processing
            }
        }
    }

    private boolean isNewWord(long ms){
        if (ms > 430l && ms < 600l)
            return true;
        return false;
    }

    private boolean isNewChar(long ms){
        if (ms > 930l && ms < 1200l)
            return true;
        return false;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

        fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/audio.3gp";
        isPlaying = false;
        isRecording = false;
        isDetecting = false;
        needUIUpdate = false;
        // get buttons
        recordButton = (Button)findViewById(R.id.recordB);
        playButton = (Button)findViewById(R.id.playB);
        detectButton = (Button)findViewById(R.id.detectB);

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRecord();
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlaying();
            }
        });

        detectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDetecting();
            }
        });
    }

    private String beepToMorse(long ms){
        if (ms > 180l && ms < 300l)
            return ".";
        if (ms > 600l && ms < 850l)
            return "-";
        return "";
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_audio, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
