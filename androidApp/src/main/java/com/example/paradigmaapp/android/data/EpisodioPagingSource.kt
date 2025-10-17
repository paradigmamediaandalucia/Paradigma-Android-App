package com.example.paradigmaapp.android.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.paradigmaapp.exception.Either
import com.example.paradigmaapp.model.Episode
import com.example.paradigmaapp.model.stableListKey
import com.example.paradigmaapp.repository.Repository
import java.io.IOException

/**
 * PagingSource para cargar Episodes de un programa específico desde el EpisodeRepository.
 * Se encarga de la lógica de paginación, solicitando páginas a la API a medida que el usuario se desplaza.
 */
class EpisodePagingSource(
    private val repository: Repository,
    private val programaId: String
) : PagingSource<Int, Episode>() {

    companion object {
        private const val API_PAGE_LIMIT = 999
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Episode> {
        return try {
            when (params) {
                is LoadParams.Append, is LoadParams.Prepend -> {
                    LoadResult.Page(
                        data = emptyList(),
                        prevKey = null,
                        nextKey = null
                    )
                }
                is LoadParams.Refresh -> {
                    val episodes = mutableListOf<Episode>()
                    val seenKeys = mutableSetOf<String>()
                    var currentOffset = 0

                    while (true) {
                        when (val result = repository.getEpisodes(programaId, currentOffset, API_PAGE_LIMIT)) {
                            is Either.Left -> return LoadResult.Error(Exception(result.a.toString()))
                            is Either.Right -> {
                                val fetched = result.b
                                if (fetched.isEmpty()) {
                                    break
                                }

                                val uniqueFetched = fetched.filter { episode ->
                                    val key = episode.stableListKey()
                                    seenKeys.add(key)
                                }

                                if (uniqueFetched.isEmpty()) {
                                    break
                                }

                                episodes += uniqueFetched
                                currentOffset += fetched.size

                                if (fetched.size < API_PAGE_LIMIT) {
                                    break
                                }
                            }
                        }
                    }

                    LoadResult.Page(
                        data = episodes,
                        prevKey = null,
                        nextKey = null
                    )
                }
            }
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Episode>): Int? {
        return 0
    }
}
