package com.example.webrtctest.rtc.interfacertc

import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import org.webrtc.StatsReport

/**
 *  Peer connection events.
 */
interface PeerConnectionEvents {
    /**
     * Callback fired once local SDP is created and set.
     */
    fun onLocalDescription(sdp: SessionDescription?)

    /**
     * Callback fired once local Ice candidate is generated.
     */
    fun onIceCandidate(candidate: IceCandidate?)

    /**
     * Callback fired once local ICE candidates are removed.
     */
    fun onIceCandidatesRemoved(candidates: Array<IceCandidate?>?)

    /**
     * Callback fired once connection is established (IceConnectionState is
     * CONNECTED).
     */
    fun onIceConnected()

    /**
     * Callback fired once connection is disconnected (IceConnectionState is
     * DISCONNECTED).
     */
    fun onIceDisconnected()

    /**
     * Callback fired once DTLS connection is established (PeerConnectionState
     * is CONNECTED).
     */
    fun onConnected()

    /**
     * Callback fired once DTLS connection is disconnected (PeerConnectionState
     * is DISCONNECTED).
     */
    fun onDisconnected()

    /**
     * Callback fired once peer connection is closed.
     */
    fun onPeerConnectionClosed()

    /**
     * Callback fired once peer connection statistics is ready.
     */
    fun onPeerConnectionStatsReady(reports: Array<StatsReport?>?)

    /**
     * Callback fired once peer connection error happened.
     */
    fun onPeerConnectionError(description: String?)
}