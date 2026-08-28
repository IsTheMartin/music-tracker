package com.mrtnmrls.music_tracker_app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mrtnmrls.music_tracker_app.data.local.dao.PlayDao
import com.mrtnmrls.music_tracker_app.data.local.entity.PlayEntity

@Database(entities = [PlayEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playDao(): PlayDao


}
