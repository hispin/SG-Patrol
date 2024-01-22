package com.sensoguard.detectsensor.services


import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.global.CHECK_USB_CONN_SW
import com.sensoguard.detectsensor.global.STOP_GENERAL_TIMER
import java.util.*

class TimerGeneralService : ParentService() {

    //set if the timer is repeated
    var notificationTimer: Timer? = null


    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        notificationTimer?.cancel()
        notificationTimer = null
        stopSelf()
    }


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


        //if the timer is is already active
        try {
            notificationTimer?.cancel()
            notificationTimer = null
            notificationTask?.cancel()
        } catch (ex: Exception) {

        }

        startNotifTask()

        notificationTimer = Timer()
        notificationTimer?.schedule(notificationTask, 3 * 1000L, 3 * 1000L)

        return START_NOT_STICKY
    }

    private var notificationTask: TimerTask? = null
    fun startNotifTask() {
        notificationTask = object : TimerTask() {
            override fun run() {
                //check SW usb connection
                sendBroadcast(Intent(CHECK_USB_CONN_SW))
                //Log.d("testGeneralTimer","ok")
                //showShortToast(this@TimerGeneralService,"generalTimer")
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
    }

    private fun setFilter() {
        val filter = IntentFilter(STOP_GENERAL_TIMER)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, AppCompatActivity.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(usbReceiver, filter)
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, inn: Intent) {
            if (inn.action == STOP_GENERAL_TIMER) {
                notificationTimer?.cancel()
                notificationTimer = null
                //sendBroadcast(Intent(MAX_TIMER_RESPONSE))
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