package com.example.paradigmaapp.repository

import com.example.paradigmaapp.exception.Either
import com.example.paradigmaapp.exception.Failure
import com.example.paradigmaapp.model.Episode
import com.example.paradigmaapp.model.Programa
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import com.example.paradigmaapp.config.Config
import com.example.paradigmaapp.exception.Either.Left
import com.example.paradigmaapp.exception.Either.Right
import com.example.paradigmaapp.exception.Failure.NetworkConnection
import com.example.paradigmaapp.exception.Failure.ServerError
import com.example.paradigmaapp.cache.AppDatabase
import com.example.paradigmaapp.cache.entity.toEpisode
import com.example.paradigmaapp.cache.entity.toEpisodeEntity
import com.example.paradigmaapp.cache.entity.toPrograma
import com.example.paradigmaapp.cache.entity.toProgramaEntity
import com.example.paradigmaapp.model.EpisodeDetailResponse
import com.example.paradigmaapp.model.EpisodeResponse
import com.example.paradigmaapp.model.ProgramaDetailResponse
import com.example.paradigmaapp.model.ProgramaResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.util.Collections

class RepositoryImpl(private val httpClient: HttpClient, private val appDatabase: AppDatabase) : Repository {

    private val programaIdsCache = Collections.synchronizedSet(mutableSetOf<String>())

    private fun HttpRequestBuilder.addSpreakerAuth() {
        header(HttpHeaders.Authorization, "Bearer ${Config.SPREAKER_API_TOKEN}")
    }

    override suspend fun getProgramas(): Either<Failure, List<Programa>> = try {
        val response = httpClient.get("${Config.SPREAKER_API_BASE_URL}users/${Config.SPREAKER_USER_ID}/shows") {
            addSpreakerAuth()
        }.body<ProgramaResponse>()
        val programas = response.response.items
        storeProgramas(programas, replaceCache = true)
        Right(programas)
    } catch (e: Exception) {
        val cachedProgramas = appDatabase.programaDao().getAllProgramas().first().map { it.toPrograma() }
        if (cachedProgramas.isNotEmpty()) {
            storeProgramas(cachedProgramas, replaceCache = true)
            Right(cachedProgramas)
        } else {
            handleException(e)
        }
    }

    override suspend fun getProgramaDetail(programId: String): Either<Failure, Programa> = try {
        val programa = httpClient.get("${Config.SPREAKER_API_BASE_URL}shows/$programId") {
            addSpreakerAuth()
        }.body<ProgramaDetailResponse>().response.show
        storeProgramas(listOf(programa), replaceCache = false)
        Right(programa)
    } catch (e: Exception) {
        val cachedPrograma = appDatabase.programaDao().getProgramaById(programId).first()?.toPrograma()
        if (cachedPrograma != null) {
            storeProgramas(listOf(cachedPrograma), replaceCache = false)
            Right(cachedPrograma)
        } else {
            handleException(e)
        }
    }

    override suspend fun getEpisodes(programId: String, offset: Int, limit: Int): Either<Failure, List<Episode>> = try {
        val safeOffset = offset.coerceAtLeast(0)
        val safeLimit = limit.takeIf { it > 0 } ?: DEFAULT_EPISODE_PAGE_SIZE

        val response = httpClient.get("${Config.SPREAKER_API_BASE_URL}shows/$programId/episodes") {
            addSpreakerAuth()
            parameter("offset", safeOffset)
            parameter("limit", safeLimit)
        }.body<EpisodeResponse>()
        Right(response.response.items)
    } catch (e: Exception) {
        handleException(e)
    }

    override suspend fun getEpisodeDetail(episodeId: String): Either<Failure, Episode> = try {
        val response = httpClient.get("${Config.SPREAKER_API_BASE_URL}episodes/$episodeId") {
            addSpreakerAuth()
        }.body<EpisodeDetailResponse>()
        Right(response.response.episode)
    } catch (e: Exception) {
        handleException(e)
    }

    override suspend fun saveEpisode(episode: Episode) {
        appDatabase.episodeDao().insertEpisode(episode.toEpisodeEntity())
    }

