package com.silauncer.cepat.notification

data class NotificationItem(
    val id: Int,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long
)
