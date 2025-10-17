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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.Result

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
class DownloadedEpisodeViewModel(
    private val appPreferences: AppPreferences,
    private val repository: Repository,
    private val applicationContext: Context
) : ViewModel() {

    /** Contiene la lista de objetos [Episode] que han sido descargados. La UI observa este flujo. */
    private val _downloadedEpisodes = MutableStateFlow<List<Episode>>(emptyList())
    val downloadedEpisodes: StateFlow<List<Episode>> = _downloadedEpisodes.asStateFlow()

    /** Un conjunto de IDs para rastrear las descargas que están actualmente en progreso y evitar duplicados. */
    private val downloadsInProgress = mutableSetOf<String>()

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
            onResult(Result.failure(IllegalStateException("El Episode ya está descargado.")))
            return
        }
        if (downloadsInProgress.contains(episode.id)) {
            // La descarga ya está en curso, no se hace nada para no duplicar notificaciones.
            return
        }
        if (_downloadedEpisodes.value.size >= MAX_DOWNLOADS) {
            onResult(Result.failure(IOException("Máximo de $MAX_DOWNLOADS Episodes descargados alcanzado.")))
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val file = File(applicationContext.filesDir, createFileName(episode))
            try {
                downloadsInProgress.add(episode.id)

                val sourceUrl = episode.downloadUrl?.takeIf { it.isNotBlank() } ?: episode.audioUrl
                val url = URL(sourceUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("Error del servidor: ${connection.responseCode}")
                }

                FileOutputStream(file).use { output -> connection.inputStream.use { it.copyTo(output) } }

                val listaActualizada = _downloadedEpisodes.value + episode
                appPreferences.saveDownloadedEpisodes(listaActualizada)

                withContext(Dispatchers.Main) {
                    _downloadedEpisodes.value = listaActualizada
                    onResult(Result.success(Unit)) // Notifica éxito
                }
            } catch (e: Exception) {
                file.delete()
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(e)) // Notifica fallo
                }
            } finally {
                downloadsInProgress.remove(episode.id)
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
