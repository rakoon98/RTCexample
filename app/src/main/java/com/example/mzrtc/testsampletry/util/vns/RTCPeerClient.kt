package com.example.mzrtc.testsampletry.util.vns

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.mzrtc.App
import com.example.mzrtc.testsampletry.TryRTCClient
import com.example.mzrtc.utils.setLogDebug
import org.webrtc.*

class RTCPeerClient(
    val context : Application,
    val observer : PeerConnection.Observer
) {

    companion object {
        const val LOCAL_STREAM_ID = "ARDAMSs0"
        const val VIDEO_TRACK_ID = "ARDAMSv0"
        const val AUDIO_TRACK_ID = "ARDAMSa0"
    }

    // coroutines channel
    val channel = App.coChannel.channel

    // eglbase
    private val rootEglbase : EglBase = EglBase.create()

    // ice Candidate stun server
    private val iceServer = listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())

    // peerConnections
    private val peerConnectionFactory by lazy { buildPeerConnectionFactory() }
    private val peerConnection by lazy { observer.buildPeerConnection() }

    private val videoCapturer by lazy { context.getVideoCapturer() }
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }
    private val constraints = MediaConstraints().apply { // mediaConstraints
        mandatory.add( MediaConstraints.KeyValuePair("OfferToReceiveVideo","true") )
        mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))
        optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
    }

    init {
        context.initPeerconnectionFactory()
    }

    // peerConnection initialize
    fun Application.initPeerconnectionFactory(  )  {
        val options =
            PeerConnectionFactory.InitializationOptions.builder(this)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()

        PeerConnectionFactory.initialize(options)
    }

    // connectionFactory build
    fun buildPeerConnectionFactory() : PeerConnectionFactory {
        val factory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglbase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(rootEglbase.eglBaseContext, true,true))
        // 오디오는 ???
            .setOptions(PeerConnectionFactory.Options().apply { networkIgnoreMask = 0 /** 인터넷 상태 관련 체크인듯. **/})
            .createPeerConnectionFactory()

        return factory
    }

    // peerConnnection build
    fun PeerConnection.Observer.buildPeerConnection() : PeerConnection? =
        peerConnectionFactory.createPeerConnection( PeerConnection.RTCConfiguration(iceServer).apply { enableDtlsSrtp = true } , this )

    // surfaceViewRender init view
    fun SurfaceViewRenderer.initSurfaceView() = kotlin.run {
        setMirror(true)
        setEnableHardwareScaler(true)
        init( rootEglbase.eglBaseContext, null )
    }

    // initialize video capturer
    fun Context.getVideoCapturer() =
        Camera2Enumerator(this).run {
            deviceNames.find { dn ->
                isFrontFacing(dn)
            } ?. let { c2e ->
                createCapturer( c2e , null )
            } ?: throw IllegalStateException()
        }

    // start local video capturer
    fun SurfaceViewRenderer.startLocalVideoCapture(){
        var surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, rootEglbase.eglBaseContext)
        (videoCapturer as VideoCapturer).initialize(surfaceTextureHelper, this.context, localVideoSource.capturerObserver)
        videoCapturer.startCapture(240, 240, 60)

        val localVideoTrack = peerConnectionFactory.createVideoTrack(TryRTCClient.VIDEO_TRACK_ID, localVideoSource)
        localVideoTrack.addSink(this)

        val localAudioTrack = peerConnectionFactory.createAudioTrack(TryRTCClient.AUDIO_TRACK_ID, localAudioSource )
        localAudioTrack.setEnabled(true)

        val localStream = peerConnectionFactory.createLocalMediaStream(TryRTCClient.LOCAL_STREAM_ID)
        localStream.addTrack(localVideoTrack)
        localStream.addTrack(localAudioTrack)

        peerConnection?.addStream(localStream) ?: run{
            Log.d("요호호","peerconnection null")
        }
    }

    // peerConnection createOffer with sdpObserver
    private fun PeerConnection.call( sdpObserver: SdpObserver ){
        createOffer( object : SdpObserver by sdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                setLocalDescription( object: SdpObserver {
                    override fun onSetFailure(p0: String?) {}
                    override fun onSetSuccess() {}
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, sessionDescription )
                setLogDebug("onCreateSuccess Offer")
                sdpObserver.onCreateSuccess(sessionDescription) // 왜여기일까.
            }
        }, constraints)
    }

    // peerConnection createAnser with sdpObserver
    private fun PeerConnection.answer(sdpObserver: SdpObserver){
        createAnswer( object: SdpObserver by sdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                setLocalDescription( object : SdpObserver {
                    override fun onSetFailure(p0: String?) {}
                    override fun onSetSuccess() {}
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                },sessionDescription)
                sdpObserver.onCreateSuccess(sessionDescription) // 예제에는 이게 없었따.. 테스트해보
            }
        },constraints)
    }

    // sessionDescription to setRemoteDescription
    fun onRemoteSessionReceived( sessionDescription: SessionDescription ){
        peerConnection?.setRemoteDescription( object : SdpObserver {
            override fun onSetFailure(p0: String?) {}
            override fun onSetSuccess() {}
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, sessionDescription )
    }

    // call to method ( call or answer )
    fun SdpObserver.call() = peerConnection?.run {  call(sdpObserver = this@call) }
    fun SdpObserver.answer() = peerConnection?.run {  answer(sdpObserver = this@answer) }

    // peerConnection Add IceCandidates
    fun addIceCandidate(iceCandidate: IceCandidate?) =
        iceCandidate?.let { candidate ->
            peerConnection?.addIceCandidate(candidate)
        }


}