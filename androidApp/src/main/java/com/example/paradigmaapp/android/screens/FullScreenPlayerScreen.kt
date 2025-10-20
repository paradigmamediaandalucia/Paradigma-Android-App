package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R
import com.example.paradigmaapp.android.audio.VolumeControl
import com.example.paradigmaapp.android.ui.formatTime
import com.example.paradigmaapp.android.utils.dpToPreferredSquarePx
import com.example.paradigmaapp.android.utils.rememberCoilImageRequest
import com.example.paradigmaapp.android.utils.selectSpreakerImageSource
import com.example.paradigmaapp.android.utils.unescapeHtmlEntities
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.DownloadStatus
import com.example.paradigmaapp.android.viewmodel.VolumeControlViewModel
import com.example.paradigmaapp.android.viewmodel.NotificationType
import kotlin.math.roundToInt

/**
 * Un Composable que renderiza la pantalla del reproductor a pantalla completa.
 * Muestra información detallada del Episode en reproducción, como la carátula,
 * el título y el progreso, junto con controles de reproducción extendidos
 * (play/pausa, anterior/siguiente).
 *
 * @param mainViewModel El ViewModel principal que proporciona el estado del reproductor
 * (Episode actual, estado de reproducción, progreso, etc.) y maneja las acciones del usuario.
 * @param onBackClick Lambda que se invoca cuando el usuario pulsa el botón de retroceso
 * para cerrar esta pantalla.
 * @param volumeControlViewModel El ViewModel para la gestión del volumen.
 * @param onOpenSettings Lambda para navegar a la pantalla de ajustes desde el reproductor.
 *
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPlayerScreen(
    mainViewModel: MainViewModel,
    volumeControlViewModel: VolumeControlViewModel,
    onBackClick: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val currentEpisode by mainViewModel.currentPlayingEpisode.collectAsState()
    val isPlaying by mainViewModel.isPodcastPlaying.collectAsState()
    val progress by mainViewModel.podcastProgress.collectAsState()
    val duration by mainViewModel.podcastDuration.collectAsState()
    val hasNextEpisode by mainViewModel.hasNextEpisode.collectAsState()
    val hasPreviousEpisode by mainViewModel.hasPreviousEpisode.collectAsState()
    val currentDownloadStatus by mainViewModel.downloadedViewModel.currentDownloadStatus.collectAsState()
    val queueEpisodeIds by mainViewModel.queueViewModel.queueEpisodeIds.collectAsState()
    val downloadedEpisodes by mainViewModel.downloadedViewModel.downloadedEpisodes.collectAsState()
    var overflowExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reproduciendo ahora") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    currentEpisode?.let { episode ->
                        IconButton(
                            onClick = { overflowExpanded = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (overflowExpanded) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent
                                )
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                        }
                        val isInQueue = queueEpisodeIds.contains(episode.id)
                        val isDownloaded = downloadedEpisodes.any { it.id == episode.id }
                        DropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = { overflowExpanded = false },
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = MaterialTheme.shapes.extraSmall
                            )
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isInQueue) "Quitar de la cola" else "Añadir a la cola") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isInQueue) Icons.Default.RemoveCircleOutline else Icons.Default.PlaylistAdd,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    if (isInQueue) {
                                        mainViewModel.queueViewModel.removeEpisodeFromQueue(episode)
                                        mainViewModel.showTopNotification("Eliminado de la cola", NotificationType.SUCCESS)
                                    } else {
                                        mainViewModel.queueViewModel.addEpisodeToQueue(episode)
                                        mainViewModel.showTopNotification("Añadido a la cola", NotificationType.SUCCESS)
                                    }
                                    overflowExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isDownloaded) "Eliminar descarga" else "Descargar") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isDownloaded) Icons.Default.DeleteOutline else Icons.Default.Download,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    if (isDownloaded) {
                                        mainViewModel.downloadedViewModel.deleteDownloadedEpisode(episode)
                                        mainViewModel.showTopNotification("Descarga eliminada", NotificationType.SUCCESS)
                                        overflowExpanded = false
                                    } else {
                                        overflowExpanded = false
                                        mainViewModel.downloadedViewModel.downloadEpisode(episode) { result ->
                                            result.onSuccess {
                                                mainViewModel.showTopNotification("Descarga completada", NotificationType.SUCCESS)
                                            }.onFailure {
                                                mainViewModel.showTopNotification("Descarga fallida", NotificationType.FAILURE)
                                            }
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Ajustes") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                                },
                                onClick = {
                                    overflowExpanded = false
                                    onOpenSettings()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            currentEpisode?.let { episode ->
                currentDownloadStatus?.let { status ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        DownloadStatusChip(status = status)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                val configuration = LocalConfiguration.current
                val density = LocalDensity.current.density
                val imageWidthDp = (configuration.screenWidthDp - 48).coerceAtLeast(160)
                val targetPx = remember(configuration.screenWidthDp, density) {
                    dpToPreferredSquarePx(imageWidthDp.toFloat(), density)
                }
                val imageSource = remember(episode.imageUrl, episode.imageOriginalUrl, targetPx) {
                    selectSpreakerImageSource(episode.imageUrl, episode.imageOriginalUrl, targetPx)
                }
                val artworkRequest = rememberCoilImageRequest(
                    primaryData = imageSource.preferred,
                    fallbackData = imageSource.fallback,
                    debugLabel = "player:${episode.id}"
                )

                AsyncImage(
                    model = artworkRequest,
                    contentDescription = "Portada de ${episode.title.unescapeHtmlEntities()}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.mipmap.logo_foreground),
                    placeholder = painterResource(R.mipmap.logo_foreground)
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = episode.title.unescapeHtmlEntities(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = progress,
                        onValueChange = { mainViewModel.seekEpisodeTo(it) },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            formatTime((progress * duration).toLong()),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(formatTime(duration), style = MaterialTheme.typography.labelMedium)
                    }
                }

                val previousButtonTint = if (hasPreviousEpisode) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                val nextButtonTint = if (hasNextEpisode) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { mainViewModel.playPreviousEpisode() },
                        modifier = Modifier.size(56.dp),
                        enabled = hasPreviousEpisode
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Episodio anterior",
                            modifier = Modifier.fillMaxSize(),
                            tint = previousButtonTint
                        )
                    }

                    IconButton(
                        onClick = { mainViewModel.onPlayerPlayPauseClick() },
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { mainViewModel.playNextEpisode() },
                        modifier = Modifier.size(56.dp),
                        enabled = hasNextEpisode
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Siguiente episodio",
                            modifier = Modifier.fillMaxSize(),
                            tint = nextButtonTint
                        )
                    }
                }

                VolumeControl(volumeControlViewModel = volumeControlViewModel)
            }
        }
    }
}

@Composable
private fun DownloadStatusChip(status: DownloadStatus) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DownloadProgressIndicator(
            progress = status.progress,
            isComplete = status.isComplete,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (status.isComplete) "Descarga lista" else "Descargando",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = status.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DownloadProgressIndicator(
    progress: Float,
    isComplete: Boolean,
    modifier: Modifier = Modifier
) {
    val strokeWidth = with(LocalDensity.current) { 6.dp.toPx() }
    val clampedProgress = progress.coerceIn(0f, 1f)
    val backgroundColor = Color(0xFFFFD54F)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = backgroundColor)
            if (clampedProgress > 0f && !isComplete) {
                drawArc(
                    color = Color.Black,
                    startAngle = -90f,
                    sweepAngle = clampedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            } else if (isComplete) {
                drawArc(
                    color = Color.Black,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        if (isComplete) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Descarga finalizada",
                tint = Color.Black,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(
                text = "${(clampedProgress * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
