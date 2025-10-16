package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.paradigmaapp.android.ui.EpisodeListItem
import com.example.paradigmaapp.android.ui.ErrorView
import com.example.paradigmaapp.android.ui.LayoutConstants
import com.example.paradigmaapp.android.ui.ProgramaInfoHeader
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodeViewModel
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.NotificationType
import com.example.paradigmaapp.android.viewmodel.ProgramaViewModel
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.model.Episode
import com.example.paradigmaapp.model.stableListKey

/**
 * Muestra la pantalla de detalles de un programa, incluyendo su información
 * en una cabecera y una lista paginada de sus Episodes.
 *
 * @param programaViewModel ViewModel específico para los datos de este programa.
 * @param mainViewModel ViewModel principal para gestionar la reproducción y el estado de carga.
 * @param queueViewModel ViewModel para interactuar con la cola de reproducción.
 * @param downloadedViewModel ViewModel para gestionar las descargas.
 * @param onEpisodeLongClicked Lambda para manejar el clic largo en un Episode (ej. navegar a detalles).
 * @param onBackClick Lambda para manejar la acción de retroceso.
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramaScreen(
    programaViewModel: ProgramaViewModel,
    mainViewModel: MainViewModel,
    queueViewModel: QueueViewModel,
    downloadedViewModel: DownloadedEpisodeViewModel,
    onEpisodeLongClicked: (Episode) -> Unit,
    onBackClick: () -> Unit
) {
    // Observa los detalles del programa para la cabecera.
    val programa by programaViewModel.programa.collectAsState()
    // Conecta el Flow de PagingData a la UI de Compose.
    val lazyPagingItems = programaViewModel.EpisodesPaginados.collectAsLazyPagingItems()

    // Estados para la Snackbar, descargas y cola.
    val snackbarHostState = remember { SnackbarHostState() }
    val downloadedEpisodes by downloadedViewModel.downloadedEpisodes.collectAsState()
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()

    // Recoge el ID del Episode que se está preparando para la reproducción.
    val preparingEpisodeId by mainViewModel.preparingEpisodeId.collectAsState()

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val halfStatusBarPadding = (statusBarPadding / 2f).coerceAtLeast(0.dp)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val listState = rememberLazyListState()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = LayoutConstants.bottomContentPadding)
            ) {
                // Cabecera con la información del programa.
                item {
                    Spacer(modifier = Modifier.height(halfStatusBarPadding))
                    ProgramaInfoHeader(programa = programa, modifier = Modifier.padding(bottom = 24.dp))

                    if (programa != null) {
                        Text(
                            text = "Episodes",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                        )
                    }
                }

                // Lista de Episodes paginados.
                items(
                    count = lazyPagingItems.itemCount,
                    key = lazyPagingItems.itemKey { it.stableListKey() }
                ) { index ->
                    val episode = lazyPagingItems[index]
                    if (episode != null) {
                        // Determina si este ítem debe mostrar el indicador de carga.
                        val isLoading = episode.id == preparingEpisodeId

                        EpisodeListItem(
                            episode = episode,
                            isLoading = isLoading, // Se pasa el estado de carga
                            onPlayEpisode = { mainViewModel.selectEpisode(it) },
                            onEpisodeLongClick = onEpisodeLongClicked,
                            onAddToQueue = { queueViewModel.addEpisodeToQueue(it) },
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
                            isInQueue = queueEpisodeIds.contains(episode.id),
                            isParentScrolling = listState.isScrollInProgress,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                // Indicadores de estado para la carga de más Episodes.
                item {
                    when (val state = lazyPagingItems.loadState.append) {
                        is LoadState.Loading -> {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                        }
                        is LoadState.Error -> {
                            ErrorView(message = "Error al cargar más: ${state.error.localizedMessage}", onRetry = { lazyPagingItems.retry() })
                        }
                        else -> {}
                    }
                }
            }

            // Indicadores de estado para la carga inicial.
            when (val state = lazyPagingItems.loadState.refresh) {
                is LoadState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is LoadState.Error -> {
                    ErrorView(
                        message = "Error al cargar Episodes:\n${state.error.localizedMessage}",
                        onRetry = { lazyPagingItems.retry() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    if (lazyPagingItems.itemCount == 0 && lazyPagingItems.loadState.append.endOfPaginationReached) {
                        Box(modifier = Modifier.align(Alignment.Center).padding(16.dp)) {
                            Text("No hay Episodes disponibles para este programa.", textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // Botón para volver atrás.
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = halfStatusBarPadding + 8.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
