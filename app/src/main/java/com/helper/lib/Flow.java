package com.helper.lib;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
// Version 2.3.0
// removed UiFlow to separate class
// added ui listener LOAD_LAYOUT
// Added runType for events RESULT_CHANGE, RESULT_UPDATE, EVENT_UPDATE

// Added Help examples
// ## EXAMPLES ##
// Flow flow = new Flow(flowCode)
// Example 1: flow.registerEvents(1, "email_entered", "password_entered", "verify_code_entered" ) action 1 gets called when all those events occur
//          : flow.onEvent("email_entered", true, extra(opt), object(opt))  is trigger for the registered event "email_entered",
//          :  when all three events are triggered with flow.onEvent(...., true), action 1 is executed with bSuccess = true
//          :  after 3 event true(s), if one onEvent(...., false) sends false, action 1 will be executed with bSuccess = false
//          :  now action 1 will only trigger again when all onEvents(...., true) are true, i.e the events which sent false, send true again
// Example 2: flow.registerUiEvent(2, spinnerView, Flow.Event.SPINNER_ITEM_SELECT) action two gets called when ever a spinner item is selected
// Example 3: flow.run(3, true(opt), extra(opt), object(opt)) runs an action on background thread, same as registering for one event and triggering that event
// Example 4: flow.runOnUi(4, true(opt), extra(opt), object(opt)) runs code on Ui thread
// Example 5: flow.runDelayed(5, true(opt), extra(opt), 4000) runs delayed code
// Example 6: flow.runDelayedOnUi(6, true(opt), extra(opt), 4000) runs delayed code on Ui thread

// Flow.Code flowCode = new Flow.Code(){
//  @override public void onAction(int iAction, boolean bSuccess, int iExtra, Object data){
//  switch(iAction){
//      case 1:  ...... break;   // this code will run in first example when all events are triggered as true
//      case 2: ...... break;    // this code will run when a spinner item is selected
//      case 3: ....... break;   // this will run when ever run(3) is called
//      case 4: ........ break;  // this will run on ui thread whenever runOnUi(4) is called
//      case 5: ........ break;  // this will run on delayed by 4 secs
// }  }
// Example 7: new Flow().runDelayed(2000).execute(() -{})
// Example 8: new Flow().runRepeat(500).execute(() -{})

public class Flow implements LifecycleObserver{
    public static final int RESULT_CHANGE = 0; // called once all events are fired, and when events AND result change
    public static final int RESULT_UPDATE = 1; // called once all events are fired with true, and every time any event updates as long as events AND is true
    public static final int EVENT_UPDATE = 2;  // called every time an event is fired or changed
    private boolean bRunning;
    protected HThread hThread;
    private static int iThreadCount = 0;
    private static final int LOG_LEVEL = 4;
    private static final String LOG_TAG = "Flow";
    private static final int FLAG_REPEAT = 0x00000004;
    private static final int FLAG_SUCCESS = 0x00000001;
    private static final int FLAG_RUNonUI = 0x00000002;
    private List<Action> listActions = new ArrayList<Action>();  // List of registered actions

    private Execute code = null;                                                 // Call back for onAction to be executed

    // INTERFACES for code execution and keyboard listener
    private interface Execute {}
    public interface Run extends Execute{ public void onAction();}
    public interface Code extends Execute{ public void onAction(int iAction, boolean bSuccess, int iExtra, Object data); }

    public Flow(){ this(null); }
    public Flow(Code codeCallback) {
        code = codeCallback;
        hThread = new HThread();
    }

    public void code(Execute codeCallback){  code = codeCallback; }
    public void execute(Execute CodeOrRunCallback){ code = CodeOrRunCallback;  }

    public class UiEvent{
        // EVENTS for which listeners are set
        public static final int TOUCH = 3;
        public static final int ON_CLICK = 4;
        public static final int TEXT_CHANGED = 5;
        public static final int TEXT_ENTERED = 6;
        public static final int CHECKBOX_STATE = 7;
        public static final int LIST_ITEM_SELECT = 8;
        public static final int SPINNER_ITEM_SELECT = 9;
        public static final int KEYBOARD_STATE_CHANGE = 10; //   works only for android:windowSoftInputMode="adjustResize" or adjustPan
        public static final int LOAD_LAYOUT = 11; //   called when a view is loaded with width and height set
    }

