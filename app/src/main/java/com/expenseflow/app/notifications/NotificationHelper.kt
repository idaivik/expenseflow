package com.expenseflow.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.expenseflow.app.R
import com.expenseflow.app.model.BudgetAlert
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Central place for creating the notification channel and posting real system
 * notifications for budget alerts. These are genuine notifications posted via
 * [NotificationManagerCompat] (not a demo/toast) and appear in the status bar
 * and notification shade.
 */
object NotificationHelper {

    private const val CHANNEL_ID = "budget_alerts"
    private const val CHANNEL_NAME = "Budget Alerts"
    private const val CHANNEL_DESCRIPTION = "Alerts when you approach or exceed a category budget"

    private const val TXN_CHANNEL_ID = "sms_transactions"
    private const val TXN_CHANNEL_NAME = "Transaction Detection"
    private const val TXN_CHANNEL_DESCRIPTION = "Instant alert when a credit or debit is detected from an SMS"

    /**
     * Creates the notification channels. Safe to call multiple times - creating
     * an existing channel is a no-op. Should be called once at app startup.
     */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = CHANNEL_DESCRIPTION
                }
            )
            manager?.createNotificationChannel(
                NotificationChannel(TXN_CHANNEL_ID, TXN_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = TXN_CHANNEL_DESCRIPTION
                }
            )
        }
    }

    /**
     * Returns true if the app is allowed to post notifications. On Android 13+
     * this requires the POST_NOTIFICATIONS runtime permission.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Posts a budget alert notification for the given [alert].
     */
    fun showBudgetAlert(context: Context, alert: BudgetAlert, currencySymbol: String) {
        createChannel(context)

        if (!hasNotificationPermission(context)) return

        val spent = String.format(Locale.US, "%,.0f", alert.spent)
        val limit = String.format(Locale.US, "%,.0f", alert.limit)

        val title: String
        val message: String
        if (alert.isOverBudget) {
            val over = String.format(Locale.US, "%,.0f", alert.spent - alert.limit)
            title = "${alert.category} budget exceeded"
            message = "You've spent $currencySymbol$spent of your $currencySymbol$limit " +
                "${alert.category} budget - that's $currencySymbol$over over the limit."
        } else {
            val percent = if (alert.limit > 0) ((alert.spent / alert.limit) * 100).toInt() else 0
            title = "${alert.category} budget almost reached"
            message = "You've used $percent% ($currencySymbol$spent of $currencySymbol$limit) " +
                "of your ${alert.category} budget."
        }

        // Percentage of the budget used (clamped to 100 for the progress bar).
        val percent = if (alert.limit > 0) ((alert.spent / alert.limit) * 100).roundToInt() else 0
        val barProgress = percent.coerceIn(0, 100)
        val detail = "$percent% of $currencySymbol$limit ${alert.category} budget used"

        // Custom XML layouts (RemoteViews) rendered inside the notification shade,
        // decorated with the standard app-name/timestamp header.
        val collapsed = RemoteViews(context.packageName, R.layout.notification_budget_alert).apply {
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_text, message)
        }
        val expanded = RemoteViews(context.packageName, R.layout.notification_budget_alert_expanded).apply {
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_text, message)
            setProgressBar(R.id.notif_progress, 100, barProgress, false)
            setTextViewText(R.id.notif_detail, detail)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable._notification_1)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsed)
            .setCustomBigContentView(expanded)
            .setColor(ContextCompat.getColor(context, R.color.ef_cat_orange))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Use the category name hash as the id so each category gets its own
        // notification (and re-alerts replace the previous one for that category).
        NotificationManagerCompat.from(context)
            .notify(alert.category.hashCode(), notification)
    }

    /**
     * Posts an instant notification the moment a bank SMS is parsed into a
     * credit or debit — fired right after the transaction is auto-logged, so
     * the user sees it appear the same way a manual entry would look, just
     * without having to type it in.
     */
    fun showTransactionDetected(
        context: Context,
        title: String,
        amount: Double,
        isExpense: Boolean,
        currencySymbol: String,
    ) {
        createChannel(context)
        if (!hasNotificationPermission(context)) return

        val formatted = String.format(Locale.US, "%,.2f", amount)
        val notifTitle = if (isExpense) "Debit detected" else "Credit detected"
        val message = if (isExpense) {
            "$currencySymbol$formatted spent on $title — added to your expenses."
        } else {
            "$currencySymbol$formatted received from $title — added to your income."
        }
        val color = if (isExpense) R.color.ef_money_out else R.color.ef_money_in

        val notification = NotificationCompat.Builder(context, TXN_CHANNEL_ID)
            .setSmallIcon(R.drawable._notification_1)
            .setContentTitle(notifTitle)
            .setContentText(message)
            .setColor(ContextCompat.getColor(context, color))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(System.currentTimeMillis().toInt(), notification)
    }
}
