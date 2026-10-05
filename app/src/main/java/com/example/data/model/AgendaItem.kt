package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AgendaType {
    EVENEMENT,
    DILIGENCE,
    AUDIENCE
}

@Entity(tableName = "agenda_items")
data class AgendaItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // EVENEMENT, DILIGENCE, AUDIENCE
    val title: String,
    val description: String = "",
    val counterpart: String = "", // vis-à-vis pour les diligences (ex: Greffe, Ministère)
    val date: String, // YYYY-MM-DD
    val startTime: String = "", // ex: "15:00"
    val endTime: String = "",   // ex: "18:00"
    val isDone: Boolean = false,
    val completionTimestamp: Long? = null,
    val reminder: String = "1 h avant",
    val recurrence: String = "Jamais",
    val audienceId: Long? = null,
    val isAdjourned: Boolean = false, // marqué reporté si ancienne audience
    val createdAt: Long = System.currentTimeMillis()
)
