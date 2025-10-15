package com.example.paradigmaapp.android.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.paradigmaapp.android.audio.AudioPlayer
import com.example.paradigmaapp.android.audio.VolumeControl
import com.example.paradigmaapp.android.screens.*
import com.example.paradigmaapp.android.ui.TopNotificationBanner
import com.example.paradigmaapp.android.viewmodel.*
import kotlinx.coroutines.launch

/**
 * Define el grafo de navegación para la aplicación principal, una vez que el usuario
 * ha completado el onboarding.
 * Gestiona la estructura de la UI con el reproductor y la barra de navegación persistentes,
 * y define todas las rutas navegables (pantallas) de la app.
 *
 * @author Mario Alguacil Juárez
 * @param navController El controlador de navegación para moverse entre pantallas.
 * @param viewModelFactory La factoría para crear instancias de ViewModels.
 * @param mainViewModel El ViewModel principal que gestiona el estado global.
 * @param searchViewModel El ViewModel para la funcionalidad de búsqueda.
 * @param settingsViewModel El ViewModel para la configuración de la app.
 * @param volumeControlViewModel El ViewModel para el control de volumen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    viewModelFactory: ViewModelFactory,
    mainViewModel: MainViewModel,
    searchViewModel: SearchViewModel,
    settingsViewModel: SettingsViewModel,
    volumeControlViewModel: VolumeControlViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val currentPlayingEpisode by mainViewModel.currentPlayingEpisode.collectAsState()
    val isPodcastPlaying by mainViewModel.isPodcastPlaying.collectAsState()
    val isAndainaPlaying by mainViewModel.isAndainaPlaying.collectAsState()
    val isPlayingGeneral = if (currentPlayingEpisode != null) isPodcastPlaying else isAndainaPlaying
    val episodeProgress by mainViewModel.podcastProgress.collectAsState()
    val isAndainaStreamActive by settingsViewModel.isStreamActive.collectAsState()
    val andainaRadioInfo by mainViewModel.andainaRadioInfo.collectAsState()
    val volumeBottomSheetScaffoldState = rememberBottomSheetScaffoldState()
    val isFullScreenPlayerVisible by mainViewModel.isFullScreenPlayerVisible.collectAsState()
    val topNotification by mainViewModel.topNotification.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = volumeBottomSheetScaffoldState,
            sheetContent = {
                VolumeControl(volumeControlViewModel = volumeControlViewModel)
            },
            sheetPeekHeight = 0.dp,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            mainViewModel = mainViewModel,
                            onProgramaSelected = { progId, progNombre -> navController.navigate(Screen.Programa.createRoute(progId, progNombre)) }
                        )
                    }
                    composable(route = Screen.Programa.route, arguments = listOf(navArgument("programaId") { type = NavType.StringType }, navArgument("programaNombre") { type = NavType.StringType })) { navBackStackEntry -> val programaViewModel: ProgramaViewModel = viewModel(key = "programa_vm_${navBackStackEntry.arguments?.getString("programaId")}", viewModelStoreOwner = navBackStackEntry, factory = viewModelFactory); ProgramaScreen(programaViewModel, mainViewModel, mainViewModel.queueViewModel, mainViewModel.downloadedViewModel, onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) }, onBackClick = { navController.popBackStack() }) }
                    composable(Screen.Search.route) { SearchScreen(searchViewModel = searchViewModel, mainViewModel = mainViewModel, queueViewModel = mainViewModel.queueViewModel, downloadedViewModel = mainViewModel.downloadedViewModel, onEpisodeSelected = { mainViewModel.selectEpisode(it) }, onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) }, onBackClick = { navController.popBackStack() }) }
                    composable(Screen.Downloads.route) { DownloadedEpisodeScreen(downloadedEpisodeViewModel = mainViewModel.downloadedViewModel, mainViewModel = mainViewModel, queueViewModel = mainViewModel.queueViewModel, onEpisodeSelected = { mainViewModel.selectEpisode(it) }, onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) }, onBackClick = { navController.popBackStack() }) }
                    composable(Screen.Queue.route) { QueueScreen(queueViewModel = mainViewModel.queueViewModel, mainViewModel = mainViewModel, downloadedViewModel = mainViewModel.downloadedViewModel, onEpisodeSelected = { mainViewModel.selectEpisode(it) }, onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) }, onBackClick = { navController.popBackStack() }) }
                    composable(Screen.OnGoing.route) { OnGoingEpisodeScreen(onGoingEpisodeViewModel = mainViewModel.onGoingViewModel, mainViewModel = mainViewModel, queueViewModel = mainViewModel.queueViewModel, downloadedViewModel = mainViewModel.downloadedViewModel, onEpisodeSelected = { mainViewModel.selectEpisode(it) }, onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) }, onBackClick = { navController.popBackStack() }) }
                    composable(route = Screen.EpisodeDetail.route, arguments = listOf(navArgument("episodeId") { type = NavType.StringType })) { navBackStackEntry -> val episodeDetailViewModel: EpisodeDetailViewModel = viewModel(key = "episode_detail_vm_${navBackStackEntry.arguments?.getString("episodeId")}", viewModelStoreOwner = navBackStackEntry, factory = viewModelFactory); EpisodeDetailScreen(episodeDetailViewModel, mainViewModel, mainViewModel.queueViewModel, mainViewModel.downloadedViewModel, onBackClick = { navController.popBackStack() }) }
                    composable(Screen.Settings.route) { SettingsScreen(settingsViewModel = settingsViewModel, onBackClick = { navController.popBackStack() }) }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                ) {
                    AudioPlayer(
                        activePlayer = if (currentPlayingEpisode != null) mainViewModel.podcastExoPlayer else mainViewModel.andainaStreamPlayer.exoPlayer,
                        currentEpisode = currentPlayingEpisode,
                        andainaRadioInfo = andainaRadioInfo,
                        isPlayingGeneral = isPlayingGeneral,
                        episodeProgress = episodeProgress,
                        onProgressChange = { newProgress -> mainViewModel.seekEpisodeTo(newProgress) },
                        isAndainaStreamActive = isAndainaStreamActive,
                        isAndainaPlaying = isAndainaPlaying,
                        onPlayPauseClick = { mainViewModel.onPlayerPlayPauseClick() },
                        onPlayStreamingClick = { mainViewModel.toggleAndainaStreamPlayer() },
                        onEpisodeInfoClick = { mainViewModel.toggleFullScreenPlayer() },
                        onVolumeIconClick = {
                            coroutineScope.launch {
                                if (volumeBottomSheetScaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
                                    volumeBottomSheetScaffoldState.bottomSheetState.partialExpand()
                                } else {
                                    volumeBottomSheetScaffoldState.bottomSheetState.expand()
                                }
                            }
                        },
                        volumeControlViewModel = volumeControlViewModel
                    )

                    BottomNavigationBar(
                        navController = navController
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isFullScreenPlayerVisible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            FullScreenPlayerScreen(
                mainViewModel = mainViewModel,
                volumeControlViewModel = volumeControlViewModel,
                onBackClick = { mainViewModel.toggleFullScreenPlayer() }
            )
        }

        AnimatedVisibility(
            visible = topNotification != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            topNotification?.let { (message, type) ->
                TopNotificationBanner(message = message, type = type)
            }
        }
    }
}
