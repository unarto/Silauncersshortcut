package com.silauncer.cepat.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// [app/src/main/java/com/silauncer/cepat/storage/db/AppItemEntity.kt]: Entitas Database Tata Letak
// [Penjelasan]: Menyimpan relasi posisi dan koordinat grid aplikasi sesuai aturan Strict (DB untuk Tata Letak)
@Entity(tableName = "app_items")
data class AppItemEntity(
    @PrimaryKey
    val packageName: String,
    val componentName: String, // Flattened component name
    val cellX: Int,
    val cellY: Int,
    val pageIndex: Int,
    val isFolder: Boolean = false,
    val folderId: String? = null
)
