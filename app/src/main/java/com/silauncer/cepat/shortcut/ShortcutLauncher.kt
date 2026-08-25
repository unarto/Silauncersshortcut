package com.silauncer.cepat.shortcut

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.os.Build
import android.os.Process
import android.os.UserHandle

// [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutLauncher.kt]: Peluncuran Intent Shortcut
// [Penjelasan]: Mengeksekusi/membuka shortcut menggunakan LauncherApps API dengan fallback Intent langsung tanpa duplikasi logika
object ShortcutLauncher {

    fun launch(context: Context, shortcutInfo: ShortcutInfo, sourceBounds: Rect? = null, userHandle: UserHandle? = null): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return false
        }

        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
        val targetUser = userHandle ?: shortcutInfo.userHandle ?: Process.myUserHandle()

        if (launcherApps != null && launcherApps.hasShortcutHostPermission()) {
            try {
                launcherApps.startShortcut(
                    shortcutInfo.`package`,
                    shortcutInfo.id,
                    sourceBounds,
                    null,
                    targetUser
                )
                return true
            } catch (e: Exception) {
                // Fallthrough to direct intent launch
            }
        }

        return try {
            val intent = shortcutInfo.intent ?: shortcutInfo.intents?.firstOrNull()
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

