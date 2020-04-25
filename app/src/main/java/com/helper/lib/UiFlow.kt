package com.helper.lib

import android.app.Activity
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.graphics.Rect
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import java.util.*

typealias UiCallback = (bSuccess: Boolean) -> Unit

class UiFlow(codeCallback: Code) :LifecycleObserver {
        private var code:Code ? = codeCallback
        private var viewActRoot:View? =null
        private var iSoftInputMode = -1
        private var rLast = Rect()
        private var bPause = false
        private var bKeybVisible = false
        private var keyList = ArrayList<KeyboardState>()
        private var flowListeners = ArrayList<UiFlowListener>()
        private var listTextListeners = hashMapOf<Any, Any>() // list of text change listeners for a text field
        private var listKBListeners = hashMapOf<Any, Any>() // list of keyboard state change listeners

        private interface KeyboardState {
                fun onStateChange(bVisible:Boolean)
        }

        enum class UiEvent { // EVENTS for which listeners are set
                ON_TOUCH,
                ON_CLICK,
                ON_TOGGLE,              // Compound button
                ON_SWITCH,              // Compound button
                ON_CHECKBOX,            // Compound button
                LOAD_LAYOUT,            // Called when a view width and height are set
                TEXT_CHANGED,
                TEXT_ENTERED,
                LIST_ITEM_SELECT,
                SPINNER_ITEM_SELECT,
                KEYBOARD_STATE_CHANGE // Works only for android:windowSoftInputMode="adjustResize" or "adjustPan"
        }

        interface Code {
                fun onAction(action:Int, bSuccess:Boolean, iExtra:Int, tag:Any)
        }

        private abstract inner class UiFlowListener internal
        constructor(protected val view: View, protected  val iAction:Int, protected val localCallback : UiCallback? = null) {
                internal abstract fun register():UiFlowListener
                internal abstract fun unRegister()
                protected abstract fun onEvent(iAction: Int, bSuccess: Boolean, iExtra: Int, data: Any? )
        }

        fun registerClick(iAction:Int, view:View, localCallback : UiCallback? = null) {
                registerListener( iAction, view, UiEvent.ON_CLICK, localCallback)
        }

        @JvmOverloads
        fun registerUiEvent(iAction:Int,  view:View, iEvent:UiEvent, localCallback : UiCallback? = null):UiFlow {
                registerListener( iAction, view, iEvent, localCallback)
                return this
        }

        // VIEW LISTENERS set event listeners for View objects
        private fun registerListener( iAction:Int, view:View, iListener:UiEvent, localCallback : UiCallback? = null) {
                when (iListener) {
                        // Triggered when Text entered in text field, i.e when text field loses focus, enter button is pressed on keyboard
                        // for text entered to work with keyboard hide, set android:windowSoftInputMode="adjustResize" or "adjustPan"
                        // and setup KEYBOARD_STATE UiEvent, provided with main activity root decor view
                        UiEvent.TEXT_ENTERED -> flowListeners.add(TextEntered(view, iAction, localCallback).register()) // Triggered when text changes

                        UiEvent.TEXT_CHANGED -> flowListeners.add(TextChanged(view, iAction, localCallback).register())

                        // Triggered when ui layout changes with width/height values > 0 and called only once
                        UiEvent.LOAD_LAYOUT -> flowListeners.add(LoadLayout(view, iAction, localCallback).register())

                        UiEvent.ON_CLICK -> flowListeners.add(OnClick(view, iAction, localCallback).register())// triggered listener when view is clicked

                        // Method reports keyboard state change, should be provided with view, uses view.getRootView() to get parent view
                        UiEvent.KEYBOARD_STATE_CHANGE -> flowListeners.add(KeyboardStateChange(view, iAction, localCallback).register())

                        UiEvent.LIST_ITEM_SELECT -> flowListeners.add(ListItemSelect(view, iAction, localCallback).register())

                        UiEvent.SPINNER_ITEM_SELECT -> flowListeners.add(SpinnerItemSelect(view, iAction, localCallback).register())

                        UiEvent.ON_CHECKBOX -> flowListeners.add(CompoundButton(view, iAction, localCallback).register())

                        UiEvent.ON_TOGGLE -> flowListeners.add(CompoundButton(view, iAction, localCallback).register())

                        UiEvent.ON_SWITCH -> flowListeners.add(CompoundButton(view, iAction, localCallback).register())

                        // Listener returns true for Touch down and Move, false when finger is lifted up
                        UiEvent.ON_TOUCH  -> flowListeners.add(TouchListener(view, iAction, localCallback).register())
                }
        }

