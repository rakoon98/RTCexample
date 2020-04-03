package com.example.mzrtc.rtc.classrtc

/**
 *   Peer connection parameters.
 */
class DataChannelParameters(
    val ordered: Boolean, val maxRetransmitTimeMs: Int, val maxRetransmits: Int,
    val protocol: String, val negotiated: Boolean, val id: Int
)