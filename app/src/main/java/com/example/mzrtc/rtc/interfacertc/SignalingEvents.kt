package com.example.mzrtc.rtc.interfacertc

import com.example.mzrtc.rtc.SignalingParameters
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

/**
 * Callback interface for messages delivered on signaling channel.
 *
 *
 * Methods are guaranteed to be invoked on the UI thread of |activity|.
 */
interface SignalingEvents {
    /**
     * Callback fired once the room's signaling parameters
     * SignalingParameters are extracted.
     */
    fun onConnectedToRoom(params: SignalingParameters?)

    /**
     * Callback fired once remote SDP is received.
     */
    fun onRemoteDescription(sdp: SessionDescription?)

    /**
     * Callback fired once remote Ice candidate is received.
     */
    fun onRemoteIceCandidate(candidate: IceCandidate?)

    /**
     * Callback fired once remote Ice candidate removals are received.
     */
    fun onRemoteIceCandidatesRemoved(candidates: Array<IceCandidate?>)

    /**
     * Callback fired once channel is closed.
     */
    fun onChannelClose()

    /**
     * Callback fired once channel error happened.
     */
    fun onChannelError(description: String?)
}