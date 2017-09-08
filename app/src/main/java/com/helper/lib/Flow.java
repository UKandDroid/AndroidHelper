package com.helper.lib;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;



// Version 2.0.3
// Added on touch listener for view
// Added SPINNER_ITEM_SELECTED ui event
// Changed onClick for EditText as it takes two clicks when not in focus to register onClick

public class Flow {
    private Code code;                                      // Call back for onAction to be executed
    private boolean bRunning;
    private HThread hThread;
    private int iKeybCount = 0;
    private EditText selText;                               // Selected text filed, used for EditText text enter
    private ViewGroup keybViewRoot;
    private Action waitAction = null;
    private static int iThreadCount = 0;
    private boolean bTextEntered = false;
    private boolean bKeyboardVisible = false;
    private RelativeLayout layoutKbDetect = null;           // For edit text, text entered event, check if keyboard is hidden
    private static final String LOG_TAG = "Flow";
    private static final int LOG_LEVEL = 4;
    private List<Action> listActions = new ArrayList<Action>();  // List of registered actions
    private static final int FLAG_SUCCESS = 0x00000001;
    private static final int FLAG_RUNonUI = 0x00000002;
    private static final int FLAG_REPEAT = 0x00000004;

    public Flow(Code codeCallback) {
        bRunning = true;
        code = codeCallback;
        hThread = new HThread();
    }

    // STATE METHODS pause, resume, stop the action, should be called to release resources
    public void pause() {
        bRunning = false;
    }

    public void resume() {
        bRunning = true;
    }

    public void stop() {
        try {
            bRunning = false;
            for (int i = 0; i < listActions.size(); i++) {
                listActions.get(i).recycle();
            }
            hThread.stop();
            listActions = null;
            waitAction = null;
            Event.releasePool();

            if (keybViewRoot != null && layoutKbDetect != null)
                keybViewRoot.removeView(layoutKbDetect);
        } catch (Exception e) {}
    }

    // METHODS run an action

    public void run(int iAction) {
        hThread.run(iAction);
    }

    public void run(int iAction, int iExtra, Object obj) {
        hThread.run(iAction, true, iExtra, obj);
    }

    public void runOnUI(int iAction) {
        hThread.run(iAction, true);
    }

    public void runOnUI(int iAction, int iExtra, Object obj) {
        hThread.runOnUI(iAction, true, iExtra, obj);
    }

    public void runOnUI(int iAction, boolean bSuccess, int iExtra, Object obj) {
        hThread.runOnUI(iAction, bSuccess, iExtra, obj);
    }

    // METHODS run repeated actions / Timer
    public void runRepeat(int iAction, long iDelay) {
        hThread.runRepeat(false, iAction, true, 0, iDelay);
    }

    public void runRepeat(int iAction, boolean bSuccess, int iExtra, long iDelay) {
        hThread.runRepeat(false, iAction, bSuccess, iExtra, iDelay);
    }

    public void runRepeatOnUI(int iAction, long iDelay) {
        hThread.runRepeat(true, iAction, true, 0, iDelay);
    }

    public void runRepeatOnUI(int iAction, boolean bSuccess, int iExtra, long iDelay) {
        hThread.runRepeat(true, iAction, bSuccess, iExtra, iDelay);
    }

    // METHODS run action delayed
    public void runDelayed(int iAction, long iTime) {
        runDelayed(iAction, true, 0, iTime);
    }

    public void runDelayedOnUI(int iAction, long iTime) {
        runDelayedOnUI(iAction, true, 0, iTime);
    }

    public void runDelayedOnUI(int iAction, boolean bSuccess, int iExtra, long iTime) {
        Message msg = Message.obtain();
        msg.what = iAction;
        msg.arg1 = iExtra;
        msg.arg2 = bSuccess ? 1 : 0;
        hThread.mUiHandler.removeMessages(iAction);                             // Remove any pending messages in queue
        hThread.mUiHandler.sendMessageDelayed(msg, iTime);
    }

    public void runDelayed(int iAction, boolean bSuccess, int iExtra, long iTime) {
        Message msg = Message.obtain();
        msg.what = iAction;
        msg.arg1 = iExtra;
        msg.arg2 = bSuccess ? 1 : 0;
        hThread.mHandler.removeMessages(iAction);                               // Remove any pending messages in queue
        hThread.mHandler.sendMessageDelayed(msg, iTime);
    }

