package com.example.paradigmaapp.cache.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.paradigmaapp.model.Episode
import com.example.paradigmaapp.model.Programa

@Entity(tableName = "programas")
data class ProgramaEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val episodes: List<Episode> // This will need a TypeConverter
)

fun ProgramaEntity.toPrograma(): Programa {
    return Programa(
        id = id,
        title = title,
        description = description,
        imageUrl = imageUrl,
        episodes = episodes
    )
}

fun Programa.toProgramaEntity(): ProgramaEntity {
    return ProgramaEntity(
        id = id,
        title = title,
        description = description,
        imageUrl = imageUrl,
        episodes = episodes
    )
}
