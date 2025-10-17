package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R // Recursos de Android
import com.example.paradigmaapp.android.utils.dpToPreferredSquarePx
import com.example.paradigmaapp.android.utils.rememberCoilImageRequest
import com.example.paradigmaapp.android.utils.selectSpreakerImageSource
import com.example.paradigmaapp.android.utils.unescapeHtmlEntities // Utilidad para decodificar HTML
import com.example.paradigmaapp.model.Programa // Modelo de Programa del módulo shared

/**
 * Un Composable que representa un ítem individual de [Programa] en una lista o cuadrícula.
 * Muestra la imagen del programa y su título. Está diseñado para tener una altura
 * consistente en el área del título, ayudando a la alineación visual cuando se muestra en una cuadrícula.
 *
 * @param programa El objeto [Programa] cuyos datos se mostrarán.
 * @param onClicked Lambda que se invoca cuando el usuario hace clic en el ítem del programa.
 * @param modifier Modificador opcional para aplicar al [Card] principal.
 *
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class) // Necesario para Card en Material 3
@Composable
fun ProgramaListItem(
    programa: Programa,
    onClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isListMode: Boolean = false
) {
    Card(
        onClick = onClicked,
        modifier = modifier.fillMaxWidth(), // La tarjeta ocupa el ancho disponible en su celda/columna
        shape = RoundedCornerShape(12.dp), // Esquinas redondeadas para la tarjeta
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Elevación sutil
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        val density = LocalDensity.current.density
        val targetSize = if (isListMode) 72f else 200f
        val targetPx = remember(density, isListMode) { dpToPreferredSquarePx(targetSize, density) }
        val imageSource = remember(programa.imageUrl, programa.imageOriginalUrl, targetPx) {
            selectSpreakerImageSource(programa.imageUrl, programa.imageOriginalUrl, targetPx)
        }
        val imageRequest = rememberCoilImageRequest(
            primaryData = imageSource.preferred,
            fallbackData = imageSource.fallback,
            debugLabel = "program:${programa.id}"
        )

        if (isListMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = "Portada de ${programa.title.unescapeHtmlEntities()}",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.mipmap.logo_foreground),
                    placeholder = painterResource(R.mipmap.logo_foreground)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = programa.title.unescapeHtmlEntities(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(), // La columna interna ocupa toda la tarjeta
                horizontalAlignment = Alignment.CenterHorizontally // Centra la imagen y el texto horizontalmente
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = "Portada de ${programa.title.unescapeHtmlEntities()}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.mipmap.logo_foreground),
                    placeholder = painterResource(R.mipmap.logo_foreground)
                )

                Spacer(modifier = Modifier.height(8.dp)) // Espacio entre la imagen y el título

                // Contenedor para el título, con altura mínima para asegurar consistencia visual en cuadrículas.
                val typography = MaterialTheme.typography.titleSmall
                // Calcula la altura aproximada para dos líneas de texto del estilo titleSmall.
                val twoLinesHeight: Dp = with(LocalDensity.current) {
                    // (lineHeight de titleSmall) * 2 líneas.
                    // Se asume que typography.lineHeight está definido en sp y se convierte a Dp.
                    (typography.lineHeight * 2).toDp() //
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp) // Padding horizontal para el texto del título
                        .padding(bottom = 8.dp) // Padding inferior para el texto del título
                        .heightIn(min = twoLinesHeight), // Altura mínima para el área del título
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center // Centra el texto verticalmente si ocupa menos de la altura mínima
                ) {
                    Text(
                        text = programa.title.unescapeHtmlEntities(), // Nombre del programa decodificado
                        style = typography,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center, // Texto del título centrado
                        maxLines = 2, // Máximo dos líneas para el título
                        overflow = TextOverflow.Visible
                    )
                }
            }
        }
    }
}
