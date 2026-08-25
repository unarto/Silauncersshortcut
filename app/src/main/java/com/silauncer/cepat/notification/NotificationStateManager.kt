package com.silauncer.cepat.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object NotificationStateManager {
    // Map of packageName to list of notifications
    private val _notifications = MutableStateFlow<Map<String, List<NotificationItem>>>(emptyMap())
    val notifications: StateFlow<Map<String, List<NotificationItem>>> = _notifications.asStateFlow()

    fun updateNotifications(newMap: Map<String, List<NotificationItem>>) {
        _notifications.update { newMap }
    }
}
