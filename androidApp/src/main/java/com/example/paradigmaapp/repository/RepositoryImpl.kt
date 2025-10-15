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
import com.example.paradigmaapp.cache.AppDatabase // Assuming this will be the Room database
import com.example.paradigmaapp.cache.entity.toEpisode
import com.example.paradigmaapp.cache.entity.toEpisodeEntity
import kotlinx.coroutines.flow.first

class RepositoryImpl(private val httpClient: HttpClient, private val appDatabase: AppDatabase) : Repository {

    override suspend fun getProgramas(): Either<Failure, List<Programa>> = try {
        val programas = httpClient.get("${Config.SPREAKER_API_BASE_URL}users/${Config.SPREAKER_USER_ID}/shows").body<List<Programa>>()
        Right(programas)
    } catch (e: Exception) {
        handleException(e)
    }

    override suspend fun getProgramaDetail(programId: String): Either<Failure, Programa> = try {
        val programa = httpClient.get("${Config.SPREAKER_API_BASE_URL}users/${Config.SPREAKER_USER_ID}/shows/$programId").body<Programa>()
        Right(programa)
    } catch (e: Exception) {
        handleException(e)
    }

    override suspend fun getEpisodeDetail(episodeId: String): Either<Failure, Episode> = try {
        val episode = httpClient.get("${Config.SPREAKER_API_BASE_URL}episodes/$episodeId").body<Episode>()
        Right(episode)
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
        val episodes = httpClient.get("${Config.SPREAKER_API_BASE_URL}episodes/search") {
            parameter("q", query)
        }.body<List<Episode>>()
        Right(episodes)
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
