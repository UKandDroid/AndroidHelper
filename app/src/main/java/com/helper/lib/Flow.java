package com.helper.lib;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

// Version 1.0.1
// CLASS for event based onAction execution
public class Flow {
    private Code code;                                      // Call back for onAction to be executed
    private boolean bRunning;
    private HThread hThread ;
    private Action waitAction = null;
    private static int iThreadCount = 0;
    private List<Action> listActions = new ArrayList<Action>();  // List of registered actions

    public Flow(Code codeCallback){
        bRunning = true;
        code = codeCallback;
        hThread = new HThread();
    }

    // STATE METHODS pause, resume, stop the action, should be called to release resources
    public void pause(){ bRunning = false; }
    public void resume(){ bRunning = true; }
    public void stop(){
        for (int i = 0; i< listActions.size(); i++){ listActions.get(i).recycle();}
        hThread.stop();
        listActions = null;
        waitAction = null;
        Event.releasePool();
    }

    // METHODS run an action
    public void run(int iCodeStep){ hThread.run(iCodeStep); }
    public void runUI(int iCodeStep){ hThread.run(iCodeStep, true); }

    // METHODS run action delayed
    public void runDelayed(int iCodeStep, long iTime){ hThread.mHandler.sendEmptyMessageDelayed(iCodeStep, iTime);}
    public void runDelayedUI(int iCodeStep, long iTime){ hThread.mHandlerUI.sendEmptyMessageDelayed(iCodeStep, iTime);}

    // METHODS register for event
    public void registerEvents(int iStep, String events[]){ registerEvents(false, iStep, events); }
    public void registerEvents(boolean bRunOnUI, int iStep, String events[]){
        Action act = new Action(iStep, events);
        act.bRunOnUI = bRunOnUI;
        listActions.add(act);
    }

    // METHOD wait for event once dispatched clears it
    public void waitForEvents(int iAction, String events[]){ waitForEvents(false, iAction, events);}
    public void waitForEvents(boolean bRunOnUI, int iAction, String events[]){
        waitAction = new Action(iAction, events);
        waitAction.bRunOnUI = bRunOnUI;
        waitAction.bClearFired = true;                  // waitForEvents is fired only once, release after fired
    }

    // METHODS register UI events for Action
    public void registerEventsUI(final int iStep, View view){ registerListener(false, iStep, view, Event.ON_CLICK); }
    public void registerEventsUI(final int iStep, View view, int iEvent){ registerListener(false, iStep, view, iEvent); }
    public void registerEventsUI(boolean bRunOnUI, int iStep, View view, int iEvent){
        registerListener(bRunOnUI, iStep, view, iEvent);
    }

