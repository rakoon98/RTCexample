package com.example.mzrtc.rtc.socket

import android.util.Log
import com.example.mzrtc.model.data.TCPChannelClient_TAG
import com.example.mzrtc.rtc.client.TCPChannelClient
import com.example.mzrtc.rtc.interfacertc.TCPChannelEvents
import java.io.*
import java.net.Socket
import java.nio.charset.Charset

/**
 * Base class for server and client sockets.
 * Contains a listening thread that will call eventListener.
 * onTCPMessage on new messages.
 *
 * 새로운 메시지를 읽는 서버와 클라이언트소켓을 위한 기본 클래스?
 */
abstract class TCPSocket(
    private val tcpChannelClient: TCPChannelClient,
    private val eventListener : TCPChannelEvents
) : Thread() {

    var rawSocketlock = Any()
    private var out: PrintWriter? = null
    private var rawSocket: Socket? = null

    /**
     * Connect to the peer, potentially a slow operation.
     * @return Socket connection, null if connection failed.  <-- 중요
     */
    abstract fun connect(): Socket?

    /** Returns true if sockets is a server rawSocket.  */
    abstract fun isServer(): Boolean

    /**
     *   The listening thread.  듣는 쓰레드로 판단됨.
     */
    override fun run() {
        super.run()

        Log.d(TCPChannelClient_TAG, "Listening thread started...")
        // Receive connection to temporary variable first, so we don't block.

        // Receive connection to temporary variable first, so we don't block.
        val tempSocket = connect()
        var bReader: BufferedReader ?= null

        Log.d(TCPChannelClient_TAG, "TCP connection established.")

        synchronized(rawSocketlock) {
//            if (rawSocket != null) { Log.e(TAG, "Socket already existed and will be replaced.") }

            rawSocket = tempSocket
            rawSocket?.let {  rSocket ->
                try{
                    out = PrintWriter( OutputStreamWriter( rSocket.getOutputStream(), Charset.forName("UTF-8")), true )
                    bReader = BufferedReader( InputStreamReader( rSocket.getInputStream(), Charset.forName("UTF-8") ) )
                }catch (e:Exception){
                    // reportError ...
                    return
                }
            } ?: kotlin.run { return@synchronized }
        }

        Log.v(TCPChannelClient_TAG, "Execute onTCPConnected")
        tcpChannelClient.executorService.execute( Runnable {
            Log.v(TCPChannelClient_TAG, "Run onTCPConnected")
            eventListener.onTCPConnected(isServer())
        })

        while (true) {
            var message = try {
                bReader?.readLine()
            } catch (e: IOException) {
                synchronized(rawSocketlock) {
                    // If socket was closed, this is expected.
                    if(rawSocket==null) return
                }
                break // reportError("Failed to read from rawSocket: " + e.message)
            } ?: break   // No data received, rawSocket probably closed.

            tcpChannelClient.executorService.execute {
                Log.v(TCPChannelClient_TAG, "Receive: $message")
                eventListener.onTCPMessage(message)
            }
        }

        Log.d(TCPChannelClient_TAG, "Receiving thread exiting...")

        // Close the rawSocket if it is still open.
        disconnect()
    }

    /** 연결해제 : Closes the rawSocket if it is still open. Also fires the onTCPClose event. */
    open fun disconnect(){
        try{
            synchronized(rawSocketlock){
                rawSocket?.let { rSocket ->
                    rSocket.close()
                    rawSocket = null
                    out = null

                    tcpChannelClient.executorService.execute {
                        eventListener.onTCPClose()
                    }
                }
            }
        }catch (e:IOException){
            // reportError("Failed to close rawSocket: " + e.getMessage());
        }
    }


    /**
     *  --전송
     *  Sends a message on the socket. Should only be called on the executor thread.
     */
    fun send( message:String ){
        Log.v(TCPChannelClient_TAG, "Send: $message")
        synchronized(rawSocketlock){
            out?.let {
                it.write(message + "\n")
                it.flush()
            } ?: run{
//                reportError("Sending data on closed socket.")
                return
            }
        }
    }
}