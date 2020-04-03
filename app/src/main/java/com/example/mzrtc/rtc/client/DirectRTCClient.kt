package com.example.mzrtc.rtc.client

import android.util.Log
import com.example.mzrtc.model.data.DirectRTCClient_TAG
import com.example.mzrtc.rtc.SignalingParameters
import com.example.mzrtc.rtc.classrtc.RoomConnectionParameters
import com.example.mzrtc.rtc.data.ConnectionState
import com.example.mzrtc.rtc.data.IP_PATTERN
import com.example.mzrtc.rtc.interfacertc.AppRTCClient
import com.example.mzrtc.rtc.interfacertc.SignalingEvents
import com.example.mzrtc.rtc.interfacertc.TCPChannelEvents
import com.example.mzrtc.utils.jsonPut
import com.example.mzrtc.utils.toJavaCandidate
import com.example.mzrtc.utils.toJsonCandidate
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.util.concurrent.Executors

/**
 *  Implementation of AppRTCClient that uses direct TCP connection as the signaling channel.
 *  This eliminates the need for an external server.
 *  This class does not support loopback connections.
 */
class DirectRTCClient(
//    private val executor: ExecutorService? = null ?: Executors.newSingleThreadExecutor(),
    private val events: SignalingEvents
) : AppRTCClient, TCPChannelEvents {

    private val executor = Executors.newSingleThreadExecutor()

    // All alterations of the room state should be done from inside the looper thread.
    private var roomState: ConnectionState? = ConnectionState.NEW

//    private val tcpClient: TCPChannelClient? = null
    private var connectionParameters: RoomConnectionParameters? = null
    private var tcpClient: TCPChannelClient? = null

    /**
     *  TCPChannelEvents override
     */
    /** If the client is the server side, this will trigger onConnectedToRoom. **/
    override fun onTCPConnected(server: Boolean) {
        if(server){
            roomState = ConnectionState.CONNECTED

            val parameters = SignalingParameters(arrayListOf(), server, null,null,null,null,null)
            events?.onConnectedToRoom(parameters)
        }
    }

    // tcp message Signaling sdp
    override fun onTCPMessage(message: String?) {
        try {
            var json = JSONObject()
            var type = json.optString("type")
            when(type){
                "candidate" -> {
                    events?.onRemoteIceCandidate( toJavaCandidate(json) )
                }
                "remove-candidates" -> {
                    var candidateArray = json.getJSONArray("candidates")
                    val candidates = arrayOfNulls<IceCandidate>(candidateArray.length())

                    for (i in 0 until candidateArray.length()) {
                        candidates[i] = toJavaCandidate(candidateArray.getJSONObject(i))
                    }

                    events?.onRemoteIceCandidatesRemoved(candidates)
                }
                "answer" -> {
                    var sdp = SessionDescription( SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp") )
                    events?.onRemoteDescription(sdp)
                }
                "offer" -> {
                    var sdp = SessionDescription( SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp") )
                    var parameters = SignalingParameters(
                        listOf(),
                        false,
                        null,
                        null,
                        null,
                        sdp,
                        null
                    )

                    roomState = ConnectionState.CONNECTED
                    events?.onConnectedToRoom(parameters)
                }
                else -> {
                    reportError("Unexpected TCP Message : $message")
                }
            }
        }catch (e:Exception){
            reportError("TCP message JSON parsing error : ${e.message}")
        }
    }

    override fun onTCPError(description: String?) {
        reportError("TCP connection error: $description")
    }

    override fun onTCPClose() {
        events?.onChannelClose()
    }

    /**
     *  AppRTCClient override
     */
    // local iceCandidate 전송!
    override fun sendLocalIceCandidate(candidate: IceCandidate) {
        executor?.execute {
            var json = JSONObject().apply {
                jsonPut("type","candidate")
                jsonPut("label",candidate.sdpMLineIndex)
                jsonPut("id",candidate.sdpMid)
                jsonPut("candidate",candidate.sdp)
            }

            if(roomState != ConnectionState.CONNECTED ){
                reportError("Sending ICE candidate in non connected state.")
                return@execute
            }
            sendMessage("$json")
        }
    }

    override fun disconnectFromRoom() { disconnectFromRoomInternal() }

    override fun connectToRoom(connectionParameters: RoomConnectionParameters?) {
        this.connectionParameters = connectionParameters

//        connectionParameters?.let {
//            Log.d(DirectRTCClient_TAG, "${it.loopback} -- ${it.roomId} -- ${it.roomUrl} -- ${it.urlParameters}")
////            if (connectionParameters!!.loopback) { reportError("Loopback connections aren't supported by DirectRTCClient.") }
//        } ?: kotlin.run {
//            Log.d(DirectRTCClient_TAG,"roomConnectionParameters is null")
//        }

        executor?.execute {
            Log.d(DirectRTCClient_TAG, "executor.execute()")
            connectToRoomInternal()
        }?: kotlin.run {
            Log.d(DirectRTCClient_TAG, "executor is null")
        }
    }

    // 요청 sdp 를 보낸다
    override fun sendOfferSdp(sdp: SessionDescription) {
        executor?.execute {
            if(roomState!=ConnectionState.CONNECTED){
                reportError("Sending offer SDP in non connected state.")
                return@execute
            }

            var json = JSONObject().apply {
                jsonPut("sdp",sdp.description)
                jsonPut("type","offer")
            }
            sendMessage("$json")
        }
    }

    // 답변 sdp 를 보낸다
    override fun sendAnswerSdp(sdp: SessionDescription) {
        executor?.execute {
            var json = JSONObject().apply {
                jsonPut("sdp",sdp.description)
                jsonPut("type","answer")
            }
            sendMessage("$json")
        }
    }

    /** Send removed Ice candidates to the other participant. = iceCandidate 를 다른 사람에게 보내기 */
    override fun sendLocalIceCandidateRemovals(candidates: Array<out IceCandidate>?) {
        executor?.execute {
            var json = JSONObject().apply { jsonPut("type","remove-candidates") }
            var jsonArray = JSONArray()
            candidates?.let{
                it.forEach {  iceCandidate ->
                    jsonArray.put( toJsonCandidate(iceCandidate) )
                }
            }

            json.jsonPut("candidates", jsonArray)

            if(roomState!=ConnectionState.CONNECTED){
                reportError("Sending ICE Candidates removals in non connected state.")
                return@execute
            }

            sendMessage("$json")
        }
    }

    // connect to client
    fun connectToRoomInternal() {
        roomState = ConnectionState.NEW

        var endPoint = connectionParameters?.roomId
        var matcher = IP_PATTERN.matcher(endPoint)
//        if(!matcher.matches()){
//            // reportError ..
//            Log.d(DirectRTCClient_TAG, "matches return")
//            return
//        } // matcher 에 부합하지않으면 연결을 끊어버린다?

        var ip = matcher.group(1)                   // ip 를 구한다.
        var portStr = matcher.group(matcher.groupCount()) // port 를 구한다.
        var port : Int ?= null
        portStr?.let {
            try {
                port = portStr.toInt()
            }catch (e:NumberFormatException){
                reportError("Invalid port number: $portStr")
                Log.d(DirectRTCClient_TAG, "${e.message}")
                return
            }
        }

        Log.d(DirectRTCClient_TAG, "NOT ERROR go : $ip --> $port")
        executor?.let {exec ->
            port?.let { port_ ->
                Log.d(DirectRTCClient_TAG, "NOT ERROR : $ip --> $port")
                tcpClient = TCPChannelClient(exec, this, ip , port_)
            } ?: reportError("port is null in DirectRTCClient")
        } ?: reportError("executor is null in DirectRTCClient")
    }

    /**
     *  Disconnects from the room.
     *  Runs on the looper thread
     */
    fun disconnectFromRoomInternal(){
        roomState = ConnectionState.CLOSED // 방상태 바꿔주고

        // tcpCliecnt 연결해제해주고
        tcpClient?.let {
            it.disConnect()
            tcpClient = null
        }

        executor?.shutdown()
    }

    // --------------------------------------------------------------------
    // Helper functions.
    private fun reportError(errorMessage: String) {
        Log.e(DirectRTCClient_TAG, errorMessage)
        executor?.execute {
            if (roomState != ConnectionState.ERROR) {
                roomState = ConnectionState.ERROR
                events!!.onChannelError(errorMessage)
            }
        }
    }

    // 메시지 보내는 메소드
    fun sendMessage(message : String){
        executor?.execute {
            tcpClient?.send(message)
        }
    }
}