package com.example.mzrtc.testsampletry.data

import org.webrtc.SessionDescription

data class SessionDescriptionsType(
    val type : String,
    val description : SessionDescription
)

data class TestData(
    val test1 : String,
    val test2 : Int,
    val test3 : Any,
    val test4 : Long
)