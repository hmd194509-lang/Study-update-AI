package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import com.example.data.model.FlashcardSubject
import com.example.data.model.FlashcardType
import com.example.data.model.StudyFlashcard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanTertiary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FlashcardsScreen(
    flashcards: List<StudyFlashcard>,
    isOffline: Boolean,
    isGeneratingAiDeck: Boolean,
    onBack: () -> Unit,
    onReviewCard: (String, Boolean) -> Unit,
    onAddCustomCard: (FlashcardSubject, String, String, String) -> Unit,
    onGenerateAiCards: (FlashcardSubject, FlashcardType, String, Int, String, (Boolean, String) -> Unit) -> Unit,
    onDeleteCard: (String) -> Unit
) {
    BackHandler { onBack() }

    var selectedSubject by remember { mutableStateOf<FlashcardSubject?>(null) }
    var selectedType by remember { mutableStateOf<FlashcardType?>(null) }
    var unmasteredOnly by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var shuffleSeed by remember { mutableIntStateOf(0) }

    var showAddDialog by remember { mutableStateOf(false) }
    var newSubject by remember { mutableStateOf(FlashcardSubject.PHYSICS) }
    var newFront by remember { mutableStateOf("") }
    var newBack by remember { mutableStateOf("") }
    var newNote by remember { mutableStateOf("") }

    var showAiGenDialog by remember { mutableStateOf(false) }
    var aiSubject by remember { mutableStateOf(FlashcardSubject.PHYSICS) }
    var aiType by remember { mutableStateOf(FlashcardType.MIXED) }
    var aiCount by remember { mutableIntStateOf(10) }
    var aiTopic by remember { mutableStateOf("") }
    var aiCustomInstr by remember { mutableStateOf("") }
    var aiStatusMsg by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val visibleCards = remember(flashcards, selectedSubject, selectedType, unmasteredOnly, shuffleSeed) {
        val base = flashcards.filter { card ->
            (selectedSubject == null || card.subject == selectedSubject) &&
                (selectedType == null || card.type == selectedType) &&
                (!unmasteredOnly || !card.isMastered)
        }
        if (shuffleSeed > 0) base.shuffled(kotlin.random.Random(shuffleSeed)) else base
    }

    val safeIndex = if (visibleCards.isEmpty()) 0 else currentIndex.coerceIn(0, visibleCards.lastIndex)
    val activeCard = visibleCards.getOrNull(safeIndex)
    val masteredCount = flashcards.count { it.isMastered }
    val masteryFraction = if (flashcards.isEmpty()) 0f else masteredCount.toFloat() / flashcards.size

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 360),
        label = "flashcard_flip"
    )

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
                        imageVector = Icons.Default.Style,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Study Flashcards")
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("flashcards_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to chat"
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        shuffleSeed++
                        currentIndex = 0
                        isFlipped = false
                    }
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = "Shuffle deck")
                }
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_flashcard_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add flashcard")
                }
            }
        )

        // Deck Mastery Progress Bar + Unlimited AI Generate Action
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .widthIn(max = 960.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Mastered: $masteredCount / ${flashcards.size} cards",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldSuccess
                )
                OutlinedButton(
                    onClick = {
                        aiStatusMsg = null
                        showAiGenDialog = true
                    },
                    enabled = !isGeneratingAiDeck && !isOffline,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isGeneratingAiDeck) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isOffline) "AI Gen (Offline)" else "AI Unlimited")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { masteryFraction },
                color = EmeraldSuccess,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
            )

            aiStatusMsg?.let { (ok, msg) ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ok) EmeraldSuccess else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subject Filters
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    selected = selectedSubject == null,
                    onClick = {
                        selectedSubject = null
                        currentIndex = 0
                        isFlipped = false
                    },
                    label = { Text("All (${flashcards.size})") }
                )
                FlashcardSubject.entries.forEach { subj ->
                    val count = flashcards.count { it.subject == subj }
                    FilterChip(
                        selected = selectedSubject == subj,
                        onClick = {
                            selectedSubject = subj
                            currentIndex = 0
                            isFlipped = false
                        },
                        label = { Text("${subj.label} ($count)") }
                    )
                }
                FilterChip(
                    selected = unmasteredOnly,
                    onClick = {
                        unmasteredOnly = !unmasteredOnly
                        currentIndex = 0
                        isFlipped = false
                    },
                    label = { Text("Practice Needed") }
                )
            }

            // Type Filters (Formulas, Definitions, MCQ Traps, CQ, Reactions, Mixed)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = {
                        selectedType = null
                        currentIndex = 0
                        isFlipped = false
                    },
                    label = { Text("All Types") }
                )
                FlashcardType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = if (selectedType == type) null else type
                            currentIndex = 0
                            isFlipped = false
                        },
                        label = { Text(type.label) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .widthIn(max = 960.dp)
        ) {
            // Interactive Flip Card Stage
            item {
                if (activeCard == null) {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "All filtered flashcards are mastered!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(onClick = { unmasteredOnly = false }) {
                                Text("Show All Cards")
                            }
                        }
                    }
                } else {
                    // Flippable Flashcard
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 240.dp)
                            .graphicsLayer {
                                rotationY = rotation
                                cameraDistance = 12f * density
                            }
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { isFlipped = !isFlipped }
                            .testTag("active_flashcard")
                    ) {
                        val showingBack = rotation > 90f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 240.dp)
                                .background(
                                    if (!showingBack) {
                                        Brush.linearGradient(listOf(IndigoPrimary, VioletSecondary))
                                    } else {
                                        Brush.linearGradient(listOf(Color(0xFF0F2F2A), Color(0xFF0F172A)))
                                    }
                                )
                                .graphicsLayer {
                                    if (showingBack) rotationY = 180f
                                }
                                .padding(20.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 200.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Text(
                                            text = if (!showingBack) {
                                                "${activeCard.subject.label} • ${activeCard.type.label} (${safeIndex + 1}/${visibleCards.size})"
                                            } else {
                                                "${activeCard.subject.label} • ANSWER & FORMULA"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    if (activeCard.isMastered) {
                                        Surface(
                                            color = EmeraldSuccess.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(999.dp)
                                        ) {
                                            Text(
                                                text = "Mastered",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (!showingBack) {
                                    Text(
                                        text = activeCard.front,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = activeCard.back,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                        if (activeCard.admissionNote.isNotBlank()) {
                                            Surface(
                                                color = CyanTertiary.copy(alpha = 0.18f),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = activeCard.admissionNote,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFFE0F2FE),
                                                    modifier = Modifier.padding(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = if (!showingBack) "Tap card to reveal answer" else "Tap card to flip back",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Review + Navigation Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                if (visibleCards.isNotEmpty()) {
                                    currentIndex = if (safeIndex == 0) visibleCards.lastIndex else safeIndex - 1
                                    isFlipped = false
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous card")
                        }

                        OutlinedButton(
                            onClick = {
                                onReviewCard(activeCard.id, false)
                                if (visibleCards.size > 1) {
                                    currentIndex = (safeIndex + 1) % visibleCards.size
                                }
                                isFlipped = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = AmberWarning)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Practice")
                        }

                        Button(
                            onClick = {
                                onReviewCard(activeCard.id, true)
                                if (visibleCards.size > 1 && !unmasteredOnly) {
                                    currentIndex = (safeIndex + 1) % visibleCards.size
                                } else {
                                    currentIndex = 0
                                }
                                isFlipped = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("flashcard_mastered_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Got It")
                        }

                        IconButton(
                            onClick = {
                                if (visibleCards.isNotEmpty()) {
                                    currentIndex = (safeIndex + 1) % visibleCards.size
                                    isFlipped = false
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next card")
                        }
                    }
                }
            }

            // Deck List Header
            item {
                Text(
                    text = "ALL CARDS IN THIS DECK (${visibleCards.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            itemsIndexed(visibleCards, key = { _, card -> card.id }) { idx, card ->
                val isSelected = idx == safeIndex
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            currentIndex = idx
                            isFlipped = false
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${card.subject.label} • ${card.type.label} • ${if (card.isMastered) "Mastered" else "Practicing"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (card.isMastered) EmeraldSuccess else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = card.front,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (card.isCustom) {
                            IconButton(onClick = { onDeleteCard(card.id) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete custom flashcard",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Custom Flashcard Dialog (Works 100% Offline)
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Create Study Flashcard") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FlashcardSubject.entries.forEach { subj ->
                            FilterChip(
                                selected = newSubject == subj,
                                onClick = { newSubject = subj },
                                label = { Text(subj.label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = newFront,
                        onValueChange = { newFront = it },
                        label = { Text("Front (Question / Term)") },
                        minLines = 2
                    )
                    OutlinedTextField(
                        value = newBack,
                        onValueChange = { newBack = it },
                        label = { Text("Back (Answer / Formula)") },
                        minLines = 2
                    )
                    OutlinedTextField(
                        value = newNote,
                        onValueChange = { newNote = it },
                        label = { Text("Admission Shortcut / Tip (Optional)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFront.isNotBlank() && newBack.isNotBlank()) {
                            onAddCustomCard(newSubject, newFront, newBack, newNote)
                            newFront = ""
                            newBack = ""
                            newNote = ""
                            showAddDialog = false
                        }
                    },
                    enabled = newFront.isNotBlank() && newBack.isNotBlank()
                ) { Text("Save Card") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Unlimited Topic & Type-Based AI Flashcard Generator Dialog
    if (showAiGenDialog) {
        AlertDialog(
            onDismissRequest = { showAiGenDialog = false },
            title = { Text("AI Unlimited Flashcards") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Subject",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FlashcardSubject.entries.forEach { subj ->
                            FilterChip(
                                selected = aiSubject == subj,
                                onClick = { aiSubject = subj },
                                label = { Text(subj.label) }
                            )
                        }
                    }

                    Text(
                        text = "Card Type (টাইপ)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FlashcardType.entries.forEach { type ->
                            FilterChip(
                                selected = aiType == type,
                                onClick = { aiType = type },
                                label = { Text(type.label) }
                            )
                        }
                    }

                    Text(
                        text = "Batch Size (Generate unlimited batches anytime)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5, 10, 15, 20, 30).forEach { qty ->
                            FilterChip(
                                selected = aiCount == qty,
                                onClick = { aiCount = qty },
                                label = { Text("$qty Cards") }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = aiTopic,
                        onValueChange = { aiTopic = it },
                        label = { Text("Chapter or Topic (টপিক)") },
                        placeholder = { Text("e.g. ভেক্টর, জৈব রসায়ন, বা DNA Replication") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = aiCustomInstr,
                        onValueChange = { aiCustomInstr = it },
                        label = { Text("Custom Prompt (কীভাবে বানাতে চান - ঐচ্ছিক)") },
                        placeholder = { Text("e.g. মেডিকেল MCQ ট্র্যাপ + মনে রাখার টেকনিক দাও") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val topicClean = aiTopic.trim()
                        if (topicClean.isNotEmpty()) {
                            showAiGenDialog = false
                            onGenerateAiCards(
                                aiSubject,
                                aiType,
                                topicClean,
                                aiCount,
                                aiCustomInstr.trim()
                            ) { ok, msg ->
                                aiStatusMsg = ok to msg
                                if (ok) {
                                    aiTopic = ""
                                    aiCustomInstr = ""
                                }
                            }
                        }
                    },
                    enabled = aiTopic.isNotBlank()
                ) { Text("Generate $aiCount Cards") }
            },
            dismissButton = {
                TextButton(onClick = { showAiGenDialog = false }) { Text("Cancel") }
            }
        )
    }
}
