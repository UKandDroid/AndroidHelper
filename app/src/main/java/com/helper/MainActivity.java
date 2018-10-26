package com.helper;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


import com.helper.lib.Anim;
import com.helper.lib.Logger;
import com.helper.lib.WakeTimer;


import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.disposables.CompositeDisposable;


public class MainActivity extends AppCompatActivity {
    WakeTimer wake;
    Logger log = new Logger("FlowTest");
    public boolean bFlip = true;
    CompositeDisposable disposable = new CompositeDisposable(); // disposable for Rx Android

    private MyComponent myComponent;
    @Inject @Named("two") MyExample myExample;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Anim anim = new Anim();
     anim.setView(v);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }



    public void onClick(View v){
        if(bFlip)
            wake.runRepeat(1, 10* 1000L, "one");
        else
            wake.runRepeat(1, 20* 1000L, "one");
        bFlip = !bFlip;
    }


}
