package com.silauncer.cepat.shortcut

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import org.xmlpull.v1.XmlPullParser

// [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutFetcher.kt]: Mengambil daftar dynamic/pinned/manifest shortcut
// [Penjelasan]: Menggunakan LauncherApps API resmi jika memiliki izin host, atau fallback membaca manifest shortcuts.xml asli dari APK
object ShortcutFetcher {

    // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutFetcher.kt]: Konstanta Tag dan Atribut XML Android
    // [Penjelasan]: Menghindari string hardcoded pada XML Pull Parser shortcut manifest
    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    private const val META_DATA_SHORTCUTS = "android.app.shortcuts"
    private const val TAG_SHORTCUT = "shortcut"
    private const val TAG_INTENT = "intent"
    private const val ATTR_SHORTCUT_ID = "shortcutId"
    private const val ATTR_ENABLED = "enabled"
    private const val ATTR_SHORT_LABEL = "shortcutShortLabel"
    private const val ATTR_LONG_LABEL = "shortcutLongLabel"
    private const val ATTR_ICON = "icon"
    private const val ATTR_ACTION = "action"
    private const val ATTR_TARGET_PACKAGE = "targetPackage"
    private const val ATTR_TARGET_CLASS = "targetClass"
    private const val ATTR_DATA = "data"

