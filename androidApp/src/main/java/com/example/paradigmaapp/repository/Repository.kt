package com.example.paradigmaapp.repository

import com.example.paradigmaapp.exception.Either
import com.example.paradigmaapp.exception.Failure
import com.example.paradigmaapp.model.Episode
import com.example.paradigmaapp.model.Programa

interface Repository {
    suspend fun getProgramas(): Either<Failure, List<Programa>>
    suspend fun getProgramaDetail(programId: String): Either<Failure, Programa>
    suspend fun getEpisodes(programId: String, offset: Int, limit: Int): Either<Failure, List<Episode>>
    suspend fun getEpisodeDetail(episodeId: String): Either<Failure, Episode>
    suspend fun saveEpisode(episode: Episode)
    suspend fun getSavedEpisodes(): Either<Failure, List<Episode>>
    suspend fun deleteEpisode(episodeId: String)
    suspend fun searchEpisodes(query: String): Either<Failure, List<Episode>>
    suspend fun getEpisodeFromCache(episodeId: String): Episode?
    suspend fun getLatestEpisodeFromCache(programId: String): Episode?
}
