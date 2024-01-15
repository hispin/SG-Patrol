package com.sensoguard.detectsensor.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.ToggleButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.classes.AlarmSensor
import com.sensoguard.detectsensor.classes.GeneralItemMenu
import com.sensoguard.detectsensor.controler.ViewModelListener
import com.sensoguard.detectsensor.fragments.AlarmsLogFragment
import com.sensoguard.detectsensor.fragments.ConfigurationFragment
import com.sensoguard.detectsensor.fragments.MapmobFragment
import com.sensoguard.detectsensor.fragments.SensorsFragment
import com.sensoguard.detectsensor.global.ACTION_TOGGLE_TEST_MODE
import com.sensoguard.detectsensor.global.ALARM_FLICKERING_DURATION_DEFAULT_VALUE_SECONDS
import com.sensoguard.detectsensor.global.ALARM_FLICKERING_DURATION_KEY
import com.sensoguard.detectsensor.global.CREATE_ALARM_KEY
import com.sensoguard.detectsensor.global.CURRENT_ITEM_TOP_MENU_KEY
import com.sensoguard.detectsensor.global.IS_VIBRATE_WHEN_ALARM_KEY
import com.sensoguard.detectsensor.global.MAIN_MENU_NUM_ITEM
import com.sensoguard.detectsensor.global.MAP_SHOW_SATELLITE_VALUE
import com.sensoguard.detectsensor.global.MAP_SHOW_VIEW_TYPE_KEY
import com.sensoguard.detectsensor.global.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
import com.sensoguard.detectsensor.global.SELECTED_NOTIFICATION_SOUND_KEY
import com.sensoguard.detectsensor.global.STOP_ALARM_SOUND
import com.sensoguard.detectsensor.global.STOP_READ_DATA_KEY
import com.sensoguard.detectsensor.global.USB_DEVICES_EMPTY
import com.sensoguard.detectsensor.global.USB_DEVICES_NOT_EMPTY
import com.sensoguard.detectsensor.global.USB_DEVICE_CONNECT_STATUS
import com.sensoguard.detectsensor.global.UserSession
import com.sensoguard.detectsensor.global.getBooleanInPreference
import com.sensoguard.detectsensor.global.getIntInPreference
import com.sensoguard.detectsensor.global.getLongInPreference
import com.sensoguard.detectsensor.global.getStringInPreference
import com.sensoguard.detectsensor.global.setAppLanguage
import com.sensoguard.detectsensor.global.setBooleanInPreference
import com.sensoguard.detectsensor.global.setIntInPreference
import com.sensoguard.detectsensor.global.setLongInPreference
import com.sensoguard.detectsensor.global.setStringInPreference
import com.sensoguard.detectsensor.interfaces.OnFragmentListener
import com.sensoguard.detectsensor.services.ServiceConnectSensor
import com.sensoguard.detectsensor.services.ServiceHandleAlarms
import kotlinx.android.synthetic.main.activity_my_screens.*
import java.util.*


class MyScreensActivity : ParentActivity(), OnFragmentListener, Observer {


    override fun onDestroy() {
        super.onDestroy()
        clearScreenOn(this)
    }

    /**
     * clear screen on (enable sleep mode)
     */
    fun clearScreenOn(activity: Activity) {
        activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private var clickHundler: ClickHandler? = null

    // When requested, this adapter returns a DemoObjectFragment,
    // representing an object in the collection.
    private lateinit var collectionPagerAdapter: CollectionPagerAdapter
    private lateinit var viewPager: ViewPager
    private var currentItemTopMenu = 0
    private var togChangeStatus: ToggleButton? = null
    private var consMyActionBar: ConstraintLayout? = null


    val TAG = "MyScreensActivity"


    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        startTimerListener()

        setContentView(R.layout.activity_my_screens)
        Log.d("david", "hello")
        //store locally default values of configuration
        setConfigurationDefault()

        currentItemTopMenu = intent.getIntExtra(CURRENT_ITEM_TOP_MENU_KEY, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setLocationPermission()
        } else {
            init()
        }


    }

