package com.example.gestorarchivos.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "recent_files")
data class RecentFile(
    @PrimaryKey
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val lastAccessed: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val extension: String = ""
)