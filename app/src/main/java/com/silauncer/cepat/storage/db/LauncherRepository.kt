package com.silauncer.cepat.storage.db

// [app/src/main/java/com/silauncer/cepat/storage/db/LauncherRepository.kt]: Repositori Tata Letak
// [Penjelasan]: Abstraksi akses ke Room DB
class LauncherRepository(private val appItemDao: AppItemDao) {
    suspend fun getAllItems(): List<AppItemEntity> = appItemDao.getAllItems()

    suspend fun saveCustomOrder(items: List<AppItemEntity>) {
        appItemDao.replaceAll(items)
    }

    suspend fun deleteItem(packageName: String) {
        appItemDao.deleteItem(packageName)
    }
}
