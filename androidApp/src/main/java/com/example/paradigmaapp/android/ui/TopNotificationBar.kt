package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.viewmodel.NotificationType

/**
 * Un banner de notificación que se muestra en la parte superior.
 *
 * @param message El mensaje a mostrar.
 * @param type El tipo de notificación (SUCCESS o FAILURE) para determinar el color y el icono.
 */
@Composable
fun TopNotificationBanner(
    message: String,
    type: NotificationType
) {
    val backgroundColor = when (type) {
        NotificationType.SUCCESS -> Color(0xFF388E3C) // Un verde oscuro
        NotificationType.FAILURE -> MaterialTheme.colorScheme.error
    }
    val icon = when (type) {
        NotificationType.SUCCESS -> Icons.Default.CheckCircle
        NotificationType.FAILURE -> Icons.Default.Error
    }
    val contentColor = Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor, contentColor = contentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                imageVector = icon,
                contentDescription = type.name
            )
        }
    }
}
