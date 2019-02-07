package com.example.history

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import com.google.android.play.core.splitcompat.SplitCompat
import kotlinx.android.synthetic.main.hist.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class HistoryActivity : Activity(), CoroutineScope {
    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hist)
        text.text = "History"

        job = Job()

        this.launch {
            Toast.makeText(this@HistoryActivity, "Hello Coroutine!!", Toast.LENGTH_LONG).show()
        }

    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        SplitCompat.install(this)
    }


}