package com.example.mzrtc.rtc.client

import android.content.Context
import android.util.Log
import com.example.mzrtc.model.data.AUDIO_CODEC_ISAC
import com.example.mzrtc.model.data.PeerConnectionClient_TAG
import com.example.mzrtc.model.data.VIDEO_CODEC_H264_HIGH
import com.example.mzrtc.rtc.SignalingParameters
import com.example.mzrtc.rtc.classrtc.PeerConnectionParameters
import com.example.mzrtc.rtc.interfacertc.PeerConnectionEvents
import com.example.mzrtc.utils.getFieldTrials
import org.webrtc.*
import org.webrtc.PeerConnection.*
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule.*
import java.nio.charset.Charset
import java.util.concurrent.Executors

open class PeerConnectionClient(
    val context: Context,
    val eglBase : EglBase,
    val peerConnectionParameters: PeerConnectionParameters,
    val events : PeerConnectionEvents
) {

     open val TAG = this::class.java.simpleName

    // Executor thread is started once in private ctor and is used for all
    // peer connection API calls to ensure new peer connection factory is
    // created on the same thread as previously destroyed factory.
    val executor = Executors.newSingleThreadExecutor()

    var factory : PeerConnectionFactory? = null
    var peerConnection : PeerConnection? = null
    private var localRender: VideoSink? = null
    private var remoteSinks: List<VideoSink?>? = null
    private var signalingParameters: SignalingParameters? = null
    private var videoCapturer: VideoCapturer? = null

    // 변수
    var isError : Boolean = false
    var preferIsac = false
    var dataChannelEnabled = false

    init {
        val fieldTrials = peerConnectionParameters.getFieldTrials(  )
        dataChannelEnabled = peerConnectionParameters.dataChannelParameters != null
        // 쓰레드에서 peerConnection 초기화.
        executor.execute {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                    .setFieldTrials(fieldTrials)
                    .setEnableInternalTracer(true)
                    .createInitializationOptions()
            )
        }
    }


    /**
     * This function should only be called once.
     */
    fun createPeerConnectionFactory(options: PeerConnectionFactory.Options) {
        check(factory == null) { "PeerConnectionFactory has already been constructed" }
        executor.execute{
            options.createPeerConnectionFactoryInternal()
        }
    }

    fun PeerConnectionFactory.Options.createPeerConnectionFactoryInternal(){
        isError = false

        if(peerConnectionParameters.tracing){
//            PeerConnectionFactory.startInternalTracingCapture( /* 파일저장 */ )
        }

        preferIsac = peerConnectionParameters.audioCodec!=null && peerConnectionParameters.audioCodec==AUDIO_CODEC_ISAC

        // 오디오 저장 관련 메소드 : 컨트롤러 구현 및 해당상항 구현 start
        // 오디오 저장 관련 메소드 : 컨트롤러 구현 및 해당상항 구현 end

        val adm = createJavaAudioDevice()

        val enableH264HighProfile = VIDEO_CODEC_H264_HIGH==peerConnectionParameters.videoCodec
        var encoderFactory: VideoEncoderFactory? = null
        var decoderFactory: VideoDecoderFactory? = null

        if(peerConnectionParameters.videoCodecHwAcceleration){
            encoderFactory = DefaultVideoEncoderFactory( eglBase.eglBaseContext, true, enableH264HighProfile )
            decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        } else {
            encoderFactory = SoftwareVideoEncoderFactory()
            decoderFactory = SoftwareVideoDecoderFactory()
        }

        factory = PeerConnectionFactory.builder()
            .setOptions(this)
            .setAudioDeviceModule(adm)

            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()

        adm?.release()

    }

    fun createPeerConnectionInternal(){
        var rtcConfig = PeerConnection.RTCConfiguration(signalingParameters?.iceServers).apply {
            // TCP candidates are only useful when connecting to a server that supports.
            // ICE-TCP
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY

            // Use ECDSA encryption.
            keyType = PeerConnection.KeyType.ECDSA

            // Enable DTLS for normal calls and disable for loopback calls.
            enableDtlsSrtp = !peerConnectionParameters.loopback
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        peerConnection = factory?.createPeerConnection(rtcConfig, PCObserver)
    }

    fun createJavaAudioDevice(): AudioDeviceModule? {
        // Enable/disable OpenSL ES playback.
        if (!peerConnectionParameters.useOpenSLES) {
            Log.w(TAG, "External OpenSLES ADM not implemented yet.")
            // TODO(magjed): Add support for external OpenSLES ADM.
        }

        // Set audio record error callbacks.
        val audioRecordErrorCallback: AudioRecordErrorCallback = object : AudioRecordErrorCallback {
            override fun onWebRtcAudioRecordInitError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioRecordInitError: $errorMessage")
                reportError(errorMessage)
            }

            override fun onWebRtcAudioRecordStartError(
                errorCode: AudioRecordStartErrorCode,
                errorMessage: String
            ) {
                Log.e(TAG, "onWebRtcAudioRecordStartError: $errorCode. $errorMessage")
                reportError(errorMessage)
            }

            override fun onWebRtcAudioRecordError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioRecordError: $errorMessage")
                reportError(errorMessage)
            }
        }
        val audioTrackErrorCallback: AudioTrackErrorCallback = object : AudioTrackErrorCallback {
            override fun onWebRtcAudioTrackInitError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioTrackInitError: $errorMessage")
                reportError(errorMessage)
            }

            override fun onWebRtcAudioTrackStartError(
                errorCode: AudioTrackStartErrorCode,
                errorMessage: String
            ) {
                Log.e(TAG, "onWebRtcAudioTrackStartError: $errorCode. $errorMessage")
                reportError(errorMessage)
            }

            override fun onWebRtcAudioTrackError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioTrackError: $errorMessage")
                reportError(errorMessage)
            }
        }

        // Set audio record state callbacks.
        val audioRecordStateCallback: AudioRecordStateCallback = object : AudioRecordStateCallback {
            override fun onWebRtcAudioRecordStart() {
                Log.i(TAG, "Audio recording starts")
            }

            override fun onWebRtcAudioRecordStop() {
                Log.i(TAG, "Audio recording stops")
            }
        }

        // Set audio track state callbacks.
        val audioTrackStateCallback: AudioTrackStateCallback = object : AudioTrackStateCallback {
            override fun onWebRtcAudioTrackStart() {
                Log.i(TAG, "Audio playout starts")
            }

            override fun onWebRtcAudioTrackStop() {
                Log.i(TAG, "Audio playout stops")
            }
        }
        return builder(context)
