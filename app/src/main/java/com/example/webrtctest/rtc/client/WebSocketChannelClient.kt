package com.example.webrtctest.rtc.client

import android.os.Handler
import android.util.Log
import com.example.webrtctest.rtc.data.WebSocketConnectionState
import com.example.webrtctest.rtc.interfacertc.WebSocketChannelEvents
import com.example.webrtctest.rtc.util.AsyncHttpURLConnection
import de.tavendo.autobahn.WebSocket
import de.tavendo.autobahn.WebSocketConnection
import de.tavendo.autobahn.WebSocketException
import org.json.JSONObject
import java.net.URI
import java.net.URISyntaxException
import java.util.*

class WebSocketChannelClient (
    val handler : Handler,
    val events : WebSocketChannelEvents
){
    val TAG = This@WebSocketChannelClient::class.java.simpleName
    private val CLOSE_TIMEOUT :Long= 1000

    // initialize to websocketrtcclient
    var roomId :String ?= null
    var clientId :String ?= null
    var state = WebSocketConnectionState.NEW
    var wsServerUrl : String = ""
    var postServerUrl : String = ""
    lateinit var ws : WebSocketConnection

    private var closeEvent : Boolean = true
    private val closeEventLock = Any() as Object

    // WebSocket send queue. Messages are added to the queue when WebSocket
    // client is not registered and are consumed in register() call.
    private var wsSendQueue: ArrayList<String> = ArrayList()


    // WebSocketConnectionObserver observer
    var wsObserver = object : WebSocket.WebSocketConnectionObserver {
        override fun onOpen() {
            handler.post {
                state = WebSocketConnectionState.CONNECTED // 상태 변경

                roomId?.let { rId->
                    clientId?.let { cId ->
                        register(rId, cId)
                    }
                }

            }
        }
        override fun onClose(
            code : WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification,
            reason : String?
        ) {
            synchronized(closeEventLock){
                closeEvent = true
                closeEventLock.notify()
            }

            handler.post {
                if(state!=WebSocketConnectionState.CLOSED){
                    state = WebSocketConnectionState.CLOSED
                    events.onWebSocketClose()
                }
            }
        }

        override fun onTextMessage(payload: String) {
            Log.d(this@WebSocketChannelClient::class.java.simpleName, "WSS->C: $payload")
            handler.post {
                if(state==WebSocketConnectionState.CONNECTED || state==WebSocketConnectionState.REGISTERED){
                    events.onWebSocketMessage(payload)
                }
            }
        }


        override fun onRawTextMessage(p0: ByteArray?) {
            TODO("Not yet implemented")
        }

        override fun onBinaryMessage(p0: ByteArray?) {
            TODO("Not yet implemented")
        }
    }

    // 연결
    fun connect(
        wsUrl : String,
        postUrl : String
    ){
        checkIfCalledOnValidThread()  //  looper thread 에서 불렸는지 체크
        if(state!=WebSocketConnectionState.NEW){
            Log.e(TAG, "WebSocket is already connected.")
            return
        }
        wsServerUrl = wsUrl
        postServerUrl = postUrl
        closeEvent = false

        Log.d(TAG, "Connecting WebSocket to: $wsUrl. Post URL: $postUrl")

        ws = WebSocketConnection()

        try{
            ws.connect( URI(wsServerUrl), wsObserver )
        }catch (e:URISyntaxException){
            reportError("${e.message}")
        }catch (e:WebSocketException){
            reportError("${e.message}")
        }
    }

    fun disConnect(waitForComplete:Boolean){
        checkIfCalledOnValidThread() // check thread
        Log.d(TAG, "Disconnect WebSocket. state : $state")

        if(state==WebSocketConnectionState.REGISTERED){
            send("{\"type\": \"bye\"}")
            state = WebSocketConnectionState.CONNECTED

            sendWSSMessage("DELETE","")
        }

        // close WebSocket in CONNECTED or ERROR states only.
        if(state==WebSocketConnectionState.CONNECTED || state==WebSocketConnectionState.ERROR){
            ws.disconnect()
            state = WebSocketConnectionState.CLOSED

            // Wait for websocket close event to prevent websocket library from
            // sending any pending messages to deleted looper thread.
            if(waitForComplete){
                synchronized( closeEventLock ){
                    while(!closeEvent){
                        try {
                            closeEventLock.wait(CLOSE_TIMEOUT)
                            break
                        }catch (e:InterruptedException){
                            Log.e(TAG, "Wait error : ${e.message}")
                        }
                    }
                }
            }
        }
        Log.d(TAG, "Disconnecting WebSocket done.")
    }

    // 가입
    fun register(
        roomId : String,
        clientId : String
    ){
        checkIfCalledOnValidThread()  //  looper thread 에서 불렸는지 체크

        this.roomId = roomId
        this.clientId = clientId

        // 이미 연결되어있으니 스킵
        if(state!=WebSocketConnectionState.CONNECTED){
            Log.w(TAG, "WebSocket register() in state $state")
            return
        }
        Log.d(TAG, "Registering WebSocket for room $roomId. ClientID: $clientId")

        try{
            var jsonObject = JSONObject().apply {
                put("cmd","register")
                put("roomid",roomId)
                put("clientid",clientId)
            }
            Log.d(TAG,"$jsonObject")

            ws.sendTextMessage("$jsonObject")
            state = WebSocketConnectionState.REGISTERED

            // Send any previously accumulated messages.
            // Send any previously accumulated messages.
            for (sendMessage in wsSendQueue) {
                send(sendMessage)
            }

            wsSendQueue.clear()
        }catch (e:Exception){

        }
    }

    // 전송
    fun send(message:String){
        checkIfCalledOnValidThread() // thread check

        when(state){
            WebSocketConnectionState.NEW -> { }
            WebSocketConnectionState.CONNECTED -> {
                Log.e(TAG, "WS ACC : $message")
                wsSendQueue.add(message)
            }
            WebSocketConnectionState.REGISTERED -> {
                try{
                    var json = JSONObject().apply {
                        put("cmd","send")
                        put("msg",message)
                    }
                    Log.d(TAG, "C->WSS : $json")
                    ws.sendTextMessage("$json")
                }catch (e:Exception){
                    reportError("Websocket send JSON error : ${e.message}")
                }
            }
            WebSocketConnectionState.CLOSED -> {
                Log.e(TAG,"Websocket send() in error r closed state : $message")
            }
            WebSocketConnectionState.ERROR -> { }
        }
    }

    fun sendWSSMessage(method:String, message:String){
        var postUrl = "$postServerUrl/$roomId/$clientId"
        Log.d(TAG, "WS $method : $postUrl : $message")

        val httpConnection = AsyncHttpURLConnection(method, postUrl, message, object : AsyncHttpURLConnection.AsyncHttpEvents{
            override fun onHttpError(errorMessage: String?) {
                reportError("WS : $method , error : $errorMessage")
            }

            override fun onHttpComplete(response: String?) {}
        })

        httpConnection.send()
    }


    // Helper method for debugging purposes. Ensures that WebSocket method is
    // called on a looper thread.
    private fun checkIfCalledOnValidThread() {
        check(!(Thread.currentThread() !== handler.looper.thread)) { "WebSocket method is not called on valid thread" }
    }

    // 에러
    fun reportError(errorMessage : String){
        Log.e(TAG, errorMessage)
        handler.post {
            if (state != WebSocketConnectionState.ERROR) {
                state = WebSocketConnectionState.ERROR
                events.onWebSocketError(errorMessage)
            }
        }
    }
}