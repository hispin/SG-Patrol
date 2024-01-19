package com.sensoguard.detectsensor.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.global.ACTION_INTERVAL
import com.sensoguard.detectsensor.global.COMMAND_TYPE
import com.sensoguard.detectsensor.global.IS_REPEATED
import com.sensoguard.detectsensor.global.MAX_TIMEOUT
import com.sensoguard.detectsensor.global.MAX_TIMER_RESPONSE
import com.sensoguard.detectsensor.global.STOP_TIMER
import com.sensoguard.detectsensor.global.TIMER_VALUE
import com.sensoguard.detectsensor.global.showToast
import java.util.*

class TimerService : ParentService() {

    //set if the timer is repeated
    private var isRepeated: Boolean? = false
    private var commandType: String? = "default"
    var notificationTimer: Timer? = null
    var timerValue: Float = 1f
    var maxTimeout = 60
    var counter = 0f
    //var isContinue = false


    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()
        startSysForeGround()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        setFilter()

        commandType = intent?.getStringExtra(COMMAND_TYPE)
        isRepeated = intent?.getBooleanExtra(IS_REPEATED, false)
        timerValue = intent?.getFloatExtra(TIMER_VALUE, 1f)!!
        maxTimeout = intent.getIntExtra(MAX_TIMEOUT, -1)

        //showToast(this,"maxTimeout="+maxTimeout+"timerValue="+timerValue)

        //if the timer is is already active
        try {
            notificationTimer?.cancel()
            notificationTimer?.purge()
            notificationTimer = null
            counter = 0f
        } catch (ex: Exception) {
            showToast(this, "exception")
        }

        startNotifTask()

        notificationTimer = Timer()
        //check if the timer is will repeated
        if (isRepeated == true) {
            notificationTimer?.schedule(
                notificationTask,
                (timerValue * 1000L).toLong(),
                (timerValue * 1000L).toLong()
            )
        } else {
            notificationTimer?.schedule(notificationTask, (timerValue * 1000L).toLong())
        }

        return START_NOT_STICKY
    }

    private var notificationTask: TimerTask? = null
    fun startNotifTask() {
        notificationTask = object : TimerTask() {
            override fun run() {
                if (isRepeated != null && !isRepeated!!) {
                    //release the timer to enable the next timer
                    notificationTimer?.cancel()
                    notificationTimer = null
                    sendBroadcast(Intent(ACTION_INTERVAL))
                    stopSelf()
                    return
                }

                //if defined max timer
                if (maxTimeout != -1) {
                    counter += timerValue
                    if (counter < maxTimeout) {
                        val intent = Intent(ACTION_INTERVAL)
                        intent.putExtra(COMMAND_TYPE, resources.getString(R.string.set_ref_timer))
                        sendBroadcast(intent)
                    } else {
                        //reach to max time out
                        notificationTimer?.cancel()
                        notificationTimer = null
                        sendBroadcast(Intent(MAX_TIMER_RESPONSE))
                        stopSelf()
                    }
                } else {
                    val intent = Intent(ACTION_INTERVAL)
                    intent.putExtra(COMMAND_TYPE, resources.getString(R.string.set_ref_timer))
                    sendBroadcast(intent)
                }

            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
    }

    private fun setFilter() {
        val filter = IntentFilter(STOP_TIMER)
        filter.addAction(STOP_TIMER)
        registerReceiver(usbReceiver, filter)
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, inn: Intent) {
            if (inn.action == STOP_TIMER) {
                notificationTimer?.cancel()
                notificationTimer = null
                sendBroadcast(Intent(MAX_TIMER_RESPONSE))
                stopSelf()
            }
        }
    }


    //The system allows apps to call Context.startForegroundService() even while the app is in the background. However, the app must call that service's startForeground() method within five seconds after the service is created
    private fun startSysForeGround() {
        fun getNotificationIcon(): Int {
            val useWhiteIcon =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
            return if (useWhiteIcon) R.drawable.ic_app_notification else R.mipmap.ic_launcher
        }
        if (Build.VERSION.SDK_INT >= 26) {
            val CHANNEL_ID = "my_channel_01"
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val `object` = getSystemService(Context.NOTIFICATION_SERVICE)
            if (`object` != null && `object` is NotificationManager) {
                `object`.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentText("SG-Patrol is running")
                .setSmallIcon(getNotificationIcon())
                .build()

            startForeground(1, notification)
        }
    }


}