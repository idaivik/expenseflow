package com.expenseflow.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.expenseflow.app.data.AppDatabase
import com.expenseflow.app.data.SettingsRepository
import com.expenseflow.app.data.TransactionEntity
import com.expenseflow.app.data.TransactionRepository
import com.expenseflow.app.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Listens for incoming SMS, and for anything that reads as a bank/UPI/card
 * credit or debit (see [SmsParser]), logs it as a real transaction and fires
 * an instant notification — the same effect as the user adding it by hand,
 * minus the typing.
 *
 * `goAsync()` is required here: a [BroadcastReceiver] is normally killed the
 * instant [onReceive] returns, but the DB write and notification both happen
 * on a background coroutine, so the system is told to keep the receiver alive
 * until [android.content.BroadcastReceiver.PendingResult.finish] is called.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // A single SMS can arrive split across multiple PDUs; the body is the
        // concatenation of all parts, exactly what the user sees as one message.
        val body = messages.joinToString(separator = "") { it.messageBody ?: "" }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleMessage(context.applicationContext, body)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleMessage(context: Context, body: String) {
        val settingsRepository = SettingsRepository(context)
        if (!settingsRepository.settings.first().smsAutoDetect) return

        val parsed = SmsParser.parse(body) ?: return

        val database = AppDatabase.getDatabase(context)
        val repository = TransactionRepository(
            database.transactionDao(), database.budgetDao(), database.goalDao(), database.billDao(),
            database.plannedExpenseDao(), database.categoryMetaDao(), database.categoryDao(), context,
        )

        val now = Calendar.getInstance().time
        val category = "Other"
        val iconColor: Color = Color(0xFF2E9BF5) // CategoryVisuals falls back to CategoryTone.BLUE for "Other"

        val transaction = TransactionEntity(
            title = parsed.title,
            category = category,
            amount = parsed.amount,
            date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now),
            time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now),
            iconName = category,
            iconColor = iconColor.toArgb().toLong(),
            isExpense = parsed.isExpense,
        )
        repository.insert(transaction)

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
