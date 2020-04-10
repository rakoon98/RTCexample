package com.example.mzrtc

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.example.mzrtc.testsampletry.TryRTCClient
import com.example.mzrtc.testsampletry.TrySignalingClient
import com.example.mzrtc.testsampletry.data.*
import com.example.mzrtc.utils.setLogDebug
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import me.amryousef.webrtc_demo.AppSdpObserver
import me.amryousef.webrtc_demo.PeerConnectionObserver
import me.amryousef.webrtc_demo.TrySignallingClientListener
import org.webrtc.*
import java.nio.charset.Charset

class MRtcManager(

) {
    var progressLD  = MutableLiveData<Boolean>().apply{ value = true }  // 프로그레스바 연동
    var isStarted : Boolean = false  //  시작했는지의 여부

    var rtcClient : TryRTCClient? = null
    var signalingClient : TrySignalingClient? = null

    var localSurfaceview: SurfaceViewRenderer? = null
    var remoteSurfaceview: SurfaceViewRenderer? = null

    /** Coroutines Receive Data Channel **/
    val recevice = CoroutineScope(Dispatchers.Main).async {
        val receiveData = App.coChannel.channel.asFlow()
        receiveData.collect {  data -> // 받은 아이들을 수집하여 그것을 진행한다.
            setLogDebug( "receive_Data_in_Manager  : $data " )
            when( data ) {
                is String -> {
                    when(data){
                        GOT_USER_MEDIA ->  { rtcClient?.run { call(sdpObserver) } }
                        BYE -> {
                            stopPeer()
//                            rtcClient?.run { /** rtcClient 자원해제 하는 부분 추가 **/ }
//                            signalingClient?.run { onDestroy() }
                        }
                    }
                }
                else -> {
                    when(data){
                        CREATE_OFFER -> {
                            setLogDebug("CREATE_OFFER")
                            rtcClient?.call(sdpObserver)}
                        }
                    }
                }
        }
    }

    /** SdpObserver : SessionDescription send to signalingClient  **/
    open val sdpObserver = object: AppSdpObserver(){
        override fun onCreateSuccess(p0: SessionDescription?) {
            super.onCreateSuccess(p0)

            setLogDebug("가즈아 sdpObserver onCreateSuccess: ${p0?.type}")
            signalingClient?.send(p0)
        }
    }

    /** PeerConnectionObserver : use in RtcClient ( PeerConnection Managing ) **/
    val peerObserver = object : PeerConnectionObserver() {
        override fun onIceCandidate(p0: IceCandidate?) {
            super.onIceCandidate(p0)

            setLogDebug("[PEERCONNECTION_OBSERVER]Try:onIceCandidate : ?? : $p0")
            signalingClient?.send(p0)
            rtcClient?.addIceCandidate(p0)
        }

        override fun onAddStream(mediaStream: MediaStream?) {
            super.onAddStream(mediaStream)
            setLogDebug("[PEERCONNECTION_OBSERVER]Try:onAddStream : ?? : $mediaStream")

            // videoTrack 추가
//            mediaStream?.videoTracks?.get(0)?.addSink(remoteSurfaceview)
            remoteSurfaceview?.let {
                setLogDebug("[PEERCONNECTION_OBSERVER]Try:onAddStream : ?? : surfaceView is have")
                mediaStream?.videoTracks?.get(0)?.addSink(it)
            } ?: kotlin.run {
                setLogDebug("[PEERCONNECTION_OBSERVER]Try:onAddStream : ?? : surfaceView is null")
            }
            // mediaStream?.audioTracks?.get(0).  // ???? 오디오 트랙만 넣는 방법?
        }
//        override fun onRenegotiationNeeded() {
//            super.onRenegotiationNeeded()
//            setLogDebug("[PEERCONNECTION_OBSERVER]Try:onRenegotiationNeeded : ??")
//        }
//
//        override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
//            super.onStandardizedIceConnectionChange(newState)
//            setLogDebug("[PEERCONNECTION_OBSERVER]Try:onStandardizedIceConnectionChange : $newState")
//        }
//
//        override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent?) {
//            super.onSelectedCandidatePairChanged(event)
//            setLogDebug("[PEERCONNECTION_OBSERVER]Try:onSelectedCandidatePairChanged : $event")
//        }

//        override fun onDataChannel(dc: DataChannel?) {
//            setLogDebug("[PEERCONNECTION_OBSERVER]onDataChannel : $dc")
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
//            setLogDebug("[PEERCONNECTION_OBSERVER]onIceConnectionReceivingChange : $p0")
//        }
//        override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
//            setLogDebug("[PEERCONNECTION_OBSERVER]onIceConnectionChange : $p0")
//        }
//        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
//            setLogDebug("[PEERCONNECTION_OBSERVER]onIceGatheringChange : $p0")
//        }
//        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
//            setLogDebug("[PEERCONNECTION_OBSERVER]onSignalingChange : $p0")
//        }
//        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
//            setLogDebug("[PEERCONNECTION_OBSERVER]onIceCandidatesRemoved : $p0")
//        }
//        override fun onRemoveStream(p0: MediaStream?) {
//            setLogDebug("[PEERCONNECTION_OBSERVER]onRemoveStream : $p0")
//        }
//        override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
//            setLogDebug("[PEERCONNECTION_OBSERVER]onAddTrack : $p0")
//        }
    }

   /** Signaling listener **/
    val signalingClientListener = object : TrySignallingClientListener {
        override fun reConnected() {
            rtcClient?.observer?.onRenegotiationNeeded()
        }
        override fun onConnectionEstablished() {
            Log.d("요호호","Try:onConnectionEstablished ")
//            call_button.isClickable = true
        }

        override fun onOfferReceived(description: SessionDescription) {
            Log.d("요호호","Try:onOfferRecived : $description")
            rtcClient?.onRemoteSessionReceived(description)
            rtcClient?.answer(sdpObserver)
            progressLD.postValue(false)
        }

        override fun onAnswerReceived(description: SessionDescription) {
            Log.d("요호호","Try:onAnswerReceived : $description")
            rtcClient?.onRemoteSessionReceived(description)
            progressLD.postValue(false)
        }

        override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
            Log.d("요호호","Try:onIceCandidateReceived : $iceCandidate")
            rtcClient?.peerConnection?.run {
                setLogDebug("senders : $senders  //  receivers : $receivers")
            }

            rtcClient?.addIceCandidate(iceCandidate)
        }
    }

    /**
     * Connect socket & peerConnection
     * Initialize to PeerConnection ( RTCClient ) & Socket ( SignalingClient )
     **/
    fun Context.startPeer(
        local_view : SurfaceViewRenderer,
        remote_view : SurfaceViewRenderer,
        url:String,port:String,roomId:String
    ){
        rtcClient = TryRTCClient(this, peerObserver)
        rtcClient?.run {
            // 나와 상대방 서피스뷰 초기화
            if(localSurfaceview==null){
                localSurfaceview = local_view.apply {
                    initSurfaceView()
                    startLocalVideoCapture()
                }
            }
            if(remoteSurfaceview==null){
                remoteSurfaceview = remote_view.apply {
                    initSurfaceView()
                }
            }

            // 시그널(소켓) 클라이언트 초기화
            signalingClient = TrySignalingClient(
                signalingClientListener,
                url,port,roomId
            )
        }
    }

    /**
     * disConnect socket & peerConnection
     * close to PeerConnection ( RTCClient ) & Socket ( SignalingClient )
     **/
    fun stopPeer(){
        remoteSurfaceview?.run {
                release()
                remoteSurfaceview = null
            }
            localSurfaceview?.run {
                release()
                localSurfaceview = null
        }

        rtcClient?.run {
            disConnect()
            rtcClient = null
        }
        signalingClient?.run {
            onDestroy()
            signalingClient = null
        }
    }

}