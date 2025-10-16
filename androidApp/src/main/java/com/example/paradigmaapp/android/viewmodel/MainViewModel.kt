package com.example.paradigmaapp.android.viewmodel

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.paradigmaapp.android.audio.MediaAttribution
import com.example.paradigmaapp.android.api.AndainaStream
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.exception.Failure
import com.example.paradigmaapp.model.Episode
import com.example.paradigmaapp.model.Programa
import com.example.paradigmaapp.model.RadioInfo
import com.example.paradigmaapp.repository.Repository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/*
 * Enumeración de tipos de notificaciones.
 *
 * SUCCESS: Notificación de éxito.
 * FAILURE: Notificación de error.
 */
enum class NotificationType {
    SUCCESS,
    FAILURE
}

/**
 * ViewModel principal de la aplicación.
 * Gestiona el estado global de la UI, la carga inicial de datos, la lógica de los reproductores
 * de audio para podcasts y streaming, y coordina otros ViewModels.
 *
 * @property repository Repositorio principal para obtener datos.
 * @property appPreferences Gestor de preferencias para persistir estados.
 * @property context Contexto de la aplicación, necesario para ExoPlayer.
 * @property queueViewModel ViewModel para la cola de reproducción.
 * @property onGoingViewModel ViewModel para los Episodes en curso.
 * @property downloadedViewModel ViewModel para los Episodes descargados.
 *
 * @author Mario Alguacil Juárez
 */
