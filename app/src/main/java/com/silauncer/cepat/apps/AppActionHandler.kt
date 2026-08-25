package com.silauncer.cepat.apps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.widget.Toast
import com.silauncer.cepat.R
import com.silauncer.cepat.popup.PopupController

// [app/src/main/java/com/silauncer/cepat/apps/AppActionHandler.kt]: Penanganan Aksi Peluncuran & Popup Menu
// [Penjelasan]: Mengintegrasikan PopupController untuk menampilkan menu popup pintar long-press dengan pintasan dinamis
class AppActionHandler(private val context: Context) {
    private val popupController = PopupController(context)

    fun launchApp(app: AppInfo) {
        val intent = app.launchIntent()
        startActivitySafely(intent, app.name)
    }

    fun showAppMenu(app: AppInfo, targetView: View? = null) {
        if (targetView != null) {
            popupController.showPopup(targetView, app)
        } else {
            // Fallback default
            openAppInfo(app)
        }
    }

    fun dismissAppMenu() {
        popupController.dismiss()
    }

    private fun openAppInfo(app: AppInfo) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${app.packageName}")
        }
        startActivitySafely(intent, app.name)
    }

    // [app/src/main/java/com/silauncer/cepat/apps/AppActionHandler.kt]: Peluncuran Intent Aman
    // [Penjelasan]: Menangani peluncuran Activity dengan pesan error terlokalisasi tanpa string hardcoded
    private fun startActivitySafely(intent: Intent, appName: String) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.error_app_not_found, appName), Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(context, context.getString(R.string.error_cannot_open_app, appName), Toast.LENGTH_SHORT).show()
        }
    }
}


