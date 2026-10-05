package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.TheKuzanRepository
import com.example.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen {
    AUTH,
    AGENDA,
    CAHIER,
    HISTORIQUE,
    PROFIL
}

enum class AgendaFilter {
    TOUS,
    EVENEMENTS,
    DILIGENCES
}

class TheKuzanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TheKuzanRepository

    init {
        val database = AppDatabase.getInstance(application)
        repository = TheKuzanRepository(database.dao())
    }

    // Screens and Navigation
    private val _currentScreen = MutableStateFlow(AppScreen.AGENDA)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val screenStack = mutableListOf<AppScreen>()

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            screenStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun handleBack(): Boolean {
        if (_selectedAudienceForDetail.value != null) {
            _selectedAudienceForDetail.value = null
            return true
        }
        if (_selectedClosedCaseForDetail.value != null) {
            _selectedClosedCaseForDetail.value = null
            return true
        }
        if (_isNotificationCenterOpen.value) {
            _isNotificationCenterOpen.value = false
            return true
        }
        if (screenStack.isNotEmpty()) {
            _currentScreen.value = screenStack.removeAt(screenStack.size - 1)
            return true
        }
        return false
    }

    // Authentication State
    private val _isAuthenticated = MutableStateFlow(true)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    fun login(emailOrUser: String, pass: String): Boolean {
        if (emailOrUser.isNotBlank() && pass.length >= 4) {
            _isAuthenticated.value = true
            _currentScreen.value = AppScreen.AGENDA
            return true
        }
        return false
    }

    fun logout() {
        _isAuthenticated.value = false
        _currentScreen.value = AppScreen.AUTH
    }

    // Data Streams from Repository
    val user: StateFlow<User?> = repository.user
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeAudiences: StateFlow<List<AudienceWithDetails>> = repository.activeAudiences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val closedAudiences: StateFlow<List<AudienceWithDetails>> = repository.closedAudiences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAgendaItems: StateFlow<List<AgendaItem>> = repository.allAgendaItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<AppNotification>> = repository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = notifications.map { list ->
        list.count { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Agenda State
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val _selectedDate = MutableStateFlow(sdf.format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _agendaFilter = MutableStateFlow(AgendaFilter.TOUS)
    val agendaFilter: StateFlow<AgendaFilter> = _agendaFilter.asStateFlow()

    private val _selectedAgendaItemIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedAgendaItemIds: StateFlow<Set<Long>> = _selectedAgendaItemIds.asStateFlow()

    fun selectDate(date: String) {
        _selectedDate.value = date
        _selectedAgendaItemIds.value = emptySet()
    }

    fun setAgendaFilter(filter: AgendaFilter) {
        _agendaFilter.value = filter
    }

    fun toggleAgendaItemSelection(id: Long) {
        _selectedAgendaItemIds.value = if (_selectedAgendaItemIds.value.contains(id)) {
            _selectedAgendaItemIds.value - id
        } else {
            _selectedAgendaItemIds.value + id
        }
    }

    fun clearAgendaSelection() {
        _selectedAgendaItemIds.value = emptySet()
    }

    // RG-08: Toggle item done
    fun toggleAgendaItemDone(item: AgendaItem) {
        viewModelScope.launch {
            repository.toggleAgendaItemDone(item)
        }
    }

    fun deleteSelectedAgendaItems() {
        viewModelScope.launch {
            _selectedAgendaItemIds.value.forEach { id ->
                repository.deleteAgendaItem(id)
            }
            clearAgendaSelection()
        }
    }

    // Add Event & Diligence
    fun createEvent(
        title: String,
        description: String,
        date: String,
        startTime: String,
        endTime: String,
        reminder: String,
        recurrence: String
    ) {
        viewModelScope.launch {
            val item = AgendaItem(
                type = AgendaType.EVENEMENT.name,
                title = title,
                description = description,
                date = date,
                startTime = startTime,
                endTime = endTime,
                reminder = reminder,
                recurrence = recurrence
            )
            repository.addAgendaItem(item)
        }
    }

    fun createDiligence(
        counterpart: String,
        purpose: String,
        date: String,
        startTime: String,
        endTime: String,
        linkedAudienceId: Long?
    ) {
        viewModelScope.launch {
            val item = AgendaItem(
                type = AgendaType.DILIGENCE.name,
                title = counterpart,
                description = purpose,
                counterpart = counterpart,
                date = date,
                startTime = startTime,
                endTime = endTime,
                audienceId = linkedAudienceId
            )
            repository.addAgendaItem(item)
        }
    }

    // Cahier d'audiences State
    private val _cahierSearchQuery = MutableStateFlow("")
    val cahierSearchQuery: StateFlow<String> = _cahierSearchQuery.asStateFlow()

    private val _cahierJurisdictionFilter = MutableStateFlow("TOUS")
    val cahierJurisdictionFilter: StateFlow<String> = _cahierJurisdictionFilter.asStateFlow()

    private val _selectedAudienceForDetail = MutableStateFlow<AudienceWithDetails?>(null)
    val selectedAudienceForDetail: StateFlow<AudienceWithDetails?> = _selectedAudienceForDetail.asStateFlow()

    fun setCahierSearchQuery(query: String) {
        _cahierSearchQuery.value = query
    }

    fun setCahierJurisdictionFilter(jur: String) {
        _cahierJurisdictionFilter.value = jur
    }

    fun openAudienceDetail(audienceWithDetails: AudienceWithDetails) {
        _selectedAudienceForDetail.value = audienceWithDetails
    }

    fun closeAudienceDetail() {
        _selectedAudienceForDetail.value = null
    }

    // RG-01: Create Audience
    fun createAudience(
        jurisdiction: String,
        subJurisdiction: String,
        party1: String,
        party2: String,
        dossierNumber: String,
        lawyerRole: String,
        date: String,
        time: String,
        notes: String
    ) {
        viewModelScope.launch {
            val newAudience = Audience(
                jurisdiction = jurisdiction,
                subJurisdiction = subJurisdiction,
                party1 = party1.trim(),
                party2 = party2.trim(),
                dossierNumber = dossierNumber.trim(),
                lawyerRole = lawyerRole,
                status = AudienceStatus.EN_COURS.name,
                nextAudienceDate = date,
                nextAudienceTime = if (time.isNotBlank()) time else "09:00",
                startDate = date,
                notes = notes.trim()
            )
            repository.createAudience(newAudience)
        }
    }

    // RG-02: Record Report (Renvoi)
    fun recordReport(
        audience: Audience,
        newDate: String,
        newTime: String,
        reason: String
    ) {
        viewModelScope.launch {
            repository.recordReport(audience, newDate, newTime, reason)
            closeAudienceDetail()
        }
    }

    // RG-03: Record Final Decision (Décision)
    fun recordFinalDecision(
        audience: Audience,
        decisionDate: String,
        decisionText: String
    ) {
        viewModelScope.launch {
            repository.recordFinalDecision(audience, decisionDate, decisionText)
            closeAudienceDetail()
        }
    }

    // RG-04 & RG-09: Delete Audience
    fun deleteAudience(audienceId: Long) {
        viewModelScope.launch {
            repository.deleteAudience(audienceId)
            closeAudienceDetail()
        }
    }

    // Historique State
    private val _historiqueSearchQuery = MutableStateFlow("")
    val historiqueSearchQuery: StateFlow<String> = _historiqueSearchQuery.asStateFlow()

    private val _historiqueJurisdictionFilter = MutableStateFlow("TOUS")
    val historiqueJurisdictionFilter: StateFlow<String> = _historiqueJurisdictionFilter.asStateFlow()

    private val _selectedClosedCaseForDetail = MutableStateFlow<AudienceWithDetails?>(null)
    val selectedClosedCaseForDetail: StateFlow<AudienceWithDetails?> = _selectedClosedCaseForDetail.asStateFlow()

    fun setHistoriqueSearchQuery(query: String) {
        _historiqueSearchQuery.value = query
    }

    fun setHistoriqueJurisdictionFilter(jur: String) {
        _historiqueJurisdictionFilter.value = jur
    }

    fun openClosedCaseDetail(case: AudienceWithDetails) {
        _selectedClosedCaseForDetail.value = case
    }

    fun closeClosedCaseDetail() {
        _selectedClosedCaseForDetail.value = null
    }

    // Notifications Center
    private val _isNotificationCenterOpen = MutableStateFlow(false)
    val isNotificationCenterOpen: StateFlow<Boolean> = _isNotificationCenterOpen.asStateFlow()

    fun openNotificationCenter() {
        _isNotificationCenterOpen.value = true
    }

    fun closeNotificationCenter() {
        _isNotificationCenterOpen.value = false
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
        }
    }

    // Update Profile
    fun updateUser(user: User) {
        viewModelScope.launch {
            repository.saveUser(user)
        }
    }
}
