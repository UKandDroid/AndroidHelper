package com.helper.lib;

import android.app.Activity;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.graphics.Rect;
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
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UiFlow implements LifecycleObserver {
    private interface KeyboardState { public void onStateChange(boolean bVisible); }

    static class UiEvent {
        // EVENTS for which listeners are set
        static final int TOUCH = 3;
        static final int ON_CLICK = 4;
        static final int TEXT_CHANGED = 5;
        static final int TEXT_ENTERED = 6;
        static final int CHECKBOX_STATE = 7;
        static final int LIST_ITEM_SELECT = 8;
        static final int SPINNER_ITEM_SELECT = 9;
        static final int KEYBOARD_STATE_CHANGE = 10; //   works only for android:windowSoftInputMode="adjustResize" or adjustPan
        static final int LOAD_LAYOUT = 11;      //   called when a view is loaded with width and height set
    }

    interface Code{
        void onAction(int action, boolean bSuccess, int iExtra, Object tag);
    }

    private Code code;
    private View viewActRoot;
    private int iSoftInputMode = -1;
    private Rect rLast = new Rect();
    private int iThreadCount = 0;
    private boolean bKeybVisible = false;
    private HThread hThread = new HThread();
    private List<KeyboardState> keyList = new ArrayList<>();
    private HashMap listTextListeners = new HashMap();        // list of text change listeners for a text field
    private HashMap listKBListeners = new HashMap();        // list of keyboard state change listeners

    public UiFlow(){ }
    public UiFlow(Code codeCallback) {
        this.code = codeCallback;
    }


    // METHODS registers/un registers UI events for Action
    public void unRegisterUIEvent( View view, int iEvent) { unRegisterListener(view, iEvent); }
    public void registerUiEvent( View view) { registerListener(false, -1, view, UiEvent.ON_CLICK); }
    public void registerUiEvent(final int iAction, View view) { registerListener(false, iAction, view, UiEvent.ON_CLICK); }
    public UiFlow registerUiEvent( View view, int iEvent) { registerListener(false, -1, view, iEvent); return this;}
    public UiFlow registerUiEvent(final int iAction, View view, int iEvent) { registerListener(false, iAction, view, iEvent); return this;}
    public void registerUiEvent(int iStep, boolean bRunOnUI, View view) { registerListener(bRunOnUI, iStep, view, UiEvent.ON_CLICK); }
    public UiFlow registerUiEvent( boolean bRunOnUI, View view, int iEvent) { registerListener(bRunOnUI, -1, view, iEvent); return this; }
    public UiFlow registerUiEvent(int iAction, boolean bRunOnUI, View view, int iEvent) { registerListener(bRunOnUI, iAction, view, iEvent); return this; }



    // VIEW LISTENERS set event listeners for View objects
    private void registerListener(final boolean bRunOnUI, final int iAction, final View view, int iListener) {
        switch (iListener) {
            // Triggered when ui layout changes with width/height values > 0 and called only once
            case UiEvent.LOAD_LAYOUT:
                view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                        if((i+i1+i2+i3) > 0) {
                            view.removeOnLayoutChangeListener(this);
                            if (bRunOnUI) {
                                hThread.runOnUI(iAction, true, 0, view);
                            } else {
                                hThread.run(iAction, true, 0, view);
                            } } }});
                break;

            // triggered listener when view is clicked
            case UiEvent.ON_CLICK:
                if(view instanceof EditText){                                                         // NOTE: for editText  first tap get focus, 2nd to trigger onClick, unless focusable is setfalse()
                    view.setOnFocusChangeListener((v, hasFocus) -> {
                        if(hasFocus){
                            if (bRunOnUI) {
                                hThread.runOnUI(iAction, true, 0, view);
                            } else {
                                hThread.run(iAction, true, 0, view);
                            }
                        }});
                }
                view.setOnClickListener(view1 -> {
                    if (bRunOnUI) {
                        hThread.runOnUI(iAction, true, 0, view1);
                    } else {
                        hThread.run(iAction, true, 0, view1);
                    }
                });
                break;

            case UiEvent.KEYBOARD_STATE_CHANGE: // Method reports keyboard state change, should be provided with root activity view (activity.window.decorView)
                KeyboardState list  = new KeyboardState() {
                    @Override public void onStateChange(boolean bVisible) {
                        if(view.hasFocus()){
                            if (bRunOnUI) {
                                hThread.runOnUI(iAction, bVisible, 0, view);
                            } else {
                                hThread.run(iAction, bVisible, 0, view);
                            }}}};
                setUpKeybListener(list, view);
                break;

            // Triggered when Text entered in text field, i.e when text field loses focus, enter button is pressed on keyboard
            // for text entered to work with keyboard hide, set android:windowSoftInputMode="adjustResize" or "adjustPan"
            // and setup KEYBOARD_STATE UiEvent, provided with main activity root decor view
            case UiEvent.TEXT_ENTERED:
                KeyboardState listKb = bVisible -> {
                    if(view.hasFocus() && !bVisible){
                        if (bRunOnUI) { hThread.runOnUI(iAction, bVisible, 0, view);
                        } else { hThread.run(iAction, bVisible, 0, view); }}};
                listKBListeners.put(view, listKb);
                addKeybListener(listKb );

                view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (!hasFocus) {
                            Log.w("UiFlow", "Text ENTERED on Lost focus");
                            if (bRunOnUI) {
                                hThread.runOnUI(iAction, true, 0, view);
                            } else {
                                hThread.run(iAction, true, 1, view);
                            }}
                    }});

                ((EditText) view).setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        Log.w("UiFlow", "Text ENTERED on KB Done");
                        if (bRunOnUI) {
                            hThread.runOnUI(iAction, true, 0, view);
                        } else {
                            hThread.run(iAction, true, 3, view);
                        }
                    }
                    return false;
                });
                break;

            // Triggered when text changes
            case UiEvent.TEXT_CHANGED:
                TextWatcher txtListen =  new TextWatcher() {
                    @Override public void afterTextChanged(Editable s) {}
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (bRunOnUI) {
                            hThread.runOnUI(iAction, true, 0, view);
                        } else {
                            hThread.run(iAction, true, 0, view);
                        }}
                };
                listTextListeners.put(view, txtListen);
                ((EditText) view).addTextChangedListener(txtListen);
                break;

            case UiEvent.LIST_ITEM_SELECT:
                ((ListView) view).setOnItemClickListener((parent, view12, position, id) -> {
                    if (bRunOnUI) {
                        hThread.runOnUI(iAction, true, position, view12);
                    } else {
                        hThread.run(iAction, true, position, view12);
                    }
                });
                break;

            case UiEvent.SPINNER_ITEM_SELECT:
                ((Spinner) view).setOnItemSelectedListener(
                        new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                if (bRunOnUI) {
                                    hThread.runOnUI(iAction, true, position, view);
                                } else {
                                    hThread.run(iAction, true, position, view);
                                }
                            }
                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                                if (bRunOnUI) {
                                    hThread.runOnUI(iAction, false, -1, view);
                                } else {
                                    hThread.run(iAction, false, -1, view);
                                }
                            }
                        });
                break;

            case UiEvent.CHECKBOX_STATE:
                ((CheckBox) view).setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (bRunOnUI) {
                        hThread.runOnUI(iAction, isChecked, 0, view);
                    } else {
                        hThread.run(iAction, isChecked, 0, view);
                    }
                });
                break;

            case UiEvent.TOUCH:           // Listener returns true for Touch down and Move, false when finger is lifted up
                view.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (bRunOnUI) {
                            hThread.runOnUI(iAction, event.getAction() != MotionEvent.ACTION_UP, event.getAction(), event);
                        } else {
                            hThread.run(iAction, event.getAction() != MotionEvent.ACTION_UP, event.getAction(), event);
                        }
                        return false;
                    }
                });
        }
    }

    // VIEW LISTENERS set event listeners for View objects
    private void unRegisterListener( final View view, int iListener) {
        switch (iListener) {
            case UiEvent.ON_CLICK:
                if(view instanceof EditText){ view.setOnFocusChangeListener(null); }
                view.setOnClickListener(null);
                break;

            case UiEvent.TEXT_ENTERED:
                listKBListeners.remove(view);
                view.setOnFocusChangeListener(null);
                ((EditText) view).setOnEditorActionListener(null);
                break;

            case UiEvent.KEYBOARD_STATE_CHANGE: removeKeybListener();                           break;
            case UiEvent.TEXT_CHANGED: listTextListeners.remove(view);                          break;
            case UiEvent.LIST_ITEM_SELECT: ((ListView) view).setOnItemClickListener(null);      break;
            case UiEvent.SPINNER_ITEM_SELECT:((Spinner) view).setOnItemSelectedListener(null);  break;
            case UiEvent.CHECKBOX_STATE:((CheckBox) view).setOnCheckedChangeListener(null);     break;
            case UiEvent.TOUCH:view.setOnTouchListener(null);                                   break;
            case UiEvent.LOAD_LAYOUT: view.removeOnLayoutChangeListener(null);                 break;
        }
    }

    // METHOD - sets/removes global keyboard listener, also sets resets SoftInputMode
    private void removeKeybListener() {
        viewActRoot.getViewTreeObserver().removeOnGlobalLayoutListener(keybListener);
        Activity act = (Activity) ((ViewGroup) viewActRoot).getChildAt(0).getContext();
        Window window = act.getWindow();
        iSoftInputMode = window.getAttributes().softInputMode;     // save it so we can restore, when keyboard listener is removed
        if(iSoftInputMode != -1)
            window.setSoftInputMode(iSoftInputMode);

    }

    // METHOD - sets up a listener for keyboard state change, also change SoftInputMode if its not correct
    private void setUpKeybListener(final KeyboardState keyListener, final View view) {
        Activity act = (Activity) ((ViewGroup) view).getChildAt(0).getContext();  // Change soft input mode to SOFT_INPUT_ADJUST_PAN or SOFT_INPUT_ADJUST_RESIZE, for it to work
        Window window = act.getWindow();
        iSoftInputMode = window.getAttributes().softInputMode;     // save it so we can restore, when keyboard listener is removed
        if(iSoftInputMode != WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN && iSoftInputMode != WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE )
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        rLast = new Rect();         // set a new rect for storing screen state
        viewActRoot = view;
        keyList.add(keyListener);
        view.getViewTreeObserver().addOnGlobalLayoutListener(keybListener);
    }

    private void addKeybListener(final KeyboardState keyListener){
        keyList.add(keyListener);
    }

    private ViewTreeObserver.OnGlobalLayoutListener keybListener =  new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override public void onGlobalLayout() {
            Rect rCur = new Rect();
            viewActRoot.getWindowVisibleDisplayFrame(rCur);

            if(rLast.bottom == 0){ rLast.bottom = viewActRoot.getHeight();} // just get size of window, something to start with

            if((rLast.bottom - rCur.bottom) > 200){ // means keyboard is visible
                if(!bKeybVisible){                // if its not already set set it
                    bKeybVisible = true;
                    rLast = rCur;
                    for(int i=0; i < keyList.size(); i++)
                        keyList.get(i).onStateChange(true);
                }
            } else if( (rCur.bottom - rLast.bottom) > 200){
                if(bKeybVisible){
                    bKeybVisible = false;
                    rLast = rCur;
                    for(int i=0; i < keyList.size(); i++)
                        keyList.get(i).onStateChange(false);
                }
            }
        }
    };


    public void pause() {
    }

    public void resume() {
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stop() {
        if(viewActRoot != null){
            viewActRoot.getViewTreeObserver().removeOnGlobalLayoutListener(null);
            viewActRoot = null;
            hThread.stop();
        }
    }

    // CLASS for thread handler
    private class HThread implements Handler.Callback {
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

        private void run(int iStep, boolean bRunUI) {
            if (bRunUI) {
                runOnUI(iStep, true, 0, null);
            } else {
                run(iStep, true, 0, null);
            }
        }

        private void run(int iStep, boolean bSuccess, int iExtra, Object obj) {

                Message msg = Message.obtain();
                msg.what = iStep;
                msg.arg1 = iExtra;
                msg.arg2 = bSuccess ? 1 : 0;
                msg.obj = obj;
                mHandler.sendMessage(msg);

        }

        private void runOnUI(int iStep, boolean bSuccess, int iExtra, Object obj) {
                Message msg = Message.obtain();
                msg.what = iStep;
                msg.arg1 = iExtra;
                msg.arg2 = bSuccess ? 1 : 0;
                msg.obj = obj;
                mUiHandler.sendMessage(msg);

        }


        // METHOD MESSAGE HANDLER
        @Override
        public boolean handleMessage(Message msg) {
            code.onAction(msg.what, msg.arg2 == 1, msg.arg1, msg.obj);
            return true;
        }

        public void stop() {
            mHandler.removeCallbacksAndMessages(null);
            mUiHandler.removeCallbacksAndMessages(null);
            mHandler.getLooper().quit();
        }
    }


}
