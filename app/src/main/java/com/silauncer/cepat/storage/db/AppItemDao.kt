package com.silauncer.cepat.storage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

// [app/src/main/java/com/silauncer/cepat/storage/db/AppItemDao.kt]: DAO Tata Letak
// [Penjelasan]: Akses ke database tata letak aplikasi
@Dao
interface AppItemDao {
    @Query("SELECT * FROM app_items ORDER BY pageIndex ASC, cellY ASC, cellX ASC")
    suspend fun getAllItems(): List<AppItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: AppItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AppItemEntity>)

    @Query("DELETE FROM app_items WHERE packageName = :packageName")
    suspend fun deleteItem(packageName: String)

    @Query("DELETE FROM app_items")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(items: List<AppItemEntity>) {
        deleteAll()
        insertAll(items)
    }
}
