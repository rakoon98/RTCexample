package com.example.mzrtc.rtc.interfacertc

import com.example.mzrtc.rtc.data.AudioDevice

interface AudioManagerEvents {

    fun onAudioDeviceChanged(
        selectedAudioDevice: AudioDevice,
        availableAudioDevices : Set<AudioDevice>
    )

}