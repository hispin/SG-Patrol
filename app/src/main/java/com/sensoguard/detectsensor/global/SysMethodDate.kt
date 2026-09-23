package com.sensoguard.detectsensor.global

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

fun getStringFromCalendar(calendar: Calendar,format:String,context: Context):String{
    val locale = context.resources.configuration.locales.getFirstMatch(
        context.resources.assets.locales
    )
    val dateFormat = SimpleDateFormat(format, locale)//"kk:mm dd/MM/yy"
    //dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val dateString = dateFormat.format(calendar.time)
    return dateString
}

fun getStringFromCalendar(context: Context): String {
    val locale =
        context.resources.configuration.locales.getFirstMatch(context.resources.assets.locales)
    val dateFormat= SimpleDateFormat("kk:mm dd/MM/yy", locale)//"kk:mm dd/MM/yy"
    val dateString=dateFormat.format(Calendar.getInstance().time)
    return dateString
}

//get string format by current date time in milliseconds format
fun getStrDateTimeByMilliSeconds(milliSeconds: Long,format:String,context: Context):String{
    val locale =
        context.resources.configuration.locales.getFirstMatch(context.resources.assets.locales)
    val dateFormat= SimpleDateFormat(format, locale)
    val date = Date(milliSeconds)
    return dateFormat.format(date)
}