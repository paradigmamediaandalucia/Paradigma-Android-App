package com.example.paradigmaapp.android.data

import android.content.Context
import android.content.SharedPreferences
import com.example.paradigmaapp.model.Episode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Gestiona el almacenamiento y la recuperación de las preferencias de la aplicación
 * utilizando [SharedPreferences].
 *
 * @param context El [Context] de la aplicación.
 * @author Mario Alguacil Juárez
 */
class AppPreferences(context: Context) {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "ParadigmaAppPrefsV2"
        private const val PREF_CURRENT_EPISODE_ID = "currentEpisodeId_v2"
        private const val PREF_IS_STREAM_ACTIVE = "isStreamActive_v2"
        private const val PREF_AUTO_PLAY_STREAM_ON_START = "autoPlayStreamOnStart_v1"
        private const val PREF_EPISODE_POSITIONS = "episodePositions_v2"
        private const val PREF_EPISODE_QUEUE_IDS = "episodeQueueIds_v2"
        private const val PREF_DOWNLOADED_EpisodeS = "downloadedEpisodes_v1"
        private const val PREF_EPISODE_DETAILS_MAP = "episodeDetailsMap_v1"
        private const val PREF_ONBOARDING_COMPLETE = "onboardingComplete_v1"
        private const val PREF_MANUALLY_SET_DARK_THEME = "manuallySetDarkTheme_v1"
        private const val PREF_REMEMBER_EPISODE_PROGRESS = "rememberEpisodeProgress_v1"
        private const val PREF_AUTO_PLAY_NEXT_EPISODE = "autoPlayNextEpisode_v1"
        private const val PREF_PROGRAM_DISPLAY_MODE = "programDisplayMode_v1"

