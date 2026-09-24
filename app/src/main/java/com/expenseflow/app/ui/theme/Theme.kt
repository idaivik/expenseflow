package com.expenseflow.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** The six category "tones" used across the design (icons, donut, progress). */
enum class CategoryTone { PURPLE, BLUE, GREEN, ORANGE, PINK, RED;
    companion object {
        fun from(name: String?): CategoryTone = when (name?.lowercase()) {
            "purple" -> PURPLE
            "blue" -> BLUE
            "green" -> GREEN
            "orange" -> ORANGE
            "pink" -> PINK
            "red" -> RED
            else -> BLUE
        }
    }
}

/**
 * Design-system semantic colors that Material3's [androidx.compose.material3.ColorScheme]
 * doesn't cover (category palette, soft tints, the hero gradient, fine-grained text/border
 * tokens). Provided through [LocalExpenseFlowColors] and read via `MaterialTheme.efColors`.
 */
@Immutable
data class ExpenseFlowColors(
    val isDark: Boolean,
    val bgApp: Color,
    val surfaceCard: Color,
    val surfaceSunken: Color,
    val surfaceInset: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textInverse: Color,
    val borderSubtle: Color,
    val borderDefault: Color,
    val brand: Color,
    val brandSoft: Color,
    val onBrand: Color,
    val moneyIn: Color,
    val moneyOut: Color,
    val catPurple: Color,
    val catBlue: Color,
    val catGreen: Color,
    val catOrange: Color,
    val catPink: Color,
    val catRed: Color,
    val tintPurple: Color,
    val tintBlue: Color,
    val tintGreen: Color,
    val tintOrange: Color,
    val tintPink: Color,
    val tintRed: Color,
    val heroColors: List<Color>,
) {
    /** Solid category color for a [CategoryTone]. */
    fun cat(tone: CategoryTone): Color = when (tone) {
        CategoryTone.PURPLE -> catPurple
        CategoryTone.BLUE -> catBlue
        CategoryTone.GREEN -> catGreen
        CategoryTone.ORANGE -> catOrange
        CategoryTone.PINK -> catPink
        CategoryTone.RED -> catRed
    }

    /** Soft tinted background for a [CategoryTone]'s icon tile. */
    fun tint(tone: CategoryTone): Color = when (tone) {
        CategoryTone.PURPLE -> tintPurple
        CategoryTone.BLUE -> tintBlue
        CategoryTone.GREEN -> tintGreen
        CategoryTone.ORANGE -> tintOrange
        CategoryTone.PINK -> tintPink
        CategoryTone.RED -> tintRed
    }

    fun cat(name: String): Color = cat(CategoryTone.from(name))
    fun tint(name: String): Color = tint(CategoryTone.from(name))
}

private val LightEFColors = ExpenseFlowColors(
    isDark = false,
    bgApp = Gray50,
    surfaceCard = NeutralWhite,
    surfaceSunken = Gray100,
    surfaceInset = Gray50,
    textPrimary = Ink,
    textSecondary = Gray500,
    textMuted = Gray400,
    textInverse = NeutralWhite,
    borderSubtle = Gray100,
    borderDefault = Gray200,
    brand = Blue500,
    brandSoft = Blue50,
    onBrand = NeutralWhite,
    moneyIn = MoneyIn,
    moneyOut = MoneyOut,
    catPurple = CatPurple,
    catBlue = CatBlue,
    catGreen = CatGreen,
    catOrange = CatOrange,
    catPink = CatPink,
    catRed = CatRed,
    tintPurple = TintPurpleLight,
    tintBlue = TintBlueLight,
    tintGreen = TintGreenLight,
    tintOrange = TintOrangeLight,
    tintPink = TintPinkLight,
    tintRed = TintRedLight,
    heroColors = listOf(HeroGradStart, HeroGradMid, HeroGradEnd),
)

private val DarkEFColors = ExpenseFlowColors(
    isDark = true,
    bgApp = DarkBgApp,
    surfaceCard = DarkSurfaceCard,
    surfaceSunken = DarkSurfaceSunken,
    surfaceInset = DarkSurfaceInset,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    textInverse = Ink,
    borderSubtle = DarkBorderSubtle,
    borderDefault = DarkBorderDefault,
    brand = Blue500,
    brandSoft = DarkBrandSoft,
    onBrand = NeutralWhite,
    moneyIn = MoneyIn,
    moneyOut = MoneyOut,
    catPurple = CatPurple,
    catBlue = CatBlue,
    catGreen = CatGreen,
    catOrange = CatOrange,
    catPink = CatPink,
    catRed = CatRed,
    tintPurple = TintPurpleDark,
    tintBlue = TintBlueDark,
    tintGreen = TintGreenDark,
    tintOrange = TintOrangeDark,
    tintPink = TintPinkDark,
    tintRed = TintRedDark,
    heroColors = listOf(HeroGradStart, HeroGradMid, HeroGradEnd),
)

val LocalExpenseFlowColors = staticCompositionLocalOf { LightEFColors }

/** Convenience accessor: `MaterialTheme.efColors`. */
val MaterialTheme.efColors: ExpenseFlowColors
    @Composable get() = LocalExpenseFlowColors.current

private val LightColorScheme = lightColorScheme(
    primary = Blue500,
    onPrimary = NeutralWhite,
    primaryContainer = Blue50,
    onPrimaryContainer = Blue700,
    secondary = Violet500,
    onSecondary = NeutralWhite,
    background = Gray50,
    onBackground = Ink,
    surface = NeutralWhite,
    onSurface = Ink,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray500,
    outline = Gray200,
    outlineVariant = Gray100,
    error = MoneyOut,
    onError = NeutralWhite,
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue500,
    onPrimary = NeutralWhite,
    primaryContainer = DarkBrandSoft,
    onPrimaryContainer = Blue200,
    secondary = Violet500,
    onSecondary = NeutralWhite,
    background = DarkBgApp,
    onBackground = DarkTextPrimary,
    surface = DarkSurfaceCard,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceInset,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorderDefault,
    outlineVariant = DarkBorderSubtle,
    error = MoneyOut,
    onError = NeutralWhite,
)

/** Diagonal violet-blue hero gradient (dashboard header, auth, profile). */
@Composable
fun heroBrush(): Brush = Brush.linearGradient(LocalExpenseFlowColors.current.heroColors)

@Composable
fun ExpenseFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val efColors = if (darkTheme) DarkEFColors else LightEFColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalExpenseFlowColors provides efColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ExpenseFlowTypography,
            shapes = ExpenseFlowShapes,
            content = content,
        )
    }
}