    // METHODS register for events, Registered events are permanent, unlike waitForEvents
    public void registerEvents(int iStep, String events[]) {
        registerEvents(false, iStep, events);
    }

    public void registerEvents(boolean bRunOnUI, int iStep, String events[]) {
        Action act = new Action(iStep, events);
        act.bRunOnUI = bRunOnUI;
        listActions.add(act);
    }

    // METHOD wait for event once dispatched clear it
    public void waitForEvents(int iAction, String events[]) {
        waitForEvents(false, iAction, events);
    }

    public void waitForEvents(boolean bRunOnUI, int iAction, String events[]) {
        waitAction = new Action(iAction, events);
        waitAction.bRunOnUI = bRunOnUI;
        waitAction.bFireOnce = true;                  // waitForEvents is fired only once, and is released after fired
    }

    // METHODS register UI events for Action
    public void registerUIEvent(final int iStep, View view) {
        registerListener(false, iStep, view, Event.ON_CLICK);
    }

    public void registerUIEvent(final int iStep, View view, int iEvent) {
        registerListener(false, iStep, view, iEvent);
    }

    public void registerUIEvent(boolean bRunOnUI, int iStep, View view) {
        registerListener(bRunOnUI, iStep, view, Event.ON_CLICK);
    }

    public void registerUIEvent(boolean bRunOnUI, int iStep, View view, int iEvent) {
        registerListener(bRunOnUI, iStep, view, iEvent);
    }

    // METHODS to send event
    public void event(String sEvent) {
        event(sEvent, true, 0, null);
    }

    public void event(String sEvent, boolean bSuccess) {
        event(sEvent, bSuccess, 0, null);
    }

    public void event(String sEvent, boolean bSuccess, int iExtra) {
        event(sEvent, bSuccess, iExtra, null);
    }

    public void event(String sEvent, boolean bSuccess, int iExtra, Object obj) {
        if (!bRunning) return;

        if (waitAction != null) {
            waitAction.onEvent(sEvent, bSuccess, iExtra, obj);
        }
        int iSize = listActions.size();
        for (int i = 0; i < iSize; i++) {
            listActions.get(i).onEvent(sEvent, bSuccess, iExtra, obj);
        }
    }

    // METHOD cancel a runDelay or RunRepeated
    public void cancelRun(int iAction) {
        if (!bRunning) return;
        hThread.mHandler.removeMessages(iAction);
        hThread.mUiHandler.removeMessages(iAction);
    }

    // INTERFACE for code execution on events
    public interface Code {
        public void onAction(int iAction, boolean bSuccess, int iExtra, Object data);
    }

    // CLASS for event Pool
    public static class Event {
        // EVENTS for self use
        private static final int WAITING = 0;
        private static final int SUCCESS = 1;
        private static final int FAILURE = 2;

        // EVENTS for which listeners are set
        public static final int TOUCH = 3;
        public static final int ON_CLICK = 4;
        public static final int TEXT_CHANGE = 5;
        public static final int TEXT_ENTERED = 6;
        public static final int CHECKBOX_STATE = 7;
        public static final int LIST_ITEM_SELECT = 8;
        public static final int SPINNER_ITEM_SELECT = 9;


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
        private Event() {
        }

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
        public static void releasePool() {
            sPoolSize = 0;
            sPool = null;
        }
    }

    // CLASS for events for action, when all events occur action is triggered
    public class Action {
        private int iCodeStep;                                      // Code step to execute for this action
        private int iEventCount;                                    // How many event are for this action code to be triggered
        //   private boolean bEventFound;
        private boolean bRunOnUI = false;                           // Code run on Background / UI thread
        public boolean bFireOnce = false;                           // Clear Action once fired, used for wait action
        private int iSetStatus = Event.WAITING;                     // Event set status as a whole, waiting, success, non success
        private List<Event> listEvents = new ArrayList<>();         // List to store events needed for this action

        // CONSTRUCTOR
        public Action(int iCodeStep, String events[]) {
            this.iCodeStep = iCodeStep;
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
            waitAction = null;
        }

