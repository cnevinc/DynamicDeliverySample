package com.example.myapplication

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest

class MainActivity : AppCompatActivity() {

    private lateinit var splitInstallManager: SplitInstallManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        splitInstallManager = SplitInstallManagerFactory.create(this)
    }


    fun click(v: View) {


        val request = SplitInstallRequest
            .newBuilder()
            .addModule("history")
            .build()


        splitInstallManager
            .startInstall(request)
            .addOnSuccessListener {
                toast(" install success :$it")
                Intent().setClassName(packageName, "com.example.history.HistoryActivity")
                    .also { intent ->
                        startActivity(intent)
                    }
            }
            .addOnFailureListener { toast(" install fail :$it") }

    }


    private fun toast(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    }

}
