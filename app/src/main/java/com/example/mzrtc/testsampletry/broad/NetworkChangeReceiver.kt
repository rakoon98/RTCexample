package com.example.mzrtc.testsampletry.broad

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.mzrtc.testsampletry.util.NetworkUtil.isNetworkAvailable
import com.example.mzrtc.utils.setLogDebug


class NetworkChangeReceiver : BroadcastReceiver() {

    companion object {
        const val NETWORK_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        setLogDebug("Sulod sa network reciever")

        val status = isNetworkAvailable(context)
        if (NETWORK_CHANGE == intent.action) {
            if (status) {
                // 인터넷이 연결안됨으로 변경됨
                setLogDebug("인터넷 변경 : $status")
            } else {
                // 인터넷이 연결된 상태
                setLogDebug("인터넷 변경 : $status")
            }
        }
    }
}