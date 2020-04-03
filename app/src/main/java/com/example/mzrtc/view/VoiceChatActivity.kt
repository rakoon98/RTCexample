package com.example.mzrtc.view

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.mzrtc.R
import com.example.mzrtc.model.data.EXTRA_VIDEO_FILE_AS_CAMERA
import com.example.mzrtc.model.data.VOICECHATACTIVITY_TAG
import com.example.mzrtc.rtc.AppRTCAudioManager
import com.example.mzrtc.rtc.SignalingParameters
import com.example.mzrtc.rtc.WebSocketRTCClient
import com.example.mzrtc.rtc.classrtc.DataChannelParameters
import com.example.mzrtc.rtc.classrtc.PeerConnectionParameters
import com.example.mzrtc.rtc.classrtc.ProxyVideoSink
import com.example.mzrtc.rtc.classrtc.RoomConnectionParameters
import com.example.mzrtc.rtc.client.DirectRTCClient
import com.example.mzrtc.rtc.client.PeerConnectionClient
import com.example.mzrtc.rtc.data.IP_PATTERN
import com.example.mzrtc.rtc.interfacertc.AppRTCClient
import com.example.mzrtc.rtc.interfacertc.OnCallEvents
import com.example.mzrtc.rtc.interfacertc.PeerConnectionEvents
import com.example.mzrtc.rtc.interfacertc.SignalingEvents
import kotlinx.android.synthetic.main.activity_video_chat.*
import org.webrtc.*
import java.io.IOException
import java.util.*


/**
 *  연결 화면 : 1. 연결 시도 중..
 *            2. 연결 성공/실패 : 성공 -> 음성 채팅 , 실패 :  ~~이유로 인해 실패 함 -> 복귀.
 *            3. 연결 해제 -> 대기 화면으로 복귀
 */
