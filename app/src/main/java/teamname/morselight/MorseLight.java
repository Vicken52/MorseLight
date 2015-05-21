package teamname.morselight;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;


public class MorseLight extends ActionBarActivity {
    TextWatcher input = null;
    private EditText plain;
    private TextView morse, decode;
    final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);

    //Detect low battery, and create a warning DialogInterface
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            if (batteryLevel < 10) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("The battery is getting low.Less than " + batteryLevel
                        + "% is remaining. The flash is disabled."
                        + "Please charge your device so you can use the flash again.");
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

        plain = (EditText) findViewById(R.id.PlainText);
        morse = (TextView) findViewById(R.id.MorseCode);
        decode = (TextView) findViewById(R.id.MorseCodeDecode);

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
                tg.startTone(ToneGenerator.TONE_DTMF_A, 250);
            }
        };
        plain.addTextChangedListener(input);

        //Display low battery warning DialogInterface
        this.registerReceiver(this.mBatInfoReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
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