    //start listener to timer
    private fun startTimerListener() {
        //this?.let {
        ViewModelProviders.of(this).get(ViewModelListener::class.java)
            .startCurrentCalendarListener()?.observe(this, androidx.lifecycle.Observer { calendar ->

                //if there is no alarm in process then shut down the timer
                if (UserSession.instance.alarmSensors == null || UserSession.instance.alarmSensors?.isEmpty()!!) {
                    ViewModelProviders.of(this).get(ViewModelListener::class.java).shutDownTimer()
                    sendBroadcast(Intent(STOP_ALARM_SOUND))
                }

            })

    }

    override fun onResume() {
        super.onResume()
        if (isAnySensorAlarmNotTimeOut()) {
            startTimer()
        }
        configureActionBar()
    }

    //create timeout for reset sensor to regular icon and cancel the alarm icon
    private fun startTimer() {

        Log.d("testTimer", "start timer")
        ViewModelProviders.of(this).get(ViewModelListener::class.java).startTimer()


    }

    //check it there is any sensor alarm which is not time out
    private fun isAnySensorAlarmNotTimeOut(): Boolean {
        val iteratorList = UserSession.instance.alarmSensors?.listIterator()
        while (iteratorList != null && iteratorList.hasNext()) {
            val sensorItem = iteratorList.next()
            if (!isSensorAlarmTimeout(sensorItem)) {
                return true
            }
        }
        return false
    }

    //check if the alarm sensor is in duration
    private fun isSensorAlarmTimeout(alarmProcess: AlarmSensor?): Boolean {

        val timeout = getLongInPreference(
            this,
            ALARM_FLICKERING_DURATION_KEY,
            ALARM_FLICKERING_DURATION_DEFAULT_VALUE_SECONDS
        )
        val futureTimeout = timeout?.let { alarmProcess?.alarmTime?.timeInMillis?.plus(it * 1000) }

        if (futureTimeout != null) {
            val calendar = Calendar.getInstance()
            return when {
                futureTimeout < calendar.timeInMillis -> true
                else -> false
            }
        }
        return true
    }

    //store locally default values of configuration
    private fun setConfigurationDefault() {

        if (getLongInPreference(this, ALARM_FLICKERING_DURATION_KEY, -1L) == -1L) {
            //set the duration of flickering icon when accepted alarm
            setLongInPreference(
                this,
                ALARM_FLICKERING_DURATION_KEY,
                ALARM_FLICKERING_DURATION_DEFAULT_VALUE_SECONDS
            )
        }

        if (getIntInPreference(this, MAP_SHOW_VIEW_TYPE_KEY, -1) == -1) {
            //set the type of map
            setIntInPreference(this, MAP_SHOW_VIEW_TYPE_KEY, MAP_SHOW_SATELLITE_VALUE)
        }

        if (getStringInPreference(this, SELECTED_NOTIFICATION_SOUND_KEY, "-1").equals("-1")) {

            val uri=Uri.parse("android.resource://$packageName/raw/alarm_sound")

            setStringInPreference(this, SELECTED_NOTIFICATION_SOUND_KEY, uri.toString())
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, arg1: Intent) {
            when {
                arg1.action == USB_DEVICES_EMPTY -> {
                    //setBooleanInPreference(this@MyScreensActivity, USB_DEVICE_CONNECT_STATUS, false)
                    setUIusbConnection(false)

                }
                arg1.action == USB_DEVICES_NOT_EMPTY -> {
                    val isConnected =
                        getBooleanInPreference(context, USB_DEVICE_CONNECT_STATUS, false)
                    //setBooleanInPreference(this@MyScreensActivity, USB_DEVICE_CONNECT_STATUS, true)
                    if (isConnected) {
                        setUIusbConnection(true)
                    } else {
                        //if there is device and the status is wrong then restart the connection
                        startConnectionService()
                    }

                }
                arg1.action == UsbManager.ACTION_USB_DEVICE_ATTACHED -> {

                }
                //when disconnect the device from USB
                arg1.action == UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    //Toast.makeText(this@MyScreensActivity, "detach", Toast.LENGTH_SHORT).show()
                    showUsbReadDisconnection()
                    playVibrate()
                    showDisconnectUsbDialog()
                }
                arg1.action == CREATE_ALARM_KEY -> {
                    //startTimer()
                }
                arg1.action == STOP_ALARM_SOUND -> {

                }
                arg1.action == "not_connection" -> {
                    //showToast(this@MyScreensActivity,"not_connection")
                }
                arg1.action == "yes_connection" -> {
                    //showToast(this@MyScreensActivity,"yes_connection")
                }
            }
        }

