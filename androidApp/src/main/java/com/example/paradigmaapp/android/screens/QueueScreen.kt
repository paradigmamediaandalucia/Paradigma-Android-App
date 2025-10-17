package com.example.paradigmaapp.android.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.ui.EpisodeListItem
import com.example.paradigmaapp.android.ui.LayoutConstants
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodeViewModel
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.NotificationType
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.model.Episode
import com.example.paradigmaapp.model.stableListKey

/**
 * Muestra la cola de reproducción actual del usuario, permitiéndole ver y gestionar
 * los próximos Episodes a escuchar.
 *
 * @param queueViewModel ViewModel que gestiona la cola de reproducción.
 * @param mainViewModel ViewModel principal para gestionar la reproducción y el estado de carga.
 * @param downloadedViewModel ViewModel para interactuar con las descargas.
 * @param onEpisodeSelected Lambda que se invoca al seleccionar un Episode.
 * @param onEpisodeLongClicked Lambda para acciones contextuales sobre un Episode.
 * @param onBackClick Lambda para manejar la acción de retroceso.
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    queueViewModel: QueueViewModel,
    mainViewModel: MainViewModel, // <-- PARÁMETRO AÑADIDO
    downloadedViewModel: DownloadedEpisodeViewModel,
    onEpisodeSelected: (Episode) -> Unit,
    onEpisodeLongClicked: (Episode) -> Unit,
    onBackClick: () -> Unit
) {
    // Estados de la pantalla
    val queueEpisodes by queueViewModel.queueEpisodes.collectAsState()
    val downloadedEpisodes by downloadedViewModel.downloadedEpisodes.collectAsState()
    val preparingEpisodeId by mainViewModel.preparingEpisodeId.collectAsState()
    val currentDownloadStatus by downloadedViewModel.currentDownloadStatus.collectAsState()

    // Controladores de UI y corutinas
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(onBack = onBackClick)

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topContentPadding = statusBarPadding + LayoutConstants.topActionPadding

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Spacer(modifier = Modifier.height(topContentPadding))
            if (queueEpisodes.isEmpty()) {
                // Mensaje si la cola está vacía.
                Box(Modifier.fillMaxSize().padding(16.dp), Alignment.Center) {
                    Text("Tu cola de reproducción está vacía.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                }
            } else {
                // Lista de Episodes en la cola.
                val listState = rememberLazyListState()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = LayoutConstants.bottomContentPadding)
                ) {
                    items(queueEpisodes, key = { it.stableListKey() }) { episode ->
                        val isLoading = episode.id == preparingEpisodeId
                        EpisodeListItem(
                            episode = episode,
                            isLoading = isLoading,
                            onPlayEpisode = { onEpisodeSelected(it) },
                            onEpisodeLongClick = { onEpisodeLongClicked(it) },
                            onAddToQueue = { /* No se puede añadir desde la propia cola */ },
                            onRemoveFromQueue = { queueViewModel.removeEpisodeFromQueue(it) },
                            onDownloadEpisode = { episode ->
                                downloadedViewModel.downloadEpisode(episode) { result ->
                                    result.onSuccess {
                                        mainViewModel.showTopNotification("Descarga completada", NotificationType.SUCCESS)
                                    }.onFailure {
                                        mainViewModel.showTopNotification("Descarga fallida", NotificationType.FAILURE)
                                    }
                                }
                            },
                            onDeleteDownload = { downloadedViewModel.deleteDownloadedEpisode(it) },
                            isDownloaded = downloadedEpisodes.any { it.id == episode.id },
                            isInQueue = true, // Todos en esta lista están en la cola
                            isParentScrolling = listState.isScrollInProgress,
                            currentDownloadStatus = currentDownloadStatus,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
