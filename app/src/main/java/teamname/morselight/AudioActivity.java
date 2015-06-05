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
    private Button detectButton;

    private TextView decodedMessage;

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


    private void onDetecting(){
        if (!isDetecting) {
            detectButton.setText("STOP DETECTING");

            decodedMessage = (TextView) findViewById(R.id.decodeSoundMessage);
            decodedMessage.setText("");

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
                                        // This is where it starts to decode the message and show it.
                                        decodedMessage.setText(MorseCode.decode(morseEt.getText().toString()));
                                    } else {
                                        TextView tv = (TextView) findViewById(R.id.silenceTv);
                                        tv.setText("Silence duration" + silenceDuration);
                                        EditText morseEt = (EditText) findViewById(R.id.morseEt);
                                        if (isNewChar(silenceDuration)){
                                            morseEt.append(" ");
                                        }
                                        if (isNewWord(silenceDuration))
                                            morseEt.append(" / ");
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
        if (ms > 1900l && ms < 2800l)
            return true;
        return false;
    }

    private boolean isNewChar(long ms){
        if (ms > 600l && ms < 1400l)
            return true;
        return false;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

        fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/audio.3gp";
        isDetecting = false;
        needUIUpdate = false;
        // get buttons
        detectButton = (Button)findViewById(R.id.detectB);

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
