package com.stryde.library.device


import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Log
import com.stryde.library.device.Flow.Event.Companion.FAILURE
import com.stryde.library.device.Flow.Event.Companion.SUCCESS
import java.util.*

// Version 2.4.0
// Added <Type> based events
// get events for action, get action error methods
// removed UiFlow to separate class
// added ui listener LOAD_LAYOUT
// Added runType for events RESULT_CHANGE, RESULT_UPDATE, EVENT_UPDATE
// Added Help examples
// ## EXAMPLES ##
//Flow<ExternalEvents>flow = new Flow(flowCode)
// Example 1: flow.registerEvents(1, "email_entered", "password_entered", "verify_code_entered" ) action 1 gets called when all those events occur
//          : flow.onEvent("email_entered", true, extra(opt), object(opt))  is trigger for the registered event "email_entered",
//          :  when all three events are triggered with flow.onEvent(...., true), action 1 is executed with bSuccess = true
//          :  after 3 event true(s), if one onEvent(...., false) sends false, action 1 will be executed with bSuccess = false
//          :  now action 1 will only trigger again when all onEvents(...., true) are true, i.e the events which sent false, send true again
// Example 2: flow.registerUiEvent(2, spinnerView, Flow.Event.SPINNER_ITEM_SELECT) action two gets called when ever a spinner item is selected
// Example 3: flow.run(3, true(opt), extra(opt), object(opt)) runs an action on background thread, same as registering for one event and triggering that event
// Example 4: flow.runOnUi(4, true(opt), extra(opt), object(opt)) runs code on Ui thread
// Example 5: flow.runDelayed(5, true(opt), extra(opt), 4000) runs delayed code
// Example 6: flow.runDelayedOnUi(6, true(opt), extra(opt), 4000) runs delayed code on Ui thread
// Flow.Code flowCode = new Flow.Code(){
//  @override public void onAction(int iAction, boolean bSuccess, int iExtra, Object data){
//  switch(iAction){
//      case 1:  ...... break;   // this code will run in first example when all events are triggered as true
//      case 2: ...... break;    // this code will run when a spinner item is selected
//      case 3: ....... break;   // this will run when ever run(3) is called
//      case 4: ........ break;  // this will run on ui thread whenever runOnUi(4) is called
//      case 5: ........ break;  // this will run on delayed by 4 secs
// }  }
// Example 7: new Flow().runDelayed(2000).execute(() -{})
// Example 8: new Flow().runRepeat(500).execute(() -{})
open class Flow<ExternalEvents> @JvmOverloads constructor(codeCallback: Execute? = null) : LifecycleObserver {
    private var bRunning = true
    @JvmField
    protected var hThread: HThread
    private var listActions: MutableList<Action>? = ArrayList() // List of registered actions
    private var code: ExecuteInterface? = null // Call back for onAction to be executed

    // INTERFACES for code execution and keyboard listener
    interface ExecuteInterface

    interface Run : ExecuteInterface {
        fun onAction()
    }

    interface Execute : ExecuteInterface {
        fun onAction(iAction: Int, bSuccess: Boolean, iExtra: Int, data: Any?)
    }

    fun actionCode(codeCallback: ExecuteInterface) {
        code = codeCallback
    }

