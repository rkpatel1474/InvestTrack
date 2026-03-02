package com.investtrack.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    fun Long.toDisplayDate(): String = displayFormat.format(Date(this))
    fun Long.toMonthYear(): String = monthFormat.format(Date(this))
    fun String.parseDate(): Long? = runCatching { displayFormat.parse(this)?.time }.getOrNull()
    fun today(): Long = System.currentTimeMillis()

    fun yearsBetween(startMillis: Long, endMillis: Long): Double {
        return (endMillis - startMillis) / (365.25 * 24 * 3600 * 1000.0)
    }

    fun addMonths(epochMillis: Long, months: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochMillis
            add(Calendar.MONTH, months)
        }
        return cal.timeInMillis
    }

    fun nextCouponDates(firstCouponDate: Long, frequency: com.investtrack.data.database.entities.CouponFrequency, count: Int): List<Long> {
        val monthsInterval = when (frequency) {
            com.investtrack.data.database.entities.CouponFrequency.MONTHLY -> 1
            com.investtrack.data.database.entities.CouponFrequency.QUARTERLY -> 3
            com.investtrack.data.database.entities.CouponFrequency.HALF_YEARLY -> 6
            com.investtrack.data.database.entities.CouponFrequency.ANNUALLY -> 12
        }
        return (0 until count).map { i -> addMonths(firstCouponDate, i * monthsInterval) }
    }
}

fun Double.formatAs2Dec() = "%.2f".format(this)
