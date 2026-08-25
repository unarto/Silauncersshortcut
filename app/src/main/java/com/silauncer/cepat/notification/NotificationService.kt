package com.silauncer.cepat.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

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
            
            NotificationStateManager.updateNotifications(sortedMap)
        } catch (e: Exception) {
            // Safe catch
        }
    }
}
