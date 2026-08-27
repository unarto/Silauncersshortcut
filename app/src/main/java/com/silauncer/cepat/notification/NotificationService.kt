package com.silauncer.cepat.notification

import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

// [app/src/main/java/com/silauncer/cepat/notification/NotificationService.kt]: Service Pemantau Notifikasi
// [Penjelasan]: Mengirimkan update notifikasi melalui broadcast intent internal aplikasi secara decoupled tanpa Global State/Manager
class NotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateAllNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateAllNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateAllNotifications()
    }

    private fun updateAllNotifications() {
        try {
            val activeNotifications = activeNotifications ?: return
            val newMap = mutableMapOf<String, MutableList<NotificationItem>>()
            
            for (sbn in activeNotifications) {
                if (!sbn.isClearable) continue
                val packageName = sbn.packageName
                val notification = sbn.notification
                val extras = notification.extras
                val title = extras.getString(android.app.Notification.EXTRA_TITLE) ?: ""
                val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
                
                val item = NotificationItem(
                    id = sbn.id,
                    packageName = packageName,
                    title = title,
                    text = text,
                    postTime = sbn.postTime
                )
                
                if (!newMap.containsKey(packageName)) {
                    newMap[packageName] = mutableListOf()
                }
                newMap[packageName]?.add(item)
            }
            
            val sortedMap = newMap.mapValues { entry -> 
                entry.value.sortedByDescending { it.postTime } 
            }
            
            // [app/src/main/java/com/silauncer/cepat/notification/NotificationService.kt]: Broadcast Pembaruan Notifikasi
            // [Penjelasan]: Mengirimkan notifikasi ke komponen UI melalui intent internal terdaftar dengan package name yang valid
            val intent = Intent(ACTION_NOTIFICATION_UPDATE).apply {
                setPackage(packageName)
                val bundle = Bundle()
                for ((pkg, list) in sortedMap) {
                    bundle.putParcelableArrayList(pkg, ArrayList(list))
                }
                putExtras(bundle)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e("NotificationService", "Gagal memperbarui notifikasi", e)
        }
    }

    companion object {
        const val ACTION_NOTIFICATION_UPDATE = "com.silauncer.cepat.ACTION_NOTIFICATION_UPDATE"
    }
}