class VoiceChatActivity : AppCompatActivity(),
    PeerConnectionEvents ,
    SignalingEvents,
        OnCallEvents
{
    val TAG = this::class.java.simpleName

    private var callControlFragmentVisible = true   // 하단 부분 : 전화, 카메라, 화면크기, 마이크 등등 옵션 변

    private val remoteProxyRenderer = ProxyVideoSink()  // 원격 리모트 렌더러  ??
    private val localProxyVideoSink = ProxyVideoSink()  // 로컬(내화면?) 싱크  ??

    private var peerConnectionParameters: PeerConnectionParameters? = null
    var peerConnectionClient : PeerConnectionClient? = null
    var eglBase : EglBase = EglBase.create()
    var appRtcClient : AppRTCClient? = null

    var audioManager: AppRTCAudioManager? = null
    var roomConnectionParameters: RoomConnectionParameters? = null
    private var signalingParameters: SignalingParameters? = null
    private var remoteSinks: List<VideoSink> = ArrayList()

    // 옵션 변수
    private var screencaptureEnabled : Boolean = false    //   화면 ( 화상 ) 을 캡쳐해서 전송하고 받을지의 여부
    private var callStartedTimeMillis: Long = 0

    var mediaProjectionPermissionResultCode = -1
    var mediaProjectionPermissionResultData : Intent? = null

    private var commandLineRun = false
    private var activityRunning = false

    override fun onCreate( savedInstanceState: Bundle? ) {
        super.onCreate( savedInstanceState )
        setContentView(R.layout.activity_video_chat)

        connection()

    }

    // 연결시도
    fun connection() {
        // 화면 연결
        fullscreen_video_view.init( eglBase.eglBaseContext, null )
        fullscreen_video_view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        fullscreen_video_view.setEnableHardwareScaler(true)
//        localProxyVideoSink.setTarget(fullscreen_video_view)

//        var roomId = intent.getStringExtra(EXTRA_ROOMID)
        var roomUri = "98888660"
        var roomId = "8080"

        /**
         *   등등 중간 스킵
         */

        appRtcClient = if(/** loopback || **/ IP_PATTERN.matcher(roomId).matches()){
            WebSocketRTCClient(events = this)
        } else {
            DirectRTCClient(events = this@VoiceChatActivity)
        }

        // create connection parameters.
        var urlParameters = ""
        roomConnectionParameters = RoomConnectionParameters(roomUri, roomId, loopback = true, urlParameters = urlParameters)

        // if(~~~) disconnect()
        var dataChannelParameters = DataChannelParameters(true, -1, -1, "", false, -1)
        peerConnectionParameters = PeerConnectionParameters(
            videoCallEnabled = true,
            loopback = true,
            tracing = false,
            videoWidth = 0,
            videoHeight = 0,
            videoFps = 0,
            videoMaxBitrate = 0,
            videoCodec = "",
            videoCodecHwAcceleration = true,
            videoFlexfecEnabled = false,
            audioStartBitrate = 0,
            audioCodec = "",
            noAudioProcessing = false,
            aecDump = false,
            saveInputAudioToFile = false,
            useOpenSLES = false, disableBuiltInAEC = false,
            disableBuiltInAGC = false, disableBuiltInNS = false,
            disableWebRtcAGCAndHPF = false, enableRtcEventLog = false,
            dataChannelParameters = dataChannelParameters
        )

        peerConnectionClient = PeerConnectionClient(
            this,
            eglBase = eglBase,
            peerConnectionParameters = peerConnectionParameters!!,
            events = this@VoiceChatActivity
        )

        var options = PeerConnectionFactory.Options()
        peerConnectionClient?.createPeerConnectionFactory(options)

        if(screencaptureEnabled){
            // 화상채팅
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                startScreenCapture()
            } else {
                startCall()
            }
        } else {
            // 음성채팅
            startCall()
        }

    }

    fun onConnectedToRoomInternal(params:SignalingParameters){
        var delta: Long = System.currentTimeMillis() - callStartedTimeMillis

        signalingParameters = params
        var videoCapture: VideoCapturer? = null
        if(peerConnectionParameters!!.videoCallEnabled) videoCapture = createVideoCapturer()

        peerConnectionClient!!.createPeerConnection(
            localProxyVideoSink, remoteSinks = remoteSinks, videoCapturer = videoCapture!!, signalingParameters = signalingParameters
        )

        if( signalingParameters!!.initiator ){
//            peerConnectionClient.createOffer()
        } else {
            if( params.offerSdp != null ){
//                peerConnectionClient.setRemoteDescription(params.offerSdp)
//                peerConnectionClient.createAnswer()
            }
            if(params.iceCandidates != null ){

                // Add remote ICE candidates from room.
                for (iceCandidate in params.iceCandidates) {
//                    peerConnectionClient.addRemoteIceCandidate(iceCandidate)
                }
            }
        }
    }



    /** PeerConnectionEvents Implementation **/
    override fun onLocalDescription(sdp: SessionDescription?) {
        TODO("Not yet implemented")
    }

    override fun onIceCandidate(candidate: IceCandidate?) {
        TODO("Not yet implemented")
    }

    override fun onIceCandidatesRemoved(candidates: Array<IceCandidate?>?) {
        TODO("Not yet implemented")
    }

    override fun onIceConnected() {
        TODO("Not yet implemented")
    }

    override fun onIceDisconnected() {
        TODO("Not yet implemented")
    }

    override fun onConnected() {
        TODO("Not yet implemented")
    }

    override fun onDisconnected() {
        TODO("Not yet implemented")
    }

    override fun onPeerConnectionClosed() {
        TODO("Not yet implemented")
    }

    override fun onPeerConnectionStatsReady(reports: Array<StatsReport?>?) {
        TODO("Not yet implemented")
    }

    override fun onPeerConnectionError(description: String?) {
        TODO("Not yet implemented")
    }

    /** SignalingEvents Implemetaion **/
    override fun onConnectedToRoom(params: SignalingParameters?) {
        runOnUiThread { onConnectedToRoomInternal(params!!) }
    }

    override fun onRemoteDescription(sdp: SessionDescription?) {
        TODO("Not yet implemented")
    }

    override fun onRemoteIceCandidate(candidate: IceCandidate?) {
        TODO("Not yet implemented")
    }

    override fun onRemoteIceCandidatesRemoved(candidates: Array<IceCandidate?>) {
        TODO("Not yet implemented")
    }

    override fun onChannelClose() {
        TODO("Not yet implemented")
    }

    override fun onChannelError(description: String?) {
        Log.d(TAG, "Connect to room: $description")
    }

    /** OnCallEvents Implementation **/
    override fun onCallHangUp() {
        TODO("Not yet implemented")
    }

    override fun onCameraSwitch() {
        TODO("Not yet implemented")
    }

    override fun onVideoScalingSwitch(scalingType: RendererCommon.ScalingType?) {
        TODO("Not yet implemented")
    }

    override fun onCaptureFormatChange(width: Int, height: Int, framerate: Int) {
        TODO("Not yet implemented")
    }

    override fun onToggleMic(): Boolean {
        TODO("Not yet implemented")
    }



    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun startScreenCapture(){
        val mediaProjectionManager = application.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),1)
    }

    private fun startCall(){
        appRtcClient?.let {
            callStartedTimeMillis = System.currentTimeMillis()

            // start room connection
//            logAndToast(TAG, getString(R.string.connecting_to, roomConnectionParameters.roomUrl))
            it.connectToRoom(roomConnectionParameters)

            // create and audio manager that will take care of audio routing,
            // audio modes, audio device enumeration etc.
//            audioManager = AppRTCAudioManager.create(applicationContext)

            // store existing audio settings and change audio mode to
            // MODE_IN_COMMUNICATION for best possible VoIP performance.
            Log.d(TAG, "Starting the audio manager...")
//            audioManager.start(AudioManagerEvents())
        } ?: kotlin.run {
            return
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode!=1)return

        mediaProjectionPermissionResultCode = resultCode
        mediaProjectionPermissionResultData = data
        startCall()
    }

