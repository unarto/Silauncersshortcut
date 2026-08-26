package com.silauncer.cepat.popup

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.silauncer.cepat.R
import com.silauncer.cepat.apps.AppInfo
import com.silauncer.cepat.shortcut.ShortcutFetcher
import com.silauncer.cepat.shortcut.ShortcutLauncher
import com.silauncer.cepat.shortcut.ShortcutParser
import com.silauncer.cepat.notification.NotificationStateManager

// [app/src/main/java/com/silauncer/cepat/popup/PopupController.kt]: Pengelola Lifecycle PopupWindow
// [Penjelasan]: Mengontrol penampilan, animasi, penghitungan posisi, dan dismiss popup saat touch outside
class PopupController(private val context: Context) {
    private var popupWindow: PopupWindow? = null

    fun showPopup(targetView: View, appInfo: AppInfo) {
        dismiss()
        val smartPopupView = SmartPopupView(context)

        // Fetch dynamic shortcuts
        val rawShortcuts = ShortcutFetcher.getShortcuts(context, appInfo.packageName, appInfo.user)
        val parsedShortcuts = ShortcutParser.parseList(context, rawShortcuts).toMutableList()
        
        // If dynamic shortcuts are empty, check static manifest shortcuts from APK resources
        if (parsedShortcuts.isEmpty()) {
            val manifestShortcuts = ShortcutFetcher.getManifestShortcutsFromXml(context, appInfo.packageName)
            parsedShortcuts.addAll(manifestShortcuts)
        }

        // Fetch config shortcuts
        val rawConfigShortcuts = ShortcutFetcher.getConfigShortcuts(context, appInfo.packageName, appInfo.user)
        val parsedConfigShortcuts = ShortcutParser.parseConfigList(context, rawConfigShortcuts)
        parsedShortcuts.addAll(parsedConfigShortcuts)

        smartPopupView.setupShortcuts(parsedShortcuts)
        
        // Fetch notifications
        val notifications = NotificationStateManager.notifications.value[appInfo.packageName] ?: emptyList()
        smartPopupView.setupNotifications(notifications)
        
        // Filter System Actions visibility based on system app status
        val pm = context.packageManager
        var isSystemApp = false
        try {
            val aInfo = pm.getApplicationInfo(appInfo.packageName, 0)
            isSystemApp = (aInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 || 
                          (aInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        } catch (e: Exception) {
            // Ignore
        }
        val showUninstall = !isSystemApp
        val showShare = !isSystemApp
        smartPopupView.setupSystemActions(
            showInfo = true,
            showUninstall = showUninstall,
            showShare = showShare,
            hasShortcuts = parsedShortcuts.isNotEmpty()
        )

        // Action: Info aplikasi
        smartPopupView.setOnInfoClickListener {
            dismiss()
            openAppInfo(appInfo.packageName)
        }

        // Action: Uninstall
        smartPopupView.setOnUninstallClickListener {
            dismiss()
            uninstallApp(appInfo.packageName)
        }

        // Action: Share
        smartPopupView.setOnShareClickListener {
            dismiss()
            shareApp(appInfo.packageName)
        }

        // [app/src/main/java/com/silauncer/cepat/popup/PopupController.kt]: Aksi Klik Shortcut
        // [Penjelasan]: Menghitung koordinat nyata layar (Rect) dari targetView sebagai sourceBounds untuk LauncherApps.startShortcut
        smartPopupView.setOnShortcutClickListener { shortcut ->
            dismiss()
            val location = IntArray(2)
            targetView.getLocationOnScreen(location)
            val sourceBounds = android.graphics.Rect(
                location[0],
                location[1],
                location[0] + targetView.width,
                location[1] + targetView.height
            )
            ShortcutLauncher.launch(context, shortcut, sourceBounds, appInfo.user)
        }

        popupWindow = PopupWindow(
            smartPopupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            isFocusable = true
        }

        // Calculate Position
        smartPopupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupWidth = smartPopupView.measuredWidth
        val popupHeight = smartPopupView.measuredHeight
        val displayMetrics = context.resources.displayMetrics
        val marginPx = context.resources.getDimensionPixelSize(R.dimen.popup_margin)

        val pos = PopupPositionCalculator.calculate(
            targetView = targetView,
            popupWidth = popupWidth,
            popupHeight = popupHeight,
            screenWidth = displayMetrics.widthPixels,
            screenHeight = displayMetrics.heightPixels,
            marginPx = marginPx
        )

        // Hitung posisi indikator panah (Arrow Indicator) agar menunjuk tepat ke ikon
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val targetCenterX = location[0] + targetView.width / 2
        val arrowWidth = 24 * displayMetrics.density
        val rawArrowOffset = (targetCenterX - pos.x) - (arrowWidth / 2)
        val minArrowX = marginPx.toFloat()
        val maxArrowX = (popupWidth - arrowWidth - marginPx).coerceAtLeast(minArrowX)
        val clampedArrowOffset = rawArrowOffset.coerceIn(minArrowX, maxArrowX)

        smartPopupView.setupArrow(pos.showAbove, clampedArrowOffset)
        popupWindow?.showAtLocation(targetView, 0, pos.x, pos.y)
    }

    fun dismiss() {
        popupWindow?.dismiss()
        popupWindow = null
    }

    private fun openAppInfo(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    // [app/src/main/java/com/silauncer/cepat/popup/PopupController.kt]: Aksi Copot Pemasangan Aplikasi
    // [Penjelasan]: Menggunakan Intent ACTION_DELETE modern untuk meminta konfirmasi uninstall ke sistem Android
    private fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    private fun shareApp(packageName: String) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "https://play.google.com/store/apps/details?id=$packageName"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            // Safe fallback
        }
    }
}
