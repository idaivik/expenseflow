package com.expenseflow.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import androidx.glance.color.ColorProvider
import com.expenseflow.app.MainActivity
import java.util.Locale

/**
 * Shared color palettes and design tokens for ExpenseFlow Glance AppWidgets.
 * Directly maps to ExpenseFlow design system tokens for consistent dark & light mode.
 */
object WidgetTheme {
    // Surface & Background Colors
    val surfaceBackground = ColorProvider(
        day = Color(0xFFFFFFFF),
        night = Color(0xFF1A1A24)
    )

    val surfaceSunken = ColorProvider(
        day = Color(0xFFF5F5F8),
        night = Color(0xFF141420)
    )

    val surfaceInset = ColorProvider(
        day = Color(0xFFEEEEF2),
        night = Color(0xFF22222E)
    )

    val surfaceAccent = ColorProvider(
        day = Color(0xFFEEF3FF),
        night = Color(0xFF16244A)
    )

    // Text Colors
    val textPrimary = ColorProvider(
        day = Color(0xFF101019),
        night = Color(0xFFF4F4F8)
    )

    val textSecondary = ColorProvider(
        day = Color(0xFF7C7C8F),
        night = Color(0xFFA2A2B4)
    )

    val textMuted = ColorProvider(
        day = Color(0xFFA7A7B8),
        night = Color(0xFF6B6B7D)
    )

    val textOnBrand = ColorProvider(
        day = Color(0xFFFFFFFF),
        night = Color(0xFFFFFFFF)
    )

    // Brand & Semantic Colors
    val brandPrimary = ColorProvider(
        day = Color(0xFF2F6BF6),
        night = Color(0xFF3D70F0)
    )

    val brandButton = ColorProvider(
        day = Color(0xFF2F6BF6),
        night = Color(0xFF2F6BF6)
    )

    val moneyPositive = ColorProvider(
        day = Color(0xFF29C26B),
        night = Color(0xFF34D399)
    )

    val moneyNegative = ColorProvider(
        day = Color(0xFFF0324B),
        night = Color(0xFFF87171)
    )

    val warningAmber = ColorProvider(
        day = Color(0xFFF59E0B),
        night = Color(0xFFFBBF24)
    )

    val borderSubtle = ColorProvider(
        day = Color(0xFFE2E2EA),
        night = Color(0xFF26262F)
    )

    // Category Tone Colors (Icons & Backgrounds)
    val catFoodBg = ColorProvider(day = Color(0xFFE5F2FE), night = Color(0xFF142A3F))
    val catFoodFg = ColorProvider(day = Color(0xFF2E9BF5), night = Color(0xFF60A5FA))

    val catShoppingBg = ColorProvider(day = Color(0xFFFDEEDE), night = Color(0xFF2F2213))
    val catShoppingFg = ColorProvider(day = Color(0xFFF5811F), night = Color(0xFFFB923C))

    val catTransportBg = ColorProvider(day = Color(0xFFE2F7EC), night = Color(0xFF123028))
    val catTransportFg = ColorProvider(day = Color(0xFF29C26B), night = Color(0xFF34D399))

    val catBillsBg = ColorProvider(day = Color(0xFFFDE6EA), night = Color(0xFF331820))
    val catBillsFg = ColorProvider(day = Color(0xFFF0324B), night = Color(0xFFF87171))

    val catHealthBg = ColorProvider(day = Color(0xFFFDE6F1), night = Color(0xFF331C2B))
    val catHealthFg = ColorProvider(day = Color(0xFFEC4D9A), night = Color(0xFFF472B6))

    val catEntertainmentBg = ColorProvider(day = Color(0xFFEFE9FF), night = Color(0xFF241D3A))
    val catEntertainmentFg = ColorProvider(day = Color(0xFF8B5CF6), night = Color(0xFFA78BFA))

    /**
     * Intent factory for launching MainActivity with quick add expense action
     */
    fun createAddExpenseIntent(
        context: Context,
        category: String? = null,
        amount: String? = null,
        isExpense: Boolean = true
    ): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_ADD, true)
            if (!category.isNullOrBlank()) {
                putExtra(MainActivity.EXTRA_PREFILL_CATEGORY, category)
            }
            if (!amount.isNullOrBlank()) {
                putExtra(MainActivity.EXTRA_PREFILL_AMOUNT, amount)
            }
            putExtra(MainActivity.EXTRA_IS_EXPENSE, isExpense)
        }
    }

    /**
     * Intent factory for opening a specific tab / overlay in MainActivity
     */
    fun createNavigationIntent(
        context: Context,
        tab: String = "home",
        overlay: String? = null
    ): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_TAB, tab)
            if (!overlay.isNullOrBlank()) {
                putExtra(MainActivity.EXTRA_OPEN_OVERLAY, overlay)
            }
        }
    }

    fun formatMoney(currencySymbol: String, amount: Double): String {
        return String.format(Locale.US, "%s%.2f", currencySymbol, amount)
    }

    fun formatCompactMoney(currencySymbol: String, amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            String.format(Locale.US, "%s%.0f", currencySymbol, amount)
        } else {
            String.format(Locale.US, "%s%.2f", currencySymbol, amount)
        }
    }
}
