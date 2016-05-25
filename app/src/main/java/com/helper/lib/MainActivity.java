package com.helper.lib;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.helper.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        R.id Id = new R.id();
        setContentView(R.layout.activity_main);
        final ViewHelper layout = new ViewHelper(findViewById(R.id.lay_linear));
        layout.setViewText(Id.txt_hello, "Changed");
        String str = layout.getViewText(R.id.txt_hello);
        layout.setViewText(R.id.edit_hello, str);
        layout.setViewText(R.id.btn_hello, str);
        layout.setViewText(Id.txt_hello, "Changed");
         str = layout.getViewText(R.id.txt_hello);
        layout.setViewText(R.id.edit_hello, str);
        layout.setViewText(R.id.btn_hello, str);
        layout.setViewText(Id.txt_hello, "Changed");
         str = layout.getViewText(R.id.txt_hello);
        layout.setViewText(R.id.edit_hello, str);
        layout.setViewText(R.id.btn_hello, str);
        layout.setViewText(Id.txt_hello, "Changed");
         str = layout.getViewText(R.id.txt_hello);
        layout.setViewText(R.id.edit_hello, str);
        layout.setViewText(R.id.btn_hello, str);
        layout.setViewText(Id.txt_hello, "Changed");
         str = layout.getViewText(R.id.txt_hello);
        layout.setViewText(R.id.edit_hello, str);
        layout.setViewText(R.id.btn_hello, str);
        layout.setBackground(R.id.edit_hello, R.drawable.abc_btn_check_material);


        Utils uObj = new Utils();

     /*   layout.setOnClickListener(R.id.btn_hello, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Button clicked", Toast.LENGTH_SHORT).show();
            }
        });
        layout.updateUI(this, new ViewHelper.UIThread() {
            @Override
            public void execute() {

            }
        });
*/
        Button btn = layout.button(R.id.btn_hello);
        TextView text =  layout.textView(R.id.txt_hello);

     //   Log.d("JsonHelper", "Time: "+ layout.stopTimer());
        JsonHelper help = new JsonHelper();
        //   help.loadJSONFile(this, "personas.js");

        //   boolean bActive = help.getBoolean("persona[0].poster[0].is_active");
        //   help.setBoolean("persona[0].poster[0].is_active", true);
        //   boolean bSuccess = help.getBoolean("persona[0].poster[0].is_active");
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
