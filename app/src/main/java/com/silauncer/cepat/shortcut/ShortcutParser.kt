package com.silauncer.cepat.shortcut

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Build
import com.silauncer.cepat.cache.ShortcutCache

// [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutParser.kt]: Model & Ekstraksi ShortcutInfo
// [Penjelasan]: Mengekstrak label, icon Drawable, ID, dan Intent langsung dari ShortcutInfo/Manifest XML
data class ParsedShortcut(
    val id: String,
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val rawInfo: ShortcutInfo? = null,
    val configInfo: android.content.pm.LauncherActivityInfo? = null,
    val directIntent: android.content.Intent? = null
)

object ShortcutParser {

    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.N_MR1)
    fun parse(context: Context, shortcutInfo: ShortcutInfo, density: Int): ParsedShortcut {
        val label = shortcutInfo.shortLabel?.toString()
            ?: shortcutInfo.longLabel?.toString()
            ?: shortcutInfo.id

        val cacheKey = "${shortcutInfo.`package`}:${shortcutInfo.id}"
        // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutParser.kt]: Ambil ikon dari ShortcutCache
        // [Penjelasan]: Mencegah pemuatan ulang ikon dari LauncherApps jika sudah ada di LruCache
        var iconDrawable: Drawable? = ShortcutCache.get(cacheKey)

        if (iconDrawable == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutParser.kt]: Pemuatan Ikon Shortcut Resmi
            // [Penjelasan]: Menggunakan LauncherApps.getShortcutIconDrawable API publik resmi tanpa refleksi hidden API (mencegah error runtime Accessing hidden method getIcon)
            try {
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                if (launcherApps != null) {
                    iconDrawable = launcherApps.getShortcutIconDrawable(shortcutInfo, density)
                }
            } catch (e: Exception) {
                // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutParser.kt]: Log icon loading error
                // [Penjelasan]: Mencatat log error saat getShortcutIconDrawable gagal dimuat dari LauncherApps
                android.util.Log.w("ShortcutParser", "Gagal memuat icon shortcut via LauncherApps untuk ${shortcutInfo.id}", e)
                iconDrawable = null
            }

            if (iconDrawable != null) {
                // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutParser.kt]: Simpan ikon ke ShortcutCache
                // [Penjelasan]: Menyimpan ikon shortcut yang baru dimuat ke dalam cache LruCache
                ShortcutCache.put(cacheKey, iconDrawable)
            }
        }

        return ParsedShortcut(
            id = shortcutInfo.id,
            packageName = shortcutInfo.`package`,
            label = label,
            icon = iconDrawable,
            rawInfo = shortcutInfo
        )
    }

    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.N_MR1)
    fun parseList(context: Context, shortcuts: List<ShortcutInfo>): List<ParsedShortcut> {
        val density = context.resources.displayMetrics.densityDpi
        return shortcuts.map { parse(context, it, density) }
    }

    // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutParser.kt]: Konversi LauncherActivityInfo
    // [Penjelasan]: Mengekstrak data dari pintasan konfigurasi (ShortcutConfigActivityInfo) untuk ditampilkan
    fun parseConfig(context: Context, configInfo: android.content.pm.LauncherActivityInfo, density: Int): ParsedShortcut {
        val label = configInfo.label?.toString() ?: configInfo.name
        val cacheKey = "config:${configInfo.componentName.flattenToShortString()}"
        
        var iconDrawable: Drawable? = ShortcutCache.get(cacheKey)
        if (iconDrawable == null) {
            try {
                iconDrawable = configInfo.getIcon(density)
                if (iconDrawable != null) {
                    ShortcutCache.put(cacheKey, iconDrawable)
                }
            } catch (e: Exception) {
                // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutParser.kt]: Log config icon error
                // [Penjelasan]: Mencatat log error saat configInfo.getIcon gagal dimuat
                android.util.Log.w("ShortcutParser", "Gagal memuat icon config shortcut untuk ${configInfo.name}", e)
                iconDrawable = null
            }
        }
        
        return ParsedShortcut(
            id = configInfo.name,
            packageName = configInfo.componentName.packageName,
            label = label,
            icon = iconDrawable,
            rawInfo = null,
            configInfo = configInfo
        )
    }

    fun parseConfigList(context: Context, configActivities: List<android.content.pm.LauncherActivityInfo>): List<ParsedShortcut> {
        val density = context.resources.displayMetrics.densityDpi
        return configActivities.map { parseConfig(context, it, density) }
    }
}
