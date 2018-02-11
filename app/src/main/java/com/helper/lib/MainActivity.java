package com.helper.lib;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.helper.R;


public class MainActivity extends AppCompatActivity {
    WakeTimer wake;
    public boolean bFlip = true;
    Logger log = new Logger();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View v = findViewById(R.id.btn_hello);
        wake = WakeTimer.init(this, new Flow.Code() {
            @Override public void onAction(int iAction, boolean bSuccess, int iExtra, Object data) {

            }
        });
        log.d("testing");
        wake.runRepeat(1, 1000L*20, "one");
        wake.runRepeat(2, 1000L*18, "two");
    }


    @Override protected void onPostResume() {
        super.onPostResume();

    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onStop() {
        super.onStop();


    }

    public void onClick(View v){
        if(bFlip)
            wake.runRepeat(1, 10* 1000L, "one");
        else
            wake.runRepeat(1, 20* 1000L, "one");
        bFlip = !bFlip;
    }


}
