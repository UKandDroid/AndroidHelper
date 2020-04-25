package com.helper

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ToggleButton
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

        val button = findViewById<ToggleButton>(R.id.toggleButton)
        button.setOnCheckedChangeListener { buttonView, isChecked ->  {

        } }

        flow.registerClick(1, findViewById(R.id.btn_one)){ }
        flow.registerUiEvent(2, findViewById(R.id.btn_one), UiFlow.UiEvent.KEYBOARD_STATE_CHANGE ){}
        flow.registerUiEvent(3, findViewById(R.id.edit_one), UiFlow.UiEvent.TEXT_ENTERED){}
        flow.registerUiEvent(4, findViewById(R.id.toggleButton), UiFlow.UiEvent.ON_TOGGLE){
           log.d("toggle: $it")
        }
        flow.registerUiEvent(5, findViewById(R.id.switch1), UiFlow.UiEvent.ON_SWITCH){
            log.d("switch: $it")
        }


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