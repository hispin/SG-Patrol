package com.sensoguard.detectsensor.global

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory


//convert bitmap to bitmap discriptor
fun convertBitmapToBitmapDiscriptor(context:Context,resId:Int): BitmapDescriptor? {
    val bitmap = context.let { getBitmapFromVectorDrawable(it, resId) }
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

//convert resId to bitmap
fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap? {
    var drawable =  AppCompatResources.getDrawable(context, drawableId)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        drawable = (drawable?.let { DrawableCompat.wrap(it) })?.mutate()
    }

    val bitmap = drawable?.intrinsicWidth?.let {
        Bitmap.createBitmap(
            it,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    } ?: return null

    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap
}