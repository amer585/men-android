package com.madrastna.teacher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

private val DarkColorScheme = darkColorScheme(
    primary = Gold500,
    secondary = Gold400,
    tertiary = Gold300,
    background = Ink950,
    surface = Ink850,
    onPrimary = Ink950,
    onSecondary = Ink950,
    onBackground = Color0FInk,
    onSurface = Color0FInk,
    error = Rose400,
)

// Workaround: Compose doesn't let us reference a val before it's defined in this file
private val Color0FInk = androidx.compose.ui.graphics.Color(0xFFDFE4EE)

private val AppTypography = Typography(
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Black),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun MadrastnaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content,
    )
}
