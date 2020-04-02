package com.example.webrtctest.relive

import org.webrtc.VideoFrame
import org.webrtc.VideoSink

class ProxyVideoSink : VideoSink {
    private var target : VideoSink? = null
    override fun onFrame(frame: VideoFrame?) {
        target?.let {
            it.onFrame(frame)
        } ?: kotlin.run {
            return@run
        }
    }

    @Synchronized
    fun setTarget(target:VideoSink){this.target = target}
}