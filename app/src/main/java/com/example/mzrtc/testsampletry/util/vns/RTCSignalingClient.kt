package com.example.mzrtc.testsampletry.util.vns

import com.example.mzrtc.App
import com.example.mzrtc.testsampletry.data.BYE
import com.example.mzrtc.utils.setLogDebug
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.security.cert.X509Certificate
import javax.net.ssl.*

class RTCSignalingClient(
    var url :String,
    var port : String,
    var roomId: String
) {
    // 이벤트 전달 코루틴 채널
    val channel = App.coChannel
    var socket: Socket? = null
    var socketOnListener :SocketOnListener? = null

    init {
//        connect("https://192.168.0.16", "8889", "hi")
//        connect("https://192.168.0.223", "8889", "")
        connect(url,port,roomId)
    }

    fun connect (
        address: String, port : String, roomId : String
    ) = CoroutineScope(Dispatchers.Main).launch {
        initialSocket(address,port, roomId)
    }

    private fun initialSocket( address: String, port : String, roomId : String ){
        try {
            val socketUrl = "$address:$port"

            setLogDebug(socketUrl)
            /** SSL SIGNED -> TRUST MANAGER & HOST_NAME_VERIFIER **/
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
            /** SSL SIGNED -> TRUST MANAGER & HOST_NAME_VERIFIER **/

            // socket initialize with options
            IO.Options().apply {
                callFactory = okHttpClient
                webSocketFactory = okHttpClient
            } .run {
                socket = IO.socket(socketUrl, this).run {
                    // create on then connect
                    socketOnListener = SocketOnListener(this, roomId)
                    socketOnListener?.run {
                        on(Socket.EVENT_CONNECT, connectListener)
                        on(Socket.EVENT_CONNECT_ERROR, errorListener)
                        on(Socket.EVENT_MESSAGE, messageListener)
                    } ?: run {
                        // 소켓 리스너 생성 에러
                    }
                    connect()
                }
            }

        }catch (e:Exception){
            // 소켓 연결중 문제가 생겼으니 sokcet 및 peerConnection 관련 전부 해제후
            // 에러 발생을 알린 후 , 되돌아간다.
            // TODO() = 연결 해제 및 뷰(or 상태) 롤백

        }
    }

    // 연결해제시 전부 해제
    fun destroy(){
        socket?.run {
            send(BYE)
            socketOnListener?.run {
                off(Socket.EVENT_CONNECT, connectListener)
                off(Socket.EVENT_CONNECT_ERROR, errorListener)
                off(Socket.EVENT_MESSAGE, messageListener)
            }
            disconnect()
        }
    }

}