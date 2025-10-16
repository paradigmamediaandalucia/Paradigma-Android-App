package com.example.paradigmaapp.android.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.paradigmaapp.model.Episode
import com.example.paradigmaapp.model.stableListKey
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

    private val seenEpisodeKeys = mutableSetOf<String>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Episode> {
        if (params is LoadParams.Refresh) {
            seenEpisodeKeys.clear()
        }
        val offset = params.key ?: 0
        val tamanoDePagina = params.loadSize

        return try {
            val result = repository.getEpisodes(programaId, offset, tamanoDePagina)
            result.fold(
                { failure ->
                    LoadResult.Error(Exception(failure.toString())) // Convert Failure to Exception
                },
                { episodes ->
                    val uniqueEpisodes = episodes.filter { episode ->
                        val key = episode.stableListKey()
                        if (seenEpisodeKeys.contains(key)) {
                            false
                        } else {
                            seenEpisodeKeys.add(key)
                            true
                        }
                    }

                    if (uniqueEpisodes.isEmpty()) {
                        return@fold LoadResult.Page(
                            data = emptyList(),
                            prevKey = if (offset == 0) null else (offset - tamanoDePagina).coerceAtLeast(0),
                            nextKey = null
                        )
                    }

                    val nextOffset = if (episodes.size < tamanoDePagina) null else offset + episodes.size
                    val prevKey = if (offset == 0) null else (offset - tamanoDePagina).coerceAtLeast(0)

                    LoadResult.Page(
                        data = uniqueEpisodes,
                        prevKey = prevKey,
                        nextKey = nextOffset
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
            val closestPage = state.closestPageToPosition(anchorPosition)
            closestPage?.prevKey?.let { it + closestPage.data.size }
                ?: closestPage?.nextKey?.let { it - closestPage.data.size }
        }?.coerceAtLeast(0)
    }
}
