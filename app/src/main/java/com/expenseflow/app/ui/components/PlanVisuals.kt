package com.expenseflow.app.ui.components

import com.composables.icons.lucide.*

import androidx.compose.ui.graphics.vector.ImageVector
import com.expenseflow.app.ui.theme.CategoryTone

/** Category presets for goals/budgets and bills on the Plan screen. */
object PlanVisuals {

    data class Preset(val key: String, val tone: CategoryTone)

    val goalCategories = listOf(
        Preset("House", CategoryTone.ORANGE),
        Preset("Car", CategoryTone.RED),
        Preset("Travel", CategoryTone.BLUE),
        Preset("Education", CategoryTone.PURPLE),
        Preset("Health", CategoryTone.GREEN),
        Preset("Other", CategoryTone.PINK),
    )

    val billCategories = listOf(
        Preset("Water", CategoryTone.BLUE),
        Preset("Electricity", CategoryTone.ORANGE),
        Preset("Gas", CategoryTone.RED),
        Preset("Internet", CategoryTone.PURPLE),
        Preset("Phone", CategoryTone.GREEN),
        Preset("Streaming", CategoryTone.PINK),
        Preset("Rent", CategoryTone.ORANGE),
        Preset("Insurance", CategoryTone.GREEN),
        Preset("Other", CategoryTone.PURPLE),
    )

    fun icon(key: String): ImageVector = when (key) {
        "House" -> Lucide.House
        "Car" -> Lucide.Car
        "Travel" -> Lucide.TreePalm
        "Education" -> Lucide.GraduationCap
        "Health" -> Lucide.HeartPulse
        "Water" -> Lucide.Droplet
        "Electricity" -> Lucide.Zap
        "Gas" -> Lucide.Flame
        "Internet" -> Lucide.Wifi
        "Phone" -> Lucide.Smartphone
        "Streaming" -> Lucide.Tv
        "Rent" -> Lucide.Building2
        "Insurance" -> Lucide.ShieldCheck
        "Other" -> Lucide.Sparkles
        else -> Lucide.PiggyBank
    }
}
