package com.example.paradigmaapp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.paradigmaapp.android.api.AndainaStream
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.SearchViewModel
import com.example.paradigmaapp.android.viewmodel.SettingsViewModel
import com.example.paradigmaapp.android.viewmodel.ViewModelFactory
import com.example.paradigmaapp.android.viewmodel.VolumeControlViewModel
import com.example.paradigmaapp.config.Config
import com.example.paradigmaapp.sdk.ParadigmaSDK
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Actividad principal y punto de entrada de la aplicación.
 * Responsable de inicializar todas las dependencias clave y la UI.
 */
class MainActivity : ComponentActivity() {

    private lateinit var viewModelFactory: ViewModelFactory
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Config.initialize(applicationContext)

        val appPreferencesInstance = AppPreferences(applicationContext)
        val appDatabase = Room.databaseBuilder(
            applicationContext,
            com.example.paradigmaapp.cache.AppDatabase::class.java,
            "app-database"
        ).build()

        val paradigmaSDK = ParadigmaSDK(appDatabase)

        val andainaStream = AndainaStream(applicationContext, Config.LIVE_STREAM_URL, Config.LIVE_STREAM_API_URL)

        val volumeControlViewModel = VolumeControlViewModel(appPreferences = appPreferencesInstance)

        viewModelFactory = ViewModelFactory(
            appPreferences = appPreferencesInstance,
            paradigmaSDK = paradigmaSDK,
            applicationContext = applicationContext,
            andainaStream = andainaStream,
            volumeControlViewModel = volumeControlViewModel
        )

        setContent {
            val settingsViewModel: SettingsViewModel = ViewModelProvider(this, viewModelFactory)[SettingsViewModel::class.java]
            val manualDarkThemeSetting by settingsViewModel.isManuallySetToDarkTheme.collectAsState()

            Theme(manualDarkThemeSetting = manualDarkThemeSetting) {
                val mainViewModel: MainViewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
                val searchViewModel: SearchViewModel = ViewModelProvider(this, viewModelFactory)[SearchViewModel::class.java]

                ParadigmaApp(
                    viewModelFactory = viewModelFactory,
                    mainViewModel = mainViewModel,
                    searchViewModel = searchViewModel,
                    settingsViewModel = settingsViewModel,
                    volumeControlViewModel = volumeControlViewModel
                )
            }
        }
    }
}
