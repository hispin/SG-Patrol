package com.sensoguard.detectsensor.activities

//import com.crashlytics.android.Crashlytics
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.classes.MyExceptionHandler
import com.sensoguard.detectsensor.global.*

//import io.fabric.sdk.android.Fabric


class MainActivity : ParentActivity() {

    private var clickConsSensorTable: ConstraintLayout? = null
    private var clickConsMap: ConstraintLayout? = null
    private var clickConsConfiguration: ConstraintLayout? = null
    private var clickAlarmLog: ConstraintLayout? = null
    private var tvShowVer: TextView? = null
    private var btnTest: AppCompatButton? = null
    //private var ivOfflineMap : AppCompatImageView?=null

//    @Override
//    protected override fun attachBaseContext(newBase:Context) {
//        configurationLanguage()
//    }

    override fun onStart() {
        super.onStart()
        //show version name
        val verName = packageManager.getPackageInfo(packageName, 0).versionName
        val verTitle = "version:$verName"
        tvShowVer?.text = verTitle
        var Test = "br3"
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        configureGeneralCatch()

        //Fabric.with(this, Crashlytics())

        super.onCreate(savedInstanceState)

        //configurationLanguage()

        setContentView(R.layout.activity_main)


        //hide unwanted badge of app icon
        hideBudgetNotification()

        initViews()
        setOnClickSensorTable()
        setOnClickMapTable()
        setOnClickConfigTable()
        setOnClickAlarmLogTable()

        //hide status bar
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        //for testing
        //saveMyAccount()
    }

    //for testing :save locally the Email account details
    private fun saveMyAccount() {
        setStringInPreference(this, USER_NAME_MAIL, "sg-patrol@sgsmtp.com")
        setStringInPreference(this, PASSWORD_MAIL, "SensoGuard1234")
        setStringInPreference(this, SERVER_MAIL, "mail.sgsmtp.com")
        setIntInPreference(this, PORT_MAIL, 587)
        setStringInPreference(this, RECIPIENT_MAIL, "hag.swead@gmail.com")
        setBooleanInPreference(this, IS_SSL_MAIL, false)
    }


    //hide unwanted badge of app icon
    private fun hideBudgetNotification() {
        val id = "my_channel_01"
        val name = getString(com.sensoguard.detectsensor.R.string.channel_name)
        val descriptionText = getString(com.sensoguard.detectsensor.R.string.channel_description)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val mChannel =
                NotificationChannel(id, name, importance).apply {
                    description = descriptionText
                    setShowBadge(false)
                }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        } else {

        }

    }

    override fun onBackPressed() {
        showConformDialog()
    }

    //show confirm dialog before stop usb process
    private fun showConformDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.disconnect_usb))
        val yes = resources.getString(R.string.yes)
        val no = resources.getString(R.string.no)
        builder.setMessage(resources.getString(R.string.this_will_disconnect_the_usb))
            .setCancelable(false)
        builder.setPositiveButton(yes) { dialog, which ->

            super.onBackPressed()
            //disconnect usb device and stop the process
            sendBroadcast(Intent(DISCONNECT_USB_PROCESS_KEY))
            setBooleanInPreference(this@MainActivity, USB_DEVICE_CONNECT_STATUS, false)
            sendBroadcast(Intent(DISCONNECT_USB_PROCESS_KEY))
            sendBroadcast(Intent(STOP_GENERAL_TIMER))

            dialog.dismiss()

        }


        // Display a negative button on alert dialog
        builder.setNegativeButton(no) { dialog, which ->
            dialog.dismiss()
        }
        val alert = builder.create()
        alert.show()
    }

    private fun configureGeneralCatch() {
        Thread.setDefaultUncaughtExceptionHandler(MyExceptionHandler(this))
    }

    private fun setOnClickSensorTable() {
        clickConsSensorTable?.setOnClickListener {
            val inn = Intent(this, MyScreensActivity::class.java)
            inn.putExtra(CURRENT_ITEM_TOP_MENU_KEY, 0)
            inn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(inn)
        }
    }
    private fun setOnClickMapTable() {
        clickConsMap?.setOnClickListener{
            val inn=Intent(this,MyScreensActivity::class.java)
            inn.putExtra(CURRENT_ITEM_TOP_MENU_KEY,1)
            inn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(inn)
        }
    }
    private fun setOnClickConfigTable() {
        clickConsConfiguration?.setOnClickListener{
            val inn=Intent(this,MyScreensActivity::class.java)
            inn.putExtra(CURRENT_ITEM_TOP_MENU_KEY, 3)
            inn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(inn)
        }
    }
    private fun setOnClickAlarmLogTable() {
        clickAlarmLog?.setOnClickListener{
            val inn=Intent(this,MyScreensActivity::class.java)
            inn.putExtra(CURRENT_ITEM_TOP_MENU_KEY, 2)
            inn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(inn)
        }
    }

    private fun initViews() {
        clickConsSensorTable = findViewById(com.sensoguard.detectsensor.R.id.clickConsSensorTable)
        clickConsMap = findViewById(com.sensoguard.detectsensor.R.id.clickConsMap)
        clickConsConfiguration =
            findViewById(com.sensoguard.detectsensor.R.id.clickConsConfiguration)
        clickAlarmLog = findViewById(com.sensoguard.detectsensor.R.id.clickAlarmLog)
        tvShowVer = findViewById(com.sensoguard.detectsensor.R.id.tvShowVer)
//        btnTest = findViewById(com.sensoguard.detectsensor.R.id.btnTest)
//        btnTest?.setOnClickListener {
//            //CustomMapTileProvider(ivOfflineMap,this)
//            startActivity(Intent(this@MainActivity,
//                DownloadOfflineTilesActivity::class.java))
//
//            //replaceFragment(R.id.flTestMapmob, MapmobFragment(),true,"MapmobFragment")
//        }

    }

//    private fun configurationLanguage() {
//        LanguageManager.setLanguageList()
//        val currentLanguage = getStringInPreference(this, CURRENT_LANG_KEY_PREF, "-1")
//        if (currentLanguage != "-1") {
//            GeneralItemMenu.selectedItem = currentLanguage
//            setAppLanguage(this, GeneralItemMenu.selectedItem)
//        } else {
//            val deviceLang = getAppLanguage()
//            if (LanguageManager.isExistLang(deviceLang)) {
//                GeneralItemMenu.selectedItem = deviceLang
//                setAppLanguage(this, GeneralItemMenu.selectedItem)
//            }
//        }
//    }

    //Change View of fragment
    private fun replaceFragment(
        resId: Int,
        fragment: Fragment,
        add_to_back_stack: Boolean,
        tag: String
    ) {
        try {
            val fragmentManager = supportFragmentManager
            val fragmentTransaction = fragmentManager.beginTransaction()


//            when (tag) {
//                "enterReservationFragment" -> {
//                    fragmentTransaction.setCustomAnimations(R.animator.slide_up_in, 0)
//                }
//            }

            if (add_to_back_stack) {
                fragmentTransaction.addToBackStack(fragment.tag)
            }

            fragmentTransaction.replace(resId, fragment, tag)

            //fragmentTransaction.show(fragment)
            fragmentTransaction.commit()
        } catch (ex: IllegalStateException) {
            ex.printStackTrace()
        }

    }
}
