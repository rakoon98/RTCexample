package com.example.mzrtc.analysis

import android.app.Application
import org.webrtc.*
import org.webrtc.PeerConnection.RTCConfiguration
import org.webrtc.RendererCommon.ScalingType
import java.util.*

class PeerConnectionManager(
    val context: Application,
    val observer : PeerConnection.Observer
) {

    // ice Candidate stun server
    private val iceServer = listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())

    var sdpMediaConstraints : MediaConstraints? = null

    // 자원
//     var audioManager: AppRTCAudioManager? = null
     var localRenderer: SurfaceViewRenderer? = null
     var remoteRenderer: SurfaceViewRenderer? = null

     var remoteSinks: ArrayList<VideoSink> = ArrayList()
     val remoteProxyRenderer: ProxyVideoSink = ProxyVideoSink()
     val localProxyVideoSink: ProxyVideoSink = ProxyVideoSink()

    // RTC
    var encoderFactory: VideoEncoderFactory? = null
    var decoderFactory: VideoDecoderFactory? = null

    var factory : PeerConnectionFactory? = null
    var peerConnection : PeerConnection? = null

    val eglBase = EglBase.create()

    init {
        remoteSinks.add(remoteProxyRenderer)
    }

    fun rendererInit(
        localRenderer_ : SurfaceViewRenderer,
        remoteRenderer_ : SurfaceViewRenderer
    ){
        // Create video renderers.
        this.localRenderer = localRenderer_.apply {
            init(eglBase.eglBaseContext, null)
            setEnableHardwareScaler(true)
            setScalingType(ScalingType.SCALE_ASPECT_FIT)
        }
        this.remoteRenderer = remoteRenderer_.apply {
            init(eglBase.eglBaseContext, null)
            setEnableHardwareScaler(true)
            setScalingType(ScalingType.SCALE_ASPECT_FIT)
        }
    }


    fun createPeerConnectionFactoryInternal() {
//        if (peerConnectionParameters.videoCodecHwAcceleration) {
//            encoderFactory = DefaultVideoEncoderFactory(
//                rootEglBase.getEglBaseContext(),
//                true /* enableIntelVp8Encoder */,
//                enableH264HighProfile
//            )
//            decoderFactory = DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext())
//        } else {
            encoderFactory = SoftwareVideoEncoderFactory()
            decoderFactory = SoftwareVideoDecoderFactory()
//        }
        val options = PeerConnectionFactory.Options().apply {
            networkIgnoreMask = 0
        }

        factory = PeerConnectionFactory.builder()
            .setOptions(options)
//            .setAudioDeviceModule(adm)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    fun createMediaConstraintsInternal() {
        // Create SDP constraints.
        sdpMediaConstraints = MediaConstraints()
        sdpMediaConstraints?.apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
    }

    fun createPeerConnectionInternal() {
        val rtcConfig = RTCConfiguration(iceServer)
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
        rtcConfig.continualGatheringPolicy =
            PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        // Use ECDSA encryption.
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA
        // Enable DTLS for normal calls and disable for loopback calls.
        // Enable DTLS for normal calls and disable for loopback calls.
        rtcConfig.enableDtlsSrtp = true
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN


        peerConnection = factory?.createPeerConnection(rtcConfig, observer)
    }



}