//    // Show/hide call control fragment on view click.
//    var listener = View.OnClickListener { toggleCallControlFragmentVisibility() }
//    // Helper functions.
//    private fun toggleCallControlFragmentVisibility() {
//        if (!connected || !callFragment.isAdded()) {
//            return
//        }
//        // Show/hide call control fragment
//        callControlFragmentVisible = !callControlFragmentVisible
//        val ft = supportFragmentManager.beginTransaction()
////        val ft = fragmentManager.beginTransaction()
//        if (callControlFragmentVisible) {
//            ft.show(callFragment)
//            ft.show(hudFragment)
//        } else {
//            ft.hide(callFragment)
//            ft.hide(hudFragment)
//        }
//        ft.setTransition( FragmentTransaction.TRANSIT_FRAGMENT_FADE  )
//        ft.commit()
//    }


    private fun createVideoCapturer(): VideoCapturer? {
        var videoCapturer: VideoCapturer? = null
        var videoFileAsCamera =
            intent.getStringExtra(EXTRA_VIDEO_FILE_AS_CAMERA)
        when {
            videoFileAsCamera != null -> {
                videoCapturer = try {
                    FileVideoCapturer(videoFileAsCamera)
                } catch (e: IOException) {
                    reportError("Failed to open video file for emulated camera")
                    return null
                }
            }
            screencaptureEnabled -> {
//                return createScreenCapturer()
            }
//            useCamera2() -> {
//                if (!captureToTexture()) {
//                    reportError(getString(R.string.camera2_texture_only_error))
//                    return null
//                }
//                Logging.d(VOICECHATACTIVITY_TAG, "Creating capturer using camera2 API.")
//                videoCapturer = createCameraCapturer(Camera2Enumerator(this))
//            }
            else -> {
                Logging.d(VOICECHATACTIVITY_TAG, "Creating capturer using camera1 API.")
//                videoCapturer = createCameraCapturer(Camera1Enumerator(captureToTexture()))
            }
        }
//        if (videoCapturer == null) {
//            reportError("Failed to open camera")
//            return null
//        }
        return videoCapturer
    }


    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames

        // First, try to find front facing camera
        Logging.d(VOICECHATACTIVITY_TAG, "Looking for front facing cameras.")
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(VOICECHATACTIVITY_TAG, "Creating front facing camera capturer.")
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d(VOICECHATACTIVITY_TAG, "Looking for other cameras.")
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(VOICECHATACTIVITY_TAG, "Creating other camera capturer.")
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        return null
    }


    private fun reportError(description: String) {
        runOnUiThread {
//            if (!isError) {
//                isError = true
                disconnectWithErrorMessage(description)
//            }
        }
    }

    private fun disconnectWithErrorMessage(errorMessage: String) {
        if (commandLineRun || !activityRunning) {
            Log.e(VOICECHATACTIVITY_TAG, "Critical error: $errorMessage")
            disconnect()
        } else {
            AlertDialog.Builder(this)
                .setTitle("채널에러")
                .setMessage(errorMessage)
                .setCancelable(false)
                .setNeutralButton("ok", DialogInterface.OnClickListener { dialog, id ->
                        dialog.cancel()
                        disconnect()
                    })
                .create()
                .show()
        }
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    private fun disconnect() {
//        activityRunning = false
//        remoteProxyRenderer.setTarget(null)
//        localProxyVideoSink.setTarget(null)
//        if (appRtcClient != null) {
//            appRtcClient!!.disconnectFromRoom()
//            appRtcClient = null
//        }
//        if (pipRenderer != null) {
//            pipRenderer.release()
//            pipRenderer = null
//        }
//        if (videoFileRenderer != null) {
//            videoFileRenderer.release()
//            videoFileRenderer = null
//        }
//        if (fullscreenRenderer != null) {
//            fullscreenRenderer.release()
//            fullscreenRenderer = null
//        }
//        if (peerConnectionClient != null) {
//            peerConnectionClient.close()
//            peerConnectionClient = null
//        }
//        if (audioManager != null) {
//            audioManager.stop()
//            audioManager = null
//        }
//        if (connected && !isError) {
//            setResult(Activity.RESULT_OK)
//        } else {
//            setResult(Activity.RESULT_CANCELED)
//        }
//        finish()
    }
}