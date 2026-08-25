package com.silauncer.cepat.apps

import android.content.ComponentName
import android.content.Intent
import android.os.Process
import android.os.UserHandle

/**
 * Minimal app model untuk Silauncer.
 *
 * Hanya menyimpan identitas aplikasi dan informasi untuk launch.
 * Icon dikelola terpisah oleh IconCache.
 */
data class AppInfo(
    val name: String,
    val componentName: ComponentName,
    val packageName: String,
    val user: UserHandle = Process.myUserHandle()
) {

    fun launchIntent(): Intent {
        return Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = componentName
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
    }

    // [app/src/main/java/com/silauncer/cepat/apps/AppInfo.kt]: Identitas Aplikasi & Kunci Cache
    // [Penjelasan]: Cache key unik berbasis user dan component name tanpa instansiasi LauncherPreferences berulang
    val cacheKey: String = "${user.hashCode()}:${componentName.flattenToString()}"
}
