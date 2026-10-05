package com.example.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PillButton
import com.example.ui.theme.*
import kotlinx.coroutines.delay

enum class AuthTab {
    CONNEXION,
    INSCRIPTION
}

enum class ForgotPasswordState {
    INITIAL,
    ERREUR,
    SUCCES
}

@Composable
fun AuthScreen(
    onLoginSuccess: (emailOrUser: String, pass: String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(AuthTab.CONNEXION) }

    // Connexion state
    var loginUsername by remember { mutableStateOf("pndongo") }
    var loginPassword by remember { mutableStateOf("avocat2026") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Inscription state
    var regUsername by remember { mutableStateOf("") }
    var regFullName by remember { mutableStateOf("") }
    var regTitle by remember { mutableStateOf("Maître") }
    var regFirmName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("+237 6 ") }
    var regPassword by remember { mutableStateOf("") }
    var isRegPasswordVisible by remember { mutableStateOf(false) }
    var acceptTerms by remember { mutableStateOf(false) }

    // Forgot password dialog
    var showForgotPassword by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }
    var forgotState by remember { mutableStateOf(ForgotPasswordState.INITIAL) }
    var resendCountdown by remember { mutableIntStateOf(60) }

    LaunchedEffect(forgotState) {
        if (forgotState == ForgotPasswordState.SUCCES) {
            resendCountdown = 60
            while (resendCountdown > 0) {
                delay(1000)
                resendCountdown--
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Bandeau supérieur bleu marine
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo TheKuzan
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = "Logo TheKuzan",
                        tint = Color(0xFFF3B23E),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "TheKuzan",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Procédures Judiciaires & Cahier d'Audiences",
                    fontSize = 12.sp,
                    color = BlueAccentLight.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Commençons Maintenant",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Text(
                    text = "Créer un compte ou Connectez-vous pour accéder à l'intégralité de l'application",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Carte blanche aux angles supérieurs arrondis
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = BgLight
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Sélecteur à deux onglets (Connexion / Inscription)
                    TabRow(
                        selectedTabIndex = if (selectedTab == AuthTab.CONNEXION) 0 else 1,
                        containerColor = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp)),
                        indicator = { tabPositions ->
                            // Custom clean pill tab indicator
                        },
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == AuthTab.CONNEXION,
                            onClick = { selectedTab = AuthTab.CONNEXION },
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTab == AuthTab.CONNEXION) NavyPrimary else Color.Transparent)
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = "Connexion",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == AuthTab.CONNEXION) Color.White else TextSecondary,
                                fontSize = 14.sp
                            )
                        }

                        Tab(
                            selected = selectedTab == AuthTab.INSCRIPTION,
                            onClick = { selectedTab = AuthTab.INSCRIPTION },
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTab == AuthTab.INSCRIPTION) NavyPrimary else Color.Transparent)
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = "Inscription",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == AuthTab.INSCRIPTION) Color.White else TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "AuthFormAnim"
                    ) { tab ->
                        if (tab == AuthTab.CONNEXION) {
                            // FORMULAIRE CONNEXION
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = loginUsername,
                                    onValueChange = {
                                        loginUsername = it
                                        loginError = null
                                    },
                                    label = { Text("Nom d'utilisateur ou e-mail") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = NavyPrimary)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NavyPrimary,
                                        unfocusedBorderColor = SurfaceBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_username_input")
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = loginPassword,
                                    onValueChange = {
                                        loginPassword = it
                                        loginError = null
                                    },
                                    label = { Text("Mot de passe") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = NavyPrimary)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                                contentDescription = if (isPasswordVisible) "Masquer" else "Afficher"
                                            )
                                        }
                                    },
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NavyPrimary,
                                        unfocusedBorderColor = SurfaceBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_password_input")
                                )

                                // Lien Mot de passe oublié aligné à droite
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = "Mot de passe oublié ?",
                                        color = BlueAccent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clickable {
                                                forgotState = ForgotPasswordState.INITIAL
                                                showForgotPassword = true
                                            }
                                            .padding(vertical = 4.dp)
                                    )
                                }

                                if (loginError != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(ColorLateLight, RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ColorLate)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = loginError!!, color = ColorLate, fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                PillButton(
                                    text = if (isLoading) "Connexion en cours..." else "Se connecter",
                                    icon = Icons.Default.Login,
                                    enabled = loginUsername.isNotBlank() && loginPassword.isNotBlank() && !isLoading,
                                    onClick = {
                                        isLoading = true
                                        onLoginSuccess(loginUsername, loginPassword)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_button")
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Quick demo note
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = NavyLight, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Compte démo pré-rempli : Maître Paul Ndongo (Barreau du Cameroun)",
                                            fontSize = 11.5.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        } else {
                            // FORMULAIRE INSCRIPTION
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = regUsername,
                                    onValueChange = { regUsername = it },
                                    label = { Text("Nom d'utilisateur") },
                                    leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = NavyPrimary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = regFullName,
                                    onValueChange = { regFullName = it },
                                    label = { Text("Nom complet avec Titre (ex: Me Paul Ndongo)") },
                                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = NavyPrimary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = regFirmName,
                                    onValueChange = { regFirmName = it },
                                    label = { Text("Nom du cabinet (ex: Cabinet Ndongo & Associés)") },
                                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = NavyPrimary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = regEmail,
                                    onValueChange = { regEmail = it },
                                    label = { Text("Adresse e-mail professionnelle") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = NavyPrimary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = regPhone,
                                    onValueChange = { regPhone = it },
                                    label = { Text("Téléphone (+237, Cameroun)") },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = NavyPrimary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = regPassword,
                                    onValueChange = { regPassword = it },
                                    label = { Text("Mot de passe (8 car. min)") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NavyPrimary) },
                                    trailingIcon = {
                                        IconButton(onClick = { isRegPasswordVisible = !isRegPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isRegPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    visualTransformation = if (isRegPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Password strength bar
                                val passwordStrength = when {
                                    regPassword.isEmpty() -> 0f
                                    regPassword.length < 6 -> 0.33f
                                    regPassword.length < 8 -> 0.66f
                                    else -> 1f
                                }
                                val strengthColor = when {
                                    passwordStrength <= 0.33f -> ColorLate
                                    passwordStrength <= 0.66f -> ColorReport
                                    else -> ColorSuccess
                                }

                                if (regPassword.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { passwordStrength },
                                        color = strengthColor,
                                        trackColor = SurfaceBorder,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // CGU checkbox
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { acceptTerms = !acceptTerms }
                                ) {
                                    Checkbox(
                                        checked = acceptTerms,
                                        onCheckedChange = { acceptTerms = it }
                                    )
                                    Text(
                                        text = "J'accepte les conditions d'utilisation et la politique de confidentialité (secret professionnel garanti)",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                PillButton(
                                    text = "S'inscrire",
                                    icon = Icons.Default.PersonAdd,
                                    enabled = regUsername.isNotBlank() && regEmail.contains("@") && regPassword.length >= 6 && acceptTerms,
                                    onClick = {
                                        onLoginSuccess(regUsername, regPassword)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Mot de passe oublié (3 états)
    if (showForgotPassword) {
        AlertDialog(
            onDismissRequest = { showForgotPassword = false },
            title = {
                Text(
                    text = "Mot de passe oublié ?",
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    when (forgotState) {
                        ForgotPasswordState.INITIAL -> {
                            Text(
                                text = "Saisissez votre adresse e-mail professionnelle pour recevoir un lien sécurisé de réinitialisation.",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = forgotEmail,
                                onValueChange = { forgotEmail = it },
                                label = { Text("Adresse e-mail") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        ForgotPasswordState.ERREUR -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ColorLateLight, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = ColorLate)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Adresse e-mail invalide. Veuillez saisir l'adresse enregistrée à ce compte.",
                                    color = ColorLate,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        ForgotPasswordState.SUCCES -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ColorSuccessLight, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ColorSuccess)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Veuillez consulter votre boîte mail, vous devrez recevoir un lien via un mail.",
                                    color = ColorSuccess,
                                    fontSize = 13.sp
                                )
                            }
                            if (resendCountdown > 0) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Renvoyer possible dans $resendCountdown s",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (forgotState != ForgotPasswordState.SUCCES) {
                    PillButton(
                        text = "Envoyer Mail",
                        icon = Icons.Default.Send,
                        onClick = {
                            if (forgotEmail.contains("@") && forgotEmail.contains(".")) {
                                forgotState = ForgotPasswordState.SUCCES
                            } else {
                                forgotState = ForgotPasswordState.ERREUR
                            }
                        }
                    )
                } else {
                    TextButton(onClick = { showForgotPassword = false }) {
                        Text("Fermer", color = NavyPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (forgotState != ForgotPasswordState.SUCCES) {
                    TextButton(onClick = { showForgotPassword = false }) {
                        Text("Annuler")
                    }
                }
            }
        )
    }
}
