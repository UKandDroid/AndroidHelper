package com.helper.lib;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.helper.R;


public class MainActivity extends AppCompatActivity {
    Flow testFlow;
    Wake wake;
    public static final int VERIFIED = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wake =  Wake.init(this, new Flow.Code() {
            @Override public void onAction(int iAction, boolean bSuccess, int iExtra, Object data) {
                Log.w("Wake", "TIMER Action : "+ iAction);
            }
        });
        int iAction = wake.pendingCount();

        wake.runDelayed(++iAction, 15 * 1000L);
        wake.runDelayed(++iAction, 20 * 1000L);
        wake.runDelayed(++iAction, 25 * 1000L);

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



}
