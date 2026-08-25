package com.silauncer.cepat.cache

import android.graphics.drawable.Drawable
import android.util.LruCache

// [app/src/main/java/com/silauncer/cepat/cache/ShortcutCache.kt]: Cache Ikon Shortcut Aplikasi
// [Penjelasan]: Menggunakan LruCache sederhana untuk menyimpan ikon shortcut agar tidak memuat ulang dari sistem setiap kali popup dibuka
object ShortcutCache {
    private val cache = LruCache<String, Drawable>(100)

    // [app/src/main/java/com/silauncer/cepat/cache/ShortcutCache.kt]: Mengambil ikon dari cache berdasarkan kunci
    // [Penjelasan]: Mengembalikan Drawable jika tersedia dalam LruCache
    fun get(key: String): Drawable? {
        return cache.get(key)
    }

    // [app/src/main/java/com/silauncer/cepat/cache/ShortcutCache.kt]: Menyimpan ikon ke cache
    // [Penjelasan]: Memasukkan pasangan kunci dan Drawable ikon shortcut ke dalam LruCache
    fun put(key: String, drawable: Drawable) {
        cache.put(key, drawable)
    }

    // [app/src/main/java/com/silauncer/cepat/cache/ShortcutCache.kt]: Menghapus cache ikon berdasarkan nama paket
    // [Penjelasan]: Membersihkan seluruh entri cache yang terkait dengan paket tertentu saat aplikasi diperbarui/dihapus
    fun removePackage(packageName: String) {
        val keysToRemove = cache.snapshot().keys.filter { key ->
            key.startsWith("$packageName:")
        }
        for (key in keysToRemove) {
            cache.remove(key)
        }
    }

    // [app/src/main/java/com/silauncer/cepat/cache/ShortcutCache.kt]: Membersihkan seluruh cache ikon shortcut
    // [Penjelasan]: Mengosongkan LruCache shortcut
    fun clear() {
        cache.evictAll()
    }
}
