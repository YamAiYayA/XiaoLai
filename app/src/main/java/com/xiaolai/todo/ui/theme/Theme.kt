package com.xiaolai.todo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Pink = Color(0xFFFF6B81)
private val SoftPinkBg = Color(0xFFFFF7F9)
private val Ink = Color(0xFF3D2C29)
private val SoftTeal = Color(0xFF4DB6AC)

private val LightColors = lightColorScheme(
    primary = Pink,
    onPrimary = Color.White,
    secondary = SoftTeal,
    onSecondary = Color.White,
    background = SoftPinkBg,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
)

private val DarkColors = darkColorScheme(
    primary = Pink,
    onPrimary = Color.White,
    secondary = SoftTeal,
    background = Color(0xFF2A1F22),
    onBackground = SoftPinkBg,
    surface = Color(0xFF3A2C30),
    onSurface = SoftPinkBg,
)

@Composable
fun XiaoLaiTodoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
