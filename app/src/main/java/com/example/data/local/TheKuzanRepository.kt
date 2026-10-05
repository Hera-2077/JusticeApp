package com.example.data.local

import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class TheKuzanRepository(private val dao: TheKuzanDao) {

    val user: Flow<User?> = dao.getUser()
    val allAudiences: Flow<List<AudienceWithDetails>> = dao.getAllAudiencesWithDetails()
    val activeAudiences: Flow<List<AudienceWithDetails>> = dao.getActiveAudiencesWithDetails()
    val closedAudiences: Flow<List<AudienceWithDetails>> = dao.getClosedAudiencesWithDetails()
    val allAgendaItems: Flow<List<AgendaItem>> = dao.getAllAgendaItems()
    val notifications: Flow<List<AppNotification>> = dao.getAllNotifications()

    fun getAudienceById(id: Long): Flow<AudienceWithDetails?> = dao.getAudienceWithDetailsById(id)

    suspend fun saveUser(user: User) {
        dao.insertUser(user)
    }

    // RG-01: Chaque audience créée génère automatiquement un ÉlémentAgenda de type AUDIENCE
    suspend fun createAudience(audience: Audience): Long {
        val audienceId = dao.insertAudience(audience)
        val agendaItem = AgendaItem(
            type = AgendaType.AUDIENCE.name,
            title = "Audience: ${audience.formattedParties}",
            description = "${audience.jurisdiction} (${audience.subJurisdiction})" +
                    if (audience.dossierNumber.isNotBlank()) " - Dossier ${audience.dossierNumber}" else "",
            date = audience.nextAudienceDate,
            startTime = audience.nextAudienceTime,
            endTime = addHoursToTime(audience.nextAudienceTime, 2),
            audienceId = audienceId
        )
        dao.insertAgendaItem(agendaItem)
        return audienceId
    }

    // RG-02: Un report oblige à saisir une nouvelle date et un motif. Il crée un nouveau ÉlémentAgenda AUDIENCE
    // et ajoute une ligne à l'historique des reports. L'ancien élément est conservé et marqué comme reporté.
    suspend fun recordReport(
        audience: Audience,
        newDate: String,
        newTime: String,
        reason: String
    ) {
        val oldDate = audience.nextAudienceDate
        // Record report in database
        val report = ReportEntry(
            audienceId = audience.id,
            reportedAudienceDate = oldDate,
            newAudienceDate = newDate,
            reason = reason
        )
        dao.insertReport(report)

        // Update audience next date
        val updatedAudience = audience.copy(
            nextAudienceDate = newDate,
            nextAudienceTime = if (newTime.isNotBlank()) newTime else audience.nextAudienceTime,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateAudience(updatedAudience)

        // Mark previous agenda item as adjourned (conservé dans l'historique)
        dao.markAgendaItemAdjourned(audience.id, oldDate)

        // Create new agenda item for the new hearing date
        val newAgendaItem = AgendaItem(
            type = AgendaType.AUDIENCE.name,
            title = "Audience: ${audience.formattedParties}",
            description = "${audience.jurisdiction} (${audience.subJurisdiction}) - Suite renvoi: $reason",
            date = newDate,
            startTime = updatedAudience.nextAudienceTime,
            endTime = addHoursToTime(updatedAudience.nextAudienceTime, 2),
            audienceId = audience.id
        )
        dao.insertAgendaItem(newAgendaItem)

        // Add system notification confirming the report
        dao.insertNotification(
            AppNotification(
                type = "AUDIENCE_UPCOMING",
                title = "Audience renvoyée",
                message = "L'audience ${audience.formattedParties} a été reportée au $newDate ($reason)",
                targetType = "AUDIENCE",
                targetId = audience.id,
                scheduledDate = newDate
            )
        )
    }

    // RG-03: Une décision finale oblige à saisir la date et le texte de décision. L'audience passe à CLOTUREE,
    // disparaît du Cahier et apparaît dans l'Historique. Les futurs ÉlémentsAgenda sont supprimés.
    suspend fun recordFinalDecision(
        audience: Audience,
        decisionDate: String,
        decisionText: String
    ) {
        val decision = DecisionEntry(
            audienceId = audience.id,
            decisionDate = decisionDate,
            decisionText = decisionText
        )
        dao.insertDecision(decision)

        val closedAudience = audience.copy(
            status = AudienceStatus.CLOTUREE.name,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateAudience(closedAudience)

        // Delete future agenda items
        dao.deleteFutureAudienceAgendaItems(audience.id, decisionDate)

        dao.insertNotification(
            AppNotification(
                type = "SYSTEM",
                title = "Affaire clôturée",
                message = "La décision finale a été enregistrée pour l'affaire ${audience.formattedParties}. Fiche archivée.",
                targetType = "AUDIENCE",
                targetId = audience.id,
                scheduledDate = decisionDate
            )
        )
    }

    // RG-04 & RG-09: Modifier l'audience met à jour l'élément d'agenda lié
    suspend fun updateAudience(audience: Audience) {
        dao.updateAudience(audience)
        dao.updateAudienceAgendaDate(audience.id, audience.nextAudienceDate, audience.nextAudienceTime)
    }

    // RG-04 & RG-09: Supprimer l'audience supprime l'élément d'agenda lié
    suspend fun deleteAudience(audienceId: Long) {
        dao.deleteAgendaItemsByAudienceId(audienceId)
        dao.deleteAudienceById(audienceId)
    }

    // Agenda Items operations
    suspend fun addAgendaItem(item: AgendaItem): Long = dao.insertAgendaItem(item)

    suspend fun updateAgendaItem(item: AgendaItem) = dao.updateAgendaItem(item)

    // RG-08: Cocher enregistre la date de réalisation, décocher restaure
    suspend fun toggleAgendaItemDone(item: AgendaItem) {
        if (item.type == AgendaType.AUDIENCE.name) {
            // RG-08: ne s'applique pas aux audiences
            return
        }
        val isNowDone = !item.isDone
        val updated = item.copy(
            isDone = isNowDone,
            completionTimestamp = if (isNowDone) System.currentTimeMillis() else null
        )
        dao.updateAgendaItem(updated)
    }

    suspend fun deleteAgendaItem(id: Long) = dao.deleteAgendaItemById(id)

    // Notification operations
    suspend fun markNotificationRead(id: Long) = dao.markNotificationRead(id)
    suspend fun markAllNotificationsRead() = dao.markAllNotificationsRead()

    private fun addHoursToTime(time: String, hoursToAdd: Int): String {
        return try {
            val parts = time.split(":")
            if (parts.size >= 2) {
                val hour = (parts[0].toInt() + hoursToAdd) % 24
                val minute = parts[1].toInt()
                String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            } else {
                "12:00"
            }
        } catch (e: Exception) {
            "12:00"
        }
    }
}
