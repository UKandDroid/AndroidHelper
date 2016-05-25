package com.helper.lib;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;

// CLASS for event based code execution
public class Flow {
    private CodeFlow code;
    private boolean bRunning;
    private HThread hThread ;
    private List<EventSet> listEventSet = new ArrayList<EventSet>();
    private EventSet waitEventSet = null;

    public Flow(CodeFlow list){
        bRunning = true;
        code = list;
        hThread = new HThread(code);
    }

    public void stop(){
        bRunning = false;
    }

    public void start(){
        bRunning = true;
    }

    // METHOD register for event
    public void registerEvents(int iStep, String events[]){
        listEventSet.add(new EventSet(iStep, events));
    }

    // METHOD wait for event once dispatched clears it
    public void waitForEvent( boolean bRunOnUI, int iStep, String events[]){

    }

    public void waitForEvent( int iStep, String events[]){
        waitEventSet = new EventSet(iStep, events);
    }

    // METHODS to send event
    public void event(String sEvent){ event(sEvent, true, 0, null); }
    public void event(String sEvent, boolean bSuccess){ event(sEvent, bSuccess, 0, null); }
    public void event(String sEvent, boolean bSuccess, int iExtra){ event(sEvent, bSuccess, iExtra, null); }
    public void event(String sEvent, boolean bSuccess, int iExtra, Object obj){
        if(waitEventSet != null){
            waitEventSet.onEvent(sEvent, bSuccess, iExtra, obj);
        }
        int iSize = listEventSet.size();
        for(int i = 0; i < iSize; i++){
            listEventSet.get(i).onEvent(sEvent, bSuccess, iExtra, obj);
        }
    }

    // CLASS for thread handler
    public class HThread implements Handler.Callback {
        private Handler mHandler;
        private Handler mUIHandler;
        HThread(final CodeFlow code){
            HandlerThread ht = new HandlerThread("BGThread");
            mHandler = new Handler(ht.getLooper(), this);
            mUIHandler = new Handler(Looper.getMainLooper(), this);
        }

        public void run(int iStep){
            mHandler.sendEmptyMessage(iStep);
        }

        public void runUI(int iStep){
            mUIHandler.sendEmptyMessage(iStep);
        }

        private void run(int iStep, int iExtra, Object obj){
            Message msg = Message.obtain();
            msg.what = iStep;
            msg.arg1 = iExtra;
            msg.obj = obj;
            mHandler.sendMessage(msg);

        }
        private void runUI(int iStep, int iExtra, Object obj){
            Message msg = Message.obtain();
            msg.what = iStep;
            msg.arg1 = iExtra;
            msg.obj = obj;
            mUIHandler.sendMessage(msg);
        }


        @Override public boolean handleMessage(Message msg) {
            code.code(msg.what, msg.arg1, (EventSet) msg.obj);
            msg.recycle();
            return true;
        }
    }

    // CLASS for event
    public class Event{
        public static final int NOT_FIRED = 0;
        public static final int FIRED_SUCCESS = 1;
        public static final int FIRED_FAILURE = 2;

        public Object obj;
        public int iExtra;
        public String sEvent, sTag;
        public int iStatus = NOT_FIRED; // 0 - not fired, 1 - fired with success, 2- fired with failure

        public Event(String sId){ this.sEvent = sId; }
    }

    // INTERFACE for code execution on events
    public interface CodeFlow {
        public void code(int iStep, int iExtra, EventSet eventSet);
    }

    // CLASS for event set
    public class EventSet{
        public static final int WAITING = 0;
        public static final int FIRED_SUCCESS = 1;
        public static final int FIRED_FAILURE = 2;

        private int iStep;
        private int iStatus = WAITING; // Event status, waiting, success, non success
        private int iEventCount;
        private boolean bEventFound;
        private boolean bRunOnUI = false;
        private List<String> mapEvent = new ArrayList<>();
        private List<Event> listEvents = new ArrayList<>();

        // CONSTRUCTOR
        public EventSet(int iStep, String events[]){
            this.iStep = iStep;
            iEventCount = events.length;
            for(int i = 0; i < iEventCount; i++){
                mapEvent.add(events[i]);
                listEvents.add(new Event(events[i]));
            }
        }

        // METHOD
        public void onEvent(String sEvent, Boolean bResult,  int iExtra, Object obj){
            int iResult = 0; // result of events
            int iFired = 0;  // How many have been fired
            bEventFound = false;
            for(int i=0; i < iEventCount; i++){
                switch (listEvents.get(i).iStatus){
                    case Event.FIRED_SUCCESS:
                        iResult++;
                    case Event.FIRED_FAILURE:
                        iFired++;
                        break;
                }

                if(sEvent.equals(mapEvent.get(i))){ // If event is found in this event list
                    bEventFound = true;
                    Event event = listEvents.get(i);
                    event.obj = obj;
                    event.iExtra = iExtra;
                    event.iStatus = bResult ? FIRED_SUCCESS : FIRED_FAILURE;
                }
            }

            if(bEventFound){                        // if event was found in this event list
                if(iFired == iEventCount){          // if all the events has been fired
                    int iCurStatus = (iResult == iEventCount)? FIRED_SUCCESS: FIRED_FAILURE;
                    if(iCurStatus != iStatus){
                        iStatus = iCurStatus;
                        if(bRunOnUI){
                            hThread.runUI(iStep, 0, this);
                        } else {
                            hThread.run(iStep, 0, this);
                        }
                    }
                }
            }
        }
    }

}

