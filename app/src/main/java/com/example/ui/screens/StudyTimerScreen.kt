package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FlashcardSubject
import com.example.data.model.StudyTimerSession
import com.example.ui.theme.CyanTertiary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.VioletSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudyTimerScreen(
    selectedSubject: FlashcardSubject,
    totalSeconds: Int,
    remainingSeconds: Int,
    isRunning: Boolean,
    isBreakMode: Boolean,
    studySessions: List<StudyTimerSession>,
    onBack: () -> Unit,
    onSelectSubject: (FlashcardSubject) -> Unit,
    onSelectDurationMinutes: (Int, Boolean) -> Unit,
    onToggleStartPause: () -> Unit,
    onResetTimer: () -> Unit,
    onCompleteEarly: () -> Unit
) {
    BackHandler { onBack() }

    var customMinutes by remember { mutableIntStateOf(45) }

    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60
    val timeFormatted = "%02d:%02d".format(mins, secs)
    val progress = if (totalSeconds > 0) {
        (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val totalFocusedMinutes = studySessions.sumOf { it.durationMinutes }

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
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Study Timer")
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("study_timer_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to chat"
                    )
                }
            }
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .widthIn(max = 680.dp)
        ) {
            // Circular Study Timer Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Surface(
                        color = if (isBreakMode) EmeraldSuccess.copy(alpha = 0.16f) else IndigoPrimary.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = if (isBreakMode) "Break Mode" else "Focus: ${selectedSubject.label} (${selectedSubject.bnLabel})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isBreakMode) EmeraldSuccess else IndigoPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Canvas Countdown Ring
                    val ringTrack = MaterialTheme.colorScheme.surfaceVariant
                    val ringColor = if (isBreakMode) EmeraldSuccess else IndigoPrimary

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(210.dp)
                    ) {
                        Canvas(modifier = Modifier.size(198.dp)) {
                            val stroke = 14.dp.toPx()
                            drawArc(
                                color = ringTrack,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = timeFormatted,
                                style = MaterialTheme.typography.displayMedium.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${totalSeconds / 60} min target",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onToggleStartPause,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) VioletSecondary else IndigoPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("study_timer_start_pause")
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isRunning) "Pause" else "Start Study")
                        }

                        OutlinedButton(
                            onClick = onResetTimer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset timer")
                        }

                        if (!isBreakMode && remainingSeconds < totalSeconds) {
                            OutlinedButton(
                                onClick = onCompleteEarly,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Finish and log", tint = EmeraldSuccess)
                            }
                        }
                    }
                }
            }

            // Preset Modes & Subject Selection
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Study Subject",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlashcardSubject.entries.forEach { subj ->
                            FilterChip(
                                selected = selectedSubject == subj,
                                onClick = { onSelectSubject(subj) },
                                label = { Text("${subj.label} (${subj.bnLabel})") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Presets (Pomodoro & Deep Study)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("25m Pomodoro", 25, false),
                            Triple("50m Deep Focus", 50, false),
                            Triple("90m Exam Mock", 90, false),
                            Triple("5m Break", 5, true),
                            Triple("10m Break", 10, true)
                        ).forEach { (label, minsVal, breakFlag) ->
                            FilterChip(
                                selected = totalSeconds == minsVal * 60 && isBreakMode == breakFlag,
                                onClick = { onSelectDurationMinutes(minsVal, breakFlag) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Custom Focus Duration: $customMinutes min",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = customMinutes.toFloat(),
                        onValueChange = { customMinutes = it.roundToInt() },
                        valueRange = 5f..180f
                    )
                    OutlinedButton(
                        onClick = { onSelectDurationMinutes(customMinutes, false) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Custom $customMinutes min Timer")
                    }
                }
            }

            // Completed Study Sessions Log
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Completed Study Sessions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$totalFocusedMinutes min total",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (studySessions.isEmpty()) {
                        Text(
                            text = "Start a Pomodoro or Deep Focus block above to record your study hours.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val fmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.US) }
                        studySessions.take(10).forEach { entry ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${entry.subject.label} (${entry.subject.bnLabel}) • ${entry.durationMinutes} min",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = fmt.format(Date(entry.completedAtMs)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