        // METHOD searches all actions, if any associated with this event
        public void onEvent(String sEvent, Boolean bResult, int iExtra, Object obj) {
            int iFired = 0;                     // How many have been fired
            int iSuccess = 0;                   // How many has been successful
            boolean bFound = false;

            for (int i = 0; i < iEventCount; i++) {
                Event event = listEvents.get(i);
                if (sEvent.equals(event.sEvent)) { // If event is found in this event list
                    bFound = true;
                    event.obj = obj;
                    event.iExtra = iExtra;
                    event.iStatus = bResult ? Event.SUCCESS : Event.FAILURE;
                }

                switch (event.iStatus) {
                    case Event.SUCCESS:
                        iSuccess++;
                    case Event.FAILURE:
                        iFired++;
                        break;
                }
            }

            if (bFound) {                             // if event was found in this Action
                if (iFired == iEventCount) {          // if all events for action has been fired
                    boolean bSuccess = (iSuccess == iEventCount); // all events registered success
                    int iCurStatus = bSuccess ? Event.SUCCESS : Event.FAILURE;
                    if (iCurStatus != iSetStatus) {    // If there is a change in action status only then run code
                        iSetStatus = iCurStatus;
                        if (bRunOnUI) {
                            hThread.runOnUI(iCodeStep, bSuccess, 0, this.listEvents);
                        } else {
                            hThread.run(iCodeStep, bSuccess, 0, this.listEvents);
                        }
                        if (bFireOnce) {
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
                code.onAction(msg.what, getFlag(msg.arg2, FLAG_SUCCESS), getExtraInt(msg.arg2), msg.obj);
            } else {
                code.onAction(msg.what, msg.arg2 == 1, msg.arg1, msg.obj);
            }
            return true;
        }

        public void stop() {
            mHandler.removeCallbacksAndMessages(null);
            mUiHandler.removeCallbacksAndMessages(null);
            mHandler.getLooper().quit();
            if (layoutKbDetect != null) {
                keybViewRoot.removeView(layoutKbDetect);
                layoutKbDetect.addOnLayoutChangeListener(null);
            }
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

    // VIEW LISTENERS set event listeners for View objects
    private void registerListener(final boolean bRunOnUI, final int iAction, final View view, int iListener) {
        switch (iListener) {
            // triggered listener when view is clicked
            case Event.ON_CLICK:
                if(view instanceof EditText){                                                         // NOTE: for editText  first tap get focus, 2nd to trigger onClick, unless focusable is setfalse()
                    view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            if(hasFocus){
                                if (bRunOnUI) {
                                    hThread.runOnUI(iAction, true, 0, view);
                                } else {
                                    hThread.run(iAction, true, 0, view);
                                }
                            }}});
                }
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (bRunOnUI) {
                            hThread.runOnUI(iAction, true, 0, view);
                        } else {
                            hThread.run(iAction, true, 0, view);
                        }
                    }
                });
                break;

