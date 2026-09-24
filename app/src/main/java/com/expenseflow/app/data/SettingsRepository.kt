package com.expenseflow.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "expenseflow_settings")

/** User-facing app preferences, persisted across launches. */
data class Settings(
    val isDark: Boolean = false,
    val currencyCode: String = "usd",
    val budgetAlerts: Boolean = true,
    val billReminders: Boolean = true,
    val weeklySummary: Boolean = false,
    val goalMilestones: Boolean = true,
    /** What the transactions list totals show: "expense", "income", or "net" (income - expense). */
    val transactionDisplayMode: String = "expense",
) {
    val currencySymbol: String
        get() = when (currencyCode) {
            "inr" -> "₹"
            "eur" -> "€"
            else -> "$"
        }
}

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    private object Keys {
        val DARK = booleanPreferencesKey("dark")
        val CURRENCY = stringPreferencesKey("currency")
        val BUDGET_ALERTS = booleanPreferencesKey("budget_alerts")
        val BILL_REMINDERS = booleanPreferencesKey("bill_reminders")
        val WEEKLY = booleanPreferencesKey("weekly_summary")
        val GOALS = booleanPreferencesKey("goal_milestones")
        val DISMISSED_NOTIFICATIONS = stringSetPreferencesKey("dismissed_notifications")
        val TX_DISPLAY_MODE = stringPreferencesKey("transaction_display_mode")
    }

    val settings: Flow<Settings> = store.data.map { p ->
        Settings(
            isDark = p[Keys.DARK] ?: false,
            currencyCode = p[Keys.CURRENCY] ?: "usd",
            budgetAlerts = p[Keys.BUDGET_ALERTS] ?: true,
            billReminders = p[Keys.BILL_REMINDERS] ?: true,
            weeklySummary = p[Keys.WEEKLY] ?: false,
            goalMilestones = p[Keys.GOALS] ?: true,
            transactionDisplayMode = p[Keys.TX_DISPLAY_MODE] ?: "expense",
        )
    }

    suspend fun setDark(value: Boolean) = store.edit { it[Keys.DARK] = value }
    suspend fun setCurrency(code: String) = store.edit { it[Keys.CURRENCY] = code }
    suspend fun setBudgetAlerts(value: Boolean) = store.edit { it[Keys.BUDGET_ALERTS] = value }
    suspend fun setBillReminders(value: Boolean) = store.edit { it[Keys.BILL_REMINDERS] = value }
    suspend fun setWeeklySummary(value: Boolean) = store.edit { it[Keys.WEEKLY] = value }
    suspend fun setGoalMilestones(value: Boolean) = store.edit { it[Keys.GOALS] = value }
    suspend fun setTransactionDisplayMode(mode: String) = store.edit { it[Keys.TX_DISPLAY_MODE] = mode }

    /**
     * Ids of notifications the user has dismissed, kept forever (not per-session) so a
     * cleared notification stays cleared until a genuinely new event produces a new id.
     */
    val dismissedNotificationIds: Flow<Set<String>> = store.data.map { it[Keys.DISMISSED_NOTIFICATIONS] ?: emptySet() }

    suspend fun dismissNotifications(ids: Collection<String>) = store.edit {
        it[Keys.DISMISSED_NOTIFICATIONS] = (it[Keys.DISMISSED_NOTIFICATIONS] ?: emptySet()) + ids
    }
}
