package com.example.webrtctest.testsampletry.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.webrtctest.R
import com.example.webrtctest.testsampletry.TryRTCClient
import com.example.webrtctest.testsampletry.TrySignalingClient
import kotlinx.android.synthetic.main.activity_main.*
import me.amryousef.webrtc_demo.AppSdpObserver
//import me.amryousef.webrtc_demo.MainActivity
import me.amryousef.webrtc_demo.PeerConnectionObserver
import me.amryousef.webrtc_demo.TrySignallingClientListener
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription

open class TryActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    }

    open lateinit var rtcClient: TryRTCClient
    open lateinit var signalingClient : TrySignalingClient
//    private lateinit var signalingClient : SignallingClient

    open val sdpObserver = object: AppSdpObserver(){
        override fun onCreateSuccess(p0: SessionDescription?) {
            super.onCreateSuccess(p0)
            Log.d("요호호","가즈아 sdpObserver onCreateSuccess: ${p0?.type}")
            signalingClient.send(p0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkCameraPermission()
    }

    // 카메라 권한
    private fun checkCameraPermission(){
        if(ContextCompat.checkSelfPermission(this,
                CAMERA_PERMISSION
            ) != PackageManager.PERMISSION_GRANTED ){
            requestCameraPermission()
        } else {
            Log.d("요호호","가즈아")
            onCameraPermissionGranted()
        }
    }

    fun onCameraPermissionGranted(){
        rtcClient =
            TryRTCClient(application,
                object : PeerConnectionObserver() {
                    override fun onIceCandidate(p0: IceCandidate?) {
                        super.onIceCandidate(p0)
                        Log.d("요호호", "Try:onIceCandidate : $p0")
                        signalingClient.send(p0)
                        rtcClient.addIceCandidate(p0)
                    }

                    override fun onAddStream(mediaStream: MediaStream?) {
                        super.onAddStream(mediaStream)
                        Log.d("요호호", "Try:onAddStream : $mediaStream")
                        mediaStream?.videoTracks?.get(0)?.addSink(remote_view)
                    }
                })
        rtcClient.run {
            // 나와 상대방 서피스뷰 초기화
            remote_view.initSurfaceView()
            local_view.initSurfaceView()

            local_view.startLocalVideoCapture()
            signalingClient =
                TrySignalingClient(
                    this@TryActivity,
                    createSignallingClientListener()
                )
//            signalingClient = SignallingClient( createSignallingClientListener() )
//            call_button.setOnClickListener { call(sdpObserver) }
            Log.d("요호호","가즈아2")
        }
    }

    // 시그널링 클라이언트 리스너 생성 !!
    private fun createSignallingClientListener() = object : TrySignallingClientListener {
        override fun onConnectionEstablished() {
            Log.d("요호호","Try:onConnectionEstablished ")
            call_button.isClickable = true
        }

        override fun onOfferReceived(description: SessionDescription) {
            Log.d("요호호","Try:onOfferRecived : $description")
            rtcClient.onRemoteSessionReceived(description)
            rtcClient.answer(sdpObserver)
            remote_view_loading.visibility = View.GONE
        }

        override fun onAnswerReceived(description: SessionDescription) {
            Log.d("요호호","Try:onAnswerReceived : $description")
            rtcClient.onRemoteSessionReceived(description)
            remote_view_loading.visibility = View.GONE
        }

        override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
            Log.d("요호호","Try:onIceCandidateReceived : $iceCandidate")
            rtcClient.addIceCandidate(iceCandidate)
        }
    }

    /** Camera Permission Request Granted & Denied  **/
    private fun requestCameraPermission(dialogShown: Boolean = false) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                CAMERA_PERMISSION
            ) && !dialogShown) {
            showPermissionRationaleDialog()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(CAMERA_PERMISSION),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }
    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Camera Permission Required")
            .setMessage("This app need the camera to function")
            .setPositiveButton("Grant") { dialog, _ ->
                dialog.dismiss()
                requestCameraPermission(true)
            }
            .setNegativeButton("Deny") { dialog, _ ->
                dialog.dismiss()
                onCameraPermissionDenied()
            }
            .show()
    }
    private fun onCameraPermissionDenied() { Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_LONG).show() }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.all { it==PackageManager.PERMISSION_GRANTED }){
            onCameraPermissionGranted()
        } else {
            onCameraPermissionDenied()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        signalingClient.onDestroy()
    }
}






