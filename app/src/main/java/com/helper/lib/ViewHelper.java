package blueband.com.Helper;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by Ubaid on 15/04/2016.
 */
public class ViewHelper {
    private ViewGroup rootView;
    private int[] arId = new int[256];   // Store loaded arView arId, so they can checked against.
    private View[] arView = new View[256];// keeps reference of loaded views, so they are not loaded again..
    private RelativeLayout layoutProgress = null;
    private ProgressBar progressBar;
    private ProgressDialog progressDialog;
    // CONSTRUCTORS
    public ViewHelper() {}
    public ViewHelper(View v){
        rootView = (ViewGroup)v;
    }
    public void setRootView(View v){
        rootView = (ViewGroup)v;
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
    public void visibilityShow(int id){
        getView(id).setVisibility(View.VISIBLE);
    }
    // METHOD - sets visibility of view to InVisible
    public void visibilityHide(int id){
        getView(id).setVisibility(View.INVISIBLE);
    }
    // METHOD - sets visibility of view to InVisible
    public void visibilityGone(int id){
        getView(id).setVisibility(View.GONE);
    }
    // METHOD - sets visibility
    public void setVisibility(int id, int visibility){
        getView(id).setVisibility(visibility);
    }
    // METHOD - sets text for Button, TextView, EditText
    public void setViewText(final int id, final String sText){
        if(Looper.myLooper() == Looper.getMainLooper()) {  // if current thread is main thread
            final View view = getView(id);
            ((TextView) view).setText(sText);
        } else {                                           // Update it on main thread
            Utils.updateUI(new Utils.ThreadCode() {
                @Override
                public void execute() {
                    final View view = getView(id);
                    ((TextView) view).setText(sText);
                }});
        }
    }
    // METHOD - returns text for Button, TextView, EditText
    public String getViewText(int id){
        View view = getView(id);
        if(view instanceof TextView) { // Button, EditText extends TextView
            return ((TextView) view).getText().toString();
        }
        return "Wrong view type";
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
        progressDialog = new ProgressDialog(context);
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

}
