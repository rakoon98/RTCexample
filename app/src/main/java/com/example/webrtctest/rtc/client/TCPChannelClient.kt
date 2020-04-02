package com.example.webrtctest.rtc.client

import android.util.Log
import com.example.webrtctest.model.data.TCPChannelClient_TAG
import com.example.webrtctest.rtc.interfacertc.TCPChannelEvents
import com.example.webrtctest.rtc.socket.TCPSocket
import com.example.webrtctest.rtc.socket.TCPSocketClient
import com.example.webrtctest.rtc.socket.TCPSocketServer
import org.webrtc.ThreadUtils
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ExecutorService

/**
 * Replacement for WebSocketChannelClient for direct communication between two IP addresses.
 * Handles the signaling between the two clients using a TCP connection.
 * <p>
 * All public methods should be called from a looper executor thread
 * passed in a constructor, otherwise exception will be thrown.
 * All events are dispatched on the same thread.
 */
class TCPChannelClient(
    val executorService: ExecutorService,
    val tcpChannelEvents: TCPChannelEvents,
    val ip : String,
    val port : Int
) {
    var executorThreadChecker : ThreadUtils.ThreadChecker ?= null
    var socket : TCPSocket?= null


    /**
     * Initializes the TCPChannelClient. If IP is a local IP address, starts a listening server on
     * that IP. If not, instead connects to the IP.
     *
     * @param eventListener Listener that will receive events from the client.
     * @param ip            IP address to listen on or connect to.
     * @param port          Port to listen on or connect to.
     */
    init {
        var address = try {
            InetAddress.getByName(ip)
        }catch (e:UnknownHostException){
//            reportError("Invalid IP address.")
            null
        }
        address?.let {
            socket = if(it.isAnyLocalAddress){
                TCPSocketServer(this, tcpChannelEvents, address = address, port = port)
            } else {
                TCPSocketClient(this, tcpChannelEvents, address = address, port = port)
            }
        }

        socket?.start()
    }

    /**
     *  Disconnects the client if not already disconnected.
     *  This will fire the onTCPClose event.
     */
    fun disConnect(){
        executorThreadChecker?.checkIsOnValidThread()
        socket?.disconnect()
    }

    /**
     *  Sends a message on the socket.
     *  @param message Message to be sent.
     */
    fun send(message : String){
        executorThreadChecker?.checkIsOnValidThread()
        socket?.send(message)
    }

    /**
     *  Helper method for firing onTCPError events.
     *  Calls onTCPError on the executor thread.
     */
    fun reportError(message: String){
        Log.e(TCPChannelClient_TAG, "TCP Error: $message")
        executorService.execute { tcpChannelEvents.onTCPError(message) }
    }



}