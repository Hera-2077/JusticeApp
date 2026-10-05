package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // AUDIENCE_UPCOMING, AUDIENCE_UPDATE_NEEDED, AGENDA_REMINDER, SYSTEM
    val title: String,
    val message: String,
    val targetType: String = "", // "AUDIENCE", "AGENDA"
    val targetId: Long = 0L,
    val scheduledDate: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