        private const val PREF_VOLUME_LEVEL = "volumeLevel_v1"
        private const val DEFAULT_VOLUME_LEVEL = 0.5f
    }

    /**
     * Guarda si el usuario ha completado la pantalla de introducción.
     */
    fun saveOnboardingComplete(isComplete: Boolean) {
        prefs.edit().putBoolean(PREF_ONBOARDING_COMPLETE, isComplete).apply()
    }

    /** Guarda el modo de visualización de programas ("grid" o "list"). */
    fun saveProgramDisplayMode(isList: Boolean) {
        prefs.edit().putString(PREF_PROGRAM_DISPLAY_MODE, if (isList) "list" else "grid").apply()
    }

    /** Carga el modo de visualización de programas. */
    fun loadProgramDisplayMode(): Boolean {
        return prefs.getString(PREF_PROGRAM_DISPLAY_MODE, "grid") == "list"
    }

    /**
     * Comprueba si el usuario ha completado la pantalla de introducción.
     * @return `true` si ya la ha completado, `false` si es la primera vez.
     */
    fun loadOnboardingComplete(): Boolean {
        return prefs.getBoolean(PREF_ONBOARDING_COMPLETE, false)
    }

    /**
     * Guarda la posición de reproducción de un Episode.
     *
     * @param episodeId El ID del Episode.
     * @param positionMillis La posición de reproducción en milisegundos.
     */
    fun saveEpisodePosition(episodeId: String, positionMillis: Long) {
        val positionsJson = prefs.getString(PREF_EPISODE_POSITIONS, "{}") ?: "{}"
        val positionsMap: MutableMap<String, Long> = try {
            jsonParser.decodeFromString(
                MapSerializer(String.serializer(), Long.serializer()),
                positionsJson
            ).toMutableMap()
        } catch (e: SerializationException) {
            mutableMapOf()
        }
        positionsMap[episodeId] = positionMillis
        val newPositionsJson = jsonParser.encodeToString(positionsMap)
        prefs.edit().putString(PREF_EPISODE_POSITIONS, newPositionsJson).apply()
    }

    /**
     * Obtiene la posición de reproducción guardada para un Episode.
     *
     * @param episodeId El ID del Episode.
     * @return La posición en milisegundos, o 0L si no se encuentra.
     */
    fun getEpisodePosition(episodeId: String): Long {
        val positionsMap = getAllEpisodePositions()
        return positionsMap[episodeId] ?: 0L
    }

    /**
     * Obtiene el mapa completo de todas las posiciones de Episodes guardadas.
     *
     * @return Un mapa de ID de Episode (String) a su posición (Long).
     */
    fun getAllEpisodePositions(): Map<String, Long> {
        val positionsJson = prefs.getString(PREF_EPISODE_POSITIONS, "{}") ?: "{}"
        return try {
            jsonParser.decodeFromString(
                MapSerializer(String.serializer(), Long.serializer()),
                positionsJson
            )
        } catch (e: SerializationException) {
            emptyMap()
        }
    }

    /** Elimina todas las posiciones de reproducción guardadas. */
    fun clearEpisodePositions() {
        prefs.edit().remove(PREF_EPISODE_POSITIONS).apply()
    }

    /** Guarda el ID del Episode actualmente activo.
     *
     * @param episodeId El ID del Episode o `null` si no se encuentra.
     */
    fun saveCurrentEpisodeId(episodeId: String?) {
        val editor = prefs.edit()
        if (episodeId == null) {
            editor.remove(PREF_CURRENT_EPISODE_ID)
        } else {
            editor.putString(PREF_CURRENT_EPISODE_ID, episodeId)
        }
        editor.apply()
    }

    /** Carga el ID del Episode actualmente activo.
     *
     * @return El ID del Episode o `null` si no se encuentra.
     */
    fun loadCurrentEpisodeId(): String? {
        return prefs.getString(PREF_CURRENT_EPISODE_ID, null)
    }

    /** Guarda si el streaming está habilitado en la interfaz (botón de la antena). */
    fun saveIsStreamActive(isActive: Boolean) {
        prefs.edit().putBoolean(PREF_IS_STREAM_ACTIVE, isActive).apply()
    }

    /** Indica si el streaming está habilitado en la interfaz (botón de la antena). */
    fun loadIsStreamActive(): Boolean {
        return prefs.getBoolean(PREF_IS_STREAM_ACTIVE, false)
    }

    /** Guarda si la app debe reproducir la radio automáticamente al iniciarse. */
    fun saveAutoPlayStreamOnStart(shouldAutoPlay: Boolean) {
        prefs.edit().putBoolean(PREF_AUTO_PLAY_STREAM_ON_START, shouldAutoPlay).apply()
    }

    /**
     * Carga si la app debe reproducir la radio automáticamente al iniciarse.
     * Si la preferencia aún no existe (usuarios anteriores), se reutiliza el valor histórico
     * de `PREF_IS_STREAM_ACTIVE` para mantener el comportamiento esperado tras la migración.
     */
    fun loadAutoPlayStreamOnStart(): Boolean {
        return if (prefs.contains(PREF_AUTO_PLAY_STREAM_ON_START)) {
            prefs.getBoolean(PREF_AUTO_PLAY_STREAM_ON_START, false)
        } else {
            prefs.getBoolean(PREF_IS_STREAM_ACTIVE, false)
        }
    }

    /** Indica si la preferencia específica de auto-reproducción ya fue establecida explícitamente. */
    fun hasAutoPlayStreamOnStartPreference(): Boolean {
        return prefs.contains(PREF_AUTO_PLAY_STREAM_ON_START)
    }

    /** Guarda si debe recordarse el progreso de los Episodes. */
    fun saveRememberEpisodeProgress(remember: Boolean) {
        prefs.edit().putBoolean(PREF_REMEMBER_EPISODE_PROGRESS, remember).apply()
    }

    /** Indica si se debe recordar el progreso de los Episodes. */
    fun loadRememberEpisodeProgress(): Boolean {
        return prefs.getBoolean(PREF_REMEMBER_EPISODE_PROGRESS, true)
    }

    /** Guarda si se debe reproducir automáticamente el siguiente Episode. */
    fun saveAutoPlayNextEpisode(autoPlay: Boolean) {
        prefs.edit().putBoolean(PREF_AUTO_PLAY_NEXT_EPISODE, autoPlay).apply()
    }

    /** Indica si debe reproducirse automáticamente el siguiente Episode al finalizar el actual. */
    fun loadAutoPlayNextEpisode(): Boolean {
        return prefs.getBoolean(PREF_AUTO_PLAY_NEXT_EPISODE, true)
    }

    /** Guarda la cola de reproducción (lista de IDs).
     *
     * @param queueEpisodeIds Una lista de Strings que representan los IDs de los Episodes en la cola.
     */
    fun saveEpisodeQueue(queueEpisodeIds: List<String>) {
        val jsonString = jsonParser.encodeToString(ListSerializer(String.serializer()), queueEpisodeIds)
        prefs.edit().putString(PREF_EPISODE_QUEUE_IDS, jsonString).apply()
    }

    /** Carga la cola de reproducción (lista de IDs).
     *
     * @return Una lista de Strings que representan los IDs de los Episodes en la cola.
     */
    fun loadEpisodeQueue(): List<String> {
        val jsonString = prefs.getString(PREF_EPISODE_QUEUE_IDS, null)
        return if (!jsonString.isNullOrEmpty()) {
            try {
                jsonParser.decodeFromString(ListSerializer(String.serializer()), jsonString)
            } catch (e: SerializationException) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /** Elimina por completo la cola de reproducción almacenada. */
    fun clearEpisodeQueue() {
        prefs.edit().remove(PREF_EPISODE_QUEUE_IDS).apply()
    }

    /** Guarda la lista completa de objetos Episode que han sido descargados.
     *
     * @param downloadedEpisodes La lista de objetos Episode a guardar.
     */
    fun saveDownloadedEpisodes(downloadedEpisodes: List<Episode>) {
        val serializer: KSerializer<List<Episode>> = ListSerializer(Episode.serializer())
        val jsonString = jsonParser.encodeToString(serializer, downloadedEpisodes)
        prefs.edit().putString(PREF_DOWNLOADED_EpisodeS, jsonString).apply()
    }

    /** Carga la lista de objetos Episode que están descargados.
     *
     * @return Una lista de objetos Episode. Si no hay datos, devuelve una lista vacía.
     */
    fun loadDownloadedEpisodes(): List<Episode> {
        val jsonString = prefs.getString(PREF_DOWNLOADED_EpisodeS, null)
        return if (!jsonString.isNullOrEmpty()) {
            try {
                val serializer: KSerializer<List<Episode>> = ListSerializer(Episode.serializer())
                jsonParser.decodeFromString(serializer, jsonString)
            } catch (e: SerializationException) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /** Borra el registro de Episodes descargados. */
    fun clearDownloadedEpisodes() {
        prefs.edit().remove(PREF_DOWNLOADED_EpisodeS).apply()
    }

    /** Guarda los detalles completos de un Episode en un mapa persistido.
     *
     * @param Episode El Episode a guardar.
     */
    fun saveEpisodeDetails(Episode: Episode) {
        val detailsJson = prefs.getString(PREF_EPISODE_DETAILS_MAP, "{}") ?: "{}"
        val detailsMap: MutableMap<String, String> = try {
            jsonParser.decodeFromString(
                MapSerializer(String.serializer(), String.serializer()),
                detailsJson
            ).toMutableMap()
        } catch (e: SerializationException) {
            mutableMapOf()
        }
        detailsMap[Episode.id] = jsonParser.encodeToString(Episode)
        val newDetailsJson = jsonParser.encodeToString(detailsMap)
        prefs.edit().putString(PREF_EPISODE_DETAILS_MAP, newDetailsJson).apply()
    }

    /** Carga los detalles de un Episode específico desde el mapa persistido.
     *
     * @param EpisodeId El ID del Episode a cargar.
     * @return Los detalles del Episode o `null` si no se encuentra.
     */
    fun loadEpisodeDetails(EpisodeId: String): Episode? {
        val detailsJson = prefs.getString(PREF_EPISODE_DETAILS_MAP, "{}") ?: "{}"
        val detailsMap: Map<String, String> = try {
            jsonParser.decodeFromString(MapSerializer(String.serializer(), String.serializer()), detailsJson)
        } catch (e: SerializationException) {
            return null
        }
        val EpisodeJson = detailsMap[EpisodeId.toString()]
        return if (EpisodeJson != null) {
            try {
                jsonParser.decodeFromString<Episode>(EpisodeJson)
            } catch (e: SerializationException) {
                null
            }
        } else {
            null
        }
    }

    /** Limpia los detalles de Episodes almacenados en caché. */
    fun clearEpisodeDetails() {
        prefs.edit().remove(PREF_EPISODE_DETAILS_MAP).apply()
    }

    /** Elimina posiciones, detalles y el Episode actual guardado. */
    fun clearEpisodeProgressData() {
        clearEpisodePositions()
        clearEpisodeDetails()
        saveCurrentEpisodeId(null)
    }

    /** Guarda la preferencia manual del usuario para el tema.
     *
     * @param isDark `true` si el tema está configurado como oscuro, `false` si está configurado como claro, o `null` si no se encuentra.
     */
    fun saveIsManuallySetDarkTheme(isDark: Boolean?) {
        val editor = prefs.edit()
        if (isDark == null) {
            editor.remove(PREF_MANUALLY_SET_DARK_THEME)
        } else {
            editor.putBoolean(PREF_MANUALLY_SET_DARK_THEME, isDark)
        }
        editor.apply()
    }

    /** Carga la preferencia manual del tema.
     *
     * @return `true` si el tema está configurado como oscuro, `false` si está configurado como claro, o `null` si no se encuentra.
     */
    fun loadIsManuallySetDarkTheme(): Boolean? {
        return if (prefs.contains(PREF_MANUALLY_SET_DARK_THEME)) {
            prefs.getBoolean(PREF_MANUALLY_SET_DARK_THEME, false)
        } else {
            null
        }
    }


    /**
     * Guarda el nivel de volumen.
     *
     * @param volume El valor del volumen a guardar (entre 0.0 y 1.0).
     */
    fun saveVolume(volume: Float) {
        prefs.edit().putFloat(PREF_VOLUME_LEVEL, volume).apply()
    }

    /**
     * Carga el nivel de volumen guardado.
     *
     * @return El nivel de volumen (entre 0.0 y 1.0), o 0.5f si no hay uno guardado.
     */
    fun loadVolume(): Float {
        return prefs.getFloat(PREF_VOLUME_LEVEL, DEFAULT_VOLUME_LEVEL)
    }
}
