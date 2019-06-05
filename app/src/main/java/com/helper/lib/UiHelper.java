package com.helper.lib;

import android.app.ProgressDialog;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
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

// Whats new Version 1.3.0
// Get view with tag
// added method getWidth, getHeight
// attempt to fix bug where UiHelper won't work if root object is changed
// fix text color resource bug
// Removed KB detect as it's implemented in flow
// Added support for chain calls e.g ui.setText(R.id.text, "test").setColor(iColor).setBg(R.drawable.bg)

// Helper class for working with android Views
public class UiHelper {
    private int[] arId;         // Store loaded arView arId, so they can checked against.
    private View[] arView;      // keeps reference of loaded views, so they are not loaded again..
    public View curView;
    private Context context;
    private ViewGroup rootView;
    private static Handler mainThread;
    private ProgressBar progressBar;
    private ProgressDialog progressDialog;
    private RelativeLayout layoutProgress = null;
    private static final String LOG_TAG = "UiHelper";

    // CONSTRUCTORS
    public UiHelper() {}
    public UiHelper(View v){
        setRootView(v);
    }
    public void setRootView(View v){
        rootView = (ViewGroup)v;
        arId = new int[256];      // Store loaded arView arId, so they can checked against.
        arView = new View[256];  // keeps reference of loaded views, so they are not loaded again..
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

    public void setTag(int id, int tag){ setTag(id, Integer.toString(tag)); }
    public void setTag(int id, String tag){
        getView(id).setTag(tag);
    }
    public String getTag(int id){
        return (String)getView(id).getTag();
    }

    //  Methods to set visibility
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

    public String getText(int id){ return ((TextView) getView(id)).getText().toString();}
    public UiHelper setText(final long lText){ return setText(curView, Long.toString(lText)); }
    public UiHelper setText(final int id, final long lText){ return setText(getView(id), Long.toString(lText)); }
    public UiHelper setText(final float fText){ return setText(curView, Float.toString(fText)); }
    public UiHelper setText(final int id, final float fText){ return setText(getView(id), Float.toString(fText)); }
    public UiHelper setText(final int iText){ return setText(curView, Integer.toString(iText)); }
    public UiHelper setText(final int id, final int iText){ return setText(getView(id), Integer.toString(iText)); }
    public UiHelper setText( final String sText){return setText(curView, sText); }
    public UiHelper setText(final int id, final String sText){return setText(getView(id), sText); }
    public UiHelper setTextRes( final int iResString){return setTextRes(curView, iResString); }
    public UiHelper setTextRes(final int id, final int iResString){return setTextRes(getView(id), iResString); }
    private UiHelper setText(final View view, final String sText){ ((TextView) view).setText(sText);    return this;}
    private UiHelper setTextRes(final View view, final int iResString){ ((TextView) view).setText(context.getResources().getString(iResString)); return this; }


    private UiHelper setGravity(final int id, final int iGravity){ setGravity(getView(id), iGravity); return this;}
    private UiHelper setGravity(final View view, final int iGravity){ ((TextView)view).setGravity(iGravity); return this;}
    public UiHelper setGravity( final int iGravity){ setGravity(curView, iGravity); return this; }


    // METHOD - sets Background for a View
    public UiHelper setBackground(final int iResId){setBackground(curView, iResId); return  this; }
    public UiHelper setBackground(final int id, final int iResId){setBackground(getView(id), iResId); return  this;}
    private UiHelper setBackground(final View view, final int iResId){
        // Padding is lost when new background is set, so we need to reapply it.
        int pL = view.getPaddingLeft();
        int pT = view.getPaddingTop();
        int pR = view.getPaddingRight();
        int pB = view.getPaddingBottom();

        view.setBackgroundResource(iResId);
        view.setPadding(pL, pT, pR, pB);
        return this;
    }

    // METHOD - sets text for Button, TextView, EditText
    public UiHelper setHint(final int id, final String sText){
        ((TextView)  getView(id)).setHint(sText);
        return this;
    }

    // METHOD - Enables or disables a view
    public UiHelper setEnabled( final boolean bEnable) { return setEnabled(curView, bEnable); }
    public UiHelper setEnabled(final int id, final boolean bEnable) { return setEnabled(getView(id), bEnable); }
    private UiHelper setEnabled(final View view, final boolean bEnable){
        view.setEnabled(bEnable);
        return this;
    }

    // METHOD - sets Text color for Button, TextView, EditText
    public UiHelper setTextColorRes(final int id, final int iColorRes ){return  setTextColor(getView(id), getColorRes(iColorRes));}
    public UiHelper setTextColorRes( final int iColorRes ){ return setTextColor(curView, getColorRes(iColorRes));}
    public UiHelper setTextColor(final int iRGB ){ return setTextColor(curView, iRGB);}
    public UiHelper setTextColor(final int id, final int iRGB ){ return setTextColor(getView(id), iRGB); }
    private UiHelper setTextColor(final View v, final int iRGB ){ ((TextView)v).setTextColor(iRGB); return this; }

    // METHOD get/set width, height
    public int getWidth(final int iResId){ return getView(iResId).getWidth(); }
    public int getHeight(final int iResId){ return getView(iResId).getHeight();  }
    public int getWidth(){return curView.getWidth(); }
    public int getHeight( ){ return curView.getHeight();}


    // METHOD - sets Text color Size Button, TextView, EditText, Note dimension resources are returned as pixels, that's why TypedValue.COMPLEX_UNIT_PX for resouce
    public UiHelper setTextSizeRes(final int id, final int iRes ){ return setTextSize(id, TypedValue.COMPLEX_UNIT_PX, getDimenRes(iRes));}
    public UiHelper setTextSizeRes(final int id, final int iUnit, final int iRes ){ return setTextSize(id, iUnit, getDimenRes(iRes));}
    public UiHelper setTextSize(final int id, final float iSize ){ return setTextSize(id, TypedValue.COMPLEX_UNIT_SP, iSize);}
    public UiHelper setTextSize(final int id, final int iUnit, final float iSize ){ ((TextView) getView(id)).setTextSize(iUnit, iSize); return this; }

    // METHOD - sets Text color for Button, TextView, EditText
    public void setSpinSelection(final int iId, final int iSel){
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
    public UiHelper setTextBold(final int id, final boolean bTrue ){
        ((TextView) getView(id)).setTypeface(null, bTrue ? Typeface.BOLD : Typeface.NORMAL);
        return this;
    }

    // METHOD - Checks/Un-checks a check box
    public UiHelper setChecked(final int id, boolean bCheck){
        CheckBox checkBox = (CheckBox) getView(id);
        checkBox.setChecked(bCheck);
        return this;
    }

    // METHOD returns color from resource id
    public int getColorRes(int iRes){
        return  context.getResources().getColor(iRes);
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

    public View getViewWithTag(Object tag){
        View v = rootView.findViewWithTag(tag);
        if(v!= null){
            short index = (short)(v.getId() & 0xFF);
            arView[index] = v;
        }
        return curView = v;
    }

    public void showKeyboard(){ showKeyboard(rootView); }
    public void showKeyboard(View v){
        if(v != null){
            v.requestFocus();
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

    // METHOD - sets Text color for Button, TextView, EditText
    public void setTypeface(final int id, final Typeface font ){
        final View view = getView(id);
        if(Looper.myLooper() == Looper.getMainLooper()) {  // if current thread is main thread
            ((TextView) view).setTypeface(font, Typeface.NORMAL);

        } else {                                           // Update it on main thread
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ((TextView) view).setTypeface(font, Typeface.NORMAL);
                }});
        }
    }

    // METHOD - executes delayed code on Main thread
    public static void runDelayedOnUI(long iTime, final Flow.Run code){
        mainThread.postDelayed(new Runnable() {
            @Override
            public void run() { code.onAction(); }}, iTime);
    }


    // METHOD - runs code on main thread, use for updating UI from non-UI thread
    public static void runOnUI( final Flow.Run code ){
        mainThread.post(new Runnable() {
            @Override
            public void run() { code.onAction(); }});
    }

}
