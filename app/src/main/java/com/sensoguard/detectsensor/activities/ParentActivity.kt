package com.sensoguard.detectsensor.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.global.configurationLanguage

open class ParentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent)
    }

    override fun onResume() {
        super.onResume()
        configurationLanguage(this)
    }


}