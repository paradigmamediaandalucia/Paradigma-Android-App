package com.example.paradigmaapp.cache.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.paradigmaapp.model.Episode

@Entity(tableName = "episodes")
data class EpisodeEntity(
    @PrimaryKey
    val episodeId: String,
    val title: String,
    val description: String,
    val downloadUrl: String,
    val imageUrl: String,
    val duration: Int,
    val publishedAt: String,
    val showId: String,
)

fun EpisodeEntity.toEpisode(): Episode {
    return Episode(
        id = episodeId,
        title = title,
        description = description,
        audioUrl = downloadUrl,
        downloadUrl = downloadUrl,
        imageUrl = imageUrl.takeIf { it.isNotBlank() },
        imageOriginalUrl = imageUrl.takeIf { it.isNotBlank() },
        duration = duration.toLong(),
        date = publishedAt,
        programId = showId,
        slug = "",
        content = null,
        excerpt = null,
        embedded = null,
        programaIds = null
    )
}

fun Episode.toEpisodeEntity(): EpisodeEntity {
    return EpisodeEntity(
        episodeId = id,
        title = title,
        description = description,
        downloadUrl = downloadUrl ?: audioUrl,
    imageUrl = imageUrl ?: imageOriginalUrl ?: "",
        duration = duration.toInt(),
        publishedAt = date ?: "",
        showId = programId
    )
}
