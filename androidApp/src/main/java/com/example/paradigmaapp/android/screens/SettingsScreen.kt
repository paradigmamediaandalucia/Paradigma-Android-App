package com.example.paradigmaapp.android.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.ui.HelpItem
import com.example.paradigmaapp.android.ui.ListDivider
import com.example.paradigmaapp.android.ui.SettingItemRow
import com.example.paradigmaapp.android.ui.ThemeSelectionButton
import com.example.paradigmaapp.android.viewmodel.SettingsViewModel

/**
 * Composable que renderiza la pantalla de Ajustes y la Guía de Ayuda de la aplicación.
 * Permite al usuario configurar preferencias y consultar una guía detallada.
 *
 * @param settingsViewModel El ViewModel que proporciona y gestiona el estado de los ajustes.
 * @param onBackClick Lambda que se invoca cuando el usuario pulsa el botón de retroceso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onClearQueue: () -> Unit,
    onClearDownloads: () -> Unit,
    onClearListeningHistory: () -> Unit,
    appVersionName: String
) {
    val uriHandler = LocalUriHandler.current
    val autoPlayStreamOnStart by settingsViewModel.autoPlayStreamOnStart.collectAsState()
    val isManuallySetToDarkTheme by settingsViewModel.isManuallySetToDarkTheme.collectAsState()
    val rememberEpisodeProgress by settingsViewModel.rememberEpisodeProgress.collectAsState()
    val autoPlayNextEpisode by settingsViewModel.autoPlayNextEpisode.collectAsState()
    val websiteUrl = settingsViewModel.mainWebsiteUrl
    val isProgramListMode by settingsViewModel.isProgramListMode.collectAsState()
    val aboutText = """
        Paradigma Media Andalucía (en adelante Paradigma) es una iniciativa ciudadana que surge de la necesidad de cubrir las carencias incuestionables que tiene la sociedad en general, y la cordobesa en particular, sobre la información que le afecta de primera mano.

        Paradigma tiene entidad sin ánimo de lucro. Todo lo recaudado por la Asociación será dirigido a conseguir los medios técnicos y humanos necesarios para mantener la mínima calidad exigible a un medio de comunicación en manos de la ciudadanía.

        Los contenidos de nuestros medios de comunicación serán de eminente carácter social. De hecho, servirán para dar voz a todos los colectivos sociales que quieran usarlos para dar a conocer sus problemáticas, sus luchas, sus denuncias, sus obstáculos, sus relaciones con las instituciones. Asimismo, se elaborarán contenidos en los que se explique de forma exhaustiva los procesos sociales, legales, laborales y, en general, políticos, que afectan de primera mano a la sociedad. También habrá programas de diversión, infantiles, de participación directa de la audiencia.

        Paradigma comienza en Córdoba, aunque nuestro proyecto ampara la colaboración y extensión por toda Andalucía. Paradigma constará de tres medios de comunicación:
           • Paradigma Radio, que emitirá tanto en FM como en streaming a través de internet, en directo.
           • Paradigma TV, que emitirá a través del canal de YouTube "Paradigma TV Andalucía".
           • Paradigma Prensa. Se trata de un periódico diario digital.

        Todas nuestras producciones se harán y distribuirán bajo licencia de Creative Commons.
    """.trimIndent()
    var isAboutExpanded by remember { mutableStateOf(false) }

    BackHandler(onBack = onBackClick)

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topContentPadding = statusBarPadding

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = topContentPadding,
                    bottom = 24.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección de Preferencias
            Text(text = "Preferencias", style = MaterialTheme.typography.titleLarge)
            SettingItemRow(
                title = "Mostrar programas en lista",
                description = "Alterna entre vista de cuadrícula y lista para los programas."
            ) {
                Switch(
                    checked = isProgramListMode,
                    onCheckedChange = { settingsViewModel.toggleProgramDisplayMode() }
                )
            }
            SettingItemRow(
                title = "Abrir con Radio en Directo",
                description = "Si está desactivado, la app abrirá con la radio en pausa."
            ) {
                Switch(
                    checked = autoPlayStreamOnStart,
                    onCheckedChange = { settingsViewModel.toggleAutoPlayStreamOnStart() }
                )
            }
            SettingItemRow(
                title = "Recordar progreso de escucha",
                description = "Guarda en qué punto dejaste cada episodio para retomarlo más tarde."
            ) {
                Switch(
                    checked = rememberEpisodeProgress,
                    onCheckedChange = { settingsViewModel.toggleRememberEpisodeProgress() }
                )
            }
            SettingItemRow(
                title = "Reproducir siguiente episodio automáticamente",
                description = "Al finalizar un episodio, comienza el siguiente disponible sin intervención."
            ) {
                Switch(
                    checked = autoPlayNextEpisode,
                    onCheckedChange = { settingsViewModel.toggleAutoPlayNextEpisode() }
                )
            }
            ListDivider()

            // Sección de Apariencia
            Text(text = "Apariencia", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeSelectionButton(
                    onClick = { settingsViewModel.setThemePreference(false) },
                    text = "Claro",
                    isSelected = isManuallySetToDarkTheme == false,
                    modifier = Modifier.weight(1f)
                )
                ThemeSelectionButton(
                    onClick = { settingsViewModel.setThemePreference(true) },
                    text = "Oscuro",
                    isSelected = isManuallySetToDarkTheme == true,
                    modifier = Modifier.weight(1f)
                )
                ThemeSelectionButton(
                    onClick = { settingsViewModel.setThemePreference(null) },
                    text = "Sistema",
                    isSelected = isManuallySetToDarkTheme == null,
                    modifier = Modifier.weight(1f)
                )
            }
            ListDivider()

            // --- SECCIONES DE AYUDA RESTAURADAS ---
            Text(text = "Ayuda y Funcionalidades", style = MaterialTheme.typography.titleLarge)
            HelpItem(
                icon = Icons.Default.TouchApp,
                title = "Ver detalles del episodio",
                description = "Mantén pulsado cualquier episodio en una lista para ver su pantalla de detalles completa."
            )
            HelpItem(
                icon = Icons.Default.OpenInFull,
                title = "Reproductor Ampliado",
                description = "Pulsa sobre la información del episodio en el reproductor inferior para abrir la vista a pantalla completa."
            )
            ListDivider()

            Text(text = "Controles del Reproductor", style = MaterialTheme.typography.titleLarge)
            HelpItem(
                icon = Icons.Default.PlayCircle,
                title = "Reproducir / Pausar",
                description = "El botón central grande inicia o detiene la reproducción del contenido actual."
            )
            HelpItem(
                icon = Icons.Default.Podcasts,
                title = "Radio en Directo",
                description = "El botón de la antena activa o desactiva la radio en directo. Se pondrá de color gris cuando esté activa."
            )
            ListDivider()

            Text(text = "Menú de Navegación", style = MaterialTheme.typography.titleLarge)
            HelpItem(
                icon = Icons.Default.Home,
                title = "Inicio",
                description = "Desde la pestaña 'Inicio' accedes al listado completo de contenidos disponibles."
            )
            HelpItem(icon = Icons.Default.Search, title = "Buscar", description = "Encuentra cualquier episodio por título o descripción.")
            HelpItem(icon = Icons.Default.History, title = "Continuar", description = "Aquí aparecen los episodios que has empezado a escuchar pero no has terminado.")
            HelpItem(icon = Icons.Default.Download, title = "Descargas", description = "Accede a los episodios guardados para escucharlos sin conexión.")
            HelpItem(icon = Icons.AutoMirrored.Filled.List, title = "Cola", description = "Organiza una lista de reproducción con los episodios que quieres escuchar a continuación.")
            HelpItem(icon = Icons.Default.Settings, title = "Ajustes", description = "Configura las preferencias de la aplicación y consulta esta ayuda.")
            ListDivider()

            Text(text = "Opciones adicionales para episodios", style = MaterialTheme.typography.titleLarge)
            HelpItem(icon = Icons.Default.Download, title = "Descargar episodio", description = "Desde los tres puntitos o la vista de detalle del episodio puedes descargarlo o eliminarlo de tu dispositivo.")
            HelpItem(icon = Icons.AutoMirrored.Filled.List, title = "Añadir a la cola", description = "Desde los tres puntitos o la vista de detalle del episodio puedes añadirlo o eliminarlo de la cola de reproducción.")
            ListDivider()


            Text(text = "Gestión de datos", style = MaterialTheme.typography.titleLarge)
            val dataActionButtonColors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFD54F),
                contentColor = Color.Black
            )
            SettingItemRow(
                title = "Vaciar cola de reproducción",
                description = "Elimina todos los episodios pendientes en la cola."
            ) {
                Button(onClick = onClearQueue, colors = dataActionButtonColors) {
                    Text("Vaciar")
                }
            }
            SettingItemRow(
                title = "Eliminar descargas",
                description = "Borra por completo los episodios guardados en el dispositivo."
            ) {
                Button(
                    onClick = onClearDownloads,
                    colors = dataActionButtonColors
                ) {
                    Text("Eliminar")
                }
            }
            SettingItemRow(
                title = "Limpiar historial de escucha",
                description = "Reinicia la lista de episodios en curso y sus progresos."
            ) {
                Button(onClick = onClearListeningHistory, colors = dataActionButtonColors) {
                    Text("Limpiar")
                }
            }
            ListDivider()


            // Sección de Más Información
            Text(text = "Más Información", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Versión de la aplicación: $appVersionName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { uriHandler.openUri(websiteUrl) },
                modifier = Modifier.fillMaxWidth(),
                colors = dataActionButtonColors
            ) {
                Text("Visitar web de Paradigma Media")
            }

            val aboutButtonColors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clickable { isAboutExpanded = !isAboutExpanded },
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sobre nosotros",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isAboutExpanded) Icons.Filled.OpenInFull else Icons.Filled.List,
                                contentDescription = if (isAboutExpanded) "Ocultar" else "Expandir",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        androidx.compose.animation.AnimatedVisibility(visible = isAboutExpanded) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = aboutText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Justify
                                )
                            }
                        }
                    }
                }

            Spacer(modifier = Modifier.height(166.dp))
        }
    }
}
