package com.helper.lib;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;

import com.helper.R;


public class MainActivity extends AppCompatActivity {
    Flow testFlow;
    Wake wake;
    public static final int VERIFIED = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View v = findViewById(R.id.btn_hello);
        Anim anim1 = new Anim(v);
        anim1.addAnimation(Anim.TYPE_TRANSLATE_X, Anim.INTER_LINEAR, 0, 200, 3000, 1000);
        anim1.addAnimation(Anim.TYPE_ROTATE, Anim.INTER_LINEAR, 0, 360, 3000, 1000);
        anim1.addAnimation(Anim.TYPE_TRANSLATE_X, Anim.INTER_LINEAR, 0, 200, 1000, 0);

        anim1.start();

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
