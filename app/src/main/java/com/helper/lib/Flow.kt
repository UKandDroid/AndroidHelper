package com.helper.lib;

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Log
import java.util.*

typealias SingleCallback = (bSuccess: Boolean) -> Unit

// Version 2.4.1
// Added <Generic Type> based events
// Added getEventsForAction(), getErrorEventForAction()
// Added runType for events RESULT_CHANGE, RESULT_UPDATE, EVENT_UPDATE
// Added Help examples
// ## EXAMPLES ##
//Flow<TriggerEvents>flow = new Flow(flowCode)
// Example 1: flow.registerAction(1, "email_entered", "password_entered", "verify_code_entered" ) action 1 gets called when all those events occur
//          : flow.event("email_entered", true, extra(opt), object(opt))  is trigger for the registered event "email_entered",
//          :  when all three events are triggered with flow.event(...., true), action 1 is executed with bSuccess = true
//          :  after 3 event true(s), if one event(...., false) sends false, action 1 will be executed with bSuccess = false
//          :  now action 1 will only trigger again when all onEvents(...., true) are true, i.e the events which sent false, send true again
// Example : flow.run(3, true(opt), extra(opt), object(opt)) runs an action on background thread, same as registering for one event and triggering that event
// Example : flow.runOnUi(4, true(opt), extra(opt), object(opt)) runs code on Ui thread
// Example : flow.runDelayed(5, true(opt), extra(opt), 4000) runs delayed code
// Example : flow.runDelayedOnUi(6, true(opt), extra(opt), 4000) runs delayed code on Ui thread
// Flow.Code flowCode = new Flow.Code(){
//  @override public void onAction(int iAction, boolean bSuccess, int iExtra, Object data){
//  switch(iAction){
//      case 1:  ...... break;   // this code will run in first example when all events are triggered as true
//      case 2: ...... break;    // this code will run when a spinner item is selected
//      case 3: ....... break;   // this will run when ever run(3) is called
//      case 4: ........ break;  // this will run on ui thread whenever runOnUi(4) is called
//      case 5: ........ break;  // this will run on delayed by 4 secs
// }  }
// Example :  Flow().runDelayed(2000).execute(() -{})
// Example :  Flow().runRepeat(500).execute(() -{})
// Example :  flow.getEventsForAction(1) // returns all events associated with the action
// Example :  flow.getErrorEventForAction(1) // returns first event that is stopping the action being fired, either its not fired or fired with false

open class Flow<ActionEvents> @JvmOverloads constructor(codeCallback: FlowCode? = null) : LifecycleObserver {

    enum class EventStatus{
        WAITING, SUCCESS, FAILURE
    }

    private var bRunning = true
    internal var hThread: HThread
    private var listActions: MutableList<_Action> = ArrayList() // List of registered actions
    private var globalCallback: FlowCode? = null // Call back for onAction to be executed

    // INTERFACE for code execution
    interface FlowCode {
        fun onAction(iAction: Int, bSuccess: Boolean, iExtra: Int, data: Any?)
    }

    init {
        globalCallback = codeCallback
        hThread = HThread()
    }

