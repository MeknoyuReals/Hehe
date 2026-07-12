package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loader_scripts")
data class LoaderScript(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val scriptIdOrUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)