//            .setSamplesReadyCallback(saveRecordedAudioToFile)
            .setUseHardwareAcousticEchoCanceler(!peerConnectionParameters.disableBuiltInAEC)
            .setUseHardwareNoiseSuppressor(!peerConnectionParameters.disableBuiltInNS)
            .setAudioRecordErrorCallback(audioRecordErrorCallback)
            .setAudioTrackErrorCallback(audioTrackErrorCallback)
            .setAudioRecordStateCallback(audioRecordStateCallback)
            .setAudioTrackStateCallback(audioTrackStateCallback)
            .createAudioDeviceModule()
    }

    open fun createPeerConnection(
        localRender: VideoSink, remoteSinks: List<VideoSink?>,
        videoCapturer: VideoCapturer, signalingParameters: SignalingParameters?
    ) {
        if (peerConnectionParameters == null) {
            Log.e(
                PeerConnectionClient_TAG,
                "Creating peer connection without initializing factory."
            )
            return
        }
        this.localRender = localRender
        this.remoteSinks = remoteSinks
        this.videoCapturer = videoCapturer
        this.signalingParameters = signalingParameters
        executor.execute(Runnable {
            try {
//                createMediaConstraintsInternal()
                createPeerConnectionInternal()
//                maybeCreateAndStartRtcEventLog()
            } catch (e: Exception) {
                reportError("Failed to create peer connection: " + e.message)
                throw e
            }
        })
    }


    private fun reportError(errorMessage: String) {
        Log.e(TAG, "Peerconnection error: $errorMessage")
        executor.execute {
            if (!isError) {
                events.onPeerConnectionError(errorMessage)
                isError = true
            }
        }
    }

    // Implementation detail: observe ICE & stream changes and react accordingly.
    val PCObserver = object : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate) {
            executor.execute(Runnable {
                events.onIceCandidate(
                    candidate
                )
            })
        }

        override fun onIceCandidatesRemoved(candidates: Array<IceCandidate?>?) {
            executor.execute(Runnable {
                events.onIceCandidatesRemoved(candidates)
            })
        }

        override fun onSignalingChange(newState: SignalingState) {
            Log.d(PeerConnectionClient_TAG, "SignalingState: $newState")
        }

        override fun onIceConnectionChange(newState: IceConnectionState) {
            executor.execute(Runnable {
                Log.d(PeerConnectionClient_TAG, "IceConnectionState: $newState")
                when (newState) {
                    IceConnectionState.CONNECTED -> {
                        events.onIceConnected()
                    }
                    IceConnectionState.DISCONNECTED -> {
                        events.onIceDisconnected()
                    }
                    IceConnectionState.FAILED -> {
                        reportError("ICE connection failed.")
                    }
                }
            })
        }

        override fun onConnectionChange(newState: PeerConnectionState) {
            executor.execute(Runnable {
                Log.d(PeerConnectionClient_TAG, "PeerConnectionState: $newState")
                when (newState) {
                    PeerConnectionState.CONNECTED -> {
                        events.onConnected()
                    }
                    PeerConnectionState.DISCONNECTED -> {
                        events.onDisconnected()
                    }
                    PeerConnectionState.FAILED -> {
                        reportError("DTLS connection failed.")
                    }
                }
            })
        }

        override fun onIceGatheringChange(newState: IceGatheringState) {
            Log.d(PeerConnectionClient_TAG, "IceGatheringState: $newState")
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {
            Log.d(
                PeerConnectionClient_TAG,
                "IceConnectionReceiving changed to $receiving"
            )
        }

        override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent) {
            Log.d(
                PeerConnectionClient_TAG,
                "Selected candidate pair changed because: $event"
            )
        }

        override fun onAddStream(stream: MediaStream) {}
        override fun onRemoveStream(stream: MediaStream) {}
        override fun onDataChannel(dc: DataChannel) {
            Log.d(PeerConnectionClient_TAG, "New Data channel " + dc.label())
            if (!dataChannelEnabled) return
            dc.registerObserver(object : DataChannel.Observer {
                override fun onBufferedAmountChange(previousAmount: Long) {
                    Log.d(
                        PeerConnectionClient_TAG,
                        "Data channel buffered amount changed: " + dc.label() + ": " + dc.state()
                    )
                }

                override fun onStateChange() {
                    Log.d(
                        PeerConnectionClient_TAG,
                        "Data channel state changed: " + dc.label() + ": " + dc.state()
                    )
                }

                override fun onMessage(buffer: DataChannel.Buffer) {
                    if (buffer.binary) {
                        Log.d(
                            PeerConnectionClient_TAG,
                            "Received binary msg over $dc"
                        )
                        return
                    }
                    val data = buffer.data
                    val bytes = ByteArray(data.capacity())
                    data[bytes]
                    val strData =
                        String(bytes, Charset.forName("UTF-8"))
                    Log.d(
                        TAG,
                        "Got msg: $strData over $dc"
                    )
                }
            })
        }

        override fun onRenegotiationNeeded() {
            // No need to do anything; AppRTC follows a pre-agreed-upon
            // signaling/negotiation protocol.
        }

        override fun onAddTrack(
            receiver: RtpReceiver,
            mediaStreams: Array<MediaStream>
        ) {
        }
    }
}