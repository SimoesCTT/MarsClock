package com.simoesctt.marsclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.simoesctt.marsclock.data.AppDb
import com.simoesctt.marsclock.data.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ReminderScheduler {

    fun schedule(ctx: Context, task: Task) {
        if (!task.remind || task.mtcHour < 0) return
        val whenMs = MarsReminder.earthMillisFor(task.solDate, task.mtcHour, task.mtcMinute)
        if (whenMs < System.currentTimeMillis()) return
        setAlarm(ctx, task.id, task.title, task.note, whenMs)
    }

    fun cancel(ctx: Context, task: Task) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(ctx, task.id, task.title, task.note))
    }

    fun rescheduleAll(ctx: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            AppDb.get(ctx).tasks().allReminders().forEach { schedule(ctx, it) }
        }
    }

    private fun setAlarm(ctx: Context, id: Long, title: String, note: String, whenMs: Long) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(ctx, id, title, note)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pi)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pi)
                }
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pi)
        }
    }

    private fun pending(ctx: Context, id: Long, title: String, note: String): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("note", note)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(ctx, id.toInt(), i, flags)
    }
}
