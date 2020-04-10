package com.example.mzrtc.testsampletry.util.vns

import android.util.Log
import com.example.mzrtc.App
import com.example.mzrtc.testsampletry.data.*
import com.example.mzrtc.utils.setLogDebug
import com.google.gson.Gson
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

/**
 *   socket listener not implementation it is listener of socket.io
 */
class SocketOnListener(
    val socket: Socket,
    val roomId: String // 서버로 부터 할당받은 룸아이
) {
    val channel = App.coChannel
    val gson = Gson()

    /** 방에 접속 하는 리스너 **/
    val inputRoomListener = Emitter.Listener {
        socket.run {
            emit(Socket.EVENT_MESSAGE, GOT_USER_MEDIA)
        }
    }

    // 연결되면 딱! 발송하는 리스너
    val connectListener = Emitter.Listener {
        emitConnectMessage()
    }

    // 메시지를 받으면 그것의 내용을 받아서 처리하는 리스너
    val messageListener = Emitter.Listener {
        it.forEach { message -> processingGetMessage(message) }
    }

    // 메시지를 받으면 그것의 내용을 받아서 처리하는 리스너
    val errorListener = Emitter.Listener {
//        it.forEach { message -> processingGetMessage(message) }
    }

    // 처음연결되면 발송하는 고정 메시지
    private fun emitConnectMessage() {
        socket.run {
            emit(CREATE_OR_JOIN, roomId)
            emit(Socket.EVENT_MESSAGE, GOT_USER_MEDIA)
        }
    }

    fun sendRTCInfo(dataObject : Any?) = runBlocking {
        socket.run {
            try{
                dataObject?.let {
                    val rtcJson = gson.toJson(dataObject)
//                    val rtcJson = JSONObject("$dataObject")
//                    val rtcJsonType = "${rtcJson["type"]}".toLowerCase()

//                    setLogDebug("type is $rtcJsonType")
                    when {
                        rtcJson.toLowerCase().contains("offer") -> {
                            if(dataObject is SessionDescription){
                                var jsonObject = JSONObject().apply {
                                    put("type", "offer")
                                    put("sdp", dataObject.description)
                                }
                                setLogDebug("data is $dataObject")
                                emit(Socket.EVENT_MESSAGE, jsonObject)
                                } else {
                                // 가져온 데이터가 SessionDescription 이 아니라서 무효처리
                            }
                        }
                        rtcJson.toLowerCase().contains("answer") -> {
                            if (dataObject is SessionDescription) {
                                var jsonObject = JSONObject().apply {
                                    put("type", "answer")
                                    put("sdp", dataObject.description)
                                }

                                Log.d("요호호", "answer data -> ${dataObject.description}")
                                emit(Socket.EVENT_MESSAGE, jsonObject)
                            } else {
                                // 가져온 데이터가 SessionDescription 이 아니라서 무효처리
                            }
                        }
                        rtcJson.toLowerCase().contains("candidate") -> {
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
                            // 알 수 없는 정보가 가져와짐.
                            setLogDebug("wrong type is $dataObject")
                        }
                    }
                } ?: run {
                    setLogDebug("Data is null")
                }
            } catch (e:Exception){
                // 소켓을 통해 받은 데이터를 json converting 해서 상대방에게 보내줄때 나는 에러
                setLogDebug("error is $e")
            }
        }
    }

    // 메시지를 받는것에 대한 프로세싱 하는 메소드
    private fun processingGetMessage(data: Any) {
        when (data) {
            is String -> {
                when (data) {
                    GOT_USER_MEDIA -> {
                        channel.run {
                            setLogDebug("processingGetMessage : $data")
                            runMain { sendString(CREATE_OFFER) }
                        }
                    }
                    BYE -> {
                        channel.run {
                            setLogDebug("processingGetMessage : $data")
                            runMain { sendString(DESTROY) }
                        }
                    }
                }
            }
            else -> {
                try{
                    val info = JSONObject("$data")
                    val type = info["type"]
                    when(type){
                        OFFER -> { // offer 를 받았으면 offer SessionDescription 을 생성하여 전달
                            var sdp = info["sdp"]
                            var sessionDescription = SessionDescription( SessionDescription.Type.OFFER, "$sdp" )
                            channel.run { setLogDebug("send sd offer"); runMain { sendSessionDescription(OFFER, sessionDescription) } }
                        }
                        ANSWER -> { // answer 를 받았으면 answer SessionDescription 을 생성하여 전달
                            var sdp = info["sdp"]
                            var sessionDescription = SessionDescription( SessionDescription.Type.ANSWER, "$sdp" )
                            channel.run { setLogDebug("send sd answer"); runMain { sendSessionDescription(ANSWER, sessionDescription) } }
                        }
                        CANDIDATE -> { // candidate 를 받았으면 IceCandidate 를 생성하여 전달
                            var id = "${info["id"]}"
                            var label = info.getInt("label")
                            var candidate = "${info["candidate"]}"
                            var iceCandidate = IceCandidate( id, label, candidate )

                            channel.run { runMain { sendCandidate(iceCandidate) } }
                        }
                    }
                } catch (e:Exception) {
                    setLogDebug("$e")
                }
            }
        }
    }

}