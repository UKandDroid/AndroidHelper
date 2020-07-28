package com.helper.lib

import android.R
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*

// Whats new Version 1.4.0
// converted to kotlin
// Get view with tag
// added method getWidth, getHeight
// attempt to fix bug where UiHelper won't work if root object is changed
// fix text color resource bug
// Removed KB detect as it's implemented in flow
// Added support for chain calls e.g ui.setText(R.id.text, "test").setColor(iColor).setBg(R.drawable.bg)
// Helper class for working with android Views
class UiHelper {
    var curView: View? = null
    private var arId: IntArray? = null // Store loaded arView arId, so they can checked against.
    private var arView: Array<View?>? = null // keeps reference of loaded views, so they are not loaded again..     
    private var context: Context? = null
    private var uiView: ViewGroup? = null
    private var progressBar: ProgressBar? = null
    private var progressDialog: ProgressDialog? = null
    private var layoutProgress: RelativeLayout? = null

    // CONSTRUCTORS
    constructor() {}

    constructor(v: View?) {
        setRootView(v)
    }

    fun setRootView(v: View?) {
        uiView = v as ViewGroup?
        arId = IntArray(256) // Store loaded arView arId, so they can checked against.
        arView = arrayOfNulls(256) // keeps reference of loaded views, so they are not loaded again..
        context = uiView!!.context
        mainThread = Handler(Looper.getMainLooper())
    }

    fun getRootView(): ViewGroup? {
        return uiView
    }

    fun textView(id: Int): TextView {
        return getView(id) as TextView
    }

    fun checkBox(id: Int): CheckBox {
        return getView(id) as CheckBox
    }

    fun editText(id: Int): EditText {
        return getView(id) as EditText
    }

    fun button(id: Int): Button {
        return getView(id) as Button
    }

    fun setTag(id: Int, tag: Int) {
        setTag(id, Integer.toString(tag))
    }

    fun setTag(id: Int, tag: String?) {
        getView(id).tag = tag
    }

    fun getTag(id: Int): String {
        return getView(id).tag as String
    }

    //  Methods to set visibility
    fun setVisible(id: Int) {
        getView(id).visibility = View.VISIBLE
    }

    fun setInvisible(id: Int) {
        getView(id).visibility = View.INVISIBLE
    }

    fun setGone(id: Int) {
        getView(id).visibility = View.GONE
    }

    fun setVisibility(id: Int, visibility: Int) {
        getView(id).visibility = visibility
    }

    fun getDimenRes(iRes: Int): Float {
        return context!!.resources.getDimension(iRes)
    }

    fun setClickable(id: Int, bTrue: Boolean) {
        getView(id).isClickable = bTrue
    }

    fun getStringRes(iResString: Int): String {
        return context!!.resources.getString(iResString)
    }

    fun showToast(sMessage: String?) {
        Toast.makeText(context, sMessage, Toast.LENGTH_SHORT).show()
    }

    fun showToast(iResString: Int) {
        Toast.makeText(context, getStringRes(iResString), Toast.LENGTH_SHORT).show()
    }

    fun getText(id: Int): String {
        return (getView(id) as TextView).text.toString()
    }

    fun setText(lText: Long): UiHelper {
        return setText(curView, java.lang.Long.toString(lText))
    }

    fun setText(id: Int, lText: Long): UiHelper {
        return setText(getView(id), java.lang.Long.toString(lText))
    }

    fun setText(fText: Float): UiHelper {
        return setText(curView, java.lang.Float.toString(fText))
    }

    fun setText(id: Int, fText: Float): UiHelper {
        return setText(getView(id), java.lang.Float.toString(fText))
    }

    fun setText(iText: Int): UiHelper {
        return setText(curView, Integer.toString(iText))
    }

    fun setText(id: Int, iText: Int): UiHelper {
        return setText(getView(id), Integer.toString(iText))
    }

    fun setText(sText: String): UiHelper {
        return setText(curView, sText)
    }

    fun setText(id: Int, sText: String): UiHelper {
        return setText(getView(id), sText)
    }

    fun setTextRes(iResString: Int): UiHelper {
        return setTextRes(curView, iResString)
    }

    fun setTextRes(id: Int, iResString: Int): UiHelper {
        return setTextRes(getView(id), iResString)
    }

    private fun setText(view: View?, sText: String): UiHelper {
        (view as TextView?)!!.text = sText
        return this
    }

    private fun setTextRes(view: View?, iResString: Int): UiHelper {
        (view as TextView?)!!.text = context!!.resources.getString(iResString)
        return this
    }

    private fun setGravity(id: Int, iGravity: Int): UiHelper {
        setGravity(getView(id), iGravity)
        return this
    }

