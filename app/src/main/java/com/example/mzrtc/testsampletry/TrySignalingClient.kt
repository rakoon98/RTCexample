package com.example.mzrtc.testsampletry

import android.util.Log
import com.example.mzrtc.App
import com.example.mzrtc.testsampletry.data.*
import com.example.mzrtc.utils.setLogDebug
import com.google.gson.Gson
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.*
import me.amryousef.webrtc_demo.TrySignallingClientListener
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.net.URISyntaxException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import kotlin.coroutines.CoroutineContext


/**
 *   각종 캔디 관련 사항은 HTTP API 사용
 *   1. socket . emit ("call started") : 통화 시작 시점에 클라에서 발송
 *      -> offer 이후 연결 시작까지 오래 걸린다면 필요함
 *   2. socket . on ( "ping", { "remain_time" } )
 *      -> 일정 주기마다 커넥션 생존 확인 / 남은 시간 sync
 */
class TrySignalingClient(
    private val listener: TrySignallingClientListener,
    val url : String,
    val port : String,
    val roomId : String
) : CoroutineScope {

    val TAG = this::class.java.simpleName
    var socket: Socket? = null

    var gson = Gson()
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
//    private val sendChannel = ConflatedBroadcastChannel<String>()

    val channel = App.coChannel

    init {
        connect()
    }

    // 소켓 연결 시도 ??
    private fun connect() = launch {
//        testInitialSocket("https://192.168.0.23", "8889")
        initializeSokcet(url = url , port = port)
    }

    // 소켓 초기화
    fun initializeSokcet(
        url: String,
        port: String
    ) {
        try {
//            Log.d("요호호", "가즈아3 : $url , $port")
//            setLogDebug("연결주소:$url")
//            val socketUrl = "$url:$port"
            val socketUrl = url
//            val hostnameVerifier: HostnameVerifier =
//                HostnameVerifier { hostname, session -> true }
//            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
//                override fun checkClientTrusted(
//                    chain: Array<X509Certificate>,
//                    authType: String
//                ) {
//                }
//
//                override fun checkServerTrusted(
//                    chain: Array<X509Certificate>,
//                    authType: String
//                ) {
//                }
//
//                override fun getAcceptedIssuers(): Array<X509Certificate?> {
//                    return arrayOfNulls(0)
//                }
//            })
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
//            socket = IO.socket(socketUrl,opts)
            socket = IO.socket(socketUrl)

            socket?.run {
                on(Socket.EVENT_CONNECT, cjListener)
                on(Socket.EVENT_CONNECT_ERROR, errorListener)
                on(Socket.EVENT_CONNECT_TIMEOUT, errorListener)
                on(Socket.EVENT_DISCONNECT, errorListener)
                on(Socket.EVENT_RECONNECT, reConnectListener)
                on(Socket.EVENT_MESSAGE, msgListener)
                on(ACK_TEST, ackListener)

                on(TERMINATED, terminatedListener)
                on(MATCHED, matchListener)
                on(JOIN,joinListener)
                on(TIMEOUT,timeoutListener)

//                on("created", createdListener)
//                on("full", fullListener)
//                on("join", joinListener)
//                on("joined", joinedListener)

                connect()
            }
        } catch (e: URISyntaxException) {
            setLogDebug("URISyntaxException : $e")
            throw RuntimeException(e)
        } catch (e: NoSuchAlgorithmException) {
            setLogDebug("NoSuchAlgorithmException : $e")
            e.printStackTrace()
        } catch (e: KeyManagementException) {
            setLogDebug("KeyManagementException : $e")
            e.printStackTrace()
        }
    }

    // 연결된 직후 바로 메소드 발송
    fun callMessage() {
        try {
            socket?.run {
                emit("join","{token= ~~ / password=~~ }" , object: Ack {
                    override fun call(vararg args: Any?) {
                        val backInfo  = args[args.size - 1]
                        var ackJson = JSONObject("$backInfo")
                        when(ackJson["success"]){
                            true,"true" -> { /** 대기상태로 넘어감 **/ }
                            else ->{ /** 에러처리 (밖으로?) **/ }
                            /** 빠른매칭은 HTTP 처리로!! **/
                        }
                    }
                })
            }

            socket?.run {
                setLogDebug("socket is connect create : create or join")
                emit("create or join", roomId)
                emit(Socket.EVENT_MESSAGE, GOT_USER_MEDIA)

////                ACK 잘오는지 테스트 하였음.
//                emit(ACK_TEST, listOf("123",123, mapOf<String,Int>("test" to 9898) , TestData("테스트합니다",123, TestData("테스트안에테스트",19048,"오호Any",172389127L) , 82746758134981394L)), object : Ack {
//                    override fun call(vararg args: Any?) {
//                        val backInfo = args[args.size - 1]
//                        setLogDebug("ACK_TEST_in_backInfo : $backInfo")
//                    }
//                })
            } ?: kotlin.run {
                setLogDebug("socket is null")
            }
        } catch (e: Exception) {
            setLogDebug("socket error : $e")
        }
    }


    // 메시지 받으면 메시지 전송?
    fun send(dataObject: Any?) = runBlocking {
        setLogDebug("send로 넘어온 데이터 : $dataObject")
        socket?.run {
            val json = gson.toJson(dataObject)
            when {
                json.toLowerCase().contains("offer") -> {
                    setLogDebug("$TAG : offerEncoding -> $json")

                    try {
                        if (dataObject is SessionDescription) {
                            var jsonObject = JSONObject().apply {
                                put("type", "offer")
                                put("sdp", dataObject.description)
                            }
                            emit(Socket.EVENT_MESSAGE, jsonObject)
                        } else {
                            setLogDebug("receive offer but is not SessionDescription")
                        }
                    } catch (e: Exception) {
                        Log.d("요호호", "offer data error -> $e")
                    }
                }
                json.toLowerCase().contains("answer") -> {
                    if (dataObject is SessionDescription) {
                        var jsonObject = JSONObject().apply {
                            put("type", "answer")
                            put("sdp", dataObject.description)
                        }
                        setLogDebug("$TAG : answer data -> ${dataObject.description}")

                        emit(Socket.EVENT_MESSAGE, jsonObject)
                    } else {
                        setLogDebug("receive answer but is not SessionDescription")
                    }
                }
                json.toLowerCase().contains("candidate") -> {
                    setLogDebug("$TAG : cadidate -> $json")
                    var can = (dataObject as IceCandidate)
                    var jsonObject = JSONObject().apply {
                        put("type", "candidate")
                        put("candidate", can.sdp)
                        put("id", can.sdpMid)
                        put("label", can.sdpMLineIndex)
                    }
                    emit("message", jsonObject)
                }
                else -> {
                    setLogDebug("SendTime when else : $json")
                }
            }
        } ?: run {
            setLogDebug("socket is null")
        }
    }


    fun process(data: Any) {
        setLogDebug("process : $data")
        when (data) {
            is String -> {
                when (data) {
                    GOT_USER_MEDIA -> {
                        launch {
                            channel.sendString(GOT_USER_MEDIA)
                        }
                    }
                    BYE -> {
                        launch {
                            channel.sendString(BYE)
                            onDestroy()
                        }
                    }
                    else -> {
                        process_any(data)
                    }
                }
            }
            else -> { process_any(data) }
        }
    }

    fun process_any(data:Any){
        try{
            val data_ = JSONObject("$data")
            // offer, answer, candidate 을 받았을때.
            when (data_["type"]) {
                "offer" -> {
                    var sDescription = SessionDescription(
                        SessionDescription.Type.OFFER,
                        "${data_["sdp"]}"
                    )
                    listener.onOfferReceived(sDescription)
                }
                "answer" -> {
                    var sDescription = SessionDescription(
                        SessionDescription.Type.ANSWER,
                        "${data_["sdp"]}"
                    )
                    listener.onAnswerReceived(sDescription)
                }
                "candidate" -> {
                    // String sdpMid, int sdpMLineIndex, String sdp
                    var candidate = IceCandidate(
                        data_["id"].toString(),
                        data_.getInt("label"),
                        data_["candidate"].toString()
                    )
                    listener.onIceCandidateReceived(candidate)
                }
                else->{
                    setLogDebug("process_any : received from ${data_["type"]}")
                }
            }
        } catch (e:Exception){
            setLogDebug("error : $e")
        }
    }


    // 종료
    fun onDestroy() {
        socket?.run {
            send("bye")
            send(HANGUP)
            off(Socket.EVENT_CONNECT, cjListener)
            off(Socket.EVENT_CONNECT_ERROR, errorListener)
            off(Socket.EVENT_CONNECT_TIMEOUT, errorListener)
            off(Socket.EVENT_MESSAGE, msgListener)

//            off("created", createdListener)
//            off("full", fullListener)
//            off("join", joinListener)
//            off("joined", joinedListener)

            disconnect()
        }
    }


    /**
     *  socket io listener
     */
    val reConnectListener = Emitter.Listener { con ->
        socket?.let { reSocket ->
            setLogDebug("소켓현황[reConnectListener]: 기존:$socket ?= 리스너:$reSocket ==>  ${ socket==reSocket }")
            setLogDebug("reConnect Socket -> $reSocket")
            setLogDebug("SocketIsConnected : ${reSocket.connected()}")
//            listener.reConnected()
        }
    }
    val cjListener = Emitter.Listener { coj ->
        socket?.let { socket_ ->
            setLogDebug("소켓현황[cjListener]: 기존:$socket ?= 리스너:$socket_ ==>  ${ socket==socket_ }")
            setLogDebug("Connect Socket -> $socket_")
            setLogDebug("SocketIsConnected : ${socket_.connected()}")
        }
        callMessage()
    }
    val joinListener = Emitter.Listener {  join ->
        join.forEach {

        }
    }
    val matchListener = Emitter.Listener {  match ->
        match.forEach {
            // 상대방 기본 프로필 조회 HTTP API 콜
            // offer = true 일때 offer 발행 , false 일때 answer 발행
            setLogDebug("matched event : $it")
            try{
                val json = JSONObject("$it")
//                val offer = json.getBoolean("offer")
                val offer = false
                if(offer){
                    launch { channel.sendString(CREATE_OFFER) }
                } else {
                    launch { channel.sendString(CREATE_ANSWER) }
                }
            } catch ( e : Exception ){
                setLogDebug("match error : $e")
            }
        }
    }
    val terminatedListener = Emitter.Listener { terminated ->
        terminated.forEach {
            // 상대방이 연결 종료해서 연결 끊기
        }
    }
    val timeoutListener = Emitter.Listener { timeout ->
        timeout.forEach {
            // 시간 제한으로 인한 연결 끊기
        }
    }

    val msgListener = Emitter.Listener{ msg ->
        msg.forEach {
            process("$it")
        }
    }
    val errorListener = Emitter.Listener { error ->
        error.forEachIndexed { index, error_ ->
            setLogDebug("socket received error : $index --> $error_")
        }
    }
    val ackListener = Emitter.Listener { args ->
        setLogDebug("ACK_TEST_in_listener : $args")
        if( args is Ack ) {
            val ack = args[args.size - 1] as Ack
            ack.call()
        }
    }

//    val fullListener = Emitter.Listener {   info ->  info.forEach { msg -> setLogDebug("full  :  $msg") }   }
//    val createdListener = Emitter.Listener {  info ->  info.forEach { msg -> setLogDebug("created  :  $msg") }  }
//    val joinListener = Emitter.Listener { info ->  info.forEach { msg -> setLogDebug("join  :  $msg") } }
//    val joinedListener = Emitter.Listener {   info ->  info.forEach { msg -> setLogDebug("joined  :  $msg") } }
}