        // VIEW LISTENERS set event listeners for View objects
        private fun unRegisterListener(view:View, iListener:UiEvent) {
                when (iListener) {
                        UiEvent.ON_CLICK -> {
                                if (view is EditText) {
                                        view.setOnFocusChangeListener(null)
                                }
                                view.setOnClickListener(null)
                        }
                        UiEvent.TEXT_ENTERED -> {
                                listKBListeners.remove(view)
                                view.setOnFocusChangeListener(null)
                                (view as EditText).setOnEditorActionListener(null)
                        }
                        // case UiEvent.KEYBOARD_STATE_CHANGE: removeKeybListener(); break;
                        UiEvent.TEXT_CHANGED -> listTextListeners.remove(view)
                        UiEvent.LIST_ITEM_SELECT -> (view as ListView).setOnItemClickListener(null)
                        UiEvent.SPINNER_ITEM_SELECT -> (view as Spinner).setOnItemSelectedListener(null)
                        UiEvent.ON_CHECKBOX -> (view as CheckBox).setOnCheckedChangeListener(null)
                        UiEvent.ON_TOUCH -> view.setOnTouchListener(null)
                        UiEvent.LOAD_LAYOUT -> view.removeOnLayoutChangeListener(null)
                }
        }
        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun pause() {
                bPause = true
        }
        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun resume() {
                bPause = false
        }
        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun stop() {
                for (listener in flowListeners) {
                        listener.unRegister()
                }
                flowListeners = arrayListOf()
        }


        // LIST_ITEM_SELECT
        private inner class SpinnerItemSelect internal constructor(view: View, iAction: Int, localCallback: UiCallback? = null) : UiFlowListener(view, iAction, localCallback) {
                override fun register():UiFlowListener {
                        (view as Spinner).onItemSelectedListener = object:AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(parent:AdapterView<*>, view:View, position:Int, id:Long) {
                                        onEvent(iAction,true, position, view)
                                }
                                override fun onNothingSelected(parent:AdapterView<*>) {
                                        onEvent(iAction,false, -1, view)
                                }
                        }
                        return this
                }
                override fun unRegister() {
                         (view as Spinner).onItemClickListener = null
                }

