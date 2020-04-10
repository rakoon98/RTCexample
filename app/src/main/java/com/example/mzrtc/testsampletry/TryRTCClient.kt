package com.example.mzrtc.testsampletry

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.mzrtc.utils.setLogDebug
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.PeerConnection.RTCConfiguration
import org.webrtc.audio.AudioDeviceModule


class TryRTCClient(
    var context: Context,
    var observer : PeerConnection.Observer
) {

    companion object {
        const val LOCAL_STREAM_ID = "ARDAMSs0"
        const val VIDEO_TRACK_ID = "ARDAMSv0"
        const val AUDIO_TRACK_ID = "ARDAMSa0"
//        private const val LOCAL_TRACK_ID = "local_track"
    }

    // eglbase
    private val rootEglbase : EglBase = EglBase.create()

    // ice Candidate stun server
    private val iceServer = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
            .createIceServer()
    )

    // rtc
    private val peerConnectionFactory by lazy { buildPeerConnectionFactory() }
    private val videoCapturer by lazy { context.getVideoCapturer() }
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }
    val peerConnection : PeerConnection? by lazy { observer.buildPeerConnection() } // peerConnection can return null -> nativeObserver if 0L



    init{
        context.initPeerconnectionFactory()
    }

    // peerConnection initialize
    fun Context.initPeerconnectionFactory(  )  {
        val options = PeerConnectionFactory.InitializationOptions.builder(this)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()

        PeerConnectionFactory.initialize(options)
    }

    // connectionFactory build
    fun buildPeerConnectionFactory() : PeerConnectionFactory {
        var audioModule = object: AudioDeviceModule {
            override fun setMicrophoneMute(p0: Boolean) {}
            override fun setSpeakerMute(p0: Boolean) {}
            override fun getNativeAudioDeviceModulePointer(): Long {return 0L}
            override fun release() {}
        }


        var factory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglbase.eglBaseContext))
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(
                    rootEglbase.eglBaseContext,
                    true,
                    true
                )
            )
            .setOptions(
                PeerConnectionFactory.Options().apply {
                    networkIgnoreMask = 0
//                    disableEncryption = true
//                    disableNetworkMonitor = true
                }
            )
//            .setOptions(PeerConnectionFactory.Options().apply {
//                disableEncryption = true
//                disableNetworkMonitor = true
//            })
            .setAudioDeviceModule( audioModule )
            .createPeerConnectionFactory()


