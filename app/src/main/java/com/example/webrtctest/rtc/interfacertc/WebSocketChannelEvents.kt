package com.example.webrtctest.rtc.interfacertc


/**
 * Callback interface for messages delivered on WebSocket.
 * All events are dispatched from a looper executor thread.
 */
interface WebSocketChannelEvents {
    fun onWebSocketMessage(message: String?)
    fun onWebSocketClose()
    fun onWebSocketError(description: String?)
}