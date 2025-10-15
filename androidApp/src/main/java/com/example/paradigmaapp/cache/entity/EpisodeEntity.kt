package com.example.paradigmaapp.cache.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.paradigmaapp.model.Embedded
import com.example.paradigmaapp.model.Episode

@Entity(tableName = "episodes")
data class EpisodeEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val imageUrl: String,
    val duration: Long,
    val date: String,
    val programId: String,
    val slug: String,
    val content: String?,
    val excerpt: String?,
    val embedded: Embedded?,
    val programaIds: List<String>?
)

fun EpisodeEntity.toEpisode(): Episode {
    return Episode(
        id = id,
        title = title,
        description = description,
        audioUrl = audioUrl,
        imageUrl = imageUrl,
        duration = duration,
        date = date,
        programId = programId,
        slug = slug,
        content = content,
        excerpt = excerpt,
        embedded = embedded,
        programaIds = programaIds
    )
}

fun Episode.toEpisodeEntity(): EpisodeEntity {
    return EpisodeEntity(
        id = id,
        title = title,
        description = description,
        audioUrl = audioUrl,
        imageUrl = imageUrl,
        duration = duration,
        date = date,
        programId = programId,
        slug = slug,
        content = content,
        excerpt = excerpt,
        embedded = embedded,
        programaIds = programaIds
    )
}
