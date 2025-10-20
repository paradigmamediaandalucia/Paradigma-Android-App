package com.example.paradigmaapp.android.inicio

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.R
import com.example.paradigmaapp.android.ui.InfoRow

/**
 * Diapositiva 1: Bienvenida con animación e imagen.
 */
@Composable
fun OnboardingPage1() {
    // Animación infinita para un efecto sutil de "pulso" en la imagen.
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
        ), label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        /*Text(
            text = "Bienvenid@ a",
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            color = Color(0xFF000000)
        )
        Spacer(Modifier.height(24.dp))*/
        Image(
            painter = painterResource(id = R.drawable.banner),
            contentDescription = "Logo de Paradigma Media",
            modifier = Modifier
                .fillMaxWidth(0.8f) // Ocupa el 80% del ancho
        )
        Spacer(Modifier.height(64.dp))

    }
}
