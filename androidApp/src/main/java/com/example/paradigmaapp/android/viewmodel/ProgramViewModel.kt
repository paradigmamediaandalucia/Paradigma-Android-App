package com.example.paradigmaapp.android.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.example.paradigmaapp.android.data.EpisodePagingSource
import com.example.paradigmaapp.model.Episode
import com.example.paradigmaapp.model.Programa
import com.example.paradigmaapp.model.stableListKey
import com.example.paradigmaapp.repository.Repository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de detalles de un Programa.
 * Sus responsabilidades son:
 * 1. Cargar los detalles del programa específico (imagen, descripción) para la cabecera.
 * 2. Proveer un flujo de datos paginados (`Flow<PagingData<Episode>>`) para la lista de Episodes,
 * utilizando la librería Jetpack Paging 3.
 *
 * @param repository Repositorio principal para obtener datos.
 * @param savedStateHandle Manejador para acceder a los argumentos de navegación.
 *
 * @author Mario Alguacil Juárez
 */
class ProgramaViewModel(
    private val repository: Repository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // --- ARGUMENTOS DE NAVEGACIÓN ---
    /** El ID del programa actual, obtenido de los argumentos de navegación. */
    val programaId: String = savedStateHandle.get<String>("programaId") ?: ""
    /** El nombre del programa actual, usado como fallback si los detalles no cargan. */
    val programaNombre: String = savedStateHandle.get<String>("programaNombre") ?: "Programa"

    // --- ESTADO PARA LA CABECERA ---
    /** StateFlow privado y mutable para los detalles del programa. */
    private val _programa = MutableStateFlow<Programa?>(null)
    /** StateFlow público e inmutable que la UI observará para la cabecera. */
    val programa: StateFlow<Programa?> = _programa.asStateFlow()

    // --- FLUJO DE DATOS PAGINADOS PARA LA LISTA ---
    /**
     * Flujo de datos paginados que la UI conectará a la LazyColumn.
     * `Pager` es el componente principal de Paging 3 que construye el flujo.
     */
    val EpisodesPaginados: Flow<PagingData<Episode>> = Pager(
        // Configuración de cómo se deben cargar las páginas.
        config = PagingConfig(
            pageSize = 20, // El número de elementos a cargar en cada página.
            enablePlaceholders = false
        ),
        // Le decimos a Paging que use nuestro EpisodePagingSource para obtener los datos.
        pagingSourceFactory = { EpisodePagingSource(repository, programaId) }
    )
        .flow
        .map { pagingData ->
            val seenEpisodeKeys = mutableSetOf<String>()
            pagingData.filter { episode ->
                val key = episode.stableListKey()
                seenEpisodeKeys.add(key)
            }
        }
        // Cachea los resultados en el ViewModelScope para que los datos sobrevivan a cambios
        // de configuración como la rotación de la pantalla, evitando recargas innecesarias.
        .cachedIn(viewModelScope)

    /**
     * El bloque init se ejecuta al crear el ViewModel y lanza la carga
     * de los detalles del programa para la cabecera.
     */
    init {
        loadProgramaDetails()
    }

    /**
     * Carga los detalles de un único programa de forma eficiente usando su ID.
     */
    private fun loadProgramaDetails() {
        // Lanzamos la corutina en el scope del ViewModel.
        viewModelScope.launch {
            repository.getProgramaDetail(programaId).fold(
                { /* Handle error */ },
                { programaDetails ->
                    _programa.value = programaDetails
                }
            )
        }
    }
}