    fun execute(CodeOrRunCallback: ExecuteInterface) {
        code = CodeOrRunCallback
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
            for (i in listActions!!.indices) {
                listActions!![i].recycle()
            }
            hThread.mHandler.removeCallbacksAndMessages(null)
            hThread.mUiHandler.removeCallbacksAndMessages(null)
            hThread.stop()
            listActions = null
            Event.releasePool()
            bRunning = false
        } catch (e: Exception) {
        }
    }

    // METHOD sets the type of action run RESULT_CHANGE, RESULT_UPDATE, EVENT_UPDATE
    fun runType(iType: Int) {
        if (listActions!!.size > 0) listActions!![listActions!!.size - 1].iRunType = iType
    }

    // METHODS run an action
    fun run(bRunOnUi: Boolean): Flow<ExternalEvents> {
        run(-1, true)
        return this
    }

    fun run(iAction: Int):Flow<ExternalEvents>{
        run(iAction, false)
        return this
    }

    fun run(iAction: Int, bRunOnUi: Boolean):Flow<ExternalEvents>{
        run(iAction, bRunOnUi, true, 0, null)
        return this
    }

    fun run(iAction: Int, iExtra: Int, obj: Any?):Flow<ExternalEvents>{
        run(iAction, false, true, iExtra, obj)
        return this
    }

    fun run(iAction: Int, bRunOnUi: Boolean, bSuccess: Boolean, iExtra: Int, obj: Any?):Flow<ExternalEvents>{
        if (bRunOnUi) hThread.runOnUI(iAction, bSuccess, iExtra, obj) else hThread.run(iAction, bSuccess, iExtra, obj)
        return this
    }

    fun runRepeat(iDelay: Long):Flow<ExternalEvents>{
        hThread.runRepeat(false, -1, true, 0, iDelay)
        return this
    }

    fun runRepeat(iAction: Int, iDelay: Long):Flow<ExternalEvents>{
        hThread.runRepeat(false, iAction, true, 0, iDelay)
        return this
    }

    fun runRepeat(iAction: Int, bRunOnUi: Boolean, iDelay: Long):Flow<ExternalEvents>{
        hThread.runRepeat(bRunOnUi, iAction, true, 0, iDelay)
        return this
    }

    fun runRepeat(iAction: Int, bSuccess: Boolean, iExtra: Int, iDelay: Long):Flow<ExternalEvents>{
        hThread.runRepeat(false, iAction, bSuccess, iExtra, iDelay)
        return this
    }

    fun runRepeat(iAction: Int, bRunOnUi: Boolean, bSuccess: Boolean, iExtra: Int, iDelay: Long):Flow<ExternalEvents>{
        hThread.runRepeat(bRunOnUi, iAction, bSuccess, iExtra, iDelay)
        return this
    }

    // METHODS run action delayed
    fun runDelayed(iTime: Long):Flow<ExternalEvents>{
        runDelayed2(-1, true, 0, null, iTime)
        return this
    }

    fun runDelayed(iAction: Int, iTime: Long):Flow<ExternalEvents>{
        runDelayed2(iAction, true, 0, null, iTime)
        return this
    }

    fun runDelayed(iAction: Int, bRunOnUi: Boolean, iTime: Long):Flow<ExternalEvents>{
        if (bRunOnUi) runDelayedOnUI(iAction, true, 0, null, iTime) else runDelayed2(iAction, true, 0, null, iTime)
        return this
    }

    fun runDelayed(iAction: Int, bSuccess: Boolean, iExtra: Int, `object`: Any?, iTime: Long):Flow<ExternalEvents>{
        runDelayed2(iAction, bSuccess, iExtra, `object`, iTime)
        return this
    }

    fun runDelayed(bRunOnUi: Boolean, bSuccess: Boolean, iExtra: Int, `object`: Any?, iTime: Long):Flow<ExternalEvents>{
        return runDelayed(-1, bRunOnUi, bSuccess, iExtra, `object`, iTime)
    }

    fun runDelayed(iAction: Int, bRunOnUi: Boolean, bSuccess: Boolean, iExtra: Int, `object`: Any?, iTime: Long):Flow<ExternalEvents>{
        if (bRunOnUi) runDelayedOnUI(iAction, bSuccess, iExtra, `object`, iTime) else runDelayed2(iAction, bSuccess, iExtra, `object`, iTime)
        return this
    }

    private fun runDelayedOnUI(iAction: Int, bSuccess: Boolean, iExtra: Int, `object`: Any?, iTime: Long) {
        val msg = Message.obtain()
        msg.what = iAction
        msg.arg1 = iExtra
        msg.arg2 = if (bSuccess) 1 else 0
        msg.obj = `object`
        hThread.mUiHandler.removeMessages(iAction) // Remove any pending messages in queue
        hThread.mUiHandler.sendMessageDelayed(msg, iTime)
    }

    private fun runDelayed2(iAction: Int, bSuccess: Boolean, iExtra: Int, `object`: Any?, iTime: Long) {
        val msg = Message.obtain()
        msg.what = iAction
        msg.arg1 = iExtra
        msg.arg2 = if (bSuccess) 1 else 0
        msg.obj = `object`
        hThread.mHandler.removeMessages(iAction) // Remove any pending messages in queue
        hThread.mHandler.sendMessageDelayed(msg, iTime)
    }

    // METHODS events registration
    fun registerAction(iAction: Int, events: Array<ExternalEvents>):Flow<ExternalEvents>{
        registerAction(iAction, false, false, false, events)
        return this
    }

    fun waitForEvents(iAction: Int, events: Array<ExternalEvents>):Flow<ExternalEvents>{
        registerAction(iAction, false, true, false, events)
        return this
    }

    fun waitForEvents(iAction: Int, bRunOnUI: Boolean, events: Array<ExternalEvents>):Flow<ExternalEvents>{
        registerAction(iAction, bRunOnUI, true, false, events)
        return this
    }

    fun registerAction(iAction: Int, bRunOnUI: Boolean, events: Array<ExternalEvents>):Flow<ExternalEvents>{
        registerAction(iAction, bRunOnUI, false, false, events)
        return this
    }

    fun registerEventSequence(iAction: Int, bRunOnUI: Boolean, events: Array<ExternalEvents>):Flow<ExternalEvents>{
        registerAction(iAction, bRunOnUI, false, true, events)
        return this
    }

    fun getEventsForAction(iAction: Int) = listActions?.first { it.iAction == iAction }?.getEventsList()

    // Returns first found event that has not been fired with true
    fun getActionError(iAction: Int) = getEventsForAction(iAction)?.first {!it.isFired() }?.sEvent
    fun getActionEventWaiting(iAction: Int) = getEventsForAction(iAction)?.first {!it.isFired() }?.sEvent


    private fun registerAction(iAction: Int, bRunOnUI: Boolean, bRunOnce: Boolean, bSequence: Boolean, events: Array<ExternalEvents>) {
        unRegisterAction(iAction) // to stop duplication, remove if the action already exists
        val aAction = Action(iAction, events)
        aAction.bRunOnUI = bRunOnUI
        aAction.bFireOnce = bRunOnce // fired only once, then removed
        aAction.bSequence = bSequence // events have to be in sequence for the action to be fired
        listActions!!.add(aAction)
        val buf = StringBuffer(400)
        for (i in events.indices) {
            buf.append(events[i].toString() + ", ")
        }
        log("ACTION: $iAction registered  EVENTS = { $buf}")
    }

    fun unRegisterAction(iAction: Int) {
        for (i in listActions!!.indices) { // remove action if it already exists
            if (listActions!![i].iAction == iAction) {
                listActions!!.removeAt(i)
                log("ACTION: $iAction exists, removing it  ")
                break
            }
        }
    }

    // METHODS to send event
    @JvmOverloads
    fun event(sEvent: ExternalEvents, bSuccess: Boolean = true, iExtra: Int = 0, obj: Any? = null) {
        if (!bRunning) return
        log("EVENT:  $sEvent $bSuccess")
        val iSize = listActions!!.size
        var bActionFired = false
        for (i in 0 until iSize) {
            bActionFired = listActions!![i].onEvent(sEvent, bSuccess, iExtra, obj)
            if (bActionFired && listActions!![i].bFireOnce) {
                listActions!!.removeAt(i)
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
    class Event<ExternalEvents> private constructor() { // CONSTRUCTOR - Private

        var obj: Any? = null
        var iExtra = 0
        var sEvent: ExternalEvents? = null
        var iStatus = WAITING // WAITING - waiting not fired yet, SUCCESS - fired with success, FAILURE - fired with failure
        // Variable for pool
        private var next  : Event<ExternalEvents>? = null // Reference to next object

        fun isFired() = iStatus == SUCCESS

        // METHOD object added to the pool, to be reused
        fun recycle() {
            synchronized(sPoolSync) {
                if (sPoolSize < MAX_POOL_SIZE) {
                    next = sPool as Event<ExternalEvents>?
                    sPool = this
                    sPoolSize++
                }
            }
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
                        val e = sPool as  Event<ExternalEvents>
                        e.sEvent = sId
                        e.iStatus = WAITING
                        e.obj = null
                        e.iExtra = 0
                        sPool = e.next
                        e.next = null
                        sPoolSize--
                        return e
                    }
                    val eve = Event<ExternalEvents>()
                    eve.sEvent = sId
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
    protected inner class Action {
        var iAction : Int // Code step to execute for this action
        private var iEventCount : Int // How many event are for this action code to be triggered
        var bSequence = false // Only trigger when events occur in right order
        //   private boolean bEventFound;
        var bRunOnUI = false // Code run on Background / UI thread
        var iRunType = RESULT_CHANGE // when this action is run,
        var bFireOnce = false // Clear Action once fired, used for wait action
        private var iLastStatus = Event.WAITING // Event set status as a whole, waiting, success, non success
        private var listEvents: MutableList<Event<ExternalEvents>>? = ArrayList() // List to store events needed for this action

        fun getEventsList() = listEvents

        // CONSTRUCTOR
        constructor(iCodeStep: Int, events: Array<ExternalEvents>) {
            bSequence = false
            iAction = iCodeStep
            iEventCount = events.size
            for (i in 0 until iEventCount) {
                listEvents!!.add(Event.obtain(events[i])) // get events from events pool
            }
        }

        constructor(iCodeStep: Int, events: Array<ExternalEvents?>, bOrder: Boolean) {
            bSequence = bOrder
            iAction = iCodeStep
            iEventCount = events.size
            for (i in 0 until iEventCount) {
                listEvents!!.add(Event.obtain(events[i])) // get events from events pool
            }
        }

        // METHOD recycles events and clears actions
        fun recycle() {
            val iSize = listEvents!!.size
            for (i in 0 until iSize) {
                listEvents!![i]!!.recycle()
            }
            listEvents = null
        }

        // METHOD searches all actions, if any associated with this event
        fun onEvent(sEvent: ExternalEvents, bResult: Boolean, iExtra: Int, obj: Any?): Boolean {
            var iFiredCount = 0 // How many have been fired
            var iSuccess = 0 // How many has been successful
            var bEventFound = false
            var bActionFired = false
            for (i in 0 until iEventCount) {
                val event = listEvents!![i]
                if (sEvent == event!!.sEvent) { // If event is found in this event list
                    bEventFound = true
                    event.obj = obj
                    event.iExtra = iExtra
                    event.iStatus = if (bResult) SUCCESS else Event.FAILURE
                } else if (bSequence && event.iStatus == Event.WAITING) { // if its a Sequence action, no event should be empty before current event
                    if (i != 0) {
                        listEvents!![i - 1]!!.iStatus = Event.WAITING
                    } // reset last one, so they are always in sequence
                    break
                }
                when (event.iStatus) {
                    SUCCESS -> {
                        iSuccess++
                        iFiredCount++ // Add to fired event regard less of success or failure
                    }
                    Event.FAILURE -> iFiredCount++
                }
                if (bEventFound && bSequence) break
            }
            if (bEventFound) { // if event was found in this Action
                logw("ACTION: $iAction Event: $sEvent fired { Total $iEventCount  Fired: $iFiredCount  Success: $iSuccess }")
                if (iRunType == EVENT_UPDATE) { // if this action is launched on every event update
                    executeAction(bResult, iExtra)
                } else if (iFiredCount == iEventCount) { // if all events for action has been fired
                    val bSuccess = iSuccess == iEventCount // all events registered success
                    val iCurStatus = if (bSuccess) SUCCESS else FAILURE
                    when (iRunType) {
                        RESULT_CHANGE -> if (iCurStatus != iLastStatus) { // If there is a change in action status only then run code
                            bActionFired = true
                            iLastStatus = iCurStatus
                            executeAction(bSuccess, iSuccess)
                        }
                        RESULT_UPDATE -> if (bSuccess) {
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
            logw("ACTION:$iAction fired")
            if (bRunOnUI) {
                hThread.runOnUI(iAction, bSuccess, iExtra, listEvents)
            } else {
                hThread.run(iAction, bSuccess, iExtra, listEvents)
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
                if (code != null) {
                    if (code is Execute) (code as Execute).onAction(msg.what, getFlag(msg.arg2, FLAG_SUCCESS), getExtraInt(msg.arg2), msg.obj) else (code as Run).onAction()
                }
            } else {
                if (code != null) {
                    if (code is Execute) (code as Execute).onAction(msg.what, msg.arg2 == 1, msg.arg1, msg.obj) else (code as Run).onAction()
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

    companion object {
        const val RESULT_CHANGE = 0 // called once all events are fired, and when events AND result change
        const val RESULT_UPDATE = 1 // called once all events are fired with true, and every time any event updates as long as events AND is true
        const val EVENT_UPDATE = 2 // called every time an event is fired or changed
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

    init {
        code = codeCallback
        hThread = HThread()
    }
}