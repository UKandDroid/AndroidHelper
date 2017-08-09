package com.helper.lib;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

// Version 1.1.6
// added int method for setTag
// Added method to return string from resource, showToast
// Changed getTextString to getText, added setText(ID, integer)
// Added setTextColorRes, sets text color from resource

public class UiHelper {

    private Context context;
    private ViewGroup rootView;
    private int[] arId = new int[256];      // Store loaded arView arId, so they can checked against.
    private View[] arView = new View[256];  // keeps reference of loaded views, so they are not loaded again..
    private RelativeLayout layoutProgress = null;
    private ProgressBar progressBar;
    private static int iKbCount = 0;
    private ProgressDialog progressDialog;
    private boolean bKeyboardVisible = false;
    private RelativeLayout layoutKbDetect = null;
    private static final String LOG_TAG = "ViewHelper";

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
    public CheckBox checkBox(int id){ return (CheckBox)getView(id); }
    public EditText editText(int id){ return (EditText)getView(id); }
    public Button button(int id){ return (Button)getView(id);}

    // METHOD - sets Background for a arView
    public void setBackground(final int id, final int resource){
        if(Looper.myLooper() == Looper.getMainLooper()) {  // if current thread is main thread
            getView(id).setBackgroundResource(resource);
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    getView(id).setBackgroundResource(resource);
                }});
        }
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
    public TextView setText(final int id, final String sText){
        final View view = getView(id);
        if(Looper.myLooper() == Looper.getMainLooper()) {   // if current thread is main thread
            ((TextView) view).setText(sText);
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ((TextView) view).setText(sText);       // Update it on main thread
                }});
        }
        return (TextView)view;
    }

    // METHOD - sets text for Button, TextView, EditText
    public TextView setText(final int id, final int iText){
        final View view = getView(id);
        if(Looper.myLooper() == Looper.getMainLooper()) {   // if current thread is main thread
            ((TextView) view).setText(Integer.toString(iText));
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ((TextView) view).setText(Integer.toString(iText));       // Update it on main thread
                }});
        }
        return (TextView)view;
    }

    // METHOD - sets text for Button, TextView, EditText
    public TextView setHint(final int id, final String sText){
        final View view = getView(id);
        if(Looper.myLooper() == Looper.getMainLooper()) {  // if current thread is main thread
            ((TextView) view).setHint(sText);
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ((TextView) view).setHint(sText);// Update it on main thread
                }});
        }
        return (TextView)view;
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

    // METHOD - sets text  and Text color for Button, TextView, EditText
    public void setTextRes(final int id, final int iResString){
        final View view = getView(id);
        if(Looper.myLooper() == Looper.getMainLooper()) {  // if current thread is main thread
            ((TextView) view).setText(context.getResources().getString(iResString));
        } else {                                           // Update it on main thread
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ((TextView) view).setText(context.getResources().getString(iResString));
                }});
        }
    }

    // METHOD - returns text string from id
    public String getStringRes(final int iResString){
        return context.getResources().getString(iResString);
    }

    // METHOD - shows toast message
    public void showToast( String sMessage){
        Toast.makeText(context, sMessage, Toast.LENGTH_SHORT).show();
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

    // METHOD - sets Text color for Button, TextView, EditText from resource
    public void setTextColorRes(final int id, final int colorId ){
        final int iColor =  getColor(colorId);
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

    // METHOD - sets Text color for Button, TextView, EditText
    public void setTextBold(final int id, final boolean bTrue ){
        final View view = getView(id);
        if(Looper.myLooper() == Looper.getMainLooper()) {  // if current thread is main thread
            ((TextView) view).setTypeface(null, bTrue ? Typeface.BOLD : Typeface.NORMAL);

        } else {                                           // Update it on main thread
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ((TextView) view).setTypeface(null, bTrue ? Typeface.BOLD : Typeface.NORMAL);
                }});
        }
    }
    // METHOD - Checks/Un checks a box
    public void setChecked(final int id, boolean bCheck){
        CheckBox checkBox = (CheckBox) getView(id);
        checkBox.setChecked(bCheck);
    }

    // METHOD returns color from resource id
    public int getColor(int rID){
        return  context.getResources().getColor(rID);
    }

    // METHOD - returns text for Button, TextView, EditText
    public String getText(int id){
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
    public View.OnClickListener setOnClickListener(int id, View.OnClickListener listener){
        getView(id).setOnClickListener(listener);
        return  listener;
    }
    // METHOD - sets Tag for a arView
    public void setTag(int id, int tag){ setTag(id, ""+tag); }
    public void setTag(int id, String tag){
        getView(id).setTag(tag);
    }
    // METHOD - returns Tag for a views
    public String getTag(int id){
        return (String)getView(id).getTag();
    }

    // METHOD - Returns view either from saved arView or by findViewById() method
    public View getView(int id){
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
                if(++iKbCount>1)                                                                    // first two calls should be ignored
                    bKeyboardVisible = !bKeyboardVisible;
            }
        });
        rootView.addView(layoutKbDetect);
    }

    // METHOD - runs code on main thread, use for updating UI from non-UI thread
    public static void runOnUI(final Utils.ThreadCode code){
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() { code.execute(); }});
    }

    // METHOD - executes delayed code on Main thread
    public static void runDelayedOnUI(long iTime, final Utils.ThreadCode code){
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() { code.execute(); }}, iTime);
    }

    // METHOD set keyboard state, as keyboard listener only detects change, so initial status could be set if required
    public void setKeyboardState(boolean bVisible){ bKeyboardVisible = bVisible;}

    public void showKeyboard(){ showKeyboard(rootView); }
    public void showKeyboard(View v){
        if(rootView != null){
            InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(v, 0);
        } else {
            Log.e(LOG_TAG, "Show Keyboard ERROR, rootView/supplied view is null");
        }
    }

    public void hideKeyboard(){ hideKeyboard(rootView); }
    public void hideKeyboard(View viewWithFocus){                       // Give view that has focus, use  <requestFocus/> for a view to get focus
        if(viewWithFocus != null){
            InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(viewWithFocus.getWindowToken(), 0);
        } else {
            Log.e(LOG_TAG, "Hide keyboard ERROR, rootView/supplied view is null");
        }
    }

    // METHOD release resources
    public void release(){
        context = null;
        rootView = null;
        arId = null;
        arView = null;
        layoutProgress = null;
        progressBar = null;
        progressDialog = null;
        layoutKbDetect = null;
    }

    // METHOD - Convert pixels to dp
    public static int pxToDp(Context con, int iPixels){
        DisplayMetrics displayMetrics = con.getResources().getDisplayMetrics();
        int dp = Math.round(iPixels / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }

    // METHOD - Convert dp to pixels
    public static int dpToPx(Context con, int dp){
        Resources r =  con.getResources();
        DisplayMetrics displayMetrics = r.getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }
}