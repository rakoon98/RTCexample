package com.example.webrtctest.rtc.interfacertc

import com.example.webrtctest.rtc.data.AudioDevice

interface AudioManagerEvents {

    fun onAudioDeviceChanged(
        selectedAudioDevice: AudioDevice,
        availableAudioDevices : Set<AudioDevice>
    )

}