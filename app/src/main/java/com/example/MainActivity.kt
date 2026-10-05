package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.agenda.AgendaScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.cahier.CahierScreen
import com.example.ui.screens.cahier.NewAudienceDialog
import com.example.ui.screens.agenda.NewDiligenceDialog
import com.example.ui.screens.agenda.NewEventDialog
import com.example.ui.screens.historique.HistoriqueScreen
import com.example.ui.screens.notifications.NotificationSheet
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.TheKuzanViewModel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TheKuzanTheme {
                val viewModel: TheKuzanViewModel = viewModel()
                TheKuzanApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TheKuzanApp(viewModel: TheKuzanViewModel) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isNotificationCenterOpen by viewModel.isNotificationCenterOpen.collectAsState()
    val activeAudiences by viewModel.activeAudiences.collectAsState()
    val selectedAudienceForDetail by viewModel.selectedAudienceForDetail.collectAsState()

    var showQuickActionSheet by remember { mutableStateOf(false) }
    var showQuickAudienceDialog by remember { mutableStateOf(false) }
    var showQuickEventDialog by remember { mutableStateOf(false) }
    var showQuickDiligenceDialog by remember { mutableStateOf(false) }

    val todayIso = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // System Back handling
    BackHandler(enabled = currentScreen != AppScreen.AGENDA || selectedAudienceForDetail != null) {
        val handled = viewModel.handleBack()
        if (!handled && currentScreen != AppScreen.AGENDA) {
            viewModel.navigateTo(AppScreen.AGENDA)
        }
    }

    if (!isAuthenticated) {
        AuthScreen(
            onLoginSuccess = { user, pass ->
                viewModel.login(user, pass)
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                TheKuzanBottomNavigation(
                    currentScreen = currentScreen,
                    onNavigate = { screen ->
                        viewModel.navigateTo(screen)
                    },
                    onQuickActionClick = {
                        showQuickActionSheet = true
                    }
                )
            },
            contentWindowInsets = WindowInsets.navigationBars,
            containerColor = BgLight
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        AppScreen.AUTH -> {
                            AuthScreen(onLoginSuccess = { u, p -> viewModel.login(u, p) })
                        }
                        AppScreen.AGENDA -> {
                            AgendaScreen(
                                viewModel = viewModel,
                                onNavigateToAudience = { audId ->
                                    val target = activeAudiences.find { it.audience.id == audId }
                                    if (target != null) {
                                        viewModel.openAudienceDetail(target)
                                        viewModel.navigateTo(AppScreen.CAHIER)
                                    } else {
                                        viewModel.navigateTo(AppScreen.CAHIER)
                                    }
                                }
                            )
                        }
                        AppScreen.CAHIER -> {
                            CahierScreen(viewModel = viewModel)
                        }
                        AppScreen.HISTORIQUE -> {
                            HistoriqueScreen(viewModel = viewModel)
                        }
                        AppScreen.PROFIL -> {
                            ProfileScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }

        // Notification Center Sheet (Section 5.6)
        if (isNotificationCenterOpen) {
            NotificationSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.closeNotificationCenter() }
            )
        }

        // Quick Action Bottom Sheet (Central elevated logo button)
        if (showQuickActionSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQuickActionSheet = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "Ajout rapide - TheKuzan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = NavyPrimary
                    )
                    Text(
                        text = "Sélectionnez le type d'élément judiciaire à créer",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    QuickActionRow(
                        title = "Nouvelle audience",
                        subtitle = "Enrôler une affaire (TGI, TPI, CA, CS, TCS)",
                        icon = Icons.Default.Gavel,
                        iconColor = Color(0xFFF3B23E),
                        onClick = {
                            showQuickActionSheet = false
                            showQuickAudienceDialog = true
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    QuickActionRow(
                        title = "Nouvelle diligence",
                        subtitle = "Démarche, dépôt d'acte, signification greffe",
                        icon = Icons.Default.Assignment,
                        iconColor = BlueAccent,
                        onClick = {
                            showQuickActionSheet = false
                            showQuickDiligenceDialog = true
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    QuickActionRow(
                        title = "Nouvel évènement",
                        subtitle = "Rendez-vous client, réunion cabinet, audience",
                        icon = Icons.Default.Event,
                        iconColor = ColorSuccess,
                        onClick = {
                            showQuickActionSheet = false
                            showQuickEventDialog = true
                        }
                    )
                }
            }
        }

        // Dialogs triggered from quick actions
        if (showQuickAudienceDialog) {
            NewAudienceDialog(
                prefilledDate = todayIso,
                onDismiss = { showQuickAudienceDialog = false },
                onConfirm = { jur, subJur, p1, p2, dossier, role, date, time, notes ->
                    viewModel.createAudience(jur, subJur, p1, p2, dossier, role, date, time, notes)
                    showQuickAudienceDialog = false
                    viewModel.navigateTo(AppScreen.CAHIER)
                }
            )
        }

        if (showQuickEventDialog) {
            NewEventDialog(
                prefilledDate = todayIso,
                onDismiss = { showQuickEventDialog = false },
                onConfirm = { title, desc, date, start, end, rem, rec ->
                    viewModel.createEvent(title, desc, date, start, end, rem, rec)
                    showQuickEventDialog = false
                    viewModel.navigateTo(AppScreen.AGENDA)
                }
            )
        }

        if (showQuickDiligenceDialog) {
            NewDiligenceDialog(
                prefilledDate = todayIso,
                activeAudiences = activeAudiences,
                onDismiss = { showQuickDiligenceDialog = false },
                onConfirm = { cp, purp, date, start, end, audId ->
                    viewModel.createDiligence(cp, purp, date, start, end, audId)
                    showQuickDiligenceDialog = false
                    viewModel.navigateTo(AppScreen.AGENDA)
                }
            )
        }
    }
}

@Composable
fun TheKuzanBottomNavigation(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    onQuickActionClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Calendrier (Agenda)
            BottomNavItem(
                icon = if (currentScreen == AppScreen.AGENDA) Icons.Default.CalendarMonth else Icons.Outlined.CalendarMonth,
                label = "Agenda",
                isSelected = currentScreen == AppScreen.AGENDA,
                onClick = { onNavigate(AppScreen.AGENDA) }
            )

            // 2. Marteau de juge (Cahier d'audiences)
            BottomNavItem(
                icon = Icons.Default.Gavel,
                label = "Cahier",
                isSelected = currentScreen == AppScreen.CAHIER,
                onClick = { onNavigate(AppScreen.CAHIER) }
            )

            // 3. Central elevated logo button (TheKuzan action)
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(NavyPrimary)
                    .clickable { onQuickActionClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Ajout rapide",
                    tint = Color(0xFFF3B23E),
                    modifier = Modifier.size(28.dp)
                )
            }

            // 4. Livre (Historique des décisions)
            BottomNavItem(
                icon = if (currentScreen == AppScreen.HISTORIQUE) Icons.Default.MenuBook else Icons.Outlined.MenuBook,
                label = "Historique",
                isSelected = currentScreen == AppScreen.HISTORIQUE,
                onClick = { onNavigate(AppScreen.HISTORIQUE) }
            )

            // 5. Avatar (Profil)
            BottomNavItem(
                icon = if (currentScreen == AppScreen.PROFIL) Icons.Default.Person else Icons.Outlined.Person,
                label = "Profil",
                isSelected = currentScreen == AppScreen.PROFIL,
                onClick = { onNavigate(AppScreen.PROFIL) }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) NavyPrimary else TextMuted,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) NavyPrimary else TextMuted
        )
        // Indicator dot/bar under active tab
        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .width(16.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(NavyPrimary)
            )
        }
    }
}

@Composable
fun QuickActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BgLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyPrimary)
                Text(text = subtitle, fontSize = 11.5.sp, color = TextSecondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
        }
    }
}
