package com.expenseflow.app.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.expenseflow.app.data.SettingsRepository
import com.expenseflow.app.sms.PaymentLogger
import com.expenseflow.app.sms.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Reads payment-related notifications from any app (Paytm, GPay, PhonePe,
 * banks, SMS apps…) and logs them as transactions, the same way incoming bank
 * SMS are handled. Needs the user to grant "notification access" in system
 * settings. Follows the "Auto-detect" setting, and ignores our own
 * notifications so a detected payment can't trigger itself.
 */
class PaymentNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        Log.d(TAG, "listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val n = sbn.notification ?: return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = n.extras
        val text = listOfNotNull(
            extras.getCharSequence(Notification.EXTRA_TITLE),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT),
        ).joinToString(". ")
        Log.d(TAG, "posted from ${sbn.packageName}")
        if (text.isBlank()) return
        // Android 15+ swaps in this placeholder for notifications it treats as sensitive (OTPs,
        // some payment alerts); there's nothing to parse, so don't waste a coroutine on it.
        if (text.contains("Sensitive notification content hidden")) return

        val appContext = applicationContext
        scope.launch {
            try {
                if (!SettingsRepository(appContext).settings.first().smsAutoDetect) {
                    Log.d(TAG, "auto-detect off, skipping")
                    return@launch
                }
                val parsed = SmsParser.parseNotification(text)
                if (parsed == null) {
                    Log.d(TAG, "not a payment, skipping")
                    return@launch
                }
                Log.d(TAG, "logging detected payment")
                PaymentLogger.log(appContext, parsed)
            } catch (e: Exception) {
                Log.e(TAG, "failed to log payment notification", e)
            }
        }
    }

    private companion object {
        const val TAG = "PaymentNotifListener"
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
