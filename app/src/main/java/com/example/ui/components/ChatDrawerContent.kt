package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Switch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppSettings
import com.example.data.model.ChatSession
import com.example.data.model.UiState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanTertiary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletSecondary

enum class AppRoute {
    CHAT,
    NOTEBOOK,
    STUDIO,
    HSC_HUB,
    FLASHCARDS,
    PROGRESS,
    STUDY_TIMER,
    ADMISSION,
    FOCUS_LOCK,
    SETTINGS,
    API_CONFIG
}

@Composable
fun ChatDrawerContent(
    sessionsState: UiState<List<ChatSession>>,
    activeSessionId: String?,
    isTemporaryChat: Boolean,
    isOfflineEffective: Boolean,
    currentRoute: AppRoute,
    settings: AppSettings,
    userEmail: String?,
    onNewChat: () -> Unit,
    onStartTemporaryChat: () -> Unit,
    onToggleOfflineMode: (Boolean) -> Unit,
    onSelectSession: (ChatSession) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onSignOut: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<ChatSession?>(null) }
    var renameInput by remember { mutableStateOf("") }

    ModalDrawerSheet(
        modifier = Modifier.width(308.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Study AI Liquid Glass Brand Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                StudyAiLiquidLogo(size = 42.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Study AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Personal Study & Creative Assistant",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // New Chat + Temporary Chat Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        onNewChat()
                        onNavigate(AppRoute.CHAT)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("new_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Chat", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = {
                        onStartTemporaryChat()
                        onNavigate(AppRoute.CHAT)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(46.dp)
                        .testTag("temp_chat_drawer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = "Temporary Chat",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Feature Hubs
            DrawerFooterItem(
                icon = Icons.Default.AutoStories,
                label = "Study AI Notebook & 1GB PDF Podcast",
                selected = currentRoute == AppRoute.NOTEBOOK,
                onClick = { onNavigate(AppRoute.NOTEBOOK) },
                testTag = "drawer_notebook_button"
            )
            DrawerFooterItem(
                icon = Icons.Default.Image,
                label = "Picture & Video Studio",
                selected = currentRoute == AppRoute.STUDIO,
                onClick = { onNavigate(AppRoute.STUDIO) },
                testTag = "drawer_studio_button"
            )
            DrawerFooterItem(
                icon = Icons.Default.School,
                label = "HSC & Admission Lab",
                selected = currentRoute == AppRoute.HSC_HUB,
                onClick = { onNavigate(AppRoute.HSC_HUB) },
                testTag = "drawer_hsc_button"
            )
            DrawerFooterItem(
                icon = Icons.Default.Style,
                label = "Study Flashcards",
                selected = currentRoute == AppRoute.FLASHCARDS,
                onClick = { onNavigate(AppRoute.FLASHCARDS) },
                testTag = "drawer_flashcards_button"
            )
            DrawerFooterItem(
                icon = Icons.Default.Analytics,
                label = "Progress Dashboard",
                selected = currentRoute == AppRoute.PROGRESS,
                onClick = { onNavigate(AppRoute.PROGRESS) },
                testTag = "drawer_progress_button"
            )
            DrawerFooterItem(
                icon = Icons.Default.HourglassTop,
                label = "Study Timer (Pomodoro)",
                selected = currentRoute == AppRoute.STUDY_TIMER,
                onClick = { onNavigate(AppRoute.STUDY_TIMER) },
                testTag = "drawer_study_timer_button"
            )
            DrawerFooterItem(
                icon = Icons.Default.EventAvailable,
                label = "Admission Circulars & Links",
                selected = currentRoute == AppRoute.ADMISSION,
                onClick = { onNavigate(AppRoute.ADMISSION) },
                testTag = "drawer_admission_button"
            )
            DrawerFooterItem(
                icon = Icons.Default.Timer,
                label = "App Time Limit & Unlock",
                selected = currentRoute == AppRoute.FOCUS_LOCK,
                onClick = { onNavigate(AppRoute.FOCUS_LOCK) },
                testTag = "drawer_focus_button"
            )

            // Offline Mode Quick Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = if (isOfflineEffective) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (settings.offlineModeEnabled) "Offline Mode (On)" else "Offline Mode",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = settings.offlineModeEnabled,
                    onCheckedChange = onToggleOfflineMode,
                    modifier = Modifier.testTag("drawer_offline_switch")
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Search History
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search chats...", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search chats",
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_chats_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "RECENT CHATS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )

            // Session List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when (sessionsState) {
                    is UiState.Loading -> {
                        item {
                            Text(
                                text = "Loading conversations...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    is UiState.Error -> {
                        item {
                            Text(
                                text = sessionsState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    is UiState.Success -> {
                        val filtered = sessionsState.data.filter {
                            searchQuery.isBlank() ||
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                it.lastMessagePreview.contains(searchQuery, ignoreCase = true)
                        }
                        if (filtered.isEmpty()) {
                            item {
                                Text(
                                    text = if (searchQuery.isBlank()) "Start your first chat below!" else "No matching chats.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            items(filtered, key = { it.id }) { session ->
                                val isSelected =
                                    !isTemporaryChat && currentRoute == AppRoute.CHAT && session.id == activeSessionId
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    } else {
                                        Color.Transparent
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onSelectSession(session)
                                            onNavigate(AppRoute.CHAT)
                                        }
                                        .testTag("session_item_${session.id}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChatBubbleOutline,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = session.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                renameTarget = session
                                                renameInput = session.title
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Rename conversation",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteSession(session.id) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete conversation",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            // Settings & API Config
            DrawerFooterItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                selected = currentRoute == AppRoute.SETTINGS,
                onClick = { onNavigate(AppRoute.SETTINGS) },
                testTag = "drawer_settings_button"
            )

            val keyHealthy = !settings.useCustomApiKey || !settings.maskedCustomApiKey.isNullOrBlank()
            DrawerFooterItem(
                icon = Icons.Default.Key,
                label = "API Configuration & Key Link",
                selected = currentRoute == AppRoute.API_CONFIG,
                onClick = { onNavigate(AppRoute.API_CONFIG) },
                testTag = "drawer_api_config_button",
                trailing = {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (keyHealthy) EmeraldSuccess else AmberWarning)
                    )
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Account + Sign Out
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = userEmail ?: "Signed in",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign out",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Conversation") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    label = { Text("Title") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        renameTarget?.let { onRenameSession(it.id, renameInput) }
                        renameTarget = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DrawerFooterItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    trailing: (@Composable () -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        } else {
            Color.Transparent
        },
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            trailing?.invoke()
        }
    }
}
