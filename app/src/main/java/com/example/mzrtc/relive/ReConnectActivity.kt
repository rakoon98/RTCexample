//package com.example.webrtctest.relive
//
//import android.content.Context
//import android.content.Intent
//import android.media.AudioManager
//import android.media.projection.MediaProjectionManager
//import android.os.Build
//import android.os.Bundle
//import android.util.Log
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import com.example.webrtctest.R
//import com.example.webrtctest.rtc.classrtc.DataChannelParameters
//import com.example.webrtctest.rtc.classrtc.PeerConnectionParameters
//import com.example.webrtctest.utils.getFieldTrials
//import io.socket.client.IO
//import io.socket.client.Socket
//import io.socket.emitter.Emitter
//import kotlinx.android.synthetic.main.activity_video_chat.*
//import kotlinx.android.synthetic.main.activity_video_chat.view.*
//import okhttp3.OkHttpClient
//import org.json.JSONObject
//import org.webrtc.*
//import java.net.URISyntaxException
//import java.security.KeyManagementException
//import java.security.NoSuchAlgorithmException
//import java.security.cert.X509Certificate
//import javax.net.ssl.*
//
//
//class ReConnectActivity : AppCompatActivity() {
//
//    // const
//    private val CAPTURE_PERMISSION_REQUEST_CODE: Int = 1
//
//    // 전역변수
//    val TAG = "테스트하는소켓이다"
//    var room = "foo"
//    var socket : Socket? = null
//    var mediaProjectionPermissionResultData: Intent? = null
//    var mediaProjectionPermissionResultCode = 0
//    var encoderFactory: VideoEncoderFactory = SoftwareVideoEncoderFactory()
//    var decoderFactory: VideoDecoderFactory = SoftwareVideoDecoderFactory()
//
//    var peerConnection : PeerConnection? = null
//
//    // RTC
//    var eglBase = EglBase.create()
//    var remoteVideoSInk : ProxyVideoSink = ProxyVideoSink()
//    var remoteSinks : MutableList<VideoSink> = mutableListOf()
//    val remoteProxyRenderer: ProxyVideoSink = ProxyVideoSink()
//
//    var localSdp : SessionDescription? = null
//    val sdpObserver = SdpObserver_()
//    var queuedRemoteCandidates: MutableList<IceCandidate>? = null
//
//    // Media
//    var audioSource : AudioSource? = null
//    var audioConstraints = MediaConstraints() // 오디오
//    var localAudioTrack: AudioTrack? = null
//
//    var audioManager : AudioManager? = null
//
//    val event = NativePeerConnectionFactory {
//        Log.d("뭐지?","")
//        1000L
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_video_chat)
//
//        // camera initial
//        testinitialCam()
//    }
//
//
//
//   // 연결된 직후 바로 메소드 발송
//    fun callMessage(){
//        try{
//            var data = JSONObject().apply { put("create or join",room) }
//
//            socket?.run{
//                emit("create or join", data)
//                emit("room","hi")
//                Log.d(TAG, "오호호")
//            }?: kotlin.run {
//                Log.d(TAG, "socket is null")
//            }
//        }catch (e:Exception){
//            Log.d(TAG, "${e.message}")
//        }
//    }
//
//    // 메시지 전송
//    fun sendMessage(message:String){
//        socket?.run {
//            emit("message", message)
//        }
//    }
//
//
//    val cjListener = Emitter.Listener { coj -> callMessage() }
//    val msgListener = Emitter.Listener { msg -> callMessage() }
//    val errorListener = Emitter.Listener { error ->
//        error.forEachIndexed{ index, error_ ->
//            Log.d(TAG, "에러 : $error_")
//        }
//    }
//
//
//    override fun onDestroy() {
//        super.onDestroy()
//        socket?.run {
//            sendMessage("bye")
//            off(Socket.EVENT_CONNECT, cjListener)
//            off(Socket.EVENT_CONNECT_ERROR, errorListener)
//            off(Socket.EVENT_CONNECT_TIMEOUT, errorListener)
//            disconnect()
//        }
//    }
//
//    // 캠 초기화
//    fun testinitialCam(){
//        fullscreen_video_view?.apply {
//            init(eglBase.eglBaseContext, null)
//            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
//            setEnableHardwareScaler(false)
//
//            remoteVideoSInk.setTarget(fullscreen_video_view)
//            setMirror(false)
//        }
//        testInitialConnect()
//    }
//
//    fun testInitialConnect(){
//        // url , roomId 가지고 연결시도 --> "https://192.168.0.16:8889"
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            var mediaPrjectionManager : MediaProjectionManager = application.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
//            startActivityForResult( mediaPrjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE )
//        } else {
//            testStartCall()
//        }
//    }
//
//    fun testStartCall(){
//        var dataChannelParameters = DataChannelParameters(true, -1, -1, "", false, -1)
//        var  peerConnectionParameters = PeerConnectionParameters(
//            videoCallEnabled = true,
//            loopback = true,
//            tracing = false,
//            videoWidth = 0,
//            videoHeight = 0,
//            videoFps = 0,
//            videoMaxBitrate = 0,
//            videoCodec = "",
//            videoCodecHwAcceleration = true,
//            videoFlexfecEnabled = false,
//            audioStartBitrate = 0,
//            audioCodec = "",
//            noAudioProcessing = false,
//            aecDump = false,
//            saveInputAudioToFile = false,
//            useOpenSLES = false, disableBuiltInAEC = false,
//            disableBuiltInAGC = false, disableBuiltInNS = false,
//            disableWebRtcAGCAndHPF = false, enableRtcEventLog = false,
//            dataChannelParameters = dataChannelParameters
//        )
//
//        val fieldTrials: String = peerConnectionParameters.getFieldTrials()
//        PeerConnectionFactory.initialize(
//            PeerConnectionFactory.InitializationOptions.builder(applicationContext)
//                .setFieldTrials(fieldTrials)
//                .setEnableInternalTracer(true)
//                .createInitializationOptions()
//        )
//
//        val options = PeerConnectionFactory.Options()
//        var factory : PeerConnectionFactory = PeerConnectionFactory.builder()
//            .setOptions(options)
////            .setAudioDeviceModule(adm)
//            .setVideoEncoderFactory(encoderFactory)
//            .setVideoDecoderFactory(decoderFactory)
//            .createPeerConnectionFactory()
//
////        amd.release()
////        val rtcConfig = RTCConfiguration(signalingParameters.iceServers)
////        factory.createPeerConnection( rtcConfig, pcObserver )
//        peerConnection = PeerConnection( event )
////        val cvs = PeerConnectionFactory().createVideoSource(true)
//
////        remoteVideoSInk.
//        remoteSinks.add(remoteProxyRenderer)
////        remoteSinks.add( VideoFileRenderer("",100,100,eglBase.eglBaseContext) )
//
//
//        // 미디어
////        audioSource = factory.createAudioSource( audioConstraints )
////        localAudioTrack = factory.createAudioTrack(AUDIO_TRACK_ID, audioSource).apply {
////            setEnabled(false)
////        }
////
////        val mediaStreamLabels = listOf("ARDAMS")
////        peerConnection?.addTrack( localAudioTrack, mediaStreamLabels )
//
//        setAudioManager()   // 오디오 설정
//        testInitialSocket("https://192.168.0.16","8889") // 소켓
//
//    }
//
//    fun setAudioManager(){
//        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//        audioManager?.apply {
//            mode = AudioManager.MODE_IN_COMMUNICATION
//        }
//    }
//
//
//    // 소켓 초기화
//    fun testInitialSocket(
//        address : String,
//        port: String
//    ){
//        try {
//            val socketUrl = "$address:$port"
//            val hostnameVerifier: HostnameVerifier =
//                HostnameVerifier { hostname, session -> true }
//            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
//                    override fun checkClientTrusted(
//                        chain: Array<X509Certificate>,
//                        authType: String
//                    ) {
//                    }
//
//                    override fun checkServerTrusted(
//                        chain: Array<X509Certificate>,
//                        authType: String
//                    ) {
//                    }
//
//                    override fun getAcceptedIssuers(): Array<X509Certificate?> {
//                        return arrayOfNulls(0)
//                    }
//                })
//            val trustManager =
//                trustAllCerts[0] as X509TrustManager
//            val sslContext = SSLContext.getInstance("SSL")
//            sslContext.init(null, trustAllCerts, null)
//            val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory
//            val okHttpClient = OkHttpClient.Builder()
//                .hostnameVerifier(hostnameVerifier)
//                .sslSocketFactory(sslSocketFactory, trustManager)
//                .build()
//            val opts = IO.Options()
//            opts.callFactory = okHttpClient
//            opts.webSocketFactory = okHttpClient
//            socket = IO.socket(socketUrl, opts)
//            socket?.run {
//                on(Socket.EVENT_CONNECT, cjListener)
//                on(Socket.EVENT_CONNECT_ERROR, errorListener)
//                on(Socket.EVENT_CONNECT_TIMEOUT, errorListener)
//                connect()
//            }
//        } catch (e: URISyntaxException) {
//            throw RuntimeException(e)
//        } catch (e: NoSuchAlgorithmException) {
//            e.printStackTrace()
//        } catch (e: KeyManagementException) {
//            e.printStackTrace()
//        }
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if(requestCode!=CAPTURE_PERMISSION_REQUEST_CODE) { return } else {
//            mediaProjectionPermissionResultData = data
//            mediaProjectionPermissionResultCode = resultCode
//            testStartCall()
//        }
//    }
//
//
//    fun SdpObserver_() = object : SdpObserver {
//        override fun onCreateSuccess(origingSdp: SessionDescription?) {
//            localSdp?.let {
//                origingSdp?.run {
//
//                    var sdp = SessionDescription( type, this.description )
//                    localSdp = sdp
//                    peerConnection?.setLocalDescription(sdpObserver , sdp)
//                } ?: kotlin.run {
//                    Toast.makeText(this@ReConnectActivity, "알수없는 오류가 발생하였습니다", Toast.LENGTH_SHORT).show()
//                }
//            } ?: kotlin.run {
//                return
//            }
//        }
//        override fun onSetSuccess() {
//            peerConnection?.let {
//                // createOffer = true, createAnser = false
//                if(  )
//            } ?: kotlin.run {
//                return
//            }
//        }
//        override fun onSetFailure(p0: String?) {
//            TODO("Not yet implemented")
//        }
//        override fun onCreateFailure(p0: String?) {
//            TODO("Not yet implemented")
//        }
//    }
//
//    fun dd(){
//        var peer = PeerConnection(null)
//        peer.addIceCandidate()
//        peer.addStream()
//
//        peer.createOffer( object : SdpObserver{
//            override fun onSetFailure(p0: String?) {
//                TODO("Not yet implemented")
//            }
//
//            override fun onSetSuccess() {
//                TODO("Not yet implemented")
//            }
//
//            override fun onCreateSuccess(p0: SessionDescription?) {
//                TODO("Not yet implemented")
//            }
//
//            override fun onCreateFailure(p0: String?) {
//                TODO("Not yet implemented")
//            }
//
//        } , audioConstraints )
//    }
//
//
//}
//
//
//
