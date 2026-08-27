package com.silauncer.cepat.cache

// [app/src/main/java/com/silauncer/cepat/cache/ShortcutCache.kt]: Cache Ikon Shortcut Aplikasi
// [Penjelasan]: Menggunakan MemoryCache (LruCache) dengan aturan identifikasi prefix paket shortcut
object ShortcutCache : MemoryCache(100) {
    override fun isKeyForPackage(key: String, packageName: String): Boolean {
        return key.startsWith("$packageName:")
    }
}
