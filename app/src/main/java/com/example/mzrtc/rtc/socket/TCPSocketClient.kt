package com.example.mzrtc.rtc.socket

import android.util.Log
import com.example.mzrtc.model.data.TCPChannelClient_TAG
import com.example.mzrtc.rtc.client.TCPChannelClient
import com.example.mzrtc.rtc.interfacertc.TCPChannelEvents
import java.net.InetAddress
import java.net.Socket

class TCPSocketClient(
    private val tcpChannelClient: TCPChannelClient,
    private val eventListener : TCPChannelEvents,
    private var address : InetAddress,
    private var port : Int
) : TCPSocket(tcpChannelClient, eventListener) {

    // Peer 에 연결!!
    override fun connect(): Socket? {
        Log.d(TCPChannelClient_TAG, "Connecting to [ ${address.hostAddress} ]: $port")

        return try{
            Socket(address, port)
        }catch (e:Exception){
            // reportError("Failed to connect: " + e.getMessage());
            null
        }
    }

    // 왜 false 고정 이지? -> 이건 클라이언트 클래스니까 false 고정
    override fun isServer(): Boolean {
        return false
    }
}