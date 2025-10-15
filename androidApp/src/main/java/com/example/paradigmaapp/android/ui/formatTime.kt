package com.example.paradigmaapp.android.ui

import java.util.concurrent.TimeUnit

/**
 * Formatea un tiempo en milisegundos a un formato HH:mm:ss o mm:ss.
 *
 * @param millis El tiempo en milisegundos.
 * @return Una cadena de texto con el tiempo formateado.
 */
fun formatTime(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
