package com.example.paradigmaapp.cache

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.paradigmaapp.cache.dao.EpisodeDao
import com.example.paradigmaapp.cache.dao.ProgramaDao
import com.example.paradigmaapp.cache.entity.EpisodeEntity
import com.example.paradigmaapp.cache.entity.ProgramaEntity

@Database(entities = [EpisodeEntity::class, ProgramaEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun episodeDao(): EpisodeDao
    abstract fun programaDao(): ProgramaDao
}
