package com.silauncer.cepat.cache

import android.graphics.drawable.Drawable
import android.util.LruCache

// [app/src/main/java/com/silauncer/cepat/cache/MemoryCache.kt]: Abstraksi Cache LruCache
// [Penjelasan]: Mengabstraksikan duplikasi logika caching icon dan shortcut dengan memberikan fungsi generik berprinsip SRP.
abstract class MemoryCache(maxSize: Int) {
    private val cache = LruCache<String, Drawable>(maxSize)

    // [app/src/main/java/com/silauncer/cepat/cache/MemoryCache.kt]: Mengambil item dari cache
    // [Penjelasan]: Mengembalikan Drawable dari memori jika ditemukan berdasarkan key
    fun get(key: String): Drawable? {
        return cache.get(key)
    }

    // [app/src/main/java/com/silauncer/cepat/cache/MemoryCache.kt]: Menyimpan item ke cache
    // [Penjelasan]: Memasukkan Drawable ke dalam LruCache
    fun put(key: String, drawable: Drawable) {
        cache.put(key, drawable)
    }

    // [app/src/main/java/com/silauncer/cepat/cache/MemoryCache.kt]: Menghapus cache berdasarkan paket
    // [Penjelasan]: Membersihkan entri yang sesuai dengan paket tertentu saat aplikasi diubah/dihapus
    fun removePackage(packageName: String) {
        val keysToRemove = cache.snapshot().keys.filter { key ->
            isKeyForPackage(key, packageName)
        }
        for (key in keysToRemove) {
            cache.remove(key)
        }
    }

    // [app/src/main/java/com/silauncer/cepat/cache/MemoryCache.kt]: Membersihkan seluruh cache
    // [Penjelasan]: Mengosongkan LruCache
    fun clear() {
        cache.evictAll()
    }
    
    protected abstract fun isKeyForPackage(key: String, packageName: String): Boolean
}