    fun getShortcuts(context: Context, packageName: String, userHandle: UserHandle? = null): List<ShortcutInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return emptyList()
        }

        val launcherShortcuts = mutableListOf<ShortcutInfo>()

        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            if (launcherApps != null && launcherApps.hasShortcutHostPermission()) {
                val targetUser = userHandle ?: Process.myUserHandle()
                val query = LauncherApps.ShortcutQuery().apply {
                    setPackage(packageName)
                    setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                    )
                }
                val result = try {
                    launcherApps.getShortcuts(query, targetUser)
                } catch (e: Exception) {
                    android.util.Log.e("ShortcutFetcher", "Gagal mengambil dynamic shortcuts", e)
                    null
                }
                if (!result.isNullOrEmpty()) {
                    launcherShortcuts.addAll(result)
                }
            }
        } catch (e: Exception) {
            // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutFetcher.kt]: Logging penanganan error LauncherApps
            // [Penjelasan]: Mencatat log kegagalan pengambilan shortcut dari LauncherApps alih-alih silent catch
            android.util.Log.e("ShortcutFetcher", "Error saat mengakses LauncherApps getShortcuts untuk $packageName", e)
        }

        return launcherShortcuts.sortedBy { it.rank }
    }

    // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutFetcher.kt]: Parsing Manifest shortcuts.xml APK Asli
    // [Penjelasan]: Mengekstrak pintasan statis asli yang dideklarasikan aplikasi pada AndroidManifest.xml (seperti YouTube, Chrome, dll.)
    fun getManifestShortcutsFromXml(context: Context, packageName: String): List<ParsedShortcut> {
        val result = mutableListOf<ParsedShortcut>()
        try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(packageName)
            val activities = pm.queryIntentActivities(mainIntent, PackageManager.GET_META_DATA)

            val xmlResIds = mutableListOf<Int>()

            for (resolveInfo in activities) {
                val actInfo = try {
                    pm.getActivityInfo(
                        android.content.ComponentName(packageName, resolveInfo.activityInfo.name),
                        PackageManager.GET_META_DATA
                    )
                } catch (e: Exception) {
                    resolveInfo.activityInfo
                }
                val metaData = actInfo?.metaData ?: continue
                if (metaData.containsKey(META_DATA_SHORTCUTS)) {
                    val resId = metaData.getInt(META_DATA_SHORTCUTS)
                    if (resId != 0 && !xmlResIds.contains(resId)) {
                        xmlResIds.add(resId)
                    }
                }
            }

            if (xmlResIds.isEmpty()) {
                try {
                    val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA)
                    val actList = pkgInfo.activities
                    if (actList != null) {
                        for (a in actList) {
                            val meta = a.metaData ?: continue
                            if (meta.containsKey(META_DATA_SHORTCUTS)) {
                                val resId = meta.getInt(META_DATA_SHORTCUTS)
                                if (resId != 0 && !xmlResIds.contains(resId)) {
                                    xmlResIds.add(resId)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutFetcher.kt]: Logging fallback metadata manifest
                    // [Penjelasan]: Mencatat log kegagalan parsing metadata package info shortcuts
                    android.util.Log.e("ShortcutFetcher", "Gagal membaca metadata activities untuk $packageName", e)
                }
            }

            val appRes = pm.getResourcesForApplication(packageName)

            for (xmlResId in xmlResIds) {
                val parser = appRes.getXml(xmlResId)
                var eventType = parser.eventType
                var currentShortcutId: String? = null
                var currentLabel: String? = null
                var currentIcon: Drawable? = null
                var currentIntent: Intent? = null
                var isEnabled = true

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        val tagName = parser.name
                        if (tagName == TAG_SHORTCUT) {
                            currentShortcutId = parser.getAttributeValue(ANDROID_NS, ATTR_SHORTCUT_ID)
                                ?: parser.getAttributeValue(null, ATTR_SHORTCUT_ID)

                            val enabledAttr = parser.getAttributeValue(ANDROID_NS, ATTR_ENABLED)
                                ?: parser.getAttributeValue(null, ATTR_ENABLED)
                            isEnabled = enabledAttr?.toBoolean() ?: true

                            val shortLabelRes = parser.getAttributeResourceValue(ANDROID_NS, ATTR_SHORT_LABEL, 0)
                            val longLabelRes = parser.getAttributeResourceValue(ANDROID_NS, ATTR_LONG_LABEL, 0)

                            var label: String? = null
                            if (shortLabelRes != 0) {
                                try {
                                    label = appRes.getString(shortLabelRes)
                                } catch (e: Exception) {
                                    // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutFetcher.kt]: Log resource string resolution failure
                                    // [Penjelasan]: Mencatat kegagalan resolving shortLabel resource ID
                                    android.util.Log.w("ShortcutFetcher", "Resource string shortLabel ID $shortLabelRes tidak ditemukan di $packageName")
                                }
                            }
                            if (label.isNullOrBlank() && longLabelRes != 0) {
                                try {
                                    label = appRes.getString(longLabelRes)
                                } catch (e: Exception) {
                                    // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutFetcher.kt]: Log resource string resolution failure
                                    // [Penjelasan]: Mencatat kegagalan resolving longLabel resource ID
                                    android.util.Log.w("ShortcutFetcher", "Resource string longLabel ID $longLabelRes tidak ditemukan di $packageName")
                                }
                            }
                            if (label.isNullOrBlank()) {
                                label = parser.getAttributeValue(ANDROID_NS, ATTR_SHORT_LABEL)
                                    ?: parser.getAttributeValue(ANDROID_NS, ATTR_LONG_LABEL)
                                    ?: parser.getAttributeValue(null, ATTR_SHORT_LABEL)
                                    ?: parser.getAttributeValue(null, ATTR_LONG_LABEL)
                            }
                            currentLabel = label

                            val iconRes = parser.getAttributeResourceValue(ANDROID_NS, ATTR_ICON, 0)
                            currentIcon = if (iconRes != 0) {
                                try {
                                    androidx.core.content.res.ResourcesCompat.getDrawable(appRes, iconRes, null)
                                } catch (e: Exception) {
                                    try {
                                        appRes.getDrawable(iconRes, null)
                                    } catch (e2: Exception) {
                                        // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutFetcher.kt]: Log resource drawable resolution failure
                                        // [Penjelasan]: Mencatat kegagalan resolving drawable icon dari target package
                                        android.util.Log.w("ShortcutFetcher", "Resource drawable icon ID $iconRes tidak ditemukan di $packageName", e2)
                                        null
                                    }
                                }
                            } else null

                            currentIntent = null
                        } else if (tagName == TAG_INTENT && currentShortcutId != null) {
                            val action = parser.getAttributeValue(ANDROID_NS, ATTR_ACTION)
                                ?: parser.getAttributeValue(null, ATTR_ACTION)
                                ?: Intent.ACTION_VIEW
                            val targetPkg = parser.getAttributeValue(ANDROID_NS, ATTR_TARGET_PACKAGE)
                                ?: parser.getAttributeValue(null, ATTR_TARGET_PACKAGE)
                                ?: packageName
                            val targetCls = parser.getAttributeValue(ANDROID_NS, ATTR_TARGET_CLASS)
                                ?: parser.getAttributeValue(null, ATTR_TARGET_CLASS)
                            val dataUri = parser.getAttributeValue(ANDROID_NS, ATTR_DATA)
                                ?: parser.getAttributeValue(null, ATTR_DATA)

                            val intent = Intent(action)
                            if (targetCls != null) {
                                intent.setClassName(targetPkg, targetCls)
                            } else {
                                intent.setPackage(targetPkg)
                            }
                            if (dataUri != null) {
                                intent.data = Uri.parse(dataUri)
                            }
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            currentIntent = intent
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        if (parser.name == TAG_SHORTCUT && currentShortcutId != null && isEnabled) {
                            if (!currentLabel.isNullOrBlank()) {
                                result.add(
                                    ParsedShortcut(
                                        id = currentShortcutId,
                                        packageName = packageName,
                                        label = currentLabel,
                                        icon = currentIcon,
                                        directIntent = currentIntent
                                    )
                                )
                            }
                            currentShortcutId = null
                            currentLabel = null
                            currentIcon = null
                            currentIntent = null
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutFetcher.kt]: Logging error parser shortcut XML
            // [Penjelasan]: Mencatat kegagalan parsing shortcuts.xml dari package APK
            android.util.Log.e("ShortcutFetcher", "Gagal parsing shortcuts XML untuk $packageName", e)
        }
        return result
    }

    // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutFetcher.kt]: Mengambil daftar Config Shortcuts
    // [Penjelasan]: Menggunakan LauncherApps API untuk mengambil ShortcutConfigActivityInfo O+ 
    fun getConfigShortcuts(context: Context, packageName: String, userHandle: UserHandle? = null): List<android.content.pm.LauncherActivityInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return emptyList()
        }

        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            if (launcherApps != null && launcherApps.hasShortcutHostPermission()) {
                val targetUser = userHandle ?: Process.myUserHandle()
                val configActivities = launcherApps.getShortcutConfigActivityList(packageName, targetUser)
                if (configActivities != null) {
                    return configActivities
                }
            }
        } catch (e: Exception) {
            // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutFetcher.kt]: Logging error config shortcuts
            // [Penjelasan]: Mencatat kegagalan getShortcutConfigActivityList
            android.util.Log.e("ShortcutFetcher", "Gagal mengambil shortcut config activities untuk $packageName", e)
        }
        return emptyList()
    }
}
