package com.example.gestorarchivos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RecentFile::class], version = 1, exportSchema = false)
abstract class FileDatabase : RoomDatabase() {

    abstract fun recentFileDao(): RecentFileDao

    companion object {
        @Volatile
        private var INSTANCE: FileDatabase? = null

        fun getDatabase(context: Context): FileDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FileDatabase::class.java,
                    "file_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}