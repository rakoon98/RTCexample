package com.example.webrtctest.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.webrtctest.R
import kotlinx.android.synthetic.main.activity_video_chat.*
import org.webrtc.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_chat)

        initting()
    }

    fun initting(){
//        PeerConnectionFactory.initializeAndroidGlobals(this,true)
//
//        val options = PeerConnectionFactory.Options()
//        var peerConnectionFactory = PeerConnectionFactory(options)

//        var videoCapturerAndroid = getVideoCapturer( CustomCameraEventsHandler() )

        // 비디오 인스턴스
//        var videoSource = peerConnectionFactory.createVideoSource(videoCapturerAndroid)
//        var videoTrack = peerConnectionFactory.createVideoTrack("100",videoSource)

        // 카메라로부터 비디오 캡쳐를 시작
        // 파라미터는 width, hegith , fps
//        videoCapturerAndroid.startCapture(1000,1000,30)

        fullscreen_video_view.setMirror(true)
        var rootEglBase = EglBase.create()
        fullscreen_video_view.init(rootEglBase.eglBaseContext, null)
//        videoTrack.addRenderer(VideoRenderer(fullscreen_video_view))



    }

    fun makeVideoCapture(enumerator: CameraEnumerator) : VideoCapturer? {
        val deviceNames = enumerator.deviceNames

        // 후면 카메
        for( dName in deviceNames ){
            if(enumerator.isBackFacing(dName)){
                var videoCapturer = enumerator.createCapturer(dName, null)

                if(videoCapturer!=null) return videoCapturer
            }
        }

        // 후면 카메라를 찾을 수 없다면 다른 카메라를 찾는다
        for( dName in deviceNames ){
            if(!enumerator.isBackFacing(dName)){
                var videoCapturer = enumerator.createCapturer(dName, null)
                if(videoCapturer!=null) return videoCapturer
            }
        }

        return null
    }


}