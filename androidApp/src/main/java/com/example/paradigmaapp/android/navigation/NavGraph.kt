package com.example.paradigmaapp.android.navigation

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.paradigmaapp.android.audio.AudioPlayer
import com.example.paradigmaapp.android.audio.VolumeControl
import com.example.paradigmaapp.android.screens.*
import com.example.paradigmaapp.android.R
import com.example.paradigmaapp.android.ui.TopNotificationBanner
import com.example.paradigmaapp.android.ui.navigateToScreenIfDifferent
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
    val isAndainaStreamActive by mainViewModel.isAndainaStreamActive.collectAsState()
    val shouldShowPlayer = currentPlayingEpisode != null || isAndainaStreamActive || isAndainaPlaying
    val andainaRadioInfo by mainViewModel.andainaRadioInfo.collectAsState()
    val volumeBottomSheetScaffoldState = rememberBottomSheetScaffoldState()
    val isFullScreenPlayerVisible by mainViewModel.isFullScreenPlayerVisible.collectAsState()
    val topNotification by mainViewModel.topNotification.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val canNavigateBack = navController.previousBackStackEntry != null
    val shouldShowTopActions = !isFullScreenPlayerVisible
    val context = LocalContext.current
    val appVersionName = remember(context) {
        runCatching {
            val packageManager = context.packageManager
            val packageName = context.packageName
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0L)
                ).versionName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).versionName
            }
        }.getOrNull() ?: "Desconocido"
    }

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
                            settingsViewModel = settingsViewModel,
                            onProgramaSelected = { progId, progNombre -> navController.navigate(Screen.Programa.createRoute(progId, progNombre)) }
                        )
                    }
                    composable(route = Screen.Programa.route, arguments = listOf(navArgument("programaId") { type = NavType.StringType }, navArgument("programaNombre") { type = NavType.StringType })) { navBackStackEntry -> val programaViewModel: ProgramaViewModel = viewModel(key = "programa_vm_${navBackStackEntry.arguments?.getString("programaId")}", viewModelStoreOwner = navBackStackEntry, factory = viewModelFactory); ProgramaScreen(programaViewModel, mainViewModel, mainViewModel.queueViewModel, mainViewModel.downloadedViewModel, onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) }, onBackClick = { navController.popBackStack() }) }
                    composable(Screen.Search.route) { SearchScreen(searchViewModel = searchViewModel, mainViewModel = mainViewModel, queueViewModel = mainViewModel.queueViewModel, downloadedViewModel = mainViewModel.downloadedViewModel, onEpisodeSelected = { mainViewModel.selectEpisode(it) }, onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) }, onBackClick = { navController.popBackStack() }) }
                    composable(Screen.Downloads.route) { DownloadedEpisodeScreen(downloadedEpisodeViewModel = mainViewModel.downloadedViewModel, mainViewModel = mainViewModel, queueViewModel = mainViewModel.queueViewModel, onEpisodeSelected = { mainViewModel.selectEpisode(it) }, onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) }, onBackClick = { navController.popBackStack() }) }
                    composable(Screen.Queue.route) { QueueScreen(queueViewModel = mainViewModel.queueViewModel, mainViewModel = mainViewModel, downloadedViewModel = mainViewModel.downloadedViewModel, onEpisodeSelected = { mainViewModel.selectEpisode(it) }, onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) }, onBackClick = { navController.popBackStack() }) }
                    composable(Screen.OnGoing.route) { OnGoingEpisodeScreen(onGoingEpisodeViewModel = mainViewModel.onGoingViewModel, mainViewModel = mainViewModel, queueViewModel = mainViewModel.queueViewModel, downloadedViewModel = mainViewModel.downloadedViewModel, onEpisodeSelected = { mainViewModel.selectEpisode(it) }, onEpisodeLongClicked = { navController.navigate(Screen.EpisodeDetail.createRoute(it.id)) }, onBackClick = { navController.popBackStack() }) }
                    composable(route = Screen.EpisodeDetail.route, arguments = listOf(navArgument("episodeId") { type = NavType.StringType })) { navBackStackEntry -> val episodeDetailViewModel: EpisodeDetailViewModel = viewModel(key = "episode_detail_vm_${navBackStackEntry.arguments?.getString("episodeId")}", viewModelStoreOwner = navBackStackEntry, factory = viewModelFactory); EpisodeDetailScreen(episodeDetailViewModel, mainViewModel, mainViewModel.queueViewModel, mainViewModel.downloadedViewModel, onBackClick = { navController.popBackStack() }) }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            settingsViewModel = settingsViewModel,
                            onBackClick = { navController.popBackStack() },
                            onClearQueue = {
                                mainViewModel.queueViewModel.clearQueue()
                                mainViewModel.showTopNotification("Cola vaciada", NotificationType.SUCCESS)
                            },
                            onClearDownloads = {
                                mainViewModel.downloadedViewModel.clearAllDownloads { result ->
                                    result.onSuccess {
                                        mainViewModel.showTopNotification("Descargas eliminadas", NotificationType.SUCCESS)
                                    }.onFailure {
                                        mainViewModel.showTopNotification("Error al eliminar descargas", NotificationType.FAILURE)
                                    }
                                }
                            },
                            onClearListeningHistory = {
                                mainViewModel.onGoingViewModel.clearListeningHistory()
                                mainViewModel.showTopNotification("Historial limpiado", NotificationType.SUCCESS)
                            },
                            appVersionName = appVersionName
                        )
                    }
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
                    if (shouldShowPlayer) {
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
                    } else {
                        RadioQuickActionButton(
                            onClick = {
                                if (!isAndainaStreamActive && !isAndainaPlaying) {
                                    mainViewModel.toggleAndainaStreamPlayer()
                                }
                            }
                        )
                    }

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
                onBackClick = { mainViewModel.toggleFullScreenPlayer() },
                onOpenSettings = {
                    mainViewModel.toggleFullScreenPlayer()
                    navController.navigate(Screen.Settings.route) {
                        launchSingleTop = true
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        restoreState = true
                    }
                }
            )
        }

        if (shouldShowTopActions) {
            val isOnSettingsScreen = navBackStackEntry?.destination?.route == Screen.Settings.route
            val buttonBackground = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                if (navBackStackEntry?.destination?.route !in setOf(Screen.Home.route, Screen.Search.route)) {
                    IconButton(
                        onClick = {
                            val didPop = navController.popBackStack()
                            if (!didPop) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(navController.graph.findStartDestination().id)
                                    launchSingleTop = true
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(44.dp)
                            .shadow(6.dp, CircleShape, clip = false)
                            .clip(CircleShape)
                            .background(buttonBackground, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
// Boton ajustes
                if (!isOnSettingsScreen) {
                    FilledIconButton(
                        onClick = { navigateToScreenIfDifferent(navController, Screen.Settings.route) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 0.dp, top = 4.dp)
                            .size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Abrir ajustes",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
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

@Composable
private fun RadioQuickActionButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 16.dp, bottom = 16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                painter = painterResource(R.mipmap.streaming),
                contentDescription = "Reproducir radio",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
