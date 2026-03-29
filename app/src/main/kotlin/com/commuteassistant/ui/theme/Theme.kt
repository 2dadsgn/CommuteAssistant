package com.commuteassistant.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue80  = Color(0xFF9ECAFF)
private val Blue40  = Color(0xFF1565C0)
private val Teal80  = Color(0xFF80CBC4)
private val Teal40  = Color(0xFF00796B)

private val LightColors = lightColorScheme(
    primary         = Blue40,
    onPrimary       = Color.White,
    secondary       = Teal40,
    onSecondary     = Color.White,
    surface         = Color(0xFFF8F9FA),
    background      = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary         = Blue80,
    onPrimary       = Color(0xFF003064),
    secondary       = Teal80,
    onSecondary     = Color(0xFF003731),
    surface         = Color(0xFF1A1C1E),
    background      = Color(0xFF111314)
)

@Composable
fun CommuteAssistantTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = Typography(),
        content     = content
    )
}
