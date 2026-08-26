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
                    null
                }
                if (!result.isNullOrEmpty()) {
                    launcherShortcuts.addAll(result)
                }
            }
        } catch (e: Exception) {
            // Ignore exception from LauncherApps
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
                extractShortcutXmlResId(actInfo?.metaData)?.let { resId ->
                    if (!xmlResIds.contains(resId)) {
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
                            extractShortcutXmlResId(a.metaData)?.let { resId ->
                                if (!xmlResIds.contains(resId)) {
                                    xmlResIds.add(resId)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Safe fallback
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
                        if (tagName == "shortcut") {
                            currentShortcutId = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "shortcutId")
                                ?: parser.getAttributeValue(null, "shortcutId")

                            val enabledAttr = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "enabled")
                                ?: parser.getAttributeValue(null, "enabled")
                            isEnabled = enabledAttr?.toBoolean() ?: true

                            val shortLabelRes = parser.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "shortcutShortLabel", 0)
                            val longLabelRes = parser.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "shortcutLongLabel", 0)

                            var label: String? = null
                            if (shortLabelRes != 0) {
                                try { label = appRes.getString(shortLabelRes) } catch (e: Exception) { null }
                            }
                            if (label.isNullOrBlank() && longLabelRes != 0) {
                                try { label = appRes.getString(longLabelRes) } catch (e: Exception) { null }
                            }
                            if (label.isNullOrBlank()) {
                                label = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "shortcutShortLabel")
                                    ?: parser.getAttributeValue("http://schemas.android.com/apk/res/android", "shortcutLongLabel")
                                    ?: parser.getAttributeValue(null, "shortcutShortLabel")
                                    ?: parser.getAttributeValue(null, "shortcutLongLabel")
                            }
                            currentLabel = label

                            val iconRes = parser.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "icon", 0)
                            currentIcon = if (iconRes != 0) {
                                try {
                                    androidx.core.content.res.ResourcesCompat.getDrawable(appRes, iconRes, null)
                                } catch (e: Exception) {
                                    try {
                                        appRes.getDrawable(iconRes, null)
                                    } catch (e2: Exception) {
                                        null
                                    }
                                }
                            } else null

                            currentIntent = null
                        } else if (tagName == "intent" && currentShortcutId != null) {
                            val action = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "action")
                                ?: parser.getAttributeValue(null, "action")
                                ?: Intent.ACTION_VIEW
                            val targetPkg = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "targetPackage")
                                ?: parser.getAttributeValue(null, "targetPackage")
                                ?: packageName
                            val targetCls = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "targetClass")
                                ?: parser.getAttributeValue(null, "targetClass")
                            val dataUri = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "data")
                                ?: parser.getAttributeValue(null, "data")

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
                        if (parser.name == "shortcut" && currentShortcutId != null && isEnabled) {
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
            // Safe fallback
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
            // Ignore exception
        }
        return emptyList()
    }

    // [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutFetcher.kt]: Ekstraksi ResId Shortcut XML Metadata
    // [Penjelasan]: Helper terpusat untuk membaca resource ID android.app.shortcuts dari metadata activity/package
    private fun extractShortcutXmlResId(metaData: android.os.Bundle?): Int? {
        if (metaData != null && metaData.containsKey("android.app.shortcuts")) {
            val resId = metaData.getInt("android.app.shortcuts")
            if (resId != 0) return resId
        }
        return null
    }
}
