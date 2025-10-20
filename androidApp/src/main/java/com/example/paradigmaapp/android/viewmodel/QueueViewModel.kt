package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.exception.Either
import com.example.paradigmaapp.model.Episode
import com.example.paradigmaapp.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel para gestionar la cola de reproducción de Episodes.
 * Responsable de:
 * - Cargar y guardar el estado de la cola (lista de IDs de Episodes) usando [AppPreferences].
 * - Obtener los detalles completos de los [Episode]s en la cola para la UI, utilizando
 * una caché local (`allAvailableEpisodesCache`) o el [Repository].
 * - Permitir añadir, eliminar y reordenar (implícitamente al eliminar el actual) Episodes en la cola.
 *
 * @property appPreferences Instancia de [AppPreferences] para la persistencia de la cola.
 * @property repository Repositorio para obtener detalles de Episodes.
 *
 * @author Mario Alguacil Juárez
 */
class QueueViewModel(
    private val appPreferences: AppPreferences,
    private val repository: Repository
) : ViewModel() {

    // StateFlow para los IDs de los Episodes en la cola. Esta es la fuente primaria de verdad para el orden.
    private val _queueEpisodeIds = MutableStateFlow<List<String>>(emptyList())
    val queueEpisodeIds: StateFlow<List<String>> = _queueEpisodeIds.asStateFlow()

    // StateFlow para los objetos Episode completos de la cola, derivados de _queueEpisodeIds.
    private val _queueEpisodes = MutableStateFlow<List<Episode>>(emptyList())
    val queueEpisodes: StateFlow<List<Episode>> = _queueEpisodes.asStateFlow()

    // Caché de todos los Episodes disponibles, proporcionada por MainViewModel.
    private var allAvailableEpisodesCache: List<Episode> = emptyList()

    init {
        loadQueueState() // Carga el estado de la cola al iniciar.
    }

    /**
     * Establece la lista de todos los Episodes disponibles en la aplicación.
     * Esta caché se utiliza para construir la lista de [_queueEpisodes]
     * de forma más eficiente.
     *
     * @param episodes Lista de todos los [Episode]s disponibles.
     */
    fun setAllAvailableEpisodes(episodes: List<Episode>) {
        allAvailableEpisodesCache = episodes
        viewModelScope.launch(Dispatchers.IO) {
            episodes.forEach { repository.saveEpisode(it) }
            updateQueueEpisodesListFromIds()
        }
    }

    /**
     * Carga la lista de IDs de la cola desde [AppPreferences] y luego
     * actualiza la lista de objetos [Episode] completos.
     */
    private fun loadQueueState() {
        viewModelScope.launch(Dispatchers.IO) {
            _queueEpisodeIds.value = appPreferences.loadEpisodeQueue()
            updateQueueEpisodesListFromIds()
        }
    }

    /** Guarda la lista actual de IDs de la cola en [AppPreferences]. */
    private fun saveQueueState() {
        viewModelScope.launch(Dispatchers.IO) {
            appPreferences.saveEpisodeQueue(_queueEpisodeIds.value)
        }
    }

    /**
     * Actualiza `_queueEpisodes` (lista de objetos [Episode]) basada en `_queueEpisodeIds`.
     * Intenta obtener los detalles del Episode desde `allAvailableEpisodesCache` primero;
     * si no se encuentra, lo busca a través de [repository].
     * El orden de `_queueEpisodes` reflejará el orden de `_queueEpisodeIds`.
     */
    private suspend fun updateQueueEpisodesListFromIds() {
        val episodeDetailsList = mutableListOf<Episode>()
        _queueEpisodeIds.value.forEach { id ->
            val cachedEpisode = allAvailableEpisodesCache.find { it.id == id }
            if (cachedEpisode != null) {
                episodeDetailsList.add(cachedEpisode)
                return@forEach
            }

            val storedEpisode = repository.getEpisodeFromCache(id)
            if (storedEpisode != null) {
                episodeDetailsList.add(storedEpisode)
                return@forEach
            }

            when (val episodeResult = repository.getEpisodeDetail(id)) {
                is Either.Right -> {
                    val fetchedEpisode = episodeResult.b
                    episodeDetailsList.add(fetchedEpisode)
                    repository.saveEpisode(fetchedEpisode)
                }
                else -> {
                    // No-op on failure; queue episode list skips this entry until it can be fetched.
                }
            }
        }
        // Asegurar que los objetos Episode en _queueEpisodes sigan el orden de _queueEpisodeIds.
        // Lo hacemos reconstruyendo la lista en el orden de los IDs.
        withContext(Dispatchers.Main) { // Actualizar el StateFlow en el hilo principal
            _queueEpisodes.value = episodeDetailsList
        }
    }

    /**
     * Añade un Episode al final de la cola de reproducción si aún no está presente.
     *
     * @param episode El [Episode] a añadir.
     */
    fun addEpisodeToQueue(episode: Episode) {
        viewModelScope.launch(Dispatchers.IO) { // Operaciones de lista y guardado en IO
            if (!_queueEpisodeIds.value.contains(episode.id)) {
                val newIds = _queueEpisodeIds.value.toMutableList().apply { add(episode.id) }
                _queueEpisodeIds.value = newIds
                saveQueueState() // Guardar la nueva lista de IDs.

                // Actualizar la lista de objetos Episode
                val newEpisodes = _queueEpisodes.value.toMutableList()
                if (newEpisodes.none { it.id == episode.id }) { // Doble check por si acaso
                    newEpisodes.add(episode)
                }
                repository.saveEpisode(episode)
                withContext(Dispatchers.Main) {
                    _queueEpisodes.value = newEpisodes
                }
            }
        }
    }

    /**
     * Elimina un Episode de la cola de reproducción.
     *
     * @param episode El [Episode] a eliminar.
     */
    fun removeEpisodeFromQueue(episode: Episode) {
        viewModelScope.launch(Dispatchers.IO) { // Operaciones de lista y guardado en IO
            if (_queueEpisodeIds.value.contains(episode.id)) {
                _queueEpisodeIds.value = _queueEpisodeIds.value.toMutableList().apply { remove(episode.id) }
                saveQueueState() // Guardar la nueva lista de IDs.

                // Actualizar la lista de objetos Episode.
                withContext(Dispatchers.Main) {
                    _queueEpisodes.value = _queueEpisodes.value.filterNot { it.id == episode.id }
                }
            }
        }
    }

    /**
     * Elimina el Episode que se acaba de reproducir de la cola y devuelve el siguiente Episode.
     * Esta función se llama típicamente desde [MainViewModel] cuando un Episode finaliza.
     *
     * @param playedEpisodeId El ID del Episode que acaba de terminar de reproducirse.
     * @return El siguiente [Episode] en la cola para reproducir, o `null` si la cola está vacía
     * o el siguiente Episode no se puede cargar.
     */
    suspend fun dequeueNextEpisode(playedEpisodeId: String): Episode? = withContext(Dispatchers.IO) {
        val currentIds = _queueEpisodeIds.value.toMutableList()
        val playedIndex = currentIds.indexOf(playedEpisodeId)

        if (playedIndex != -1) {
            currentIds.removeAt(playedIndex)
            _queueEpisodeIds.value = currentIds
            saveQueueState() // Guardar el estado de IDs actualizado.

            // Reconstruir la lista de objetos Episode en el orden correcto.
            updateQueueEpisodesListFromIds() // Esto actualiza _queueEpisodes en el hilo principal.

            // Devolver el primer elemento de la lista de objetos recién actualizada.
            return@withContext _queueEpisodes.value.firstOrNull()
        }
        // Si el playedEpisodeId no estaba en la cola (caso raro),
        // o si la cola estaba vacía después de quitarlo.
        return@withContext _queueEpisodes.value.firstOrNull()
    }


    /** Limpia completamente la cola de reproducción. */
    fun clearQueue() {
        viewModelScope.launch(Dispatchers.IO) {
            _queueEpisodeIds.value = emptyList()
            _queueEpisodes.value = emptyList() // Limpiar también la lista de objetos.
            saveQueueState()
        }
    }
}
