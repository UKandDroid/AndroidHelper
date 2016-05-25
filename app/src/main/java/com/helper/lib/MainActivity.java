package com.helper.lib;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.helper.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        R.id Id = new R.id();
        setContentView(R.layout.activity_main);

        Flow testFlow  = new Flow(new Flow.CodeFlow() {
            @Override public void code(int iStep, boolean bSuccess, int iExtra, Object obj) {
                switch (iStep){
                    case 0:
                        Log.d("Flow", "Code 0 executed "+ bSuccess);
                        break;
                    case 1:
                        Log.d("Flow", "Code 1 executed "+ bSuccess);
                        Toast.makeText(MainActivity.this, "Code executed ", Toast.LENGTH_SHORT).show();              break;
                    case 2:
                        Log.d("Flow", "Code 2 executed " + bSuccess );
                        break;
                }
            }
        });

        testFlow.waitForEvent(2, new String[]{"event_one", "event_two"});
        testFlow.run(0);
        testFlow.run(1);
        testFlow.event("event_one");
        testFlow.event("event_two");
        testFlow.event("event_two", false);
        testFlow.event("event_two", true);


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
}
