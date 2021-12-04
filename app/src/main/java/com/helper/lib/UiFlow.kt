package com.helper.lib

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Rect
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import java.util.*

typealias UiCallback = (bSuccess: Boolean) -> Unit

class UiFlow(codeCallback: Code) : LifecycleObserver {
    private val INVALID = -1
    private var bPause = false   // listeners have to implement pause functionality themself
    private var code: Code? = codeCallback
    private var keyList = ArrayList<KeyboardState>()
    private var flowListeners = ArrayList<UiFlowListener>()

    private interface KeyboardState {
        fun onStateChange(bVisible: Boolean)
    }

    interface Code {
        fun onAction(action: Int, bSuccess: Boolean, iExtra: Int, viewData: Any)
    }

    private abstract inner class UiFlowListener(
        protected val view: View,
        protected val iAction: Int,
        protected val localCallback: UiCallback? = null
    ) {
        abstract fun register(): UiFlowListener
        abstract fun unRegister()
        protected abstract fun onEvent(iAction: Int, bSuccess: Boolean, iExtra: Int, data: Any?)
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
        KEYBOARD_STATE_CHANGE   // Works only for android:windowSoftInputMode="adjustResize" or "adjustPan"
    }

    fun registerClick(view: View, localCallback: UiCallback? = null) {
        registerListener(-1, view, UiEvent.ON_CLICK, localCallback)
    }

    fun registerClick(iAction: Int, view: View, localCallback: UiCallback? = null) {
        registerListener(iAction, view, UiEvent.ON_CLICK, localCallback)
    }

    @JvmOverloads
    fun registerUiEvent(
        iAction: Int,
        view: View,
        iEvent: UiEvent,
        localCallback: UiCallback? = null
    ): UiFlow {
        registerListener(iAction, view, iEvent, localCallback)
        return this
    }

    @JvmOverloads
    fun registerUiEvent(view: View, iEvent: UiEvent, localCallback: UiCallback? = null): UiFlow {
        registerListener(-1, view, iEvent, localCallback)
        return this
    }

