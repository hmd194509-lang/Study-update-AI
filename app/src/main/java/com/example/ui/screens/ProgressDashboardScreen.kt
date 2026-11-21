package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLimitRule
import com.example.data.model.FlashcardSubject
import com.example.data.model.StudyFlashcard
import com.example.data.model.StudyMode
import com.example.data.model.StudyProgressStats
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanTertiary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.VioletSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressDashboardScreen(
    stats: StudyProgressStats,
    flashcards: List<StudyFlashcard>,
    appLimits: List<AppLimitRule>,
    totalSessionsCount: Int,
    onBack: () -> Unit,
    onOpenFlashcards: () -> Unit,
    onOpenHscLab: () -> Unit,
    onResetStats: () -> Unit
) {
    BackHandler { onBack() }

    var showResetConfirm by remember { mutableStateOf(false) }

    val masteredCards = flashcards.count { it.isMastered }
    val totalCards = flashcards.size.coerceAtLeast(1)
    val masteryPct = ((masteredCards * 100f) / totalCards).toInt()
    val masteryFraction = masteredCards.toFloat() / totalCards

    val last7Days = remember(stats.dailyActivityCounts) {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFmt = SimpleDateFormat("EEE", Locale.US)
        val cal = Calendar.getInstance()
        (6 downTo 0).map { offset ->
            val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val key = fmt.format(c.time)
            val label = labelFmt.format(c.time).take(2)
            Triple(key, label, stats.dailyActivityCounts[key] ?: 0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Progress Dashboard")
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("progress_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to chat"
                    )
                }
            },
            actions = {
                TextButton(onClick = { showResetConfirm = true }) {
                    Text("Reset")
                }
            }
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .widthIn(max = 720.dp)
        ) {
            // Hero Streak + Mastery Ring Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(IndigoPrimary, VioletSecondary)))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${stats.studyStreakDays.coerceAtLeast(if (stats.totalQuestionsAsked > 0 || stats.flashcardsReviewed > 0) 1 else 0)} Day Streak",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$masteredCards of ${flashcards.size} flashcards mastered\n${stats.totalQuestionsAsked} AI questions • $totalSessionsCount saved chats",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    // Canvas Mastery Ring
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(92.dp)
                    ) {
                        Canvas(modifier = Modifier.size(84.dp)) {
                            val strokeWidth = 10.dp.toPx()
                            drawArc(
                                color = Color.White.copy(alpha = 0.22f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = CyanTertiary,
                                startAngle = -90f,
                                sweepAngle = 360f * masteryFraction,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = "$masteryPct%",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Key Metric Grid
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricTile(
                    icon = Icons.Default.Style,
                    title = "Flashcard Reviews",
                    value = stats.flashcardsReviewed.toString(),
                    accent = EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    icon = Icons.Default.School,
                    title = "HSC Topics Studied",
                    value = stats.hscTopicsStudied.toString(),
                    accent = CyanTertiary,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    icon = Icons.Default.CloudOff,
                    title = "Offline Answers",
                    value = stats.offlineQuestionsAnswered.toString(),
                    accent = IndigoPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            // 7-Day Activity Bar Chart (Canvas)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "7-Day Study Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val maxVal = (last7Days.maxOfOrNull { it.third } ?: 1).coerceAtLeast(1)
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant
                    val barColor = IndigoPrimary

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        val count = last7Days.size
                        val slotWidth = size.width / count
                        val barWidth = (slotWidth * 0.45f).coerceAtMost(28.dp.toPx())
                        last7Days.forEachIndexed { idx, (_, _, value) ->
                            val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                            drawRoundRect(
                                color = trackColor,
                                topLeft = Offset(x, 0f),
                                size = Size(barWidth, size.height),
                                cornerRadius = CornerRadius(10f, 10f)
                            )
                            val barHeight = (value.toFloat() / maxVal) * size.height
                            if (barHeight > 0f) {
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(x, size.height - barHeight),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(10f, 10f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        last7Days.forEach { (_, dayLabel, value) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = value.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = dayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Subject Mastery & Questions Breakdown
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Subject Progress (HSC & Admission)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val subjects = listOf(
                        Triple(FlashcardSubject.PHYSICS, StudyMode.HSC_PHYSICS.name, IndigoPrimary),
                        Triple(FlashcardSubject.CHEMISTRY, StudyMode.HSC_CHEMISTRY.name, VioletSecondary),
                        Triple(FlashcardSubject.BIOLOGY, StudyMode.HSC_BIOLOGY.name, EmeraldSuccess),
                        Triple(FlashcardSubject.MATH, StudyMode.HSC_MATH.name, CyanTertiary),
                        Triple(FlashcardSubject.GENERAL, StudyMode.GENERAL.name, AmberWarning)
                    )

                    subjects.forEach { (fcSubject, modeKey, accent) ->
                        val subjCards = flashcards.filter { it.subject == fcSubject }
                        val subjMastered = subjCards.count { it.isMastered }
                        val qCount = stats.subjectQuestionCounts[modeKey] ?: 0
                        val frac = if (subjCards.isEmpty()) 0f else subjMastered.toFloat() / subjCards.size

                        Column {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${fcSubject.label} (${fcSubject.bnLabel})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "$subjMastered/${subjCards.size} cards • $qCount Qs",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { frac },
                                color = accent,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(999.dp))
                            )
                        }
                    }
                }
            }

            // App Focus & Unlock Discipline Summary
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Digital Focus & Unlock Discipline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Tracked Apps: ${appLimits.size} (Locked: ${appLimits.count { it.isLocked }})\n" +
                            "• Approved Unlock Requests: ${stats.unlockRequestsApproved}\n" +
                            "• Rejected Weak/Useless Requests: ${stats.unlockRequestsRejected}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Quick Study Navigation
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onOpenFlashcards,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Practice Flashcards")
                }
                OutlinedButton(
                    onClick = onOpenHscLab,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("HSC & Admission Lab")
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Study Progress?") },
            text = { Text("This clears your study counters and streak, while keeping your saved chats and flashcards.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetStats()
                        showResetConfirm = false
                    }
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MetricTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
