package com.example.paradigmaapp.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.model.Episode
import com.example.paradigmaapp.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.LinkedList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.Result
import kotlin.io.DEFAULT_BUFFER_SIZE

/**
 * ViewModel responsable de gestionar la lógica y el estado de los Episodes descargados.
 * Es autosuficiente para el modo offline, ya que lee y escribe la lista completa de
 * objetos [Episode] descargados desde y hacia [AppPreferences].
 *
 * @property appPreferences Instancia de [AppPreferences] para acceder a las preferencias guardadas.
 * @property repository Repositorio para obtener detalles de Episodes y gestionar descargas.
 * @property applicationContext El [Context] de la aplicación, necesario para operaciones de sistema de archivos.
 *
 * @author Mario Alguacil Juárez
 */
data class DownloadStatus(
    val episodeId: String,
    val title: String,
    val progress: Float,
    val isComplete: Boolean
)

class DownloadedEpisodeViewModel(
    private val appPreferences: AppPreferences,
    private val repository: Repository,
    private val applicationContext: Context
) : ViewModel() {

    /** Contiene la lista de objetos [Episode] que han sido descargados. La UI observa este flujo. */
    private val _downloadedEpisodes = MutableStateFlow<List<Episode>>(emptyList())
    val downloadedEpisodes: StateFlow<List<Episode>> = _downloadedEpisodes.asStateFlow()


    /** Un conjunto de IDs para rastrear las descargas que están actualmente en progreso y evitar duplicados. */
    private val downloadsInProgress = mutableMapOf<String, String>()

    /** Cola de episodios pendientes de descarga. */
    private val downloadQueue = LinkedList<Episode>()

    /** Estado observable de la cola de descargas. */
    private val _queuedEpisodes = MutableStateFlow<List<Episode>>(emptyList())
    val queuedEpisodes: StateFlow<List<Episode>> = _queuedEpisodes.asStateFlow()

    /** Estado de la descarga en curso (si existe). */
    private val _currentDownloadStatus = MutableStateFlow<DownloadStatus?>(null)
    val currentDownloadStatus: StateFlow<DownloadStatus?> = _currentDownloadStatus.asStateFlow()

    /** Límite máximo de Episodes que se pueden descargar. */
    private val MAX_DOWNLOADS = 10

    init {
        loadDownloadedState()
    }

    /**
     * Carga la lista de Episodes descargados desde las SharedPreferences al iniciar el ViewModel.
     */
    private fun loadDownloadedState() {
        viewModelScope.launch(Dispatchers.IO) {
            val EpisodesGuardados = appPreferences.loadDownloadedEpisodes()
            withContext(Dispatchers.Main) {
                _downloadedEpisodes.value = EpisodesGuardados
            }
        }
    }

    /**
     * Inicia la descarga de un Episode, evitando duplicados y descargas concurrentes.
     *
     * @param episode El [Episode] a descargar.
     * @param onResult Callback que se invoca con el resultado de la operación (éxito o fallo).
     */
    fun downloadEpisode(episode: Episode, onResult: (Result<Unit>) -> Unit) {
        if (isEpisodeDownloaded(episode.id)) {
            onResult(Result.failure(IllegalStateException("El episodio ya está descargado.")))
            return
        }
        if (downloadsInProgress.containsKey(episode.id) || downloadQueue.any { it.id == episode.id }) {
            // Ya está en curso o en cola
            return
        }
        if (_downloadedEpisodes.value.size + downloadQueue.size >= MAX_DOWNLOADS) {
            onResult(Result.failure(IOException("Máximo de $MAX_DOWNLOADS episodios descargados alcanzado.")))
            return
        }

        // Si hay una descarga activa, encola
        if (downloadsInProgress.isNotEmpty()) {
            downloadQueue.add(episode)
            _queuedEpisodes.value = downloadQueue.toList()
            return
        }
        // Si no, inicia la descarga
        startDownload(episode, onResult)
    }

    private fun startDownload(episode: Episode, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(applicationContext.filesDir, createFileName(episode))
            var downloadSucceeded = false
            try {
                downloadsInProgress[episode.id] = episode.title
                _currentDownloadStatus.value = DownloadStatus(
                    episodeId = episode.id,
                    title = episode.title,
                    progress = 0f,
                    isComplete = false
                )

                val sourceUrl = episode.downloadUrl?.takeIf { it.isNotBlank() } ?: episode.audioUrl
                val url = URL(sourceUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("Error del servidor: ${connection.responseCode}")
                }

                val contentLength = connection.contentLengthLong.takeIf { it > 0 } ?: -1L
                var bytesCopied = 0L

                connection.inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val bytesRead = input.read(buffer)
                            if (bytesRead == -1) break
                            output.write(buffer, 0, bytesRead)
                            if (contentLength > 0) {
                                bytesCopied += bytesRead
                                val fraction = (bytesCopied.toDouble() / contentLength.toDouble()).toFloat().coerceIn(0f, 1f)
                                _currentDownloadStatus.update { status ->
                                    if (status?.episodeId == episode.id) status.copy(progress = fraction) else status
                                }
                            }
                        }
                        output.flush()
                    }
                }

                // Agrega el episodio a la lista de descargas ANTES de terminar la descarga
                val listaActualizada = if (_downloadedEpisodes.value.any { it.id == episode.id }) {
                    _downloadedEpisodes.value
                } else {
                    _downloadedEpisodes.value + episode
                }
                appPreferences.saveDownloadedEpisodes(listaActualizada)
                withContext(Dispatchers.Main) {
                    _downloadedEpisodes.value = listaActualizada
                }

                _currentDownloadStatus.update { status ->
                    if (status?.episodeId == episode.id) status.copy(progress = 1f, isComplete = true) else status
                }

                withContext(Dispatchers.Main) {
                    onResult(Result.success(Unit)) // Notifica éxito
                }
                downloadSucceeded = true
            } catch (e: Exception) {
                file.delete()
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(e)) // Notifica fallo
                }
            } finally {
                downloadsInProgress.remove(episode.id)
                if (!downloadSucceeded) {
                    _currentDownloadStatus.update { status ->
                        if (status?.episodeId == episode.id) null else status
                    }
                } else {
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(1200)
                        _currentDownloadStatus.update { status ->
                            if (status?.episodeId == episode.id && status.isComplete) null else status
                        }
                    }
                }
                // Al terminar, inicia la siguiente descarga en cola si existe
                if (downloadQueue.isNotEmpty()) {
                    val next = downloadQueue.poll()
                    _queuedEpisodes.value = downloadQueue.toList()
                    if (next != null) startDownload(next) { }
                } else {
                    _queuedEpisodes.value = emptyList()
                }
            }
        }
    }

    /**
     * Elimina un Episode descargado, borrando su archivo físico del almacenamiento
     * y actualizando la lista de descargas en las preferencias.
     *
     * @param episode El [Episode] a eliminar.
     */
    fun deleteDownloadedEpisode(episode: Episode) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(applicationContext.filesDir, createFileName(episode))
            if (file.exists()) {
                file.delete()
            }

            val listaActualizada = _downloadedEpisodes.value.filterNot { it.id == episode.id }
            appPreferences.saveDownloadedEpisodes(listaActualizada)
            withContext(Dispatchers.Main) {
                _downloadedEpisodes.value = listaActualizada
            }
        }
    }

    /** Elimina todas las descargas almacenadas y limpia el estado asociado. */
    fun clearAllDownloads(onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val episodesToRemove = _downloadedEpisodes.value
                episodesToRemove.forEach { episode ->
                    val file = File(applicationContext.filesDir, createFileName(episode))
                    if (file.exists()) {
                        file.delete()
                    }
                }
                appPreferences.clearDownloadedEpisodes()
                downloadsInProgress.clear()
                _currentDownloadStatus.value = null
                withContext(Dispatchers.Main) {
                    _downloadedEpisodes.value = emptyList()
                    onResult(Result.success(Unit))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(e))
                }
            }
        }
    }

    /**
     * Verifica si un Episode está actualmente en la lista de descargados.
     *
     * @param episodeId El ID del Episode a verificar.
     * @return `true` si el Episode está descargado, `false` en caso contrario.
     */
    fun isEpisodeDownloaded(episodeId: String): Boolean {
        return _downloadedEpisodes.value.any { it.id == episodeId }
    }

    /**
     * Obtiene la ruta del archivo local para un Episode descargado.
     *
     * @param episodeId El ID del Episode a buscar.
     * @return La ruta absoluta al archivo como [String], o `null` si el Episode no está descargado o el archivo no existe.
     */
    fun getDownloadedFilePathByEpisodeId(episodeId: String): String? {
        val Episode = _downloadedEpisodes.value.find { it.id == episodeId }
        return Episode?.let {
            val file = File(applicationContext.filesDir, createFileName(it))
            if (file.exists()) file.absolutePath else null
        }
    }

    /**
     * Crea un nombre de archivo único y seguro para un Episode.
     * Reemplaza caracteres no válidos del slug para evitar errores en el sistema de archivos.
     *
     * @param episode El [Episode] para el cual se creará el nombre de archivo.
     * @return Un [String] con el nombre del archivo (ej: "123_nombre-del-slug.mp3").
     */
    private fun createFileName(episode: Episode): String {
        val sanitizedId = episode.id.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        return "${sanitizedId}.mp3"
    }
}
