package com.example.mzrtc.rtc

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.example.mzrtc.model.data.ROOM_JOIN
import com.example.mzrtc.model.data.ROOM_LEAVE
import com.example.mzrtc.model.data.ROOM_MESSAGE
import com.example.mzrtc.rtc.classrtc.RoomConnectionParameters
import com.example.mzrtc.rtc.classrtc.RoomParametersFetcher
import com.example.mzrtc.rtc.client.WebSocketChannelClient
import com.example.mzrtc.rtc.data.ConnectionState
import com.example.mzrtc.rtc.data.MessageType
import com.example.mzrtc.rtc.data.WebSocketConnectionState
import com.example.mzrtc.rtc.interfacertc.AppRTCClient
import com.example.mzrtc.rtc.interfacertc.SignalingEvents
import com.example.mzrtc.rtc.interfacertc.WebSocketChannelEvents
import com.example.mzrtc.rtc.util.AsyncHttpURLConnection
import com.example.mzrtc.utils.jsonPut
import com.example.mzrtc.utils.toJavaCandidate
import com.example.mzrtc.utils.toJsonCandidate
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription


/**
 * Negotiates signaling for chatting with https://appr.tc "rooms".
 * Uses the client<->server specifics of the apprtc AppEngine webapp.
 *
 * <p>To use: create an instance of this object (registering a message handler) and
 * call connectToRoom().  Once room connection is established
 * onConnectedToRoom() callback with room parameters is invoked.
 * Messages to other party (with local Ice candidates and answer SDP) can
 * be sent after WebSocket connection is established.
 */
