package com.helper.lib;

import android.content.Context;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Ubaid on 22/04/2016.
 */
public class Utils {
    private long iStartTime;                 // Timer variable
    private String strTimerTag;
    private static Handler handler;          // Handler for background thread
    private static Handler handBg;
    private static String LOG_TAG =  "Helper_Utils";

    static{
        handBg = new Handler(Looper.myLooper());
    }
    // INTERFACE - callback for code run on UI thread
    public interface ThreadCode {
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
    public static void updateUI(final ThreadCode code){
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post( new Runnable() { @Override
                                           public void run() { code.execute();}});
    }
    // METHOD - executes delayed code on Main thread
    public static void updateDelayedUI(long iTime, final ThreadCode code){
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                code.execute();
            }
        }, iTime);
    }

    public static Runnable runDelayed(final long iTime, final ThreadCode code){
        Runnable threadCode  =  new Runnable() {
            @Override
            public void run() {
                code.execute();
            }};
        handBg.postDelayed(threadCode, iTime);
        return threadCode;
    }

    public static Runnable run( final ThreadCode code){
        Runnable threadCode  =  new Runnable() {
            @Override
            public void run() {
                code.execute();
            }};
        handBg.post(threadCode);
        return threadCode;
    }

    public static void cancelRunDelayed(Runnable runCode){
        handBg.removeCallbacks(runCode);
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

    // DATE Conversion methods
    public static String getEventTime(long unixTime){
        Date utcTime = new Date(unixTime);
        SimpleDateFormat outputFmt = new SimpleDateFormat("HH:mm:ss");
        return outputFmt.format(utcTime);
    }

    public static String getUTCDay(int iDay) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, iDay + 1 );
        //  calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.setTime(calendar.getTime());
        Date UTCTime = calendar.getTime();
        SimpleDateFormat outputFmt = new SimpleDateFormat("ddMMyyyy-HHmmss");
        return outputFmt.format(UTCTime);
    }

    public static String getChartsScreenDay(int iDay) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, iDay );
        //  calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.setTime(calendar.getTime());
        Date UTCTime = calendar.getTime();
        SimpleDateFormat outputFmt = new SimpleDateFormat("EEEE, dd MMM yyyy");
        return outputFmt.format(UTCTime);
    }

    public static void playSound(Context context){
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(context, notification);
            r.play();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static long getUnixTime(String sDateTime){
        SimpleDateFormat utcFmt = new SimpleDateFormat("ddMMyyyy");
        Date dateSensor = null;
        try {
            dateSensor = utcFmt.parse(sDateTime);
            return dateSensor.getTime();
        } catch (ParseException e) { e.printStackTrace();
        }
        loge( "APIDate::getUnixTime() Date Parsing error");
        return  0;
    }

    public static String getDayName(long iDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
        Date d = new Date(iDate);
        return sdf.format(d);
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
    // METHOD for logging
    private void log(String sLog){  { Log.d(LOG_TAG, sLog); } }
    private static void loge(String sLog){  { Log.e(LOG_TAG, sLog); } }
    private void logw(String sLog){  { Log.w(LOG_TAG, sLog); } }


}
