package com.example.myapplication

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.myserviceloader.IFactory
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val targetComponent = "com.example.history.HistoryActivity"
    }

    private lateinit var splitInstallManager: SplitInstallManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        splitInstallManager = SplitInstallManagerFactory.create(this)

    }

    fun tryServiceLoader(v: View) {
        val serviceLoaders = ServiceLoader.load(IFactory::class.java, MainActivity::class.java.classLoader)
        for (serviceLoader in serviceLoaders) {
            toast(serviceLoader.name())
            return
        }
        toast("can't find any implementation. Check the DexPathList?")
    }


    fun installModule(v: View) {

        val request = SplitInstallRequest
            .newBuilder()
            .addModule("history")
            .build()


        splitInstallManager
            .startInstall(request)
            .addOnSuccessListener {
                Log.d("MainActivity", " install success :$it")
                Intent().setClassName(packageName, targetComponent)
                    .also { intent ->
                        startActivity(intent)
                    }
            }
            .addOnFailureListener { toast(" install fail :$it") }

    }


    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

}
