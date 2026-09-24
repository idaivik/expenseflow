package com.expenseflow.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Raw color palette — a 1:1 translation of the ExpenseFlow Design System
 * `tokens/colors.css`. Semantic light/dark aliases are assembled from these
 * in [ExpenseFlowColors] (see Theme.kt).
 */

// --- Brand: violet (accent) ---
val Violet50 = Color(0xFFF3EFFF)
val Violet500 = Color(0xFF8B5CF6)
val Violet600 = Color(0xFF7C4DF1)

// --- Brand: blue (primary) ---
val Blue50 = Color(0xFFEEF3FF)
val Blue100 = Color(0xFFDCE7FF)
val Blue200 = Color(0xFFBCD0FF)
val Blue300 = Color(0xFF8FB0FB)
val Blue400 = Color(0xFF5C87F7)
val Blue500 = Color(0xFF2F6BF6) // primary / FAB
val Blue600 = Color(0xFF1F57E6) // pressed
val Blue700 = Color(0xFF1B46C4)
val Blue800 = Color(0xFF1A3C9E)
val Blue900 = Color(0xFF17357C)

// Hero gradient stops (155deg #5b8bf7 -> #3d70f0 -> #2f6bf6)
val HeroGradStart = Color(0xFF5B8BF7)
val HeroGradMid = Color(0xFF3D70F0)
val HeroGradEnd = Color(0xFF2F6BF6)

// --- Category palette (donut + icons) ---
val CatPurple = Color(0xFF8B5CF6)
val CatBlue = Color(0xFF2E9BF5)
val CatGreen = Color(0xFF29C26B)
val CatOrange = Color(0xFFF5811F)
val CatPink = Color(0xFFEC4D9A)
val CatRed = Color(0xFFF0324B)

// --- Soft tinted icon backgrounds (light) ---
val TintPurpleLight = Color(0xFFEFE9FF)
val TintBlueLight = Color(0xFFE5F2FE)
val TintGreenLight = Color(0xFFE2F7EC)
val TintOrangeLight = Color(0xFFFDEEDE)
val TintPinkLight = Color(0xFFFDE6F1)
val TintRedLight = Color(0xFFFDE6EA)

// --- Soft tinted icon backgrounds (dark, richer) ---
val TintPurpleDark = Color(0xFF241D3A)
val TintBlueDark = Color(0xFF142A3F)
val TintGreenDark = Color(0xFF123028)
val TintOrangeDark = Color(0xFF2F2213)
val TintPinkDark = Color(0xFF331C2B)
val TintRedDark = Color(0xFF331820)

// --- Neutrals ---
val NeutralWhite = Color(0xFFFFFFFF)
val Gray25 = Color(0xFFFAFAFB)
val Gray50 = Color(0xFFF5F5F8)
val Gray100 = Color(0xFFEEEEF2)
val Gray200 = Color(0xFFE2E2EA)
val Gray300 = Color(0xFFCFCFDA)
val Gray400 = Color(0xFFA7A7B8)
val Gray500 = Color(0xFF7C7C8F)
val Gray600 = Color(0xFF5B5B6B)
val Gray700 = Color(0xFF3F3F4D)
val Gray800 = Color(0xFF26262F)
val Gray900 = Color(0xFF15151C)
val Ink = Color(0xFF101019)

// --- Semantic money ---
val MoneyIn = Color(0xFF29C26B) // positive / income
val MoneyOut = Color(0xFFF0324B) // negative / expense

// --- Dark surfaces ---
val DarkBgApp = Color(0xFF0E0E14)
val DarkSurfaceCard = Color(0xFF1A1A24)
val DarkSurfaceSunken = Color(0xFF141420)
val DarkSurfaceInset = Color(0xFF22222E)
val DarkTextPrimary = Color(0xFFF4F4F8)
val DarkTextSecondary = Color(0xFFA2A2B4)
val DarkTextMuted = Color(0xFF6B6B7D)
val DarkBorderSubtle = Color(0xFF26262F)
val DarkBorderDefault = Color(0xFF30303C)
val DarkBrandSoft = Color(0xFF16244A)
