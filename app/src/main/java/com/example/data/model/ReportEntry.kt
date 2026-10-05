package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reports",
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
data class ReportEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val audienceId: Long,
    val reportedAudienceDate: String, // YYYY-MM-DD (date qui a été renvoyée)
    val newAudienceDate: String,      // YYYY-MM-DD (nouvelle date fixée)
    val reason: String,              // Motif (ex: "Production du bilan certifié", "Communication de pièces")
    val recordedAt: Long = System.currentTimeMillis()
)
