package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.GoogleSignInButton
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.StudyAiLiquidLogo
import com.example.ui.theme.CyanTertiary
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletSecondary

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val auroraTransition = rememberInfiniteTransition(label = "auth_aurora")
    val drift by auroraTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            Color(0xFF050816),
                            Color(0xFF0B122B),
                            Color(0xFF080D1F)
                        )
                    } else {
                        listOf(
                            Color(0xFFEEF2FF),
                            Color(0xFFF5F3FF),
                            Color(0xFFE0F7FA)
                        )
                    }
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Ambient Liquid Aurora Glow Orbs
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        IndigoPrimary.copy(alpha = if (isDark) 0.42f else 0.24f),
                        Color.Transparent
                    ),
                    center = Offset(w * (0.20f + 0.12f * drift), h * (0.18f + 0.05f * drift)),
                    radius = w * 0.65f
                ),
                center = Offset(w * (0.20f + 0.12f * drift), h * (0.18f + 0.05f * drift)),
                radius = w * 0.65f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CyanTertiary.copy(alpha = if (isDark) 0.34f else 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(w * (0.85f - 0.14f * drift), h * (0.44f - 0.06f * drift)),
                    radius = w * 0.58f
                ),
                center = Offset(w * (0.85f - 0.14f * drift), h * (0.44f - 0.06f * drift)),
                radius = w * 0.58f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        VioletSecondary.copy(alpha = if (isDark) 0.32f else 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(w * (0.42f + 0.10f * drift), h * 0.80f),
                    radius = w * 0.62f
                ),
                center = Offset(w * (0.42f + 0.10f * drift), h * 0.80f),
                radius = w * 0.62f
            )
        }

        // Main Centered Liquid Glass Login Card
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.Center)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            LiquidGlassSurface(
                shape = RoundedCornerShape(34.dp),
                accentGlow = true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 26.dp, vertical = 34.dp)
                ) {
                    // Decorative Glass Halo Ring + Study AI Liquid Logo
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(106.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        CyanTertiary.copy(alpha = 0.26f),
                                        VioletSecondary.copy(alpha = 0.14f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.55f),
                                        CyanTertiary.copy(alpha = 0.30f)
                                    )
                                ),
                                shape = CircleShape
                            )
                    ) {
                        StudyAiLiquidLogo(size = 76.dp)
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Decorative subtle wave dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(IndigoPrimary, VioletSecondary, CyanTertiary).forEach { dotColor ->
                            Box(
                                modifier = Modifier
                                    .size(width = 22.dp, height = 5.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(dotColor.copy(alpha = 0.85f))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Welcome to Study AI",
                        style = MaterialTheme.typography.headlineLarge.merge(
                            TextStyle(
                                brush = Brush.linearGradient(
                                    colors = if (isDark) {
                                        listOf(Color.White, CyanTertiary, VioletSecondary)
                                    } else {
                                        listOf(IndigoPrimary, VioletSecondary, CyanTertiary)
                                    }
                                )
                            )
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    GoogleSignInButton(
                        onAuthSuccess = {
                            errorMessage = null
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            errorMessage = msg
                        }
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Footer strictly in English as requested
            LiquidGlassSurface(
                shape = RoundedCornerShape(999.dp),
                tintAlpha = if (isDark) 0.42f else 0.72f,
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Text(
                    text = "Developer & CEO: MD Taimur Rahman",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 0.4.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isDark) Color.White.copy(alpha = 0.92f) else IndigoPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
    }
}