    override suspend fun getSavedEpisodes(): Either<Failure, List<Episode>> {
        return try {
            val episodes = appDatabase.episodeDao().getAllEpisodes().first().map { it.toEpisode() }
            Right(episodes)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    override suspend fun deleteEpisode(episodeId: String) {
        appDatabase.episodeDao().deleteEpisodeById(episodeId)
    }

    override suspend fun searchEpisodes(query: String): Either<Failure, List<Episode>> = try {
        ensureProgramaIdsCache()
        val normalizedQuery = normalizeForSearch(query)

        val response = httpClient.get("${Config.SPREAKER_API_BASE_URL}search") {
            parameter("q", query)
            parameter("type", "episodes")
            parameter("user_id", Config.SPREAKER_USER_ID)
        }.body<EpisodeResponse>()
        val allowedIds = synchronized(programaIdsCache) { programaIdsCache.toSet() }

        val apiEpisodes = response.response.items.filter { episode ->
            val programId = episode.programId.ifBlank { episode.show?.id ?: "" }
            allowedIds.isEmpty() || allowedIds.contains(programId)
        }

        val rankedApiEpisodes = apiEpisodes.sortedByDescending { episodeSearchScore(normalizedQuery, it) }
        val topScore = rankedApiEpisodes.firstOrNull()?.let { episodeSearchScore(normalizedQuery, it) } ?: 0.0

        val needFallback = normalizedQuery.isNotEmpty() && (rankedApiEpisodes.isEmpty() || topScore < TOP_RESULT_MIN_SCORE)
        val similarityFallback = if (needFallback) {
            findEpisodesBySimilarity(normalizedQuery, allowedIds)
        } else {
            emptyList()
        }

        val combined = (rankedApiEpisodes + similarityFallback)
            .distinctBy { it.id }
            .sortedByDescending { episodeSearchScore(normalizedQuery, it) }
            .take(MAX_SEARCH_RESULTS)

        Right(combined)
    } catch (e: Exception) {
        handleException(e)
    }

    private suspend fun findEpisodesBySimilarity(
        normalizedQuery: String,
        allowedIds: Set<String>
    ): List<Episode> {
        val storedProgramas = appDatabase.programaDao().getAllProgramas().first().map { it.toPrograma() }
        if (storedProgramas.isEmpty()) return emptyList()

        val candidatePrograms = storedProgramas
            .map { programa -> programa to searchSimilarityScore(normalizedQuery, programa.title) }
            .filter { (_, score) -> score >= PROGRAM_NAME_SCORE_THRESHOLD }
            .sortedByDescending { it.second }
            .take(MAX_PROGRAM_CANDIDATES)

        if (candidatePrograms.isEmpty()) return emptyList()

        val fallbackEpisodes = mutableListOf<Episode>()
        for ((programa, _) in candidatePrograms) {
            when (val episodesResult = getEpisodes(programa.id, 0, DEFAULT_EPISODE_PAGE_SIZE)) {
                is Left -> Unit
                is Right -> {
                    val episodes = episodesResult.b.filter { episode ->
                        val programId = episode.programId.ifBlank { episode.show?.id ?: "" }
                        allowedIds.isEmpty() || allowedIds.contains(programId)
                    }
                    fallbackEpisodes += episodes
                }
            }
        }

        return fallbackEpisodes
    }

    private fun episodeSearchScore(normalizedQuery: String, episode: Episode): Double {
        if (normalizedQuery.isEmpty()) return 0.0
        val titleScore = searchSimilarityScore(normalizedQuery, episode.title)
        val showTitleScore = episode.show?.let { searchSimilarityScore(normalizedQuery, it.title) } ?: 0.0
        val descriptionScore = searchSimilarityScore(normalizedQuery, episode.description)
        return maxOf(titleScore, showTitleScore, descriptionScore)
    }

    companion object {
        private const val DEFAULT_EPISODE_PAGE_SIZE = 20
        private const val MAX_SEARCH_RESULTS = 60
        private const val MAX_PROGRAM_CANDIDATES = 4
        private const val PROGRAM_NAME_SCORE_THRESHOLD = 0.55
        private const val TOP_RESULT_MIN_SCORE = 0.65
    }

    private fun handleException(e: Exception): Left<Failure> {
        return when (e) {
            is java.net.UnknownHostException -> Left(NetworkConnection)
            else -> Left(ServerError)
        }
    }

    private suspend fun storeProgramas(programas: List<Programa>, replaceCache: Boolean) {
        if (programas.isEmpty()) return
        val programaDao = appDatabase.programaDao()
        programas.forEach { programa ->
            programaDao.insertPrograma(programa.toProgramaEntity())
        }
        synchronized(programaIdsCache) {
            if (replaceCache) {
                programaIdsCache.clear()
            }
            programaIdsCache.addAll(programas.map { it.id })
        }
    }

    private suspend fun ensureProgramaIdsCache() {
        if (programaIdsCache.isNotEmpty()) return
        val cachedProgramas = appDatabase.programaDao().getAllProgramas().first()
        if (cachedProgramas.isNotEmpty()) {
            storeProgramas(cachedProgramas.map { it.toPrograma() }, replaceCache = true)
        } else {
            when (val result = getProgramas()) {
                is Either.Right -> {
                    // cache already populated inside getProgramas
                }
                else -> {
                    // noop, let caller handle empty cache scenario
                }
            }
        }
    }

    override suspend fun getEpisodeFromCache(episodeId: String): Episode? {
        return try {
            appDatabase.episodeDao().getEpisodeById(episodeId).firstOrNull()?.toEpisode()
        } catch (_: Exception) {
            null
        }
    }
}
