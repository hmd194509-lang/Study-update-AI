package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoCameraBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.data.model.ImageGenUiState
import com.example.data.model.VideoGenUiState
import com.example.ui.theme.CyanTertiary
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletSecondary
import java.io.File
import java.io.FileOutputStream

private val IMAGE_PRESETS = listOf(
    "Labeled diagram of a Human Heart with blood flow arrows on crisp white background",
    "Physics Projectile Motion trajectory diagram showing angle, velocity components, max height H, range R",
    "Benzene resonance structure and sp2 orbital hybridization chemistry textbook diagram",
    "Futuristic sunlit library study desk with glowing holographic physics equations, digital art"
)

private val VIDEO_PRESETS = listOf(
    "3D educational animation of solar system planets orbiting the sun smoothly, crisp studio lighting",
    "Cinematic slow-motion chemistry laboratory crystal growth in glowing blue glass beaker",
    "Peaceful aesthetic study room with rain on window and warm desk lamp light"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreativeStudioScreen(
    initialPrompt: String = "",
    initialTab: Int = 0,
    imageGenState: ImageGenUiState,
    videoGenState: VideoGenUiState,
    onBack: () -> Unit,
    onOpenApiConfig: () -> Unit,
    onGeneratePicture: (String, Bitmap?) -> Unit,
    onGenerateVideo: (Context, String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var imagePrompt by remember(initialPrompt) { mutableStateOf(initialPrompt) }
    var videoPrompt by remember { mutableStateOf("") }
    var referenceBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val refImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    referenceBitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {
            }
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
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Creative Media Studio")
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("studio_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to chat"
                    )
                }
            }
        )

        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Picture Skill") },
                icon = { Icon(Icons.Default.Image, contentDescription = null) },
                modifier = Modifier.testTag("studio_tab_picture")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Video Skill") },
                icon = { Icon(Icons.Default.VideoCameraBack, contentDescription = null) },
                modifier = Modifier.testTag("studio_tab_video")
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .widthIn(max = 680.dp)
        ) {
            if (selectedTab == 0) {
                // Picture Generation Skill
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Generate or Edit Pictures & HSC Diagrams",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Powered by Gemini Nano Banana 2.1. Create scientific diagrams, illustrations, or upload a photo to edit.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = imagePrompt,
                            onValueChange = { imagePrompt = it },
                            label = { Text("Describe the image or diagram") },
                            placeholder = { Text("e.g., Labeled Human Heart diagram or futuristic study space...") },
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("picture_prompt_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    refImageLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (referenceBitmap == null) "Reference Image" else "Change Image")
                            }

                            if (referenceBitmap != null) {
                                IconButton(onClick = { referenceBitmap = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear reference image")
                                }
                            }
                        }

                        if (referenceBitmap != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Image(
                                bitmap = referenceBitmap!!.asImageBitmap(),
                                contentDescription = "Reference image preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onGeneratePicture(imagePrompt, referenceBitmap) },
                            enabled = imagePrompt.isNotBlank() && imageGenState !is ImageGenUiState.Loading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("generate_picture_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (imageGenState is ImageGenUiState.Loading) "Generating Picture..." else "Generate Picture"
                            )
                        }
                    }
                }

                // Quick diagram / art prompts
                Text(
                    text = "QUICK IDEAS & HSC DIAGRAMS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IMAGE_PRESETS.forEach { preset ->
                        AssistChip(
                            onClick = { imagePrompt = preset },
                            label = {
                                Text(
                                    text = preset.take(44) + if (preset.length > 44) "..." else "",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }

                when (val state = imageGenState) {
                    is ImageGenUiState.Loading -> {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp)
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Synthesizing high-res image (may take 30–60s)...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    is ImageGenUiState.Success -> {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Image(
                                    bitmap = state.bitmap.asImageBitmap(),
                                    contentDescription = state.prompt,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = state.prompt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { shareBitmap(context, state.bitmap) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share / Save Picture")
                                }
                            }
                        }
                    }
                    is ImageGenUiState.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                    ImageGenUiState.Idle -> Unit
                }
            } else {
                // Video Generation Skill
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            IndigoPrimary.copy(alpha = 0.14f),
                                            VioletSecondary.copy(alpha = 0.14f),
                                            CyanTertiary.copy(alpha = 0.14f)
                                        )
                                    )
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Gemini Veo Video Skill generates real MP4 clips using your Gemini API key (long-running operation, typically 45–120s).",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = videoPrompt,
                            onValueChange = { videoPrompt = it },
                            label = { Text("Describe the video scene") },
                            placeholder = { Text("e.g., 3D animation of planetary motion in space...") },
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("video_prompt_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onGenerateVideo(context, videoPrompt) },
                            enabled = videoPrompt.isNotBlank() && videoGenState !is VideoGenUiState.Loading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("generate_video_button")
                        ) {
                            Icon(Icons.Default.VideoCameraBack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (videoGenState is VideoGenUiState.Loading) "Rendering Video..." else "Generate Video"
                            )
                        }
                    }
                }

                Text(
                    text = "VIDEO IDEAS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VIDEO_PRESETS.forEach { preset ->
                        AssistChip(
                            onClick = { videoPrompt = preset },
                            label = {
                                Text(
                                    text = preset.take(44) + if (preset.length > 44) "..." else "",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }

                when (val state = videoGenState) {
                    is VideoGenUiState.Loading -> {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp)
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = state.statusMessage,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    is VideoGenUiState.Success -> {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                AndroidView(
                                    factory = { ctx ->
                                        VideoView(ctx).apply {
                                            setVideoPath(state.videoFilePath)
                                            val controller = MediaController(ctx)
                                            controller.setAnchorView(this)
                                            setMediaController(controller)
                                            setOnPreparedListener { mp ->
                                                mp.isLooping = true
                                                start()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { shareVideoFile(context, File(state.videoFilePath)) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share / Save MP4")
                                }
                            }
                        }
                    }
                    is VideoGenUiState.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (state.needsApiKey) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(onClick = onOpenApiConfig) {
                                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Configure Gemini API Key")
                                    }
                                }
                            }
                        }
                    }
                    VideoGenUiState.Idle -> Unit
                }
            }
        }
    }
}

private fun shareBitmap(context: Context, bitmap: Bitmap) {
    val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
    val file = File(dir, "study_update_ai_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Picture"))
}

private fun shareVideoFile(context: Context, file: File) {
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Video"))
}
