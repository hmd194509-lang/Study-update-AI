package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanTertiary
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletSecondary

/**
 * Frosted Liquid Glass surface container with translucent fill, luminous top-left specular
 * rim gradient, and soft elevation depth.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    tintAlpha: Float? = null,
    accentGlow: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val baseFillAlpha = tintAlpha ?: if (isDark) 0.58f else 0.78f
    val glassBrush = if (accentGlow) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = (baseFillAlpha + 0.08f).coerceAtMost(0.92f)),
                IndigoPrimary.copy(alpha = if (isDark) 0.22f else 0.10f),
                CyanTertiary.copy(alpha = if (isDark) 0.16f else 0.08f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = (baseFillAlpha + 0.10f).coerceAtMost(0.95f)),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = baseFillAlpha)
            )
        )
    }

    val specularBorder = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.42f else 0.88f),
            CyanTertiary.copy(alpha = if (isDark) 0.34f else 0.42f),
            VioletSecondary.copy(alpha = if (isDark) 0.28f else 0.28f),
            Color.White.copy(alpha = if (isDark) 0.14f else 0.45f)
        )
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isDark) 10.dp else 8.dp,
                shape = shape,
                ambientColor = IndigoPrimary.copy(alpha = 0.22f),
                spotColor = CyanTertiary.copy(alpha = 0.22f)
            )
            .clip(shape)
            .background(glassBrush)
            .border(width = 1.15.dp, brush = specularBorder, shape = shape),
        content = content
    )
}

/**
 * Custom Liquid Glass Study AI Emblem with layered aurora glass orbs & specular sheen.
 */
@Composable
fun StudyAiLiquidLogo(
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val shape = CircleShape
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = CyanTertiary.copy(alpha = 0.45f),
                spotColor = VioletSecondary.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        IndigoPrimary,
                        VioletSecondary,
                        CyanTertiary
                    )
                )
            )
            .border(
                width = 1.4.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        CyanTertiary.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.25f)
                    )
                ),
                shape = shape
            )
    ) {
        // Specular liquid reflection inside the orb
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.48f),
                        Color.Transparent
                    ),
                    center = Offset(this.size.width * 0.32f, this.size.height * 0.26f),
                    radius = this.size.minDimension * 0.45f
                )
            )
        }
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "Study AI Liquid Logo",
            tint = Color.White,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

/**
 * Sleek Liquid Glass rounded badge for header / top-bar option icons.
 */
@Composable
fun LiquidGlassIconBadge(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    active: Boolean = false,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val badgeShape = CircleShape
    val bgBrush = if (active) {
        Brush.linearGradient(
            listOf(
                IndigoPrimary.copy(alpha = 0.28f),
                CyanTertiary.copy(alpha = 0.24f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.55f else 0.82f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.40f else 0.65f)
            )
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(badgeShape)
            .background(bgBrush)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (isDark) 0.38f else 0.85f),
                        CyanTertiary.copy(alpha = if (active) 0.65f else 0.25f)
                    )
                ),
                shape = badgeShape
            )
            .padding(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.54f)
        )
    }
}
