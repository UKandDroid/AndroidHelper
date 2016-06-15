package com.helper.lib;

import android.content.Context;
import android.content.res.Resources;
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

// CLASS Utility class for various functions
public class Utils {
    private long iStartTime;                 // Timer variable
    private String strTimerTag;
    private static Handler handler;          // Handler for background thread
    private static Handler handBg;
    private static String LOG_TAG = "Helper_Utils";

    static{
        handBg = new Handler(Looper.myLooper());
    }
    // INTERFACE - callback for actions run on UI thread
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
    // METHOD - runs actions on main thread, use for updating UI from non-UI thread
    public static void updateUI(final ThreadCode code){
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post( new Runnable() { @Override
        public void run() { code.execute();}});
    }
    // METHOD - executes delayed actions on Main thread
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
    public static String getLocalTime(long linuxTime){
        Date utcTime = new Date(linuxTime*1000);
        SimpleDateFormat outputFmt = new SimpleDateFormat("ddMMyyyy-HHmmss");
        return outputFmt.format(utcTime);
    }

    public static String getUTCDay(int iDay) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, iDay + 1);
        //  calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.setTime(calendar.getTime());
        Date UTCTime = calendar.getTime();
        SimpleDateFormat outputFmt = new SimpleDateFormat("ddMMyyyy-000000");
        return outputFmt.format(UTCTime);
    }

    public static long getLinuxTime(String sDateTime){
        SimpleDateFormat utcFmt = new SimpleDateFormat("ddMMyyyy-HHmmss");
        Date dateSensor = null;
        try {
            dateSensor = utcFmt.parse(sDateTime);
            return dateSensor.getTime()/1000l;
        } catch (ParseException e) { e.printStackTrace();
        }
        return  0;
    }
    // Class create a HandlerThread, that uses message to execute actions
    public static class HelperThread{
        private static int iNumCreated = 0;
        private Handler handler;
        private ThreadCode threadCode;
        public HelperThread(ThreadCode code){
            threadCode= code;
            HandlerThread ht = new HandlerThread("HelperThread"+ Integer.toString(iNumCreated++));
            ht.start();
            handler = new Handler(ht.getLooper(), new Handler.Callback() {
                @Override
                public boolean handleMessage(Message message) {
                    threadCode.execute();
                    return true;
                }
            });
        }
        // METHOD runs the thread actions,
        public void run(){
            handler.sendEmptyMessage(1);
        }
        // METHOD runs the thread actions,
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



}
