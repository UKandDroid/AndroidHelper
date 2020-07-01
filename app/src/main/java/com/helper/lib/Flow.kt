package com.stryde.base.libs;

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import java.util.*

typealias SingleCallback = (bSuccess: Boolean) -> Unit

// Version 3.0.1
// Fixed bug where runAction() causes exception
// encapsulation for Action & Event classes
// Added <Generic Type> based events
// Added getEventsForAction(), getErrorEventForAction()
// Added runType for events RESULT_CHANGE, RESULT_UPDATE, EVENT_UPDATE
// Added Help examples
// ## EXAMPLES ##
// var flow = Flow<String>( flowCode )
// Example 1: flow.registerAction(1,  listOf("email_entered", "password_entered", "verify_code_entered") ) action 1 gets called when all those events occur
//          : flow.event("email_entered", true, extra(opt), object(opt))  is trigger for the registered event "email_entered",
//          :  when all three events are triggered with flow.event(...., true), action 1 is executed with bSuccess = true
//          :  after 3 event true(s), if one event(...., false) sends false, action 1 will be executed with bSuccess = false
//          :  now action 1 will only trigger again when all onEvents(...., true) are true, i.e the events which sent false, send true again
// Example : flow.run(3, true(opt), extra(opt), object(opt))  runs an action on background thread, same as registering for one event and triggering that event
// Example : flow.runOnUi(4, true(opt), extra(opt), object(opt)) runs code on Ui thread
// Example : flow.runDelayed(5, true(opt), extra(opt), 4000) runs delayed code
// Example : flow.runDelayedOnUi(6, true(opt), extra(opt), 4000) { //optional } runs delayed code on Ui thread
// var flowCode = object: Flow.ExecuteCode(){
//  @override fun onAction(int iAction, boolean bSuccess, int iExtra, Object data){
//  when(iAction){
//       1 ->   // this code will run in first example when all events are triggered as true
//       3 ->   // this will run when ever run(3) is called
//       4 ->   // this will run on ui thread whenever runOnUi(4) is called
//       5 ->   // this will run on delayed by 4 secs
//       6(NOT CALLED) ->   this wont be called as local callback is provided
// }  }

// Example :  flow.runDelayed(2000){}
// Example :  flow.runRepeat(500){}
// Example :  flow.getEventsForAction(1) // returns all events associated with the action
// Example :  flow.getActionWaitingEvent(1) // returns first event that is stopping the action being fired, either its not fired or fired with false

open class Flow<EventType> @JvmOverloads constructor(codeCallback: ExecuteCode? = null) : LifecycleObserver {

    enum class EventStatus{
        WAITING, SUCCESS, FAILURE
    }

    private var autoIndex = -1
    private var bRunning = true
    private var hThread: HThread
    private var listActions: MutableList<_Action> = ArrayList() // List of registered actions
    private var globalCallback: ExecuteCode? = null // Call back for onAction to be executed

    // INTERFACE for code execution
    interface ExecuteCode {
        fun onAction(iAction: Int, bSuccess: Boolean, iExtra: Int, data: Any?)
    }

    init {
        globalCallback = codeCallback
        hThread = HThread()
    }

    fun execute(codeCallback: ExecuteCode) {
        globalCallback = codeCallback
    }

