package com.helper.lib;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Created by Ubaid on 24/06/2016.
 */

// Ver 1.1 - Wake class Change date 20 Dec 2016
// Remove event if it already exists, repeating or non repeating

// Class to keep CPU awake, or awake at certain time
public class WakeTimer extends BroadcastReceiver {

    private static WakeTimer instance;
    private AlarmManager alarmMgr;
    private static Context context;
    private PendingIntent alarmIntent;
    private static Flow.Code actionCode;
    private static int iCurAction = -1;
    private static long iCurActionTime = 0;
    private static PowerManager.WakeLock wakeLock;
    private static List<String> listTag = new ArrayList<>();
    private static List<Integer> listAction = new ArrayList<>();
    private static List<Long> listActionTime = new ArrayList<>();
    private static List<Long> listRepeatTime = new ArrayList<>();

    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    private static String ALARM_INTENT = ".receiver.WakeTimer";
    private static int API_VERSION = Build.VERSION.SDK_INT;
    private static String LOG_TAG = "Timer";
    private static Logger log = new Logger(LOG_TAG);
    // CONSTRUCTOR to be called by Intent service, Don't use, instead use init() to initialize the class
    public WakeTimer(){}

    // METHOD to get instance of the class, as class is singleton
    public WakeTimer instance(){
        if(instance == null){ throw  new RuntimeException("Class not initialised, use init(Context, Flow.Code )");}
        log.saveToFile();
        return instance;
    }

    public int pendingCount(){
        return listActionTime.size();
    }

    // METHOD to initialise the class, as class is singleton
    public static WakeTimer init(Context con, Flow.Code flowCode){
        context = con;
        ALARM_INTENT = context.getPackageName() + ALARM_INTENT;
        instance = new WakeTimer();
        actionCode = flowCode;
        listTag.clear();
        listAction.clear();
        listActionTime.clear();
        listRepeatTime.clear();
        instance.iCurAction = -1;                                      // So same event can be loaded again, in-case static variable is still same

        context.registerReceiver(new WakeTimer(), new IntentFilter(ALARM_INTENT));
        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK , "Wake");

