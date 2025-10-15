package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.ui.EpisodeListItem
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodeViewModel
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.NotificationType
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.model.Episode

/**
 * Muestra la lista de Episodes que el usuario ha descargado en el dispositivo.
 * Permite la reproducción offline y la gestión de estas descargas.
 *
 * @param downloadedEpisodeViewModel ViewModel que gestiona la lógica de las descargas.
 * @param mainViewModel ViewModel principal para gestionar la reproducción y el estado de carga.
 * @param queueViewModel ViewModel para interactuar con la cola.
 * @param onEpisodeSelected Lambda que se invoca al seleccionar un Episode.
 * @param onEpisodeLongClicked Lambda para acciones contextuales sobre un Episode.
 * @param onBackClick Lambda para manejar la acción de retroceso.
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadedEpisodeScreen(
    downloadedEpisodeViewModel: DownloadedEpisodeViewModel,
    mainViewModel: MainViewModel,
    queueViewModel: QueueViewModel,
    onEpisodeSelected: (Episode) -> Unit,
    onEpisodeLongClicked: (Episode) -> Unit,
    onBackClick: () -> Unit
) {
    // Estados de la pantalla
    val downloadedEpisodes by downloadedEpisodeViewModel.downloadedEpisodes.collectAsState()
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()
    val preparingEpisodeId by mainViewModel.preparingEpisodeId.collectAsState()

    // Controladores de UI y corutinas
    val snackbarHostState = remember { SnackbarHostState() }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mis Descargas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background)
        ) {
            if (downloadedEpisodes.isEmpty()) {
                // Mensaje si no hay descargas.
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tienes Episodes descargados.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Lista de Episodes descargados.
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp)
                ) {
                    items(downloadedEpisodes, key = { it.id }) { Episode ->
                        val isLoading = Episode.id == preparingEpisodeId
                        EpisodeListItem(
                            Episode = Episode,
                            isLoading = isLoading, // <-- Pasando el estado de carga
                            onPlayEpisode = { onEpisodeSelected(it) },
                            onEpisodeLongClick = { onEpisodeLongClicked(it) },
                            onAddToQueue = { queueViewModel.addEpisodeToQueue(it) },
                            onRemoveFromQueue = { queueViewModel.removeEpisodeFromQueue(it) },
                            onDownloadEpisode = { Episode ->
                                downloadedEpisodeViewModel.downloadEpisode(Episode) { result ->
                                    result.onSuccess {
                                        mainViewModel.showTopNotification("Descarga completada", NotificationType.SUCCESS)
                                    }.onFailure {
                                        mainViewModel.showTopNotification("Descarga fallida", NotificationType.FAILURE)
                                    }
                                }
                            },
                            onDeleteDownload = { downloadedEpisodeViewModel.deleteDownloadedEpisode(it) },
                            isDownloaded = true, // Todos en esta lista están descargados
                            isInQueue = queueEpisodeIds.contains(Episode.id),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
