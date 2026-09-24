package com.expenseflow.app.sms

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.expenseflow.app.data.AppDatabase
import com.expenseflow.app.data.SettingsRepository
import com.expenseflow.app.data.TransactionEntity
import com.expenseflow.app.data.TransactionRepository
import com.expenseflow.app.notifications.NotificationHelper
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Turns a [ParsedSmsTransaction] into a stored transaction plus an instant
 * notification. Shared by the SMS receiver and the payment-notification
 * listener so both sources behave identically.
 *
 * One payment often produces both a bank SMS and an app notification, so
 * [log] drops a second transaction with the same amount and direction that
 * arrives within [DEDUP_WINDOW_MS] of the first.
 */
object PaymentLogger {

    private const val DEDUP_WINDOW_MS = 2 * 60 * 1000L

    private val recent = mutableMapOf<String, Long>()

    private fun isDuplicate(parsed: ParsedSmsTransaction): Boolean = synchronized(recent) {
        val now = System.currentTimeMillis()
        recent.entries.removeAll { now - it.value > DEDUP_WINDOW_MS }
        val key = "${parsed.amount}|${parsed.isExpense}"
        if (recent.containsKey(key)) return true
        recent[key] = now
        false
    }

    suspend fun log(context: Context, parsed: ParsedSmsTransaction) {
        if (isDuplicate(parsed)) return

        val settingsRepository = SettingsRepository(context)
        val database = AppDatabase.getDatabase(context)
        val repository = TransactionRepository(
            database.transactionDao(), database.budgetDao(), database.goalDao(), database.billDao(),
            database.plannedExpenseDao(), database.categoryMetaDao(), database.categoryDao(), context,
        )

        val now = Calendar.getInstance().time
        val category = "Other"
        val iconColor: Color = Color(0xFF2E9BF5) // CategoryVisuals falls back to CategoryTone.BLUE for "Other"

        repository.insert(
            TransactionEntity(
                title = parsed.title,
                category = category,
                amount = parsed.amount,
                date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now),
                time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now),
                iconName = category,
                iconColor = iconColor.toArgb().toLong(),
                isExpense = parsed.isExpense,
            )
        )

        val settings = settingsRepository.settings.first()
        NotificationHelper.showTransactionDetected(
            context = context,
            title = parsed.title,
            amount = parsed.amount,
            isExpense = parsed.isExpense,
            currencySymbol = settings.currencySymbol,
        )
    }
}