    // VIEW LISTENERS set event listeners for View objects
    private fun registerListener(
        iAction: Int,
        view: View,
        iListener: UiEvent,
        localCallback: UiCallback? = null
    ) {
        when (iListener) {
            // Triggered when Text entered in text field, i.e when text field loses focus, enter button is pressed on keyboard
            // for text entered to work with keyboard hide, set android:windowSoftInputMode="adjustResize" or "adjustPan"
            // and setup KEYBOARD_STATE UiEvent, provided with main activity root decor view
            UiEvent.TEXT_ENTERED -> flowListeners.add(
                TextEnteredListener(
                    view,
                    iAction,
                    localCallback
                ).register()
            ) // Triggered when text changes

            UiEvent.TEXT_CHANGED -> flowListeners.add(
                TextChangedListener(
                    view,
                    iAction,
                    localCallback
                ).register()
            )

            // Triggered when ui layout changes with width/height values > 0 and called only once
            UiEvent.LOAD_LAYOUT -> flowListeners.add(
                LoadLayoutListener(
                    view,
                    iAction,
                    localCallback
                ).register()
            )

            UiEvent.ON_CLICK -> flowListeners.add(
                OnClickListener(
                    view,
                    iAction,
                    localCallback
                ).register()
            )// triggered listener when view is clicked

            // Method reports keyboard state change, should be provided with view, uses view.getRootView() to get parent view
            UiEvent.KEYBOARD_STATE_CHANGE -> flowListeners.add(
                KeyboardStateChangeListener(
                    view,
                    iAction,
                    localCallback
                ).register()
            )

            UiEvent.LIST_ITEM_SELECT -> flowListeners.add(
                ListItemSelectListener(
                    view,
                    iAction,
                    localCallback
                ).register()
            )

            UiEvent.SPINNER_ITEM_SELECT -> flowListeners.add(
                SpinnerItemSelectListener(
                    view,
                    iAction,
                    localCallback
                ).register()
            )


            UiEvent.ON_CHECKBOX,
            UiEvent.ON_TOGGLE -> flowListeners.add(
                CompoundButtonListener(
                    view,
                    iAction,
                    localCallback
                ).register()
            )

            UiEvent.ON_SWITCH -> flowListeners.add(
                CompoundButtonListener(
                    view,
                    iAction,
                    localCallback
                ).register()
            )

            // Listener returns true for Touch down and Move, false when finger is lifted up
            UiEvent.ON_TOUCH -> flowListeners.add(
                TouchListener(
                    view,
                    iAction,
                    localCallback
                ).register()
            )
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

        code = null
        keyList.clear()
        flowListeners.clear()
    }

    // LIST_ITEM_SELECT
    private inner class SpinnerItemSelectListener internal constructor(
        view: View,
        iAction: Int,
        localCallback: UiCallback? = null
    ) : UiFlowListener(view, iAction, localCallback) {
        override fun register(): UiFlowListener {
            (view as Spinner).onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    onEvent(iAction, true, position, view)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    onEvent(iAction, false, -1, view)
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
    private inner class TouchListener internal constructor(
        view: View,
        iAction: Int,
        localCallback: UiCallback? = null
    ) : UiFlowListener(view, iAction, localCallback) {
        @SuppressLint("ClickableViewAccessibility")
        override fun register(): UiFlowListener {
            view.setOnTouchListener { v, event ->
                onEvent(iAction, event.action != MotionEvent.ACTION_UP, event.action, event)
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
    private inner class CompoundButtonListener internal constructor(
        view: View,
        iAction: Int,
        localCallback: UiCallback? = null
    ) : UiFlowListener(view, iAction, localCallback) {
        override fun register(): UiFlowListener {
            (view as CompoundButton).setOnCheckedChangeListener { buttonView, isChecked ->
                onEvent(iAction, isChecked, 0, view)
            }
            return this
        }

        override fun unRegister() {
            (view as CompoundButton).setOnCheckedChangeListener(null)
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
    private inner class ListItemSelectListener internal constructor(
        view: View,
        iAction: Int,
        localCallback: UiCallback? = null
    ) : UiFlowListener(view, iAction, localCallback) {
        override fun register(): UiFlowListener {
            (view as ListView).setOnItemClickListener { parent, view12, position, id ->
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
    private inner class TextChangedListener constructor(
        view: View,
        iAction: Int,
        localCallback: UiCallback? = null
    ) : UiFlowListener(view, iAction, localCallback) {
        private lateinit var listener: TextWatcher

        override fun register(): UiFlowListener {
            listener = object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    onEvent(iAction, true, 0, view)
                }
            }

            // listTextListeners.put(view, listener)
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
    private inner class TextEnteredListener constructor(
        view: View,
        iAction: Int,
        localCallback: UiCallback? = null
    ) : UiFlowListener(view, iAction, localCallback) {

        override fun register(): UiFlowListener {
            val listKb = object : KeyboardState {
                override fun onStateChange(bVisible: Boolean) {
                    if (view.hasFocus() && !bVisible) {
                        onEvent(iAction, bVisible, 0, view)
                    }
                }
            }

            addKeyboardListener(listKb)

            view.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    Log.w("UiFlow", "Text ENTERED on Lost focus")
                    onEvent(iAction, true, 1, view)
                }
            }

            (view as EditText).setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Log.w("UiFlow", "Text ENTERED on KB Done")
                    onEvent(iAction, true, 3, view)
                }
                false
            }
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

        private fun addKeyboardListener(keyListener: KeyboardState) {
            keyList.add(keyListener)
        }
    }

    // LOAD_LAYOUT
    private inner class LoadLayoutListener internal constructor(
        view: View,
        iAction: Int,
        localCallback: UiCallback? = null
    ) : UiFlowListener(view, iAction, localCallback) {
        private lateinit var listener: View.OnLayoutChangeListener

        public override fun register(): UiFlowListener {
            listener = object : View.OnLayoutChangeListener {
                override fun onLayoutChange(
                    view: View,
                    i: Int,
                    i1: Int,
                    i2: Int,
                    i3: Int,
                    i4: Int,
                    i5: Int,
                    i6: Int,
                    i7: Int
                ) {
                    if ((i + i1 + i2 + i3) > 0) {
                        view.removeOnLayoutChangeListener(this)
                        onEvent(iAction, true, 0, view)
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
    private inner class OnClickListener internal constructor(
        view: View,
        iAction: Int,
        localCallback: UiCallback? = null
    ) : UiFlowListener(view, iAction, localCallback) {
        private var bIsFocusListener = false

        override fun register(): UiFlowListener {
            if (view is EditText) { // NOTE: for editText first tap get focus, 2nd to trigger onClick, unless focusable is setfalse()
                bIsFocusListener = true
                view.setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        onEvent(iAction, true, 0, view)
                    }
                }
            }

            view.setOnClickListener { view1 ->
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
    private inner class KeyboardStateChangeListener internal constructor(
        view: View,
        iAction: Int,
        localCallback: UiCallback? = null
    ) : UiFlowListener(view, iAction, localCallback) {

        private var iSoftInputMode = INVALID
        private var lastPosition = Rect()
        private var bKeybVisible = false
        private var viewActRoot: View? = null

        override fun register(): UiFlowListener {
            val list = object : KeyboardState {
                override fun onStateChange(bVisible: Boolean) {
                    onEvent(iAction, bVisible, 0, view)
                }
            }
            setUpKeyboardListener(list, view)
            return this
        }

        override fun unRegister() {
            removeKeyboardListener()
            viewActRoot = null
        }

        override fun onEvent(iAction: Int, bSuccess: Boolean, iExtra: Int, data: Any?) {
            if (localCallback != null) {
                localCallback.invoke(bSuccess)
            } else {
                code?.onAction(iAction, bSuccess, iExtra, data!!)
            }
        }

        // METHOD - sets up a listener for keyboard state change, also change SoftInputMode if its not correct
        private fun setUpKeyboardListener(keyListener: KeyboardState, view: View) {
            val act =
                view.context as Activity // Change soft input mode to SOFT_INPUT_ADJUST_PAN or SOFT_INPUT_ADJUST_RESIZE, for it to work
            val window = act.getWindow()
            iSoftInputMode =
                window.getAttributes().softInputMode // save it so we can restore, when keyboard listener is removed
            if (iSoftInputMode != WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN && iSoftInputMode != WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
            lastPosition = Rect() // set a new rect for storing screen state
            viewActRoot = view.rootView
            keyList.add(keyListener)
            view.viewTreeObserver.addOnGlobalLayoutListener(keybListener)
        }

        // METHOD - sets/removes global keyboard listener, also sets resets SoftInputMode
        private fun removeKeyboardListener() {
            viewActRoot?.viewTreeObserver?.removeOnGlobalLayoutListener(keybListener)
            val act = (viewActRoot as ViewGroup).getChildAt(0).context as Activity
            val window = act.window
            iSoftInputMode =
                window.attributes.softInputMode // save it so we can restore, when keyboard listener is removed
            if (iSoftInputMode != INVALID)
                window.setSoftInputMode(iSoftInputMode)
        }

        private val keybListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rCur = Rect()
            viewActRoot?.getWindowVisibleDisplayFrame(rCur)
            if (lastPosition.bottom == 0) {
                lastPosition.bottom = viewActRoot!!.height
            } // just get size of window, something to start with

            if ((lastPosition.bottom - rCur.bottom) > 200) { // means keyboard is visible
                if (!bKeybVisible) { // if its not already set set it
                    bKeybVisible = true
                    lastPosition = rCur
                    for (listener in keyList)
                        listener.onStateChange(true)
                }
            } else if ((rCur.bottom - lastPosition.bottom) > 200) {
                if (bKeybVisible) {
                    bKeybVisible = false
                    lastPosition = rCur
                    for (listener in keyList)
                        listener.onStateChange(false)
                }
            }
        }

    }
}