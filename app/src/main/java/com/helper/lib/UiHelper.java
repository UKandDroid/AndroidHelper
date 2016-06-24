package com.helper.lib;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by Ubaid on 15/04/2016.
 */
// Version 1.1.1
public class UiHelper {

    private Context context;
    private ViewGroup rootView;
    private int[] arId = new int[256];   // Store loaded arView arId, so they can checked against.
    private View[] arView = new View[256];// keeps reference of loaded views, so they are not loaded again..
    private RelativeLayout layoutProgress = null;
    private ProgressBar progressBar;
    private static int iKbCount = 0;
    private ProgressDialog progressDialog;
    private boolean bKeyboardVisible = false;
    private RelativeLayout layoutKbDetect = null;
    public interface ThreadCode { public void execute(); }

    // CONSTRUCTORS
    public UiHelper() {}
    public UiHelper(View v){
        setRootView(v);
    }
    public void setRootView(View v){
        rootView = (ViewGroup)v;
        context = rootView.getContext();
    }
    public ViewGroup getRootView(){
        return rootView;
    }
    // METHODS - returns arView based on type
    public TextView textView(int id){ return (TextView)getView(id); }
    public EditText editText(int id){ return (EditText)getView(id); }
    public Button button(int id){ return (Button)getView(id);}
    // METHOD - sets Background for a arView
    public void setBackground(int id, int resource){
        getView(id).setBackgroundResource(resource);
    }
    // METHOD - sets visibility of view to Visible
    public void setVisible(int id){
        getView(id).setVisibility(View.VISIBLE);
    }
    public void setInvisible(int id){
        getView(id).setVisibility(View.INVISIBLE);
    }
    public void setGone(int id){
        getView(id).setVisibility(View.GONE);
    }
    public void setVisibility(int id, int visibility){
        getView(id).setVisibility(visibility);
    }
    public void setClickable(int id, boolean bTrue){ getView(id).setClickable(bTrue); }

    // METHOD - sets text for Button, TextView, EditText
    public void setText(final int id, final String sText){
        final View view = getView(id);
        if(Looper.myLooper() == Looper.getMainLooper()) {  // if current thread is main thread
            ((TextView) view).setText(sText);
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ((TextView) view).setText(sText);// Update it on main thread
                }});
        }
    }
    // METHOD - sets text  and Text color for Button, TextView, EditText
    public void setText(final int id, final int iColor, final String sText ){
        final View view = getView(id);
        if(Looper.myLooper() == Looper.getMainLooper()) {  // if current thread is main thread
            ((TextView) view).setTextColor(iColor);
            ((TextView) view).setText(sText);
        } else {                                           // Update it on main thread
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ((TextView) view).setText(sText);// Update it on main thread
                    ((TextView) view).setTextColor(iColor);
                }});
        }
    }
    // METHOD - sets Text color for Button, TextView, EditText
    public void setTextColor(final int id, final int iColor ){
        final View view = getView(id);
        if(Looper.myLooper() == Looper.getMainLooper()) {  // if current thread is main thread
            ((TextView) view).setTextColor(iColor);
        } else {                                           // Update it on main thread
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ((TextView) view).setTextColor(iColor);
                }});
        }
    }
    // METHOD - returns text for Button, TextView, EditText
    public String getViewText(int id){
        View view = getView(id);
        return ((TextView) view).getText().toString();
    }
    // METHOD - shows progress bar
    public void showProgress(Context context){
        if(layoutProgress == null){
            layoutProgress = new RelativeLayout(context);
            progressBar = new ProgressBar(context);

            progressBar.setIndeterminate(true);
            RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT);
            layoutProgress.setLayoutParams(rlp);
            layoutProgress.addView(progressBar);
            rootView.addView(layoutProgress);
            layoutProgress.bringToFront();
            layoutProgress.setGravity(Gravity.CENTER_HORIZONTAL| Gravity.CENTER_VERTICAL);
        }
        layoutProgress.setVisibility(View.VISIBLE);
    }

    // METHOD - shows progress bar
    public void showProgress(Context context, String sMessage){
        if(progressDialog == null){
            progressDialog = new ProgressDialog(context);
        }
        progressDialog.setMessage(sMessage);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
    }

    // METHOD - Hides progress bar
    public void hideProgress(){
        if(layoutProgress != null){
            layoutProgress.setVisibility(View.GONE);
        }
        if(progressDialog != null){
            progressDialog.hide();
            progressDialog.dismiss();
        }
    }
    // METHOD - sets onClickListener
    public void setOnClickListener(int id, View.OnClickListener listener){
        getView(id).setOnClickListener(listener);
    }
    // METHOD - sets Tag for a arView
    public void setTag(int id, String tag){
        getView(id).setTag(tag);
    }
    // METHOD - returns Tag for a views
    public String getTag(int id){
        return (String)getView(id).getTag();
    }

    // METHOD - Returns view either from saved arView or by findViewById() method
    private View getView(int id){
        short index = (short)(id & 0xFF);
        if(arId[index] != id) {
            arId[index] = id;
            arView[index] = rootView.findViewById(id);
        }
        return arView[index];
    }

    // METHOD returns if keyboard is visible
    public  boolean isKeyboardVisible(){
        if(layoutKbDetect == null){ throw new NullPointerException(); }
        return bKeyboardVisible;
    }

    // METHOD - sets up a full screen view, change of the view size means change in keyboard state.
    public void setKeyboardListener(){
        if(layoutKbDetect == null){
            layoutKbDetect = new RelativeLayout(context);
            layoutKbDetect.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        layoutKbDetect.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                iKbCount++; // first two keyboard counts should be ignored
                bKeyboardVisible = !bKeyboardVisible;
            }
        });
        rootView.addView(layoutKbDetect);
    }

    // METHOD - runs code on main thread, use for updating UI from non-UI thread
    public static void runOnUI(final ThreadCode code){
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                code.execute();
            }
        });
    }
    // METHOD - executes delayed code on Main thread
    public static void runDelayedOnUI(long iTime, final ThreadCode code){
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                code.execute();
            }
        }, iTime);
    }

    // METHOD set keyboard state, as keyboard listener only detects change, so initial status must be set for accuracy
    public void setKeyboardState(boolean bVisible){
        bKeyboardVisible = bVisible;
    }
    public void showKeyboard(){
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(rootView, 0);
    }

    public void hideKeyboard(){
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
    }

    // METHOD release resources
    public void stop(){
        context = null;
        rootView = null;
        arId = null;
        arView = null;
        layoutProgress = null;
        progressBar = null;
        progressDialog = null;
        layoutKbDetect = null;
    }
}
