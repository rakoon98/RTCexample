package com.example.mzrtc.rtc.classrtc

import com.example.mzrtc.model.data.ProxyVideoSink_TAG
import org.webrtc.Logging
import org.webrtc.VideoFrame
import org.webrtc.VideoSink

class ProxyVideoSink : VideoSink {
    private var target : VideoSink? = null
    override fun onFrame(frame: VideoFrame) {
        target?.let {
            it.onFrame(frame)
        }?: kotlin.run {
            Logging.d(ProxyVideoSink_TAG,"Dropping frame in proxy because target is null")
        }
    }

    @Synchronized
    fun setTarget(target:VideoSink){
        this.target = target
    }
}