    fun execute(codeCallback: FlowCode) {
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
            Event.releasePool()
            bRunning = false
        } catch (e: Exception) {
        }
    }

    // METHOD sets the type of action callback
    // RESULT_CHANGE = When result changes from false to true or true to false
    // RESULT_UPDATE = when result updates means all events are fired a
    // EVENT_UPDATE = whenever an event associated with action is updated
    fun actionCallbackType(type: ResultType) {
        if (listActions.size > 0) listActions[listActions.size - 1].resultType = type
    }

    private fun _getAction(iAction: Int) = listActions.first { it.iAction == iAction }
    fun getAction(iAction: Int) = _getAction(iAction) as Flow<*>.Action
    fun getActionEvents(iAction: Int) = _getAction(iAction).getEventsList()
    fun getActionWaitingEvent(iAction: Int) = _getAction(iAction).getWaitingEvent() // // Returns first found event that is stopping the action from triggering
    fun resetAction(iAction: Int) { _getAction(iAction).reset() } // Resets action by resetting all events to initial Waiting state

    @JvmOverloads
    fun run(iAction: Int = -1, bUiThread: Boolean = false, bSuccess: Boolean = true, iExtra: Int = 0, obj: Any? = null): Flow<ActionEvents> {
        if (bUiThread) hThread.runOnUI(iAction, bSuccess, iExtra, obj) else hThread.run(iAction, bSuccess, iExtra, obj)
        return this
    }

    @JvmOverloads
    fun runRepeat(iAction: Int, bUiThread: Boolean = false, iDelay: Long, callback: SingleCallback? = null): Flow<ActionEvents> {
        val delayEvent = "repeat_event_$iAction"
        _registerAction(iAction, bUiThread, false, false, true, listOf(delayEvent), callback)
        hThread.mHandler.postDelayed((Runnable { this.event(delayEvent, true, 0, iDelay) }), iDelay)
        return this
    }

    @JvmOverloads
    fun runDelayed(iAction: Int, bUiThread: Boolean = false, iTime: Long, bSuccess: Boolean = true, iExtra: Int = 0, any: Any? = null, callback: SingleCallback? = null): Flow<ActionEvents> {
        val delayEvent = "delay_event_$iAction"
        _registerAction(iAction, bUiThread, true, false, false, listOf(delayEvent), callback)
        hThread.mHandler.postDelayed((Runnable { this.event(delayEvent, bSuccess, iExtra, any) }), iTime)
        return this
    }

    @JvmOverloads
    fun registerAction(iAction: Int, bUiThread: Boolean = false, events: List<ActionEvents>, singleCallback: SingleCallback? = null): Flow<ActionEvents> {
        _registerAction(iAction, bUiThread, false, false, false, events, singleCallback)
        return this
    }

    @JvmOverloads
    fun waitForEvents(iAction: Int, bUiThread: Boolean = false, events: List<ActionEvents>): Flow<ActionEvents> {
        _registerAction(iAction, bUiThread, true, false, false, events)
        return this
    }

    fun registerActionSequence(iAction: Int, bUiThread: Boolean, events: List<ActionEvents>): Flow<ActionEvents> {
        _registerAction(iAction, bUiThread, false, true, false, events)
        return this
    }

    fun cancelAction(iAction: Int) {

        for (i in listActions.indices) { // remove action if it already exists
            if (listActions[i].iAction == iAction) {
                _cancelAction(i)
                loge("CANCEL: Action($iAction), removed  ")
                break
            }
        }
    }

    private fun _cancelAction(index: Int){
        val action = listActions[index]
        action.recycle()
        listActions.removeAt(index)


    }

    private fun _registerAction(iAction: Int, bUiThread: Boolean, bRunOnce: Boolean, bSequence: Boolean, bRepeat: Boolean, events: List<*>, actionCallback: SingleCallback? = null) {
        cancelAction(iAction) // to stop duplication, remove if the action already exists
        val actionFlags = setActionFlags(runOnUI = bUiThread, runOnce = bRunOnce, eventSequence = bSequence, repeatAction = bRepeat)
        val aAction = _Action(iAction, actionFlags, events, actionCallback)
        listActions.add(aAction)

        val buf = StringBuffer(400)
        for (i in events.indices) {
            buf.append(events[i].toString() + ", ")
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
            for (i in 0 until listActions.size) {
                val iAction = listActions[i].iAction
                val result = listActions[i].onEvent(sEvent, bSuccess, iExtra, obj)
                if (result.first && result.second) {
                    _cancelAction(i)
                    loge("REMOVING: Action($iAction, runOnce) as its executed")
                }
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

    // CLASS for event Pool
    class Event<ActionEvents> private constructor() { // CONSTRUCTOR - Private

        var obj: Any? = null
        var extra = 0
        var event: ActionEvents? = null
        internal  var status :EventStatus = EventStatus.WAITING // WAITING - waiting not fired yet, SUCCESS - fired with success, FAILURE - fired with failure
        // Variable for pool
        private var next: Event<ActionEvents>? = null // Reference to next object

        fun isFired() = status == EventStatus.SUCCESS

        // METHOD object added to the pool, to be reused
        internal fun recycle() {
            synchronized(sPoolSync) {
                if (sPoolSize < MAX_POOL_SIZE) {
                    next = sPool as Event<ActionEvents>?
                    sPool = this
                    sPoolSize++
                }
            }
        }

        fun resetEvent() {
            obj = null
            extra = 0
            status = EventStatus.WAITING
        }

        companion object {
            // EVENTS for self use
            private var sPool: Any? = null
            private var sPoolSize = 0
            private const val MAX_POOL_SIZE = 50
            private val sPoolSync = Any() // The lock used for synchronization
            // METHOD get pool object only through this method, so no direct allocation are made
            fun <ExternalEvents> obtain(sId: ExternalEvents?): Event<*> {
                synchronized(sPoolSync) {
                    if (sPool != null) {
                        val e = sPool as Event<ExternalEvents>
                        e.event = sId
                        e.status = EventStatus.WAITING
                        e.obj = null
                        e.extra = 0
                        sPool = e.next
                        e.next = null
                        sPoolSize--
                        return e
                    }
                    val eve = Event<ExternalEvents>()
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
    }

    private inner class _Action(
            internal val iAction: Int,
            private var actionFlags: Int = 0,
            events: List<*>,
            private var singleCallback: SingleCallback? = null
    ) : Action() {
        internal var resultType: ResultType = ResultType.RESULT_CHANGE
        private var iEventCount: Int = events.size          // How many event are for this action code to be triggered

        init {
            for (i in 0 until iEventCount) {
                listEvents.add(Event.obtain(events[i])) // get events from events pool
            }
        }

        internal fun getFlag(flag: Int) = Companion.getFlag(actionFlags, flag)
        internal fun setFlag(flag: Int) {
            Companion.setFlag(actionFlags, flag, true)
        }

        internal fun clearFlag(flag: Int) {
            Companion.setFlag(actionFlags, flag, false)
        }

        internal fun getEventsList() = listEvents
        internal fun callback(bSuccess: Boolean): Boolean {
            if (singleCallback != null) {
                singleCallback?.invoke(bSuccess)
                return true
            }
            return false
        }

        internal fun getEventData(events: ActionEvents): Any? {
            return listEvents.find { it.event == events }?.obj
        }


        // METHOD recycles events and clears actions
        internal fun recycle() {
            singleCallback = null
            iEventCount = 0
            for (event in listEvents) {
                event.recycle()
            }
            listEvents = ArrayList()
        }

        // METHOD searches all actions, if any associated with this event
        fun onEvent(sEvent: Any, bResult: Boolean, iExtra: Int, obj: Any?): Pair<Boolean, Boolean> {
            var iFiredCount = 0 // How many have been fired
            var iSuccess = 0 // How many has been successful
            var bEventFound = false
            var bActionFired = false
            for (i in 0 until iEventCount) {
                val event = listEvents[i]
                if (sEvent == event.event) { // If event is found in this event list
                    bEventFound = true
                    event.obj = obj
                    event.extra = iExtra
                    event.status = if (bResult) EventStatus.SUCCESS else EventStatus.FAILURE
                } else if (getFlag(FLAG_SEQUENCE) && event.status == EventStatus.WAITING) { // if its a Sequence action, no event should be empty before current event
                    if (i != 0) {
                        listEvents[i - 1].status = EventStatus.WAITING
                    } // reset last one, so they are always in sequence
                    break
                }
                when (event.status) {
                    EventStatus.SUCCESS -> {
                        iSuccess++
                        iFiredCount++ // Add to fired event regard less of success or failure
                    }
                    EventStatus.FAILURE -> iFiredCount++
                }
                if (bEventFound && getFlag(FLAG_SEQUENCE)) break
            }
            if (bEventFound) { // if event was found in this Action
                logw("ACTION: $iAction Event: $sEvent fired { Total $iEventCount  Fired: $iFiredCount  Success: $iSuccess }")
                if (resultType == ResultType.EVENT_UPDATE) { // if this action is launched on every event update
                    executeAction(bResult, iExtra)
                } else if (iFiredCount == iEventCount) { // if all events for action has been fired
                    val bSuccess = iSuccess == iEventCount // all events registered success
                    val iCurStatus = if (bSuccess) EventStatus.SUCCESS else EventStatus.FAILURE
                    when (resultType) {
                        ResultType.RESULT_CHANGE -> if (iCurStatus != iLastStatus) { // If there is a change in action status only then run code
                            bActionFired = true
                            iLastStatus = iCurStatus
                            executeAction(bSuccess, iSuccess)
                        }
                        ResultType.RESULT_UPDATE -> if (bSuccess) {
                            bActionFired = true
                            executeAction(bSuccess, iSuccess)
                        }
                    }
                    if (getFlag(FLAG_RUNONCE)) {
                        recycle()
                    } // Recycle if its flagged for it
                }
            }
            return Pair(bActionFired, getFlag(FLAG_RUNONCE))
        }

        // METHOD executes action code on appropriate thread
        private fun executeAction(bSuccess: Boolean, iExtra: Int) {
            logw("ACTION: $iAction Executed with $bSuccess ")

            if (getFlag(FLAG_RUNonUI)) {
                hThread.runOnUI(iAction, bSuccess, iExtra, this as Flow<*>.Action)
            } else {
                hThread.run(iAction, bSuccess, iExtra, this)
            }
        }
    }

    // CLASS for events for action, when all events occur action is triggered
    inner open class Action() {
        protected var listEvents: MutableList<Event<*>> = ArrayList() // List to store Flow.Events needed for this action
        protected var iLastStatus = EventStatus.WAITING       // Event set status as a whole, waiting, success, non success

        // returns first event that has not been fired or fired with false
        fun getWaitingEvent() = listEvents.firstOrNull { !it.isFired() }?.event

        fun isWaitingFor(event: Any) = getWaitingEvent() == event

        fun reset() {
            iLastStatus = EventStatus.WAITING
            for (event in listEvents) {
                event.resetEvent()
            }
        }
    }

    // CLASS for thread handler
    internal inner class HThread internal constructor() : Handler.Callback {
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

        fun run(iStep: Int, bSuccess: Boolean, iExtra: Int, obj: Any?) {
            if (bRunning) {
                val msg = Message.obtain()
                msg.what = iStep
                msg.arg1 = iExtra
                msg.arg2 = if (bSuccess) ACTION_SUCCESS else ACTION_FAIL
                msg.obj = obj
                mHandler.sendMessage(msg)
            }
        }

        fun runOnUI(iStep: Int, bSuccess: Boolean, iExtra: Int, obj: Any?) {
            if (bRunning) {
                val msg = Message.obtain()
                msg.what = iStep
                msg.arg1 = iExtra
                msg.arg2 = if (bSuccess) ACTION_SUCCESS else ACTION_FAIL
                msg.obj = obj
                mUiHandler.sendMessage(msg)
            }
        }

        // METHOD MESSAGE HANDLER
        override fun handleMessage(msg: Message): Boolean {
            val action = msg.obj as Flow<ActionEvents>._Action

            if(action.iAction == 2)
                logw("ACTION: $2 Executed with  ")

            if (action.getFlag(FLAG_REPEAT)) { // If its a repeat action, we have to post it again
                val event = action.getEventsList()[0] // get delay event for data
                hThread.mHandler.postDelayed((Runnable { action.onEvent(event.event!!, !event.isFired(), event.extra++, event.obj) }), event.obj as Long)
                // posting action.onEvent() to repeat action only, not Flow.event(), to keep it local and filling queue for fast repeating actions
            }

            if (!action.callback(msg.arg2 == ACTION_SUCCESS)) { // if there is no specific callback for action, call generic call back
                Log.d("flow", "code callback: $globalCallback")
                globalCallback?.onAction(msg.what, msg.arg2 == ACTION_SUCCESS, msg.arg1, msg.obj)
            }

            return true
        }


        fun stop() {
            mHandler.removeCallbacksAndMessages(null)
            mUiHandler.removeCallbacksAndMessages(null)
            mHandler.looper.quit()
        }

        init {
            val ht = HandlerThread("BGThread_" + Integer.toString(++iThreadCount))
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
