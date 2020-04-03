package com.example.mzrtc.relive

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Build
import androidx.annotation.RequiresApi
import org.webrtc.*

fun Context.createVideoCapture(
    videoFifleAsCamera: String?,
    screencaptureEnabled:Boolean,
    mediaProjectionPermissionResultData : Intent
) : VideoCapturer? {
    return when {
        videoFifleAsCamera != null -> {
            var videoCapturer = FileVideoCapturer(videoFifleAsCamera)
            videoCapturer
        }
        screencaptureEnabled -> {
            createScreenCaptrue(mediaProjectionPermissionResultData)
        }
        useCamera2() -> {
            createCameraCapturer(Camera2Enumerator(this))
        }
        else -> {
            null
        }
    }
}

fun createScreenCaptrue(mediaProjectionPermissionResultData:Intent) : VideoCapturer {
    return ScreenCapturerAndroid(mediaProjectionPermissionResultData,
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        object : MediaProjection.Callback() {
            override fun onStop() {
//                reportError("User revoked permission to capture the screen.")
            }
        })
}

fun Context.useCamera2() : Boolean {
    return Camera2Enumerator.isSupported(this)
}

fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
    val deviceNames = enumerator.deviceNames

    // First, try to find front facing camera
    for (deviceName in deviceNames) {
        if (enumerator.isFrontFacing(deviceName)) {
            val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
            if (videoCapturer != null) {
                return videoCapturer
            }
        }
    }

    // Front facing camera not found, try something else
    for (deviceName in deviceNames) {
        if (!enumerator.isFrontFacing(deviceName)) {
            val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
            if (videoCapturer != null) {
                return videoCapturer
            }
        }
    }
    return null
}