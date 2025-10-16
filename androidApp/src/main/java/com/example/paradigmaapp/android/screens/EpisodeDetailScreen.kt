package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R
import com.example.paradigmaapp.android.ui.ErrorType
import com.example.paradigmaapp.android.ui.ErrorView
import com.example.paradigmaapp.android.ui.LayoutConstants
import com.example.paradigmaapp.android.utils.extractMeaningfulDescription
import com.example.paradigmaapp.android.utils.unescapeHtmlEntities
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodeViewModel
import com.example.paradigmaapp.android.viewmodel.EpisodeDetailViewModel
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.NotificationType
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.android.ui.formatTime
import com.example.paradigmaapp.model.Episode
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Pantalla que muestra los detalles completos de un [Episode] específico.
 * Proporciona información como título, imagen, descripción, fecha, duración,
 * y programas asociados. Ofrece acciones como reproducir, añadir/quitar de cola
 * y descargar/eliminar descarga.
 *
 * @param episodeDetailViewModel ViewModel que gestiona la carga y el estado del Episode actual.
 * @param mainViewModel ViewModel principal para acciones de reproducción y notificaciones.
 * @param queueViewModel ViewModel para gestionar la cola de reproducción.
 * @param downloadedViewModel ViewModel para gestionar las descargas.
 * @param onBackClick Lambda para manejar la acción de retroceso y volver a la pantalla anterior.
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDetailScreen(
    episodeDetailViewModel: EpisodeDetailViewModel,
    mainViewModel: MainViewModel,
    queueViewModel: QueueViewModel,
    downloadedViewModel: DownloadedEpisodeViewModel,
    onBackClick: () -> Unit
) {
    val EpisodeState by episodeDetailViewModel.episode.collectAsState()
    val programasAsociados by episodeDetailViewModel.associatedPrograms.collectAsState()
    val isLoading by episodeDetailViewModel.isLoading.collectAsState()
    val error by episodeDetailViewModel.error.collectAsState()

    val downloadedEpisodes by downloadedViewModel.downloadedEpisodes.collectAsState()
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val halfStatusBarPadding = if (statusBarPadding > 0.dp) statusBarPadding * 0.5f else 0.dp

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading && EpisodeState == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null && EpisodeState == null -> {
                    val errorType = if (error!!.contains("internet", ignoreCase = true)) {
                        ErrorType.NO_INTERNET
                    } else if (error!!.contains("No se pudo encontrar", ignoreCase = true)) {
                        ErrorType.NO_RESULTS
                    } else {
                        ErrorType.GENERAL_SERVER_ERROR
                    }
                    ErrorView(
                        message = error!!,
                        errorType = errorType,
                        onRetry = { episodeDetailViewModel.loadEpisodeDetails() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                EpisodeState != null -> {
                    val episode = EpisodeState!!
                    val isDownloaded = downloadedEpisodes.any { it.id == episode.id }
                    val isInQueue = queueEpisodeIds.contains(episode.id)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = LayoutConstants.bottomContentPadding)
                    ) {
                        val configuration = LocalConfiguration.current
                        val screenWidth = configuration.screenWidthDp.dp
                        val availableWidth = (screenWidth - 32.dp).coerceAtLeast(0.dp)
                        val targetWidth = if (availableWidth == 0.dp) {
                            232.dp
                        } else {
                            (availableWidth * 0.64f).coerceIn(174.dp, 275.dp)
                        }

                        Spacer(modifier = Modifier.height(halfStatusBarPadding))
                        AsyncImage(
                            model = episode.imageUrl ?: episode.imageOriginalUrl,
                            contentDescription = "Portada de ${episode.title.unescapeHtmlEntities()}",
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .align(Alignment.CenterHorizontally)
                                .shadow(6.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .widthIn(max = targetWidth)
                                .sizeIn(
                                    minWidth = 174.dp,
                                    minHeight = 174.dp,
                                    maxWidth = targetWidth,
                                    maxHeight = targetWidth
                                )
                                .aspectRatio(1f),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.mipmap.logo_foreground),
                            placeholder = painterResource(R.mipmap.logo_foreground)
                        )

                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = episode.title.unescapeHtmlEntities(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MetaDataItem(Icons.Default.DateRange, formatarFechaIso(episode.date))
                                if (episode.duration > 0) { // Duration is Long, check if greater than 0
                                    MetaDataItem(Icons.Default.Timer, formatTime(episode.duration))
                                }
                            }
                            if (programasAsociados.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Programa: ${programasAsociados.joinToString { it.title.unescapeHtmlEntities() }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            // Botones de acción
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalIconButton(onClick = { mainViewModel.selectEpisode(episode, true) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Filled.PlayArrow, "Reproducir Episode") }
                                val queueIcon = if (isInQueue) Icons.Filled.RemoveCircleOutline else Icons.Filled.PlaylistAdd
                                val queueAction = if (isInQueue) "Quitar de cola" else "Añadir a cola"
                                OutlinedIconButton(onClick = { if (isInQueue) queueViewModel.removeEpisodeFromQueue(episode) else queueViewModel.addEpisodeToQueue(episode) }, modifier = Modifier.size(48.dp), border = ButtonDefaults.outlinedButtonBorder) { Icon(queueIcon, queueAction) }
                                val downloadIcon = if (isDownloaded) Icons.Filled.DeleteOutline else Icons.Filled.Download
                                val downloadAction = if (isDownloaded) "Borrar Descarga" else "Descargar Episode"
                                OutlinedIconButton(
                                    onClick = {
                                        if (isDownloaded) {
                                            downloadedViewModel.deleteDownloadedEpisode(episode)
                                        } else {
                                            downloadedViewModel.downloadEpisode(episode) { result ->
                                                result.onSuccess {
                                                    mainViewModel.showTopNotification("Descarga completada", NotificationType.SUCCESS)
                                                }.onFailure {
                                                    mainViewModel.showTopNotification("Descarga fallida", NotificationType.FAILURE)
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(48.dp),
                                    border = ButtonDefaults.outlinedButtonBorder
                                ) { Icon(downloadIcon, downloadAction) }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            val displayDescription = episode.description.extractMeaningfulDescription()

                            if (displayDescription.isNotBlank()) {
                                Text(text = "Descripción", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onBackground)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = displayDescription, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                else -> {
                    ErrorView(message = "No se pudo cargar la información del Episode.", errorType = ErrorType.NO_RESULTS, onRetry = { episodeDetailViewModel.loadEpisodeDetails() }, modifier = Modifier.align(Alignment.Center))
                }
            }
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        start = 8.dp,
                        top = halfStatusBarPadding + 8.dp
                    )
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), CircleShape)
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.onSurface) }
        }
    }
}

/**
 * Formatea una fecha en formato ISO 8601 (GMT) a un formato legible en español.
 *
 * @param isoDate La fecha en formato ISO 8601 (GMT), que puede ser nula.
 * @return Una cadena con la fecha formateada (ej: "17 de junio de 2025") o "Fecha desconocida".
 */
private fun formatarFechaIso(isoDate: String?): String {
    if (isoDate.isNullOrBlank()) return "Fecha desconocida"
    val patterns = listOf("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss")
    val outputFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
    outputFormat.timeZone = TimeZone.getDefault()

    for (pattern in patterns) {
        try {
            val inputFormat = SimpleDateFormat(pattern, Locale.ROOT)
            inputFormat.timeZone = TimeZone.getTimeZone("GMT")
            val date = inputFormat.parse(isoDate)
            if (date != null) {
                return outputFormat.format(date)
            }
        } catch (_: Exception) {
            // Try next pattern
        }
    }
    return "Fecha no procesable"
}

/**
 * Un Composable auxiliar para mostrar una pieza de metadato con un icono y texto.
 *
 * @param icon El [ImageVector] que se mostrará a la izquierda del texto.
 * @param text El texto del metadato.
 */
@Composable
private fun MetaDataItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
