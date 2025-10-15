package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.media3.exoplayer.ExoPlayer
import com.example.paradigmaapp.android.api.AndainaStream
import com.example.paradigmaapp.android.data.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel que gestiona el estado y la persistencia del volumen de la aplicación.
 *
 * @param appPreferences Una instancia de AppPreferences para acceder a los datos persistentes.
 */
class VolumeControlViewModel(
    private val appPreferences: AppPreferences
) : ViewModel() {

    // Utiliza MutableStateFlow para mantener el estado del volumen.
    private val _volume = MutableStateFlow(appPreferences.loadVolume())

    // Expone el estado como StateFlow de solo lectura para la UI.
    val volume: StateFlow<Float> = _volume.asStateFlow()

    /**
     * Actualiza y guarda el nivel de volumen en las preferencias compartidas.
     *
     * @param newVolume El nuevo valor de volumen (entre 0.0f y 1.0f).
     */
    fun setAndSaveVolume(newVolume: Float) {
        val coercedVolume = newVolume.coerceIn(0f, 1f)
        _volume.value = coercedVolume
        appPreferences.saveVolume(coercedVolume)
    }

    /**
     * Aplica el volumen guardado en los reproductores de audio.
     * Esta función debe ser llamada después de que los reproductores hayan sido inicializados.
     *
     * @param podcastPlayer El ExoPlayer para podcasts.
     * @param andainaStreamPlayer El reproductor del stream de Andaina.
     */
    fun applySavedVolume(podcastPlayer: ExoPlayer, andainaStreamPlayer: AndainaStream) {
        // Obtenemos el valor de volumen actual del StateFlow
        val savedVolume = _volume.value

        // Aplicamos el volumen a los reproductores
        podcastPlayer.volume = savedVolume
        andainaStreamPlayer.exoPlayer?.volume = savedVolume
    }
}
