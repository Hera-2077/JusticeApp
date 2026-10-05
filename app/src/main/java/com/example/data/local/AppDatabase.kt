package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Database(
    entities = [
        User::class,
        Audience::class,
        ReportEntry::class,
        DecisionEntry::class,
        AgendaItem::class,
        AppNotification::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): TheKuzanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "thekuzan_legal.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    prepopulateDatabase(database.dao())
                }
            }
        }

        private suspend fun prepopulateDatabase(dao: TheKuzanDao) {
            // Default user (Avocat camerounais)
            val defaultUser = User(
                id = 1L,
                username = "pndongo",
                fullName = "Maître Paul Ndongo",
                title = "Maître",
                email = "p.ndongo@avocats-cameroun.org",
                phone = "+237 6 99 12 34 56",
                firmName = "Cabinet Ndongo & Associés",
                firmAddress = "Rue Victoria, Bonanjo, Douala",
                barAssociation = "Ordre des Avocats du Barreau du Cameroun",
                isEmailVerified = true,
                notificationsEnabled = true,
                defaultReminder = "1 h avant",
                biometricLockEnabled = false,
                pinCode = "1234",
                language = "Français",
                timezone = "Africa/Douala"
            )
            dao.insertUser(defaultUser)

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()

            val todayStr = sdf.format(cal.time)

            cal.add(Calendar.DAY_OF_YEAR, -2)
            val twoDaysAgoStr = sdf.format(cal.time)

            cal.add(Calendar.DAY_OF_YEAR, 5) // +3 days from today
            val inThreeDaysStr = sdf.format(cal.time)

            cal.add(Calendar.DAY_OF_YEAR, 4) // +7 days from today
            val inSevenDaysStr = sdf.format(cal.time)

            cal.add(Calendar.DAY_OF_YEAR, -15)
            val pastDate1 = sdf.format(cal.time)

            // Audience 1: À mettre à jour (date passée sans report ni décision) -> RG-07
            val audience1 = Audience(
                jurisdiction = "TGI",
                subJurisdiction = "Civil",
                party1 = "Ndongo",
                party2 = "Ngapeck",
                dossierNumber = "RG 102/CIV/2026",
                lawyerRole = "Conseil du demandeur",
                status = AudienceStatus.EN_COURS.name,
                nextAudienceDate = twoDaysAgoStr,
                nextAudienceTime = "09:00",
                startDate = pastDate1,
                notes = "Litige foncier portant sur le titre foncier N° 4512 du Mfoundi. Communication des originaux requise."
            )
            val aud1Id = dao.insertAudience(audience1)
            dao.insertReport(
                ReportEntry(
                    audienceId = aud1Id,
                    reportedAudienceDate = pastDate1,
                    newAudienceDate = twoDaysAgoStr,
                    reason = "Document non signé par le chef de famille"
                )
            )
            dao.insertAgendaItem(
                AgendaItem(
                    type = AgendaType.AUDIENCE.name,
                    title = "Audience: Ndongo || Ngapeck",
                    description = "TGI (Civil) - Dossier RG 102/CIV/2026",
                    date = twoDaysAgoStr,
                    startTime = "09:00",
                    endTime = "11:00",
                    audienceId = aud1Id
                )
            )

            // Audience 2: Aujourd'hui
            val audience2 = Audience(
                jurisdiction = "TPI",
                subJurisdiction = "Correctionnel",
                party1 = "Ministère Public & Dame Salazar",
                party2 = "Carine",
                dossierNumber = "RP 418/COR/2026",
                lawyerRole = "Défenseur",
                status = AudienceStatus.EN_COURS.name,
                nextAudienceDate = todayStr,
                nextAudienceTime = "10:30",
                startDate = todayStr,
                notes = "Comparution des témoins à charge. Demande de mise en liberté provisoire."
            )
            val aud2Id = dao.insertAudience(audience2)
            dao.insertAgendaItem(
                AgendaItem(
                    type = AgendaType.AUDIENCE.name,
                    title = "Audience: Salazar || Carine",
                    description = "TPI (Correctionnel) - Comparution témoins",
                    date = todayStr,
                    startTime = "10:30",
                    endTime = "12:30",
                    audienceId = aud2Id
                )
            )

            // Audience 3: Dans 3 jours
            val audience3 = Audience(
                jurisdiction = "CA",
                subJurisdiction = "Chambre Judiciaire",
                party1 = "Société CamBois SA",
                party2 = "État du Cameroun (MINFOF)",
                dossierNumber = "ARR 88/2026",
                lawyerRole = "Conseil du demandeur",
                status = AudienceStatus.EN_COURS.name,
                nextAudienceDate = inThreeDaysStr,
                nextAudienceTime = "11:00",
                startDate = pastDate1,
                notes = "Plaidoiries sur conclusions récapitulatives en appel."
            )
            val aud3Id = dao.insertAudience(audience3)
            dao.insertReport(
                ReportEntry(
                    audienceId = aud3Id,
                    reportedAudienceDate = pastDate1,
                    newAudienceDate = inThreeDaysStr,
                    reason = "Attente des conclusions écrites du Ministère Public"
                )
            )
            dao.insertAgendaItem(
                AgendaItem(
                    type = AgendaType.AUDIENCE.name,
                    title = "Audience: CamBois SA || État du Cameroun",
                    description = "Cour d'Appel (Chambre Judiciaire) - Plaidoiries",
                    date = inThreeDaysStr,
                    startTime = "11:00",
                    endTime = "13:00",
                    audienceId = aud3Id
                )
            )

            // Audience 4: Tribunal Criminel Spécial (TCS) plus tard
            val audience4 = Audience(
                jurisdiction = "TCS",
                subJurisdiction = "Section Spéciale",
                party1 = "Ministère Public",
                party2 = "Mba & Cie",
                dossierNumber = "TCS 15/2026",
                lawyerRole = "Défenseur",
                status = AudienceStatus.EN_COURS.name,
                nextAudienceDate = inSevenDaysStr,
                nextAudienceTime = "09:30",
                startDate = pastDate1,
                notes = "Instruction d'audience et audition de l'expert judiciaire."
            )
            val aud4Id = dao.insertAudience(audience4)
            dao.insertAgendaItem(
                AgendaItem(
                    type = AgendaType.AUDIENCE.name,
                    title = "Audience: Ministère Public || Mba & Cie",
                    description = "TCS - Audition expert",
                    date = inSevenDaysStr,
                    startTime = "09:30",
                    endTime = "12:00",
                    audienceId = aud4Id
                )
            )

            // Affaire Clôturée (dans l'Historique des décisions, avec reports et décision finale prête pour export PDF)
            val closedAudience = Audience(
                jurisdiction = "TGI",
                subJurisdiction = "Civil",
                party1 = "Ondoua",
                party2 = "Ryan",
                dossierNumber = "RG 304/CIV/2025",
                lawyerRole = "Conseil du demandeur",
                status = AudienceStatus.CLOTUREE.name,
                nextAudienceDate = "2025-12-10",
                nextAudienceTime = "10:00",
                startDate = "2025-05-23",
                notes = "Procédure d'expulsion et paiement d'arriérés de loyers commerciaux.",
                updatedAt = System.currentTimeMillis() - 86400000L * 30
            )
            val closedId = dao.insertAudience(closedAudience)
            dao.insertReport(
                ReportEntry(
                    audienceId = closedId,
                    reportedAudienceDate = "2025-05-23",
                    newAudienceDate = "2025-07-14",
                    reason = "Document non signé par le chef de famille"
                )
            )
            dao.insertReport(
                ReportEntry(
                    audienceId = closedId,
                    reportedAudienceDate = "2025-07-14",
                    newAudienceDate = "2025-10-18",
                    reason = "Constitution d'avocat par la partie défenderesse"
                )
            )
            dao.insertReport(
                ReportEntry(
                    audienceId = closedId,
                    reportedAudienceDate = "2025-10-18",
                    newAudienceDate = "2025-12-10",
                    reason = "Mise en délibéré pour jugement"
                )
            )
            dao.insertDecision(
                DecisionEntry(
                    audienceId = closedId,
                    decisionDate = "2025-12-10",
                    decisionText = "Par jugement contradictoire en premier ressort : Déclare recevable l'action de M. Ondoua ; Constate la résiliation de plein droit du bail commercial liant les parties ; Ordonne l'expulsion de M. Ryan et de tous occupants de son chef sous astreinte de 100 000 FCFA par jour de retard ; Condamne le défendeur au paiement de la somme de 8 500 000 FCFA au titre des arriérés de loyers, ainsi qu'aux entiers dépens liquidés à 450 000 FCFA distraits au profit de Maître Paul Ndongo."
                )
            )

            // Affaire Clôturée 2
            val closedAudience2 = Audience(
                jurisdiction = "CS",
                subJurisdiction = "Chambre Judiciaire",
                party1 = "Thérèse",
                party2 = "Georges",
                dossierNumber = "POURVOI 112/2024",
                lawyerRole = "Défenseur",
                status = AudienceStatus.CLOTUREE.name,
                nextAudienceDate = "2025-09-15",
                nextAudienceTime = "09:00",
                startDate = "2024-11-20",
                notes = "Pourvoi en cassation contre arrêt de la Cour d'Appel du Littoral.",
                updatedAt = System.currentTimeMillis() - 86400000L * 60
            )
            val closed2Id = dao.insertAudience(closedAudience2)
            dao.insertDecision(
                DecisionEntry(
                    audienceId = closed2Id,
                    decisionDate = "2025-09-15",
                    decisionText = "Rejette le pourvoi formé par dame Thérèse comme non fondé. Laisse les dépens à la charge de la demanderesse au pourvoi."
                )
            )

            // Diligences & Évènements pour l'Agenda
            dao.insertAgendaItem(
                AgendaItem(
                    type = AgendaType.DILIGENCE.name,
                    title = "Cotisation Quadras Barreau",
                    description = "Paiement de la cotisation trimestrielle à l'Ordre des Avocats",
                    counterpart = "Ordre des Avocats du Cameroun",
                    date = todayStr,
                    startTime = "14:00",
                    endTime = "15:30",
                    isDone = false
                )
            )

            dao.insertAgendaItem(
                AgendaItem(
                    type = AgendaType.DILIGENCE.name,
                    title = "Dépôt d'acte et signification",
                    description = "Dépôt des conclusions récapitulatives au Greffe civil",
                    counterpart = "Greffe du TGI du Mfoundi",
                    date = todayStr,
                    startTime = "15:30",
                    endTime = "16:30",
                    isDone = true,
                    completionTimestamp = System.currentTimeMillis()
                )
            )

            dao.insertAgendaItem(
                AgendaItem(
                    type = AgendaType.EVENEMENT.name,
                    title = "Rendez-vous client M. Essomba",
                    description = "Consultation juridique au cabinet - dossier succession",
                    counterpart = "",
                    date = todayStr,
                    startTime = "17:00",
                    endTime = "18:00",
                    isDone = false
                )
            )

            // Notifications
            dao.insertNotification(
                AppNotification(
                    type = "AUDIENCE_UPDATE_NEEDED",
                    title = "Audience à mettre à jour",
                    message = "L'audience Ndongo || Ngapeck (TGI) est passée le $twoDaysAgoStr. Quelle est la suite ?",
                    targetType = "AUDIENCE",
                    targetId = aud1Id,
                    scheduledDate = twoDaysAgoStr,
                    isRead = false
                )
            )
            dao.insertNotification(
                AppNotification(
                    type = "AUDIENCE_UPCOMING",
                    title = "Audience aujourd'hui",
                    message = "Audience Salazar || Carine au TPI (Correctionnel) à 10:30",
                    targetType = "AUDIENCE",
                    targetId = aud2Id,
                    scheduledDate = todayStr,
                    isRead = false
                )
            )
            dao.insertNotification(
                AppNotification(
                    type = "AGENDA_REMINDER",
                    title = "Diligence à venir",
                    message = "Cotisation Quadras Barreau prévue à 14:00",
                    targetType = "AGENDA",
                    targetId = 1L,
                    scheduledDate = todayStr,
                    isRead = true
                )
            )
        }
    }
}
