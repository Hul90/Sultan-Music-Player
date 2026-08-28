package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.model.ThemeOption

data class SultanThemePalette(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val cardBackground: Color,
    val accent: Color,
    val gradientBrush: Brush,
    val headerBrush: Brush
)

val LocalSultanPalette = staticCompositionLocalOf<SultanThemePalette> {
    error("No SultanThemePalette provided")
}

fun getPaletteForTheme(theme: ThemeOption): SultanThemePalette {
    return when (theme) {
        ThemeOption.SULTAN_GOLD -> SultanThemePalette(
            primary = SultanGoldPrimary,
            secondary = SultanGoldSecondary,
            background = SultanGoldBackground,
            surface = SultanGoldSurface,
            cardBackground = SultanGoldCard,
            accent = SultanGoldAccent,
            gradientBrush = Brush.verticalGradient(
                listOf(Color(0xFF262010), Color(0xFF14120C), Color(0xFF0A0906))
            ),
            headerBrush = Brush.horizontalGradient(
                listOf(Color(0xFFFFD700), Color(0xFFFFB300), Color(0xFFE5A910))
            )
        )
        ThemeOption.NEON_DARK -> SultanThemePalette(
            primary = NeonPurplePrimary,
            secondary = NeonCyanSecondary,
            background = NeonDarkBackground,
            surface = NeonDarkSurface,
            cardBackground = NeonDarkCard,
            accent = Color(0xFFFF007F),
            gradientBrush = Brush.verticalGradient(
                listOf(Color(0xFF1B1038), Color(0xFF100D24), Color(0xFF080711))
            ),
            headerBrush = Brush.horizontalGradient(
                listOf(NeonPurplePrimary, NeonCyanSecondary)
            )
        )
        ThemeOption.NATURE_GREEN -> SultanThemePalette(
            primary = NatureGreenPrimary,
            secondary = NatureGreenSecondary,
            background = NatureGreenBackground,
            surface = NatureGreenSurface,
            cardBackground = NatureGreenCard,
            accent = Color(0xFF69F0AE),
            gradientBrush = Brush.verticalGradient(
                listOf(Color(0xFF0F3621), Color(0xFF092014), Color(0xFF04100A))
            ),
            headerBrush = Brush.horizontalGradient(
                listOf(NatureGreenPrimary, NatureGreenSecondary)
            )
        )
        ThemeOption.PASTEL_PINK -> SultanThemePalette(
            primary = PastelPinkPrimary,
            secondary = PastelPinkSecondary,
            background = PastelPinkBackground,
            surface = PastelPinkSurface,
            cardBackground = PastelPinkCard,
            accent = Color(0xFFFF80AB),
            gradientBrush = Brush.verticalGradient(
                listOf(Color(0xFF38152C), Color(0xFF220E1B), Color(0xFF12070E))
            ),
            headerBrush = Brush.horizontalGradient(
                listOf(PastelPinkPrimary, PastelPinkSecondary)
            )
        )
        ThemeOption.OCEAN_BLUE -> SultanThemePalette(
            primary = OceanBluePrimary,
            secondary = OceanBlueSecondary,
            background = OceanBlueBackground,
            surface = OceanBlueSurface,
            cardBackground = OceanBlueCard,
            accent = Color(0xFF80D8FF),
            gradientBrush = Brush.verticalGradient(
                listOf(Color(0xFF0A2B4C), Color(0xFF061B30), Color(0xFF030E1A))
            ),
            headerBrush = Brush.horizontalGradient(
                listOf(OceanBluePrimary, OceanBlueSecondary)
            )
        )
        ThemeOption.SUNSET_ORANGE -> SultanThemePalette(
            primary = SunsetOrangePrimary,
            secondary = SunsetOrangeSecondary,
            background = SunsetOrangeBackground,
            surface = SunsetOrangeSurface,
            cardBackground = SunsetOrangeCard,
            accent = Color(0xFFFFD180),
            gradientBrush = Brush.verticalGradient(
                listOf(Color(0xFF3E1A0C), Color(0xFF240F07), Color(0xFF120803))
            ),
            headerBrush = Brush.horizontalGradient(
                listOf(SunsetOrangePrimary, SunsetOrangeSecondary)
            )
        )
        ThemeOption.ROYAL_PURPLE -> SultanThemePalette(
            primary = RoyalPurplePrimary,
            secondary = RoyalPurpleSecondary,
            background = RoyalPurpleBackground,
            surface = RoyalPurpleSurface,
            cardBackground = RoyalPurpleCard,
            accent = Color(0xFFEA80FC),
            gradientBrush = Brush.verticalGradient(
                listOf(Color(0xFF27134A), Color(0xFF170B2E), Color(0xFF0D061A))
            ),
            headerBrush = Brush.horizontalGradient(
                listOf(RoyalPurplePrimary, RoyalPurpleSecondary)
            )
        )
        ThemeOption.GRADIENT_GLASS -> SultanThemePalette(
            primary = GlassCyanPrimary,
            secondary = GlassVioletSecondary,
            background = GlassBackground,
            surface = GlassSurface,
            cardBackground = GlassCard,
            accent = Color(0xFF84FFFF),
            gradientBrush = Brush.verticalGradient(
                listOf(Color(0xFF1C2333), Color(0xFF111722), Color(0xFF0B0E14))
            ),
            headerBrush = Brush.horizontalGradient(
                listOf(GlassCyanPrimary, GlassVioletSecondary)
            )
        )
    }
}

@Composable
fun SultanMusicTheme(
    selectedTheme: ThemeOption = ThemeOption.SULTAN_GOLD,
    content: @Composable () -> Unit
) {
    val palette = getPaletteForTheme(selectedTheme)

    val colorScheme = darkColorScheme(
        primary = palette.primary,
        secondary = palette.secondary,
        background = palette.background,
        surface = palette.surface,
        surfaceVariant = palette.cardBackground,
        onPrimary = Color.Black,
        onSecondary = Color.White,
        onBackground = TextWhitePrimary,
        onSurface = TextWhitePrimary
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = palette.background.toArgb()
                window.navigationBarColor = palette.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    CompositionLocalProvider(LocalSultanPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
