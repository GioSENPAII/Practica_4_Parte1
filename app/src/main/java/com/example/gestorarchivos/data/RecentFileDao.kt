package com.example.gestorarchivos.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface RecentFileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recentFile: RecentFile)

    @Update
    suspend fun update(recentFile: RecentFile)

    @Query("SELECT * FROM recent_files ORDER BY lastAccessed DESC LIMIT 20")
    fun getRecentFiles(): LiveData<List<RecentFile>>

    @Query("SELECT * FROM recent_files WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteFiles(): LiveData<List<RecentFile>>

    @Query("UPDATE recent_files SET isFavorite = :isFavorite WHERE path = :path")
    suspend fun updateFavorite(path: String, isFavorite: Boolean)

    @Query("DELETE FROM recent_files WHERE path = :path")
    suspend fun delete(path: String)

    @Query("DELETE FROM recent_files WHERE isFavorite = 0")
    suspend fun clearHistory()
}