package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: Long = 1L,
    val username: String = "pndongo",
    val fullName: String = "Maître Paul Ndongo",
    val title: String = "Maître",
    val email: String = "p.ndongo@avocats-cameroun.org",
    val phone: String = "+237 6 99 12 34 56",
    val firmName: String = "Cabinet Juridique Ndongo & Associés",
    val firmAddress: String = "Immeuble rose, Rue de Narvick, Yaoundé",
    val barAssociation: String = "Barreau du Cameroun",
    val isEmailVerified: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val defaultReminder: String = "1 h avant",
    val biometricLockEnabled: Boolean = false,
    val pinCode: String = "1234",
    val language: String = "Français",
    val timezone: String = "Africa/Douala"
)
