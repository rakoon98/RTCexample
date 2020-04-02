package com.example.webrtctest.utils

import android.util.Log
import com.example.webrtctest.model.data.*
import com.example.webrtctest.rtc.classrtc.PeerConnectionParameters
import com.example.webrtctest.rtc.client.PeerConnectionClient
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule.*

fun PeerConnectionParameters.getFieldTrials() : String {
    var fieldTrials = ""
    if (videoFlexfecEnabled) {
        fieldTrials += VIDEO_FLEXFEC_FIELDTRIAL
        Log.d(PCRTCClient_TAG, "Enable FlexFEC field trial.")
    }
    fieldTrials += VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL
    if (disableWebRtcAGCAndHPF) {
        fieldTrials += DISABLE_WEBRTC_AGC_FIELDTRIAL
        Log.d(PCRTCClient_TAG, "Disable WebRTC AGC field trial.")
    }
    return fieldTrials
}

