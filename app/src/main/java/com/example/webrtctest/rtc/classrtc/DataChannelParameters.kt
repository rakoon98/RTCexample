package com.example.webrtctest.rtc.classrtc

/**
 *   Peer connection parameters.
 */
class DataChannelParameters(
    val ordered: Boolean, val maxRetransmitTimeMs: Int, val maxRetransmits: Int,
    val protocol: String, val negotiated: Boolean, val id: Int
)