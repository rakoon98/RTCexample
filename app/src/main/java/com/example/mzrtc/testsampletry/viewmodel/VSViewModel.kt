package com.example.mzrtc.testsampletry.viewmodel

import android.app.Application
import android.view.Surface
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mzrtc.App
import com.example.mzrtc.testsampletry.data.ANSWER
import com.example.mzrtc.testsampletry.data.CREATE_OFFER
import com.example.mzrtc.testsampletry.data.OFFER
import com.example.mzrtc.testsampletry.data.SessionDescriptionsType
import com.example.mzrtc.testsampletry.util.vns.RTCPeerClient
import com.example.mzrtc.testsampletry.util.vns.RTCSignalingClient
import com.example.mzrtc.utils.setLogDebug
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import me.amryousef.webrtc_demo.AppSdpObserver
import me.amryousef.webrtc_demo.PeerConnectionObserver
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer

/**
 *  1. Voice Screen 역할을 하는 ViewModel
 */
class VSViewModel(
    val context: Application,
    val url : String,
    val port : String,
    val roomId: String
) : ViewModel() {

    val coChannel = App.coChannel

    open lateinit var peerClient: RTCPeerClient
    open lateinit var signalingClient : RTCSignalingClient

    var progressStatus : MutableLiveData<Boolean> = MutableLiveData<Boolean>().apply{ value = true }

    val recevice = CoroutineScope(Dispatchers.Main).async {
        val receiveData = coChannel.channel.asFlow()
        receiveData.collect {  data -> // 받은 아이들을 수집하여 그것을 진행한다.
            setLogDebug("receive Data : $data")
            when( data ) {
                CREATE_OFFER -> {
                    setLogDebug("create offer")
                    peerClient.run{ sdpObserver.call() }
                }
                is SessionDescriptionsType -> {
                    when( data.type ){
                        OFFER -> {
                            setLogDebug("get sd offer")
                            peerClient.run { onRemoteSessionReceived(data.description);  sdpObserver.answer() }
                            progressStatus.postValue(false)
                        }
                        ANSWER -> {
                            peerClient.run { onRemoteSessionReceived(data.description) }
                            progressStatus.postValue(false)
                        }
                    }
                }
                is IceCandidate -> {
                    peerClient.run { addIceCandidate(data) }
                }
            }
        }
    }

    open val sdpObserver = object: AppSdpObserver(){
        override fun onCreateSuccess(p0: SessionDescription?) {
            super.onCreateSuccess(p0)
            p0?.let {
                setLogDebug("onCreateSuccess")
                signalingClient.socketOnListener?.sendRTCInfo(it)
            }
        }
    }

    // 카메라 권한 습득시
    fun onCameraPermissionGranted(){
        peerClient =
            RTCPeerClient( context,
                object : PeerConnectionObserver() {
                    override fun onIceCandidate(p0: IceCandidate?) {
                        super.onIceCandidate(p0)
                        p0?.let {
                            setLogDebug("onIceCandidate : $p0")
                            signalingClient.socketOnListener?.sendRTCInfo(it)
                            peerClient.addIceCandidate(it)
                        }
                    }

                    override fun onAddStream(mediaStream: MediaStream?) {
                        super.onAddStream(mediaStream)
                        coChannel.run{ sendMediaStream(mediaStream) }
                    }
                })

        peerClient.apply {
            // 나와 상대방 서피스뷰 초기화
            coChannel.run {
                runMain { sendString("initView") }
            }
            signalingClient = RTCSignalingClient(
                url = url, port = port, roomId = roomId
            )
        }

    }

    // render initial
    fun setInitRender(
        local : SurfaceViewRenderer,
        remote : SurfaceViewRenderer
    ){
        peerClient.run {
            local.initSurfaceView()
            local.startLocalVideoCapture()

            remote.initSurfaceView()
        }
    }

    fun destroyPeerAndSocket(){
        signalingClient.destroy()
    }

}