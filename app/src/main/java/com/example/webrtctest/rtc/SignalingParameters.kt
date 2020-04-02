package com.example.webrtctest.rtc

import org.webrtc.IceCandidate
import org.webrtc.PeerConnection.IceServer
import org.webrtc.SessionDescription

/**
 * Struct holding the signaling parameters of an AppRTC room.
 */
class SignalingParameters(
    val iceServers: List<IceServer>,
    val initiator: Boolean,
    val clientId: String?,
    val wssUrl: String?,
    val wssPostUrl: String?,
    val offerSdp: SessionDescription?,
    val iceCandidates: List<IceCandidate>?
)