package com.example.history

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.example.myapplication.MainActivity
import com.example.myserviceloader.IFactory
import com.google.android.play.core.splitcompat.SplitCompat
import kotlinx.android.synthetic.main.hist.*
import java.util.*

class HistoryActivity : Activity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        SplitCompat.install(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hist)
    }

    override fun onResume() {
        super.onResume()
        val serviceLoaders = ServiceLoader.load(IFactory::class.java, HistoryActivity::class.java.classLoader)
        var cakes = "No Implementation found, some library will crash if it can't find an implementation."
        for (serviceLoader in serviceLoaders) {
            cakes = serviceLoader.name()
        }
        text.text = cakes
    }
}