package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.TranslationRecord

@Database(
    entities = [TranslationRecord::class],
    version = 3,
    exportSchema = false
)
abstract class GameLingoDatabase : RoomDatabase() {
    abstract fun translationDao(): TranslationDao

    companion object {
        @Volatile
        private var INSTANCE: GameLingoDatabase? = null

        fun getDatabase(context: Context): GameLingoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameLingoDatabase::class.java,
                    "gamelingo_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
