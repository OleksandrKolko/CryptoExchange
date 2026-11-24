package com.example.cryptoexchange.presentation.components

import java.math.BigDecimal
import java.math.MathContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val symbols = DecimalFormatSymbols(Locale.US)

/**
 * Ціна без «втрати» малих значень:
 *  ≥ 1      → 2 знаки з розділювачем тисяч: 98,123.45
 *  < 1      → 4 значущі цифри: 0.00001234 (а не 0.00)
 *  null     → «Немає даних» (а не 0)
 */
fun formatPrice(value: Double?): String {
    if (value == null) return "Немає даних"
    return if (value >= 1.0) {
        "$" + DecimalFormat("#,##0.00", symbols).format(value)
    } else {
        "$" + BigDecimal(value).round(MathContext(4)).stripTrailingZeros().toPlainString()
    }
}

/** Великі суми: 1.23T, 211.2B, 15.4M */
fun formatLargeUsd(value: Double?): String {
    if (value == null) return "Немає даних"
    val (divider, suffix) = when {
        value >= 1e12 -> 1e12 to "T"
        value >= 1e9 -> 1e9 to "B"
        value >= 1e6 -> 1e6 to "M"
        else -> 1.0 to ""
    }
    return "$" + DecimalFormat("#,##0.##", symbols).format(value / divider) + suffix
}

fun formatPercent(value: Double?): String =
    value?.let { (if (it >= 0) "+" else "") + DecimalFormat("0.00", symbols).format(it) + "%" }
        ?: "Немає даних"

fun formatDateTime(millis: Long?): String =
    millis?.let { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(it)) }
        ?: "—"
