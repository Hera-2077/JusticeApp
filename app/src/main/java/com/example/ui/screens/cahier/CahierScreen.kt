package com.example.ui.screens.cahier

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Audience
import com.example.data.model.AudienceWithDetails
import com.example.ui.components.JurisdictionBadge
import com.example.ui.components.PillButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.TheKuzanViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CahierScreen(
    viewModel: TheKuzanViewModel
) {
    val activeAudiences by viewModel.activeAudiences.collectAsState()
    val searchQuery by viewModel.cahierSearchQuery.collectAsState()
    val jurisdictionFilter by viewModel.cahierJurisdictionFilter.collectAsState()
    val selectedAudienceForDetail by viewModel.selectedAudienceForDetail.collectAsState()

    var showNewAudienceDialog by remember { mutableStateOf(false) }
    var audienceToUpdate by remember { mutableStateOf<AudienceWithDetails?>(null) }
    var audienceToDelete by remember { mutableStateOf<AudienceWithDetails?>(null) }

    val sdfIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayIso = remember { sdfIso.format(Date()) }

    // Filter and group audiences
    val filteredAudiences = remember(activeAudiences, searchQuery, jurisdictionFilter) {
        activeAudiences
            .filter { audWithDetails ->
                val aud = audWithDetails.audience
                val matchesQuery = searchQuery.isBlank() ||
                        aud.formattedParties.contains(searchQuery, ignoreCase = true) ||
                        aud.jurisdiction.contains(searchQuery, ignoreCase = true) ||
                        aud.subJurisdiction.contains(searchQuery, ignoreCase = true) ||
                        aud.dossierNumber.contains(searchQuery, ignoreCase = true)

                val matchesJurisdiction = jurisdictionFilter == "TOUS" ||
                        aud.jurisdiction.equals(jurisdictionFilter, ignoreCase = true)

                matchesQuery && matchesJurisdiction
            }
            .sortedBy { it.audience.nextAudienceDate }
    }

    // Grouping according to Section 5.3.a:
    // 1. "À mettre à jour" (audience passée sans issue saisie, en tête et en rouge - RG-07)
    // 2. "Aujourd'hui"
    // 3. "Cette semaine" (dans les 7 prochains jours)
    // 4. "Plus tard"
    val groupToUpdate = remember(filteredAudiences, todayIso) {
        filteredAudiences.filter { it.audience.nextAudienceDate < todayIso }
    }
    val groupToday = remember(filteredAudiences, todayIso) {
        filteredAudiences.filter { it.audience.nextAudienceDate == todayIso }
    }
    val groupThisWeek = remember(filteredAudiences, todayIso) {
        val nextWeekIso = getOffsetIsoDate(7)
        filteredAudiences.filter { it.audience.nextAudienceDate > todayIso && it.audience.nextAudienceDate <= nextWeekIso }
    }
    val groupLater = remember(filteredAudiences, todayIso) {
        val nextWeekIso = getOffsetIsoDate(7)
        filteredAudiences.filter { it.audience.nextAudienceDate > nextWeekIso }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgLight)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Cahier d'audiences",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = NavyPrimary
                        )
                        Text(
                            text = "${activeAudiences.size} affaire(s) en cours",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    // Total hearings badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(NavyPrimary)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Enrôlées",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Champ de recherche
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setCahierSearchQuery(it) },
                    placeholder = { Text("Rechercher parties, juridiction, rôle...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NavyPrimary) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setCahierSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Effacer")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = NavyPrimary,
                        unfocusedBorderColor = SurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cahier_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filtres de juridiction rapides
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val jurisdictions = listOf("TOUS", "TGI", "TPI", "CA", "CS", "TCS")
                    items(jurisdictions) { jur ->
                        val isSelected = jurisdictionFilter == jur
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setCahierJurisdictionFilter(jur) },
                            label = {
                                Text(
                                    text = if (jur == "TOUS") "Toutes" else jur,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.5.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) NavyPrimary else SurfaceBorder
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewAudienceDialog = true },
                containerColor = NavyPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_audience_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nouvelle audience")
            }
        },
        containerColor = BgLight
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Empty state if nothing matches
            if (filteredAudiences.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Aucune audience en cours",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ajoutez une audience pour suivre vos affaires judiciaires camerounaises.",
                            fontSize = 13.sp,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        PillButton(
                            text = "Ajouter une audience",
                            icon = Icons.Default.Add,
                            onClick = { showNewAudienceDialog = true }
                        )
                    }
                }
            } else {
                // 1. Groupe: À mettre à jour (En tête et en rouge - RG-07)
                if (groupToUpdate.isNotEmpty()) {
                    item {
                        GroupHeader(
                            title = "À mettre à jour",
                            count = groupToUpdate.size,
                            color = ColorLate,
                            isUrgent = true
                        )
                    }
                    items(groupToUpdate, key = { it.audience.id }) { aud ->
                        AudienceCard(
                            audienceWithDetails = aud,
                            todayIso = todayIso,
                            isToUpdate = true,
                            onClick = { viewModel.openAudienceDetail(aud) },
                            onQuickUpdate = { audienceToUpdate = aud }
                        )
                    }
                }

                // 2. Groupe: Aujourd'hui
                if (groupToday.isNotEmpty()) {
                    item {
                        GroupHeader(
                            title = "Aujourd'hui",
                            count = groupToday.size,
                            color = NavyPrimary
                        )
                    }
                    items(groupToday, key = { it.audience.id }) { aud ->
                        AudienceCard(
                            audienceWithDetails = aud,
                            todayIso = todayIso,
                            onClick = { viewModel.openAudienceDetail(aud) },
                            onQuickUpdate = { audienceToUpdate = aud }
                        )
                    }
                }

                // 3. Groupe: Cette semaine
                if (groupThisWeek.isNotEmpty()) {
                    item {
                        GroupHeader(
                            title = "Cette semaine",
                            count = groupThisWeek.size,
                            color = NavyLight
                        )
                    }
                    items(groupThisWeek, key = { it.audience.id }) { aud ->
                        AudienceCard(
                            audienceWithDetails = aud,
                            todayIso = todayIso,
                            onClick = { viewModel.openAudienceDetail(aud) },
                            onQuickUpdate = { audienceToUpdate = aud }
                        )
                    }
                }

                // 4. Groupe: Plus tard
                if (groupLater.isNotEmpty()) {
                    item {
                        GroupHeader(
                            title = "Plus tard",
                            count = groupLater.size,
                            color = TextSecondary
                        )
                    }
                    items(groupLater, key = { it.audience.id }) { aud ->
                        AudienceCard(
                            audienceWithDetails = aud,
                            todayIso = todayIso,
                            onClick = { viewModel.openAudienceDetail(aud) },
                            onQuickUpdate = { audienceToUpdate = aud }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // CARTE FLOTTANTE D'AUDIENCE (Modal centrée, arrière-plan flouté et assombri - Section 5.3.b)
    if (selectedAudienceForDetail != null) {
        val detail = selectedAudienceForDetail!!
        AudienceFloatingDetailModal(
            audienceWithDetails = detail,
            todayIso = todayIso,
            onDismiss = { viewModel.closeAudienceDetail() },
            onUpdateAudience = {
                val target = detail
                viewModel.closeAudienceDetail()
                audienceToUpdate = target
            },
            onDeleteAudience = {
                val target = detail
                viewModel.closeAudienceDetail()
                audienceToDelete = target
            }
        )
    }

    // FORMULAIRE: Nouvelle Audience (Section 5.3.c)
    if (showNewAudienceDialog) {
        NewAudienceDialog(
            prefilledDate = todayIso,
            onDismiss = { showNewAudienceDialog = false },
            onConfirm = { jur, subJur, p1, p2, dossier, role, date, time, notes ->
                viewModel.createAudience(jur, subJur, p1, p2, dossier, role, date, time, notes)
                showNewAudienceDialog = false
            }
        )
    }

    // FORMULAIRE: Mise à jour de l'audience (Report vs Décision finale - Section 5.3.d)
    if (audienceToUpdate != null) {
        UpdateAudienceDialog(
            audienceWithDetails = audienceToUpdate!!,
            todayIso = todayIso,
            onDismiss = { audienceToUpdate = null },
            onRecordReport = { newDate, newTime, reason ->
                viewModel.recordReport(audienceToUpdate!!.audience, newDate, newTime, reason)
                audienceToUpdate = null
            },
            onRecordFinalDecision = { decDate, decText ->
                viewModel.recordFinalDecision(audienceToUpdate!!.audience, decDate, decText)
                audienceToUpdate = null
            }
        )
    }

    // CONFIRMATION SUPPRESSION AUDIENCE (RG-09)
    if (audienceToDelete != null) {
        AlertDialog(
            onDismissRequest = { audienceToDelete = null },
            title = {
                Text(
                    text = "Confirmer la suppression",
                    fontWeight = FontWeight.Bold,
                    color = ColorLate
                )
            },
            text = {
                Text(
                    text = "Voulez-vous supprimer définitivement l'affaire « ${audienceToDelete!!.audience.formattedParties} » ? Les évènements d'agenda associés seront également retirés."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAudience(audienceToDelete!!.audience.id)
                        audienceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorLate)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { audienceToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun GroupHeader(
    title: String,
    count: Int,
    color: Color,
    isUrgent: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isUrgent) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = ColorLate,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 7.dp, vertical = 1.dp)
        ) {
            Text(
                text = "$count",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun AudienceCard(
    audienceWithDetails: AudienceWithDetails,
    todayIso: String,
    isToUpdate: Boolean = false,
    onClick: () -> Unit,
    onQuickUpdate: () -> Unit
) {
    val aud = audienceWithDetails.audience
    val reports = audienceWithDetails.reports
    val lastReport = reports.maxByOrNull { it.recordedAt }

    val delayLabel = getDelayLabel(aud.nextAudienceDate, todayIso)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable { onClick() }
            .border(
                width = if (isToUpdate) 1.5.dp else 1.dp,
                color = if (isToUpdate) ColorLate else SurfaceBorder,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isToUpdate) Color(0xFFFFF7F7) else Color.White
        ),
        elevation = CardDefaults.cardElevation(1.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Top Row: Badges & Next date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    JurisdictionBadge(jurisdiction = aud.jurisdiction)
                    Text(
                        text = aud.subJurisdiction,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }

                StatusBadge(
                    statusText = delayLabel,
                    isUrgent = isToUpdate || delayLabel == "Aujourd'hui"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Parties: "Partie 1 || Partie 2" (Notation camerounaise de la maquette)
            Text(
                text = aud.formattedParties,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (aud.dossierNumber.isNotBlank()) {
                Text(
                    text = "Dossier : ${aud.dossierNumber} • ${aud.lawyerRole}",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date & Time row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Audience le ${aud.nextAudienceDate} à ${aud.nextAudienceTime}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NavyPrimary
                )

                if (reports.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "• ${reports.size} report(s)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorReport
                    )
                }
            }

            // Dernier motif de report (ligne tronquée)
            if (lastReport != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dernier renvoi : ${lastReport.reason}",
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onQuickUpdate,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isToUpdate) ColorLate else NavyPrimary
                    ),
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isToUpdate) Icons.Default.EditCalendar else Icons.Default.Update,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isToUpdate) "Mettre à jour" else "Report / Décision",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// CARTE FLOTTANTE D'AUDIENCE (Modal centrée, arrière-plan flouté et assombri - Section 5.3.b)
@Composable
fun AudienceFloatingDetailModal(
    audienceWithDetails: AudienceWithDetails,
    todayIso: String,
    onDismiss: () -> Unit,
    onUpdateAudience: () -> Unit,
    onDeleteAudience: () -> Unit
) {
    val aud = audienceWithDetails.audience
    val reports = audienceWithDetails.reports
    val isPast = aud.nextAudienceDate < todayIso

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header: Close icon + Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        JurisdictionBadge(jurisdiction = aud.jurisdiction)
                        Text(
                            text = aud.subJurisdiction,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Parties
                Text(
                    text = aud.formattedParties,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )

                if (aud.dossierNumber.isNotBlank()) {
                    Text(
                        text = "Rôle N° ${aud.dossierNumber} • ${aud.lawyerRole}",
                        fontSize = 12.5.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Key info Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BgLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Prochaine audience : ${aud.nextAudienceDate} à ${aud.nextAudienceTime}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Date de première saisine : ${aud.startDate}",
                            fontSize = 11.5.sp,
                            color = TextSecondary
                        )

                        if (isPast) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = ColorLate, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Audience échue : issue en attente d'enregistrement",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorLate
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Chronologie résumée des reports
                Text(
                    text = "Historique des renvois (${reports.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )

                if (reports.isEmpty()) {
                    Text(
                        text = "Aucun report enregistré pour cette affaire.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        for (rep in reports) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = rep.reportedAudienceDate,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorReport,
                                    modifier = Modifier.width(82.dp)
                                )
                                Text(
                                    text = "→ ${rep.reason} (Renvoyée au ${rep.newAudienceDate})",
                                    fontSize = 11.5.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Notes de l'avocat
                if (aud.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Notes du conseil :",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Text(
                        text = aud.notes,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions: Mettre à jour l'audience (Report ou décision)
                PillButton(
                    text = "Mettre à jour l'audience",
                    icon = Icons.Default.Gavel,
                    onClick = onUpdateAudience,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDeleteAudience) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = ColorLate, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Supprimer l'affaire", color = ColorLate, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// FORMULAIRE NOUVELLE AUDIENCE (Section 5.3.c)
@Composable
fun NewAudienceDialog(
    prefilledDate: String,
    onDismiss: () -> Unit,
    onConfirm: (jur: String, subJur: String, p1: String, p2: String, dossier: String, role: String, date: String, time: String, notes: String) -> Unit
) {
    val jurisdictions = listOf("TGI", "TPI", "CA", "CS", "TCS")
    val subJurDict = mapOf(
        "TGI" to listOf("Civil", "Commercial", "Référé"),
        "TPI" to listOf("Correctionnel", "Civil & Commercial", "Simple Police"),
        "CA" to listOf("Chambre Judiciaire", "Chambre Civile & Commerciale", "Chambre Correctionnelle"),
        "CS" to listOf("Chambre Judiciaire", "Chambre Administrative", "Chambre des Comptes"),
        "TCS" to listOf("Section Spéciale", "Instruction Préparatoire")
    )

    var jurisdiction by remember { mutableStateOf("TGI") }
    var subJurisdiction by remember { mutableStateOf("Civil") }
    var party1 by remember { mutableStateOf("") }
    var party2 by remember { mutableStateOf("") }
    var dossierNumber by remember { mutableStateOf("") }
    var lawyerRole by remember { mutableStateOf("Conseil du demandeur") }
    var date by remember { mutableStateOf(prefilledDate) }
    var time by remember { mutableStateOf("09:00") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle audience", fontWeight = FontWeight.Bold, color = NavyPrimary) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Juridiction & Sous-juridiction
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    var expandedJur by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { expandedJur = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Juridiction: $jurisdiction", fontSize = 12.sp)
                        }
                        DropdownMenu(expanded = expandedJur, onDismissRequest = { expandedJur = false }) {
                            jurisdictions.forEach { j ->
                                DropdownMenuItem(
                                    text = { Text(j) },
                                    onClick = {
                                        jurisdiction = j
                                        subJurisdiction = subJurDict[j]?.firstOrNull() ?: "Civil"
                                        expandedJur = false
                                    }
                                )
                            }
                        }
                    }

                    var expandedSub by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { expandedSub = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Section: $subJurisdiction", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        DropdownMenu(expanded = expandedSub, onDismissRequest = { expandedSub = false }) {
                            (subJurDict[jurisdiction] ?: emptyList()).forEach { sj ->
                                DropdownMenuItem(
                                    text = { Text(sj) },
                                    onClick = {
                                        subJurisdiction = sj
                                        expandedSub = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Partie 1 || Partie 2
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = party1,
                        onValueChange = { party1 = it },
                        label = { Text("Partie 1 (Demandeur)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = party2,
                        onValueChange = { party2 = it },
                        label = { Text("Partie 2 (Défendeur)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Date et Heure
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Heure") },
                        modifier = Modifier.weight(0.8f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dossier et Rôle
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dossierNumber,
                        onValueChange = { dossierNumber = it },
                        label = { Text("N° Dossier / Rôle") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = lawyerRole,
                        onValueChange = { lawyerRole = it },
                        label = { Text("Qualité de l'avocat") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Remarques / Notes du dossier (facultatif)") },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            PillButton(
                text = "Ajouter",
                icon = Icons.Default.Add,
                enabled = party1.isNotBlank() && party2.isNotBlank() && date.isNotBlank(),
                onClick = {
                    onConfirm(jurisdiction, subJurisdiction, party1, party2, dossierNumber, lawyerRole, date, time, notes)
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

// FORMULAIRE: MISE À JOUR DE L'AUDIENCE (Section 5.3.d)
@Composable
fun UpdateAudienceDialog(
    audienceWithDetails: AudienceWithDetails,
    todayIso: String,
    onDismiss: () -> Unit,
    onRecordReport: (newDate: String, newTime: String, reason: String) -> Unit,
    onRecordFinalDecision: (decisionDate: String, decisionText: String) -> Unit
) {
    val aud = audienceWithDetails.audience
    var isDecisionTaken by remember { mutableStateOf(false) }

    // Report fields
    var reportNewDate by remember { mutableStateOf(getOffsetIsoDate(14)) } // 2 weeks ahead by default
    var reportNewTime by remember { mutableStateOf("09:00") }
    var reportReason by remember { mutableStateOf("") }
    var reportError by remember { mutableStateOf<String?>(null) }

    // Decision fields
    var decisionDate by remember { mutableStateOf(todayIso) }
    var decisionText by remember { mutableStateOf("") }
    var showDecisionConfirmationDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Mise à jour d'audience",
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary,
                    fontSize = 18.sp
                )
                Text(
                    text = aud.formattedParties,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Question d'entrée : "Une décision a-t-elle été prise ?" (Radio Oui / Non)
                Text(
                    text = "Une décision a-t-elle été prise ?",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = NavyDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isDecisionTaken = false }
                    ) {
                        RadioButton(
                            selected = !isDecisionTaken,
                            onClick = { isDecisionTaken = false }
                        )
                        Text("Non (Renvoi / Report)", fontSize = 13.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isDecisionTaken = true }
                    ) {
                        RadioButton(
                            selected = isDecisionTaken,
                            onClick = { isDecisionTaken = true }
                        )
                        Text("Oui (Décision finale)", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isDecisionTaken) {
                    // FORMULAIRE DE REPORT (RG-02, RG-05)
                    Text(
                        text = "Enregistrer un renvoi :",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = ColorReport
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = reportNewDate,
                        onValueChange = {
                            reportNewDate = it
                            reportError = null
                        },
                        label = { Text("Nouvelle date fixée (YYYY-MM-DD) *") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = reportNewTime,
                        onValueChange = { reportNewTime = it },
                        label = { Text("Heure d'audience") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = {
                            reportReason = it
                            reportError = null
                        },
                        label = { Text("Motif du renvoi *") },
                        placeholder = { Text("ex: Communication de pièces, Attente rapport d'expertise, Délibéré...") },
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (reportError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = reportError!!, color = ColorLate, fontSize = 12.sp)
                    }
                } else {
                    // FORMULAIRE DÉCISION FINALE (RG-03, RG-05)
                    Text(
                        text = "Enregistrer la décision finale :",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = ColorSuccess
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = decisionDate,
                        onValueChange = { decisionDate = it },
                        label = { Text("Date de la décision (YYYY-MM-DD) *") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = decisionText,
                        onValueChange = { decisionText = it },
                        label = { Text("Dispositif résumé / Texte de la décision *") },
                        placeholder = { Text("ex: Rejetée, Débouté, Condamné à...") },
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            PillButton(
                text = if (!isDecisionTaken) "Enregistrer le report" else "Enregistrer la décision",
                onClick = {
                    if (!isDecisionTaken) {
                        // RG-05: Date de report doit être postérieure à l'audience
                        if (reportNewDate <= aud.nextAudienceDate) {
                            reportError = "La nouvelle date doit être postérieure à la date d'audience (${aud.nextAudienceDate})."
                        } else if (reportReason.isBlank()) {
                            reportError = "Le motif du renvoi est obligatoire."
                        } else {
                            onRecordReport(reportNewDate, reportNewTime, reportReason)
                        }
                    } else {
                        if (decisionDate.isNotBlank() && decisionText.isNotBlank()) {
                            // Demander confirmation renforcée (RG-09)
                            showDecisionConfirmationDialog = true
                        }
                    }
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )

    // Confirmation renforcée pour la décision finale (RG-09)
    if (showDecisionConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDecisionConfirmationDialog = false },
            title = {
                Text(
                    text = "Classer définitivement l'affaire ?",
                    fontWeight = FontWeight.Bold,
                    color = ColorLate
                )
            },
            text = {
                Text(
                    text = "Cette action classe définitivement l'affaire. Elle quittera le Cahier d'audiences et sera archivée dans l'Historique des décisions. Confirmez-vous l'enregistrement ?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDecisionConfirmationDialog = false
                        onRecordFinalDecision(decisionDate, decisionText)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("Oui, classer l'affaire")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDecisionConfirmationDialog = false }) {
                    Text("Vérifier")
                }
            }
        )
    }
}

fun getOffsetIsoDate(daysToAdd: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, daysToAdd)
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
}

fun getDelayLabel(hearingDate: String, todayIso: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val hearing = sdf.parse(hearingDate)
        val today = sdf.parse(todayIso)
        if (hearing != null && today != null) {
            val diff = hearing.time - today.time
            val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
            when {
                days < 0 -> "Passée"
                days == 0L -> "Aujourd'hui"
                days == 1L -> "Demain"
                else -> "Dans $days jours"
            }
        } else hearingDate
    } catch (e: Exception) {
        hearingDate
    }
}
