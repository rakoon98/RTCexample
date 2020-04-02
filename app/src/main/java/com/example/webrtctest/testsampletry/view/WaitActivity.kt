package com.example.webrtctest.testsampletry.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.webrtctest.R
import kotlinx.android.synthetic.main.activity_wait.*

class WaitActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wait)

        connectCallBnt.setOnClickListener {
            startActivity(
                Intent(this, TryActivity::class.java)
            )
        }

    }

}