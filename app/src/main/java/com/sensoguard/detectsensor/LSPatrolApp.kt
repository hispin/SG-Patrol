package com.sensoguard.detectsensor

import android.app.Application
import com.sensoguard.detectsensor.classes.GeneralItemMenu
import com.sensoguard.detectsensor.classes.LanguageManager
import com.sensoguard.detectsensor.global.CURRENT_LANG_KEY_PREF
import com.sensoguard.detectsensor.global.getAppLanguage
import com.sensoguard.detectsensor.global.getStringInPreference
import com.sensoguard.detectsensor.global.setAppLanguage

class LSPatrolApp : Application() {

    private fun configurationLanguage() {
        LanguageManager.setLanguageList()
        val currentLanguage = getStringInPreference(this, CURRENT_LANG_KEY_PREF, "-1")
        if (currentLanguage != "-1") {
            GeneralItemMenu.selectedItem = currentLanguage
            setAppLanguage(this, GeneralItemMenu.selectedItem)
        } else {
            val deviceLang = getAppLanguage()
            if (LanguageManager.isExistLang(deviceLang)) {
                GeneralItemMenu.selectedItem = deviceLang
                setAppLanguage(this, GeneralItemMenu.selectedItem)
            }
        }

    }
}