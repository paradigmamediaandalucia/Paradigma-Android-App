package com.example.paradigmaapp.android.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.exception.Failure
import com.example.paradigmaapp.model.Episode
import com.example.paradigmaapp.model.Programa
import com.example.paradigmaapp.repository.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class EpisodeDetailUiState {
    data object Loading : EpisodeDetailUiState()
    data class Success(val episode: Episode) : EpisodeDetailUiState()
    data class Error(val failure: Failure) : EpisodeDetailUiState()
}

class EpisodeDetailViewModel(
    private val episodeId: String,
    private val repository: Repository
) : ViewModel() {

    private val _episode = MutableStateFlow<Episode?>(null)
    val episode: StateFlow<Episode?> = _episode

    private val _associatedPrograms = MutableStateFlow<List<Programa>>(emptyList())
    val associatedPrograms: StateFlow<List<Programa>> = _associatedPrograms

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadEpisodeDetails()
    }

    fun loadEpisodeDetails() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getEpisodeDetail(episodeId).fold(
                { failure ->
                    _error.value = when (failure) {
                        is Failure.NetworkConnection -> "No hay conexión a internet."
                        is Failure.ServerError -> "Error del servidor. Inténtalo de nuevo más tarde."
                        is Failure.CustomError -> failure.message
                        is Failure.FeatureFailure -> "Error desconocido."
                    }
                    _isLoading.value = false
                },
                { episode ->
                    _episode.value = episode
                    _isLoading.value = false
                    episode.programaIds?.let { ids ->
                        if (ids.isNotEmpty()) {
                            loadAssociatedPrograms(ids)
                        }
                    } ?: episode.programId.let { id ->
                        if (id.isNotBlank()) {
                            loadAssociatedPrograms(listOf(id))
                        }
                    }
                }
            )
        }
    }

    private fun loadAssociatedPrograms(programIds: List<String>) {
        viewModelScope.launch {
            val programs = mutableListOf<Programa>()
            for (id in programIds) {
                repository.getProgramaDetail(id).fold(
                    {}, // Handle failure if needed, but for now, just skip if a program fails to load
                    { programa -> programs.add(programa) }
                )
            }
            _associatedPrograms.value = programs
        }
    }

    fun saveEpisode(episode: Episode) {
        viewModelScope.launch {
            repository.saveEpisode(episode)
        }
    }
}
