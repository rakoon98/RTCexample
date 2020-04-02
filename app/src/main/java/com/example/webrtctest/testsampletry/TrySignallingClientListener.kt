package me.amryousef.webrtc_demo

import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface TrySignallingClientListener {
    fun onConnectionEstablished()
    fun onOfferReceived(description: SessionDescription)
    fun onAnswerReceived(description: SessionDescription)
    fun onIceCandidateReceived(iceCandidate: IceCandidate)
}