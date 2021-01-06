package com.sensoguard.detectsensor.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.sensoguard.detectsensor.global.configurationLanguage

open class ParentService : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        val languageToLoad:String? = getStringInPreference(this, CURRENT_LANG_KEY_PREF, "en")
//        if(languageToLoad!=null) {
//            setAppLanguage(this, languageToLoad)
//        }
        configurationLanguage(applicationContext)
        return super.onStartCommand(intent, flags, startId)
    }
}