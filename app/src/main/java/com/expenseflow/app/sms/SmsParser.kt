package com.expenseflow.app.sms

/**
 * A transaction extracted from a bank/payment SMS body.
 */
data class ParsedSmsTransaction(
    val amount: Double,
    /** true = money left an account (debit/spend), false = money arrived (credit). */
    val isExpense: Boolean,
    /** Best-effort merchant / counterparty name, or a generic fallback. */
    val title: String,
)

/**
 * Heuristic, regex-based reader for bank/UPI/card transaction SMS. There's no
 * universal format — every bank phrases these differently — so this matches on
 * the vocabulary that's common across Indian and US bank templates rather than
 * one fixed template.
 *
 * Deliberately conservative: a message only becomes a transaction when it has
 * both a recognizable amount AND a debit/credit verb, and it's rejected outright
 * if it looks like an OTP, a promotional message, or a balance-only ping. False
 * negatives (a real transaction SMS that's missed) are far preferable to false
 * positives (a phantom transaction silently added to someone's ledger).
 */
object SmsParser {

    // "Rs.", "Rs", "INR", "₹", or "$" followed by a number with optional commas/decimals.
    private val amountRegex = Regex(
        """(?:Rs\.?|INR|₹|\$|USD)\s?([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE,
    )

    private val debitWords = Regex(
        """\b(debited|spent|paid|purchase|withdrawn|deducted|debit)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val creditWords = Regex(
        """\b(credited|received|deposited|credit)\b""",
        RegexOption.IGNORE_CASE,
    )

    // Messages that mention money but aren't a completed transaction — skip these
    // even if they'd otherwise match (OTPs, promos, reminders, balance checks).
    private val exclusionWords = Regex(
        """\b(otp|one time password|will be debited|will be credited|due date|minimum due|reminder|offer|cashback offer|available balance is|avl bal is)\b""",
        RegexOption.IGNORE_CASE,
    )

    // Merchant name after "at"/"to"/"on" (UPI/card SMS commonly read "... at AMAZON on 05-10-24").
    private val merchantRegex = Regex(
        """\b(?:at|to)\s+([A-Za-z0-9][A-Za-z0-9&.'*_\- ]{1,28}?)(?:\s+on\b|\s+dated\b|[.,]|$)""",
        RegexOption.IGNORE_CASE,
    )

    // Payment-app notifications ("Paid ₹10 to X", "₹10 sent to X", "Payment of ₹10 successful")
    // use looser wording than bank SMS, so they get a few extra verbs.
    private val notificationDebitWords = Regex(
        """\b(sent|payment successful|payment of .{1,20} successful|transferred)\b""",
        RegexOption.IGNORE_CASE,
    )

    /** Like [parse], but also understands payment-app notification phrasing. */
    fun parseNotification(text: String): ParsedSmsTransaction? =
        parse(text) ?: parse(text, extraDebit = true)

    fun parse(body: String, extraDebit: Boolean = false): ParsedSmsTransaction? {
        val text = body.trim()
        if (text.isEmpty()) return null
        if (exclusionWords.containsMatchIn(text)) return null

        val isDebit = debitWords.containsMatchIn(text) ||
            (extraDebit && notificationDebitWords.containsMatchIn(text))
        val isCredit = creditWords.containsMatchIn(text)
        // Ambiguous (mentions both, or neither) — not confident enough to auto-log.
        if (isDebit == isCredit) return null

        val amountMatch = amountRegex.find(text) ?: return null
        val amount = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        val merchant = merchantRegex.find(text)?.groupValues?.get(1)?.trim()
        val title = when {
            !merchant.isNullOrBlank() -> merchant.split(" ").joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
            isDebit -> "Card/UPI payment"
            else -> "Bank credit"
        }

        return ParsedSmsTransaction(amount = amount, isExpense = isDebit, title = title)
    }
}
