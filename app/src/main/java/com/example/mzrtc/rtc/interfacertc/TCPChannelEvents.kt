package com.example.mzrtc.rtc.interfacertc

/**
 *  Callback interface for messages delivered on TCP Connection. All callbacks are invoked from the
 *  looper executor thread.
 */
interface TCPChannelEvents {
    fun onTCPConnected(server: Boolean)
    fun onTCPMessage(message: String?)
    fun onTCPError(description: String?)
    fun onTCPClose()
}