                override fun onEvent(iAction: Int, bSuccess: Boolean, iExtra: Int, data: Any?) {
                        if (localCallback != null) {
                                localCallback.invoke(bSuccess)
                        } else {
                                code?.onAction(iAction, bSuccess, iExtra, data!!)
                        }
                }
        }

        // TOUCH
        private inner class TouchListener internal constructor(view:View, iAction:Int, localCallback: UiCallback? = null) : UiFlowListener(view, iAction, localCallback) {
                 override fun register():UiFlowListener {
                        view.setOnTouchListener { v, event ->
                                onEvent(iAction,event.action != MotionEvent.ACTION_UP, event.action, event)
                                false
                        }
                        return this
                }
                 override fun unRegister() {
                        view.setOnTouchListener(null)
                }

                override fun onEvent(iAction: Int, bSuccess: Boolean, iExtra: Int, data: Any?) {
                        if (localCallback != null) {
                                localCallback.invoke(bSuccess)
                        } else {
                                code?.onAction(iAction, bSuccess, iExtra, data!!)
                        }
                }
        }

        // CHECK_BOX_STATE
        private inner class CompoundButton internal constructor(view:View, iAction:Int, localCallback: UiCallback? = null) : UiFlowListener(view, iAction, localCallback) {
                 override fun register():UiFlowListener {
                        (view as android.widget.CompoundButton).setOnCheckedChangeListener { buttonView, isChecked->
                               onEvent(iAction, isChecked, 0, view)
                         }
                        return this
                }
                 override fun unRegister() {
                        (view as CheckBox).setOnCheckedChangeListener(null)
                }
                override fun onEvent(iAction: Int, bSuccess: Boolean, iExtra: Int, data: Any?) {
                        if (localCallback != null) {
                                localCallback.invoke(bSuccess)
                        } else {
                                code?.onAction(iAction, bSuccess, iExtra, data!!)
                        }
                }
        }

        // LIST_ITEM_SELECT
        private inner class ListItemSelect internal constructor(view:View, iAction:Int, localCallback: UiCallback? = null) : UiFlowListener(view, iAction, localCallback) {
                 override fun register():UiFlowListener {
                        (view as ListView).setOnItemClickListener { parent, view12, position, id->
                                onEvent(iAction, true, position, view12)
                         }
                         return this
                }

                override fun unRegister() {
                        (view as ListView).onItemClickListener = null
                }
                override fun onEvent(iAction: Int, bSuccess: Boolean, iExtra: Int, data: Any?) {
                        if (localCallback != null) {
                                localCallback.invoke(bSuccess)
                        } else {
                                code?.onAction(iAction, bSuccess, iExtra, data!!)
                        }
                }
        }

        // TEXT_ENTERED
        private inner class TextChanged internal constructor(view:View, iAction:Int, localCallback: UiCallback? = null) : UiFlowListener(view, iAction, localCallback) {
                private lateinit var listener:TextWatcher

                override fun register():UiFlowListener {
                        listener = object:TextWatcher {
                                override  fun afterTextChanged(s:Editable) {}
                                override fun beforeTextChanged(s:CharSequence, start:Int, count:Int, after:Int) {}
                                override fun onTextChanged(s:CharSequence, start:Int, before:Int, count:Int) {
                                       onEvent(iAction, true, 0, view)
                                }
                        }

                        listTextListeners.put(view, listener)
                        (view as EditText).addTextChangedListener(listener)
                        return this
                }

                override fun unRegister() {
                        (view as EditText).removeTextChangedListener(listener)
                }

                override fun onEvent(iAction: Int, bSuccess: Boolean, iExtra: Int, data: Any?) {
                        if (localCallback != null) {
                                localCallback.invoke(bSuccess)
                        } else {
                                code?.onAction(iAction, bSuccess, iExtra, data!!)
                        }
                }
        }

        // TEXT_ENTERED
        private inner class TextEntered internal constructor(view:View, iAction:Int, localCallback: UiCallback? = null) : UiFlowListener(view, iAction, localCallback) {

                override fun register(): UiFlowListener {
                        val listKb = object : KeyboardState {
                                override fun onStateChange(bVisible: Boolean) {
                                        if (view.hasFocus() && !bVisible) {
                                                onEvent(iAction, bVisible, 0, view)
                                        }
                                }
                        }

                        listKBListeners.put(view, listKb)
                        addKeyboardListener(listKb)

                        view.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                                if (!hasFocus) {
                                        Log.w("UiFlow", "Text ENTERED on Lost focus")
                                        onEvent(iAction,true, 1, view)
                                }
                        }

                        (view as EditText).setOnEditorActionListener { v, actionId, event->
                                if (actionId == EditorInfo.IME_ACTION_DONE) {
                                        Log.w("UiFlow", "Text ENTERED on KB Done")
                                       onEvent(iAction,true, 3, view)
                                }
                                false }
                        return this
                }

                override fun unRegister() {
                        view.onFocusChangeListener = null
                        (view as EditText).setOnEditorActionListener(null)
                }

                override fun onEvent(iAction: Int, bSuccess: Boolean, iExtra: Int, data: Any?) {
                        if (localCallback != null) {
                                localCallback.invoke(bSuccess)
                        } else {
                                code?.onAction(iAction, bSuccess, iExtra, data!!)
                        }
                }

                private fun addKeyboardListener(keyListener:KeyboardState) {
                        keyList.add(keyListener)
                }
        }
        // LOAD_LAYOUT
        private inner class LoadLayout internal constructor(view:View, iAction:Int, localCallback: UiCallback? = null) : UiFlowListener(view, iAction, localCallback) {
                private lateinit var listener:View.OnLayoutChangeListener

                public override fun register():UiFlowListener {
                        listener = object:View.OnLayoutChangeListener {
                                override fun onLayoutChange(view:View, i:Int, i1:Int, i2:Int, i3:Int, i4:Int, i5:Int, i6:Int, i7:Int) {
                                        if ((i + i1 + i2 + i3) > 0) {
                                                view.removeOnLayoutChangeListener(this)
                                                onEvent(iAction,true, 0, view)
                                        }
                                }
                        }
                        view.addOnLayoutChangeListener(listener)
                        return this
                }

                public override fun unRegister() {
                        view.removeOnLayoutChangeListener(listener)
                }

                override fun onEvent(iAction: Int, bSuccess: Boolean, iExtra: Int, data: Any?) {
                        if (localCallback != null) {
                                localCallback.invoke(bSuccess)
                        } else {
                                code?.onAction(iAction, bSuccess, iExtra, data!!)
                        }
                }
        }
        // ON_CLICK
        private inner class OnClick internal constructor(view:View, iAction:Int, localCallback: UiCallback? = null) : UiFlowListener(view, iAction, localCallback) {
                private var bIsFocusListener = false

                 override fun register():UiFlowListener {
                        if (view is EditText) { // NOTE: for editText first tap get focus, 2nd to trigger onClick, unless focusable is setfalse()
                                bIsFocusListener = true
                                view.setOnFocusChangeListener { v, hasFocus-> if (hasFocus) {
                                                onEvent(iAction, true, 0, view)
                                } }
                        }

                        view.setOnClickListener { view1->
                                bIsFocusListener = false
                                onEvent(iAction, true, 0, view1)
                                 }
                        return this
                }

                 override fun unRegister() {
                        if (bIsFocusListener) {
                                view.onFocusChangeListener = null
                        } else {
                                view.setOnClickListener(null)
                        }
                }

                override fun onEvent(iAction: Int, bSuccess: Boolean, iExtra: Int, data: Any?) {
                        if (localCallback != null) {
                                localCallback.invoke(bSuccess)
                        } else {
                                code?.onAction(iAction, bSuccess, iExtra, data!!)
                        }
                }
        }

        // LOAD_LAYOUT
        private inner class KeyboardStateChange internal constructor(view:View, iAction:Int, localCallback: UiCallback? = null) : UiFlowListener(view, iAction, localCallback) {

                override fun register():UiFlowListener {
                        val list = object:KeyboardState {
                                override fun onStateChange(bVisible:Boolean) {
                                        onEvent(iAction, bVisible, 0, view )
                                }
                        }
                        setUpKeyboardListener(list, view)
                        return this
                }

                override fun unRegister() {
                        removeKeyboardbListener()
                }

                override fun onEvent(iAction: Int, bSuccess: Boolean, iExtra: Int, data: Any?) {
                        if (localCallback != null) {
                                localCallback.invoke(bSuccess)
                        } else{
                                code?.onAction(iAction, bSuccess, iExtra, data!!)
                        }
                }

                // METHOD - sets up a listener for keyboard state change, also change SoftInputMode if its not correct
                private fun setUpKeyboardListener(keyListener:KeyboardState, view:View) {
                        val act = view.context  as Activity // Change soft input mode to SOFT_INPUT_ADJUST_PAN or SOFT_INPUT_ADJUST_RESIZE, for it to work
                        val window = act.getWindow()
                        iSoftInputMode = window.getAttributes().softInputMode // save it so we can restore, when keyboard listener is removed
                        if (iSoftInputMode != WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN && iSoftInputMode != WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
                        rLast = Rect() // set a new rect for storing screen state
                        viewActRoot = view.rootView
                        keyList.add(keyListener)
                        view.viewTreeObserver.addOnGlobalLayoutListener(keybListener)
                }

                // METHOD - sets/removes global keyboard listener, also sets resets SoftInputMode
                private fun removeKeyboardbListener() {
                        viewActRoot?.viewTreeObserver?.removeOnGlobalLayoutListener(keybListener)
                        val act = (viewActRoot as ViewGroup).getChildAt(0).context as Activity
                        val window = act.window
                        iSoftInputMode = window.attributes.softInputMode // save it so we can restore, when keyboard listener is removed
                        if (iSoftInputMode != -1)
                                window.setSoftInputMode(iSoftInputMode)
                }

                private val keybListener = ViewTreeObserver.OnGlobalLayoutListener {
                        val rCur = Rect()
                        viewActRoot?.getWindowVisibleDisplayFrame(rCur)
                        if (rLast.bottom == 0) {
                                rLast.bottom = viewActRoot!!.height
                        } // just get size of window, something to start with

                        if ((rLast.bottom - rCur.bottom) > 200) { // means keyboard is visible
                                if (!bKeybVisible) { // if its not already set set it
                                        bKeybVisible = true
                                        rLast = rCur
                                        for (listener in keyList)
                                                listener.onStateChange(true)
                                }
                        } else if ((rCur.bottom - rLast.bottom) > 200) {
                                if (bKeybVisible) {
                                        bKeybVisible = false
                                        rLast = rCur
                                        for (listener in keyList)
                                                listener.onStateChange(false)
                                }
                        }
                }

        }
}