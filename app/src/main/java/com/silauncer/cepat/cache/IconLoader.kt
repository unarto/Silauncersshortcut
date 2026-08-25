package com.silauncer.cepat.cache

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.iconpack.IconPackRepository
import com.silauncer.cepat.storage.LauncherPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

// [app/src/main/java/com/silauncer/cepat/cache/IconLoader.kt]: Pemuatan Ikon Asinkron & Caching
// [Penjelasan]: Menghindari alokasi berulang LauncherPreferences pada IO dispatcher dan menangani fallback ikon secara aman
class IconLoader(
    private val scope: CoroutineScope,
    private val prefs: LauncherPreferences = LauncherPreferences()
) {
    private var defaultIcon: Drawable? = null
    
    // Menyimpan job loading yang sedang berjalan (In-Flight deduplication)
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<Drawable>>()

    private fun getDefaultIcon(context: Context): Drawable {
        if (defaultIcon == null) {
            defaultIcon = context.packageManager.defaultActivityIcon
        }
        return defaultIcon ?: context.packageManager.defaultActivityIcon
    }

    fun loadIconAsync(context: Context, appInfo: AppInfo, onLoaded: (Drawable, String) -> Unit) {
        val cacheKey = appInfo.cacheKey
        
        // 1. Cek Cache Memory (Cepat, sinkron)
        val cached = IconCache.get(cacheKey)
        if (cached != null) {
            onLoaded(cached, cacheKey)
            return
        }

        // 2. Placeholder instan agar view hasil recycle bersih
        onLoaded(getDefaultIcon(context), cacheKey)

        val appContext = context.applicationContext

        scope.launch {
            // 3. Gabung ke request in-flight yang ada, atau buat yang baru
            val deferred = inFlightRequests.computeIfAbsent(cacheKey) {
                scope.async(Dispatchers.IO) {
                    val pm = appContext.packageManager
                    
                    val iconPack = prefs.selectedIconPack
                    var icon: Drawable? = null
                    
                    if (iconPack.isNotEmpty()) {
                        icon = IconPackRepository.getIcon(appContext, iconPack, appInfo.componentName)
                    }

                    if (icon == null) {
                        icon = try {
                            pm.getActivityIcon(appInfo.componentName)
                        } catch (e: PackageManager.NameNotFoundException) {
                            pm.defaultActivityIcon
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            pm.defaultActivityIcon // Fallback jika OS bermasalah
                        }
                    }
                    
                    val finalIcon = icon ?: pm.defaultActivityIcon
                    IconCache.put(cacheKey, finalIcon)
                    finalIcon
                }
            }

            try {
                // Tunggu request selesai (entah request baru atau numpang yang lama)
                val icon = deferred.await()
                withContext(Dispatchers.Main) {
                    onLoaded(icon, cacheKey)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onLoaded(getDefaultIcon(context), cacheKey)
                }
            } finally {
                // Bersihkan in-flight map secara atomik hanya jika instance Deferred masih sama
                inFlightRequests.remove(cacheKey, deferred)
            }
        }
    }
}
