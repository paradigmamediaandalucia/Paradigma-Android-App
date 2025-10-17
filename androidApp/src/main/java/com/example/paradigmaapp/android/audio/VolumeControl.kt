package com.example.paradigmaapp.android.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.R
import com.example.paradigmaapp.android.viewmodel.VolumeControlViewModel

/**
 * Composable que proporciona una interfaz de usuario para ajustar el volumen del audio.
 * El estado del volumen es gestionado por un ViewModel dedicado.
 *
 * @param volumeControlViewModel El ViewModel que maneja la lógica del volumen.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun VolumeControl(
    volumeControlViewModel: VolumeControlViewModel
) {
    // El Composable observa el estado del volumen desde el ViewModel.
    val currentVolume by volumeControlViewModel.volume.collectAsState()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.mipmap.volume_down),
                contentDescription = "Volumen Bajo",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = currentVolume,
                onValueChange = { newVolume ->
                    // Llama a la función del ViewModel para guardar el valor.
                    volumeControlViewModel.setAndSaveVolume(newVolume)
                },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.mipmap.volume),
                contentDescription = "Volumen Alto",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}
