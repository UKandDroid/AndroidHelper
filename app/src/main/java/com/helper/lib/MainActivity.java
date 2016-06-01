package com.helper.lib;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.helper.R;

public class MainActivity extends AppCompatActivity {
    Flow testFlow;
    ViewHelper vhelp;
    public static final int VERIFIED = 4;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        vhelp = new ViewHelper(findViewById(android.R.id.content));
    }



    @Override
    protected void onPostResume() {
        super.onPostResume();
        R.id Id = new R.id();
        testFlow = new Flow(code);
        testFlow.registerEvents(VERIFIED,  new String[]{"name", "email", "agreed"});
        testFlow.registerEventsUI(0, findViewById(Id.edit_name), Flow.Event.TEXT_CHANGE);
        testFlow.registerEventsUI(1, findViewById(Id.edit_email), Flow.Event.TEXT_CHANGE);
        testFlow.registerEventsUI(2, findViewById(Id.btn_hello), Flow.Event.ON_CLICK);
        vhelp.setKeyboardListener();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    @Override
    protected void onPause() {
        super.onPause();
        testFlow.stop();
    }

    @Override
    protected void onStop() {
        super.onStop();


    }

    Flow.Code code = new Flow.Code() {
        @Override public void onAction(int iStep, boolean bSuccess, int iExtra, Object obj) {
            switch (iStep){
                case 0:
                    EditText txt = ((EditText)(obj));
                    testFlow.event("name", txt.getText().length() > 0);
                    Log.d("Flow", "name length: "+ txt.getText().length());
                    break;
                case 1:
                    txt = ((EditText)(obj));
                    testFlow.event("email", txt.getText().length() > 0);
                    Log.d("Flow", "email length: "+ txt.getText().length());
                    break;
                case 2:
                    testFlow.event("agreed");
                    Log.d("Flow", "Agreed Press " + bSuccess );
                    break;
                case VERIFIED:
                    Log.d("Flow", "Verified: " + bSuccess );
                    break;
            }
        }
    };
}
