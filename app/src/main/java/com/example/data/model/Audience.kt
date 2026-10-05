package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AudienceStatus {
    EN_COURS,
    CLOTUREE
}

@Entity(tableName = "audiences")
data class Audience(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jurisdiction: String, // TGI, TPI, CA, CS, TCS
    val subJurisdiction: String, // Civil, Pénal, Correctionnel, Criminel, Commercial, Administratif
    val party1: String, // ex: Ndongo
    val party2: String, // ex: Ngapeck
    val dossierNumber: String = "", // ex: "RP 245/2025"
    val lawyerRole: String = "Conseil du demandeur", // Demandeur, Défendeur, Partie Civile
    val status: String = AudienceStatus.EN_COURS.name,
    val nextAudienceDate: String, // YYYY-MM-DD
    val nextAudienceTime: String = "09:00",
    val startDate: String, // YYYY-MM-DD (première audience)
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val formattedParties: String
        get() = "$party1 || $party2"
}
