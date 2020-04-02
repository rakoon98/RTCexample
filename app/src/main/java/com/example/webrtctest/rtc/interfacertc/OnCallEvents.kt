package com.example.webrtctest.rtc.interfacertc

import org.webrtc.RendererCommon.ScalingType

/**
 *  Call control interface for container activity.
 */
interface OnCallEvents {
    fun onCallHangUp()
    fun onCameraSwitch()
    fun onVideoScalingSwitch(scalingType: ScalingType?)
    fun onCaptureFormatChange(width: Int, height: Int, framerate: Int)
    fun onToggleMic(): Boolean
}