package com.silauncer.cepat.shortcut

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.os.UserHandle

// [app/src/main/java/com/silauncer/cepat/shortcut/ShortcutKey.kt]: Identifier Unik Shortcut AOSP
// [Penjelasan]: Meniru mekanisme AOSP Launcher3 untuk identifikasi dan eksekusi shortcut
class ShortcutKey(
    val componentName: ComponentName,
    val user: UserHandle
) {
    constructor(packageName: String, user: UserHandle, id: String) : this(
        ComponentName(packageName, id), user
    )

    val id: String
        get() = componentName.className

    val packageName: String
        get() = componentName.packageName

    fun buildRequest(context: android.content.Context): Any? {
        return null // Placeholder if needed
    }

    companion object {
        const val EXTRA_SHORTCUT_ID = "shortcut_id"
        private const val INTENT_CATEGORY = "com.android.launcher3.DEEP_SHORTCUT"

        fun fromInfo(shortcutInfo: ShortcutInfo): ShortcutKey {
            return ShortcutKey(
                shortcutInfo.`package`,
                shortcutInfo.userHandle ?: android.os.Process.myUserHandle(),
                shortcutInfo.id
            )
        }

        fun fromIntent(intent: Intent, user: UserHandle): ShortcutKey {
            val shortcutId = intent.getStringExtra(EXTRA_SHORTCUT_ID) ?: ""
            return ShortcutKey(intent.`package` ?: "", user, shortcutId)
        }

        fun makeIntent(shortcutId: String, packageName: String): Intent {
            return Intent(Intent.ACTION_MAIN)
                .addCategory(INTENT_CATEGORY)
                .setPackage(packageName)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                .putExtra(EXTRA_SHORTCUT_ID, shortcutId)
        }
        
        fun makeIntent(si: ShortcutInfo): Intent {
            return makeIntent(si.id, si.`package`).apply {
                component = si.activity
            }
        }
    }
}
