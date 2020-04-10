package com.example.mzrtc.testsampletry.util.vns

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.mzrtc.testsampletry.TryRTCClient
import com.example.mzrtc.utils.setLogDebug
import org.webrtc.*
import java.lang.IllegalStateException

class RTCPeerClient2(
    val context : Application,
    val observer : PeerConnection.Observer
) {

    private val rootEglbase = EglBase.create()

    // ice Candidate stun server
    private val iceServer = listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())


    private var peerConnectionFactory : PeerConnectionFactory? = null
    private var peerConnection : PeerConnection? = null

    private var surfaceTextureHelper : SurfaceTextureHelper? = null
    private var videoCapturer : VideoCapturer? = null
    private var localVideoSource : VideoSource? = null
    private var localAudioSource : AudioSource? = null
    private var localVideoTrack : VideoTrack? = null
    private var localAudioTrack : AudioTrack? = null
    private var localStream : MediaStream? = null

    private val constraints = MediaConstraints().apply { // mediaConstraints
        mandatory.add( MediaConstraints.KeyValuePair("OfferToReceiveVideo","true") )
        mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))
        optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
    }

    init {
        peerConnection = observer.buildPeerConnection()
        context.initPeerConnectionFactory()
    }

    fun Application.initPeerConnectionFactory(){
        val options =
            PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions()

        PeerConnectionFactory.initialize(options)

        peerConnectionFactory = observer.buildPeerConnectionFactory()
    }

    // connectionFactory build
    fun PeerConnection.Observer.buildPeerConnectionFactory() : PeerConnectionFactory {
        val factory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglbase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(rootEglbase.eglBaseContext, true,true))
            // 오디오는 ???
            .setOptions(PeerConnectionFactory.Options().apply { networkIgnoreMask = 0 /** 인터넷 상태 관련 체크인듯. **/})
            .createPeerConnectionFactory()

        localVideoSource = factory.createVideoSource(false)
        localAudioSource = factory.createAudioSource(MediaConstraints())

        return factory
    }


    // peerConnnection build
    fun PeerConnection.Observer.buildPeerConnection() : PeerConnection? =
        peerConnectionFactory?.createPeerConnection( PeerConnection.RTCConfiguration(iceServer).apply { enableDtlsSrtp = true } , this )



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
        surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, rootEglbase.eglBaseContext)
        videoCapturer?.run {
            initialize(surfaceTextureHelper, context, localVideoSource?.capturerObserver)
            startCapture(240, 240, 60)
        } ?: kotlin.run {
            videoCapturer = context.getVideoCapturer()
            (videoCapturer as VideoCapturer).run {
                initialize(surfaceTextureHelper, context, localVideoSource?.capturerObserver)
                startCapture(240, 240, 60)
            }
        }



        localVideoTrack = peerConnectionFactory?.createVideoTrack(TryRTCClient.VIDEO_TRACK_ID, localVideoSource)?.apply {
            addSink(this@startLocalVideoCapture)
        }

        localAudioTrack = peerConnectionFactory?.createAudioTrack(TryRTCClient.AUDIO_TRACK_ID, localAudioSource )?.apply {
            setEnabled(true)
        }

        localStream = peerConnectionFactory?.createLocalMediaStream(TryRTCClient.LOCAL_STREAM_ID)?.apply {
            addTrack(localVideoTrack)
            addTrack(localAudioTrack)
        }

        peerConnection?.addStream(localStream)
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
    fun SdpObserver.call() = peerConnection?.run {  call(sdpObserver = this@call) }?: kotlin.run {
        peerConnection = observer.buildPeerConnection()
        setLogDebug("뚱 : peerConnection is null")
        peerConnection!!.call(this@call)
    }
    fun SdpObserver.answer() = peerConnection?.run {  answer(sdpObserver = this@answer) }

    // peerConnection Add IceCandidates
    fun addIceCandidate(iceCandidate: IceCandidate?) =
        iceCandidate?.let { candidate ->
            peerConnection?.addIceCandidate(candidate)
        }



    fun destroy(){
        if(peerConnection!=null){
            peerConnection!!.dispose()
            peerConnection = null
        }

        if(localAudioSource!=null){
            localAudioSource!!.dispose()
            localAudioSource = null
        }

        if(videoCapturer!=null){
            try{
                videoCapturer!!.stopCapture()
            }catch (e:Exception){
                setLogDebug("비디오 캡처 스탑 에러 : $e")
            }
            videoCapturer!!.dispose()
            videoCapturer = null
        }

        if(localVideoSource!=null){
            localVideoSource!!.dispose()
            localVideoSource = null
        }

        if(surfaceTextureHelper!=null){
            surfaceTextureHelper!!.dispose()
            surfaceTextureHelper = null
        }

        if(peerConnectionFactory!=null){
            peerConnectionFactory!!.dispose()
            peerConnectionFactory = null
        }

        PeerConnectionFactory.stopInternalTracingCapture()
        PeerConnectionFactory.shutdownInternalTracer()
    }


}