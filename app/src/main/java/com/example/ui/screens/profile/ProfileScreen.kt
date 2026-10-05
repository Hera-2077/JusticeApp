package com.example.ui.screens.profile

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.ui.components.PillButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.TheKuzanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: TheKuzanViewModel
) {
    val context = LocalContext.current
    val user by viewModel.user.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    val currentUser = user ?: User()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profil & Paramètres",
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary,
                        fontSize = 22.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgLight)
            )
        },
        containerColor = BgLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // 1. En-tête profil (Avatar, Titre, Nom, Cabinet, Email)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3B23E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PN",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = currentUser.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )

                    Text(
                        text = currentUser.firmName,
                        fontSize = 13.sp,
                        color = BlueAccentLight
                    )

                    Text(
                        text = currentUser.email,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { showEditProfileDialog = true },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Modifier le profil", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Informations du Cabinet & Barreau
            ProfileSectionCard(title = "Cabinet & Barreau") {
                ProfileInfoRow(icon = Icons.Default.Business, label = "Nom du Cabinet", value = currentUser.firmName)
                Divider(color = SurfaceBorder)
                ProfileInfoRow(icon = Icons.Default.Gavel, label = "Barreau", value = currentUser.barAssociation)
                Divider(color = SurfaceBorder)
                ProfileInfoRow(icon = Icons.Default.LocationOn, label = "Adresse", value = currentUser.firmAddress)
                Divider(color = SurfaceBorder)
                ProfileInfoRow(icon = Icons.Default.Phone, label = "Téléphone", value = currentUser.phone)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Notifications & Rappels
            ProfileSectionCard(title = "Notifications & Rappels") {
                var notifEnabled by remember { mutableStateOf(currentUser.notificationsEnabled) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Notifications d'audiences", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                        Text("Rappel la veille à 18h et le matin même à 7h", fontSize = 11.5.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = notifEnabled,
                        onCheckedChange = {
                            notifEnabled = it
                            viewModel.updateUser(currentUser.copy(notificationsEnabled = it))
                        }
                    )
                }
                Divider(color = SurfaceBorder)
                ProfileInfoRow(
                    icon = Icons.Default.NotificationsActive,
                    label = "Rappel par défaut évènements",
                    value = currentUser.defaultReminder
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Sécurité & Verrouillage
            ProfileSectionCard(title = "Sécurité & Confidentialité") {
                var bioLock by remember { mutableStateOf(currentUser.biometricLockEnabled) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Verrouillage biométrique / Code PIN", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                        Text("Protéger les données du secret professionnel", fontSize = 11.5.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = bioLock,
                        onCheckedChange = {
                            bioLock = it
                            viewModel.updateUser(currentUser.copy(biometricLockEnabled = it))
                            Toast.makeText(context, if (it) "Verrouillage activé" else "Verrouillage désactivé", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                Divider(color = SurfaceBorder)
                ProfileInfoRow(
                    icon = Icons.Default.Devices,
                    label = "Appareil synchronisé",
                    value = "Téléphone principal (Connecté)"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Préférences & Localisation
            ProfileSectionCard(title = "Préférences de localisation") {
                ProfileInfoRow(icon = Icons.Default.Language, label = "Langue de l'interface", value = "Français (Cameroun)")
                Divider(color = SurfaceBorder)
                ProfileInfoRow(icon = Icons.Default.Schedule, label = "Fuseau horaire", value = "Africa/Douala (WAT UTC+1)")
                Divider(color = SurfaceBorder)
                ProfileInfoRow(icon = Icons.Default.CalendarViewWeek, label = "Premier jour de la semaine", value = "Lundi")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Déconnexion
            PillButton(
                text = "Se déconnecter",
                icon = Icons.Default.Logout,
                containerColor = Color(0xFFFEE2E2),
                contentColor = ColorLate,
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("logout_button")
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Modal Confirmation Déconnexion
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Déconnexion", fontWeight = FontWeight.Bold, color = NavyPrimary) },
            text = { Text("Êtes-vous sûr de vouloir vous déconnecter de votre compte TheKuzan ?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorLate)
                ) {
                    Text("Déconnexion")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modal Modification Profil
    if (showEditProfileDialog) {
        var editedFullName by remember { mutableStateOf(currentUser.fullName) }
        var editedFirmName by remember { mutableStateOf(currentUser.firmName) }
        var editedAddress by remember { mutableStateOf(currentUser.firmAddress) }
        var editedPhone by remember { mutableStateOf(currentUser.phone) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Modifier le profil", fontWeight = FontWeight.Bold, color = NavyPrimary) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editedFullName,
                        onValueChange = { editedFullName = it },
                        label = { Text("Nom complet avec titre") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editedFirmName,
                        onValueChange = { editedFirmName = it },
                        label = { Text("Nom du Cabinet") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editedAddress,
                        onValueChange = { editedAddress = it },
                        label = { Text("Adresse du Cabinet") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editedPhone,
                        onValueChange = { editedPhone = it },
                        label = { Text("Numéro de téléphone (+237)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                PillButton(
                    text = "Enregistrer",
                    onClick = {
                        viewModel.updateUser(
                            currentUser.copy(
                                fullName = editedFullName,
                                firmName = editedFirmName,
                                firmAddress = editedAddress,
                                phone = editedPhone
                            )
                        )
                        showEditProfileDialog = false
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Annuler") }
            }
        )
    }
}

@Composable
fun ProfileSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary,
                modifier = Modifier.padding(start = 14.dp, top = 14.dp, bottom = 6.dp)
            )
            content()
        }
    }
}

@Composable
fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NavyPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 11.5.sp, color = TextMuted)
            Text(text = value, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
    }
}
