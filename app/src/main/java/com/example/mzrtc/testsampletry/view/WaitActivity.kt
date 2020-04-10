package com.example.mzrtc.testsampletry.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mzrtc.R
import com.example.mzrtc.testsampletry.data.INTENT_PORT
import com.example.mzrtc.testsampletry.data.INTENT_ROOM
import com.example.mzrtc.testsampletry.data.INTENT_URL
import com.example.mzrtc.testsampletry.view.vns.VSActivity
import kotlinx.android.synthetic.main.activity_wait.*

class WaitActivity : AppCompatActivity() {

    var awsUrl = "http://nd-voice-chat-598404652.ap-northeast-2.elb.amazonaws.com"
    var myurl = "https://192.168.0.23"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wait)

        urlData_.setText(awsUrl)
        connectCallBnt1.setOnClickListener {
            startActivity(
                Intent(this, TryActivity::class.java).apply {
                    putExtra(INTENT_URL,urlData_.text.toString())
                    putExtra(INTENT_PORT,portData_.text.toString())
                    putExtra(INTENT_ROOM,roomIdData_.text.toString())
                }
            )
        }
        connectCallBnt2.setOnClickListener {
            startActivity(
                Intent(this, VSActivity::class.java).apply {
                    putExtra("url","https://192.168.0.23")
                    putExtra("port","8889")
                    putExtra("roomId","hi")
                }
            )
        }


    }

}