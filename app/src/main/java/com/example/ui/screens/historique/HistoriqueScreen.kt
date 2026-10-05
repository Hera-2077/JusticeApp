package com.example.ui.screens.historique

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudienceWithDetails
import com.example.pdf.PdfExporter
import com.example.ui.components.JurisdictionBadge
import com.example.ui.components.PillButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.TheKuzanViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoriqueScreen(
    viewModel: TheKuzanViewModel
) {
    val context = LocalContext.current
    val closedAudiences by viewModel.closedAudiences.collectAsState()
    val searchQuery by viewModel.historiqueSearchQuery.collectAsState()
    val jurisdictionFilter by viewModel.historiqueJurisdictionFilter.collectAsState()
    val selectedClosedCase by viewModel.selectedClosedCaseForDetail.collectAsState()
    val user by viewModel.user.collectAsState()

    // Filter closed audiences
    val filteredCases = remember(closedAudiences, searchQuery, jurisdictionFilter) {
        closedAudiences
            .filter { caseWithDetails ->
                val aud = caseWithDetails.audience
                val dec = caseWithDetails.decision
                val matchesQuery = searchQuery.isBlank() ||
                        aud.formattedParties.contains(searchQuery, ignoreCase = true) ||
                        aud.jurisdiction.contains(searchQuery, ignoreCase = true) ||
                        (dec?.decisionText?.contains(searchQuery, ignoreCase = true) == true)

                val matchesJurisdiction = jurisdictionFilter == "TOUS" ||
                        aud.jurisdiction.equals(jurisdictionFilter, ignoreCase = true)

                matchesQuery && matchesJurisdiction
            }
            .sortedByDescending { it.decision?.decisionDate ?: it.audience.nextAudienceDate }
    }

    if (selectedClosedCase != null) {
        // VUE DÉTAILS DE L'AFFAIRE ACHEVÉE (Section 5.4.b)
        BackHandler {
            viewModel.closeClosedCaseDetail()
        }

        ClosedCaseDetailView(
            caseDetails = selectedClosedCase!!,
            user = user,
            onBack = { viewModel.closeClosedCaseDetail() },
            onExportPdf = {
                val pdfFile = PdfExporter.generateAndShareCasePdf(context, selectedClosedCase!!, user)
                if (pdfFile != null) {
                    PdfExporter.sharePdf(context, pdfFile)
                } else {
                    Toast.makeText(context, "Erreur lors de la génération du PDF", Toast.LENGTH_SHORT).show()
                }
            }
        )
    } else {
        // LISTE DES AFFAIRES ACHEVÉES (Section 5.4.a)
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
                                text = "Historique des décisions",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = NavyPrimary
                            )
                            Text(
                                text = "${closedAudiences.size} affaire(s) achevée(s)",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        // Archive status chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(ColorSuccessLight)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Archives",
                                color = ColorSuccess,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Barre de recherche
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setHistoriqueSearchQuery(it) },
                        placeholder = { Text("Rechercher par parties, décision, tribunal...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NavyPrimary) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setHistoriqueSearchQuery("") }) {
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
                            .testTag("historique_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filtres par juridiction
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val jurisdictions = listOf("TOUS", "TGI", "TPI", "CA", "CS", "TCS")
                        items(jurisdictions) { jur ->
                            val isSelected = jurisdictionFilter == jur
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setHistoriqueJurisdictionFilter(jur) },
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
                                )
                            )
                        }
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
                if (filteredCases.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Aucune affaire achevée pour le moment",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Les affaires apparaîtront ici dès qu'une décision finale sera enregistrée dans le Cahier d'audiences.",
                                fontSize = 13.sp,
                                color = TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "Liste des affaires achevées",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NavyPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    items(filteredCases, key = { it.audience.id }) { caseWithDetails ->
                        val aud = caseWithDetails.audience
                        val dec = caseWithDetails.decision
                        val reports = caseWithDetails.reports
                        val durationDays = calculateDurationDays(aud.startDate, dec?.decisionDate ?: aud.nextAudienceDate)

                        // Cartes bleu marine aux textes blancs (selon la maquette)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clickable { viewModel.openClosedCaseDetail(caseWithDetails) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Affaire ${aud.formattedParties}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    JurisdictionBadge(jurisdiction = aud.jurisdiction)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "${aud.subJurisdiction} • Décision rendue le ${dec?.decisionDate ?: "Date non précisée"}",
                                    fontSize = 12.sp,
                                    color = BlueAccentLight
                                )

                                if (dec?.decisionText != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "« ${dec.decisionText} »",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "$durationDays jours de procédure",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFFF3B23E),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "• ${reports.size} renvoi(s)",
                                            fontSize = 11.5.sp,
                                            color = BlueAccentLight
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Voir détails",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}

// VUE DÉTAILLÉE D'UNE AFFAIRE AVEC TABLEAU CHRONOLOGIQUE ET EXPORT PDF (Section 5.4.b & 5.4.c)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosedCaseDetailView(
    caseDetails: AudienceWithDetails,
    user: com.example.data.model.User?,
    onBack: () -> Unit,
    onExportPdf: () -> Unit
) {
    val aud = caseDetails.audience
    val reports = caseDetails.reports.sortedBy { it.recordedAt }
    val dec = caseDetails.decision
    val durationDays = calculateDurationDays(aud.startDate, dec?.decisionDate ?: aud.nextAudienceDate)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Détails de l'affaire",
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = NavyPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgLight)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    PillButton(
                        text = "Exporter les détails en PDF",
                        icon = Icons.Default.Download,
                        onClick = onExportPdf,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_pdf_button")
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
                .padding(horizontal = 16.dp)
        ) {
            // 1. Fiche d'identification
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Affaire : ${aud.formattedParties}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                            JurisdictionBadge(jurisdiction = aud.jurisdiction)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Juridiction : ${aud.jurisdiction} (${aud.subJurisdiction})",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )

                        if (aud.dossierNumber.isNotBlank()) {
                            Text(
                                text = "Rôle N° : ${aud.dossierNumber} • Rôle : ${aud.lawyerRole}",
                                fontSize = 12.5.sp,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = SurfaceBorder)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Indicateurs synthétiques
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            DetailIndicator(title = "1ère Audience", value = aud.startDate)
                            DetailIndicator(title = "Décision", value = dec?.decisionDate ?: "-")
                            DetailIndicator(title = "Durée", value = "$durationDays j")
                            DetailIndicator(title = "Reports", value = "${reports.size}")
                        }
                    }
                }
            }

            // 2. Décision finale
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ColorSuccessLight.copy(alpha = 0.5f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ColorSuccess)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Gavel, contentDescription = null, tint = ColorSuccess, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Décision Finale",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorSuccess
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = dec?.decisionDate ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorSuccess
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = dec?.decisionText ?: "Aucun texte de décision saisi.",
                            fontSize = 13.5.sp,
                            color = TextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // 3. Tableau chronologique des mesures & motifs (Section 5.4.b)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Historique des mesures & motifs",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // En-tête de tableau bleu-gris
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE2E8F0))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Date",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NavyPrimary,
                                modifier = Modifier.width(85.dp)
                            )
                            Text(
                                text = "Mesures & Motifs",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NavyPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (reports.isEmpty() && dec == null) {
                            Text(
                                text = "Aucune audience enregistrée",
                                fontSize = 12.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(14.dp)
                            )
                        } else {
                            var index = 0
                            for (rep in reports) {
                                val isEven = index % 2 == 0
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isEven) Color.White else Color(0xFFF8FAFC))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = rep.reportedAudienceDate,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        modifier = Modifier.width(85.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Renvoi : ${rep.reason}",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "Prochaine audience fixée au ${rep.newAudienceDate}",
                                            fontSize = 11.sp,
                                            color = ColorReport,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Divider(color = SurfaceBorder)
                                index++
                            }

                            // Final line: Audience de décision
                            if (dec != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ColorSuccessLight.copy(alpha = 0.3f))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = dec.decisionDate,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorSuccess,
                                        modifier = Modifier.width(85.dp)
                                    )
                                    Text(
                                        text = "Décision finale prononcée - Dossier clos",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorSuccess,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Notes du conseil
            if (aud.notes.isNotBlank()) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Notes de l'avocat",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = aud.notes,
                            fontSize = 12.5.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun DetailIndicator(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 10.5.sp, color = TextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
    }
}

fun calculateDurationDays(startDateStr: String, endDateStr: String): Long {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val start = sdf.parse(startDateStr)
        val end = sdf.parse(endDateStr)
        if (start != null && end != null) {
            val diff = end.time - start.time
            val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
            if (days < 0) 0 else days
        } else 0
    } catch (e: Exception) {
        0
    }
}
