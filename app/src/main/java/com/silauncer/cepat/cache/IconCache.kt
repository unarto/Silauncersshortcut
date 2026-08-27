package com.silauncer.cepat.cache

// [app/src/main/java/com/silauncer/cepat/cache/IconCache.kt]: Cache Ikon Aplikasi
// [Penjelasan]: Menggunakan MemoryCache (LruCache) dengan aturan identifikasi paket icon
object IconCache : MemoryCache(150) {
    override fun isKeyForPackage(key: String, packageName: String): Boolean {
        return key.substringAfter(':').substringBefore('/') == packageName
    }
}
