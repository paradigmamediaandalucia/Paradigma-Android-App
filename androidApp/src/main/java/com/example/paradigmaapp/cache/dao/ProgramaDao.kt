package com.example.paradigmaapp.cache.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.paradigmaapp.cache.entity.ProgramaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograma(programa: ProgramaEntity)

    @Query("SELECT * FROM programas WHERE id = :programId")
    fun getProgramaById(programId: String): Flow<ProgramaEntity?>

    @Query("SELECT * FROM programas")
    fun getAllProgramas(): Flow<List<ProgramaEntity>>

    @Query("DELETE FROM programas WHERE id = :programId")
    suspend fun deleteProgramaById(programId: String)
}
