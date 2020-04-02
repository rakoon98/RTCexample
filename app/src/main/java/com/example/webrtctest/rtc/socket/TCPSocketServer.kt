package com.example.webrtctest.rtc.socket

import android.util.Log
import com.example.webrtctest.model.data.TCPChannelClient_TAG
import com.example.webrtctest.rtc.client.TCPChannelClient
import com.example.webrtctest.rtc.interfacertc.TCPChannelEvents
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class TCPSocketServer(
    private val tcpChannelClient: TCPChannelClient,
    private val eventListener : TCPChannelEvents,
    private var address : InetAddress,
    private var port : Int
) : TCPSocket(tcpChannelClient, eventListener) {
    private var serverSocket: ServerSocket? = null
    var tempSocket : ServerSocket ?= null

    override fun connect(): Socket? {
        Log.d(TCPChannelClient_TAG, "Listening on [ ${address.hostAddress} ]: $port")

        try{
            tempSocket = ServerSocket(port,0, address)
        }catch (e:IOException){
//            reportError("Failed to create server socket: " + e.message)
            return null
        }

        synchronized(rawSocketlock){
            serverSocket?.let {
                Log.e(TCPChannelClient_TAG, "Server rawSocket was already listening and new will be opened.")
            } ?: kotlin.run {
                serverSocket = tempSocket
            }
        }

        return try {
            serverSocket!!.accept()
        }catch (e:Exception){
            //  reportError("Failed to receive connection: " + e.getMessage());
            null
        }
    }

    override fun disconnect() {
        try{
            synchronized(rawSocketlock){
                serverSocket?.let { sSocket ->
                    sSocket.close()
                    serverSocket = null
                }
            }
        }catch (e:IOException){
//            reportError("Failed to close server socket: ${e.message)")
        }
        super.disconnect()
    }

    // 이건 서버니까 true 고정
    override fun isServer(): Boolean {
        return true
    }

}