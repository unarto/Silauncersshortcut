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
        val parsedShortcuts = ShortcutParser.parseList(context, rawShortcuts)

        smartPopupView.setupShortcuts(parsedShortcuts)
        
        // Fetch notifications
        val notifications = NotificationStateManager.notifications.value[appInfo.packageName] ?: emptyList()
        smartPopupView.setupNotifications(notifications)

        // Action: Info aplikasi
        smartPopupView.setOnInfoClickListener {
            dismiss()
            openAppInfo(appInfo.packageName)
        }

        // Action: Storage
        smartPopupView.setOnStorageClickListener {
            dismiss()
            openAppStorage(appInfo.packageName)
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
            ShortcutLauncher.launch(context, shortcut.rawInfo, sourceBounds, appInfo.user)
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

    private fun openAppStorage(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            // In Android, there isn't a direct standard intent to open storage, 
            // ACTION_APPLICATION_DETAILS_SETTINGS is the closest and most reliable entry point.
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
