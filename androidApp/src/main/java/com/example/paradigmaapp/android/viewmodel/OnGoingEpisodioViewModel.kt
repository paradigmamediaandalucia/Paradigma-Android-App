package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.model.Episode
import com.example.paradigmaapp.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel para gestionar los Episodes cuya reproducción está en curso o ha sido pausada.
 * Esta versión es autosuficiente para el modo offline.
 *
 * @property appPreferences Instancia de [AppPreferences] para persistir y recuperar el progreso y los detalles del Episode.
 * @property repository Repositorio para obtener detalles de Episodes.
 *
 * @author Mario Alguacil Juárez
 */
class OnGoingEpisodeViewModel(
    private val appPreferences: AppPreferences,
    private val repository: Repository
) : ViewModel() {

    private val _onGoingEpisodes = MutableStateFlow<List<Episode>>(emptyList())
    val onGoingEpisodes: StateFlow<List<Episode>> = _onGoingEpisodes.asStateFlow()

    init {
        refrescarListaEpisodesEnCurso()
    }

    /**
     * Carga/Refresca la lista de Episodes en curso.
     * Obtiene todas las posiciones de reproducción guardadas y, para cada una
     * con progreso significativo (> 0), carga los detalles completos del Episode desde AppPreferences.
     */
    fun refrescarListaEpisodesEnCurso() {
        viewModelScope.launch(Dispatchers.IO) {
            val episodePositions = appPreferences.getAllEpisodePositions()
            val EpisodesEnCurso = mutableListOf<Episode>()

            for ((idStr, position) in episodePositions) {
                if (position > 0) {
                    // El ID del Episode ahora es String, no Int.
                    appPreferences.loadEpisodeDetails(idStr)?.let { Episode ->
                        EpisodesEnCurso.add(Episode)
                    }
                }
            }

            val EpisodesOrdenados = EpisodesEnCurso.sortedByDescending {
                appPreferences.getEpisodePosition(it.id)
            }

            withContext(Dispatchers.Main) {
                _onGoingEpisodes.value = EpisodesOrdenados
            }
        }
    }

    /**
     * Guarda los detalles de un Episode y refresca la lista de "en curso".
     * Se llama cuando el progreso de un Episode cambia significativamente.
     *
     * @param episode El Episode actualizado.
     */
    fun addOrUpdateOnGoingEpisode(episode: Episode) {
        viewModelScope.launch(Dispatchers.IO) {
            appPreferences.saveEpisodeDetails(episode)
            refrescarListaEpisodesEnCurso() // Refresca la lista para reflejar cualquier cambio
        }
    }
}
