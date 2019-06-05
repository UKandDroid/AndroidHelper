package com.helper.lib;

import android.content.Context;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


// Version 1.3.0

public class Utils {
    private long iStartTime;                 // Timer variable
    private String strTimerTag;
    private static Handler handler;          // Handler for background thread
    private static Handler handBg;
    private static String LOG_TAG = "Helper_Utils";
    private static HandlerThread handlerThread;
    // INTERFACE - callback for code run on UI thread
    public interface ThreadCode {
        public void execute();
    }

    public static void initialise(){
        HandlerThread handlerThread = new HandlerThread("BGThread");        // start background thread
        handlerThread.start();
        handBg = new Handler(handlerThread.getLooper());
        if(handBg == null){
            handBg = new Handler(Looper.getMainLooper());
            loge("Error setting up Handler");
        }
    }

    public static Runnable run( final ThreadCode code){
        if(handBg == null){
             handlerThread = new HandlerThread("BGThread");                            // start background thread
            handlerThread.start();
            handBg = new Handler(handlerThread.getLooper());
        }
        Runnable threadCode  =  new Runnable() {
            @Override public void run() {
                code.execute();
            }};
        handBg.post(threadCode);
        return threadCode;
    }

    public static void cancelRunDelayed(Runnable runCode){
        if(handBg != null)
            handBg.removeCallbacks(runCode);
    }

    // METHOD - sleep thread
    public static void sleep(long millis){
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

    // METHOD - get android device id
    public static String getPhoneId(Context context) {
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return androidId;
    }

    // DATE Conversion methods
    public static String getLocalTime(long unixTime){
        Date utcTime = new Date(unixTime);
        SimpleDateFormat outputFmt = new SimpleDateFormat("ddMMyyyy-HHmmss");
        return outputFmt.format(utcTime);
    }

    // Returns Just name of the Day
    public static String getDayName(long iDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
        Date d = new Date(iDate);
        return sdf.format(d);
    }

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }


    public static void playSound(Context context){
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(context, notification);
            r.play();
        } catch (Exception e) { e.printStackTrace(); }
    }


    // Class create a HandlerThread, that uses message to execute code
    public static class HelperThread {
        private static int iNumCreated = 0;
        private Handler handler;
        private ThreadCode threadCode;

        public HelperThread(ThreadCode code) {
            threadCode = code;
            HandlerThread ht = new HandlerThread("HelperThread" + Integer.toString(iNumCreated++));
            ht.start();
            handler = new Handler(ht.getLooper(), new Handler.Callback() {
                @Override
                public boolean handleMessage(Message message) {
                    threadCode.execute();
                    return true;
                }
            });
        }



        // METHOD runs the thread code,
        public void run(){
            handler.sendEmptyMessage(1);
        }
        // METHOD runs the thread code,
        public void runDelayed( long timeInMillis){
            handler.sendEmptyMessageDelayed(1, timeInMillis);
        }
        // Cancels a delayed run call
        public void cancelRun(){
            handler.removeMessages(1);
        }
        // Release resources used by the Thread
        public void release(){
            threadCode = null;
            handler.getLooper().quit();
        }

    }

    // METHOD - Convert dp to pixels
    public static int dpToPx(Context con, int dp){
        Resources r =  con.getResources();
        DisplayMetrics displayMetrics = r.getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }


    // METHOD checks if internet is accessible, not just connected (Note: Dont use this method on Main Thread)
    public static boolean isNetConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            try {
                URL url = new URL("http://www.google.com/");
                HttpURLConnection urlc = (HttpURLConnection)url.openConnection();
                urlc.setRequestProperty("User-Agent", "test");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(1000); // mTimeout is in seconds
                urlc.connect();
                if (urlc.getResponseCode() == 200) {
                    return true;
                } else {
                    return false;
                }
            } catch (IOException e) {
                Log.i("warning", "Error checking internet connection", e);
                return false;
            }
        }

        return false;

    }


    // METHOD for logging
    private void log(String sLog){  { Log.d(LOG_TAG, sLog); } }
    private static void loge(String sLog){  { Log.e(LOG_TAG, sLog); } }
    private void logw(String sLog){  { Log.w(LOG_TAG, sLog); } }


}
