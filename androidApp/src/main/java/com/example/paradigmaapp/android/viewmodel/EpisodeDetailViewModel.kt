package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.exception.Failure
import com.example.paradigmaapp.model.Episode
import com.example.paradigmaapp.model.Programa
import com.example.paradigmaapp.repository.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de detalle de un [Episode].
 * Se encarga de cargar la información completa del Episode especificado por su ID,
 * el cual se recibe a través de [SavedStateHandle] desde los argumentos de navegación.
 * También extrae los programas asociados al Episode a partir de los datos embebidos.
 *
 * @property repository Repositorio para obtener los datos del Episode.
 * @param savedStateHandle Manejador del estado guardado, utilizado para acceder a los
 * argumentos de navegación (como `episodeId`).
 *
 * @author Mario Alguacil Juárez
 */
class EpisodeDetailViewModel(
    private val repository: Repository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /**
     * El ID del Episode cuyos detalles se van a mostrar.
     * Se obtiene de los argumentos de navegación a través de [SavedStateHandle].
     * Si no se encuentra, se establece a una cadena vacía, indicando un estado inválido.
     */
    val episodeId: String = savedStateHandle.get<String>("episodeId") ?: ""

    /**
     * Contiene el estado del objeto [Episode] actual, con todos sus detalles.
     * Es `null` mientras se carga o si ocurre un error.
     */
    private val _episode = MutableStateFlow<Episode?>(null)
    val episode: StateFlow<Episode?> = _episode.asStateFlow()

    /**
     * Contiene el estado de la lista de [Programa]s asociados a este Episode.
     * Se utiliza para mostrar información como los creadores o el programa al que pertenece.
     */
    private val _associatedPrograms = MutableStateFlow<List<Programa>>(emptyList())
    val associatedPrograms: StateFlow<List<Programa>> = _associatedPrograms.asStateFlow()

    /** Indica si los detalles del Episode se están cargando actualmente. */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Almacena cualquier mensaje de error que ocurra durante la carga de datos. */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Iniciar la carga de datos solo si se ha proporcionado un ID de Episode válido.
        if (episodeId.isNotEmpty()) {
            loadEpisodeDetails()
        } else {
            _error.value = "ID de Episode no válido."
        }
    }

    /**
     * Carga los detalles completos del Episode utilizando el [episodeId] actual.
     * Obtiene el Episode del [repository] y extrae los programas asociados
     * de los datos embebidos (`_embedded.wp:term`) del Episode.
     * Actualiza los StateFlows correspondientes ([_episode], [_associatedPrograms], [_isLoading], [_error]).
     */
    fun loadEpisodeDetails() {
        if (episodeId.isEmpty()) {
            _error.value = "ID de Episode no válido. No se pueden cargar los detalles."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null // Limpiar errores previos al iniciar la carga
            repository.getEpisodeDetail(episodeId).fold(
                { failure ->
                    _error.value = when (failure) {
                        is Failure.NetworkConnection -> "Sin conexión a internet."
                        is Failure.ServerError -> "Error del servidor."
                        is Failure.CustomError -> failure.message
                        else -> "Ocurrió un error desconocido."
                    }
                },
                { fetchedEpisode ->
                    _episode.value = fetchedEpisode

                    fetchedEpisode.embedded?.terms?.let { terminosAnidados ->
                        val programasEncontrados = terminosAnidados.flatten()
                            .distinctBy { programa -> programa.id }

                        _associatedPrograms.value = fetchedEpisode.programaIds?.let { idsDelEpisode ->
                            programasEncontrados.filter { programa -> idsDelEpisode.contains(programa.id) }
                        } ?: programasEncontrados
                    } ?: run {
                        _associatedPrograms.value = emptyList()
                    }
                }
            )
            _isLoading.value = false
        }
    }
}
