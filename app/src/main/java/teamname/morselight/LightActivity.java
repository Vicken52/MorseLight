package teamname.morselight;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
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
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;


public class LightActivity extends ActionBarActivity {
    TextWatcher input = null;
    private EditText plain;
    private TextView morse, decode;
    private Button button;
    private String encode = "";
    private AudioManager aManager = null;
    private Camera mCamera;
    private CameraPreview mPreview;
    private boolean cameraFront = false;
    private float volume = 0.0f;
    final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
    private MediaPlayer b = null, l = null;

    // Detect low battery level and create a DialogInterface warning
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            if (batteryLevel <= 10) {
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
                                MorseLight.switch1 = (Switch) findViewById(R.id.switch1);
                                MorseLight.switch1.setEnabled(false);
                                Intent intent = new Intent(LightActivity.this, MorseLight.class);
                                startActivity(intent);
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        //setContentView(R.layout.activity_light);
        try {
            if (mCamera == null){
                mCamera = getCameraInstance();
            }
            mPreview.resume(mCamera);
            this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int findFrontFacingCamera(){
        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for(int i = 0; i < numberOfCameras; i++){
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if(info.facing == CameraInfo.CAMERA_FACING_FRONT){
                cameraId = i;
                cameraFront = true;
                break;
            }
        }
        return cameraId;
    }

    private int findBackFacingCamear(){
        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for(int i = 0; i < numberOfCameras; i++){
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if(info.facing == CameraInfo.CAMERA_FACING_BACK){
                cameraId = i;
                cameraFront = false;
                break;
            }
        }
        return cameraId;
    }

    private boolean checkCameraHardware(Context context){
        if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            return true;
        }else{
            return false;
        }
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light);

        plain = (EditText)findViewById(R.id.PlainText);
        morse = (TextView) findViewById(R.id.MorseCode);
        decode = (TextView) findViewById(R.id.MorseCodeDecode);
        button = (Button) findViewById(R.id.button);
        button.requestFocus();
        aManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        volume = (float) aManager.getStreamVolume(AudioManager.STREAM_RING);

        createBeep();
        createLongBeep();

        if (checkCameraHardware(this.getApplicationContext())){
            mCamera = getCameraInstance();
            mPreview = new CameraPreview(this, mCamera);
            FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
            preview.addView(mPreview);
        }else{
            Log.v("CAMERA", "did not load the camera");
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
                if (!text.isEmpty()) {
                    MorseCode code = new MorseCode();
                    encode = code.encode(text);
                    if (aManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                        long duration = 0;
                        new Thread(new Runnable() {
                            public void run() {
                                playSounds(encode);
                            }
                        }).start();
                    } else {
                        Toast.makeText(getApplicationContext(), "You cannot play tone while phone is silent", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        // Display the low battery warning DialogInterface
        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(this.mBatInfoReceiver);
        mPreview.pause();
        mCamera.release();
        mCamera = null;
    }

    public void playSounds(String text){
        int delay = 0;
        float maxVol = 10*.01f;
        maxVol = (float) aManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
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
                    SystemClock.sleep(750);
                }
            }
        }
    }

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
        getMenuInflater().inflate(R.menu.menu_light, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_help_light) {
            Intent intent = new Intent(LightActivity.this, Help.class);
            mCamera.release();
            startActivity(intent);
        } else if (id == R.id.action_about_light) {
            Intent intent = new Intent(LightActivity.this, About.class);
            mCamera.release();
            startActivity(intent);
        } else if (id == R.id.encode_setting_light){
            Intent intent = new Intent(LightActivity.this, MorseLight.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }
}
