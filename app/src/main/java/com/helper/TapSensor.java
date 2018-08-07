package com.helper;

import android.view.MotionEvent;

import com.helper.lib.Flow;
import com.helper.lib.Logger;

public final class TapSensor {
    private final Logger log;
    private int iFingerCount;
    private int fingerId;
    private float touchTime;
    private Flow flowOutput;
    private final Flow touchEvent;
    private static long iTimer;

    public TapSensor(Flow flowInput) {
        this.touchEvent = flowInput;
        this.log = new Logger();
        this.flowOutput = new Flow();
        start();
    }

    private final float getTimer() {
        return (float)(System.currentTimeMillis() - iTimer) / 1000.0F;
    }

    private final void start() {
        touchEvent.code((new Flow.Code() {
            public final void onAction(int i, boolean b, int i1, Object any) {
                handleEvent((MotionEvent)any, i);
            }
        }));
    }

    public final Flow onData() {
        return flowOutput;
    }

    public final void reset() {
        iTimer = 0L;
        fingerId = 0;
    }

    private final void handleEvent(MotionEvent event, int iButton) {
        int index = event.getActionIndex();
        int action = event.getActionMasked();
        int pointerId = event.getPointerId(index);
        if (iTimer == 0L) { iTimer = System.currentTimeMillis(); }

        switch(action) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN:
                if (iFingerCount == 0) {
                    touchTime = this.getTimer();
                    fingerId = pointerId;
                    iFingerCount++;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                if (iFingerCount > 0 && fingerId == pointerId) {
                    iFingerCount--;
                    flowOutput.run(iButton, 0, new TapSensor.TapData(iButton, touchTime, this.getTimer() - touchTime, event.getRawX(), event.getRawY()));
                }
                break;
        }
    }


    public static final class TapData {
        private int iButton;
        private float fStartTime;
        private float fDuration;
        private float x;
        private float y;

        public TapData(int iButton, float fStartTime, float fDuration, float rawX, float rawY) {
            this.iButton = iButton;
            this.fStartTime = fStartTime;
            this.fDuration = fDuration;
            this.x = rawX;
            this.y = rawY;
        }

        public String toString() {
            return "\"<ORKTappingSample: 0x00000000; button: " + this.iButton + "; timestamp: " + this.fStartTime + "; timestamp: " + this.fDuration + "; location: {" + this.x + ", " + this.y + "}>\"";
        }
    }
}