        // loadPendingActions();
        instance.setNextTimer();
        return instance;
    }

    // METHOD to release wake lock, when not set for fixed time
    public void release(){ wakeLock.release(); }
    public void stay(){ wakeLock.acquire(); }
    public static void stay(long iDuration){ wakeLock.acquire(iDuration); }

    // METHOD WakeTimer CPU up, and sends action to be execute, Note, this only waits for one Action, new call will override last Action
    // NOTE: the resolution for delayed call is 5 second, anything smaller then that will be called after 5 seconds
    public void runRepeat(int iAction, long timeMillis) {runDelayed(iAction, timeMillis, true, ""); }
    public void runDelayed(int iAction, long timeMillis) { runDelayed(iAction, timeMillis, false, "");}
    public void runRepeat(int iAction, long timeMillis, String sTag) {runDelayed(iAction, timeMillis, true, sTag); }
    public void runDelayed(int iAction, long timeMillis, String sTag) { runDelayed(iAction, timeMillis, false, sTag);}
    private void runDelayed(int iAction, long timeMillis, boolean bRepeat, String sTag) {
        long iTime = System.currentTimeMillis() + timeMillis;
        int iSize = listAction.size();

        for(int i=0; i<iSize; i++){                                                // if action already exists, remove it so we can add new
            if(listAction.get(i) == iAction) {
                removeAction(i);
                iSize = listActionTime.size();
            }}

        for(int i = 0; i <= iSize; i++ ){
            if(i == iSize || iTime < listActionTime.get(i)){                       // put it in right time slot
                listTag.add(i, sTag);
                listAction.add(i, iAction);
                listActionTime.add(i, iTime);
                listRepeatTime.add(i, bRepeat ? timeMillis : 0L);
                break;
            }
        }

        saveActions();
        setNextTimer();
    }

    @TargetApi(23)
    // METHOD is called, when a timer goes off, to set next timer
    private synchronized void setNextTimer(){
        boolean bNewAction = false;
        int iSize = listAction.size();

        // If we have a action in list, and its not same as old one
        if(iSize > 0 && (iCurAction != listAction.get(0) || iCurActionTime != listActionTime.get(0)))
            bNewAction = true;

        if(bNewAction) {
            iCurAction = listAction.get(0);
            iCurActionTime = listActionTime.get(0);
            String sTag = listTag.get(0);
            Intent intent = new Intent(ALARM_INTENT);
            intent.putExtra("action", iCurAction);
            intent.putExtra("tag", sTag);
            alarmIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmMgr.setExact(AlarmManager.RTC_WAKEUP, iCurActionTime, alarmIntent);

            if (API_VERSION >= Build.VERSION_CODES.M){       // In marshmallow, use this as setExact will be blocked
                alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, iCurActionTime, alarmIntent);
            } else{
                alarmMgr.setExact(AlarmManager.RTC_WAKEUP, iCurActionTime, alarmIntent);
            }
            log.d( "Set Timer " + sTag + " ("+ iCurAction + ") >  "+ sdf.format(new Date(iCurActionTime)) + (listRepeatTime.get(0)> 0 ? " - Repeat" : "") );
        }
    }

    // METHOD cancels an action Run
    public synchronized void cancelRun(int iAction){
        try{
            if(iAction == iCurAction){   // its the current pending action, cancel it and set the next one
                if(alarmIntent != null) {
                    AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    mgr.cancel(alarmIntent);
                    if(listAction.size() > 0){
                        log.w( "Cancelled Timer: "+  listTag.get(0) + " ("+ iCurAction + ") " );
                        removeAction(0);
                        setNextTimer();}}
            } else {                   // Remove action from the list
                for(int i=0; i < listAction.size(); i++){
                    if(iAction == listAction.get(i)){
                        log.w( "Cancelled Timer " + listTag.get(i)+" ("+iAction+")");
                        removeAction(i);
                        i = 0;
                    }}
            }
        }catch (Exception e){e.printStackTrace();}
    }

    // RECEIVER called when wake up timer is fired
    @Override public void onReceive(Context con, Intent intent) {
        boolean bNextNow = false;
        log.w( "Exe Timer " + intent.getStringExtra("tag") + " (" + intent.getIntExtra("action", 0) + ") :  " + sdf.format(new Date()));
        iCurAction = -1;                                                                            // Set -1, so we cannot cancel current action in execution code
        //  as its the action being executed
        if(actionCode != null)
            actionCode.onAction(intent.getIntExtra("action", 0), true, 0, intent.getStringExtra("tag"));

        if(listRepeatTime.get(0)> 0){                                       // its a repeat message, add it again
            runDelayed(listAction.get(0), listRepeatTime.get(0), true, intent.getStringExtra("tag"));
        }  else {
            removeAction(0);
            saveActions();
        }

        if(listAction.size() > 0){
            long iNextActionTime = listActionTime.get(0);
            if(iNextActionTime < System.currentTimeMillis() + 1000L){         // If next action is already late or time is less then 1 second,
                intent = new Intent(ALARM_INTENT);                            // run it now, as sleep wake minimum resolution is 5 seconds
                iCurAction = listAction.get(0);
                intent.putExtra("action", iCurAction);
                intent.putExtra("tag", listTag.get(0));
                bNextNow = true;
            }

            if(bNextNow)
                onReceive(con, intent);
            else
                setNextTimer();
        }
    }

    // METHOD removes an Action from lists
    private static void removeAction(int iIndex){
        listTag.remove(iIndex);
        listAction.remove(iIndex);
        listRepeatTime.remove(iIndex);
        listActionTime.remove(iIndex);
    }


    // METHOD cancels any pending alarms
    public void cancelPending(){
        log.d( "Cancel Pending");
        if(alarmIntent == null) {
            Intent intent = new Intent(context, WakeTimer.class);
            alarmIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        }
        alarmMgr.cancel(alarmIntent);
    }

    // Stop the WakeTimer class completely
    public void stop(){
        listTag.clear();
        listAction.clear();
        listActionTime.clear();
        listRepeatTime.clear();
        listTag.clear();
        cancelPending();
        instance = null;
    }

    // METHOD saves list to prefs, in case the app restarts
    private static void saveActions(){
        String sTime = "", sRepeat = "", sAction = "", sTag ="";
        SharedPreferences sharedPref = context.getSharedPreferences(context.getPackageName()+"WakeListener", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        int iSize = listAction.size();

        for(int i=0; i < iSize; i++){
            sTag += listTag.get(i);
            sAction += listAction.get(i).toString();
            sTime += listActionTime.get(i).toString();
            sRepeat += listRepeatTime.get(i).toString();
            if(i < iSize-1){
                sTime += ",";
                sRepeat += ",";
                sAction += ",";
                sTag += "\n";
            }
        }

        editor.putString("time", sTime);
        editor.putString("repeat", sRepeat);
        editor.putString("action", sAction);
        editor.putString("tag", sTag);
        editor.apply();
    }

    // METHOD loads list from prefs, when starts for first time
    private static void loadPendingActions(){
        SharedPreferences sharedPref = context.getSharedPreferences("WakeListener", Context.MODE_PRIVATE);
        String sTime = sharedPref.getString("time", "");
        String sRepeat = sharedPref.getString("repeat", "");
        String sAction = sharedPref.getString("action", "");
        String sTag = sharedPref.getString("tag", "");

        if(!sTime.equals("")){
            String arrTag[] = sTag.split("\n");
            String arrTime[] = sTime.split(",");
            String arrRepeat[] = sRepeat.split(",");
            String arrAction[] = sAction.split(",");
            log.e( arrTime.length + " Pending actions loaded.");

            for(int i=0; i < arrTime.length; i++){
                listTag.add(arrTag[i]);
                listAction.add(Integer.parseInt(arrAction[i]));
                listActionTime.add(Long.parseLong(arrTime[i]));
                listRepeatTime.add(Long.parseLong(arrRepeat[i]));
            }}
    }

}
