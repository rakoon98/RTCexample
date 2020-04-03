package com.example.mzrtc.testsampletry

import android.util.Log
import com.example.mzrtc.App
import com.example.mzrtc.testsampletry.view.TryActivity
import com.example.mzrtc.utils.getTimeHour
import com.example.mzrtc.utils.setLogDebug
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.*
import me.amryousef.webrtc_demo.TrySignallingClientListener
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.net.URISyntaxException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import javax.net.ssl.*
import java.security.cert.X509Certificate
import kotlin.coroutines.CoroutineContext

class TrySignalingClient(
    val activity : TryActivity,
    private val listener: TrySignallingClientListener
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
        testInitialSocket("https://192.168.0.16", "8889")
    }

    // 소켓 초기화
    fun testInitialSocket(
        address: String,
        port: String
    ) {
        try {
            Log.d("요호호", "가즈아3 : $address , $port")
            val socketUrl = "$address:$port"
            val hostnameVerifier: HostnameVerifier =
                HostnameVerifier { hostname, session -> true }
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(
                    chain: Array<X509Certificate>,
                    authType: String
                ) {
                }

                override fun checkServerTrusted(
                    chain: Array<X509Certificate>,
                    authType: String
                ) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate?> {
                    return arrayOfNulls(0)
                }
            })
            val trustManager =
                trustAllCerts[0] as X509TrustManager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, null)
            val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory
            val okHttpClient = OkHttpClient.Builder()
                .hostnameVerifier(hostnameVerifier)
                .sslSocketFactory(sslSocketFactory, trustManager)
                .build()
            val opts = IO.Options()
            opts.callFactory = okHttpClient
            opts.webSocketFactory = okHttpClient
            socket = IO.socket(socketUrl, opts)

            socket?.run {
                on(Socket.EVENT_CONNECT, cjListener)
                on(Socket.EVENT_CONNECT_ERROR, errorListener)
                on(Socket.EVENT_CONNECT_TIMEOUT, errorListener)
                on(Socket.EVENT_MESSAGE, msgListener)
                on("create or join", firstListener)
                on("receive locations", msgListener)

                connect()
            }
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        }
    }


    val firstListener = Emitter.Listener { con ->
        socket?.let {
            Log.d("요호호", "???")
            it.emit(Socket.EVENT_MESSAGE, "got user media")
        }
    }
    val cjListener = Emitter.Listener { coj ->
        coj.forEachIndexed { index, any ->
            Log.d("요호호,", "cjListener : $any")
        }
        callMessage()
    }
    val msgListener = Emitter.Listener{ msg ->
        msg.forEach {
            Log.d("요호호,", "msgListener : $it")
            process("$it")
        }
    }
    val errorListener = Emitter.Listener { error ->
        error.forEachIndexed { index, error_ ->
            Log.d("요호호", "요호호 : $error_")
        }
    }

    // 연결된 직후 바로 메소드 발송
    fun callMessage() {
        try {
            socket?.run {
                emit("create or join", "hi")
                emit(Socket.EVENT_MESSAGE, "got user media")
                Log.d(TAG, "요호호")
            } ?: kotlin.run {
                Log.d(TAG, "요호호 socket is null")
            }
        } catch (e: Exception) {
            Log.d(TAG, "${e.message}")
        }
    }


    // 메시지 받으면 메시지 전송?
    fun send(dataObject: Any?) = runBlocking {
        socket?.run {
            val json = gson.toJson(dataObject)
            Log.d("요호호", "가즈아 runblocking : $json")

            when {
                json.toLowerCase().contains("offer") -> {
                    Log.d("요호호", "offerEncoding -> $json")
//                        var json_ = gson.fromJson("$dataObject", JsonObject::class.java)
                    try {
                        if (dataObject is SessionDescription) {
                            var jsonObject = JSONObject().apply {
                                put("type", "offer")
                                put("sdp", dataObject.description)
                            }

                            Log.d("요호호", "offer data -> ${dataObject.description}")
                            emit(Socket.EVENT_MESSAGE, jsonObject)
                        } else {
                            Log.d("요호호", "offer data -> not a SessionDescription")
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

                        Log.d("요호호", "answer data -> ${dataObject.description}")
                        emit(Socket.EVENT_MESSAGE, jsonObject)
                    } else {

                    }
                }
                json.toLowerCase().contains("candidate") -> {
                    Log.d("요호호", "candidate -> $json")
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
                    Log.d("요호호", "ELSE -> $json")
                }
            }

        } ?: run {
            Log.d(TAG, "요호호 socket is null")
        }
    }


    fun process(data: Any) {
        Log.d("요호호", "가즈아 process : $data")
        when (data) {
            is String -> {
                when (data) {
                    "got user media" -> {
                        launch {
                            activity.rtcClient.run {
                                Log.d("요호호", "시작")
                                setLogDebug( getTimeHour() )
                                call(activity.sdpObserver)
//                                channel.run { CREATE_OFFER }
                            }
                        }
                    }
                    "bye" -> {
                        Log.d("요호호", "client said : bye -> 상대방이 나갔습니다")
                        onDestroy()
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
            Log.d("요호호", "process_any ==>> data = $data_ /// type = ${data_["type"]}")
            when (data_["type"]) {
                "offer" -> {
                    var sDescription = SessionDescription(
                        SessionDescription.Type.OFFER,
                        "${data_["sdp"]}"
                    )
                    Log.d("요호호", "offer received sdp = $sDescription")
                    listener.onOfferReceived(sDescription)
                }
                "answer" -> {
                    var sDescription = SessionDescription(
                        SessionDescription.Type.ANSWER,
                        "${data_["sdp"]}"
                    )
                    Log.d("요호호", "answer received sdp = $sDescription")
                    listener.onAnswerReceived(sDescription)
//                    activity.rtcClient.answer(sdpObserver = activity.sdpObserver)
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
                    Log.d("요호호", "process_any : received from ${data_["type"]}")
                }
            }
        } catch (e:Exception){
            Log.d("요호호", "error : $e")
        }
    }


    // 종료
    fun onDestroy() {
        socket?.run {
            send("bye")
            off(Socket.EVENT_CONNECT, cjListener)
            off(Socket.EVENT_CONNECT_ERROR, errorListener)
            off(Socket.EVENT_CONNECT_TIMEOUT, errorListener)
            off(Socket.EVENT_MESSAGE, msgListener)
            disconnect()
        }
    }

}

