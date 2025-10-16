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
import com.example.paradigmaapp.model.EpisodeDetailResponse
import com.example.paradigmaapp.model.EpisodeResponse
import com.example.paradigmaapp.model.ProgramaResponse
import kotlinx.coroutines.flow.first

class RepositoryImpl(private val httpClient: HttpClient, private val appDatabase: AppDatabase) : Repository {

    companion object {
        private const val DEFAULT_EPISODE_PAGE_SIZE = 20
    }

    private fun HttpRequestBuilder.addSpreakerAuth() {
        header(HttpHeaders.Authorization, "Bearer ${Config.SPREAKER_API_TOKEN}")
    }

    override suspend fun getProgramas(): Either<Failure, List<Programa>> = try {
        val response = httpClient.get("${Config.SPREAKER_API_BASE_URL}users/${Config.SPREAKER_USER_ID}/shows") {
            addSpreakerAuth()
        }.body<ProgramaResponse>()
        Right(response.response.items)
    } catch (e: Exception) {
        handleException(e)
    }

    override suspend fun getProgramaDetail(programId: String): Either<Failure, Programa> = try {
        val programa = httpClient.get("${Config.SPREAKER_API_BASE_URL}users/${Config.SPREAKER_USER_ID}/shows/$programId") {
            addSpreakerAuth()
        }.body<Programa>()
        Right(programa)
    } catch (e: Exception) {
        handleException(e)
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
        val response = httpClient.get("${Config.SPREAKER_API_BASE_URL}search") {
            parameter("q", query)
            parameter("type", "episodes")
        }.body<EpisodeResponse>()
        Right(response.response.items)
    } catch (e: Exception) {
        handleException(e)
    }

    private fun handleException(e: Exception): Left<Failure> {
        return when (e) {
            is java.net.UnknownHostException -> Left(NetworkConnection)
            else -> Left(ServerError)
        }
    }
}
