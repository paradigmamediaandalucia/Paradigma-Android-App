package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.ui.ErrorType
import com.example.paradigmaapp.android.ui.ErrorView
import com.example.paradigmaapp.android.ui.LayoutConstants
import com.example.paradigmaapp.android.ui.ProgramaListItem
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.SettingsViewModel
import com.example.paradigmaapp.model.Programa

/**
 * Pantalla principal de la aplicación que muestra una cuadrícula de [Programa]s disponibles.
 * Gestiona los estados de carga, error y contenido vacío.
 *
 * @param mainViewModel El [MainViewModel] que proporciona la lista de programas y su estado de carga/error.
 * @param settingsViewModel El [SettingsViewModel] que expone las preferencias de visualización.
 * @param onProgramaSelected Lambda que se invoca cuando el usuario selecciona un programa de la lista.
 * Recibe el ID y el nombre del programa seleccionado.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    onProgramaSelected: (programaId: String, programaNombre: String) -> Unit
) {
    // Observa los estados del MainViewModel
    val programas: List<Programa> by mainViewModel.programas.collectAsState()
    val isLoadingProgramas: Boolean by mainViewModel.isLoadingProgramas.collectAsState()
    val programasError: String? by mainViewModel.programasError.collectAsState()

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topContentPadding = statusBarPadding + LayoutConstants.topActionPadding
    val isProgramListMode by settingsViewModel.isProgramListMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(modifier = Modifier.height(topContentPadding))
        when {
            // Estado de carga: Muestra un indicador de progreso si se están cargando y no hay programas aún.
            isLoadingProgramas && programas.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            // Estado de error: Muestra el componente ErrorView.
            programasError != null -> {
                val errorType = if (programasError!!.contains("internet", ignoreCase = true) ||
                    programasError!!.contains("conectar", ignoreCase = true)) {
                    ErrorType.NO_INTERNET
                } else {
                    ErrorType.GENERAL_SERVER_ERROR
                }
                ErrorView(
                    message = programasError!!,
                    errorType = errorType,
                    onRetry = { mainViewModel.loadInitialProgramas() } // Acción para reintentar la carga
                )
            }
            // Estado de contenido vacío (sin error): Muestra un mensaje indicando que no hay programas.
            programas.isEmpty() -> {
                ErrorView(
                    message = "No hay programas disponibles en este momento.",
                    errorType = ErrorType.NO_RESULTS,
                    onRetry = { mainViewModel.loadInitialProgramas() }
                )
            }
            // Estado con contenido: Muestra la cuadrícula o lista de programas.
            else -> {
                if (isProgramListMode) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 16.dp,
                            end = 16.dp,
                            bottom = LayoutConstants.bottomContentPadding + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        lazyItems(programas, key = { programa -> programa.id }) { programa ->
                            ProgramaListItem(
                                programa = programa,
                                onClicked = {
                                    onProgramaSelected(programa.id, programa.title)
                                },
                                isListMode = true
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 16.dp,
                            end = 16.dp,
                            bottom = LayoutConstants.bottomContentPadding + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = programas, key = { programa -> programa.id }) { programa ->
                            ProgramaListItem(
                                programa = programa,
                                onClicked = {
                                    onProgramaSelected(programa.id, programa.title)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
