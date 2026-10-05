package com.example.ui.screens.agenda

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgendaItem
import com.example.data.model.AgendaType
import com.example.ui.components.PillButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.AgendaFilter
import com.example.ui.viewmodel.TheKuzanViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    viewModel: TheKuzanViewModel,
    onNavigateToAudience: (Long) -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val agendaFilter by viewModel.agendaFilter.collectAsState()
    val allAgendaItems by viewModel.allAgendaItems.collectAsState()
    val activeAudiences by viewModel.activeAudiences.collectAsState()
    val selectedItemIds by viewModel.selectedAgendaItemIds.collectAsState()
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsState()

    var showNewEventDialog by remember { mutableStateOf(false) }
    var showNewDiligenceDialog by remember { mutableStateOf(false) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var isFilterMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Calendar state (Year & Month)
    val sdfMonthYear = remember { SimpleDateFormat("MMMM yyyy", Locale.FRENCH) }
    val sdfDayNumber = remember { SimpleDateFormat("d", Locale.getDefault()) }
    val sdfIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    var calendarMonthCalendar by remember {
        val c = Calendar.getInstance()
        try {
            c.time = sdfIso.parse(selectedDate) ?: Date()
        } catch (e: Exception) {
            c.time = Date()
        }
        c.set(Calendar.DAY_OF_MONTH, 1)
        mutableStateOf(c)
    }

    // Today string
    val todayIso = remember { sdfIso.format(Date()) }

    // Dashboard metrics
    val todayItemsCount = remember(allAgendaItems, todayIso) {
        allAgendaItems.count { it.date == todayIso && !it.isDone }
    }
    val lateItemsCount = remember(allAgendaItems, todayIso) {
        allAgendaItems.count { it.date < todayIso && !it.isDone && it.type != AgendaType.AUDIENCE.name }
    }
    val audiencesThisWeekCount = remember(activeAudiences) {
        // Active audiences scheduled
        activeAudiences.size
    }

    // Filter items for the selected day
    val itemsForSelectedDay = remember(allAgendaItems, selectedDate, agendaFilter) {
        allAgendaItems
            .filter { it.date == selectedDate }
            .filter { item ->
                when (agendaFilter) {
                    AgendaFilter.TOUS -> true
                    AgendaFilter.EVENEMENTS -> item.type == AgendaType.EVENEMENT.name
                    AgendaFilter.DILIGENCES -> item.type == AgendaType.DILIGENCE.name
                }
            }
            .sortedBy { it.startTime }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectedItemIds.isEmpty()) {
                        Text(
                            text = "Agenda",
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            fontSize = 22.sp
                        )
                    } else {
                        Text(
                            text = "${selectedItemIds.size} sélectionné(s)",
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    if (selectedItemIds.isEmpty()) {
                        // Filter Dropdown Button
                        Box {
                            TextButton(onClick = { isFilterMenuExpanded = true }) {
                                Text(
                                    text = when (agendaFilter) {
                                        AgendaFilter.TOUS -> "Tous"
                                        AgendaFilter.EVENEMENTS -> "Évènements"
                                        AgendaFilter.DILIGENCES -> "Diligences"
                                    },
                                    color = NavyPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = NavyPrimary)
                            }

                            DropdownMenu(
                                expanded = isFilterMenuExpanded,
                                onDismissRequest = { isFilterMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Tous") },
                                    onClick = {
                                        viewModel.setAgendaFilter(AgendaFilter.TOUS)
                                        isFilterMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Évènements") },
                                    onClick = {
                                        viewModel.setAgendaFilter(AgendaFilter.EVENEMENTS)
                                        isFilterMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Diligences") },
                                    onClick = {
                                        viewModel.setAgendaFilter(AgendaFilter.DILIGENCES)
                                        isFilterMenuExpanded = false
                                    }
                                )
                            }
                        }

                        // Notification Bell with badge (Section 5.6)
                        IconButton(onClick = { viewModel.openNotificationCenter() }) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationsCount > 0) {
                                        Badge(containerColor = ColorLate) {
                                            Text("$unreadNotificationsCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "Notifications",
                                    tint = NavyPrimary
                                )
                            }
                        }
                    } else {
                        // Selection Mode Actions: Modifier, Supprimer
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = ColorLate)
                        }
                        IconButton(onClick = { viewModel.clearAgendaSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Annuler", tint = TextSecondary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgLight)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (isFabMenuExpanded) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isFabMenuExpanded = false
                                        showNewEventDialog = true
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Event, contentDescription = null, tint = BlueAccent)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Nouvel évènement", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }

                            Divider(color = SurfaceBorder)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isFabMenuExpanded = false
                                        showNewDiligenceDialog = true
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Assignment, contentDescription = null, tint = ColorReport)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Nouvelle diligence", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { isFabMenuExpanded = !isFabMenuExpanded },
                    containerColor = NavyPrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("agenda_add_fab")
                ) {
                    Icon(
                        imageVector = if (isFabMenuExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Ajouter"
                    )
                }
            }
        },
        containerColor = BgLight
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Tableau de bord compact (Proposition section 5.2.a)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardIndicatorCard(
                        title = "Aujourd'hui",
                        value = "$todayItemsCount à faire",
                        color = NavyPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectDate(todayIso) }
                    )
                    DashboardIndicatorCard(
                        title = "En retard",
                        value = "$lateItemsCount tâche(s)",
                        color = if (lateItemsCount > 0) ColorLate else ColorSuccess,
                        modifier = Modifier.weight(1f),
                        onClick = { /* focuses overdue */ }
                    )
                    DashboardIndicatorCard(
                        title = "Audiences",
                        value = "$audiencesThisWeekCount en cours",
                        color = ColorReport,
                        modifier = Modifier.weight(1f),
                        onClick = { /* focuses hearings */ }
                    )
                }
            }

            // 2. Calendrier mensuel interactif
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Month selector bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                val newCal = calendarMonthCalendar.clone() as Calendar
                                newCal.add(Calendar.MONTH, -1)
                                calendarMonthCalendar = newCal
                            }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Mois précédent", tint = NavyPrimary)
                            }

                            Text(
                                text = sdfMonthYear.format(calendarMonthCalendar.time).replaceFirstChar { it.uppercase() },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = NavyPrimary
                            )

                            IconButton(onClick = {
                                val newCal = calendarMonthCalendar.clone() as Calendar
                                newCal.add(Calendar.MONTH, 1)
                                calendarMonthCalendar = newCal
                            }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Mois suivant", tint = NavyPrimary)
                            }
                        }

                        // Weekday headers: Lun à Dim (la semaine commence le lundi)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim").forEach { day ->
                                Text(
                                    text = day,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMuted,
                                    modifier = Modifier.width(36.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                        // Calendar grid matrix
                        val daysMatrix = remember(calendarMonthCalendar) {
                            generateMonthDays(calendarMonthCalendar)
                        }

                        Column {
                            for (week in daysMatrix) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    for (dayInfo in week) {
                                        val dayIso = dayInfo.isoDate
                                        val isSelected = dayIso == selectedDate
                                        val isToday = dayIso == todayIso
                                        // Count items for task counter dot (RG-06)
                                        val count = allAgendaItems.count { it.date == dayIso }
                                        val allDone = count > 0 && allAgendaItems.filter { it.date == dayIso }.all { it.isDone }

                                        Box(
                                            modifier = Modifier
                                                .width(38.dp)
                                                .height(46.dp)
                                                .clickable { viewModel.selectDate(dayIso) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            when {
                                                                isSelected -> NavyPrimary
                                                                isToday -> BlueAccentLight
                                                                else -> Color.Transparent
                                                            }
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${dayInfo.dayNumber}",
                                                        fontSize = 12.5.sp,
                                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                        color = when {
                                                            isSelected -> Color.White
                                                            !dayInfo.isCurrentMonth -> TextMuted.copy(alpha = 0.5f)
                                                            isToday -> NavyPrimary
                                                            else -> TextPrimary
                                                        }
                                                    )
                                                }

                                                // Compteur de tâches sous le numéro de date (RG-06)
                                                if (count > 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(top = 2.dp)
                                                            .size(14.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (allDone) Color(0xFFCBD5E1) else Color(0xFFF3B23E)
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "$count",
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (allDone) TextSecondary else Color.White
                                                        )
                                                    }
                                                } else {
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Titre de la liste du jour sélectionné
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (agendaFilter) {
                            AgendaFilter.TOUS -> "Évènements & Diligences"
                            AgendaFilter.EVENEMENTS -> "Évènements"
                            AgendaFilter.DILIGENCES -> "Diligences"
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )

                    Text(
                        text = formatHumanDate(selectedDate),
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 4. Fil chronologique des éléments du jour (Timeline)
            if (itemsForSelectedDay.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EventAvailable,
                            contentDescription = null,
                            tint = BlueAccent.copy(alpha = 0.6f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Rien de prévu ce jour",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        PillButton(
                            text = "Ajouter",
                            icon = Icons.Default.Add,
                            onClick = { isFabMenuExpanded = true }
                        )
                    }
                }
            } else {
                items(itemsForSelectedDay, key = { it.id }) { item ->
                    val isSelected = selectedItemIds.contains(item.id)
                    val isLate = item.date < todayIso && !item.isDone && item.type != AgendaType.AUDIENCE.name

                    AgendaTimelineItemCard(
                        item = item,
                        isSelected = isSelected,
                        isLate = isLate,
                        onToggleDone = { viewModel.toggleAgendaItemDone(item) },
                        onLongClick = { viewModel.toggleAgendaItemSelection(item.id) },
                        onClick = {
                            if (selectedItemIds.isNotEmpty()) {
                                viewModel.toggleAgendaItemSelection(item.id)
                            } else if (item.type == AgendaType.AUDIENCE.name && item.audienceId != null) {
                                onNavigateToAudience(item.audienceId)
                            }
                        }
                    )
                }
            }

            // Bottom spacing for FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Modal: Nouvel évènement
    if (showNewEventDialog) {
        NewEventDialog(
            prefilledDate = selectedDate,
            onDismiss = { showNewEventDialog = false },
            onConfirm = { title, desc, date, start, end, reminder, recurrence ->
                viewModel.createEvent(title, desc, date, start, end, reminder, recurrence)
                showNewEventDialog = false
            }
        )
    }

    // Modal: Nouvelle diligence
    if (showNewDiligenceDialog) {
        NewDiligenceDialog(
            prefilledDate = selectedDate,
            activeAudiences = activeAudiences,
            onDismiss = { showNewDiligenceDialog = false },
            onConfirm = { counterpart, purpose, date, start, end, linkedAudId ->
                viewModel.createDiligence(counterpart, purpose, date, start, end, linkedAudId)
                showNewDiligenceDialog = false
            }
        )
    }

    // Modal: Confirmation suppression éléments agenda
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Confirmer la suppression", fontWeight = FontWeight.Bold, color = ColorLate) },
            text = { Text("Voulez-vous supprimer les ${selectedItemIds.size} élément(s) sélectionné(s) de l'agenda ?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedAgendaItems()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorLate)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun DashboardIndicatorCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.5.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AgendaTimelineItemCard(
    item: AgendaItem,
    isSelected: Boolean,
    isLate: Boolean,
    onToggleDone: () -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Horaires à gauche ("15 h 00", flèche, "18 h 00")
        Column(
            modifier = Modifier
                .width(62.dp)
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = if (item.startTime.isNotBlank()) item.startTime else "Toute la",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )
            if (item.endTime.isNotBlank()) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = item.endTime,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            } else if (item.startTime.isBlank()) {
                Text(
                    text = "journée",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Fil vertical
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            item.isDone -> ColorSuccess
                            item.type == AgendaType.AUDIENCE.name -> Color(0xFFF3B23E)
                            isLate -> ColorLate
                            else -> BlueAccent
                        }
                    )
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(SurfaceBorder)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Carte d'élément
        Card(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) BlueAccent else SurfaceBorder,
                    shape = RoundedCornerShape(14.dp)
                ),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) BlueAccentLight.copy(alpha = 0.5f) else Color.White
            ),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Round checkbox pour marquer fait (RG-08)
                if (item.type != AgendaType.AUDIENCE.name) {
                    IconButton(
                        onClick = onToggleDone,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isDone) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = if (item.isDone) "Fait" else "À faire",
                            tint = if (item.isDone) ColorSuccess else TextMuted
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    // Badges row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (item.type == AgendaType.AUDIENCE.name) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFEF3C7))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Gavel, contentDescription = null, tint = ColorReport, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Audience", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = ColorReport)
                            }
                        } else if (item.type == AgendaType.DILIGENCE.name) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BlueAccentLight)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Diligence", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            }
                        }

                        if (isLate) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ColorLateLight)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("En retard", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = ColorLate)
                            }
                        }

                        if (item.isAdjourned) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE2E8F0))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Reportée", fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Titre
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = if (item.isDone) TextMuted else TextPrimary,
                        textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None
                    )

                    // Details en puces
                    if (item.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val lines = item.description.lines().filter { it.isNotBlank() }
                        for (line in lines) {
                            Row(
                                modifier = Modifier.padding(vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(if (item.isDone) TextMuted else BlueAccent)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = line,
                                    fontSize = 12.sp,
                                    color = if (item.isDone) TextMuted else TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // If Audience: indicate clickable to open case in Cahier
                if (item.type == AgendaType.AUDIENCE.name) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Ouvrir",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Calendar day helper
data class DayInfo(val dayNumber: Int, val isCurrentMonth: Boolean, val isoDate: String)

fun generateMonthDays(monthCal: Calendar): List<List<DayInfo>> {
    val cal = monthCal.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)

    // In Cameroon / France, week starts on Monday (Calendar.MONDAY = 2)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val daysBefore = (firstDayOfWeek - Calendar.MONDAY + 7) % 7

    cal.add(Calendar.DAY_OF_MONTH, -daysBefore)

    val weeks = mutableListOf<List<DayInfo>>()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val targetMonth = monthCal.get(Calendar.MONTH)

    for (w in 0 until 5) {
        val week = mutableListOf<DayInfo>()
        for (d in 0 until 7) {
            val dayNum = cal.get(Calendar.DAY_OF_MONTH)
            val isCurrentMonth = cal.get(Calendar.MONTH) == targetMonth
            week.add(DayInfo(dayNum, isCurrentMonth, sdf.format(cal.time)))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        weeks.add(week)
    }
    return weeks
}

fun formatHumanDate(isoDate: String): String {
    return try {
        val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdfIn.parse(isoDate)
        if (date != null) {
            val sdfOut = SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH)
            sdfOut.format(date).replaceFirstChar { it.uppercase() }
        } else isoDate
    } catch (e: Exception) {
        isoDate
    }
}

@Composable
fun NewEventDialog(
    prefilledDate: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, date: String, start: String, end: String, reminder: String, recurrence: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(prefilledDate) }
    var startTime by remember { mutableStateOf("15:00") }
    var endTime by remember { mutableStateOf("18:00") }
    var reminder by remember { mutableStateOf("1 h avant") }
    var recurrence by remember { mutableStateOf("Jamais") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Nouvel évènement", fontWeight = FontWeight.Bold, color = NavyPrimary)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de l'évènement *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (Facultatif - puces)") },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD) *") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Début *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("Fin *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            PillButton(
                text = "Ajouter",
                icon = Icons.Default.Add,
                enabled = title.isNotBlank() && date.isNotBlank(),
                onClick = {
                    onConfirm(title, desc, date, startTime, endTime, reminder, recurrence)
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun NewDiligenceDialog(
    prefilledDate: String,
    activeAudiences: List<com.example.data.model.AudienceWithDetails>,
    onDismiss: () -> Unit,
    onConfirm: (counterpart: String, purpose: String, date: String, start: String, end: String, linkedAudId: Long?) -> Unit
) {
    var counterpart by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(prefilledDate) }
    var startTime by remember { mutableStateOf("10:00") }
    var endTime by remember { mutableStateOf("11:30") }
    var selectedAudienceId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Nouvelle diligence", fontWeight = FontWeight.Bold, color = NavyPrimary)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = counterpart,
                    onValueChange = { counterpart = it },
                    label = { Text("Vis-à-vis (ex: Greffe du TGI, Ministère) *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text("Objet de la diligence (ce qu'il faut faire) *") },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Échéance (YYYY-MM-DD) *") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Début (optionnel)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("Fin (optionnel)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            PillButton(
                text = "Ajouter",
                icon = Icons.Default.Add,
                enabled = counterpart.isNotBlank() && purpose.isNotBlank() && date.isNotBlank(),
                onClick = {
                    onConfirm(counterpart, purpose, date, startTime, endTime, selectedAudienceId)
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
