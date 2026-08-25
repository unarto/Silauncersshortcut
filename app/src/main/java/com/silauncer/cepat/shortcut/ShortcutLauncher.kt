package com.silauncer.cepat.shortcut

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.os.Build
import android.os.Process
import android.os.UserHandle
import androidx.core.content.ContextCompat

// [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutLauncher.kt]: Peluncuran Intent Shortcut 100% Real OS API
// [Penjelasan]: Mengeksekusi/membuka shortcut menggunakan LauncherApps.startShortcut dengan fallback Intent langsung (Activity, ForegroundService, Service, Broadcast) tanpa mock
object ShortcutLauncher {

    // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutLauncher.kt]: Eksekusi Nyata ShortcutInfo
    // [Penjelasan]: Mencoba LauncherApps API terlebih dahulu, jika terkendala izin atau melempar exception, fallback ke penanganan Intent nyata lengkap
    fun launch(
        context: Context,
        shortcutInfo: ShortcutInfo,
        sourceBounds: Rect? = null,
        userHandle: UserHandle? = null
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return false
        }

        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
        val targetUser = userHandle ?: shortcutInfo.userHandle ?: Process.myUserHandle()

        // 1. Eksekusi melalui LauncherApps.startShortcut (Metode Resmi Launcher OS)
        if (launcherApps != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    launcherApps.startShortcut(shortcutInfo, sourceBounds, null)
                    return true
                } else {
                    launcherApps.startShortcut(
                        shortcutInfo.`package`,
                        shortcutInfo.id,
                        sourceBounds,
                        null,
                        targetUser
                    )
                    return true
                }
            } catch (e: Exception) {
                // Fallthrough ke fallback eksekusi Intent langsung jika melempar SecurityException / ActivityNotFoundException
            }
        }

        // 2. Fallback Eksekusi Nyata via Intent asli ShortcutInfo
        val targetIntents = shortcutInfo.intents ?: shortcutInfo.intent?.let { arrayOf(it) }
        if (!targetIntents.isNullOrEmpty()) {
            val intentsToLaunch = targetIntents.map { originalIntent ->
                Intent(originalIntent).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    if (sourceBounds != null) {
                        this.sourceBounds = sourceBounds
                    }
                }
            }.toTypedArray()

            return try {
                if (intentsToLaunch.size > 1) {
                    context.startActivities(intentsToLaunch)
                    true
                } else {
                    val singleIntent = intentsToLaunch[0]
                    launchSingleIntent(context, singleIntent)
                }
            } catch (e: Exception) {
                false
            }
        }

        // 3. Fallback Ekstraksi Launch Intent jika Intent Shortcut internal tidak diekspos oleh OS
        return try {
            val fallbackIntent = context.packageManager.getLaunchIntentForPackage(shortcutInfo.`package`)
            if (fallbackIntent != null) {
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                if (sourceBounds != null) {
                    fallbackIntent.sourceBounds = sourceBounds
                }
                context.startActivity(fallbackIntent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutLauncher.kt]: Penanganan Eksekusi Single Intent
    // [Penjelasan]: Mengecek tipe komponen target (Service/Receiver/Activity) lalu mengeksekusi dengan ContextCompat.startForegroundService atau startActivity
    private fun launchSingleIntent(context: Context, intent: Intent): Boolean {
        val pm = context.packageManager

        // Periksa apakah intent menargetkan Service (misalnya Toggle VPN Sixray / V2Ray / Xray)
        val isService = try {
            pm.queryIntentServices(intent, 0).isNotEmpty()
        } catch (e: Exception) {
            false
        }

        if (isService) {
            return tryStartServiceOrBroadcast(context, intent)
        }

        // Periksa apakah intent menargetkan BroadcastReceiver
        val isReceiver = try {
            pm.queryBroadcastReceivers(intent, 0).isNotEmpty()
        } catch (e: Exception) {
            false
        }

        if (isReceiver) {
            return try {
                context.sendBroadcast(intent)
                true
            } catch (e: Exception) {
                false
            }
        }

        // Jalankan sebagai Activity dengan fallback berjenjang ke Foreground Service / Service / Broadcast
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            tryStartServiceOrBroadcast(context, intent)
        }
    }

    // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutLauncher.kt]: Fallback Service & Broadcast Target
    // [Penjelasan]: Memungkinkan pintasan tipe Service atau Broadcast (seperti Start/Stop Xray/V2Ray/VPN) dieksekusi secara nyata via ContextCompat
    private fun tryStartServiceOrBroadcast(context: Context, intent: Intent): Boolean {
        return try {
            ContextCompat.startForegroundService(context, intent)
            true
        } catch (e: Exception) {
            try {
                context.startService(intent)
                true
            } catch (e2: Exception) {
                try {
                    context.sendBroadcast(intent)
                    true
                } catch (e3: Exception) {
                    false
                }
            }
        }
    }
}