class WebSocketRTCClient (
    val events : SignalingEvents
) : AppRTCClient, WebSocketChannelEvents {
    val TAG = this::class.java.simpleName
    var roomState : ConnectionState = ConnectionState.NEW
    var handler : Handler
    private var messageUrl: String = ""
    private var leaveUrl: String = ""

    private var initiator : Boolean = false

    lateinit var connectionParameters : RoomConnectionParameters
    private var wsClient: WebSocketChannelClient? = null

    init {
        // 왜 있는거지?
        val handlerThread = HandlerThread(TAG)
        handlerThread.start()

        handler = Handler(handlerThread.looper)
    }

    /** AppRTCClient Implementation **/
    // send ice candidate to the other participant.
    override fun sendLocalIceCandidate(candidate: IceCandidate) {
        handler.post {
            val json = JSONObject().apply {
                jsonPut("type","candidate")
                jsonPut("label", candidate.sdpMLineIndex)
                jsonPut("id", candidate.sdpMid)
                jsonPut("candidate", candidate.sdp)
            }

            if(initiator){
                if(roomState!=ConnectionState.CONNECTED){
                    reportError("Sending ICE candidate in non connected state.")
                    return@post
                }

                sendPostMessage(MessageType.MESSAGE, messageUrl, "$json")

                if(connectionParameters.loopback){
                    events.onRemoteIceCandidate(candidate)
                }
            } else {
                // call receiver sends ice candidates to websocket server.
                wsClient?.send("$json")
            }
        }
    }

    override fun disconnectFromRoom() {
        handler.post { 
            disconnectFromRoomInternal()
            handler.looper.quit()
        }
    }

    override fun connectToRoom(connectionParameters: RoomConnectionParameters) {
        this.connectionParameters = connectionParameters
        handler.post {
            connectToRoomInternal()
        }
    }

    // Send local offer SDP to the other participant.
    override fun sendOfferSdp(sdp: SessionDescription) {
        handler.post { 
            if(roomState!=ConnectionState.CONNECTED){
                reportError("Sending offer SDP in non connected state.")
            }
            
            var json = JSONObject().apply { 
                jsonPut("sdp", sdp.description)
                jsonPut("type", "offer")
            }
            sendPostMessage(MessageType.MESSAGE, messageUrl, "$json")
            if( connectionParameters.loopback ){
                var sdpAnswer = SessionDescription( SessionDescription.Type.fromCanonicalForm("answer"), sdp.description)
                events.onRemoteDescription(sdpAnswer)
            }
        }
    }

    // Send local answer SDP to the other participant.
    override fun sendAnswerSdp(sdp: SessionDescription) {
        handler.post {
            if(connectionParameters.loopback){
                Log.e(TAG, "Sending answer in loopback mode.")
                return@post
            }

            var json = JSONObject().apply {
                jsonPut("sdp", sdp.description)
                jsonPut("type", "answer")
            }
            wsClient?.send("$json")
        }
    }

    // send remove ice candidates to the other participant.
    override fun sendLocalIceCandidateRemovals(candidates: Array<IceCandidate?>) {
        handler.post {
            var json = JSONObject().apply {
                jsonPut("type","remove-candidates")
            }

            var jsonArray = JSONArray().apply {
                for (candidate in candidates) { put(toJsonCandidate(candidate)) }
            }

            json.jsonPut("candidates",jsonArray)

            if(initiator){
                // call initiator sends ice candidates to GAE server.
                if(roomState!=ConnectionState.CONNECTED){
                    reportError("Sending ICE candidates removals in non connected state.")
                    return@post
                }

                sendPostMessage(MessageType.MESSAGE, messageUrl, "$json")
                if(connectionParameters.loopback){
                    events.onRemoteIceCandidatesRemoved(candidates = candidates)
                }
            } else {
                wsClient?.send("$json")
            }
        }
    }

    /** WebSocketChannelEvents Implementation **/
    // --------------------------------------------------------------------
    // All events are called by WebSocketChannelClient on a local looper thread
    // (passed to WebSocket client constructor).
    override fun onWebSocketMessage(message: String?) {
        wsClient?.let { wsClient_ ->
            if( wsClient_.state != WebSocketConnectionState.REGISTERED ){
                Log.e(TAG, "Got WebSocket message in non registered state.")
                return
            }

            try{
                var json = JSONObject(message)
                var msgText = json.getString("msg")
                var errorText = json.optString("error")

                if(msgText.isNotEmpty()){
                    json = JSONObject(msgText)
                    var type = json.optString("type")
                    when(type){
                        "candidates" -> { // iceCandidates 받는거같다
                            events.onRemoteIceCandidate(toJavaCandidate(json))
                        }
                        "remove-candidates" -> {
                            var candidateArray = json.getJSONArray("candidates")
                            val candidates = arrayOfNulls<IceCandidate>(candidateArray.length())
                            events.onRemoteIceCandidatesRemoved(candidates)
                        }
                        "answer" -> {
                            if(initiator){
                                var sdp = SessionDescription( SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp") )
                                events.onRemoteDescription(sdp)
                            } else {
                                reportError("Received answer for call initiator : $message")
                            }
                        }
                        "offer" -> {
                            if(initiator){
                                reportError("Received offer for call recevier : $message")
                            } else {
                                var sdp = SessionDescription(SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"))
                                events.onRemoteDescription(sdp)
                            }
                        }
                        "bye" -> { events.onChannelClose() }
                        else -> { reportError("Unexpected WebSocket message : $message") }
                    }
                } else {
                    if(errorText!=null && errorText.isNotEmpty()){
                        reportError("WebSocket error message: $errorText")
                    } else {
                        reportError("Unexpected WebSocket message : $message")
                    }
                }
            }catch (e:JSONException){
                reportError("WebSocket message JSON parsing error : $e")
            }
        }
    }
    override fun onWebSocketClose() = events.onChannelClose()
    override fun onWebSocketError(description: String?) = reportError("WebSocket error : $description")



    private fun connectToRoomInternal(){
        val connectionUrl = getConnectionUrl(connectionParameters)
//        val connectionUrl = "https://appr.tc/leave/8080/95113852"
        Log.d(TAG, "Connect to room: $connectionUrl")

        roomState = ConnectionState.NEW
        wsClient = WebSocketChannelClient(handler = handler, events = this@WebSocketRTCClient)

        val callbacks =  object : RoomParametersFetcher.RoomParametersFetcherEvents {
            override fun onSignalingParametersError(description: String) {
                this@WebSocketRTCClient.reportError(description)
            }

            override fun onSignalingParametersReady(params: SignalingParameters) {
                handler.post {
                    this@WebSocketRTCClient.signalingParametersReady(params)
                }
            }
        }

        RoomParametersFetcher(connectionUrl, null, callbacks).makeRequest()
    }

    // Callback issued when room parameters are extracted. Runs on local
    // looper thread.
    fun signalingParametersReady(signalingParameters: SignalingParameters){
        if(connectionParameters.loopback && ( !signalingParameters.initiator || signalingParameters.offerSdp != null ) ){
            reportError("Loopback room is busy.")
            return
        }

        if (!connectionParameters.loopback && !signalingParameters.initiator && signalingParameters.offerSdp == null) {
            Log.w(TAG, "No offer SDP in room response.")
        }

        initiator = signalingParameters.initiator
        messageUrl = getMessageUrl(connectionParameters, signalingParameters)
        leaveUrl = getLeaveUrl(connectionParameters, signalingParameters)
        Log.d(TAG, "Message URL: $messageUrl  ,  leave Url : $leaveUrl")
        roomState = ConnectionState.CONNECTED

        // fire connection and signaling parameters events.
        events.onConnectedToRoom(signalingParameters)

        // connect and register webSocket client.
        wsClient?.run {
            signalingParameters.wssUrl?.let {  wssUrl_ ->
                signalingParameters.wssPostUrl?.let { wssPostUrl_ ->
                    signalingParameters.clientId?.let { cId ->
                        connect(wssUrl_, wssPostUrl_)
                        register(connectionParameters.roomId, cId)
                    }
                }
            }
        }
    }

    // Send SDP or ICE candidate to a room server
    fun sendPostMessage(
        messageType: MessageType, url :String, message : String?
    ){
        var loginfo = url
        message?.let { loginfo += ". Message: $it" }

        Log.d(TAG, "C->GAE: $loginfo")

        val httpConnection = AsyncHttpURLConnection("POST", url, message, object : AsyncHttpURLConnection.AsyncHttpEvents{
            override fun onHttpError(errorMessage: String?) {
                reportError("GAE POST error : $errorMessage")
            }

            override fun onHttpComplete(response: String) {
                if(messageType==MessageType.MESSAGE){
                    try{
                        var roomJson = JSONObject(response)
                        var result = roomJson.getString("result")
                        if(result != "SUCCESS"){
                            reportError("GAE POST error : $result")
                        }
                    }catch (e:Exception){
                        reportError("GAE POST JSON error : $e")
                    }
                }
            }
        })

        httpConnection.send()
    }

    // Disconnect from room and send bye messages - runs on a local looper thread.
    fun disconnectFromRoomInternal(){
        if( roomState == ConnectionState.CONNECTED ){
            sendPostMessage( MessageType.LEAVE , leaveUrl, null )
        }

        roomState = ConnectionState.CLOSED
        wsClient?.run { disConnect( true ) }
    }

    /** util of private  **/
    // Helper functions to get connection, post message and leave message URLs
    private fun getConnectionUrl(connectionParameters: RoomConnectionParameters): String {
        return (connectionParameters.roomUrl + "/" + ROOM_JOIN + "/" + connectionParameters.roomId
                + getQueryString(connectionParameters))
    }


    private fun getMessageUrl(
        connectionParameters: RoomConnectionParameters, signalingParameters: SignalingParameters
    ): String {
        return (connectionParameters.roomUrl + "/" + ROOM_MESSAGE + "/" + connectionParameters.roomId
                + "/" + signalingParameters.clientId + getQueryString(connectionParameters))
    }

    private fun getLeaveUrl(
        connectionParameters: RoomConnectionParameters, signalingParameters: SignalingParameters
    ): String {
        return (connectionParameters.roomUrl + "/" + ROOM_LEAVE + "/" + connectionParameters.roomId + "/"
                + signalingParameters.clientId + getQueryString(connectionParameters))
    }

    private fun getQueryString(connectionParameters: RoomConnectionParameters): String {
        return if (connectionParameters.urlParameters != null) {
            "?" + connectionParameters.urlParameters
        } else {
            ""
        }
    }


    // --------------------------------------------------------------------
    // Helper functions.
    private fun reportError(errorMessage: String) {
        Log.e(TAG, errorMessage)
        handler.post {
            if (roomState != ConnectionState.ERROR) {
                roomState = ConnectionState.ERROR
                events.onChannelError(errorMessage)
            }
        }
    }

}