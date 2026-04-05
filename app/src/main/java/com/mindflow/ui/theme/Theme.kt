package com.mindflow.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * MindFlow Theme - MiKux inspired design
 */

// Primary colors
val PrimaryBlue = Color(0xFF1E88E5)
val PrimaryBlueLight = Color(0xFF6AB7FF)
val PrimaryBlueDark = Color(0xFF005CB2)

// Secondary colors
val SecondaryTeal = Color(0xFF26A69A)
val SecondaryTealLight = Color(0xFF64D8CB)
val SecondaryTealDark = Color(0xFF00766C)

// Neutral colors
val SurfaceLight = Color(0xFFFAFAFA)
val SurfaceDark = Color(0xFF121212)
val BackgroundLight = Color(0xFFFFFFFF)
val BackgroundDark = Color(0xFF1E1E1E)

// Text colors
val OnPrimaryLight = Color.White
val OnPrimaryDark = Color.White
val OnBackgroundLight = Color(0xFF1C1B1F)
val OnBackgroundDark = Color(0xFFE6E1E5)

// Status colors
val ErrorRed = Color(0xFFEF5350)
val SuccessGreen = Color(0xFF66BB6A)
val WarningAmber = Color(0xFFFFCA28)

// Message bubbles
val UserBubbleLight = Color(0xFF1E88E5)
val UserBubbleDark = Color(0xFF1565C0)
val AssistantBubbleLight = Color(0xFFE8E8E8)
val AssistantBubbleDark = Color(0xFF2D2D2D)

/**
 * Light color scheme
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = PrimaryBlueDark,
    secondary = SecondaryTeal,
    onSecondary = OnPrimaryLight,
    secondaryContainer = SecondaryTealLight,
    onSecondaryContainer = SecondaryTealDark,
    tertiary = Color(0xFF7C5800),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDEA1),
    onTertiaryContainer = Color(0xFF271900),
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    scrim = Color.Black,
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = PrimaryBlueLight
)

/**
 * Dark color scheme
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = PrimaryBlueDark,
    primaryContainer = PrimaryBlue,
    onPrimaryContainer = PrimaryBlueLight,
    secondary = SecondaryTealLight,
    onSecondary = SecondaryTealDark,
    secondaryContainer = SecondaryTeal,
    onSecondaryContainer = SecondaryTealLight,
    tertiary = Color(0xFFF5BD48),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFDEA1),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    scrim = Color.Black,
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = PrimaryBlue
)

/**
 * MiKux-inspired typography
 */
private val MindFlowTypography = Typography(
    displayLarge = Typography().displayLarge.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    ),
    displayMedium = Typography().displayMedium.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    ),
    displaySmall = Typography().displaySmall.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    ),
    headlineLarge = Typography().headlineLarge.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
    ),
    headlineSmall = Typography().headlineSmall.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
    ),
    titleLarge = Typography().titleLarge.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    ),
    titleMedium = Typography().titleMedium.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    ),
    titleSmall = Typography().titleSmall.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    ),
    bodyLarge = Typography().bodyLarge.copy(
        lineHeight = androidx.compose.ui.unit.TextUnit(1.6f, androidx.compose.ui.unit.TextUnitType.Sp)
    ),
    bodyMedium = Typography().bodyMedium.copy(
        lineHeight = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp)
    ),
    bodySmall = Typography().bodySmall.copy(
        lineHeight = androidx.compose.ui.unit.TextUnit(1.4f, androidx.compose.ui.unit.TextUnitType.Sp)
    ),
    labelLarge = Typography().labelLarge.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    ),
    labelMedium = Typography().labelMedium.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    ),
    labelSmall = Typography().labelSmall.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    )
)

/**
 * MiKux custom shapes
 */
private val MindFlowShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
)

/**
 * MindFlow theme composable
 */
@Composable
fun MindFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MindFlowTypography,
        shapes = MindFlowShapes,
        content = content
    )
}
