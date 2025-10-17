package com.example.paradigmaapp.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.example.paradigmaapp.android.navigation.NavGraph
import com.example.paradigmaapp.android.screens.OnboardingScreen
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.SearchViewModel
import com.example.paradigmaapp.android.viewmodel.SettingsViewModel
import com.example.paradigmaapp.android.viewmodel.ViewModelFactory
import com.example.paradigmaapp.android.viewmodel.VolumeControlViewModel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.ui.FloatingDownloadProgressOverlay

/**
 * Composable raíz y punto de entrada de la interfaz de la aplicación.
 * Actúa como un "interruptor": muestra la pantalla de bienvenida (`OnboardingScreen`)
 * o la estructura de navegación principal de la app (`NavGraph`) basándose en si
 * el usuario ya ha completado la introducción.
 *
 * @param viewModelFactory La factoría para crear instancias de ViewModels.
 * @param mainViewModel El ViewModel principal que gestiona el estado global.
 * @param searchViewModel El ViewModel para la funcionalidad de búsqueda.
 * @param settingsViewModel El ViewModel para los ajustes, que se pasará al NavGraph.
 */
@Composable
fun ParadigmaApp(
    viewModelFactory: ViewModelFactory,
    mainViewModel: MainViewModel,
    searchViewModel: SearchViewModel,
    settingsViewModel: SettingsViewModel,
    volumeControlViewModel: VolumeControlViewModel
) {
    val hasCompletedOnboarding by mainViewModel.onboardingCompleted.collectAsState()

    if (hasCompletedOnboarding) {
        val navController = rememberNavController()
        NavGraph(
            navController = navController,
            viewModelFactory = viewModelFactory,
            mainViewModel = mainViewModel,
            searchViewModel = searchViewModel,
            settingsViewModel = settingsViewModel,
            volumeControlViewModel = volumeControlViewModel
        )
    } else {
        OnboardingScreen(
            onContinueClicked = {
                mainViewModel.setOnboardingComplete()
            }
        )
    }
}
