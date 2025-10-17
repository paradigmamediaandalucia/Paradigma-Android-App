package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.ViewModel
import com.example.paradigmaapp.android.data.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.paradigmaapp.config.Config

/**
 * ViewModel para la pantalla de Ajustes.
 * Gestiona las preferencias del usuario y expone datos de la configuración remota.
 *
 * @property appPreferences Instancia para guardar y cargar las preferencias locales.
 * @property remoteConfigService Servicio para obtener URLs y otros valores de configuración.
 */
class SettingsViewModel(
    private val appPreferences: AppPreferences
) : ViewModel() {

    // Estado para el modo de visualización de programas (grid/list)
    private val _isProgramListMode = MutableStateFlow(appPreferences.loadProgramDisplayMode())
    val isProgramListMode: StateFlow<Boolean> = _isProgramListMode.asStateFlow()

    // Inicializa el estado del stream. Si no hay una preferencia guardada,
    // se establece por defecto en 'false' (desactivado).
    private val _isStreamActive = MutableStateFlow(appPreferences.loadIsStreamActive())
    val isStreamActive: StateFlow<Boolean> = _isStreamActive.asStateFlow()

    private val _isManuallySetToDarkTheme = MutableStateFlow<Boolean?>(appPreferences.loadIsManuallySetDarkTheme())
    val isManuallySetToDarkTheme: StateFlow<Boolean?> = _isManuallySetToDarkTheme.asStateFlow()

    private val _rememberEpisodeProgress = MutableStateFlow(appPreferences.loadRememberEpisodeProgress())
    val rememberEpisodeProgress: StateFlow<Boolean> = _rememberEpisodeProgress.asStateFlow()

    private val _autoPlayNextEpisode = MutableStateFlow(appPreferences.loadAutoPlayNextEpisode())
    val autoPlayNextEpisode: StateFlow<Boolean> = _autoPlayNextEpisode.asStateFlow()

    /**
     * Expone la URL del sitio web obtenida del servicio de configuración.
     * La vista (`SettingsScreen`) usará esta propiedad para el enlace.
     */
    val mainWebsiteUrl: String
        get() = Config.MAIN_WEBSITE_URL

    /**
     * Alterna la preferencia de si el streaming debe estar activo al iniciar la app.
     */
    fun toggleStreamActive() {
        val newState = !_isStreamActive.value
        appPreferences.saveIsStreamActive(newState)
        _isStreamActive.value = newState
    }

    /**
     * Establece la preferencia manual del tema de la aplicación.
     */
    fun setThemePreference(isDark: Boolean?) {
        appPreferences.saveIsManuallySetDarkTheme(isDark)
        _isManuallySetToDarkTheme.value = isDark
    }

    /** Alterna si se debe recordar el progreso de los Episodes. */
    fun toggleRememberEpisodeProgress() {
        val newState = !_rememberEpisodeProgress.value
        _rememberEpisodeProgress.value = newState
        appPreferences.saveRememberEpisodeProgress(newState)
    }

    /** Alterna la reproducción automática del siguiente Episode. */
    fun toggleAutoPlayNextEpisode() {
        val newState = !_autoPlayNextEpisode.value
        _autoPlayNextEpisode.value = newState
        appPreferences.saveAutoPlayNextEpisode(newState)
    }

    /** Alterna el modo de visualización de programas entre cuadrícula y lista. */
    fun toggleProgramDisplayMode() {
        val newState = !_isProgramListMode.value
        appPreferences.saveProgramDisplayMode(newState)
        _isProgramListMode.value = newState
    }

    /** Permite establecer explícitamente el modo de visualización de programas. */
    fun setProgramDisplayMode(isList: Boolean) {
        appPreferences.saveProgramDisplayMode(isList)
        _isProgramListMode.value = isList
    }
}
