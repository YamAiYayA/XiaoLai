package com.xiaolai.todo.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TimerScheduler.ACTION_TIMER_FIRE) return
        val label = intent.getStringExtra(TimerScheduler.EXTRA_LABEL).orEmpty().ifBlank { "倒计时结束" }
        AlertPlayer.play(context)
        AlertNotifier.showFired(context, "$label · 提示音测试成功")
    }
}
