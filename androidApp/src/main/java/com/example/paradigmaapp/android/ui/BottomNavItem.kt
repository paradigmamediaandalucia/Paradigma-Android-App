package com.example.paradigmaapp.android.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.paradigmaapp.android.R
import com.example.paradigmaapp.android.navigation.Screen

/**
 * Define de forma segura y tipada cada uno de los destinos principales
 * de la barra de navegación inferior.
 * Al ser una 'sealed class', nos aseguramos de que solo pueden existir los objetos
 * definidos aquí dentro, lo que hace el código más robusto.
 *
 * @property route La ruta de navegación asociada al item (de la clase Screen).
 * @property titleRes El recurso de texto que se mostrará como etiqueta del item.
 * @property icon El icono que representará al item en la barra.
 *
 * @author Mario Alguacil Juárez
 */
sealed class BottomNavItem(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    /** Representa la pestaña de Inicio, mostrando la selección de programas. */
    data object Home : BottomNavItem(Screen.Home.route, R.string.nav_home, Icons.Default.Home)
    /** Representa la pestaña de Búsqueda. */
    data object Search : BottomNavItem(Screen.Search.route, R.string.nav_search, Icons.Default.Search)
    /** Representa la pestaña de "Seguir Escuchando". */
    data object OnGoing : BottomNavItem(Screen.OnGoing.route, R.string.nav_continue, Icons.Default.AccessTimeFilled)
    /** Representa la pestaña de Descargas. */
    data object Downloads : BottomNavItem(Screen.Downloads.route, R.string.nav_downloads, Icons.Default.Download)
    /** Representa la pestaña de la Cola de reproducción. */
    data object Queue : BottomNavItem(Screen.Queue.route, R.string.nav_queue, Icons.AutoMirrored.Filled.List)
}