    // VIEW LISTENERS set event listeners for View objects
    private void registerListener(final boolean bRunOnUI, final int iStep, final View view, int iListener){
        switch (iListener){
            // triggered listener when view is clicked
            case Event.ON_CLICK:
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(bRunOnUI){ hThread.runUI(iStep, true, 0, view);
                        }  else {hThread.run(iStep, true, 0, view); }
                    }
                });
                break;

            // Text entered in text field, triggered when text field loses focus or enter button is pressed
            case Event.TEXT_ENTERED:
                view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (!hasFocus) {
                            if(bRunOnUI){ hThread.runUI(iStep, true, 0, view);
                            }  else { hThread.run(iStep, true, 0, view); }
                        }}});

                ((EditText)view).setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            if(bRunOnUI){ hThread.runUI(iStep, true, 0, view);
                            }  else { hThread.run(iStep, true, 0, view); }
                            return true;
                        }
                        return false;
                    }
                });
                break;

            // Listener for trigger when text changes
            case Event.TEXT_CHANGE:
                ((EditText)view).addTextChangedListener(new TextWatcher() {
                    @Override public void afterTextChanged(Editable s) {}
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if(bRunOnUI){ hThread.runUI(iStep, true, 0, view);
                        } else { hThread.run(iStep, true, 0, view); }
                    }
                });
                break;
        }
    }

    // METHODS to send event
    public void event(String sEvent){ event(sEvent, true, 0, null); }
    public void event(String sEvent, boolean bSuccess){ event(sEvent, bSuccess, 0, null); }
    public void event(String sEvent, boolean bSuccess, int iExtra){ event(sEvent, bSuccess, iExtra, null); }
    public void event(String sEvent, boolean bSuccess, int iExtra, Object obj){
        if(waitAction != null){
            waitAction.onEvent(sEvent, bSuccess, iExtra, obj);
        }
        int iSize = listActions.size();
        for(int i = 0; i < iSize; i++){
            listActions.get(i).onEvent(sEvent, bSuccess, iExtra, obj);
        }
    }

    // INTERFACE for code execution on events
    public interface Code { public void onAction(int iAction, boolean bSuccess, int iExtra, Object events); }

    // CLASS for event Pool
    public static class Event{
        // EVENTS for self use
        private static final int WAITING = 0;
        private static final int SUCCESS = 1;
        private static final int FAILURE = 2;
        // EVENTS for which listeners are set
        public static final int ON_CLICK = 3;
        public static final int TEXT_CHANGE = 4;
        public static final int TEXT_ENTERED = 5;



        public Object obj;
        public int iExtra;
        public String sEvent;
        public int iStatus = WAITING; // 0 - waiting not fired yet, 1 - fired with success, 2- fired with failure
        // Variable for pool
        private Event next;  // Reference to next object
        private static Event sPool;
        private static int sPoolSize = 0;
        private static final int MAX_POOL_SIZE = 50;
        private static final Object sPoolSync = new Object();       // The lock used for synchronization

        // CONSTRUCTOR - Private
        private Event() { }
        // METHOD get pool object only through this method, so no direct allocation are made
        public static Event obtain(String sId) {
            synchronized (sPoolSync) {
                if (sPool != null) {
                    Event e = sPool;
                    e.sEvent = sId;
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
        public static void releasePool(){
            sPoolSize = 0;
            sPool = null;
        }
    }

    // CLASS for events for action, when all events occur action is triggered
    public class Action {
        private int iCodeStep;                   // Code step to execute for this action
        private int iEventCount;                 // How many event are for this action code to be triggered
        //   private boolean bEventFound;
        private boolean bRunOnUI = false;        // Code run on Background / UI thread
        public boolean bClearFired = false;      // Clear Action once fired, used for wait action
        private int iSetStatus = Event.WAITING;  // Event set status as a whole, waiting, success, non success
        private List<Event> listEvents = new ArrayList<>();         // List to store events needed for this action

        // CONSTRUCTOR
        public Action(int iCodeStep, String events[]){
            this.iCodeStep = iCodeStep;
            iEventCount = events.length;
            for(int i = 0; i < iEventCount; i++){
                listEvents.add( Event.obtain(events[i]));           // get events from events pool
            }
        }

        // METHOD recycles events and clears actions
        public void recycle(){
            int iSize = listEvents.size();
            for(int i = 0; i < iSize ; i++ ){ listEvents.get(i).recycle();}
            listEvents = null;
            waitAction = null;
        }

        // METHOD searches all actions, if any associated with this event
        public void onEvent(String sEvent, Boolean bResult, int iExtra, Object obj){
            int iFired = 0;   // How many have been fired
            int iSuccess = 0; // How many has been successful
            boolean bFound = false;

            for(int i = 0; i < iEventCount; i++){
                Event event = listEvents.get(i);
                if(sEvent.equals(event.sEvent)){ // If event is found in this event list
                    bFound = true;
                    event.obj = obj;
                    event.iExtra = iExtra;
                    event.iStatus = bResult ? Event.SUCCESS : Event.FAILURE;
                }

                switch( event.iStatus ){
                    case Event.SUCCESS:
                        iSuccess++;
                    case Event.FAILURE:
                        iFired++;
                        break;
                }
            }

            if(bFound){                             // if event was found in this Action
                if(iFired == iEventCount){          // if all events for action has been fired
                    boolean bSuccess = (iSuccess == iEventCount); // all events registered success
                    int iCurStatus = bSuccess ? Event.SUCCESS : Event.FAILURE;
                    if(iCurStatus != iSetStatus){    // If there is a change in action status only then run code
                        iSetStatus = iCurStatus;
                        if(bRunOnUI){
                            hThread.runUI(iCodeStep, bSuccess, 0, this);
                        } else {
                            hThread.run(iCodeStep, bSuccess, 0, this);
                        }
                        if(bClearFired) {
                            recycle();                  // Recycle if its flagged for it
                        }
                    }
                }
            }
        }
    }

    // CLASS for thread handler
    public class HThread implements Handler.Callback {
        private Handler mHandler;
        private Handler mHandlerUI;
        HThread(){
            iThreadCount++;
            HandlerThread ht = new HandlerThread("BGThread"+ Integer.toString(iThreadCount));
            ht.start();
            mHandler = new Handler(ht.getLooper(), this);
            mHandlerUI = new Handler(Looper.getMainLooper(), this);
        }

        public void run(int iStep){ run(iStep, false);}
        public void run(int iStep, boolean bRunUI){
            if(bRunUI){ runUI(iStep, true, 0, null); }
            else { run(iStep, true, 0, null); }
        }

        public void run(int iStep, boolean bSuccess, int iExtra, Object obj){
            if(bRunning){
                Message msg = Message.obtain();
                msg.what = iStep;
                msg.arg1 = iExtra;
                msg.arg2 = bSuccess ? 1: 0;
                msg.obj = obj;
                mHandler.sendMessage(msg);
            }
        }

        public void runUI(int iStep,  boolean bSuccess, int iExtra, Object obj){
            if(bRunning){
                Message msg = Message.obtain();
                msg.what = iStep;
                msg.arg1 = iExtra;
                msg.arg2 = bSuccess ? 1: 0;
                msg.obj = obj;
                mHandlerUI.sendMessage(msg);
            }
        }

        @Override public boolean handleMessage(Message msg) {
            final Object obj = msg.obj;
            code.onAction(msg.what, msg.arg2 == 1,  msg.arg1, obj);
            return true;
        }

        public void stop(){
            mHandler.getLooper().quit();
        }
    }


}

