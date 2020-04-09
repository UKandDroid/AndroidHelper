package com.helper

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.helper.lib.Anim
import com.helper.lib.Flow
import com.helper.lib.Logger
import com.helper.lib.WakeTimer

class MainActivity : AppCompatActivity() {
    var wake: WakeTimer? = null
    var log = Logger("FlowTest")
    var bFlip = true


    enum class  Events{
        EVENT_ONE, EVENT_TWO
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val anim = Anim()

        val flow = Flow<Events>()

       // flow.registerAction(2, false, listOf(Events.EVENT_ONE, Events.EVENT_TWO))
        flow.registerAction(2, false, listOf<Events>(Events.EVENT_ONE, Events.EVENT_TWO))
        flow.runDelayed(1,true,  5000){
            flow.event(Events.EVENT_TWO)}
        flow.runRepeat(3,true,  1000) {   Log.w("flow", "local-call  Success:$it ")
        }

        flow.event(Events.EVENT_ONE)


        flow.execute(object : Flow.FlowCode {
            override fun onAction(iAction: Int, bSuccess: Boolean, iExtra: Int, data: Any?) {
                flow.cancelAction(3)
                Log.w("flow", "flow-call  Action: 2 Success:$bSuccess ")
            }
        })

        anim.addAnimation(Anim.TYPE_SCALE, Anim.INTER_OVERSHOOT, 1.0f, 0.6f, 700, 1000)
        anim.addAnimation(Anim.TYPE_SCALE, Anim.INTER_OVERSHOOT, 1.0f, 1.4f, 700, 2000)
        anim.setView(findViewById(R.id.btn_one))
        anim.start()

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

    fun onClick(v: View?) {
        if (bFlip) wake!!.runRepeat(1, 10 * 1000L, "one") else wake!!.runRepeat(1, 20 * 1000L, "one")
        bFlip = !bFlip
    }
}