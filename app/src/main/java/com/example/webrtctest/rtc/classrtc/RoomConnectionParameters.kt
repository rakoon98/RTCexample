package com.example.webrtctest.rtc.classrtc

/**
 * Struct holding the connection parameters of an AppRTC room.
 */
class RoomConnectionParameters @JvmOverloads constructor(
    val roomUrl: String,
    val roomId: String,
    val loopback: Boolean,
    val urlParameters: String? = null /* urlParameters */
)