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

    // Inicializa el estado del stream. Si no hay una preferencia guardada,
    // se establece por defecto en 'false' (desactivado).
    private val _isStreamActive = MutableStateFlow(
        appPreferences.loadIsStreamActive() ?: false
    )
    val isStreamActive: StateFlow<Boolean> = _isStreamActive.asStateFlow()

    private val _isManuallySetToDarkTheme = MutableStateFlow<Boolean?>(appPreferences.loadIsManuallySetDarkTheme())
    val isManuallySetToDarkTheme: StateFlow<Boolean?> = _isManuallySetToDarkTheme.asStateFlow()

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
}
