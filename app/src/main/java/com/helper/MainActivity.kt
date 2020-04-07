package com.helper

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.helper.lib.Anim
import com.helper.lib.Logger
import com.helper.lib.WakeTimer
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject
import javax.inject.Named

class MainActivity : AppCompatActivity() {
    var wake: WakeTimer? = null
    var log = Logger("FlowTest")
    var bFlip = true
    var disposable = CompositeDisposable() // disposable for Rx Android
    private val myComponent: MyComponent? = null
    @Inject
    @Named("two")
    var myExample: MyExample? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val anim = Anim()
        anim.addAnimation(Anim.TYPE_SCALE, Anim.INTER_OVERSHOOT, 1.0f, 0.6f, 700, 1000)
        anim.addAnimation(Anim.TYPE_SCALE, Anim.INTER_OVERSHOOT, 1.0f, 1.4f, 700, 2000)
        anim.setView(findViewById(R.id.btn_one))
        anim.start()
        /*
        // Android data binding
        ActivityMainBinding mainActivity = DataBindingUtil.setContentView(this, R.layout.activity_main);
        user = new User();
        user.firstName.set("ubaid");
        user.lastName.set("khaliq");
        mainActivity.setUser(user);

        // Dagger
        myComponent = DaggerMyComponent.builder().build();
        myComponent.inject(MainActivity.this);


                // Codes outputs text entered in a field to another field, only if there is a gap of one second between input
                // Note every rx call returns an object, the new object should be used for next step, you can use dot operator
                // as well

            // RX example using lambda expressions and Rx v2
            //  implementation 'io.reactivex.rxjava2:rxjava:2.1.9'
            // implementation 'io.reactivex.rxjava2:rxandroid:2.0.2'

           Observable<String> obsTxtChange = Observable.create( emitter ->{

            input.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(Editable s) {}

                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    emitter.onNext(s.toString());
                }});});

        obsTxtChange = obsTxtChange.debounce(1000, TimeUnit.MILLISECONDS);
        obsTxtChange = obsTxtChange.observeOn(AndroidSchedulers.mainThread());
        obsTxtChange = obsTxtChange.map(s ->  "Output: "+s) ;
        obsTxtChange = obsTxtChange.doOnNext(s -> { output.setText(s);});
        obsTxtChange.subscribe();



        */
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
        disposable.dispose()
    }

    fun onClick(v: View?) {
        if (bFlip) wake!!.runRepeat(1, 10 * 1000L, "one") else wake!!.runRepeat(1, 20 * 1000L, "one")
        bFlip = !bFlip
    }
}