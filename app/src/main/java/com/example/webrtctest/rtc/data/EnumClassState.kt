package com.example.webrtctest.rtc.data

enum class ConnectionState {
    NEW, CONNECTED, CLOSED, ERROR
}

enum class WebSocketConnectionState{
    NEW, CONNECTED, REGISTERED, CLOSED, ERROR
}

enum class MessageType {
    MESSAGE, LEAVE
}

/**
 * AudioDevice is the names of possible audio devices that we currently
 * support.
 */
enum class AudioDevice {
    SPEAKER_PHONE, WIRED_HEADSET, EARPIECE, BLUETOOTH, NONE
}

/** AudioManager state.  */
enum class AudioManagerState {
    UNINITIALIZED, PREINITIALIZED, RUNNING
}