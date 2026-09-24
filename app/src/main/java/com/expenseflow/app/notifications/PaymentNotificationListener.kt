package com.expenseflow.app.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
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

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val n = sbn.notification ?: return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = n.extras
        val text = listOfNotNull(
            extras.getCharSequence(Notification.EXTRA_TITLE),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT),
        ).joinToString(". ")
        if (text.isBlank()) return

        val appContext = applicationContext
        scope.launch {
            if (!SettingsRepository(appContext).settings.first().smsAutoDetect) return@launch
            val parsed = SmsParser.parseNotification(text) ?: return@launch
            PaymentLogger.log(appContext, parsed)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
