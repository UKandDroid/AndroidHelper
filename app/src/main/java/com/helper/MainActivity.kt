package com.helper

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.helper.lib.*

class MainActivity : AppCompatActivity() {
    var wake: WakeTimer? = null
    var log = Logger("FlowTest")
    var bFlip = true



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val anim = Anim()

        val flow = UiFlow(object : UiFlow.Code {
            override fun onAction(action: Int, bSuccess: Boolean, iExtra: Int, tag: Any) {
                Log.d("android-helper", "Action:$action bSuccess:$bSuccess")
            }
        });

        flow.registerClick(1, findViewById(R.id.btn_one)){ }
        flow.registerUiEvent(2, findViewById(R.id.btn_one), UiFlow.UiEvent.KEYBOARD_STATE_CHANGE ){}
        flow.registerUiEvent(3, findViewById(R.id.edit_one), UiFlow.UiEvent.TEXT_ENTERED){}


    }

    override fun onPostResume() {
        super.onPostResume()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }


}