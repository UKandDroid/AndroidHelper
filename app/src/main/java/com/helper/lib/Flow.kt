package com.helper.lib;

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.text.BoringLayout
import android.util.Log
import java.lang.ref.WeakReference
import java.util.*
typealias SingleCallback = (bSuccess: BoringLayout) -> Unit

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
    private var bRunning = true
    private var hThread: HThread
    private var listActions: MutableList<Action> = ArrayList() // List of registered actions
    private var code: WeakReference<FlowCode?>? = null // Call back for onAction to be executed

    // INTERFACE for code execution
    interface FlowCode  {
        fun onAction() {}
        fun onAction(iAction: Int, bSuccess: Boolean, iExtra: Int, data: Any?) {}
    }

    init {
        code = WeakReference(codeCallback)
        hThread = HThread()
    }

    fun execute(flowCode: FlowCode) {
        code = WeakReference(flowCode)
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
        code = null
        try {
            for (i in listActions.indices) {
                listActions[i].recycle()
            }
            hThread.mHandler.removeCallbacksAndMessages(null)
            hThread.mUiHandler.removeCallbacksAndMessages(null)
            hThread.stop()
            listActions = ArrayList()
            Event.releasePool()
            bRunning = false
        } catch (e: Exception) { }
    }

    // METHOD sets the type of action callback
    // RESULT_CHANGE = When result changes from false to true or true to false
    // RESULT_UPDATE = when result updates means all events are fired a
    // EVENT_UPDATE
    fun actionCallbackType(type: ResultType) {
        if (listActions.size > 0) listActions[listActions.size - 1].resultType = type
    }

    fun getAction(iAction: Int) = listActions.first { it.iAction == iAction }
    fun getActionEvents(iAction: Int) = getAction(iAction).getEventsList()
    fun getActionWaitingEvent(iAction: Int) = getAction(iAction).getErrorOrWaitingEvent() // // Returns first found event that is stopping the action from triggering
    fun resetAction(iAction: Int) { getAction(iAction).reset() } // Resets action by resetting all events to initial Waiting state

    @JvmOverloads
    fun run(iAction: Int = -1, bUiThread: Boolean = false, bSuccess: Boolean = true, iExtra: Int =0, obj: Any? = null) : Flow<ActionEvents>{
        if (bUiThread) hThread.runOnUI(iAction, bSuccess, iExtra, obj) else hThread.run(iAction, bSuccess, iExtra, obj)
        return this
    }

    @JvmOverloads
    fun runRepeat(iAction: Int = -1, bUiThread: Boolean = false, bSuccess: Boolean = true, iExtra: Int =0, iDelay: Long):Flow<ActionEvents>{
        hThread.runRepeat(bUiThread, iAction, bSuccess, iExtra, iDelay)
        return this
    }

    @JvmOverloads
    fun runDelayed(iAction: Int = -1, bUiThread: Boolean = false, bSuccess: Boolean = true, iExtra: Int =0, any: Any? = null, iTime: Long):Flow<ActionEvents>{
         _runDelayed(iAction, bUiThread, bSuccess, iExtra, any, iTime)
        return this
    }

    private fun _runDelayed(iAction: Int, bUiThread: Boolean = false, bSuccess: Boolean, iExtra: Int, any: Any?, iTime: Long) {
        val msg = Message.obtain()
        msg.what = iAction
        msg.arg1 = iExtra
        msg.arg2 = if (bSuccess) 1 else 0
        msg.obj = any
        val handler = if(bUiThread) hThread.mUiHandler else hThread.mHandler
        handler.removeMessages(iAction) // Remove any pending messages in queue
        handler.sendMessageDelayed(msg, iTime)
    }

    @JvmOverloads
    fun registerAction(iAction: Int, bUiThread: Boolean = false, events: Array<ActionEvents>, singleCallback: SingleCallback? = null):Flow<ActionEvents>{
        _registerAction(iAction, bUiThread, false, false, events, singleCallback)
        return this
    }

    @JvmOverloads
    fun waitForEvents(iAction: Int, bUiThread: Boolean = false, events: Array<ActionEvents>):Flow<ActionEvents>{
        _registerAction(iAction, bUiThread, true, false, events)
        return this
    }


    fun registerActionSequence(iAction: Int, bUiThread: Boolean, events: Array<ActionEvents>):Flow<ActionEvents>{
        _registerAction(iAction, bUiThread, false, true, events)
        return this
    }

    private fun _registerAction(iAction: Int, bUiThread: Boolean, bRunOnce: Boolean, bSequence: Boolean, events: Array<ActionEvents>, singleCallback: SingleCallback? = null) {
        unRegisterAction(iAction) // to stop duplication, remove if the action already exists
        val aAction = Action(iAction, events)
        aAction.bRunOnUI = bUiThread
        aAction.bFireOnce = bRunOnce // fired only once, then removed
        aAction.bSequence = bSequence // events have to be in sequence for the action to be fired
        listActions.add(aAction)
        val buf = StringBuffer(400)
        for (i in events.indices) {
            buf.append(events[i].toString() + ", ")
        }
        log("ACTION: $iAction registered  EVENTS = { $buf}")
    }

    fun unRegisterAction(iAction: Int) {
        for (i in listActions.indices) { // remove action if it already exists
            if (listActions[i].iAction == iAction) {
                listActions.removeAt(i)
                log("ACTION: $iAction exists, removing it  ")
                break
            }
        }
    }

    // METHODS to send event
    @JvmOverloads
    fun event(sEvent: ActionEvents, bSuccess: Boolean = true, iExtra: Int = 0, obj: Any? = null) {
        if (!bRunning) return
        log("EVENT:  $sEvent $bSuccess")
        val iSize = listActions.size
        var bActionFired = false
        for (i in 0 until iSize) {
            bActionFired = listActions[i].onEvent(sEvent, bSuccess, iExtra, obj)
            if (bActionFired && listActions[i].bFireOnce) {
                listActions.removeAt(i)
                log("Removing ACTION run once after been fired")
            }
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
        var status = WAITING // WAITING - waiting not fired yet, SUCCESS - fired with success, FAILURE - fired with failure
        // Variable for pool
        private var next  : Event<ActionEvents>? = null // Reference to next object

        fun isFired() = status == SUCCESS

        // METHOD object added to the pool, to be reused
        fun recycle() {
            synchronized(sPoolSync) {
                if (sPoolSize < MAX_POOL_SIZE) {
                    next = sPool as Event<ActionEvents>?
                    sPool = this
                    sPoolSize++
                }
            }
        }

        fun resetEvent(){
            obj = null
            extra = 0
            status = WAITING
        }

        companion object {
            // EVENTS for self use
            const val WAITING = 0
            const val SUCCESS = 1
            const val FAILURE = 2
            private var sPool: Any? = null
            private var sPoolSize = 0
            private const val MAX_POOL_SIZE = 50
            private val sPoolSync = Any() // The lock used for synchronization
            // METHOD get pool object only through this method, so no direct allocation are made
            fun <ExternalEvents> obtain(sId: ExternalEvents?): Event<ExternalEvents> {
                synchronized(sPoolSync) {
                    if (sPool != null) {
                        val e = sPool as Event<ExternalEvents>
                        e.event = sId
                        e.status = WAITING
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

    // CLASS for events for action, when all events occur action is triggered
    inner class Action {
        var iAction : Int // Code step to execute for this action
        private var iEventCount : Int // How many event are for this action code to be triggered
        var bSequence = false // Only trigger when events occur in right order
        //   private boolean bEventFound;
        var bRunOnUI = false // Code run on Background / UI thread
        var resultType : ResultType = ResultType.RESULT_CHANGE
        var bFireOnce = false // Clear Action once fired, used for wait action
        private var iLastStatus = Event.WAITING // Event set status as a whole, waiting, success, non success
        private var listEvents: MutableList<Event<ActionEvents>> = ArrayList() // List to store events needed for this action

        fun getEventsList() = listEvents

        // CONSTRUCTOR
        constructor(iAction: Int, events: Array<ActionEvents>) {
            bSequence = false
            this.iAction = iAction
            iEventCount = events.size
            for (i in 0 until iEventCount) {
                listEvents.add(Event.obtain(events[i])) // get events from events pool
            }
        }

        constructor(iAction: Int, events: Array<ActionEvents>, sequence: Boolean) {
            bSequence = sequence
            this.iAction = iAction
            iEventCount = events.size
            for (i in 0 until iEventCount) {
                listEvents.add(Event.obtain(events[i])) // get events from events pool
            }
        }

        fun getEventData(events: ActionEvents) : Any? {
            return listEvents.find { it.event == events }?.obj
        }
        // returns first event that has not been fired or fired with false
        fun getErrorOrWaitingEvent() = listEvents.firstOrNull{ !it.isFired() }?.event

        fun isWaitingFor(event: ActionEvents) = getErrorOrWaitingEvent() == event

        fun reset(){
            for (i in listEvents){
                i.resetEvent()
            }
        }

        // METHOD recycles events and clears actions
        fun recycle() {
            val iSize = listEvents.size
            for (i in 0 until iSize) {
                listEvents[i].recycle()
            }
            listEvents = ArrayList()
        }

        // METHOD searches all actions, if any associated with this event
        fun onEvent(sEvent: ActionEvents, bResult: Boolean, iExtra: Int, obj: Any?): Boolean {
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
                    event.status = if (bResult) Event.SUCCESS else Event.FAILURE
                } else if (bSequence && event.status == Event.WAITING) { // if its a Sequence action, no event should be empty before current event
                    if (i != 0) {
                        listEvents[i - 1].status = Event.WAITING
                    } // reset last one, so they are always in sequence
                    break
                }
                when (event.status) {
                    Event.SUCCESS -> {
                        iSuccess++
                        iFiredCount++ // Add to fired event regard less of success or failure
                    }
                    Event.FAILURE -> iFiredCount++
                }
                if (bEventFound && bSequence) break
            }
            if (bEventFound) { // if event was found in this Action
                logw("ACTION: $iAction Event: $sEvent fired { Total $iEventCount  Fired: $iFiredCount  Success: $iSuccess }")
                if (resultType == ResultType.EVENT_UPDATE) { // if this action is launched on every event update
                    executeAction(bResult, iExtra)
                } else if (iFiredCount == iEventCount) { // if all events for action has been fired
                    val bSuccess = iSuccess == iEventCount // all events registered success
                    val iCurStatus = if (bSuccess) Event.SUCCESS else Event.FAILURE
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
                    if (bFireOnce) {
                        recycle()
                    } // Recycle if its flagged for it
                }
            }
            return bActionFired
        }

        // METHOD executes action code on appropriate thread
        private fun executeAction(bSuccess: Boolean, iExtra: Int) {
            logw("ACTION:$iAction fired with $bSuccess ")
            if (bRunOnUI) {
                hThread.runOnUI(iAction, bSuccess, iExtra, this)
            } else {
                hThread.run(iAction, bSuccess, iExtra, this)
            }
        }
    }

    // CLASS for thread handler
    protected inner class HThread internal constructor() : Handler.Callback {
        val mHandler: Handler
        val mUiHandler: Handler
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
                msg.arg2 = if (bSuccess) 1 else 0
                msg.obj = obj
                mHandler.sendMessage(msg)
            }
        }

        fun runOnUI(iStep: Int, bSuccess: Boolean, iExtra: Int, obj: Any?) {
            if (bRunning) {
                val msg = Message.obtain()
                msg.what = iStep
                msg.arg1 = iExtra
                msg.arg2 = if (bSuccess) 1 else 0
                msg.obj = obj
                mUiHandler.sendMessage(msg)
            }
        }

        fun runRepeat(bRunOnUI: Boolean, iStep: Int, bSuccess: Boolean, iExtra: Int, iDelay: Long) {
            if (bRunning) {
                var flags = 0
                flags = setFlag(flags, FLAG_REPEAT, true)
                flags = setFlag(flags, FLAG_SUCCESS, bSuccess)
                flags = setFlag(flags, FLAG_RUNonUI, bRunOnUI)
                flags = addExtraInt(flags, iExtra)
                val msg = Message.obtain()
                msg.what = iStep
                msg.arg1 = iDelay.toInt() // As arg1 is integer
                msg.arg2 = flags
                if (bRunOnUI) {
                    mUiHandler.sendMessage(msg)
                } else {
                    mHandler.sendMessage(msg)
                }
            }
        }

        // METHOD MESSAGE HANDLER
        override fun handleMessage(msg: Message): Boolean {
            if (getFlag(msg.arg2, FLAG_REPEAT)) { // If its a repeat message, data is packed differently,
                val msg2 = Message.obtain()
                msg2.what = msg.what
                msg2.arg1 = msg.arg1
                msg2.arg2 = msg.arg2
                if (getFlag(msg.arg2, FLAG_RUNonUI)) {
                    mUiHandler.removeMessages(msg.what) // Clear any pending messages
                    mUiHandler.sendMessageDelayed(msg2, msg.arg1.toLong())
                } else {
                    mHandler.removeMessages(msg.what) // Clear any pending messages
                    mHandler.sendMessageDelayed(msg2, msg.arg1.toLong())
                }
                code?.get()?.onAction()
                code?.get()?.onAction(msg.what, getFlag(msg.arg2, FLAG_SUCCESS), getExtraInt(msg.arg2), msg.obj)

            } else {
                code?.get()?.onAction()
                code?.get()?.onAction(msg.what, msg.arg2 == 1, msg.arg1, msg.obj)
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

    enum class ResultType{
        RESULT_CHANGE, RESULT_UPDATE, EVENT_UPDATE
    }

    companion object {
        protected const val RESULT_CHANGE = 0 // called once all events are fired, and when events AND result change
        protected const val RESULT_UPDATE = 1 // called once all events are fired with true, and every time any event updates as long as events AND is true
        protected const val EVENT_UPDATE = 2 // called every time an event is fired or changed
        private var iThreadCount = 0
        private const val LOG_LEVEL = 4
        private const val LOG_TAG = "Flow"
        private const val FLAG_REPEAT = 0x00000004
        private const val FLAG_SUCCESS = 0x00000001
        private const val FLAG_RUNonUI = 0x00000002

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

        private fun setFlag(iValue: Int, iFlag: Int, bSet: Boolean): Int {
            return if (bSet) {
                iValue or iFlag
            } else {
                iValue and iFlag.inv()
            }
        }
    }


}