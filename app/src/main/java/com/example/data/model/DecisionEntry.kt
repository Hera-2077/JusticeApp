package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "decisions",
    foreignKeys = [
        ForeignKey(
            entity = Audience::class,
            parentColumns = ["id"],
            childColumns = ["audienceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["audienceId"])]
)
data class DecisionEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val audienceId: Long,
    val decisionDate: String, // YYYY-MM-DD
    val decisionText: String, // Texte ou dispositif du jugement / arrêt
    val recordedAt: Long = System.currentTimeMillis()
)
