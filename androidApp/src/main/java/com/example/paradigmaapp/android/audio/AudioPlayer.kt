package com.example.paradigmaapp.android.audio

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R
import com.example.paradigmaapp.android.viewmodel.VolumeControlViewModel
import com.example.paradigmaapp.model.Episode
import com.example.paradigmaapp.model.RadioInfo

/**
 * Composable que representa la interfaz de usuario del reproductor de audio compacto global de la aplicación.
 * Muestra información sobre el Episode actual o el streaming en vivo, y proporciona controles de reproducción.
 *
 * @param activePlayer El reproductor [Player] de Media3 activo. Puede ser nulo.
 * @param currentEpisode El [Episode] que se está reproduciendo, o `null` si es el stream en vivo.
 * @param andainaRadioInfo Objeto [RadioInfo] con los metadatos del stream en vivo. Puede ser nulo.
 * @param isPlayingGeneral Booleano que indica si algo (Episode o stream) se está reproduciendo.
 * @param episodeProgress Progreso de la reproducción del Episode (un valor flotante entre 0.0 y 1.0).
 * @param onProgressChange Lambda que se invoca cuando el usuario termina de arrastrar la barra de progreso.
 * @param isAndainaStreamActive Indica si el modo de streaming de Andaina está activo.
 * @param isAndainaPlaying Indica si el stream de Andaina se está reproduciendo actualmente.
 * @param onPlayPauseClick Lambda para la acción de reproducir/pausar el contenido actual.
 * @param onPlayStreamingClick Lambda para la acción de (des)activar el modo de streaming.
 * @param onEpisodeInfoClick Lambda que se invoca al hacer clic en la información del Episode para ver sus detalles.
 * @param onVolumeIconClick Lambda para la acción de mostrar el control de volumen.
 * @param volumeControlViewModel El ViewModel que maneja la lógica del volumen.
 * @param modifier Modificador opcional para aplicar al [Card] principal.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun AudioPlayer(
    activePlayer: Player?,
    currentEpisode: Episode?,
    andainaRadioInfo: RadioInfo?,
    isPlayingGeneral: Boolean,
    episodeProgress: Float,
    onProgressChange: (Float) -> Unit,
    isAndainaStreamActive: Boolean,
    isAndainaPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onPlayStreamingClick: () -> Unit,
    onEpisodeInfoClick: (Episode) -> Unit,
    onVolumeIconClick: () -> Unit,
    volumeControlViewModel: VolumeControlViewModel,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(episodeProgress) }

    val currentVolume by volumeControlViewModel.volume.collectAsState()

    LaunchedEffect(currentVolume, activePlayer) {
        activePlayer?.volume = currentVolume
    }

    LaunchedEffect(episodeProgress) {
        if (!isDragging) {
            dragPosition = episodeProgress
        }
    }

    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val isEffectivelyLiveStream = currentEpisode == null

    val (displayText, displayImageUrl) = if (isEffectivelyLiveStream) {
        val streamImageUrl = andainaRadioInfo?.art
        val streamDisplayText = if (isAndainaPlaying || isAndainaStreamActive) {
            "Directo 90.2 FM"
        } else {
            "Sin emisión en directo"
        }
        streamDisplayText to streamImageUrl
    } else {
        currentEpisode?.title to currentEpisode?.imageUrl
    } as Pair<String?, String?>

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .animateContentSize()
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = currentEpisode != null) { currentEpisode?.let(onEpisodeInfoClick) }
                        .padding(vertical = 4.dp)
                ) {
                    AsyncImage(
                        model = displayImageUrl,
                        contentDescription = "Portada de la emisión actual",
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.mipmap.logo_square),
                        placeholder = painterResource(R.mipmap.logo_square)
                    )
                    Text(
                        text = displayText ?: "Cargando...",
                        style = MaterialTheme.typography.titleSmall,
                        color = onPrimaryColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPlayPauseClick) {
                        Icon(
                            painter = painterResource(if (isPlayingGeneral) R.mipmap.pause else R.mipmap.play),
                            contentDescription = if (isPlayingGeneral) "Pausar" else "Reproducir",
                            modifier = Modifier.size(36.dp),
                            tint = onPrimaryColor
                        )
                    }
                    IconButton(onClick = onVolumeIconClick) {
                        Icon(painterResource(R.mipmap.volume), "Control de Volumen", Modifier.size(30.dp), tint = onPrimaryColor)
                    }
                    IconButton(onClick = onPlayStreamingClick) {
                        Icon(painterResource(R.mipmap.streaming), "Radio en Directo", Modifier.size(30.dp), tint = if (isAndainaStreamActive) secondaryColor else onPrimaryColor)
                    }
                }
            }

            AudioProgressBar(
                isLiveStream = isEffectivelyLiveStream,
                progress = episodeProgress,
                dragPosition = dragPosition,
                onDragChange = { newPosition ->
                    isDragging = true
                    dragPosition = newPosition
                },
                onDragEnd = {
                    isDragging = false
                    onProgressChange(dragPosition)
                }
            )
        }
    }
}