//        // Media Constraint
//        // Media Constraint
//        var name = "media"
//
//        val mediaStream: MediaStream = factory.createLocalMediaStream("MediaStream-$name")
//
//        // Audio
//        // Audio
//        val audioSource: AudioSource = factory.createAudioSource(MediaConstraints())
//        mediaStream.addTrack(factory.createAudioTrack("Audio-$name", audioSource))

        audioModule.release()
        return factory
    }

    // peerConnection build
    fun PeerConnection.Observer.buildPeerConnection() : PeerConnection? {
        val rtcConfig = RTCConfiguration(iceServer).apply {
            enableDtlsSrtp = true
//            sdpSemantics = PeerConnection.SdpSemantics.PLAN_B
        }

//        val rtcConfig = RTCConfiguration(iceServer)
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
//        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
//        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
//        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
//        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        // Use ECDSA encryption.
        // Use ECDSA encryption.
//        rtcConfig.keyType = PeerConnection.KeyType.ECDSA
        // Enable DTLS for normal calls and disable for loopback calls.
        // Enable DTLS for normal calls and disable for loopback calls.
//        rtcConfig.enableDtlsSrtp = true
//        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        return peerConnectionFactory.createPeerConnection( rtcConfig, this )
    }

    // surfaceViewRenderer init view
    fun SurfaceViewRenderer.initSurfaceView() = this.run {
        setMirror(true)
        setEnableHardwareScaler(true)
        init( rootEglbase.eglBaseContext, null)
    }

    // initialize video capturer
    private fun Context.getVideoCapturer()=
        if(Camera2Enumerator.isSupported(this)){
            Camera2Enumerator(this).run {
                deviceNames.find {  dn->
                    isFrontFacing(dn)
                } ?. let { c2e ->
                    createCapturer( c2e, null)
                } ?: throw IllegalStateException()
            }
        }else{
            Camera1Enumerator(false).run {
                deviceNames.find {  dn->
                    isFrontFacing(dn)
                } ?. let { c2e ->
                    createCapturer( c2e, null)
                } ?: throw IllegalStateException()
            }
        }


    // 로컬 비디오 캡쳐 시---작---- 하겠습니다 -------!!!! 뻐-- 바 빰빠 빰빠 빰 빠빠!
    fun SurfaceViewRenderer.startLocalVideoCapture(){
        var surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, rootEglbase.eglBaseContext)
        (videoCapturer as VideoCapturer).initialize(surfaceTextureHelper, this.context, localVideoSource.capturerObserver)
        videoCapturer.startCapture(240, 240, 60)

        val localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource)
        localVideoTrack.addSink(this)

        val localAudioTrack = peerConnectionFactory.createAudioTrack( AUDIO_TRACK_ID, localAudioSource )
        localAudioTrack.setEnabled(true)

        val localStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID)
        localStream.addTrack(localVideoTrack)
        localStream.addTrack(localAudioTrack)

        peerConnection?.addStream(localStream) ?: run{
            Log.d("요호호","peerconnection null")
        }
    }


    // peerConnection to call  ??
    private fun PeerConnection.call( sdpObserver: SdpObserver ){
        // MediaConstraints
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo","true"))
            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))
            optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        }

        createOffer(object: SdpObserver by sdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                setLocalDescription( object : SdpObserver {
                    override fun onSetFailure(p0: String?) {
                        Log.d("요호호","call:setFailure : $p0")
                    }

                    override fun onSetSuccess() {
                        Log.d("요호호","call:setSuccess : ")
                    }

                    override fun onCreateSuccess(p0: SessionDescription?) {
                        Log.d("요호호","call:onCreateSuccess : $p0")
                    }

                    override fun onCreateFailure(p0: String?) {
                        Log.d("요호호","call:onCreateFailure : $p0")
                    }
                }, desc )
                sdpObserver.onCreateSuccess(desc)
            }
        }, constraints)
    }

    // peerconnectino to answer?
    private fun PeerConnection.answer(sdpObserver: SdpObserver){
        // MediaConstraints
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo","true"))
            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))
            optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        }

        createAnswer( object : SdpObserver by sdpObserver {
            override fun onCreateSuccess(sDescription: SessionDescription?) {
                setLocalDescription( object : SdpObserver {
                    override fun onSetFailure(p0: String?) {
                        Log.d("요호호","answer:setFailure : $p0")
                    }

                    override fun onSetSuccess() {
                        Log.d("요호호","answer:onSetSuccess")
                    }

                    override fun onCreateSuccess(p0: SessionDescription?) {
                        Log.d("요호호","answer:onCreateSuccess : $p0")
                    }

                    override fun onCreateFailure(p0: String?) {
                        Log.d("요호호","answer:onCreateFailure : $p0")
                    }
                } , sDescription )

                sdpObserver.onCreateSuccess(sDescription)
            }
        }, constraints )
    }

    fun call(sdpObserver: SdpObserver) = peerConnection?.run { call(sdpObserver) }
    fun answer(sdpObserver: SdpObserver) = peerConnection?.answer(sdpObserver)

    // sessionDescription to setRemoteDescription
    fun onRemoteSessionReceived( sessionDescription: SessionDescription ){
        val sdpObserver = object : SdpObserver {
            override fun onSetFailure(p0: String?) {
                Log.d("요호호","remote:onSetFailure : $p0")
            }

            override fun onSetSuccess() {  }

            override fun onCreateSuccess(p0: SessionDescription?) {
                Log.d("요호호","remote:onCreateSuccess : $p0")
            }

            override fun onCreateFailure(p0: String?) {
                Log.d("요호호","remote:onCreateFailure : $p0")
            }
        }
        peerConnection?.run { setRemoteDescription( sdpObserver , sessionDescription ) }
    }

    // peerconnection add icecandidate
    fun addIceCandidate(iceCandidate: IceCandidate?){
        peerConnection?.addIceCandidate(iceCandidate)
//        try{
//            val jsonCandidate = JSONObject("$iceCandidate")
//            if(jsonCandidate["id"]=="audio"){
//                peerConnection?.addIceCandidate(iceCandidate)
//            } else {
//                setLogDebug("IceCandidate not audio : ${jsonCandidate["id"]}")
//            }
//        }catch (e:Exception){
//            peerConnection?.addIceCandidate(iceCandidate)
//            setLogDebug("IceCandidate catch : $iceCandidate")
//        }
    }

    /** DisConnect to PeerConnection **/
    fun disConnect(){
        peerConnection?.let {
            it.dispose()
        }
        localAudioSource?.let {
            it.dispose()
        }
        videoCapturer?.let {
            try {
                it.stopCapture()
            }catch (e:Exception){
                throw RuntimeException()
            }
            it.dispose()
        }
        localVideoSource?.let {
            it.dispose()
        }

        rootEglbase.release()

        PeerConnectionFactory.stopInternalTracingCapture()
        PeerConnectionFactory.shutdownInternalTracer()
    }

}