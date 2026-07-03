package com.madrastna.teacher.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madrastna.teacher.ui.theme.*

// ── Full-screen cinematic background ────────────────────────
// Deep navy vertical gradient + drifting gold orbs + a faint grid.
@Composable
fun CinematicBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Ink950, Ink925, Ink900, Ink925),
                )
            ),
    ) {
        // Top-right gold orb
        val pulse by rememberInfiniteTransition(label = "orb1").animateFloat(
            initialValue = 0.85f, targetValue = 1.05f,
            animationSpec = infiniteRepeatable(tween(3200), RepeatMode.Reverse), label = "p1",
        )
        Box(
            Modifier
                .offset(x = 180.dp, y = (-90).dp)
                .size(320.dp * pulse)
                .clip(CircleShape)
                .blur(70.dp)
                .background(Brush.radialGradient(listOf(Color(0x55C9A96A), Color.Transparent))),
        )
        // Bottom-left warm orb
        val pulse2 by rememberInfiniteTransition(label = "orb2").animateFloat(
            initialValue = 1.0f, targetValue = 0.82f,
            animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "p2",
        )
        Box(
            Modifier
                .offset(x = (-110).dp, y = 360.dp)
                .size(300.dp * pulse2)
                .clip(CircleShape)
                .blur(70.dp)
                .background(Brush.radialGradient(listOf(Color(0x40D4B676), Color.Transparent))),
        )
        // Center breathing warmth
        val breathe by rememberInfiniteTransition(label = "core").animateFloat(
            initialValue = 0.9f, targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(2800), RepeatMode.Reverse), label = "br",
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .size(380.dp * breathe)
                .clip(CircleShape)
                .blur(60.dp)
                .background(Brush.radialGradient(listOf(Color(0x22C9A96A), Color.Transparent))),
        )
    }
}

// ── Glass card: translucent surface with gold hairline border ─
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0x1FFFFFFF), Color(0x0AFFFFFF)),
                )
            )
            .drawBehind {
                drawRect(
                    color = GlassBorder,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f),
                )
            }
            .shadow(
                elevation = 0.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color.Transparent,
                spotColor = Color.Transparent,
            ),
        content = content,
    )
}

// ── Gold gradient button (premium CTA) ──────────────────────
@Composable
fun GoldGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    val gradColors = if (enabled)
        listOf(Gold300, Gold500, Gold400) else listOf(Gold700, Gold600, Gold700)
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = gradColors,
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                    tileMode = TileMode.Mirror,
                )
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                color = Ink950,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
            )
        }
    }
}

// ── Brand crest: glowing monogram inside a rounded shield ───
@Composable
fun BrandCrest(modifier: Modifier = Modifier, size: Dp = 88.dp) {
    val breathe by rememberInfiniteTransition(label = "crest").animateFloat(
        initialValue = 0.7f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "cb",
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        // Outer glow
        Box(
            Modifier
                .size(size * breathe)
                .clip(CircleShape)
                .blur(28.dp)
                .background(Brush.radialGradient(listOf(Color(0x66C9A96A), Color.Transparent)))
        )
        // Shield plate
        Box(
            Modifier
                .size(size * 0.82f)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Ink850, Ink900),
                        start = Offset(0f, 0f),
                        end = Offset(0f, Float.POSITIVE_INFINITY),
                    )
                )
                .drawBehind {
                    drawRect(GlassBorder, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "إ",
                color = Gold300,
                fontSize = (size.value * 0.5f).sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

// ── Status chip (verified / pending / etc.) ─────────────────
@Composable
fun StatusChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.3f)), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}
