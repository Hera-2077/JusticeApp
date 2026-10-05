package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.TheKuzanDao
import com.example.data.local.TheKuzanRepository
import com.example.data.model.*
import com.example.pdf.PdfExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TheKuzanRulesTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TheKuzanDao
    private lateinit var repository: TheKuzanRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.dao()
        repository = TheKuzanRepository(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testRG01_AudienceCreee_CreeAutomatiquementElementAgenda() = runBlocking {
        // Scénario d'acceptation 1: Audience créée
        val audience = Audience(
            jurisdiction = "TGI",
            subJurisdiction = "Civil",
            party1 = "Ndongo",
            party2 = "Ngapeck",
            dossierNumber = "RG 102/2026",
            lawyerRole = "Conseil du demandeur",
            status = AudienceStatus.EN_COURS.name,
            nextAudienceDate = "2025-05-23",
            nextAudienceTime = "09:00",
            startDate = "2025-05-23"
        )
        val audId = repository.createAudience(audience)
        assertTrue(audId > 0)

        val agendaItems = repository.allAgendaItems.first()
        val audienceItem = agendaItems.find { it.audienceId == audId }
        assertNotNull(audienceItem)
        assertEquals("Audience: Ndongo || Ngapeck", audienceItem?.title)
        assertEquals("2025-05-23", audienceItem?.date)
        assertEquals("09:00", audienceItem?.startTime)
        assertEquals(AgendaType.AUDIENCE.name, audienceItem?.type)
    }

    @Test
    fun testRG02_AudienceReportee_AjouteReportEtNouvelElementAgenda() = runBlocking {
        // Scénario d'acceptation 2: Audience reportée
        val audience = Audience(
            jurisdiction = "TGI",
            subJurisdiction = "Civil",
            party1 = "Ndongo",
            party2 = "Ngapeck",
            status = AudienceStatus.EN_COURS.name,
            nextAudienceDate = "2025-05-23",
            nextAudienceTime = "09:00",
            startDate = "2025-05-23"
        )
        val audId = repository.createAudience(audience)
        val insertedAud = audience.copy(id = audId)

        repository.recordReport(
            audience = insertedAud,
            newDate = "2025-07-14",
            newTime = "09:00",
            reason = "Document non signé par le chef"
        )

        // Audience date updated
        val updatedAudWithDetails = repository.getAudienceById(audId).first()
        assertNotNull(updatedAudWithDetails)
        assertEquals("2025-07-14", updatedAudWithDetails?.audience?.nextAudienceDate)
        assertEquals(1, updatedAudWithDetails?.reports?.size)
        assertEquals("Document non signé par le chef", updatedAudWithDetails?.reports?.first()?.reason)

        // Agenda items: old marked adjourned, new item created for 2025-07-14
        val agendaItems = repository.allAgendaItems.first().filter { it.audienceId == audId }
        assertEquals(2, agendaItems.size)
        val oldItem = agendaItems.find { it.date == "2025-05-23" }
        val newItem = agendaItems.find { it.date == "2025-07-14" }
        assertTrue(oldItem?.isAdjourned == true)
        assertFalse(newItem?.isAdjourned == true)
    }

    @Test
    fun testRG03_DecisionFinale_ClotureAffaireEtPasseDansHistorique() = runBlocking {
        // Scénario d'acceptation 3: Décision finale
        val audience = Audience(
            jurisdiction = "TGI",
            subJurisdiction = "Civil",
            party1 = "Ondoua",
            party2 = "Ryan",
            status = AudienceStatus.EN_COURS.name,
            nextAudienceDate = "2025-10-18",
            nextAudienceTime = "10:00",
            startDate = "2025-05-23"
        )
        val audId = repository.createAudience(audience)
        val insertedAud = audience.copy(id = audId)

        repository.recordFinalDecision(
            audience = insertedAud,
            decisionDate = "2025-12-10",
            decisionText = "Rejetée"
        )

        // Check active hearings (Cahier): must be 0
        val activeList = repository.activeAudiences.first()
        assertTrue(activeList.none { it.audience.id == audId })

        // Check closed hearings (Historique): must be present
        val closedList = repository.closedAudiences.first()
        val closedCase = closedList.find { it.audience.id == audId }
        assertNotNull(closedCase)
        assertEquals("CLOTUREE", closedCase?.audience?.status)
        assertEquals("Rejetée", closedCase?.decision?.decisionText)
        assertEquals("2025-12-10", closedCase?.decision?.decisionDate)
    }

    @Test
    fun testRG08_TacheCochee_BasculerFait() = runBlocking {
        // Scénario d'acceptation 5: Tâche cochée
        val diligence = AgendaItem(
            type = AgendaType.DILIGENCE.name,
            title = "Cotisation Barreau",
            counterpart = "Ordre des Avocats",
            date = "2026-10-05",
            isDone = false
        )
        val id = repository.addAgendaItem(diligence)
        val inserted = diligence.copy(id = id)

        // Toggle done
        repository.toggleAgendaItemDone(inserted)
        var current = repository.allAgendaItems.first().find { it.id == id }
        assertTrue(current?.isDone == true)
        assertNotNull(current?.completionTimestamp)

        // Toggle back to undone
        repository.toggleAgendaItemDone(current!!)
        current = repository.allAgendaItems.first().find { it.id == id }
        assertFalse(current?.isDone == true)
        assertNull(current?.completionTimestamp)
    }

    @Test
    fun testExportPdfA4_GeneratesFileOffline() = runBlocking {
        // Scénario d'acceptation 4: Export PDF
        val user = User(
            fullName = "Maître Paul Ndongo",
            firmName = "Cabinet Ndongo & Associés"
        )
        val audience = Audience(
            id = 42L,
            jurisdiction = "TGI",
            subJurisdiction = "Civil",
            party1 = "Ondoua",
            party2 = "Ryan",
            status = AudienceStatus.CLOTUREE.name,
            nextAudienceDate = "2025-12-10",
            startDate = "2025-05-23"
        )
        val reports = listOf(
            ReportEntry(
                audienceId = 42L,
                reportedAudienceDate = "2025-05-23",
                newAudienceDate = "2025-07-14",
                reason = "Document non signé par le chef"
            )
        )
        val decision = DecisionEntry(
            audienceId = 42L,
            decisionDate = "2025-12-10",
            decisionText = "Rejetée"
        )
        val caseDetails = AudienceWithDetails(audience, reports, decision)

        val file = PdfExporter.generateAndShareCasePdf(context, caseDetails, user)
        assertNotNull(file)
        assertTrue(file!!.exists())
        assertTrue(file.length() > 0)
        assertTrue(file.name.contains("Affaire_Ondoua-Ryan"))
    }
}
