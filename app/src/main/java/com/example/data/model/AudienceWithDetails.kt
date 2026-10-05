package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class AudienceWithDetails(
    @Embedded val audience: Audience,
    @Relation(
        parentColumn = "id",
        entityColumn = "audienceId"
    )
    val reports: List<ReportEntry> = emptyList(),
    @Relation(
        parentColumn = "id",
        entityColumn = "audienceId"
    )
    val decision: DecisionEntry? = null
)
