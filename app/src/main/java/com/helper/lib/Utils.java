package com.helper.lib;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;

/**
 * Created by Ubaid on 22/04/2016.
 */
public class Utils {
    private long iStartTime;                 // Timer variable
    private String strTimerTag;

    // INTERFACE - callback for code run on UI thread
    interface UIThread{
        public void execute();
    }
    // METHOD - Convert pixels to dp
    public static int pxToDp(Context con, int iPixels){
        DisplayMetrics displayMetrics = con.getResources().getDisplayMetrics();
        int dp = Math.round(iPixels / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }
    // METHOD - Convert dp to pixels
    public static int dpToPx(Context con, int dp){
        Resources r =  con.getResources();
        DisplayMetrics displayMetrics = r.getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }
    // METHOD - runs code on main thread, use for updating UI from non-UI thread
    public static void updateUI(final UIThread code){
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post( new Runnable() { @Override public void run() { code.execute();}});
    }
    // METHOD - executes delayed code on Main thread
    public static void updateDelayedUI(long iTime, final UIThread code){
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                code.execute();
            }
        }, iTime);
    }
    // Run delayed code
    public static void runDelayed(final long iTime, final UIThread code){
        Thread thread = new Thread() {
            public void run() {
                Looper.prepare();
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override public void run() {
                        code.execute();
                        handler.removeCallbacks(this);
                        Looper.myLooper().quit();
                    }
                }, iTime);
                Looper.loop();
            }};
        thread.start();
    }
    // METHOD - sleep thread
    public void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {  e.printStackTrace();}
    }
    // METHOD - Starts timer
    public void startTimer(String strTag){
        strTimerTag = strTag;
        iStartTime = System.currentTimeMillis();
    }
    // METHOD - stops timer and returns time difference in millis
    public String stopTimer(){
        return  strTimerTag +" Time: " + (System.currentTimeMillis() - iStartTime)+"ms" ;
    }


    public static class HThread implements Handler.Callback {

        private  Handler mHandler;
        HThread(){
            HandlerThread ht = new HandlerThread("BGThread");
            mHandler = new Handler(ht.getLooper(), this);
        }

        @Override
        public boolean handleMessage(Message msg) {
            return false;
        }
    }

}
