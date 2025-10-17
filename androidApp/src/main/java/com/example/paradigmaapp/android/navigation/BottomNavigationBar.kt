package com.example.paradigmaapp.android.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.paradigmaapp.android.ui.BottomNavItem
import com.example.paradigmaapp.android.ui.navigateToScreenIfDifferent

/**
 * Composable que representa la barra de navegación inferior con comportamiento dinámico.
 * El ítem correspondiente a la pantalla activa se reemplaza por un botón "Inicio".
 *
 * @param navController El [NavHostController] para gestionar la navegación y saber la ruta actual.
 * @param modifier El [Modifier] que se aplicará al contenedor.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // La lista de todos los items que componen nuestra barra de navegación.
    val navItems = listOf(
        BottomNavItem.Search,
        BottomNavItem.OnGoing,
        BottomNavItem.Home, // Home is always in the center
        BottomNavItem.Downloads,
        BottomNavItem.Queue
    )

    // Observamos la pila de navegación para saber cuál es la ruta activa.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp
    ) {
        // Iteramos sobre cada uno de los items definidos.
        navItems.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navigateToScreenIfDifferent(navController, item.route)
                },
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