    private fun setGravity(view: View?, iGravity: Int): UiHelper {
        (view as TextView?)!!.gravity = iGravity
        return this
    }

    fun setGravity(iGravity: Int): UiHelper {
        setGravity(curView, iGravity)
        return this
    }

    // METHOD - sets Background for a View
    fun setBackground(iResId: Int): UiHelper {
        setBackground(curView!!, iResId)
        return this
    }

    fun setBackground(id: Int, iResId: Int): UiHelper {
        setBackground(getView(id), iResId)
        return this
    }

    private fun setBackground(view: View, iResId: Int): UiHelper { // Padding is lost when new background is set, so we need to reapply it.
        val pL = view.paddingLeft
        val pT = view.paddingTop
        val pR = view.paddingRight
        val pB = view.paddingBottom
        view.setBackgroundResource(iResId)
        view.setPadding(pL, pT, pR, pB)
        return this
    }

    // METHOD - sets text for Button, TextView, EditText
    fun setHint(id: Int, sText: String?): UiHelper {
        (getView(id) as TextView).hint = sText
        return this
    }

    // METHOD - Enables or disables a view
    fun setEnabled(bEnable: Boolean): UiHelper {
        return setEnabled(curView, bEnable)
    }

    fun setEnabled(id: Int, bEnable: Boolean): UiHelper {
        return setEnabled(getView(id), bEnable)
    }

    private fun setEnabled(view: View?, bEnable: Boolean): UiHelper {
        view!!.isEnabled = bEnable
        return this
    }

    // METHOD - sets Text color for Button, TextView, EditText
    fun setTextColorRes(id: Int, iColorRes: Int): UiHelper {
        return setTextColor(getView(id), getColorRes(iColorRes))
    }

    fun setTextColorRes(iColorRes: Int): UiHelper {
        return setTextColor(curView, getColorRes(iColorRes))
    }

    fun setTextColor(iRGB: Int): UiHelper {
        return setTextColor(curView, iRGB)
    }

    fun setTextColor(id: Int, iRGB: Int): UiHelper {
        return setTextColor(getView(id), iRGB)
    }

    private fun setTextColor(v: View?, iRGB: Int): UiHelper {
        (v as TextView?)!!.setTextColor(iRGB)
        return this
    }

    // METHOD get/set width, height
    fun getWidth(iResId: Int): Int {
        return getView(iResId).width
    }

    fun getHeight(iResId: Int): Int {
        return getView(iResId).height
    }

    val width: Int
        get() = curView!!.width

    val height: Int
        get() = curView!!.height

    // METHOD - sets Text color Size Button, TextView, EditText, Note dimension resources are returned as pixels, that's why TypedValue.COMPLEX_UNIT_PX for resouce
    fun setTextSizeRes(id: Int, iRes: Int): UiHelper {
        return setTextSize(id, TypedValue.COMPLEX_UNIT_PX, getDimenRes(iRes))
    }

    fun setTextSizeRes(id: Int, iUnit: Int, iRes: Int): UiHelper {
        return setTextSize(id, iUnit, getDimenRes(iRes))
    }

    fun setTextSize(id: Int, iSize: Float): UiHelper {
        return setTextSize(id, TypedValue.COMPLEX_UNIT_SP, iSize)
    }

    fun setTextSize(id: Int, iUnit: Int, iSize: Float): UiHelper {
        (getView(id) as TextView).setTextSize(iUnit, iSize)
        return this
    }

    // METHOD - sets Text color for Button, TextView, EditText
    fun setSpinSelection(iId: Int, iSel: Int) {
        (getView(iId) as Spinner).setSelection(iSel)
    }

    fun setSpinData(iId: Int, iArrId: Int): ArrayAdapter<String> {
        return setSpinData(iId, context!!.resources.getStringArray(iArrId), R.layout.simple_list_item_single_choice)
    }

    fun setSpinData(iId: Int, iArrId: Int, iLayout: Int): ArrayAdapter<String> {
        return setSpinData(iId, context!!.resources.getStringArray(iArrId), iLayout)
    }

    fun setSpinData(iId: Int, arr: Array<String>?, iLayout: Int): ArrayAdapter<String> {
        val spin = getView(iId) as Spinner
        val adaptSpin = ArrayAdapter(context, iLayout, arr)
        spin.adapter = adaptSpin
        return adaptSpin
    }

    // METHOD - sets Text color for Button, TextView, EditText
    fun setTextBold(id: Int, bTrue: Boolean): UiHelper {
        (getView(id) as TextView).setTypeface(null, if (bTrue) Typeface.BOLD else Typeface.NORMAL)
        return this
    }

