package com.example.mzrtc.testsampletry.view

//import me.amryousef.webrtc_demo.MainActivity
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.example.mzrtc.App
import com.example.mzrtc.MRtcManager
import com.example.mzrtc.R
import com.example.mzrtc.testsampletry.TryRTCClient
import com.example.mzrtc.testsampletry.TrySignalingClient
import com.example.mzrtc.testsampletry.data.*
import com.example.mzrtc.testsampletry.util.NetworkUtil.getConnectivityStatusString
import com.example.mzrtc.testsampletry.util.NetworkUtil.isNetworkAvailable
import com.example.mzrtc.utils.setLogDebug
import kotlinx.android.synthetic.main.activity_main.*

open class TryActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
        private const val AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO
        private val PERMISSION_RTC = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

//    open lateinit var rtcClient: TryRTCClient
//    open lateinit var signalingClient : TrySignalingClient

//    var rtcClient : TryRTCClient? = null
//    var signalingClient : TrySignalingClient? = null
////    private lateinit var signalingClient : SignallingClient
//
//
//    val recevice = CoroutineScope(Dispatchers.Main).async {
//        val receiveData = App.coChannel.channel.asFlow()
//        receiveData.collect {  data -> // 받은 아이들을 수집하여 그것을 진행한다.
//            setLogDebug( "data is  : $data " )
//            when( data ) {
//                CREATE_OFFER -> {
////                    setLogDebug( "$CREATE_OFFER : 실행함 " )
////                    Toast.makeText(this@TryActivity, "createoffer",Toast.LENGTH_SHORT).show()
////                    rtcClient?.call(sdpObserver)
//                }
//            }
//        }
//    }
//
//    open val sdpObserver = object: AppSdpObserver(){
//        override fun onCreateSuccess(p0: SessionDescription?) {
//            super.onCreateSuccess(p0)
//            Log.d("요호호","가즈아 sdpObserver onCreateSuccess: ${p0?.type}")
//            signalingClient?.send(p0)
//        }
//    }

    var mRtcManager : MRtcManager? = null
    var url :String = ""
    var port:String = ""
    var roomId :String = ""

    val audioManager by lazy {
        (applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager).apply {
//            AudioAttributes.USAGE_MEDIA
//            mode = AudioManager.FLAG_SHOW_UI
//            mode = AudioManager.FLAG_ALLOW_RINGER_MODES
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mRtcManager = MRtcManager()
        intent?.let {
            url =  it.getStringExtra(INTENT_URL)
            port =  it.getStringExtra(INTENT_PORT)
            roomId =  it.getStringExtra(INTENT_ROOM)
        }

        connectBtn.setOnClickListener {
            var room_ = roomName.text.toString()
            if( url.isEmpty() || port.isEmpty() || roomId.isEmpty() ) {
                Toast.makeText(this@TryActivity, "정보가 넘어오지 않았습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                mRtcManager?.run { startPeer(local_view, remote_view, url, port, room_) }
            }
        }
        disConnectBtn.setOnClickListener {
            mRtcManager?.run { stopPeer() }
        }
        soundBtn.setOnCheckedChangeListener { _, isChecked ->
            when(isChecked){
                true->{ audioManager.isSpeakerphoneOn = true }
                false->{ audioManager.isSpeakerphoneOn = false }
            }
        }
        observeData()
    }


//    // 카메라 권한
//    private fun checkCameraPermission(){
//        if(
//            ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) != PackageManager.PERMISSION_GRANTED &&
//            ContextCompat.checkSelfPermission(this, AUDIO_PERMISSION) != PackageManager.PERMISSION_GRANTED
//        ){
//            requestCameraPermission()
//        } else {
//            Log.d("요호호","가즈아")
//            onCameraPermissionGranted()
//        }
//    }
//
//    fun onCameraPermissionGranted(){
//        rtcClient = TryRTCClient(application, peerObserver)
//        rtcClient?.run {
//            // 나와 상대방 서피스뷰 초기화
//            remote_view.initSurfaceView()
//            local_view.initSurfaceView()
//            local_view.startLocalVideoCapture()
//            signalingClient = TrySignalingClient(createSignallingClientListener())
////            signalingClient = SignallingClient( createSignallingClientListener() )
////            call_button.setOnClickListener { call(sdpObserver) }
//            Log.d("요호호","가즈아2")
//        }
//    }
//
//    // 시그널링 클라이언트 리스너 생성 !!
//    private fun createSignallingClientListener() = object : TrySignallingClientListener {
//        override fun reConnected() {
//            rtcClient?.observer?.onRenegotiationNeeded()
//        }
//        override fun onConnectionEstablished() {
//            Log.d("요호호","Try:onConnectionEstablished ")
////            call_button.isClickable = true
//        }
//
//        override fun onOfferReceived(description: SessionDescription) {
//            Log.d("요호호","Try:onOfferRecived : $description")
//            rtcClient?.onRemoteSessionReceived(description)
//            rtcClient?.answer(sdpObserver)
//            remote_view_loading.visibility = View.GONE
//        }
//
//        override fun onAnswerReceived(description: SessionDescription) {
//            Log.d("요호호","Try:onAnswerReceived : $description")
//            rtcClient?.onRemoteSessionReceived(description)
//            remote_view_loading.visibility = View.GONE
//        }
//
//        override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
//            Log.d("요호호","Try:onIceCandidateReceived : $iceCandidate")
//            rtcClient?.addIceCandidate(iceCandidate)
//        }
//    }

//    /** Camera Permission Request Granted & Denied  **/
//    private fun requestCameraPermission(dialogShown: Boolean = false) {
//        if (
//            ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA_PERMISSION) &&
//            ActivityCompat.shouldShowRequestPermissionRationale(this, AUDIO_PERMISSION) &&
//            !dialogShown
//        ) {
//            showPermissionRationaleDialog()
//        } else {
//            ActivityCompat.requestPermissions(this, PERMISSION_RTC, CAMERA_PERMISSION_REQUEST_CODE)
//        }
//    }
//
//
//    private fun showPermissionRationaleDialog() {
//        AlertDialog.Builder(this)
//            .setTitle("Camera Permission Required")
//            .setMessage("This app need the camera to function")
//            .setPositiveButton("Grant") { dialog, _ ->
//                dialog.dismiss()
//                requestCameraPermission(true)
//            }
//            .setNegativeButton("Deny") { dialog, _ ->
//                dialog.dismiss()
//                onCameraPermissionDenied()
//            }
//            .show()
//    }
//    private fun onCameraPermissionDenied() { Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_LONG).show() }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if(requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.all { it==PackageManager.PERMISSION_GRANTED }){
//            onCameraPermissionGranted()
//        } else {
//            onCameraPermissionDenied()
//        }
//    }
//
////    var mReceiver = NetworkChangeReceiver()
//    var mReceiver = object: BroadcastReceiver(){
//        override fun onReceive(context: Context?, intent: Intent?) {
//            setLogDebug("${intent?.action}")
//        }
//    }
//
//    fun registerReceiver(){
//        var theFilter = IntentFilter().apply {
//            addAction("android.net.conn.CONNECTIVITY_CHANGE")
//            addAction("android.net.wifi.WIFI_STATE_CHANGED")
//        }
//
//        registerReceiver(mReceiver, theFilter)
//
//        Handler(mainLooper).postDelayed ({
//            val intent = Intent("android.net.conn.CONNECTIVITY_CHANGE")
//            sendBroadcast(intent)
//        } , 3000)
//    }
//
//    fun unRegisterReceiver(){
//        mReceiver?.let {
//            unregisterReceiver(mReceiver)
//        }
//    }

    override fun onResume() {
        super.onResume()
//        Handler(mainLooper).postDelayed ({
////            val intent = Intent("android.net.conn.CONNECTIVITY_CHANGE")
////            sendBroadcast(intent)
////        } , 3000)
//        registerReceiver()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            registerCallback()
//        }
    }

    override fun onPause() {
        super.onPause()
//        unRegisterReceiver()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            unRegisterCallbak()
//        }

        mRtcManager?.run { stopPeer() }
    }

//    override fun onDestroy() {
//        super.onDestroy()
////        signalingClient?.onDestroy()
////        unRegisterReceiver()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            unRegisterCallbak()
//        }
//    }

//    var connectivyCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//        object : ConnectivityManager.NetworkCallback() {
//            @RequiresApi(Build.VERSION_CODES.M)
//            override fun onAvailable(network: Network) {
//                super.onAvailable(network)
//                var networkName = getConnectivityStatusString(applicationContext)
//                var networkStatus = isNetworkAvailable(applicationContext)
//                setLogDebug("네트워크[onAvailable-$networkStatus] : $networkName")
//            }
//            @RequiresApi(Build.VERSION_CODES.M)
//            override fun onLost(network: Network) {
//                super.onLost(network)
//                var networkName = getConnectivityStatusString(applicationContext)
//                var networkStatus = isNetworkAvailable(applicationContext)
//                setLogDebug("네트워크[onLost-$networkStatus] : $networkName")
//            }
//        }
//    } else {
////        TODO("VERSION.SDK_INT < LOLLIPOP")
//        null
//    }

//    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
//    fun registerCallback(){
//        var builder = NetworkRequest.Builder()
//        var connectivyManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        connectivyManager.registerNetworkCallback(builder.build(), connectivyCallback)
//    }
//    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
//    fun unRegisterCallbak(){
//        var connectivyManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        connectivyManager.unregisterNetworkCallback(connectivyCallback)
//    }

//    val peerObserver = object : PeerConnectionObserver() {
//        override fun onIceCandidate(p0: IceCandidate?) {
//            super.onIceCandidate(p0)
//            setLogDebug("Try:onIceCandidate : ?? : $p0")
//            signalingClient?.send(p0)
//            rtcClient?.addIceCandidate(p0)
//        }
//
//        override fun onAddStream(mediaStream: MediaStream?) {
//            super.onAddStream(mediaStream)
//            setLogDebug("Try:onAddStream : ?? : $mediaStream")
//            mediaStream?.videoTracks?.get(0)?.addSink(remote_view)
////                        mediaStream?.audioTracks?.get(0).
//        }
//        override fun onRenegotiationNeeded() {
//            super.onRenegotiationNeeded()
//            setLogDebug("Try:onRenegotiationNeeded : ??")
//        }
//
//        override fun onDataChannel(dc: DataChannel?) {
//            setLogDebug("onDataChannel : $dc")
//
//            dc?.registerObserver(object : DataChannel.Observer {
//                override fun onBufferedAmountChange(previousAmount: Long) {
//                    setLogDebug("Data channel buffered amount changed: " + dc?.label() + ": " + dc?.state())
//                }
//
//                override fun onStateChange() {
//                    setLogDebug("Data channel state changed: " + dc?.label() + ": " + dc?.state())
//                }
//
//                override fun onMessage(buffer: DataChannel.Buffer) {
//                    if (buffer.binary) {
//                        setLogDebug("Received binary msg over $dc")
//                        return
//                    }
//                    val data = buffer.data
//                    val bytes = ByteArray(data.capacity())
//                    data[bytes]
//                    val strData =
//                        String(bytes, Charset.forName("UTF-8"))
//                    setLogDebug("Got msg: $strData over $dc")
//                }
//            })
//        }
//        override fun onIceConnectionReceivingChange(p0: Boolean) {
//            setLogDebug("onIceConnectionReceivingChange : $p0")
//        }
//        override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
//            setLogDebug("onIceConnectionChange : $p0")
//        }
//        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
//            setLogDebug("onIceGatheringChange : $p0")
//        }
//        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
//            setLogDebug("onSignalingChange : $p0")
//        }
//        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
//            setLogDebug("onIceCandidatesRemoved : $p0")
//        }
//        override fun onRemoveStream(p0: MediaStream?) {
//            setLogDebug("onRemoveStream : $p0")
//        }
//        override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
//            setLogDebug("onAddTrack : $p0")
//        }
//    }


    /** observer to liveData **/
    fun observeData(){
        mRtcManager?.run {
            progressLD.observe(this@TryActivity, Observer {
                when(it){
                    true->{ remote_view_loading.visibility = View.VISIBLE }
                    false->{ remote_view_loading.visibility = View.GONE }
                }
            })
        }
    }
}






