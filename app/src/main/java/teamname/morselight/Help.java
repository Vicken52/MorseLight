package teamname.morselight;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class Help extends ActionBarActivity {

    private Intent intent;
    private String back = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        intent = getIntent();
        if(intent.getStringExtra("backButton").isEmpty()){
            back = "main";
        }else{
            back = intent.getStringExtra("backButton");
        }

        String[] items = {"What is morse code", "Morse code chart",
                "How to use MorseLight"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.activity_help_row, items);
        ListView list = (ListView) findViewById(R.id.listView_help);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent;
                switch (position) {
                    case 0:
                        intent = new Intent(Help.this, Help1.class);
                        startActivity(intent);
                        break;
                    case 1:
                        intent = new Intent(Help.this, Help2.class);
                        startActivity(intent);
                        break;
                    case 2:
                        intent = new Intent(Help.this, Help3.class);
                        startActivity(intent);
                        break;
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_help, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_settings) {
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent i;
        switch(back){
            case "main":
                i = new Intent(Help.this, MorseLight.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                break;
            case "light":
                i = new Intent(Help.this, LightActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(i);
                break;
            case "audio":
                i = new Intent(Help.this, AudioActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(i);
                break;
        }
    }
}
