package com.simoesctt.marsclock

import kotlin.math.floor

object MarsReminder {
    private const val SOL_RATIO = 1.0274912517
    private const val JD_UNIX_EPOCH = 2440587.5

    /** Earth millis when sol+mtc hour:minute occurs. */
    fun earthMillisFor(sol: Long, mtcHour: Int, mtcMinute: Int): Long {
        val fracOfSol = (mtcHour * 3600.0 + mtcMinute * 60.0) / 86400.0
        val msd = sol.toDouble() + fracOfSol
        val jd = (msd - 44796.0 + 0.0009626) * SOL_RATIO + 2451549.5
        return ((jd - JD_UNIX_EPOCH) * 86400000.0).toLong()
    }

    /** Current sol (integer). */
    fun todaySol(nowMs: Long): Long =
        floor(MarsTime.marsSolDate(nowMs)).toLong()
}
