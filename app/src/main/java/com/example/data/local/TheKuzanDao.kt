package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TheKuzanDao {

    // User
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUser(id: Long = 1L): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    // Audiences
    @Transaction
    @Query("SELECT * FROM audiences ORDER BY nextAudienceDate ASC, nextAudienceTime ASC")
    fun getAllAudiencesWithDetails(): Flow<List<AudienceWithDetails>>

    @Transaction
    @Query("SELECT * FROM audiences WHERE status = 'EN_COURS' ORDER BY nextAudienceDate ASC, nextAudienceTime ASC")
    fun getActiveAudiencesWithDetails(): Flow<List<AudienceWithDetails>>

    @Transaction
    @Query("SELECT * FROM audiences WHERE status = 'CLOTUREE' ORDER BY updatedAt DESC")
    fun getClosedAudiencesWithDetails(): Flow<List<AudienceWithDetails>>

    @Transaction
    @Query("SELECT * FROM audiences WHERE id = :id LIMIT 1")
    fun getAudienceWithDetailsById(id: Long): Flow<AudienceWithDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudience(audience: Audience): Long

    @Update
    suspend fun updateAudience(audience: Audience)

    @Delete
    suspend fun deleteAudience(audience: Audience)

    @Query("DELETE FROM audiences WHERE id = :id")
    suspend fun deleteAudienceById(id: Long)

    // Reports
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntry): Long

    @Query("SELECT * FROM reports WHERE audienceId = :audienceId ORDER BY recordedAt ASC")
    fun getReportsForAudience(audienceId: Long): Flow<List<ReportEntry>>

    // Decisions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecision(decision: DecisionEntry): Long

    @Update
    suspend fun updateDecision(decision: DecisionEntry)

    @Query("DELETE FROM decisions WHERE audienceId = :audienceId")
    suspend fun deleteDecisionForAudience(audienceId: Long)

    // Agenda Items
    @Query("SELECT * FROM agenda_items ORDER BY date ASC, startTime ASC")
    fun getAllAgendaItems(): Flow<List<AgendaItem>>

    @Query("SELECT * FROM agenda_items WHERE date = :date ORDER BY startTime ASC")
    fun getAgendaItemsForDate(date: String): Flow<List<AgendaItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgendaItem(item: AgendaItem): Long

    @Update
    suspend fun updateAgendaItem(item: AgendaItem)

    @Delete
    suspend fun deleteAgendaItem(item: AgendaItem)

    @Query("DELETE FROM agenda_items WHERE id = :id")
    suspend fun deleteAgendaItemById(id: Long)

    @Query("DELETE FROM agenda_items WHERE audienceId = :audienceId AND date > :currentDate")
    suspend fun deleteFutureAudienceAgendaItems(audienceId: Long, currentDate: String)

    @Query("DELETE FROM agenda_items WHERE audienceId = :audienceId")
    suspend fun deleteAgendaItemsByAudienceId(audienceId: Long)

    @Query("UPDATE agenda_items SET date = :newDate, startTime = :newTime WHERE audienceId = :audienceId AND isAdjourned = 0")
    suspend fun updateAudienceAgendaDate(audienceId: Long, newDate: String, newTime: String)

    @Query("UPDATE agenda_items SET isAdjourned = 1 WHERE audienceId = :audienceId AND date = :oldDate")
    suspend fun markAgendaItemAdjourned(audienceId: Long, oldDate: String)

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<AppNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationRead(id: Long)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllNotificationsRead()

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: Long)
}