    // STATE METHODS pause, resume, stop the action, should be called to release resources
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    open fun pause() {
        hThread.mHandler.removeCallbacksAndMessages(null)
        hThread.mUiHandler.removeCallbacksAndMessages(null)
        bRunning = false
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    open fun resume() {
        bRunning = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    open fun stop() {
        globalCallback = null
        try {
            for (action in listActions) { action.recycle() }
            hThread.mHandler.removeCallbacksAndMessages(null)
            hThread.mUiHandler.removeCallbacksAndMessages(null)
            hThread.stop()
            listActions = ArrayList()
            _Event.releasePool()
            bRunning = false
        } catch (e: Exception) {
        }
    }

    // METHOD sets the type of action callback
    // RESULT_CHANGE = When result changes from false to true or true to false
    // RESULT_UPDATE = when result updates means all events are fired a
    // EVENT_UPDATE = whenever an event associated with action is updated
    fun actionCallbackType(iAction:Int, type: ResultType) {
        _getAction(iAction).resultType = type
    }

    fun getAction(iAction: Int) = _getAction(iAction) as Flow.Action
    fun getActionEvents(iAction: Int) = _getAction(iAction).getEvents()
    fun getActionWaitingEvent(iAction: Int) = _getAction(iAction).getWaitingEvent() // Returns first found event that is stopping the action from triggering
    fun resetAction(iAction: Int) { _getAction(iAction).reset() }                   // Resets action by resetting all events to initial Waiting state
    private fun _getAction(iAction: Int) = listActions.first { it.iAction == iAction } // throws NoSuchElementException if action not found

    @JvmOverloads
    fun runAction(iAction: Int, bUiThread: Boolean = false, bSuccess: Boolean = true, iExtra: Int = 0, obj: Any? = null): Flow<EventType> {
        if (bUiThread) hThread.runOnUI(iAction, bSuccess, iExtra, obj) else hThread.run(iAction, bSuccess, iExtra, obj)
        return this
    }

    @JvmOverloads
    fun runRepeat(iAction: Int = autoIndex--, bUiThread: Boolean = false, iDelay: Long, callback: SingleCallback? = null): Flow<EventType> {
        val delayEvent = "repeat_event_$iAction"
        _registerAction(iAction, bUiThread, false, false, true, listOf(delayEvent), callback)
        hThread.mHandler.postDelayed((Runnable { this.event(delayEvent, true, 0, iDelay) }), iDelay)
        return this
    }

    @JvmOverloads
    fun runDelayed(iAction: Int = autoIndex--, bUiThread: Boolean = false, iDelay: Long, bSuccess: Boolean = true, iExtra: Int = 0, any: Any? = null, callback: SingleCallback? = null): Flow<EventType> {
        val delayEvent = "delay_event_$iAction"
        _registerAction(iAction, bUiThread, true, false, false, listOf(delayEvent), callback)
        hThread.mHandler.postDelayed((Runnable { this.event(delayEvent, bSuccess, iExtra, any) }), iDelay)
        return this
    }

    @JvmOverloads
    fun registerAction(iAction: Int, bUiThread: Boolean = false, events: List<EventType>, singleCallback: SingleCallback? = null): Flow<EventType> {
        _registerAction(iAction, bUiThread, false, false, false, events, singleCallback)
        return this
    }

    @JvmOverloads
    fun waitForEvents(iAction: Int, bUiThread: Boolean = false, events: List<EventType>): Flow<EventType> {
        _registerAction(iAction, bUiThread, true, false, false, events)
        return this
    }

    fun registerActionSequence(iAction: Int, bUiThread: Boolean, events: List<EventType>, singleCallback: SingleCallback? = null): Flow<EventType> {
        _registerAction(iAction, bUiThread, false, true, false, events, singleCallback)
        return this
    }

    fun cancelAction(iAction: Int) {
        listActions.firstOrNull { it.iAction == iAction }?.run {
            _cancelAction(this)
            loge("CANCEL: Action($iAction), removed  ")
        }
    }

    private fun _cancelAction(action: _Action){
        hThread.mHandler.removeMessages(action.iAction)
        hThread.mUiHandler.removeMessages(action.iAction)
        action.recycle()
        listActions.remove(action)
    }

    private fun _registerAction(iAction: Int, bUiThread: Boolean, bRunOnce: Boolean, bSequence: Boolean, bRepeat: Boolean, events: List<*>, actionCallback: SingleCallback? = null) {
        assert(iAction > 0)
        cancelAction(iAction) // to stop duplication, remove if the action already exists
        val actionFlags = setActionFlags(runOnUI = bUiThread, runOnce = bRunOnce, eventSequence = bSequence, repeatAction = bRepeat)
        val aAction = _Action(iAction, actionFlags, events, actionCallback)
        listActions.add(aAction)

        val buf = StringBuffer(400)
        for (event in events) {
            buf.append(event.toString() + ", ")
        }

        log("ACTION: $iAction registered  EVENTS = { $buf}")
    }

    // METHODS to send event
    @JvmOverloads
    fun event(sEvent: Any, bSuccess: Boolean = true, iExtra: Int = 0, obj: Any? = null) {
        if (!bRunning)
            return

        log("EVENT:  $sEvent $bSuccess")

        try {
            for (action in listActions) {
                action.onEvent(sEvent, bSuccess, iExtra, obj)
            }
        } catch (e: IndexOutOfBoundsException) {
            loge(e.toString())
        }
    }

    // METHOD cancel a runDelay or RunRepeated
    fun cancelRun(iAction: Int) {
        if (!bRunning) return
        hThread.mHandler.removeMessages(iAction)
        hThread.mUiHandler.removeMessages(iAction)
    }

    // CLASS for events for action, when all events occur action is triggered
    open class Action() {
        protected var listEvents: MutableList<Event<*>> = ArrayList() // List to store Flow.Events needed for this action
        protected var iLastStatus = EventStatus.WAITING       // Event set status as a whole, waiting, success, non success

        // returns first event that has not been fired or fired with false
        fun getWaitingEvent() = listEvents.first { !it.isSuccess() }.event // Throws exception if event not found
        fun getEvents() = listEvents as List<Event<*>>
        fun getEvent(event: Any) = getEvents().firstOrNull{ it.event == event }
        fun isWaitingFor(event: Any) = getWaitingEvent() == event

        fun reset() {
            iLastStatus = EventStatus.WAITING
            for (event in listEvents) {
                (event as _Event).resetEvent()
            }
        }
    }

    // CLASS for event Pool
    open class Event<ActionEvents>  { // CONSTRUCTOR - Private
        var obj: Any? = null
        var extra = 0
        var event: ActionEvents? = null
        protected var _status :EventStatus = EventStatus.WAITING // WAITING - waiting not fired yet, SUCCESS - fired with success, FAILURE - fired with failure
        // Variable for pool
        fun isSuccess() = _status == EventStatus.SUCCESS

    }

    private inner class _Action(
            internal val iAction: Int,
            private var actionFlags: Int = 0,
            events: List<*>,
            private var singleCallback: SingleCallback? = null
    ) : Action() {
        internal var resultType: ResultType = ResultType.RESULT_CHANGE
        internal var iEventCount: Int = events.size          // How many event are for this action code to be triggered

        init {
            for (i in 0 until iEventCount) {
                listEvents.add(_Event.obtain(events[i])) // get events from events pool
            }
        }

        internal fun getFlag(flag: Int) = Companion.getFlag(actionFlags, flag)
        internal fun setFlag(flag: Int) {
            Companion.setFlag(actionFlags, flag, true)
        }

        internal fun clearFlag(flag: Int) {
            Companion.setFlag(actionFlags, flag, false)
        }

        internal fun callback(bSuccess: Boolean): Boolean {
            if (singleCallback != null) {
                singleCallback?.invoke(bSuccess)
                return true
            }
            return false
        }

        internal fun getEventData(events: EventType): Any? {
            return listEvents.find { it.event == events }?.obj
        }


        // METHOD recycles events and clears actions
        internal fun recycle() {
            singleCallback = null
            iEventCount = 0
            for (event in listEvents) {
                (event as _Event).recycle()
            }
            listEvents = ArrayList()
        }

        // METHOD searches all actions, if any associated with this event
        fun onEvent(sEvent: Any, bResult: Boolean, iExtra: Int, obj: Any?): Pair<Boolean, Boolean> {
            var iSuccess = 0        // How many successful events has been fired
            var iFiredCount = 0     // How many events for this action have been fired
            var bEventFound = false
            var bActionExecuted = false

            for (i in 0 until iEventCount) {
                val event = listEvents[i] as _Event

                if (sEvent == event.event) { // If event is found in this event list
                    bEventFound = true
                    event.setData(bResult, iExtra, obj )
                } else if (getFlag(FLAG_SEQUENCE) && event.statusIs(EventStatus.WAITING)) {         // if its a Sequence action, no event should be empty before current event
                    if (i != 0) {
                        (listEvents[i - 1] as _Event).setStatus( EventStatus.WAITING )                 // reset last one, so they are always in sequence
                    }
                    break
                }

                when (event.status()) {
                    EventStatus.FAILURE -> iFiredCount++
                    EventStatus.SUCCESS -> {
                        iSuccess++
                        iFiredCount++                                                               // Add to fired event regard less of success or failure
                    }
                }

                if (bEventFound && getFlag(FLAG_SEQUENCE)) break
            }

            if (bEventFound) {                                                                      // if event was found in this Action
                logw("ACTION: $iAction Event: $sEvent fired { Total $iEventCount  Fired: $iFiredCount  Success: $iSuccess }")

                if (resultType == ResultType.EVENT_UPDATE) {                                        // if this action is launched on every event update
                    executeAction(bResult, iExtra)
                } else if (iFiredCount == iEventCount) {                                            // if all events for action has been fired
                    val bSuccess = iSuccess == iEventCount                                          // all events registered success
                    val iCurStatus = if (bSuccess) EventStatus.SUCCESS else EventStatus.FAILURE

                    when (resultType) {
                        ResultType.RESULT_CHANGE -> if (iCurStatus != iLastStatus) {                // If there is a change in action status only then run code
                            bActionExecuted = true
                            iLastStatus = iCurStatus
                            executeAction(bSuccess, iSuccess)
                        }
                        ResultType.RESULT_UPDATE -> if (bSuccess) {
                            bActionExecuted = true
                            executeAction(bSuccess, iSuccess)
                        }
                    }
                }
            }

            return Pair(bActionExecuted, getFlag(FLAG_RUNONCE))
        }

        // METHOD executes action code on appropriate thread
        private fun executeAction(bSuccess: Boolean, iExtra: Int) {
            logw("ACTION: $iAction Executed with $bSuccess ")

            if (getFlag(FLAG_RUNonUI)) {
                hThread.runOnUI(iAction, bSuccess, iExtra, this as Flow.Action)
            } else {
                hThread.run(iAction, bSuccess, iExtra, this)
            }
        }
    }

    private class _Event<ExternalEvents> private constructor() : Event<ExternalEvents>(){
        companion object {
            // EVENTS for self use
            private var next: _Event<Any>? = null // Reference to next object
            private var sPool: _Event<Any>? = null
            private var sPoolSize = 0
            private const val MAX_POOL_SIZE = 50
            private val sPoolSync = Any() // The lock used for synchronization

            // METHOD get pool object only through this method, so no direct allocation are made
            fun <ExternalEvents> obtain(sId: ExternalEvents?): _Event<*> {
                synchronized(sPoolSync) {
                    if (sPool != null) {
                        val e = sPool as _Event<ExternalEvents>
                        e.event = sId
                        e.setData(EventStatus.WAITING, 0, null)
                        sPool = next
                        next = null
                        sPoolSize --
                        return e
                    }
                    val eve = _Event<ExternalEvents>()
                    eve.event = sId
                    return eve
                }
            }

            // METHOD release pool, ready for garbage collection
            fun releasePool() {
                sPoolSize = 0
                sPool = null
            }
        }

        fun status() = _status
        fun setStatus(value: EventStatus) { _status = value }
        fun statusIs(value: EventStatus) = _status == value

        fun resetEvent() {
            obj = null
            extra = 0
            _status = EventStatus.WAITING
        }

        fun setData(status : EventStatus, ext : Int, data: Any? ) {
            _status = status
            obj = data
            extra = ext
        }

        fun setData(status : Boolean, ext : Int, data: Any? ) {
            _status = if (status) EventStatus.SUCCESS else EventStatus.FAILURE
            obj = data
            extra = ext
        }

        // METHOD object added to the pool, to be reused
        internal fun recycle() {
            synchronized(sPoolSync) {
                if (sPoolSize < MAX_POOL_SIZE) {
                    next = sPool as _Event<Any>?
                    sPool = this as _Event<Any>
                    sPoolSize++
                }
            }
        }
    }


    // CLASS for thread handler
    private inner class HThread internal constructor() : Handler.Callback {
        val mHandler: Handler
        val mUiHandler: Handler
        var ACTION_FAIL = 0
        var ACTION_SUCCESS = 1

        @JvmOverloads
        fun run(iStep: Int, bRunUI: Boolean = false) {
            if (bRunUI) {
                runOnUI(iStep, true, 0, null)
            } else {
                run(iStep, true, 0, null)
            }
        }

        fun run(iAction: Int, bSuccess: Boolean, iExtra: Int, obj: Any?) {
            if (bRunning) {
                val msg = Message.obtain()
                msg.what = iAction
                msg.arg1 = iExtra
                msg.arg2 = if (bSuccess) ACTION_SUCCESS else ACTION_FAIL
                msg.obj = obj
                mHandler.sendMessage(msg)
            }
        }

        fun runOnUI(iAction: Int, bSuccess: Boolean, iExtra: Int, obj: Any?) {
            if (bRunning) {
                val msg = Message.obtain()
                msg.what = iAction
                msg.arg1 = iExtra
                msg.arg2 = if (bSuccess) ACTION_SUCCESS else ACTION_FAIL
                msg.obj = obj
                mUiHandler.sendMessage(msg)
            }
        }

        // METHOD MESSAGE HANDLER
        override fun handleMessage(msg: Message): Boolean {
            if (msg.obj !is Flow<*>._Action) { // called directly with runAction() action probably does not exist

                globalCallback?.onAction(msg.what, msg.arg2 == ACTION_SUCCESS, msg.arg1, msg.obj)

            } else { // is Flow Action with events
                val action = msg.obj as Flow<EventType>._Action

                if (action.getFlag(FLAG_REPEAT)) {      // If its a repeat action, we have to post it again
                    val event = action.getEvents()[0] // get delay event for data
                    hThread.mHandler.postDelayed((Runnable { action.onEvent(event.event!!, !event.isSuccess(), event.extra++, event.obj) }), event.obj as Long)
                    // posting action.onEvent() to repeat action only, not Flow.event(), to keep it local and filling queue for fast repeating actions
                }

                if (!action.callback(msg.arg2 == ACTION_SUCCESS)) { // if there is no specific callback for action, call generic call back
                    globalCallback?.onAction(msg.what, msg.arg2 == ACTION_SUCCESS, msg.arg1, msg.obj as Flow.Action)
                }

                if (action.getFlag(FLAG_RUNONCE)) {
                    loge("REMOVING: Action(${action.iAction}, runOnce) as its executed")
                    _cancelAction(action) // Recycle if its flagged for it
                }
            }

            return true
        }

        fun stop() {
            mHandler.removeCallbacksAndMessages(null)
            mUiHandler.removeCallbacksAndMessages(null)
            mHandler.looper.quit()
        }

        init {
            val ht = HandlerThread("BGThread_ ${++iThreadCount}")
            ht.start()
            mHandler = Handler(ht.looper, this)
            mUiHandler = Handler(Looper.getMainLooper(), this)
        }
    }

    // METHOD for logging
    protected fun log(sLog: String) {
        log(1, sLog)
    }

    protected fun loge(sLog: String?) {
        loge(1, sLog)
    }

    protected fun logw(sLog: String?) {
        logw(1, sLog)
    }

    private fun log(iLevel: Int, sLog: String) {
        if (iLevel <= LOG_LEVEL) {
            Log.d(LOG_TAG, sLog)
        }
    }

    protected fun loge(iLevel: Int, sLog: String?) {
        if (iLevel <= LOG_LEVEL) {
            Log.e(LOG_TAG, sLog)
        }
    }

    protected fun logw(iLevel: Int, sLog: String?) {
        if (iLevel <= LOG_LEVEL) {
            Log.w(LOG_TAG, sLog)
        }
    }

    enum class ResultType {
        RESULT_CHANGE, RESULT_UPDATE, EVENT_UPDATE
    }

    companion object {
        private var iThreadCount = 0
        private const val LOG_LEVEL = 4
        private const val LOG_TAG = "Flow"
        private const val FLAG_SUCCESS = 0x00000001
        private const val FLAG_RUNonUI = 0x00000002
        private const val FLAG_REPEAT = 0x00000004
        private const val FLAG_RUNONCE = 0x00000008
        private const val FLAG_SEQUENCE = 0x00000010


        // METHODS for packing data for repeat event
        private fun addExtraInt(iValue: Int, iData: Int): Int {
            return iValue or (iData shl 8)
        }

        private fun getExtraInt(iValue: Int): Int {
            return iValue shr 8
        }

        private fun getFlag(iValue: Int, iFlag: Int): Boolean {
            return iValue and iFlag == iFlag
        }

        private fun setFlag(iValue: Int, iFlag: Int, bSet: Boolean = true): Int {
            return if (bSet) {
                iValue or iFlag
            } else {
                iValue and iFlag.inv()
            }
        }

        private fun setActionFlags(runOnUI: Boolean = false, runOnce: Boolean = false, eventSequence: Boolean = false, repeatAction: Boolean = false): Int {
            var intFlags: Int = 0

            intFlags = setFlag(intFlags, FLAG_RUNonUI, runOnUI)
            intFlags = setFlag(intFlags, FLAG_RUNONCE, runOnce)
            intFlags = setFlag(intFlags, FLAG_SEQUENCE, eventSequence)
            intFlags = setFlag(intFlags, FLAG_REPEAT, repeatAction)

            return intFlags
        }
    }

}
