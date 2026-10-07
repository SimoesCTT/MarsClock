package com.simoesctt.marsclock

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.floor

object MarsTime {

    const val SOLS_PER_YEAR_COMMON = 668
    const val SOLS_PER_YEAR_LEAP = 669
    private const val SOL_RATIO = 1.0274912517

    private val MONTH_NAMES = arrayOf(
        "Sagittarius", "Dhanus", "Capricornus", "Makara",
        "Aquarius", "Khumba", "Pisces", "Mina",
        "Aries", "Mesha", "Taurus", "Rishabha",
        "Gemini", "Mithuna", "Cancer", "Karka",
        "Leo", "Simha", "Virgo", "Kanya",
        "Libra", "Tula", "Scorpius", "Vrishika"
    )

    fun julianDate(millis: Long): Double = millis / 86400000.0 + 2440587.5

    fun marsSolDate(millis: Long): Double {
        val jd = julianDate(millis)
        return (jd - 2451549.5) / SOL_RATIO + 44796.0 - 0.0009626
    }

    fun mtcHours(millis: Long): Double {
        val msd = marsSolDate(millis)
        return (msd - floor(msd)) * 24.0
    }

    fun mtcString(millis: Long): String {
        val h = mtcHours(millis)
        val totalSec = (h * 3600.0).toLong()
        val hh = totalSec / 3600
        val mm = (totalSec % 3600) / 60
        val ss = totalSec % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hh, mm, ss)
    }

    fun monthName(m: Int): String = MONTH_NAMES[(m - 1).coerceIn(0, 23)]

    fun isLeapYear(year: Int): Boolean {
        if (year % 2 != 0) return false
        if (year % 100 != 0) return true
        return year % 500 == 0
    }

    fun monthLengthCommon(month: Int): Int = if (month % 2 == 1) 27 else 28

    fun monthLength(year: Int, month: Int): Int {
        val leap = isLeapYear(year)
        var len = monthLengthCommon(month)
        if (month == 6 || month == 12 || month == 18) len += 1
        if (month == 24) len += 1 + (if (leap) 1 else 0)
        return len
    }

    data class DarianDate(
        val year: Int,
        val month: Int,
        val sol: Int,
        val solOfYear: Int,
        val absoluteSol: Long
    )

    fun darian(msd: Double): DarianDate {
        val sols = floor(msd).toLong()
        var year = 0
        var remaining = sols
        while (true) {
            val yearLen = if (isLeapYear(year)) SOLS_PER_YEAR_LEAP else SOLS_PER_YEAR_COMMON
            if (remaining < yearLen) break
            remaining -= yearLen
            year++
        }
        val solOfYear = remaining.toInt() + 1
        var month = 1
        var solInMonth = solOfYear
        for (m in 1..24) {
            val len = monthLength(year, m)
            if (solInMonth <= len) { month = m; break }
            solInMonth -= len
        }
        return DarianDate(year, month, solInMonth, solOfYear, sols)
    }

    fun absoluteSolForDate(year: Int, month: Int, day: Int): Long {
        var total = 0L
        for (y in 0 until year) {
            total += if (isLeapYear(y)) SOLS_PER_YEAR_LEAP.toLong() else SOLS_PER_YEAR_COMMON.toLong()
        }
        for (m in 1 until month) total += monthLength(year, m).toLong()
        total += (day - 1).toLong()
        return total
    }

    private const val CURIOSITY_SOL0_MS = 1344235077000L
    private const val PERSEVERANCE_SOL0_MS = 1613681700000L
    private const val MARS_SOL_MS = 88775244.0

    fun curiositySol(nowMs: Long): Long = ((nowMs - CURIOSITY_SOL0_MS) / MARS_SOL_MS).toLong()
    fun perseveranceSol(nowMs: Long): Long = ((nowMs - PERSEVERANCE_SOL0_MS) / MARS_SOL_MS).toLong()

    fun earthUtcFull(nowMs: Long): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = nowMs }
        return String.format(Locale.US, "%04d-%02d-%02d  %02d:%02d:%02d UTC",
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND))
    }

    fun earthUtcShort(nowMs: Long): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = nowMs }
        return String.format(Locale.US, "Earth: %02d:%02d:%02d UTC",
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND))
    }
}