class MainViewModel(
    private val repository: Repository,
    private val appPreferences: AppPreferences,
    private val context: Context,
    val queueViewModel: QueueViewModel,
    val onGoingViewModel: OnGoingEpisodeViewModel,
    val downloadedViewModel: DownloadedEpisodeViewModel,
    val andainaStreamPlayer: AndainaStream,
    val volumeControlViewModel: VolumeControlViewModel
) : ViewModel() {
    private companion object {
        private const val CONTEXT_EPISODE_LIMIT = 20
    }

    private val _programas = MutableStateFlow<List<Programa>>(emptyList())
    val programas: StateFlow<List<Programa>> = _programas.asStateFlow()

    private val _isLoadingProgramas = MutableStateFlow(false)
    val isLoadingProgramas: StateFlow<Boolean> = _isLoadingProgramas.asStateFlow()

    private val _programasError = MutableStateFlow<String?>(null)
    val programasError: StateFlow<String?> = _programasError.asStateFlow()

    private val _initialEpisodes = MutableStateFlow<List<Episode>>(emptyList())
    val initialEpisodes: StateFlow<List<Episode>> = _initialEpisodes.asStateFlow()

    private val _isLoadingInitial = MutableStateFlow(true)
    val isLoadingInitial: StateFlow<Boolean> = _isLoadingInitial.asStateFlow()

    private val _initialDataError = MutableStateFlow<String?>(null)
    val initialDataError: StateFlow<String?> = _initialDataError.asStateFlow()

    private val _currentPlayingEpisode = MutableStateFlow<Episode?>(null)
    val currentPlayingEpisode: StateFlow<Episode?> = _currentPlayingEpisode.asStateFlow()

    private val _isPodcastPlaying = MutableStateFlow(false)
    val isPodcastPlaying: StateFlow<Boolean> = _isPodcastPlaying.asStateFlow()

    private val _podcastProgress = MutableStateFlow(0f)
    val podcastProgress: StateFlow<Float> = _podcastProgress.asStateFlow()

    private val _podcastDuration = MutableStateFlow(0L)
    val podcastDuration: StateFlow<Long> = _podcastDuration.asStateFlow()

    private val _isAndainaStreamActive = MutableStateFlow(appPreferences.loadIsStreamActive() ?: false)
    val isAndainaStreamActive: StateFlow<Boolean> = _isAndainaStreamActive.asStateFlow()

    private val _isAndainaPlaying = MutableStateFlow(false)
    val isAndainaPlaying: StateFlow<Boolean> = _isAndainaPlaying.asStateFlow()

    private val _hasStreamLoadFailed = MutableStateFlow(false)
    val hasStreamLoadFailed: StateFlow<Boolean> = _hasStreamLoadFailed.asStateFlow()

    private val _preparingEpisodeId = MutableStateFlow<String?>(null)
    val preparingEpisodeId: StateFlow<String?> = _preparingEpisodeId.asStateFlow()

    private val _isFullScreenPlayerVisible = MutableStateFlow(false)
    val isFullScreenPlayerVisible: StateFlow<Boolean> = _isFullScreenPlayerVisible.asStateFlow()

    private val _hasNextEpisode = MutableStateFlow(false)
    val hasNextEpisode: StateFlow<Boolean> = _hasNextEpisode.asStateFlow()

    private val _hasPreviousEpisode = MutableStateFlow(false)
    val hasPreviousEpisode: StateFlow<Boolean> = _hasPreviousEpisode.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(appPreferences.loadOnboardingComplete())
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _andainaRadioInfo = MutableStateFlow<RadioInfo?>(null)
    val andainaRadioInfo: StateFlow<RadioInfo?> = _andainaRadioInfo.asStateFlow()

    private val _contextualPlaylist = MutableStateFlow<List<Episode>>(emptyList())

    private val playbackContext =
        ContextCompat.createAttributionContext(context, MediaAttribution.AUDIO_PLAYBACK_TAG)
    val podcastExoPlayer: ExoPlayer = ExoPlayer.Builder(playbackContext).build()

    private var progressUpdateJob: Job? = null
    private var radioInfoUpdateJob: Job? = null
    private val podcastPlayerListener: Player.Listener
    private val andainaPlayerListener: Player.Listener

    private val _topNotification = MutableStateFlow<Pair<String, NotificationType>?>(null)
    val topNotification: StateFlow<Pair<String, NotificationType>?> = _topNotification.asStateFlow()

    /**
     * Muestra una notificación en la parte superior de la pantalla durante unos segundos.
     *
     * @param message El texto que se mostrará en la notificación.
     * @param type El tipo de notificación (SUCCESS o FAILURE) para determinar el estilo.
     */
    fun showTopNotification(message: String, type: NotificationType) {
        viewModelScope.launch {
            _topNotification.value = message to type
            delay(3000)
            _topNotification.value = null
        }
    }

    init {
        // Lógica de inicio de la aplicación para la radio
        if (appPreferences.loadOnboardingComplete() && (appPreferences.loadIsStreamActive() == true)) {
            andainaStreamPlayer.play()
        }

        // Ya no se usa _currentVolume en este ViewModel
        // _currentVolume.value = podcastExoPlayer.volume

        podcastPlayerListener = createPodcastPlayerListener()
        andainaPlayerListener = createAndainaPlayerListener()
        podcastExoPlayer.addListener(podcastPlayerListener)
        andainaStreamPlayer.addListener(andainaPlayerListener)

        // Aplica el volumen guardado al iniciar los reproductores
        volumeControlViewModel.applySavedVolume(podcastExoPlayer, andainaStreamPlayer)

        loadInitialProgramas()
        loadInitialData()
        startProgressUpdates()
        startRadioInfoUpdates()
        observeCurrentPlayingEpisode()
        observeAndainaStreamActive()

        viewModelScope.launch {
            _currentPlayingEpisode.collect {
                updateNextPreviousState()
            }
        }
        viewModelScope.launch {
            queueViewModel.queueEpisodes.collect {
                updateNextPreviousState()
            }
        }
    }

    /** Carga la lista inicial de programas desde el repositorio. */
    fun loadInitialProgramas() {
        viewModelScope.launch {
            _isLoadingProgramas.value = true
            _programasError.value = null
            repository.getProgramas().fold(
                { failure ->
                    _programasError.value = when (failure) {
                        is Failure.NetworkConnection -> "Sin conexión a internet."
                        is Failure.ServerError -> "Error del servidor."
                        is Failure.CustomError -> failure.message
                        else -> "Ocurrió un error desconocido."
                    }
                },
                { programas ->
                    _programas.value = programas
                }
            )
            _isLoadingProgramas.value = false
        }
    }

    /** Carga los datos iniciales y restaura el estado de reproducción. */
    fun loadInitialData() {
        viewModelScope.launch {
            _isLoadingInitial.value = true
            _initialDataError.value = null
            repository.getSavedEpisodes().fold(
                { failure ->
                    _initialDataError.value = when (failure) {
                        is Failure.NetworkConnection -> "Sin conexión a internet."
                        is Failure.ServerError -> "Error del servidor."
                        is Failure.CustomError -> failure.message
                        else -> "No se pudieron cargar los últimos Episodes."
                    }
                },
                { episodes ->
                    _initialEpisodes.value = episodes
                    queueViewModel.setAllAvailableEpisodes(episodes)

                    val savedEpisodeId = appPreferences.loadCurrentEpisodeId()
                    savedEpisodeId?.let { id ->
                        var episodeToRestore = appPreferences.loadEpisodeDetails(id)
                        if (episodeToRestore == null) {
                            repository.getEpisodeDetail(id).fold(
                                { /* Ignore failure for now */ },
                                { episode ->
                                    episodeToRestore = episode
                                    episodeToRestore?.let { appPreferences.saveEpisodeDetails(it) }
                                }
                            )
                        }
                        episodeToRestore?.let { episode ->
                            val savedPosition = appPreferences.getEpisodePosition(episode.id)
                            _currentPlayingEpisode.value = episode
                            prepareEpisodePlayer(episode, savedPosition, playWhenReady = false)
                        }
                    }
                }
            )
            _isLoadingInitial.value = false
        }
    }

    /**
     * Marca el onboarding como completado, guarda el estado y notifica a la UI.
     */
    fun setOnboardingComplete() {
        appPreferences.saveOnboardingComplete(true)
        _onboardingCompleted.value = true
    }
    /**
     * Selecciona un Episode para reproducción. Si ya es el Episode actual, alterna play/pause.
     * @param episode El [Episode] a reproducir.
     * @param playWhenReady Indica si la reproducción debe comenzar inmediatamente.
     */
    fun selectEpisode(episode: Episode, playWhenReady: Boolean = true) {
        if (_currentPlayingEpisode.value?.id == episode.id) {
            if (podcastExoPlayer.isPlaying) podcastExoPlayer.pause() else podcastExoPlayer.play()
            return
        }

        _preparingEpisodeId.value = episode.id

        _currentPlayingEpisode.value = episode
        if (andainaStreamPlayer.isPlaying()) andainaStreamPlayer.stop()
        val savedPosition = appPreferences.getEpisodePosition(episode.id)
        prepareEpisodePlayer(episode, savedPosition, playWhenReady)
    }

    /**
     * Alterna la visibilidad del reproductor a pantalla completa.
     */
    fun toggleFullScreenPlayer() {
        _isFullScreenPlayerVisible.value = !_isFullScreenPlayerVisible.value
    }

    /**
     * Reproduce el siguiente Episode según la lógica de prioridad (cola > lista general).
     */
    fun playNextEpisode() {
        val episode = _currentPlayingEpisode.value ?: return

        val queue = queueViewModel.queueEpisodes.value
        val indexInQueue = queue.indexOfFirst { it.id == episode.id }
        if (indexInQueue != -1 && indexInQueue < queue.size - 1) {
            selectEpisode(queue[indexInQueue + 1])
            return
        }

        val programContextList = _contextualPlaylist.value
        val indexInContextList = programContextList.indexOfFirst { it.id == episode.id }
        if (indexInContextList != -1 && indexInContextList < programContextList.size - 1) {
            selectEpisode(programContextList[indexInContextList + 1])
        }
    }

    /**
     * Reproduce el Episode anterior según la lógica de prioridad (cola > lista general).
     */
    fun playPreviousEpisode() {
        val episode = _currentPlayingEpisode.value ?: return

        val queue = queueViewModel.queueEpisodes.value
        val indexInQueue = queue.indexOfFirst { it.id == episode.id }
        if (indexInQueue != -1 && indexInQueue > 0) {
            selectEpisode(queue[indexInQueue - 1])
            return
        }

        val programContextList = _contextualPlaylist.value
        val indexInContextList = programContextList.indexOfFirst { it.id == episode.id }
        if (indexInContextList != -1 && indexInContextList > 0) {
            selectEpisode(programContextList[indexInContextList - 1])
        }
    }

    /** Prepara el [podcastExoPlayer] para un Episode específico. */
    private fun prepareEpisodePlayer(episode: Episode, positionMs: Long, playWhenReady: Boolean) {
        val mediaPath =
            downloadedViewModel.getDownloadedFilePathByEpisodeId(episode.id)
                ?: episode.audioUrl.takeIf { it.isNotBlank() }
                ?: episode.downloadUrl
                ?: ""
        if (mediaPath.isBlank()) {
            _initialDataError.value = "No se encontró una fuente de audio para '${episode.title}'."
            _preparingEpisodeId.value = null
            return
        }

        try {
            podcastExoPlayer.stop()
            podcastExoPlayer.clearMediaItems()
            podcastExoPlayer.setMediaItem(MediaItem.fromUri(mediaPath), positionMs)
            podcastExoPlayer.prepare()
            podcastExoPlayer.playWhenReady = playWhenReady
        } catch (e: Exception) {
            _initialDataError.value = "Error al preparar la reproducción de '${episode.title}'."
        } finally {
            _preparingEpisodeId.value = null
        }
    }

    /**
     * Comprueba y actualiza el estado de los botones de siguiente y anterior
     * basándose en la posición del Episode actual en la cola o en la lista general.
     */
    private fun updateNextPreviousState() {
        val episode = _currentPlayingEpisode.value ?: run {
            _hasNextEpisode.value = false
            _hasPreviousEpisode.value = false
            return
        }

        // Prioridad 1: Comprobar la cola de reproducción
        val queue = queueViewModel.queueEpisodes.value
        val indexInQueue = queue.indexOfFirst { it.id == episode.id }

        if (indexInQueue != -1) {
            _hasPreviousEpisode.value = indexInQueue > 0
            _hasNextEpisode.value = indexInQueue < queue.size - 1
            return
        }

        // ---> CAMBIO: Prioridad 2: Usar la lista de contexto del programa
        val programContextList = _contextualPlaylist.value
        val indexInContextList = programContextList.indexOfFirst { it.id == episode.id }

        if (indexInContextList != -1) {
            _hasPreviousEpisode.value = indexInContextList > 0
            _hasNextEpisode.value = indexInContextList < programContextList.size - 1
            return
        }

        _hasNextEpisode.value = false
        _hasPreviousEpisode.value = false
    }

    /** Gestiona el clic en el botón principal de play/pause del reproductor global. */
    fun onPlayerPlayPauseClick() {
        if (_currentPlayingEpisode.value != null) {
            if (podcastExoPlayer.isPlaying) podcastExoPlayer.pause() else podcastExoPlayer.play()
        } else {
            // Lógica para reproducir el stream si no hay un podcast activo
            if (_isAndainaStreamActive.value && !_hasStreamLoadFailed.value) {
                if (andainaStreamPlayer.isPlaying()) andainaStreamPlayer.pause() else andainaStreamPlayer.play()
            } else if (_hasStreamLoadFailed.value) {
                _hasStreamLoadFailed.value = false
                andainaStreamPlayer.play()
            }
        }
    }

    /** * NUEVA FUNCIÓN: Controla la reproducción de la radio y actualiza el estado de las preferencias.
     */
    fun toggleAndainaStreamPlayer() {
        // Detiene el podcast si se está reproduciendo
        if (podcastExoPlayer.isPlaying) {
            podcastExoPlayer.stop()
            _currentPlayingEpisode.value = null
        }

        val newActiveState = !_isAndainaStreamActive.value
        _isAndainaStreamActive.value = newActiveState // Sincroniza el StateFlow
        appPreferences.saveIsStreamActive(newActiveState) // Guarda en preferencias

        if (newActiveState) {
            andainaStreamPlayer.play()
        } else {
            andainaStreamPlayer.stop()
        }
    }

    /**
     * Mueve la posición de reproducción del Episode actual.
     * @param progressFraction La nueva posición como una fracción (0.0 a 1.0) de la duración total.
     */
    fun seekEpisodeTo(progressFraction: Float) {
        val currentDuration = podcastExoPlayer.duration
        if (_currentPlayingEpisode.value != null && currentDuration > 0 && currentDuration != C.TIME_UNSET) {
            podcastExoPlayer.seekTo((progressFraction * currentDuration).toLong())
        }
    }

    /**
     * Observa cambios en el Episode actual para gestionar el estado y cargar
     * la lista de contexto del programa si es necesario.
     */
    private fun observeCurrentPlayingEpisode() {
        viewModelScope.launch {
            _currentPlayingEpisode.collect { episode ->
                // Guardar el ID del Episode actual para restaurar estado
                appPreferences.saveCurrentEpisodeId(episode?.id)

                if (episode != null) {
                    // Si el Episode es nuevo, carga su contexto de programa
                    val isNotInQueue = queueViewModel.queueEpisodes.value.none { it.id == episode.id }
                    val isNotInCurrentContext = _contextualPlaylist.value.none { it.id == episode.id }

                    if (isNotInQueue && isNotInCurrentContext) {
                        // El Episode es nuevo y no viene de la cola ni del contexto actual
                        // así que cargamos su contexto de programa.
                        episode.programId.let { programId ->
                            repository.getEpisodes(programId, 0, CONTEXT_EPISODE_LIMIT).fold(
                                { /* Handle error or use default */ },
                                { episodes ->
                                    _contextualPlaylist.value = episodes
                                }
                            )
                        }
                    }
                    // Se asegura que la radio se detenga cuando un Episode se reproduce
                    if (andainaStreamPlayer.isPlaying()) {
                        andainaStreamPlayer.stop()
                    }
                    // Actualiza el estado de la UI
                    _isAndainaStreamActive.value = false
                } else {
                    // Si no hay Episode, se limpia el contexto
                    _contextualPlaylist.value = emptyList()
                }
                // Siempre actualizamos el estado de los botones al cambiar de Episode
                updateNextPreviousState()
            }
        }
    }

    /** Observa cambios en el estado de activación del stream para controlar el reproductor. */
    private fun observeAndainaStreamActive() {
        // Se ha vaciado esta función. Ahora el control de play/stop se realiza
        // exclusivamente en onPlayerPlayPauseClick()
    }

    /** Crea y devuelve el listener para el reproductor de podcasts (ExoPlayer). */
    private fun createPodcastPlayerListener(): Player.Listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            _initialDataError.value = "Error de reproducción: ${error.message}"
            _preparingEpisodeId.value = null
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                _preparingEpisodeId.value = null
            }
            _isPodcastPlaying.value = isPlaying
            if (!isPlaying && podcastExoPlayer.playbackState != Player.STATE_ENDED && podcastExoPlayer.playbackState != Player.STATE_IDLE) {
                _currentPlayingEpisode.value?.let { episode ->
                    appPreferences.saveEpisodePosition(
                        episode.id,
                        podcastExoPlayer.currentPosition
                    )
                    onGoingViewModel.addOrUpdateOnGoingEpisode(episode)
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                _currentPlayingEpisode.value?.let { episode ->
                    appPreferences.saveEpisodePosition(episode.id, 0L)
                    onGoingViewModel.refrescarListaEpisodesEnCurso()
                    viewModelScope.launch {
                        val nextEpisode = queueViewModel.dequeueNextEpisode(episode.id)
                        if (nextEpisode != null) selectEpisode(
                            nextEpisode,
                            true
                        ) else _currentPlayingEpisode.value = null
                    }
                }
            }
        }
    }

    /** Crea y devuelve el listener para el reproductor del stream de Andaina. */
    private fun createAndainaPlayerListener(): Player.Listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            _hasStreamLoadFailed.value = true
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isAndainaPlaying.value = isPlaying
        }
    }

    /** Inicia un job para actualizar periódicamente el progreso de la UI del reproductor. */
    private fun startProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (isActive) {
                if (podcastExoPlayer.isPlaying) {
                    val duration = podcastExoPlayer.duration.takeIf { it > 0 } ?: 0L
                    val currentPos = podcastExoPlayer.currentPosition
                    _podcastDuration.value = duration
                    _podcastProgress.value =
                        if (duration > 0) currentPos.toFloat() / duration.toFloat() else 0f
                    _currentPlayingEpisode.value?.let { episode ->
                        onGoingViewModel.addOrUpdateOnGoingEpisode(
                            episode
                        )
                    }
                }
                _isAndainaPlaying.value = andainaStreamPlayer.isPlaying()
                delay(250)
            }
        }
    }

    /** Inicia un job que obtiene la información del stream de Andaina FM periódicamente. */
    private fun startRadioInfoUpdates() {
        radioInfoUpdateJob?.cancel()
        radioInfoUpdateJob = viewModelScope.launch {
            while (isActive) {
                if (_isAndainaStreamActive.value || _isAndainaPlaying.value) {
                    try {
                        _andainaRadioInfo.value = andainaStreamPlayer.getRadioInfo()
                    } catch (e: Exception) {
                        _andainaRadioInfo.value = null
                    }
                }
                delay(15_000L) // Actualiza cada 15 segundos
            }
        }
    }

    /**
     * Cancela los jobs y listeners al destruir el ViewModel
     */
    override fun onCleared() {
        super.onCleared()
        _currentPlayingEpisode.value?.let { episode ->
            if (podcastExoPlayer.playbackState != Player.STATE_IDLE) {
                appPreferences.saveEpisodePosition(episode.id, podcastExoPlayer.currentPosition)
                appPreferences.saveEpisodeDetails(episode)
            }
        }
        podcastExoPlayer.removeListener(podcastPlayerListener)
        podcastExoPlayer.release()
        andainaStreamPlayer.removeListener(andainaPlayerListener)
        andainaStreamPlayer.release()
        progressUpdateJob?.cancel()
        radioInfoUpdateJob?.cancel()
    }

    /**
     * Salta hacia adelante en la reproducción del podcast actual.
     *
     * @param millis Milisegundos a adelantar (por defecto 30 segundos).
     */
    fun skipForward(millis: Long = 30000) {
        if (_currentPlayingEpisode.value != null) {
            val newPosition =
                (podcastExoPlayer.currentPosition + millis).coerceAtMost(podcastExoPlayer.duration)
            podcastExoPlayer.seekTo(newPosition)
        }
    }

    /**
     * Salta hacia atrás en la reproducción del podcast actual.
     *
     * @param millis Milisegundos a retroceder (por defecto 10 segundos).
     */
    fun rewind(millis: Long = 10000) {
        if (_currentPlayingEpisode.value != null) {
            val newPosition = (podcastExoPlayer.currentPosition - millis).coerceAtLeast(0)
            podcastExoPlayer.seekTo(newPosition)
        }
    }
}
