package com.example.webrtctest.view

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.webrtctest.*
import com.example.webrtctest.model.data.EXTRA_LOOPBACK
import com.example.webrtctest.model.data.EXTRA_RUNTIME
import com.example.webrtctest.model.data.EXTRA_USE_VALUES_FROM_INTENT
import com.example.webrtctest.model.data.PERMISSION_REQUEST
import com.example.webrtctest.utils.getMissingPermissions
import kotlinx.android.synthetic.main.activity_main.*

/**
 *   대기 화면
 */
class ConnectActivity : AppCompatActivity() {

    private val commandLineRun = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        // 가진 옵션들을 가지고 룸정보와 함꼐 이동 및 연결시도 진행...
//        connectBtn.setOnClickListener {
//            startActivity(
//                Intent(this@ConnectActivity, VoiceChatActivity::class.java).apply {
//                    // 옵션 지정.. url , encoding 등등...
//                }
//            )
//        }

    }

    fun requestPermission(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            onPermissionsGranted()
        } else {
//            val missingPermissions: Array<String> = getMissingPermissions()
            getMissingPermissions()?.let {
                if (it.isNotEmpty()) {
                    requestPermissions(it,
                        PERMISSION_REQUEST
                    )
                } else {
                    onPermissionsGranted()
                }
            }
        }
    }

    private fun onPermissionsGranted() {
        // If an implicit VIEW intent is launching the app, go directly to that URL.
        val intent = intent
        if ("android.intent.action.VIEW" == intent.action && !commandLineRun) {
            val loopback = intent.getBooleanExtra(EXTRA_LOOPBACK, false)
            val runTimeMs = intent.getIntExtra(EXTRA_RUNTIME, 0)
            val useValuesFromIntent = intent.getBooleanExtra(EXTRA_USE_VALUES_FROM_INTENT, false)
//            val room: String = sharedPref.getString(keyprefRoom, "") // ??

            // connectToRoom : 음성채팅 액티비티로 이동하고 그안에서 연결에 관련된 부분 진행하는듯함
//            connectToRoom(room, true, loopback, useValuesFromIntent, runTimeMs)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}
