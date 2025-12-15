package com.example.lightcontrolapp.utils

fun minsToHHMM(m: Int): String {
    val h = m / 60
    val min = m % 60
    return "%02d:%02d".format(h, min)
}
