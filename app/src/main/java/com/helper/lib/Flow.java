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
    private EventSet waitEventSet = null;
    private  static int iThreadCount = 0;
    private List<EventSet> listEventSet = new ArrayList<EventSet>();

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


    public void run(int iCodeStep){
        hThread.run(iCodeStep);
    }

    public void runUI(int iCodeStep){
        hThread.runUI(iCodeStep);
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
        waitEventSet.bReleaseFired = true; // Release this eventset once its fired
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


    // INTERFACE for code execution on events
    public interface CodeFlow {
        public void code(int iStep, boolean bSuccess,  int iExtra, Object events);
    }

    // CLASS for event Pool
    public static class Event{
        public static final int WAITING = 0;
        public static final int SUCCESS = 1;
        public static final int FAILURE = 2;

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
                    Event m = sPool;
                    m.sEvent = sId;
                    sPool = m.next;
                    m.next = null;
                    sPoolSize--;
                    return m;
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
            sPool = null;
        }

        public Object obj;
        public int iExtra;
        public String sEvent;
        public int iStatus = WAITING; // 0 - waiting not fired yet, 1 - fired with success, 2- fired with failure
        // CONSTRUCTOR
        //  public Event(String sId){ this.sEvent = sId; }
    }


    // CLASS for event set
    public class EventSet{
        private int iCodeStep;               // Code step to execute when event happens
        private int iEventCount;             // How many event are for this event code to be triggered
        private boolean bEventFound;
        private boolean bRunOnUI = false;    // Code run on Background / UI thread
        public boolean bReleaseFired = false;    // release events for once fired, used for wait events
        private int iSetStatus = Event.WAITING; // Event set status as a whole, waiting, success, non success
        private List<Event> listEvents = new ArrayList<>();         //

        // CONSTRUCTOR
        public EventSet(int iCodeStep, String events[]){
            this.iCodeStep = iCodeStep;
            iEventCount = events.length;
            for(int i = 0; i < iEventCount; i++){
                listEvents.add( Event.obtain(events[i]));
            }
        }

        public void recycle(){
        int iSize = listEvents.size();
            for(int i = 0; i < iSize ; i++ ){
                listEvents.get(i).recycle();
            }
            listEvents = null;
            waitEventSet = null;
        }

        // METHOD
        public void onEvent(String sEvent, Boolean bResult, int iExtra, Object obj){
            int iSuccess = 0; // result of events
            int iFired = 0;  // How many have been fired
            bEventFound = false;

            for(int i = 0; i < iEventCount; i++){
                Event event = listEvents.get(i);
                if(sEvent.equals(event.sEvent)){ // If event is found in this event list
                    bEventFound = true;
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

            if(bEventFound){                        // if event was found in this event list
                if(iFired == iEventCount){          // if all the events has been fired
                    boolean bSuccess = (iSuccess == iEventCount);
                    int iCurStatus =  bSuccess ? Event.SUCCESS : Event.FAILURE;
                    if(iCurStatus != iSetStatus){      // If there is a change only then
                        iSetStatus = iCurStatus;
                        if(bRunOnUI){
                            hThread.runUI(iCodeStep, bSuccess, 0, this);
                        } else {
                            hThread.run(iCodeStep, bSuccess, 0, this);
                        }
                        if(bReleaseFired){
                            recycle();
                        }
                    }
                }
            }
        }
    }

    // CLASS for thread handler
    public class HThread implements Handler.Callback {
        private Handler mHandler;
        private Handler mUIHandler;
        HThread(final CodeFlow code){
            iThreadCount++;
            HandlerThread ht = new HandlerThread("BGThread"+ Integer.toString(iThreadCount));
            ht.start();
            mHandler = new Handler(ht.getLooper(), this);
            mUIHandler = new Handler(Looper.getMainLooper(), this);
        }

        public void run(int iStep){
            Message msg = Message.obtain();
            msg.what = iStep;
            msg.arg2 =  1;  // Success set to true
            mHandler.sendMessage(msg);        }

        public void runUI(int iStep){
            Message msg = Message.obtain();
            msg.what = iStep;
            msg.arg2 =  1;  // Success set to true
            mUIHandler.sendMessage(msg);
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
                mUIHandler.sendMessage(msg);
            }
        }


        @Override public boolean handleMessage(Message msg) {
            final Object obj = msg.obj;
            code.code(msg.what, msg.arg2 == 1,  msg.arg1, obj);
            return true;
        }
    }


}
