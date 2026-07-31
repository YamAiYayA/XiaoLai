package com.xiaolai.todo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WarmOrange = Color(0xFFE07A5F)
private val DeepBrown = Color(0xFF3D2C29)
private val Cream = Color(0xFFFFF7F0)
private val SoftSage = Color(0xFF81B29A)

private val LightColors = lightColorScheme(
    primary = WarmOrange,
    onPrimary = Color.White,
    secondary = SoftSage,
    onSecondary = DeepBrown,
    background = Cream,
    onBackground = DeepBrown,
    surface = Color.White,
    onSurface = DeepBrown,
)

private val DarkColors = darkColorScheme(
    primary = WarmOrange,
    onPrimary = Color.White,
    secondary = SoftSage,
    onSecondary = DeepBrown,
    background = DeepBrown,
    onBackground = Cream,
    surface = Color(0xFF4A3834),
    onSurface = Cream,
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
