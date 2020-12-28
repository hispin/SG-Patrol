package com.sensoguard.detectsensor.global

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.sensoguard.detectsensor.classes.GeneralItemMenu
import com.sensoguard.detectsensor.classes.LanguageManager
import java.util.*


//Get the current language of the app
fun getAppLanguage(): String {
    return Locale.getDefault().language
}

fun configurationLanguage(context: Context) {
    LanguageManager.setLanguageList()
    val currentLanguage = getStringInPreference(context, CURRENT_LANG_KEY_PREF, "-1")
    if (currentLanguage != "-1") {
        GeneralItemMenu.selectedItem = currentLanguage
        setAppLanguage(context, GeneralItemMenu.selectedItem)
    } else {
        val deviceLang = getAppLanguage()
        if (LanguageManager.isExistLang(deviceLang)) {
            GeneralItemMenu.selectedItem = deviceLang
            setAppLanguage(context, GeneralItemMenu.selectedItem)
        }
    }
}
//
//set language for the application
fun setAppLanguage(c: Context, lang: String) {
    val localeNew = Locale(lang)
    Locale.setDefault(localeNew)

    val res = c.resources
    val newConfig = Configuration(res.configuration)
    //newConfig.locale = localeNew
    newConfig.setLocale(localeNew)
    newConfig.setLayoutDirection(localeNew)

    res.updateConfiguration(newConfig, res.displayMetrics)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        newConfig.setLocale(localeNew)
        c.createConfigurationContext(newConfig)
    }
}




