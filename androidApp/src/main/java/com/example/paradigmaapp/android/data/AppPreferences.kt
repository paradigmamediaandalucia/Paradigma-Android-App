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
        private const val PREF_EPISODE_POSITIONS = "episodePositions_v2"
        private const val PREF_EPISODE_QUEUE_IDS = "episodeQueueIds_v2"
        private const val PREF_DOWNLOADED_EpisodeS = "downloadedEpisodes_v1"
        private const val PREF_EPISODE_DETAILS_MAP = "episodeDetailsMap_v1"
        private const val PREF_ONBOARDING_COMPLETE = "onboardingComplete_v1"
        private const val PREF_MANUALLY_SET_DARK_THEME = "manuallySetDarkTheme_v1"

        private const val PREF_VOLUME_LEVEL = "volumeLevel_v1"
        private const val DEFAULT_VOLUME_LEVEL = 0.5f
    }

    /**
     * Guarda si el usuario ha completado la pantalla de introducción.
     */
    fun saveOnboardingComplete(isComplete: Boolean) {
        prefs.edit().putBoolean(PREF_ONBOARDING_COMPLETE, isComplete).apply()
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

    /** Guarda la preferencia de si el streaming debe estar activo al iniciar.
     *
     * @param isActive `true` si el streaming debe estar activo al iniciar, `false` en caso contrario.
     */
    fun saveIsStreamActive(isActive: Boolean) {
        prefs.edit().putBoolean(PREF_IS_STREAM_ACTIVE, isActive).apply()
    }

    /** Carga la preferencia de si el streaming debe estar activo.
     *
     * @return `true` si el streaming debe estar activo al iniciar, `false` en caso contrario.
     */
    fun loadIsStreamActive(): Boolean {
        return prefs.getBoolean(PREF_IS_STREAM_ACTIVE, true)
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
        detailsMap[Episode.id.toString()] = jsonParser.encodeToString(Episode)
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