        //show dialog with disconnect usb message
        private fun showDisconnectUsbDialog() {
            val builder = AlertDialog.Builder(this@MyScreensActivity)
            builder.setIcon(R.drawable.ic_alert)
            builder.setTitle(resources.getString(R.string.receiver_disconnected))
                .setCancelable(false)
            val ok = resources.getString(R.string.OK)

            builder.setPositiveButton(ok) { dialog, which ->

                dialog.dismiss()
            }
            val alert = builder.create()
            alert.show()
        }

        //execute vibrate
        private fun playVibrate() {

            val isVibrateWhenAlarm =
                getBooleanInPreference(applicationContext, IS_VIBRATE_WHEN_ALARM_KEY, true)
            if (isVibrateWhenAlarm) {
                // Get instance of Vibrator from current Context
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

                // Vibrate for 200 milliseconds
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            1000,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    vibrator.vibrate(1000)
                }

            }

        }
    }


    //show usb read disconnection
    private fun showUsbReadDisconnection() {
        //setBooleanInPreference(this@MyScreensActivity, USB_DEVICE_CONNECT_STATUS, false)
        //bug fixed : kill also the service
        //sendBroadcast(Intent(DISCONNECT_USB_PROCESS_KEY))
        setUIusbConnection(false)
    }

    private fun setFilter() {
        val filter = IntentFilter(USB_DEVICES_EMPTY)
        //filter.addAction("android.hardware.usb.action.USB_STATE")
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction(USB_DEVICES_NOT_EMPTY)
        filter.addAction(CREATE_ALARM_KEY)
        filter.addAction(STOP_ALARM_SOUND)
        filter.addAction("not_connection")
        filter.addAction("yes_connection")
        registerReceiver(usbReceiver, filter)
    }


    private fun init() {

        configureActionBar()

        //start listener to alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, ServiceHandleAlarms::class.java))
        } else {
            startService(Intent(this, ServiceHandleAlarms::class.java))
        }
        configTabs()

    }

    private fun setUIusbConnection(state: Boolean) {
        togChangeStatus?.isChecked = state
    }

    //TODO : the toggle will updated by the status changing
    private fun configureActionBar() {

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)//supportActionBar
        setSupportActionBar(toolbar)

        togChangeStatus = findViewById(
            R.id.togChangeStatus
        )

        consMyActionBar = findViewById(
            R.id.consMyActionBar
        )
        consMyActionBar?.setOnClickListener {
            if (clickHundler == null) {
                clickHundler = ClickHandler()
                Thread(clickHundler).start()
            } else {
                clickHundler?.recordNewClick()
            }
        }

        val isConnected = getBooleanInPreference(this, USB_DEVICE_CONNECT_STATUS, false)
        togChangeStatus?.isChecked = isConnected

        //Toast.makeText(this,"isConnected1="+isConnected, Toast.LENGTH_SHORT).show()

        togChangeStatus?.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                startConnectionService()
            } else {
                setBooleanInPreference(this, USB_DEVICE_CONNECT_STATUS, false)
                sendBroadcast(Intent(STOP_READ_DATA_KEY))
                //sendBroadcast(Intent(DISCONNECT_USB_PROCESS_KEY))
                sendBroadcast(Intent(STOP_ALARM_SOUND))
            }
        }
    }

    //start connection service
    private fun startConnectionService() {

        val connector = Intent(this, ServiceConnectSensor::class.java)
        //stopService(connector)
        //Toast.makeText(this,"isConnected2="+isConnected, Toast.LENGTH_SHORT).show()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(connector)
        } else {
            startService(Intent(connector))
        }
    }


    private fun configTabs() {

        val tabs = findViewById<TabLayout>(R.id.tab_layout)

        viewPager = findViewById(R.id.vPager)

        collectionPagerAdapter = CollectionPagerAdapter(supportFragmentManager)
        viewPager.adapter = collectionPagerAdapter
        viewPager.offscreenPageLimit = 0
        //prevent change screen by drag
        viewPager.setOnTouchListener(object : OnTouchListener {


            override fun onTouch(v: View, event: MotionEvent): Boolean {
                return true
            }
        })

        //relate the tab layout to viewpager because we need to add the icons
        tabs.setupWithViewPager(vPager)
        tabs.getTabAt(0)?.icon = ContextCompat.getDrawable(
            this@MyScreensActivity,
            R.drawable.selected_sensor_tab
        )
        tabs.getTabAt(1)?.icon = ContextCompat.getDrawable(
            this@MyScreensActivity,
            R.drawable.selected_map_tab
        )
        tabs.getTabAt(2)?.icon =
            ContextCompat.getDrawable(this@MyScreensActivity, R.drawable.selected_alarm_log_tab)
        tabs.getTabAt(3)?.icon =
            ContextCompat.getDrawable(this@MyScreensActivity, R.drawable.selected_config_tab)


        viewPager.currentItem = currentItemTopMenu


    }


    override fun onStart() {
        super.onStart()
        setFilter()
    }


    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(usbReceiver)
        } catch (ex: Exception) {

        }
    }


    private fun setLocationPermission() {
        /*
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            //setExternalPermission()
            init()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                init()
                //setExternalPermission()
            }
        }

    }






    // Since this is an object collection, use a FragmentStatePagerAdapter,
// and NOT a FragmentPagerAdapter.
    inner class CollectionPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(
        fm,
        BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
    ) {

        override fun getCount(): Int = MAIN_MENU_NUM_ITEM

        override fun getItem(position: Int): Fragment {

            var fragment: Fragment? = null
            //set event of click ic_on top menu
            when (position) {
                0 -> {
                    fragment = SensorsFragment()
                    fragment.arguments = Bundle().apply {
                        // Our object is just an integer :-P
                        putInt("ARG_OBJECT", position + 1)
                    }
                }
                1 -> {
                    fragment = MapmobFragment()//MapSensorsFragment()//MapmobFragment()
                    fragment.arguments = Bundle().apply {
                        // Our object is just an integer :-P
                        putInt("ARG_OBJECT", position + 1)
                    }
                }
                3 -> {
                    fragment = ConfigurationFragment()
                    fragment.arguments = Bundle().apply {
                        // Our object is just an integer :-P
                        putInt("ARG_OBJECT", position + 1)
                    }
                }
                2 -> {
                    fragment = AlarmsLogFragment()
                    fragment.arguments = Bundle().apply {
                        // Our object is just an integer :-P
                        putInt("ARG_OBJECT", position + 1)
                    }
                }
            }
            return fragment!!

        }

        override fun getPageTitle(position: Int): CharSequence {

            //set the title text of top menu
            return when (position) {
                0 -> resources.getString(R.string.sensor_table_title)
                1 -> resources.getString(R.string.map_title)
                2 -> resources.getString(R.string.alarm_log_title)
                3 -> resources.getString(R.string.config_title)
                else -> "nothing"
            }

        }

    }


    override fun onBackPressed() {
        //back press when the command fragment is showed
        val prev = supportFragmentManager.findFragmentByTag("CommandsFragment")
        if (prev != null && prev.isAdded) {
            val df: DialogFragment = prev as DialogFragment
            df.dismiss()

        } else {//normal
            super.onBackPressed()
            sendBroadcast(Intent(STOP_ALARM_SOUND))
            //start activity for loading new language if it has been changed
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    //set the language of the app (calling  from activity)
    override fun updateLanguage() {
        setAppLanguage(this, GeneralItemMenu.selectedItem)
        this.finish()
        intent.putExtra(CURRENT_ITEM_TOP_MENU_KEY, 3)
        this.startActivity(intent)
    }


    override fun update(o: Observable?, arg: Any?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    inner class ClickHandler : Runnable {

        private val WAIT_DELAY = 1000

        private var count = 1
        private var lastSubmitTime = System.currentTimeMillis()

        fun recordNewClick() {
            count++
            lastSubmitTime = System.currentTimeMillis()
        }

        override fun run() {
            while (System.currentTimeMillis() - lastSubmitTime <= WAIT_DELAY) {
                // idle
                Thread.yield()
            }
            runOnUiThread {
                if (count >= 3) {
                    sendBroadcast(Intent(ACTION_TOGGLE_TEST_MODE))
                    count = 0
                    clickHundler = null
                }
                count = 0
                clickHundler = null
            }


        }
    }

}






