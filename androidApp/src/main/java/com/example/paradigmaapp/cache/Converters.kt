package com.example.paradigmaapp.cache

import androidx.room.TypeConverter
import com.example.paradigmaapp.model.Embedded
import com.example.paradigmaapp.model.Episode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromEpisodeList(value: List<Episode>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toEpisodeList(value: String): List<Episode> {
        return Json.decodeFromString(value)
    }

    @TypeConverter
    fun fromEmbedded(value: Embedded?): String? {
        return value?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toEmbedded(value: String?): Embedded? {
        return value?.let { Json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromListOfStrings(value: List<String>?): String? {
        return value?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toListOfStrings(value: String?): List<String>? {
        return value?.let { Json.decodeFromString(it) }
    }
}
