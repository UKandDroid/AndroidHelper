package com.helper.lib;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

// Version 1.2.5
// Added support for chain calls e.g ui.setText(R.id.text, "test").setColor(iColor).setBg(R.drawable.bg)
// Changed old method calls
// Added set text for float & long
// added non static methods for pxToDp dpToPx
// Added Spinner methods - Set data, set selection
// Added method text size
// Added show Toast with string resource
// Added method SetEnabled
// Hide keyboard method, request focus
// added int method for setTag
// Added method to return string from resource, showToast
// Changed getTextString to getText, added setText(ID, integer)
// Added setTextColorRes, sets text color from resource

public class UiHelper {

    private View curView;               // View we are currently working with
    private Context context;
    private ViewGroup rootView;
    private Handler mainThread;
    private static int iKbCount = 0;
    private ProgressBar progressBar;
    private int[] arId = new int[256];      // Store loaded arView arId, so they can checked against.
    private View[] arView = new View[256];  // keeps reference of loaded views, so they are not loaded again..
    private ProgressDialog progressDialog;
    private boolean bKeyboardVisible = false;
    private RelativeLayout layoutProgress = null;
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
        mainThread = new Handler(Looper.getMainLooper());
    }

    public ViewGroup getRootView(){
        return rootView;
    }
    public TextView textView(int id){ return (TextView)getView(id); }
    public CheckBox checkBox(int id){ return (CheckBox)getView(id); }
    public EditText editText(int id){ return (EditText)getView(id); }
    public Button button(int id){ return (Button)getView(id);}
    public void setTag(int id, int tag){ setTag(id, ""+tag); }
    public void setTag(int id, String tag){
        getView(id).setTag(tag);
    }
    public String getTag(int id){
        return (String)getView(id).getTag();
    }
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
    public float getDimenRes(final int iRes){ return context.getResources().getDimension(iRes);}
    public void setClickable(int id, boolean bTrue){ getView(id).setClickable(bTrue); }
    public String getStringRes(final int iResString){ return context.getResources().getString(iResString); }
    public void showToast( String sMessage){ Toast.makeText(context, sMessage, Toast.LENGTH_SHORT).show();}
    public void showToast( int iResString){ Toast.makeText(context, getStringRes(iResString), Toast.LENGTH_SHORT).show();}

    public UiHelper setText(final long lText){ return setText(curView, Long.toString(lText)); }
    public UiHelper setText(final int id, final long lText){ return setText(getView(id), Long.toString(lText)); }
    public UiHelper setText(final float fText){ return setText(curView, Float.toString(fText)); }
    public UiHelper setText(final int id, final float fText){ return setText(getView(id), Float.toString(fText)); }
    public UiHelper setText(final int iText){ return setText(curView, Integer.toString(iText)); }
    public UiHelper setText(final int id, final int iText){ return setText(getView(id), Integer.toString(iText)); }
    public UiHelper setText( final String sText){return setText(curView, sText); }
    public UiHelper setText(final int id, final String sText){return setText(getView(id), sText); }
    private UiHelper setText(final View view, final String sText){
        if(isMainThread()) {
            ((TextView) view).setText(sText);
        } else {
            mainThread.post(new Runnable() {@Override public void run() { ((TextView) view).setText(sText); }});
        }
        return this;
    }

    // METHOD - sets Background for a View
    public void setBackground(final int iResId){setBackground(curView, iResId);}
    public void setBackground(final int id, final int iResId){setBackground(getView(id), iResId);}
    private void setBackground(final View view, final int iResId){
        if(isBgThread()) {  // if current thread is not main thread, call it on main thread
            mainThread.post(new Runnable() { @Override public void run() { setBackground(view, iResId); }});
            return;
        }

        // Padding is lost when new background is set, so we need to reapply it.
        int pL = view.getPaddingLeft();
        int pT = view.getPaddingTop();
        int pR = view.getPaddingRight();
        int pB = view.getPaddingBottom();

        view.setBackgroundResource(iResId);
        view.setPadding(pL, pT, pR, pB);
    }

    // METHOD - sets text for Button, TextView, EditText
    public TextView setHint(final int id, final String sText){
        final View view = getView(id);
        if(isMainThread()) {  // if current thread is main thread
            ((TextView) view).setHint(sText);
        } else {
            mainThread.post(new Runnable() {@Override public void run() {((TextView) view).setHint(sText);}});
        }
        return (TextView)view;
    }

    // METHOD - Enables or disables a view
    public UiHelper setEnabled( final boolean bEnable) { return setEnabled(curView, bEnable); }
    public UiHelper setEnabled(final int id, final boolean bEnable) { return setEnabled(getView(id), bEnable); }
    private UiHelper setEnabled(final View view, final boolean bEnable){
        if(isMainThread()) {                                // if current thread is main thread
            view.setEnabled(bEnable);
        } else {                                           // Update it on main thread
            mainThread.post(new Runnable() {
                @Override
                public void run() {
                    view.setEnabled(bEnable);
                }});
        }
        return this;
    }

    // METHOD - sets text  and Text color for Button, TextView, EditText
    public UiHelper setTextRes( final int iResString){return setTextRes(curView, iResString); }
    public UiHelper setTextRes(final int id, final int iResString){return setTextRes(getView(id), iResString); }
    private UiHelper setTextRes(final View view, final int iResString){
        if(isMainThread()) {  // if current thread is main thread
            ((TextView) view).setText(context.getResources().getString(iResString));
        } else {                                           // Update it on main thread
            mainThread.post(new Runnable() {
                @Override public void run() {
                    ((TextView) view).setText(context.getResources().getString(iResString));
                }});
        }
        return this;
    }

    // METHOD - sets Text color for Button, TextView, EditText
    public void setTextColor(final int iRGB ){setTextColor(curView, iRGB);}
    public void setTextColor(final int id, final int iRGB ){ setTextColor(getView(id), iRGB); }
    private void setTextColor(final View v, final int iRGB ){
        if(isBgThread()){
            mainThread.post(new Runnable() { @Override public void run() { setTextColor(v, iRGB); }});
            return;
        }
        ((TextView)v).setTextColor(iRGB);
    }

    // METHOD - sets Text color for Button, TextView, EditText from resource
    public UiHelper setTextColorRes(final int id, final int iColorRes ){return setTextColorRes(getView(id), iColorRes);}
    public UiHelper setTextColorRes( final int iColorRes ){return setTextColorRes(curView, iColorRes);}
    private UiHelper setTextColorRes(final View view, final int colorId ){
        if(isBgThread()){
            mainThread.post(new Runnable() { @Override public void run() { setTextColorRes( view,colorId ); }});
            return this;
        }
        final int iColor =  getColor(colorId);
        ((TextView) view).setTextColor(iColor);
        return this;
    }

    // METHOD - sets Text color Size Button, TextView, EditText, Note dimension resources are returned as pixels, that's why TypedValue.COMPLEX_UNIT_PX for resouce
    public void setTextSizeRes(final int id, final int iRes ){setTextSize(id, TypedValue.COMPLEX_UNIT_PX, getDimenRes(iRes));}
    public void setTextSizeRes(final int id, final int iUnit, final int iRes ){setTextSize(id, iUnit, getDimenRes(iRes));}
    public void setTextSize(final int id, final float iSize ){setTextSize(id, TypedValue.COMPLEX_UNIT_SP, iSize);}
    public void setTextSize(final int id, final int iUnit, final float iSize ){
        final View view = getView(id);
        if(isMainThread()) {  // if current thread is main thread
            ((TextView) view).setTextSize(iUnit, iSize);
        } else {                                           // Update it on main thread
            runOnUI(new Utils.ThreadCode() {
                @Override
                public void execute() {

                }
            });
            mainThread.post(new Runnable() {
                @Override
                public void run() {
                    ((TextView) view).setTextSize(iUnit, iSize);
                }});
        }
    }

    // METHOD - sets Text color for Button, TextView, EditText
    public void setSpinSelection(final int iId, final int iSel){
        if(isBgThread()){
            mainThread.post(new Runnable() { @Override public void run() { ((Spinner)getView(iId)).setSelection(iSel); }});
            return;
        }
        ((Spinner)getView(iId)).setSelection(iSel);
    }

    public ArrayAdapter<String> setSpinData(final int iId, final int iArrId) { return setSpinData(iId, context.getResources().getStringArray(iArrId), android.R.layout.simple_list_item_single_choice);}
    public ArrayAdapter<String> setSpinData(final int iId, final int iArrId, int iLayout){ return setSpinData(iId, context.getResources().getStringArray(iArrId), iLayout); }
    public ArrayAdapter<String> setSpinData(final int iId, final String arr[], final int iLayout){
        Spinner spin = (Spinner)getView(iId);
        ArrayAdapter<String> adaptSpin = new ArrayAdapter<String>(context, iLayout, arr );
        spin.setAdapter(adaptSpin);
        return adaptSpin;
    }

    // METHOD - sets Text color for Button, TextView, EditText
    public void setTextBold(final int id, final boolean bTrue ){
        final View view = getView(id);
        if(isMainThread()) {  // if current thread is main thread
            ((TextView) view).setTypeface(null, bTrue ? Typeface.BOLD : Typeface.NORMAL);
        } else {                                           // Update it on main thread
            mainThread.post(new Runnable() {
                @Override
                public void run() {
                    ((TextView) view).setTypeface(null, bTrue ? Typeface.BOLD : Typeface.NORMAL);
                }});
        }
    }
    // METHOD - Checks/Un-checks a check box
    public void setChecked(final int id, boolean bCheck){
        CheckBox checkBox = (CheckBox) getView(id);
        checkBox.setChecked(bCheck);
    }

    // METHOD returns color from resource id
    public int getColor(int iRes){
        return  context.getResources().getColor(iRes);
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

    // METHOD - Returns view either from saved arView or by findViewById() method
    public View getView(int id){
        short index = (short)(id & 0xFF);
        if(arId[index] != id) {
            arId[index] = id;
            arView[index] = rootView.findViewById(id);
        }
        return curView = arView[index];
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
            @Override public void run() { code.execute(); }}, iTime);
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
    public void hideKeyboard(View v){

        if(v != null){
            v.requestFocus();
            InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        } else {
            Log.e(LOG_TAG, "Hide keyboard ERROR, rootView/supplied view is null");
        }
    }

    // METHOD release resources
    public void release(){
        mainThread.removeCallbacksAndMessages(null);
        curView = null;
        context = null;
        rootView = null;
        arId = null;
        arView = null;
        layoutProgress = null;
        progressBar = null;
        progressDialog = null;
        layoutKbDetect = null;
        mainThread = null;
    }

    // METHOD - Convert pixels to dp
    public static int pxToDp(Context con, int iPixels){
        DisplayMetrics displayMetrics = con.getResources().getDisplayMetrics();
        int dp = Math.round(iPixels / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }

    // METHOD - Convert pixels to dp
    public int pxToDp( int iPixels){
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int dp = Math.round(iPixels / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }

    // METHOD - Convert dp to pixels
    public int dpToPx( int dp){
        Resources r =  context.getResources();
        DisplayMetrics displayMetrics = r.getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }
    // METHOD - Convert dp to pixels
    public static int dpToPx(Context con, int dp){
        Resources r =  con.getResources();
        DisplayMetrics displayMetrics = r.getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    public boolean isMainThread(){
        return Looper.myLooper() == Looper.getMainLooper();
    }
    public boolean isBgThread(){
        return Looper.myLooper() != Looper.getMainLooper();
    }

    public boolean ifBgThread(Runnable r){
        if(Looper.myLooper() != Looper.getMainLooper()){
            mainThread.post(r);
            return true;
        }
        return false ;
    }
    public int getIntRes(int iResId){
        return context.getResources().getInteger(iResId);
    }
}
