package com.silauncer.cepat.storage.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// [app/src/main/java/com/silauncer/cepat/storage/db/LauncherDatabase.kt]: Konfigurasi Room DB
// [Penjelasan]: Setup database untuk menyimpan koordinat layout (cell_x, cell_y, page_index)
@Database(entities = [AppItemEntity::class], version = 1, exportSchema = false)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun appItemDao(): AppItemDao

    companion object {
        @Volatile
        private var INSTANCE: LauncherDatabase? = null

        fun getDatabase(context: Context): LauncherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LauncherDatabase::class.java,
                    "launcher_layout_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
