package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LoaderScriptDao {
    @Query("SELECT * FROM loader_scripts ORDER BY timestamp DESC")
    fun getAllScripts(): Flow<List<LoaderScript>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: LoaderScript)

    @Delete
    suspend fun deleteScript(script: LoaderScript)

    @Query("DELETE FROM loader_scripts WHERE id = :id")
    suspend fun deleteScriptById(id: Int)
}
