package com.example.mzrtc.testsampletry.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mzrtc.R
import com.example.mzrtc.testsampletry.view.vns.VSActivity
import kotlinx.android.synthetic.main.activity_wait.*

class WaitActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wait)

        connectCallBnt1.setOnClickListener { startActivity(Intent(this, TryActivity::class.java)) }
        connectCallBnt2.setOnClickListener { startActivity(Intent(this, VSActivity::class.java)) }

    }

}