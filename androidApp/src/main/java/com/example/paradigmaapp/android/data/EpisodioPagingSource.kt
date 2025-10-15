package com.example.paradigmaapp.android.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.paradigmaapp.model.Episode
import com.example.paradigmaapp.repository.Repository
import java.io.IOException

/**
 * PagingSource para cargar Episodes de un programa específico desde el EpisodeRepository.
 * Se encarga de la lógica de paginación, solicitando páginas a la API a medida que el usuario se desplaza.
 *
 * @param repository El repositorio desde donde se obtendrán los Episodes.
 * @param programaId El ID del programa para el cual se cargarán los Episodes.
 *
 * @author Mario Alguacil Juárez
 */
class EpisodePagingSource(
    private val repository: Repository,
    private val programaId: String
) : PagingSource<Int, Episode>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Episode> {
        val paginaActual = params.key ?: 1
        // Guardamos el tamaño de página solicitado para la comparación.
        val tamanoDePagina = params.loadSize

        return try {
            val result = repository.getProgramaDetail(programaId)
            result.fold(
                { failure ->
                    LoadResult.Error(Exception(failure.toString())) // Convert Failure to Exception
                },
                { programa ->
                    val episodes = programa.episodes.drop((paginaActual - 1) * tamanoDePagina).take(tamanoDePagina)

                    // Si el número de Episodes recibidos es menor que el que pedimos,
                    // significa que esta es la última página. En ese caso, nextKey es null.
                    val siguientePagina = if (episodes.size < tamanoDePagina) {
                        null
                    } else {
                        paginaActual + 1
                    }

                    LoadResult.Page(
                        data = episodes,
                        prevKey = if (paginaActual == 1) null else paginaActual - 1,
                        nextKey = siguientePagina
                    )
                }
            )
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Episode>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
