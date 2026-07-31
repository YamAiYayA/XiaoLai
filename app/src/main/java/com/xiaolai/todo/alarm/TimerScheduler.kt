package com.xiaolai.todo.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object TimerScheduler {
    const val ACTION_TIMER_FIRE = "com.xiaolai.todo.action.TIMER_FIRE"
    const val EXTRA_LABEL = "label"
    private const val REQUEST_CODE = 2001

    fun schedule(context: Context, delayMs: Long, label: String) {
        val appContext = context.applicationContext
        AlertNotifier.ensureChannel(appContext)

        val triggerAt = System.currentTimeMillis() + delayMs
        val operation = pendingOperation(appContext, label)
        val showIntent = PendingIntent.getActivity(
            appContext,
            REQUEST_CODE + 1,
            Intent(appContext, com.xiaolai.todo.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AlertNotifier.EXTRA_OPEN_TIMER, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        // setAlarmClock is the most reliable path for alarm-like behavior.
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAt, showIntent),
            operation,
        )
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pendingOperation(appContext, label = ""))
        AlertPlayer.stop()
    }

    fun canScheduleExact(context: Context): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun pendingOperation(context: Context, label: String): PendingIntent {
        val intent = Intent(context, TimerAlarmReceiver::class.java).apply {
            action = ACTION_TIMER_FIRE
            putExtra(EXTRA_LABEL, label)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
