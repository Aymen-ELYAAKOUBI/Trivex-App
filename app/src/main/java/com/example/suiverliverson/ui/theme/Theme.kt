package com.example.suiverliverson.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LiversonDarkColors = darkColorScheme(
    primary = TrivexGold,
    onPrimary = Color(0xFF1F1600),
    secondary = Green,
    onSecondary = Color(0xFF003915),
    tertiary = Purple,
    background = AppBackground,
    onBackground = Color(0xFFF5F5F7),
    surface = AppSurface,
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = DarkGray,
    onSurfaceVariant = LightGray,
    outline = Color(0xFF33333D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LiversonLightColors = lightColorScheme(
    primary = TrivexGoldDark,
    onPrimary = Color.White,
    secondary = Green,
    onSecondary = Color.White,
    tertiary = Purple,
    background = Color(0xFFFBFBFC),
    onBackground = Color(0xFF141416),
    surface = Color.White,
    onSurface = Color(0xFF141416),
    surfaceVariant = Color(0xFFF0F0F3),
    onSurfaceVariant = Color(0xFF5A5A65),
    outline = Color(0xFFD6D6DF),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val LiversonShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun LiversonTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) LiversonDarkColors else LiversonLightColors,
        typography = Typography,
        shapes = LiversonShapes,
        content = content
    )
}