            // Triggered when Text entered in text field, i.e when text field loses focus, enter button is pressed or keyboard is closed
            case Event.TEXT_ENTERED:
                if(view.getWidth() != 0){                                                           // If view is created, set it up else wati
                    keyboardHideActionForText(Flow.this, bRunOnUI, iAction, view);
                    view.setTag(iAction);
                    selText = (EditText) view;
                }
                view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        String sFieldName = v.getResources().getResourceName(v.getId());
                        sFieldName = sFieldName.split("/")[1];
                        if (!hasFocus) {
                            log(4,  sFieldName + " field lost focus " );
                            if (!bTextEntered) {  // If Event has not already been triggered by closing the keyboard
                                logw(4, "Text ENTERED on Lost focus");
                                bTextEntered = true;
                                if (bRunOnUI) {
                                    hThread.runOnUI(iAction, true, 0, view);
                                } else {
                                    hThread.run(iAction, true, 1, view);
                                }
                            }
                        } else {
                            selText = (EditText) v;
                            bTextEntered = false;
                            log(4, sFieldName + " field lost focus ");
                            if (view.getTag() == null) {
                                keyboardHideActionForText(Flow.this, bRunOnUI, iAction, view);
                                view.setTag(iAction);
                            }
                        }}});

                ((EditText) view).setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            logw(4, "Text ENTERED on KB Done");
                            if (bRunOnUI) {
                                hThread.runOnUI(iAction, true, 0, view);
                            } else {
                                hThread.run(iAction, true, 3, view);
                            }
                            bTextEntered = true;
                        }
                        return false;
                    }});
                break;

            // Triggered when text changes
            case Event.TEXT_CHANGE:
                ((EditText) view).addTextChangedListener(new TextWatcher() {
                    @Override
                    public void afterTextChanged(Editable s) {
                    }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (bRunOnUI) {
                            hThread.runOnUI(iAction, true, 0, view);
                        } else {
                            hThread.run(iAction, true, 0, view);
                        }
                    }
                });
                break;

            case Event.LIST_ITEM_SELECT:
                ((ListView) view).setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (bRunOnUI) {
                            hThread.runOnUI(iAction, true, position, view);
                        } else {
                            hThread.run(iAction, true, position, view);
                        }
                    }
                });
                break;

            case Event.SPINNER_ITEM_SELECT:
                ((Spinner) view).setOnItemSelectedListener(
                        new AdapterView.OnItemSelectedListener() {
                            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                if (bRunOnUI) {
                                    hThread.runOnUI(iAction, true, position, view);
                                } else {
                                    hThread.run(iAction, true, position, view);
                                }

                            }
                            @Override public void onNothingSelected(AdapterView<?> parent) {
                                if (bRunOnUI) {
                                    hThread.runOnUI(iAction, false, -1, view);
                                } else {
                                    hThread.run(iAction, false, -1, view);
                                }
                            }
                        });
                break;

            case Event.CHECKBOX_STATE:
                ((CheckBox) view).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (bRunOnUI) {
                            hThread.runOnUI(iAction, isChecked, 0, view);
                        } else {
                            hThread.run(iAction, isChecked, 0, view);
                        }
                    }
                });
                break;

            case Event.TOUCH:           // Listener returns true for Touch down and Move, false when finger is lifted up
                view.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (bRunOnUI) {
                            hThread.runOnUI(iAction, event.getAction() != MotionEvent.ACTION_UP, event.getAction(), view);
                        } else {
                            hThread.run(iAction, event.getAction() != MotionEvent.ACTION_UP, event.getAction(), view);
                        }
                        return true;
                    }
                });
        }
    }


    // METHOD - run action when keyboard is hidden, works only for text field, when its focused
    public void keyboardHideActionForText(final Flow flow, final boolean bRunOnUI, final int iAction, final Object obj) {

        if (layoutKbDetect == null || layoutKbDetect.getParent() == null) {
            iKeybCount = 0;                                                                         // if its not already setup
            keybViewRoot = (ViewGroup) ((View) obj).getRootView();
            if (layoutKbDetect == null) {
                layoutKbDetect = new RelativeLayout(((View) obj).getContext());
                layoutKbDetect.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
            layoutKbDetect.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                    if (++iKeybCount > 1) {                                                         // Ignore first Two calls, layout is called twice when keyboard is first displayed
                        bKeyboardVisible = !bKeyboardVisible;
                        log(4, "Keyboard " + (bKeyboardVisible ? "open" : "close"));
                        if (selText.isFocused() && !bKeyboardVisible) {                             // if text field is focused and keyboard is hidden run action
                            logw(4, "Text ENTERED on KB Hidden ");
                            bTextEntered = true;
                            if (bRunOnUI) {
                                flow.runOnUI((Integer) selText.getTag(), 2, selText);
                            } else {
                                flow.run((Integer) selText.getTag(), 2, selText);
                            }
                        }
                    }
                }
            });

            keybViewRoot.addView(layoutKbDetect);
        }
    }

    // METHOD for logging
    private void log(String sLog) {
        log(1, sLog);
    }

    private void loge(String sLog) {
        loge(1, sLog);
    }

    private void logw(String sLog) {
        logw(1, sLog);
    }

    private void log(int iLevel, String sLog) {
        if (iLevel <= LOG_LEVEL) {
            Log.d(LOG_TAG, sLog);
        }
    }

    private void loge(int iLevel, String sLog) {
        if (iLevel <= LOG_LEVEL) {
            Log.e(LOG_TAG, sLog);
        }
    }

    private void logw(int iLevel, String sLog) {
        if (iLevel <= LOG_LEVEL) {
            Log.w(LOG_TAG, sLog);
        }
    }
}

