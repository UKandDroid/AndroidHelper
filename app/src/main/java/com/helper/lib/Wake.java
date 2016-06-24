package com.helper.lib;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ubaid on 24/06/2016.
 */

// Class to keep CPU awake, or awake at certain time
public class Wake extends BroadcastReceiver {
    private static PowerManager.WakeLock wakeLock;
    private int iCurAction = 0;
    private static int iCount = 0;
    private static Context context;
    private static Flow.Code actionCode;
    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;
    private static List<Long> listTime = new ArrayList<>();
    private static List<Long> listRepeat = new ArrayList<>();
    private static List<Integer> listAction = new ArrayList<>();


    public Wake(){}
    public Wake(Context context, Flow.Code flowCode){
        iCount++;
        actionCode = flowCode;
        this.context = context;
        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK , "Wake"+iCount);
    }

    public void release(){ wakeLock.release(); }
    public void stayAwake(){ wakeLock.acquire(); }
    public static void stayAwake(long iDuration){ wakeLock.acquire(iDuration); }

    // METHOD Wake CPU up, and sends action to be execute, Note, this only waits for one Action, new call will override last Action
    // NOTE: the resolution for delayed call is 5 second, anything smaller then that will be called after 5 seconds
    public void runRepeat(int iAction, long timeMillis) {runDelayed(iAction, timeMillis, true); }
    public void runDelayed(int iAction, long timeMillis) { runDelayed(iAction, timeMillis, false);}
    private void runDelayed(int iAction,  long timeMillis, boolean bRepeat) {
        long iTime = System.currentTimeMillis() + timeMillis;
        int iSize = listTime.size();
        boolean bAdd = true;
        for(int i = 0; i < iSize; i++ ){
            if(!(iTime > listTime.get(i)) ){
                bAdd = false;
                listTime.add(i-1, iTime);
                listAction.add(i-1, iAction);
                listRepeat.add(i-1, bRepeat ? timeMillis: 0L);
                break;}}

        if(bAdd){
            listTime.add( iTime );
            listAction.add(iAction);
            listRepeat.add( bRepeat ? timeMillis: 0L );
        }
        if(listTime.size() == 1){
            setNextTimer();
        }
    }

    private synchronized void setNextTimer(){
        if(listTime.size() > 0) {
            iCurAction = listAction.get(0);
            long iTime = listTime.get(0);
            long iRepeat = listRepeat.get(0);
            Intent intent = new Intent(context, Wake.class);
            intent.putExtra("action", iCurAction);
            alarmIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmMgr.set(AlarmManager.RTC_WAKEUP, iTime, alarmIntent);
        }
    }

    // METHOD cancels an action Run
    public void cancelRun(int iAction){
        if(iAction == iCurAction){   // its the current pending action, cancel it and set the next one
            if(alarmIntent != null) {
                AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                mgr.cancel(alarmIntent);
                if(listTime.size() > 0){
                    listTime.remove(0);
                    listAction.remove(0);
                    setNextTimer();}}
        } else {
            for(int i=0; i< listAction.size(); i++){
                if(iAction == listAction.get(i)){
                    listAction.remove(i);
                    listTime.remove(i);
                    break;}}
        }
    }

    // RECEIVER called when wake up timer is fired
    @Override public void onReceive(Context context, Intent intent) {
        if(actionCode != null)
            actionCode.onAction(intent.getIntExtra("action", 0), true, 0, null);

        if(listTime.size() > 0){
            if(listRepeat.get(0)> 0){  // its a repeat message, add it again
                runDelayed(listAction.get(0), listRepeat.get(0), true);
            }
            listTime.remove(0);
            listAction.remove(0);
            listRepeat.remove(0);
            setNextTimer();
        }
    }

    public void stop(){
        listRepeat.clear();
        listAction.clear();
        listTime.clear();
        if(alarmIntent != null) {
            AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            mgr.cancel(alarmIntent);
        }
    }
}