    // STATE METHODS pause, resume, stop the action, should be called to release resources
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void pause() {
        hThread.mHandler.removeCallbacksAndMessages(null);
        hThread.mUiHandler.removeCallbacksAndMessages(null);
        bRunning = false;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void resume() {
        bRunning = true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stop() {
        code = null;
        try {
            for (int i = 0; i < listActions.size(); i++) {
                listActions.get(i).recycle();
            }

            hThread.mHandler.removeCallbacksAndMessages(null);
            hThread.mUiHandler.removeCallbacksAndMessages(null);
            hThread.stop();
            listActions = null;
            Event.releasePool();
            bRunning = false;
        } catch (Exception e) {}
    }


    // METHOD sets the type of action run RESULT_CHANGE, RESULT_UPDATE, EVENT_UPDATE
    public void runType(int iType){
        if(listActions.size() > 0)
            listActions.get(listActions.size()-1).iRunType = iType;
    }

    // METHODS run an action
    public Flow run(boolean bRunOnUi) { run(-1, true); return  this; }
    public Flow run(int iAction) { run(iAction, false); return  this; }
    public Flow run(int iAction, boolean bRunOnUi) { run(iAction, bRunOnUi, true, 0, null); return  this;}
    public Flow run(int iAction, int iExtra, Object obj) { run(iAction, false, true, iExtra, obj); return  this;}
    public Flow run(int iAction, boolean bRunOnUi, boolean bSuccess, int iExtra, Object obj) {
        if(bRunOnUi) hThread.runOnUI(iAction, bSuccess, iExtra, obj);
        else hThread.run(iAction, bSuccess, iExtra, obj);
        return  this;
    }

    public Flow runRepeat(long iDelay) { hThread.runRepeat(false, -1, true, 0, iDelay);  return  this;}
    public Flow runRepeat(int iAction, long iDelay) { hThread.runRepeat(false, iAction, true, 0, iDelay);  return  this;}
    public Flow runRepeat(int iAction, boolean bRunOnUi, long iDelay) { hThread.runRepeat(bRunOnUi, iAction, true, 0, iDelay);  return  this;}
    public Flow runRepeat(int iAction, boolean bSuccess, int iExtra, long iDelay) { hThread.runRepeat(false, iAction, bSuccess, iExtra, iDelay);  return  this;}
    public Flow runRepeat(int iAction, boolean bRunOnUi, boolean bSuccess, int iExtra, long iDelay) { hThread.runRepeat(bRunOnUi, iAction, bSuccess, iExtra, iDelay);  return  this;}

    // METHODS run action delayed
    public Flow runDelayed( long iTime) {
        runDelayed2(-1, true, 0, null, iTime);
        return  this;
    }
    public Flow runDelayed(int iAction, long iTime) {
        runDelayed2(iAction, true, 0, null, iTime);
        return  this;
    }
    public Flow runDelayed(int iAction, boolean bRunOnUi, long iTime) {
        if(bRunOnUi) runDelayedOnUI(iAction, true, 0, null, iTime);
        else runDelayed2(iAction, true, 0, null, iTime);
        return  this;
    }
    public Flow runDelayed(int iAction, boolean bSuccess, int iExtra, Object object, long iTime) {
        runDelayed2(iAction, bSuccess, iExtra, object, iTime);
        return  this;
    }

    public Flow runDelayed( boolean bRunOnUi, boolean bSuccess, int iExtra, Object object, long iTime) {
        return runDelayed(-1, bRunOnUi, bSuccess, iExtra, object, iTime);
    }

    public Flow runDelayed(int iAction, boolean bRunOnUi, boolean bSuccess, int iExtra, Object object, long iTime) {
        if(bRunOnUi) runDelayedOnUI(iAction, bSuccess, iExtra, object, iTime);
        else runDelayed2(iAction, bSuccess, iExtra, object, iTime);
        return  this;
    }

    private void runDelayedOnUI(int iAction, boolean bSuccess, int iExtra, Object object, long iTime) {
        Message msg = Message.obtain();
        msg.what = iAction;
        msg.arg1 = iExtra;
        msg.arg2 = bSuccess ? 1 : 0;
        msg.obj = object;
        hThread.mUiHandler.removeMessages(iAction);                             // Remove any pending messages in queue
        hThread.mUiHandler.sendMessageDelayed(msg, iTime);
    }

    private void runDelayed2(int iAction, boolean bSuccess, int iExtra,Object object, long iTime) {
        Message msg = Message.obtain();
        msg.what = iAction;
        msg.arg1 = iExtra;
        msg.arg2 = bSuccess ? 1 : 0;
        msg.obj = object;
        hThread.mHandler.removeMessages(iAction);                               // Remove any pending messages in queue
        hThread.mHandler.sendMessageDelayed(msg, iTime);
    }

    // METHODS events registration
    public Flow registerEvents(int iAction, String events[]) { registerEvents(iAction, false, false, false, events); return this;}
    public Flow waitForEvents(int iAction, String events[]) { registerEvents(iAction, false, true, false, events); return this;}
    public Flow waitForEvents( int iAction, boolean bRunOnUI, String events[]) { registerEvents(iAction, bRunOnUI, true, false, events); return this;}
    public Flow registerEvents(int iAction, boolean bRunOnUI, String events[]) { registerEvents(iAction, bRunOnUI, false, false, events); return this;}
    public Flow registerEventSequence( int iAction, boolean bRunOnUI, String events[]) { registerEvents(iAction, bRunOnUI, false, true, events);return this;}
    private void registerEvents(int iAction, boolean bRunOnUI, boolean bRunOnce, boolean bSequence, String events[]){
        unRegisterEvents(iAction);  // to stop duplication, remove if the action already exists
        Action aAction = new Action(iAction, events);
        aAction.bRunOnUI = bRunOnUI;
        aAction.bFireOnce = bRunOnce;                  // fired only once, then removed
        aAction.bSequence = bSequence;                 // events have to be in sequence for the action to be fired
        listActions.add( aAction);
        StringBuffer buf = new StringBuffer(400);
        for(int i =0; i< events.length; i++){ buf.append(events[i]+", ");}
        log("ACTION: " + iAction + " registered  EVENTS = {" +buf.toString()+"}");
    }

    public void unRegisterEvents(int iAction){
        for (int i = 0; i< listActions.size(); i++){ // remove action if it already exists
            if(listActions.get(i).iAction == iAction){
                listActions.remove(i);
                log("ACTION: "+iAction+ " exists, removing it  ");
                break;
            }
        }
    }

    // METHODS to send event
    public void event(String sEvent) { event(sEvent, true, 0, null); }
    public void event(String sEvent, boolean bSuccess) { event(sEvent, bSuccess, 0, null); }
    public void event(String sEvent, boolean bSuccess, int iExtra) { event(sEvent, bSuccess, iExtra, null); }
    public void event(String sEvent, boolean bSuccess, int iExtra, Object obj) {
        if (!bRunning) return;

        log("EVENT:  "+ sEvent);
        int iSize = listActions.size();
        boolean bActionFired = false;
        for (int i = 0; i < iSize; i++) {
            bActionFired =  listActions.get(i).onEvent(sEvent, bSuccess, iExtra, obj);
            if(bActionFired && listActions.get(i).bFireOnce){
                listActions.remove(i);
                log("Removing ACTION run once after been fired");
            }
        }
    }

    // METHOD cancel a runDelay or RunRepeated
    public void cancelRun(int iAction) {
        if (!bRunning) return;
        hThread.mHandler.removeMessages(iAction);
        hThread.mUiHandler.removeMessages(iAction);
    }

    // CLASS for event Pool
    public static class Event {
        // EVENTS for self use
        private static final int WAITING = 0;
        private static final int SUCCESS = 1;
        private static final int FAILURE = 2;

        public Object obj;
        public int iExtra;
        public String sEvent;
        public int iStatus = WAITING;   // 0 - waiting not fired yet, 1 - fired with success, 2- fired with failure
        // Variable for pool
        private Event next;             // Reference to next object
        private static Event sPool;
        private static int sPoolSize = 0;
        private static final int MAX_POOL_SIZE = 50;
        private static final Object sPoolSync = new Object();       // The lock used for synchronization

        // CONSTRUCTOR - Private
        private Event() {}

        // METHOD get pool object only through this method, so no direct allocation are made
        public static Event obtain(String sId) {
            synchronized (sPoolSync) {
                if (sPool != null) {
                    Event e = sPool;
                    e.sEvent = sId;
                    e.iStatus = WAITING;
                    e.obj = null;
                    e.iExtra = 0;
                    sPool = e.next;
                    e.next = null;
                    sPoolSize--;
                    return e;
                }
                Event eve = new Event();
                eve.sEvent = sId;
                return eve;
            }
        }

        // METHOD object added to the pool, to be reused
        public void recycle() {
            synchronized (sPoolSync) {
                if (sPoolSize < MAX_POOL_SIZE) {
                    next = sPool;
                    sPool = this;
                    sPoolSize++;
                }
            }
        }

        // METHOD release pool, ready for garbage collection
        public static void releasePool() {
            sPoolSize = 0;
            sPool = null;
        }
    }

    // CLASS for events for action, when all events occur action is triggered
    public class Action {
        private int iAction;                                      // Code step to execute for this action
        private int iEventCount;                                    // How many event are for this action code to be triggered
        private boolean bSequence = false;                           // Only trigger when events occur in right order
        private static final int SUCCESS = 1;
        private static final int FAILURE = 2;
        //   private boolean bEventFound;
        private boolean bRunOnUI = false;                           // Code run on Background / UI thread
        public int iRunType = RESULT_CHANGE;                 // when this action is run,
        public boolean bFireOnce = false;                           // Clear Action once fired, used for wait action
        private int iLastStatus = Event.WAITING;                     // Event set status as a whole, waiting, success, non success
        private List<Event> listEvents = new ArrayList<>();         // List to store events needed for this action

        // CONSTRUCTOR
        public Action(int iCodeStep, String events[]) {
            bSequence = false;
            this.iAction = iCodeStep;
            iEventCount = events.length;
            for (int i = 0; i < iEventCount; i++) {
                listEvents.add(Event.obtain(events[i]));            // get events from events pool
            }
        }

        public Action(int iCodeStep, String events[], boolean bOrder) {
            this.bSequence = bOrder;
            this.iAction = iCodeStep;
            iEventCount = events.length;
            for (int i = 0; i < iEventCount; i++) {
                listEvents.add(Event.obtain(events[i]));            // get events from events pool
            }
        }

        // METHOD recycles events and clears actions
        public void recycle() {
            int iSize = listEvents.size();
            for (int i = 0; i < iSize; i++) {
                listEvents.get(i).recycle();
            }
            listEvents = null;
        }

        // METHOD searches all actions, if any associated with this event
        public boolean onEvent(String sEvent, Boolean bResult, int iExtra, Object obj) {
            int iFiredCount = 0;                     // How many have been fired
            int iSuccess = 0;                   // How many has been successful
            boolean bEventFound = false;
            boolean bActionFired = false;

            for (int i = 0; i < iEventCount; i++) {
                Event event = listEvents.get(i);
                if (sEvent.equals(event.sEvent)) {  // If event is found in this event list
                    logw("{" + sEvent + "} fired for ACTION: " + iAction + " ");
                    bEventFound = true;
                    event.obj = obj;
                    event.iExtra = iExtra;
                    event.iStatus = bResult ? Event.SUCCESS : Event.FAILURE;
                } else if(bSequence && event.iStatus == Event.WAITING){                              // if its a Sequence action, no event should be empty before current event
                    if( i != 0 ){ listEvents.get(i-1).iStatus = Event.WAITING; }                    // reset last one, so they are always in sequence
                    break;
                }

                switch (event.iStatus) {
                    case Event.SUCCESS: iSuccess++;
                    case Event.FAILURE: iFiredCount++;    // Add to fired event regard less of success or failure
                        break;
                }

                if(bEventFound && bSequence)
                    break;
            }

            if (bEventFound) {                                      // if event was found in this Action
                logw("{" + sEvent + ":} for ACTION: " + iAction + ", Total Fired: "+iFiredCount+" iSuccess: "+iSuccess);
                if(iRunType == EVENT_UPDATE){                      // if this action is launched on every event update
                    executeAction(bResult, iExtra);
                } else if (iFiredCount == iEventCount) {            // if all events for action has been fired
                    boolean bSuccess = (iSuccess == iEventCount);   // all events registered success
                    int iCurStatus = bSuccess ? Action.SUCCESS : Action.FAILURE;

                    switch (iRunType){
                        case RESULT_CHANGE:
                            if (iCurStatus != iLastStatus) {        // If there is a change in action status only then run code
                                bActionFired = true;
                                iLastStatus = iCurStatus;
                                executeAction(bSuccess, iSuccess);
                            }
                            break;
                        case RESULT_UPDATE:
                            if(bSuccess){
                                bActionFired = true;
                                executeAction(bSuccess, iSuccess);
                            }
                            break;
                    }
                    if (bFireOnce) { recycle(); }                // Recycle if its flagged for it
                }
            }
            return bActionFired;
        }

        // METHOD executes action code on appropriate thread
        private void executeAction(boolean bSuccess, int iExtra){
            logw("ACTION:"+ iAction + " fired" );
            if (bRunOnUI) {
                hThread.runOnUI(iAction, bSuccess, iExtra, this.listEvents);
            } else {
                hThread.run(iAction, bSuccess, iExtra, this.listEvents);
            }
        }
    }

    // CLASS for thread handler
    public class HThread implements Handler.Callback {
        private Handler mHandler;
        private Handler mUiHandler;

        HThread() {
            HandlerThread ht = new HandlerThread("BGThread_" + Integer.toString(++iThreadCount));
            ht.start();
            mHandler = new Handler(ht.getLooper(), this);
            mUiHandler = new Handler(Looper.getMainLooper(), this);
        }

        public void run(int iStep) {
            run(iStep, false);
        }

        public void run(int iStep, boolean bRunUI) {
            if (bRunUI) {
                runOnUI(iStep, true, 0, null);
            } else {
                run(iStep, true, 0, null);
            }
        }

        public void run(int iStep, boolean bSuccess, int iExtra, Object obj) {
            if (bRunning) {
                Message msg = Message.obtain();
                msg.what = iStep;
                msg.arg1 = iExtra;
                msg.arg2 = bSuccess ? 1 : 0;
                msg.obj = obj;
                mHandler.sendMessage(msg);
            }
        }

        public void runOnUI(int iStep, boolean bSuccess, int iExtra, Object obj) {
            if (bRunning) {
                Message msg = Message.obtain();
                msg.what = iStep;
                msg.arg1 = iExtra;
                msg.arg2 = bSuccess ? 1 : 0;
                msg.obj = obj;
                mUiHandler.sendMessage(msg);
            }
        }

        public void runRepeat(boolean bRunOnUI, int iStep, boolean bSuccess, int iExtra, long iDelay) {
            if (bRunning) {
                int flags = 0;
                flags = setFlag(flags, FLAG_REPEAT, true);
                flags = setFlag(flags, FLAG_SUCCESS, bSuccess);
                flags = setFlag(flags, FLAG_RUNonUI, bRunOnUI);
                flags = addExtraInt(flags, iExtra);

                Message msg = Message.obtain();
                msg.what = iStep;
                msg.arg1 = (int) iDelay;                               // As arg1 is integer
                msg.arg2 = flags;
                if (bRunOnUI) {
                    mUiHandler.sendMessage(msg);
                } else {
                    mHandler.sendMessage(msg);
                }
            }
        }

        // METHOD MESSAGE HANDLER
        @Override
        public boolean handleMessage(Message msg) {
            if (getFlag(msg.arg2, FLAG_REPEAT)) {         // If its a repeat message, data is packed differently,
                Message msg2 = Message.obtain();
                msg2.what = msg.what;
                msg2.arg1 = msg.arg1;
                msg2.arg2 = msg.arg2;

                if (getFlag(msg.arg2, FLAG_RUNonUI)) {
                    mUiHandler.removeMessages(msg.what);   // Clear any pending messages
                    mUiHandler.sendMessageDelayed(msg2, (long) msg.arg1);
                } else {
                    mHandler.removeMessages(msg.what);      // Clear any pending messages
                    mHandler.sendMessageDelayed(msg2, (long) msg.arg1);
                }
                if(code != null){
                    if(code instanceof Code)
                        ((Code)code).onAction(msg.what, getFlag(msg.arg2, FLAG_SUCCESS), getExtraInt(msg.arg2), msg.obj);
                    else
                        ((Run)code).onAction();
                }
            } else {
                if(code != null){
                    if(code instanceof Code)
                        ((Code)code).onAction(msg.what, msg.arg2 == 1, msg.arg1, msg.obj);
                    else
                        ((Run)code).onAction();
                }
            }
            return true;
        }

        public void stop() {
            mHandler.removeCallbacksAndMessages(null);
            mUiHandler.removeCallbacksAndMessages(null);
            mHandler.getLooper().quit();
        }
    }

    // METHODS for packing data for repeat event
    private static int addExtraInt(int iValue, int iData) {
        return iValue | (iData << 8);
    }

    private static int getExtraInt(int iValue) {
        return (iValue >> 8);
    }

    private static boolean getFlag(int iValue, int iFlag) {
        return (iValue & iFlag) == iFlag;
    }

    private static int setFlag(int iValue, int iFlag, boolean bSet) {
        if (bSet) {
            return iValue | iFlag;
        } else {
            return iValue & (~iFlag);
        }
    }


    // METHOD for logging
    protected void log(String sLog) {
        log(1, sLog);
    }
    protected void loge(String sLog) {
        loge(1, sLog);
    }
    protected void logw(String sLog) {
        logw(1, sLog);
    }
    private void log(int iLevel, String sLog) {
        if (iLevel <= LOG_LEVEL) { Log.d(LOG_TAG, sLog); }
    }

    protected void loge(int iLevel, String sLog) {
        if (iLevel <= LOG_LEVEL) { Log.e(LOG_TAG, sLog); }
    }

    protected void logw(int iLevel, String sLog) {
        if (iLevel <= LOG_LEVEL) { Log.w(LOG_TAG, sLog); }
    }

}

