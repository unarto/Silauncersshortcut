package com.silauncer.cepat.shortcut

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.os.Build
import android.os.Process
import android.os.UserHandle

// [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutLauncher.kt]: Peluncuran Intent Shortcut 100% Real OS API
// [Penjelasan]: Mengeksekusi/membuka shortcut menggunakan mekanisme ShortcutKey AOSP dan LauncherApps.startShortcut
object ShortcutLauncher {

    // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutLauncher.kt]: Eksekusi Nyata ShortcutInfo
    // [Penjelasan]: Mengeksekusi directIntent dari manifest XML, atau LauncherApps API / fallback ShortcutKey AOSP
    fun launch(
        context: Context,
        shortcut: ParsedShortcut,
        sourceBounds: Rect? = null,
        userHandle: UserHandle? = null
    ): Boolean {
        if (shortcut.configInfo != null) {
            return launchConfigActivity(context, shortcut.configInfo, userHandle)
        }

        // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutLauncher.kt]: Peluncuran Direct Intent Manifest XML
        // [Penjelasan]: Membuka intent shortcut asli dari APK manifest jika LauncherApps host permission belum aktif
        if (shortcut.directIntent != null) {
            return try {
                val intent = android.content.Intent(shortcut.directIntent).apply {
                    if (sourceBounds != null) {
                        this.sourceBounds = sourceBounds
                    }
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutLauncher.kt]: Log kegagalan Direct Intent
                // [Penjelasan]: Mencatat log error saat direct intent shortcut gagal dieksekusi
                android.util.Log.e("ShortcutLauncher", "Gagal meluncurkan directIntent untuk ${shortcut.id}", e)
                false
            }
        }

        val shortcutInfo = shortcut.rawInfo ?: return false

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return false
        }


        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
        
        // Pemetaan AOSP ShortcutKey
        val shortcutKey = ShortcutKey.fromInfo(shortcutInfo)
        val targetUser = userHandle ?: shortcutKey.user

        // 1. Eksekusi melalui LauncherApps.startShortcut (Metode Resmi Launcher OS)
        if (launcherApps != null && launcherApps.hasShortcutHostPermission()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    launcherApps.startShortcut(shortcutInfo, sourceBounds, null)
                    return true
                } else {
                    launcherApps.startShortcut(
                        shortcutKey.packageName,
                        shortcutKey.id,
                        sourceBounds,
                        null,
                        targetUser
                    )
                    return true
                }
            } catch (e: Exception) {
                // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutLauncher.kt]: Log kegagalan LauncherApps startShortcut
                // [Penjelasan]: Mencatat log error saat LauncherApps API gagal meluncurkan shortcut sebelum masuk ke fallback intent
                android.util.Log.e("ShortcutLauncher", "Gagal startShortcut via LauncherApps untuk ${shortcutKey.packageName}/${shortcutKey.id}", e)
            }
        }

        // 2. Fallback Eksekusi Nyata menggunakan com.android.launcher3.DEEP_SHORTCUT
        return try {
            val intent = ShortcutKey.makeIntent(shortcutInfo).apply {
                if (sourceBounds != null) {
                    this.sourceBounds = sourceBounds
                }
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutLauncher.kt]: Log kegagalan Intent fallback
            // [Penjelasan]: Mencatat log error saat fallback DEEP_SHORTCUT intent gagal dijalankan
            android.util.Log.e("ShortcutLauncher", "Gagal meluncurkan shortcut fallback intent untuk ${shortcutInfo.id}", e)
            false
        }
    }

    // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutLauncher.kt]: Eksekusi Shortcut Config Activity
    // [Penjelasan]: Meluncurkan aktivitas konfigurasi pintasan (ShortcutConfigActivityInfo) dengan mempertimbangkan batas keamanan UserHandle dan O+ API
    private fun launchConfigActivity(
        context: Context,
        configInfo: android.content.pm.LauncherActivityInfo,
        userHandle: UserHandle? = null
    ): Boolean {
        val targetUser = userHandle ?: configInfo.user
        
        // Coba eksekusi intent konfigurasi
        if (targetUser == Process.myUserHandle()) {
            val intent = android.content.Intent(android.content.Intent.ACTION_CREATE_SHORTCUT).apply {
                component = configInfo.componentName
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try {
                context.startActivity(intent)
                true
            } catch (e: android.content.ActivityNotFoundException) {
                // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutLauncher.kt]: Log ActivityNotFoundException
                // [Penjelasan]: Mencatat log error saat config activity shortcut tidak ditemukan
                android.util.Log.e("ShortcutLauncher", "Activity konfigurasi shortcut tidak ditemukan: ${configInfo.componentName}", e)
                false
            } catch (e: SecurityException) {
                // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutLauncher.kt]: Log SecurityException
                // [Penjelasan]: Mencatat log error saat aplikasi tidak memiliki izin menjalankan config activity
                android.util.Log.e("ShortcutLauncher", "Tidak memiliki izin meluncurkan shortcut config activity: ${configInfo.componentName}", e)
                false
            }
        }
        
        // Untuk OS O+ atau UserHandle lain
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            if (launcherApps != null) {
                try {
                    val intentSender = launcherApps.getShortcutConfigActivityIntent(configInfo)
                    if (intentSender != null) {
                        context.startIntentSender(intentSender, null, 0, 0, 0, null)
                        return true
                    }
                } catch (e: Exception) {
                    // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutLauncher.kt]: Log IntentSender Exception
                    // [Penjelasan]: Mencatat log error saat peluncuran intentSender konfigurasi pintasan gagal
                    android.util.Log.e("ShortcutLauncher", "Gagal menjalankan intentSender untuk config shortcut: ${configInfo.componentName}", e)
                    return false
                }
            }
        }
        
        return false
    }
}