    // METHOD - Checks/Un-checks a check box, toggle and switch
    fun setToggle(id: Int, bCheck: Boolean): UiHelper? { return setState(id, bCheck) }
    fun setSwitch(id: Int, bCheck: Boolean): UiHelper? { return setState(id, bCheck) }
    fun setState(id: Int, bCheck: Boolean): UiHelper? {
        val checkBox = getView(id) as CompoundButton
        checkBox.isChecked = bCheck
        return this
    }

    // METHOD returns color from resource id
    fun getColorRes(iRes: Int): Int {
        return context!!.resources.getColor(iRes)
    }

    // METHOD - shows progress bar
    fun showProgress(context: Context?) {
        if (layoutProgress == null) {
            layoutProgress = RelativeLayout(context)
            progressBar = ProgressBar(context)
            progressBar!!.isIndeterminate = true
            val rlp = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT)
            layoutProgress!!.layoutParams = rlp
            layoutProgress!!.addView(progressBar)
            uiView!!.addView(layoutProgress)
            layoutProgress!!.bringToFront()
            layoutProgress!!.gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
        }
        layoutProgress!!.visibility = View.VISIBLE
    }

    // METHOD - shows progress bar
    fun showProgress(context: Context?, sMessage: String?) {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(context)
        }
        progressDialog!!.setMessage(sMessage)
        progressDialog!!.isIndeterminate = true
        progressDialog!!.show()
    }

    // METHOD - Hides progress bar
    fun hideProgress() {
        if (layoutProgress != null) {
            layoutProgress!!.visibility = View.GONE
        }
        if (progressDialog != null) {
            progressDialog!!.hide()
            progressDialog!!.dismiss()
        }
    }

    // METHOD - sets onClickListener
    fun setOnClickListener(id: Int, listener: View.OnClickListener): View.OnClickListener {
        getView(id).setOnClickListener(listener)
        return listener
    }

    // METHOD - Returns view either from saved arView or by findViewById() method
    fun getView(id: Int): View {
        val index = (id and 0xFF).toInt()
        if (arId!![index] != id) {
            arId!![index] = id
            arView!![index] = uiView!!.findViewById(id)
        }
        curView = arView!![index]
        return curView!!
    }

    fun getViewWithTag(tag: Any?): View {
        val v = uiView!!.findViewWithTag<View>(tag)
        if (v != null) {
            @SuppressLint("ResourceType") val index = (v.id and 0xFF).toInt()
            arView!![index] = v
        }
        return v.also { curView = it }
    }

    @JvmOverloads
    fun showKeyboard(v: View? = uiView) {
        if (v != null) {
            v.requestFocus()
            val imm = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(v, 0)
        } else {
            Log.e(LOG_TAG, "Show Keyboard ERROR, rootView/supplied view is null")
        }
    }

    @JvmOverloads
    fun hideKeyboard(v: View? = uiView) {
        if (v != null) {
            v.requestFocus()
            val imm = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        } else {
            Log.e(LOG_TAG, "Hide keyboard ERROR, rootView/supplied view is null")
        }
    }

    // METHOD release resources
    fun release() {
        mainThread!!.removeCallbacksAndMessages(null)
        curView = null
        context = null
        uiView = null
        arId = null
        arView = null
        layoutProgress = null
        progressBar = null
        progressDialog = null
        mainThread = null
    }

    // METHOD - Convert pixels to dp
    fun pxToDp(iPixels: Int): Int {
        val displayMetrics = context!!.resources.displayMetrics
        return Math.round(iPixels / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
    }

    // METHOD - Convert dp to pixels
    fun dpToPx(dp: Int): Int {
        val r = context!!.resources
        val displayMetrics = r.displayMetrics
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
    }

    val isMainThread: Boolean
        get() = Looper.myLooper() == Looper.getMainLooper()

    val isBgThread: Boolean
        get() = Looper.myLooper() != Looper.getMainLooper()

    fun ifBgThread(r: Runnable?): Boolean {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainThread!!.post(r)
            return true
        }
        return false
    }

    fun getIntRes(iResId: Int): Int {
        return context!!.resources.getInteger(iResId)
    }

    // METHOD - sets Text color for Button, TextView, EditText
    fun setTypeface(id: Int, font: Typeface?) {
        val view = getView(id)
        if (Looper.myLooper() == Looper.getMainLooper()) { // if current thread is main thread
            (view as TextView).setTypeface(font, Typeface.NORMAL)
        } else { // Update it on main thread
            Handler(Looper.getMainLooper()).post { (view as TextView).setTypeface(font, Typeface.NORMAL) }
        }
    }

    companion object {
        private var mainThread: Handler? = null
        private const val LOG_TAG = "UiHelper"
        // METHOD - Convert pixels to dp
        fun pxToDp(con: Context, iPixels: Int): Int {
            val displayMetrics = con.resources.displayMetrics
            return Math.round(iPixels / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
        }
    }
}