package com.example.mzrtc.utils

import android.util.Log
import com.example.mzrtc.model.data.*
import com.example.mzrtc.rtc.classrtc.PeerConnectionParameters

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

