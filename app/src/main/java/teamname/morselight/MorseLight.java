package teamname.morselight;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;


public class MorseLight extends ActionBarActivity {
    TextWatcher input = null;
    private boolean light, isDialogShowed;
    private EditText plain;
    private TextView morse, decode;
    private Button button;
    static Switch switch1;
    private String encode = "";
    final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
    private MediaPlayer b = null, l = null, p = null;
    boolean isLighOn = false;
    Camera camera = Camera.open();
    Parameters parameters = camera.getParameters();

    // Detect low battery level and create a DialogInterface warning
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            if (batteryLevel <= 10 && !isDialogShowed) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Battery is low. " + batteryLevel
                        + "% of battery remaining. "
                        + "The light is disabled. "
                        + "Please charge the device in order to use the light again.");
                builder.setCancelable(true);
                builder.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                switch1 = (Switch) findViewById(R.id.switch1);
                                switch1.setEnabled(false);
                                switch1.setChecked(false);  // force it to sound
                            }
                        });
                AlertDialog alert = builder.create();
                isDialogShowed = true;
                alert.show();
            }
            else if (batteryLevel > 10) {
                switch1 = (Switch)findViewById(R.id.switch1);
                switch1.setEnabled(true);
                isDialogShowed = false;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_morse_light);
        light = false;
        plain = (EditText) findViewById(R.id.PlainText);
        morse = (TextView) findViewById(R.id.MorseCode);
        decode = (TextView) findViewById(R.id.MorseCodeDecode);
        button = (Button) findViewById(R.id.button);
        switch1 = (Switch) findViewById(R.id.switch1);

        createBeep();
        createLongBeep();
        createPause();
        isDialogShowed = false;
        if(getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
        {
            //Toast.makeText(getApplicationContext(), "Flash Detected!!", Toast.LENGTH_LONG).show();
            try {
                camera = Camera.open();
                parameters = camera.getParameters();
            } catch (RuntimeException e) {
                Log.e("Camera error: ", e.getMessage());
            }
        }
        else
        {
            //Toast.makeText(getApplicationContext(), "No Flash Detected!!", Toast.LENGTH_LONG).show();
        }

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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

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
                if (!text.isEmpty()) {
                    MorseCode code = new MorseCode();
                    encode = code.encode(text);
                    long duration = 0;

                    if (light) {
                        if(getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                            if (camera == null) { // check if camera is available
                                camera = getCameraInstance();
                                parameters = camera.getParameters();
                            }
                            new Thread(new Runnable() {
                                public void run() {
                                    playLights(encode);
                                }
                            }).start();
                        }else{
                            Toast.makeText(MorseLight.this, "Unable to detect your flash. Switching back to sound.", Toast.LENGTH_LONG).show();
                            switch1.setChecked(false);
                        }
                    } else {
                        new Thread(new Runnable() {
                            public void run() {
                                playSounds(encode);
                            }
                        }).start();
                    }
                }
            }
        });

        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    switch1.setText("");
                    light = true;
                }
                else
                {
                    switch1.setText("");
                    light = false;
                }
            }
        });

        // Display the low battery warning DialogInterface
        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    public void playSounds(String text) {
        int delay = 0;
        float maxVol = 10 * .01f;
        for (int i = 0; i < text.length(); i++) {
            if (b.isPlaying() || l.isPlaying()) {
                i--;
            } else {
                if (text.charAt(i) == '.') {
                    b.setVolume(maxVol, maxVol);
                    b.start();
                } else if (text.charAt(i) == '-') {
                    l.setVolume(maxVol, maxVol);
                    l.start();
                } else if (text.charAt(i) == '/') {
                    //p.seekTo(0);
                    //p.start();
                    //delay = 2000;
                    SystemClock.sleep(500);
                } else if (text.charAt(i) == ' ') {
                    SystemClock.sleep(300);
                }
            }
        }
    }

    public void playLights(String text) {
        for (int i = 0; i < text.length(); i++)
        {
            long startTime = Calendar.getInstance().getTimeInMillis();
            if (text.charAt(i) == '.')
            {
                try {
                    parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
                    camera.setParameters(parameters);
                    camera.startPreview();
                    Thread.sleep(350, 0);
                    parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                    camera.setParameters(parameters);
                    camera.stopPreview();
                    SystemClock.sleep(700);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else if (text.charAt(i) == '-')
            {
                try {
                    parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
                    camera.setParameters(parameters);
                    camera.startPreview();
                    Thread.sleep(750, 0);
                    parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                    camera.setParameters(parameters);
                    camera.stopPreview();
                    SystemClock.sleep(700);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else if (text.charAt(i) == '/')
            {
                //p.seekTo(0);
                //p.start();
                //delay = 2000;
                SystemClock.sleep(500);
            }
            else if (text.charAt(i) == ' ')
            {
                SystemClock.sleep(300);
                Log.d("morse", "flash: white space");
            }
            long duration = Calendar.getInstance().getTimeInMillis() - startTime;
            Log.d("morse", "flash: this char time " + duration);
        }
    }

    Runnable stopPlayerTask = new Runnable() {
        @Override
        public void run() {
            p.stop();
        }
    };

    public void createBeep() {
        float maxVol = 75 * .01f;
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

    public void createPause() {
        float maxVol = 75 * .01f;
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
        float maxVol = 75 * .01f;
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
        if (id == R.id.action_help) {
            Intent intent = new Intent(MorseLight.this, Help.class);
            intent.putExtra("backButton", "main");
            startActivity(intent);
        } else if (id == R.id.action_about) {
            Intent intent = new Intent(MorseLight.this, About.class);
            intent.putExtra("backButton", "main");
            startActivity(intent);
        } else if (id == R.id.decode_setting) {
            boolean testing = false;
            if (testing) {
                if (camera != null) {
                    camera.release();
                    camera = null;
                }
                Intent intent = new Intent(MorseLight.this, LightActivity.class);
                //intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
            } else {
                Intent intent = new Intent(MorseLight.this, AudioActivity.class);
                //intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("HELP", "this is where the onStop is played");
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("HELP", "this is where the onRestart is played");
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try{
            c = Camera.open();
            Camera.Parameters param = c.getParameters();
            Log.v("param", param.toString());
        } catch(Exception e){

        }
        return c;
    }
}
