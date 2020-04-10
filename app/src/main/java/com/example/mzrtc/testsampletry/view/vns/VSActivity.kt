package com.example.mzrtc.testsampletry.view.vns

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.example.mzrtc.App
import com.example.mzrtc.R
import com.example.mzrtc.testsampletry.data.*
import com.example.mzrtc.testsampletry.util.vns.RTCPeerClient
import com.example.mzrtc.testsampletry.util.vns.RTCSignalingClient
import com.example.mzrtc.testsampletry.viewmodel.VSViewModel
import com.example.mzrtc.utils.setLogDebug
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import me.amryousef.webrtc_demo.AppSdpObserver
import me.amryousef.webrtc_demo.PeerConnectionObserver
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer

class VSActivity : AppCompatActivity(), LifecycleOwner {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    }


    var url = ""
    var port = ""
    var roomId = ""

    val channel = App.coChannel
//    var vsViewModel : VSViewModel? = null
    val vsViewModel : VSViewModel by lazy { VSViewModel(application , url , port, roomId) }
    val audioManager by lazy {
        (applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager).apply {
            mode = AudioManager.FLAG_SHOW_UI
        }
    }

    val recevice = CoroutineScope(Dispatchers.Main).async {
        val receiveData = channel.channel.asFlow()
        receiveData.collect {  data -> // 받은 아이들을 수집하여 그것을 진행한다.
            setLogDebug("receive Data activity : $data")
            when( data ) {
                "initView" -> {
                    vsViewModel.run {
                        setInitRender( localSurfaceViewRender, remoteSurfaceViewRender )
                    }
                }
                DESTROY -> {
                    destroy()
                }
                is MediaStream -> {
                    setLogDebug("미디어스트림 : ${data.videoTracks} --- ${data.videoTracks[0]}")
                    data?.videoTracks?.get(0)?.addSink(remoteSurfaceViewRender)
                }
            }
        }
    }

    var localSurfaceViewRender : SurfaceViewRenderer? = null
    var remoteSurfaceViewRender : SurfaceViewRenderer? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        intent?.run {
             url = getStringExtra("url")
             port = getStringExtra("port")
             roomId = getStringExtra("roomId")
            setLogDebug("url = $url , port = $port , roomId = $roomId")

            localSurfaceViewRender = findViewById(R.id.local_view)
            remoteSurfaceViewRender = findViewById(R.id.remote_view)
        }

//        audioBtn.setOnCheckedChangeListener { buttonView, isChecked ->
//            when(isChecked){
//                true->{ audioManager.isSpeakerphoneOn = true }
//                false->{ audioManager.isSpeakerphoneOn = false }
//            }
//        }

        observeLiveData()
// ㅁㄴ이ㅑ러매ㅑ덱ㅎㅁ내랴;ㅓㅁㄱㄷㅈㅎawefasdfasdfasdf

        // 카메 권한 가져오고 다음일 진행하는 부분
        // 처음에 음성으로 진행하면 필요없는듯
        checkCameraPermission()
    }

    // 카메라 권한
    private fun checkCameraPermission(){
        setLogDebug("checkCameraPermission")
        if(ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) != PackageManager.PERMISSION_GRANTED ){
            requestCameraPermission()
        } else {
            vsViewModel?.onCameraPermissionGranted()
        }
    }

    /** Camera Permission Request Granted & Denied  **/
    private fun requestCameraPermission(dialogShown: Boolean = false) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA_PERMISSION) && !dialogShown) {
            showPermissionRationaleDialog()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(CAMERA_PERMISSION),CAMERA_PERMISSION_REQUEST_CODE)
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
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_LONG)
            }
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.all { it==PackageManager.PERMISSION_GRANTED }){
            vsViewModel?.onCameraPermissionGranted()
        } else {
            Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_LONG)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        destroy()
    }

    fun destroy(){
        vsViewModel?.run {
            progressStatus.postValue(true)
            destroyPeerAndSocket()
        }

        if(localSurfaceViewRender!=null){
            localSurfaceViewRender!!.release()
            localSurfaceViewRender!!.clearFindViewByIdCache()
            localSurfaceViewRender = null
        }

        if(remoteSurfaceViewRender!=null){
            remoteSurfaceViewRender!!.release()
            remoteSurfaceViewRender!!.clearFindViewByIdCache()
            remoteSurfaceViewRender = null
        }

        finish()
    }

    fun observeLiveData(){
        vsViewModel?.progressStatus?.observe(this, Observer {
            if(it) remote_view_loading.visibility = View.VISIBLE
            else remote_view_loading.visibility = View.GONE
        })
    }

}