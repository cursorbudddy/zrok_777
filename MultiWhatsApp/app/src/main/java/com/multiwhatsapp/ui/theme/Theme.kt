package com.multiwhatsapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF25D366),
    secondary = Color(0xFF075E54),
    tertiary = Color(0xFF128C7E),
    background = Color.White,
    surface = Color.White
)

@Composable
fun MultiWhatsAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
