package com.sensoguard.detectsensor.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.classes.CryptoHandler
import com.sensoguard.detectsensor.global.*
import kotlinx.android.synthetic.main.activity_activation.*

class InitAppActivity : ParentActivity() {

    private var myImei: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        //setBooleanInPreference(this, IS_SSL_MAIL,false)
        //init the usb disconnected
        sendBroadcast(Intent(STOP_READ_DATA_KEY))
        setBooleanInPreference(this, USB_DEVICE_CONNECT_STATUS, false)

        setContentView(R.layout.activity_init_app)
        setReadPhoneStatePermission()
    }

    //get the IMEI of the device and check it with the locally
    private fun configureActivation(){

        myImei=getDeviceIMEI()
        tvImei?.text = myImei

        val localActivateCode= getStringInPreference(this, ACTIVATION_CODE_KEY, NO_DATA)
        if(!localActivateCode.equals(NO_DATA)){
            val myActivateCode= CryptoHandler.getInstance().encrypt(myImei)
            if(localActivateCode!=null && myActivateCode.startsWith(localActivateCode)){
                val inn = Intent(this, MainActivity::class.java)
                inn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(inn)
            }else{
                Toast.makeText(this,resources.getString(R.string.wrong_activate_code), Toast.LENGTH_SHORT).show()
                openActivation()
            }
        }else{
            openActivation()
        }
    }

    //open activation screen
    private fun openActivation() {
        val inn = Intent(this, ActivationActivity::class.java)
        inn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        inn.putExtra(IMEI_KEY,myImei)
        startActivity(Intent(inn))
    }


    //get the device IMEI
    private fun getDeviceIMEI(): String? {
        var deviceUniqueIdentifier: String? = null
        val tm = this.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (null != tm) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
                
            ) {

                deviceUniqueIdentifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    tm.imei
                } else {
                    tm.deviceId
                }
            }
        }
        if (null == deviceUniqueIdentifier || deviceUniqueIdentifier.isEmpty()) {
            deviceUniqueIdentifier =
                Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
        }
        return deviceUniqueIdentifier
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_PHONE_STATE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    configureActivation()
                }
            }
        }

    }

    //set permission of READ_PHONE_STATE
    private fun setReadPhoneStatePermission() {
        /*
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            configureActivation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_PHONE_STATE), PERMISSIONS_REQUEST_READ_PHONE_STATE
            )
        }
    }
}
