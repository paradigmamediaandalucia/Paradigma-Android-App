package com.example.paradigmaapp.cache.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.paradigmaapp.model.Programa

@Entity(tableName = "programas")
data class ProgramaEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val imageOriginalUrl: String?
)

fun ProgramaEntity.toPrograma(): Programa {
    return Programa(
        id = this.id,
        title = this.title,
        description = this.description,
        imageUrl = this.imageUrl,
        imageOriginalUrl = this.imageOriginalUrl
    )
}

fun Programa.toProgramaEntity(): ProgramaEntity {
    return ProgramaEntity(
        id = this.id,
        title = this.title,
        description = this.description,
        imageUrl = this.imageUrl,
        imageOriginalUrl = this.imageOriginalUrl
    )
}
