package com.xiaolai.todo.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.xiaolai.todo.alarm.AlertNotifier
import com.xiaolai.todo.alarm.AlertPlayer
import com.xiaolai.todo.alarm.TimerScheduler
import kotlinx.coroutines.delay

private data class TimerPreset(val label: String, val seconds: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerTestScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val presets = remember {
        listOf(
            TimerPreset("5秒", 5),
            TimerPreset("10秒", 10),
            TimerPreset("30秒", 30),
            TimerPreset("1分钟", 60),
        )
    }

    var selectedSeconds by remember { mutableStateOf(10) }
    var running by remember { mutableStateOf(false) }
    var remainingMs by remember { mutableLongStateOf(0L) }
    var status by remember { mutableStateOf("选一个时长，测试提示音 / 通知 / 震动") }
    var hasNotificationPermission by remember {
        mutableStateOf(isNotificationGranted(context))
    }
    var canExactAlarm by remember {
        mutableStateOf(TimerScheduler.canScheduleExact(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasNotificationPermission = granted
        status = if (granted) "通知权限已打开" else "未授予通知权限，到期可能没有横幅"
    }

    LaunchedEffect(running, remainingMs) {
        if (!running) return@LaunchedEffect
        if (remainingMs <= 0L) {
            running = false
            status = "倒计时结束：应听到提示音，并看到通知"
            return@LaunchedEffect
        }
        delay(200L)
        remainingMs = (remainingMs - 200L).coerceAtLeast(0L)
    }

    DisposableEffect(Unit) {
        AlertNotifier.ensureChannel(context)
        onDispose {
            // Keep scheduled system alarm; only stop local preview sound.
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("提示音测试", fontWeight = FontWeight.Bold)
                        Text(
                            text = "倒计时 · 闹钟声 · 通知 · 震动",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = formatMs(if (running) remainingMs else selectedSeconds * 1000L),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                presets.forEach { preset ->
                    FilterChip(
                        selected = selectedSeconds == preset.seconds,
                        onClick = {
                            if (!running) selectedSeconds = preset.seconds
                        },
                        label = { Text(preset.label) },
                        enabled = !running,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Button(
                onClick = {
                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= 33) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    canExactAlarm = TimerScheduler.canScheduleExact(context)
                    val label = "${selectedSeconds}秒测试"
                    TimerScheduler.schedule(context, selectedSeconds * 1000L, label)
                    remainingMs = selectedSeconds * 1000L
                    running = true
                    status = "已启动：可按 Home 键切到后台，到期仍应响铃/通知"
                },
                enabled = !running,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("开始倒计时")
            }

            OutlinedButton(
                onClick = {
                    TimerScheduler.cancel(context)
                    running = false
                    remainingMs = 0L
                    AlertPlayer.stop()
                    status = "已取消"
                },
                enabled = running,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("取消")
            }

            OutlinedButton(
                onClick = {
                    AlertPlayer.play(context)
                    status = "正在试听系统闹钟提示音"
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("立即试听提示音")
            }

            PermissionHints(
                hasNotificationPermission = hasNotificationPermission,
                canExactAlarm = canExactAlarm,
                onRequestNotification = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        hasNotificationPermission = true
                    }
                },
                onOpenExactAlarmSettings = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                        }
                        runCatching { context.startActivity(intent) }
                        // setAlarmClock usually works even without this, but keep the escape hatch.
                        canExactAlarm = context.getSystemService(AlarmManager::class.java)
                            ?.canScheduleExactAlarms() == true
                    }
                },
            )
        }
    }
}

@Composable
private fun PermissionHints(
    hasNotificationPermission: Boolean,
    canExactAlarm: Boolean,
    onRequestNotification: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("权限状态", fontWeight = FontWeight.SemiBold)
        Text(
            text = "通知：" + if (hasNotificationPermission) "已允许" else "未允许",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (!hasNotificationPermission) {
            OutlinedButton(onClick = onRequestNotification, modifier = Modifier.fillMaxWidth()) {
                Text("申请通知权限")
            }
        }
        Text(
            text = "精确闹钟：" + if (canExactAlarm) "可用" else "可能受限（仍可用系统闹钟通道）",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (!canExactAlarm) {
            OutlinedButton(onClick = onOpenExactAlarmSettings, modifier = Modifier.fillMaxWidth()) {
                Text("打开精确闹钟设置")
            }
        }
        Text(
            text = "建议：开始后按 Home 回到桌面，看后台能否准时响。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ((ms + 999) / 1000).toInt().coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun isNotificationGranted(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= 33) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}
