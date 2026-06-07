package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

val JetBrainsMono = FontFamily.Monospace // Using default Monospace as fallback for JetBrains Mono

val codeStyle = TextStyle(
    fontFamily = JetBrainsMono,
    fontSize = 14.sp,
    lineHeight = 22.sp
)

val terminalOutputStyle = TextStyle(
    fontFamily = JetBrainsMono,
    fontSize = 13.sp,
    lineHeight = 20.sp
)

val lineNumberStyle = TextStyle(
    fontFamily = JetBrainsMono,
    fontSize = 12.sp,
    color = OnSurfaceDim,
    textAlign = TextAlign.End
)

val terminalInputStyle = TextStyle(
    fontFamily = JetBrainsMono,
    fontSize = 13.sp
)

val AppTypography = Typography(
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
)
