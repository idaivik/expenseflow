package com.expenseflow.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.expenseflow.app.R

/*
 * Type system — mirrors tokens/typography.css.
 *
 * Brand fonts (tokens/fonts.css): Plus Jakarta Sans for display / numerals,
 * DM Sans for UI / body. Both ship as variable fonts in `res/font/`, so each
 * FontWeight is instantiated from the same file via FontVariation.
 */
@OptIn(ExperimentalTextApi::class)
private fun variableFamily(resId: Int): FontFamily = FontFamily(
    listOf(400, 500, 600, 700, 800).map { w ->
        Font(
            resId = resId,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
        )
    },
)

val DisplayFont: FontFamily = variableFamily(R.font.plus_jakarta_sans) // Plus Jakarta Sans
val BodyFont: FontFamily = variableFamily(R.font.dm_sans)              // DM Sans
val NumberFont: FontFamily = DisplayFont                               // Plus Jakarta Sans (numerals)

// Design type scale (px -> sp): 40 / 30 / 24 / 20 / 17 / 15 / 13 / 12 / 11
val ExpenseFlowTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.02).em,
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.02).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.01).em,
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFont, fontWeight = FontWeight.Bold,
        fontSize = 17.sp, lineHeight = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 19.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 17.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 15.sp,
    ),
)
