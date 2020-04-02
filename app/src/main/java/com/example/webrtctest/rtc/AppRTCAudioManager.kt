package com.example.webrtctest.rtc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import com.example.webrtctest.rtc.data.AudioDevice
import com.example.webrtctest.rtc.data.AudioManagerState
import com.example.webrtctest.rtc.interfacertc.AudioManagerEvents
import org.webrtc.ThreadUtils


/**
 *  구현은 일단 최소화하고 기본적인것만 구현후 나머지를 채우는방식으로 진행.
 */
class AppRTCAudioManager(
    var context: Context
) {

    private val TAG = this::class.java.simpleName
    private val SPEAKERPHONE_AUTO = "auto"
    private val SPEAKERPHONE_TRUE = "true"
    private val SPEAKERPHONE_FALSE = "false"


    var amState : AudioManagerState?= null
    var audioManagerEvents : AudioManagerEvents? = null
    var savedAudioMode : Int = AudioManager.MODE_INVALID

    private var savedIsSpeakerPhoneon : Boolean = false
    private var savedIsMicrophoneMute : Boolean = false
    private var hasWiredHeadset : Boolean = false
    private lateinit var audioManager : AudioManager

    // Broadcast receiver for wired headset intent broadcasts.
    private var wiredHeadsetReceiver: BroadcastReceiver? = null

    // default audio device; speaker phone for video calls or earpiece for audio only calls.
    private var defaultAudioDevice : AudioDevice? = null

    // Contains the currently selected audio device
    // This device is changed automatically using a certain scheme where e.g.
    // a wired headset "wins" over speaker phone.
    // it is also possible for a user to explicitily select a device ( and override any perdefined scheme )
    // see |userSelectedAudioDevice| for details.
    private var selectedAudioDevice : AudioDevice? = null

    /** Construction.  */
    fun create(context: Context): AppRTCAudioManager = AppRTCAudioManager(context)

    init {
        ThreadUtils.checkIsOnMainThread()

        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//        bluetoothManager = AppRTCBluetoothManager.create(context, this)
        wiredHeadsetReceiver = WiredHeadsetReceiver()

    }


    // TODO(henrika): audioManager.reqeustAudioFocus() is deprecated. : 다른 앱 음악 일시정지하는 기능 api 26 에서 deprecated 됨.
    // TODO : https://gooners0304.tistory.com/entry/%EC%95%88%EB%93%9C%EB%A1%9C%EC%9D%B4%EB%93%9C-Audio-Focus%EB%A5%BC-%EC%9D%B4%EC%9A%A9%ED%95%98%EC%97%AC-%EB%8B%A4%EB%A5%B8-%EC%95%B1-%EC%9D%8C%EC%95%85-%EC%9D%BC%EC%8B%9C-%EC%A0%95%EC%A7%80-%EC%8B%9C%ED%82%A4%EA%B8%B0
    // TODO : 참고하여 requestAudioFocus 관련 메소드 참고하기
    fun AudioManagerEvents.start(){
        ThreadUtils.checkIsOnMainThread()

        if(amState==AudioManagerState.RUNNING){
            Log.e(TAG, "AudioManager is already active"); return
        }

        // TODO(HENRIKA) : perhaps call new method called preInitAudio() here if UNINITIALIZED.
        Log.d(TAG, "AudioManager starts...")
        this@AppRTCAudioManager.audioManagerEvents = this
        amState = AudioManagerState.RUNNING

        // store current audio state so we can restore it when stop() is called
        audioManager.run {
            savedAudioMode = mode
            savedIsSpeakerPhoneon = isSpeakerphoneOn
            savedIsMicrophoneMute = isMicrophoneMute
        }

//        hasWiredHeadset = hasWiredHeadset()

    }









    /**
     *   receiver which handles changes in wired headset availability.
     */
    class WiredHeadsetReceiver : BroadcastReceiver() {
        private val STATE_UNPLUGGED = 0
        private val STATE_PLUGGED = 1
        private val HAS_NO_MIC = 0
        private val HAS_MIC: Int = 1

        override fun onReceive(context: Context, intent: Intent) {

            var state = intent.getIntExtra("state", STATE_UNPLUGGED)
            var microphone = intent.getIntExtra("microphone",HAS_NO_MIC)
            var name = intent.getStringExtra("name")

//            hasWiredHeadset = (state==STATE_PLUGGED)
//            updateAudioDeviceState()
        }
    }
}