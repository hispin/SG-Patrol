package com.sensoguard.detectsensor.activities

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import com.sensoguard.detectsensor.global.configurationLanguage


open class ParentActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configurationLanguage(applicationContext)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        super.onKeyDown(keyCode, event)
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            Log.d("detectKey", "home")
            //The Code Want to Perform.

        }
        return true
    }

}