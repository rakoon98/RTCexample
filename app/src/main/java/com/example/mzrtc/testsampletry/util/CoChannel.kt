package com.example.mzrtc.testsampletry.util

import com.example.mzrtc.model.data.VS_ACTIVITY
import com.example.mzrtc.model.data.VS_VIEWMODEL
import com.example.mzrtc.testsampletry.data.SessionDescriptionsType
import com.example.mzrtc.utils.sdateFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription

class CoChannel {

    val vmChannel: BroadcastChannel<Any> = ConflatedBroadcastChannel()
    val activityChannel: BroadcastChannel<Any> = ConflatedBroadcastChannel()
//    val channel: BroadcastChannel<String> = ConflatedBroadcastChannel()
//    val channelDescription: BroadcastChannel<SessionDescription> = ConflatedBroadcastChannel()
//    val channelCandidate: BroadcastChannel<IceCandidate> = ConflatedBroadcastChannel()

    fun runMain(work: suspend (() -> Unit)) = CoroutineScope(Dispatchers.Main).launch { work() }

    // 이벤트를 보낸다.
    fun sendString(divide : String, o: String) = run {
        when(divide){
            VS_ACTIVITY->{ runMain { activityChannel.send(o) } }
            VS_VIEWMODEL->{ runMain { vmChannel.send(o) }  }
            else->{}
        }
    }

    fun sendSessionDescription(
        divide : String,
        type : String,
        sDescription : SessionDescription
    ) = run {
        when(divide){
            VS_ACTIVITY->{ runMain { activityChannel.send(SessionDescriptionsType(type, sDescription)) } }
            VS_VIEWMODEL->{ runMain { vmChannel.send(SessionDescriptionsType(type, sDescription)) }  }
            else->{}
        }
    }

    fun sendCandidate(
        divide : String,
        candidate: IceCandidate
    ) = run {
        when(divide){
            VS_ACTIVITY->{ runMain { activityChannel.send(candidate) } }
            VS_VIEWMODEL->{ runMain { vmChannel.send(candidate) }  }
            else->{}
        }
    }

    fun sendMediaStream(
        divide : String,
        mStream: MediaStream?
    ) = run {
        when(divide){
            VS_ACTIVITY->{ runMain { mStream?.let { activityChannel.send(it) } } }
            VS_VIEWMODEL->{ runMain { mStream?.let { vmChannel.send(it) } }  }
            else->{}
        }
    }


//    fun send(o: List<GPSData>) {
//        Coroutines.main {
//            bus.send(o)
//        }
//    }
//
//    fun send(o: LatLng) {
//        Coroutines.main {
//            bus.send(o)
//        }
//    }
//
//    inline fun <reified T> asChannel(): ReceiveChannel<T> {
//        return bus.openSubscription().filter { it is T }.map { it as T }
//    }


}