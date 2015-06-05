package teamname.morselight;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class MorseLight extends ActionBarActivity {
    TextWatcher input = null;
    private EditText plain;
    private TextView morse, decode;
    private Button button;
    private String encode = "";
    final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
    private MediaPlayer b = null, l = null, p = null;

    // Detect low battery level and create a DialogInterface warning
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            int batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            if (batteryLevel <= 10) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Battery is low. " + batteryLevel
                        + "% of battery remaining. "
                        + "The flash is disabled. Please charge the device.");
                builder.setCancelable(true);
                builder.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog alert = builder.create();
                alert.show();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_morse_light);

        plain = (EditText)findViewById(R.id.PlainText);
        morse = (TextView) findViewById(R.id.MorseCode);
        decode = (TextView) findViewById(R.id.MorseCodeDecode);
        button = (Button) findViewById(R.id.button);

        createBeep();
        createLongBeep();
        createPause();

        //tg.startTone(ToneGenerator.TONE_PROP_BEEP);
        input = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                MorseCode code = new MorseCode();
                String result = code.encode(plain.getText().toString());
                morse.setText(result);
                decode.setText(code.decode(result));
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable s) {
                //tg.startTone(ToneGenerator.TONE_DTMF_A, 250);
                //b.start();
            }
        };
        plain.addTextChangedListener(input);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = plain.getText().toString().trim();
                encode = "";
                if (!text.isEmpty()){
                    MorseCode code = new MorseCode();
                    encode = code.encode(text);
                    long duration = 0;
                    new Thread(new Runnable(){
                        public void run() {
                            playSounds(encode);
                        }
                    }).start();
                }
            }
        });

        // Display the low battery warning DialogInterface
        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    public void playSounds(String text){
        int delay = 0;
        float maxVol = 10*.01f;
        for(int i = 0; i < text.length(); i++){
            if(b.isPlaying() || l.isPlaying()){
                i--;
            }else{
                if(text.charAt(i) == '.'){
                    b.setVolume(maxVol, maxVol);
                    b.start();
                }else if (text.charAt(i) == '-'){
                    l.setVolume(maxVol, maxVol);
                    l.start();
                }else if (text.charAt(i) == '/'){
                    //p.seekTo(0);
                    //p.start();
                    //delay = 2000;
                    SystemClock.sleep(500);
                }else if (text.charAt(i) == ' '){
                    SystemClock.sleep(300);
                }
            }
        }
    }

    Runnable stopPlayerTask = new Runnable(){
        @Override
        public void run() {
            p.stop();
        }};
    public void createBeep() {
        float maxVol = 75*.01f;
        try {
            b = new MediaPlayer();

            AssetFileDescriptor descriptor = getAssets().openFd("censor-beep-1.mp3");
            b.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();

            b.prepare();
            b.setVolume(maxVol, maxVol);
            b.setLooping(false);
            //b.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createPause(){
        float maxVol = 75*.01f;
        try {
            p = new MediaPlayer();

            AssetFileDescriptor descriptor = getAssets().openFd("pause.wav");
            p.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();

            p.prepare();
            p.setVolume(maxVol, maxVol);
            p.setLooping(false);
            //b.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createLongBeep() {
        float maxVol = 75*.01f;
        try {
            l = new MediaPlayer();

            AssetFileDescriptor descriptor = getAssets().openFd("censor-beep-3.wav");
            l.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();

            l.prepare();
            l.setVolume(maxVol, maxVol);
            l.setLooping(false);
            //l.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_morse_light, menu);
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
