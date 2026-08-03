package com.xiaolai.todo.ui

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import android.app.TimePickerDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.xiaolai.todo.model.BabyRecord
import com.xiaolai.todo.model.BabyTodo
import com.xiaolai.todo.model.EventTypeOption
import com.xiaolai.todo.model.EventTypes
import com.xiaolai.todo.model.SummaryCounts
import com.xiaolai.todo.session.EventTypePrefs
import com.xiaolai.todo.session.FormulaFavoritesPrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

@Composable
fun HomePane(
    state: BabyUiState,
    onRefresh: () -> Unit,
    onQuickAdd: (String) -> Unit,
    onOpenEditor: () -> Unit,
    onOpenTimeline: () -> Unit,
) {
    val baby = state.context?.baby
    val today = state.dashboard.todaySummary
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = baby?.nickname?.let { "${it}养成记" } ?: "妍妍养成记",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (baby != null) "把喂养、睡眠、护理和成长节奏都放进今天的时间轴里。"
                else "先登录家庭后再开始记录。",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            )
        }
        item {
            Text("今天概览", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("奶粉总量", "${today.formulaAmountTotal.toInt()}ml", Modifier.weight(1f))
                StatCard("热母乳", "${today.warmBreastAmountTotal.toInt()}ml", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("喂养次数", "${today.feedCountTotal}次", Modifier.weight(1f))
                StatCard("排便次数", "${today.poopCount}次", Modifier.weight(1f))
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("快捷新增", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Button(onClick = onOpenEditor) { Text("去新增页") }
            }
            Text("默认使用当前时间", style = MaterialTheme.typography.bodySmall)
            val typePrefs = EventTypePrefs(LocalContext.current)
            FlowRowChips(
                EventTypes.visible(typePrefs.hiddenTypes())
                    .filter { !it.needsNote && !it.needsCustom },
            ) { option ->
                onQuickAdd(option.value)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("最近记录", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onOpenTimeline) { Text("查看全部") }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = onRefresh) { Text("刷新") }
            }
        }
        if (state.dashboard.recentRecords.isEmpty()) {
            item { EmptyCard("今天还没有记录", "点上面的快捷按钮，先记下第一条宝宝日常。") }
        } else {
            items(state.dashboard.recentRecords, key = { it.id }) { RecordRow(it) }
        }
        item {
            Text("近 7 天", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    "奶粉总量",
                    "${state.dashboard.weekSummary.formulaAmountTotal.toInt()}ml",
                    Modifier.weight(1f),
                )
                StatCard(
                    "热母乳",
                    "${state.dashboard.weekSummary.warmBreastAmountTotal.toInt()}ml",
                    Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    "喂养次数",
                    "${state.dashboard.weekSummary.feedCountTotal}次",
                    Modifier.weight(1f),
                )
                StatCard(
                    "总记录数",
                    "${state.dashboard.weekSummary.totalCount}条",
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorPane(
    state: BabyUiState,
    presetEventType: String? = null,
    onPresetConsumed: () -> Unit = {},
    onOpenAgeTable: () -> Unit,
    onCreate: (
        eventType: String,
        payload: JSONObject,
        occurredAt: Long,
        dateKey: String,
        onDone: () -> Unit,
    ) -> Unit,
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(EventTypes.all.first()) }
    var amount by rememberSaveable { mutableStateOf("40") }
    var side by rememberSaveable { mutableStateOf("left") }
    var durationMin by rememberSaveable { mutableStateOf("15") }
    var note by rememberSaveable { mutableStateOf("") }
    var customLabel by rememberSaveable { mutableStateOf("") }
    var forehead by rememberSaveable { mutableStateOf("") }
    var chest by rememberSaveable { mutableStateOf("") }
    val favoritesPrefs = remember { FormulaFavoritesPrefs(context) }
    var favorites by remember { mutableStateOf(favoritesPrefs.get()) }
    var showMoreAmounts by remember { mutableStateOf(false) }
    var pendingRemoveFavorite by remember { mutableStateOf<Int?>(null) }
    var pendingInstant by remember { mutableStateOf<EventTypeOption?>(null) }
    var showSuccess by remember { mutableStateOf(false) }
    var successText by remember { mutableStateOf("记录已保存") }
    val typePrefs = remember { EventTypePrefs(context) }
    var hiddenTypes by remember { mutableStateOf(typePrefs.hiddenTypes()) }
    var pendingHide by remember { mutableStateOf<EventTypeOption?>(null) }
    val visibleTypes = remember(hiddenTypes) { EventTypes.visible(hiddenTypes) }

    LaunchedEffect(presetEventType) {
        val type = presetEventType ?: return@LaunchedEffect
        val option = EventTypes.visible(hiddenTypes).firstOrNull { it.value == type }
            ?: EventTypes.of(type)
        selected = option
        onPresetConsumed()
    }

    var occurredAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var followNow by remember { mutableStateOf(true) }
    var nowTick by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(followNow) {
        while (followNow) {
            nowTick = System.currentTimeMillis()
            occurredAt = nowTick
            delay(1000)
        }
    }

    val displayTs = if (followNow) nowTick else occurredAt
    val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(displayTs))
    val timeLabel = SimpleDateFormat("HH:mm:ss", Locale.CHINA).format(Date(displayTs))
    val summary = remember(state.dashboard.recentRecords, nowTick, state.context?.baby) {
        buildRealtimeSummary(state)
    }

    fun openDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = displayTs }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                followNow = false
                val next = Calendar.getInstance().apply {
                    timeInMillis = occurredAt
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                occurredAt = next.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    fun openTimePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = displayTs }
        TimePickerDialog(
            context,
            { _, hour, minute ->
                followNow = false
                val next = Calendar.getInstance().apply {
                    timeInMillis = occurredAt
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                occurredAt = next.timeInMillis
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true,
        ).show()
    }

    fun save(option: EventTypeOption = selected, instant: Boolean = false) {
        val payload = JSONObject()
        when {
            option.needsAmount -> payload.put("amountMl", amount.toIntOrNull() ?: 0)
            option.needsSide -> {
                payload.put("side", side)
                payload.put("durationMin", durationMin.toIntOrNull() ?: 0)
            }
            option.needsJaundice -> {
                payload.put("foreheadValue", forehead.toDoubleOrNull() ?: 0.0)
                payload.put("chestValue", chest.toDoubleOrNull() ?: 0.0)
            }
            option.needsCustom -> payload.put("customLabel", customLabel.trim())
            option.needsNote -> payload.put("note", note.trim())
        }
        if (note.isNotBlank() && !option.needsNote) {
            payload.put("note", note.trim())
        }
        val ts = if (instant || followNow) System.currentTimeMillis() else occurredAt
        val key = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(ts))
        onCreate(option.value, payload, ts, key) {
            successText = "${option.label}已保存"
            showSuccess = true
            pendingInstant = null
            if (!instant) {
                note = ""
                customLabel = ""
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenAgeTable),
                    ) {
                        Text(summary.babyAgeLabel, fontWeight = FontWeight.SemiBold)
                        Text(
                            "点击查看出生天数表",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        selected.label,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(summary.sleepLabel)
                Text(summary.feedLabel)
                Text(summary.buttCleanLabel)
            }
        }

        item {
            SurfaceCard {
                Text("记录时间", fontWeight = FontWeight.SemiBold)
                Text(
                    if (followNow) "跟随当前时间（点日期/时间可手动改）" else "已手动设定时间",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TimeSelectCard(
                        label = "日期",
                        value = dateKey,
                        onClick = { openDatePicker() },
                        modifier = Modifier.weight(1f),
                    )
                    TimeSelectCard(
                        label = "时间",
                        value = timeLabel,
                        onClick = { openTimePicker() },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(1, 3, 5, 10, 15).forEach { minutes ->
                        OutlinedButton(
                            onClick = {
                                followNow = false
                                occurredAt = System.currentTimeMillis() - minutes * 60_000L
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) { Text("${minutes}分钟前") }
                    }
                    Button(
                        onClick = { followNow = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) { Text("现在") }
                }
            }
        }

        item {
            Text("记录类型", fontWeight = FontWeight.SemiBold)
            Text(
                "长按可隐藏阶段性不用的类型",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                visibleTypes.forEach { option ->
                    EventTypeChip(
                        option = option,
                        selected = selected.value == option.value,
                        onClick = {
                            selected = option
                            if (option.instantConfirm) {
                                pendingInstant = option
                            }
                        },
                        onLongClick = { pendingHide = option },
                    )
                }
            }
            if (hiddenTypes.isNotEmpty()) {
                TextButton(onClick = {
                    typePrefs.restoreAll()
                    hiddenTypes = emptySet()
                }) { Text("恢复已隐藏类型") }
            }
        }

        if (selected.needsAmount) {
            item {
                SurfaceCard {
                    Text("奶量（ml）", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { ch -> ch.isDigit() } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("输入奶量") },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        favorites.forEach { value ->
                            AmountFavoriteChip(
                                value = value,
                                selected = amount == value.toString(),
                                onClick = { amount = value.toString() },
                                onLongClick = { pendingRemoveFavorite = value },
                            )
                        }
                        AssistChip(
                            onClick = { showMoreAmounts = !showMoreAmounts },
                            label = { Text(if (showMoreAmounts) "收起" else "更多") },
                        )
                    }
                    Text(
                        "点「更多」可把奶量移到常驻；长按常驻可移回更多。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    if (showMoreAmounts) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            EventTypes.formulaQuickValues
                                .filter { it !in favorites }
                                .forEach { value ->
                                    AssistChip(
                                        onClick = {
                                            amount = value.toString()
                                            favorites = favoritesPrefs.add(value)
                                            showMoreAmounts = false
                                        },
                                        label = { Text("${value}ml") },
                                    )
                                }
                        }
                    }
                }
            }
        }

        if (selected.needsSide) {
            item {
                SurfaceCard {
                    Text("侧别", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = side == "left", onClick = { side = "left" }, label = { Text("左侧") })
                        FilterChip(selected = side == "right", onClick = { side = "right" }, label = { Text("右侧") })
                    }
                    OutlinedTextField(
                        value = durationMin,
                        onValueChange = { durationMin = it.filter { ch -> ch.isDigit() } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("时长（分钟）") },
                    )
                }
            }
        }

        if (selected.needsJaundice) {
            item {
                SurfaceCard {
                    Text("测黄疸", fontWeight = FontWeight.SemiBold)
                    Text("每次测量请同时记录额头和胸口数值。")
                    OutlinedTextField(
                        value = forehead,
                        onValueChange = { forehead = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("额头") },
                    )
                    OutlinedTextField(
                        value = chest,
                        onValueChange = { chest = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("胸口") },
                    )
                }
            }
        }

        if (selected.needsCustom) {
            item {
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it.take(20) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("做了什么") },
                    placeholder = { Text("如：游泳、早教、剪指甲") },
                )
            }
        }

        item {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("备注") },
                placeholder = { Text("如：洗屁股后喂 50ml、精神不错、睡前哭闹") },
                minLines = 2,
            )
            Text(
                "图片上传下一版再补（小程序最多 3 张）。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { save(selected, instant = false) },
                    enabled = !state.loading,
                    modifier = Modifier.weight(1f),
                ) { Text("保存记录") }
                OutlinedButton(
                    onClick = {
                        amount = favorites.firstOrNull()?.toString() ?: "40"
                        side = "left"
                        durationMin = "15"
                        note = ""
                        customLabel = ""
                        forehead = ""
                        chest = ""
                        followNow = true
                        selected = visibleTypes.firstOrNull() ?: EventTypes.all.first()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("清空重写") }
            }
            if (state.message.isNotBlank()) {
                Text(state.message, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    pendingInstant?.let { option ->
        AlertDialog(
            onDismissRequest = { pendingInstant = null },
            title = { Text(option.label) },
            text = { Text("以此刻记录吗？") },
            confirmButton = {
                TextButton(onClick = { save(option, instant = true) }) { Text("确定（瞬间记录）") }
            },
            dismissButton = {
                TextButton(onClick = { pendingInstant = null }) { Text("取消（进入详情）") }
            },
        )
    }

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = { showSuccess = false },
            title = { Text("保存成功") },
            text = { Text(successText) },
            confirmButton = {
                TextButton(onClick = { showSuccess = false }) { Text("好的") }
            },
        )
    }

    pendingHide?.let { option ->
        AlertDialog(
            onDismissRequest = { pendingHide = null },
            title = { Text("隐藏「${option.label}」？") },
            text = { Text("适合阶段性不用的类型。可在下方点「恢复已隐藏类型」。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        typePrefs.hide(option.value)
                        hiddenTypes = typePrefs.hiddenTypes()
                        if (selected.value == option.value) {
                            selected = EventTypes.visible(hiddenTypes).firstOrNull()
                                ?: EventTypes.all.first()
                        }
                        pendingHide = null
                    },
                ) { Text("隐藏") }
            },
            dismissButton = {
                TextButton(onClick = { pendingHide = null }) { Text("取消") }
            },
        )
    }

    pendingRemoveFavorite?.let { value ->
        AlertDialog(
            onDismissRequest = { pendingRemoveFavorite = null },
            title = { Text("移出常驻奶量？") },
            text = { Text("将 ${value}ml 从常驻移回「更多」。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        favorites = favoritesPrefs.remove(value)
                        pendingRemoveFavorite = null
                    },
                ) { Text("移出") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveFavorite = null }) { Text("取消") }
            },
        )
    }
}


@Composable
fun AgeCalendarPane(
    birthday: String,
    nickname: String,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val rows = remember(birthday) { buildAgeTableRows(birthday) }
    val todayIndex = remember(rows) { rows.indexOfFirst { it.isToday } }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showSearch by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }
    var searchError by remember { mutableStateOf<String?>(null) }
    var didInitialScroll by remember { mutableStateOf(false) }

    fun jumpToIndex(index: Int) {
        if (index !in rows.indices) return
        scope.launch {
            listState.animateScrollToItem(index)
        }
    }

    LaunchedEffect(rows, todayIndex) {
        if (!didInitialScroll && todayIndex >= 0) {
            listState.scrollToItem(todayIndex)
            didInitialScroll = true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("返回") }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (nickname.isBlank()) "出生天数表" else "${nickname}出生天数表",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (birthday.isBlank()) "未设置生日" else "生日 $birthday · 出生当天算第 1 天",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { jumpToIndex(0) },
                enabled = rows.isNotEmpty(),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            ) { Text("到顶") }
            Button(
                onClick = { if (todayIndex >= 0) jumpToIndex(todayIndex) },
                enabled = todayIndex >= 0,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            ) { Text("今天") }
            OutlinedButton(
                onClick = {
                    searchInput = ""
                    searchError = null
                    showSearch = true
                },
                enabled = rows.isNotEmpty(),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            ) { Text("搜索") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text("日期", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.2f))
            Text("出生天数", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("月日", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        if (rows.isEmpty()) {
            Box(modifier = Modifier.padding(16.dp)) {
                EmptyCard("暂无数据", "请先在家庭资料里设置宝宝生日。")
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(rows, key = { it.dateKey }) { row ->
                    val bg = if (row.isToday) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    } else {
                        Color.Transparent
                    }
                    val weight = if (row.isToday) FontWeight.Bold else FontWeight.Normal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(bg)
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (row.isToday) "${row.dateKey} · 今天" else row.dateKey,
                            modifier = Modifier.weight(1.2f),
                            fontWeight = weight,
                            color = if (row.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "第${row.dayIndex}天",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontWeight = weight,
                            color = if (row.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            row.monthDayLabel,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End,
                            fontWeight = weight,
                            color = if (row.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }

    if (showSearch) {
        AlertDialog(
            onDismissRequest = { showSearch = false },
            title = { Text("搜索日期或天数") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "可输入日期（如 2026-08-01）或天数（如 30 / 第30天）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    )
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = {
                            searchInput = it
                            searchError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("日期或天数") },
                        placeholder = { Text("2026-08-01 或 30") },
                    )
                    if (searchError != null) {
                        Text(searchError!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val index = findAgeTableIndex(rows, searchInput)
                        if (index == null) {
                            searchError = "未找到对应日期或天数"
                        } else {
                            showSearch = false
                            jumpToIndex(index)
                        }
                    },
                ) { Text("跳转") }
            },
            dismissButton = {
                TextButton(onClick = { showSearch = false }) { Text("取消") }
            },
        )
    }
}

private data class AgeTableRow(
    val dateKey: String,
    val dayIndex: Int,
    val monthDayLabel: String,
    val isToday: Boolean,
)

private fun buildAgeTableRows(birthday: String): List<AgeTableRow> {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    val birthDate = runCatching { fmt.parse(birthday) }.getOrNull() ?: return emptyList()
    val birth = Calendar.getInstance().apply {
        time = birthDate
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (birth.after(today)) return emptyList()
    val end = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 365) }
    val todayKey = fmt.format(today.time)
    val rows = mutableListOf<AgeTableRow>()
    val cursor = birth.clone() as Calendar
    // 出生当天算第 1 天
    var dayIndex = 1
    while (!cursor.after(end)) {
        val (months, days) = calendarAgeMonthsDays(birth, cursor)
        val dateKey = fmt.format(cursor.time)
        rows += AgeTableRow(
            dateKey = dateKey,
            dayIndex = dayIndex,
            monthDayLabel = "${months}月${days}天",
            isToday = dateKey == todayKey,
        )
        cursor.add(Calendar.DAY_OF_MONTH, 1)
        dayIndex++
        if (dayIndex > 8000) break
    }
    return rows
}

private fun findAgeTableIndex(rows: List<AgeTableRow>, raw: String): Int? {
    val query = raw.trim()
    if (query.isEmpty() || rows.isEmpty()) return null

    val digits = query.filter { it.isDigit() }
    if (digits.length == 8) {
        val key = "${digits.substring(0, 4)}-${digits.substring(4, 6)}-${digits.substring(6, 8)}"
        val idx = rows.indexOfFirst { it.dateKey == key }
        if (idx >= 0) return idx
    }

    val normalized = query
        .replace('/', '-')
        .replace('.', '-')
        .replace('年', '-')
        .replace('月', '-')
        .replace("日", "")
        .trim('-')
    val idxExact = rows.indexOfFirst { it.dateKey == normalized }
    if (idxExact >= 0) return idxExact

    // 天数：30 / 第30天 / 30天（避免把完整日期当成天数）
    val dayMatch = Regex("""^(?:第)?\s*(\d{1,4})\s*天?$""").find(query)
    if (dayMatch != null) {
        val day = dayMatch.groupValues[1].toIntOrNull()
        if (day != null) {
            val idx = rows.indexOfFirst { it.dayIndex == day }
            if (idx >= 0) return idx
        }
    }

    if (digits.length == 4) {
        val mmdd = "${digits.substring(0, 2)}-${digits.substring(2, 4)}"
        val idx = rows.indexOfFirst { it.dateKey.endsWith("-$mmdd") }
        if (idx >= 0) return idx
    }

    val idxLoose = rows.indexOfFirst { it.dateKey.endsWith(normalized) || it.dateKey.contains(normalized) }
    return idxLoose.takeIf { it >= 0 }
}

private fun calendarAgeMonthsDays(birth: Calendar, target: Calendar): Pair<Int, Int> {
    var months = (target.get(Calendar.YEAR) - birth.get(Calendar.YEAR)) * 12 +
        (target.get(Calendar.MONTH) - birth.get(Calendar.MONTH))
    var days = target.get(Calendar.DAY_OF_MONTH) - birth.get(Calendar.DAY_OF_MONTH)
    if (days < 0) {
        months -= 1
        val prevMonth = (target.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        days += prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    return months.coerceAtLeast(0) to days.coerceAtLeast(0)
}

@Composable
private fun TimeSelectCard(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp),
            )
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        )
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "点击修改",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun TimelinePane(
    state: BabyUiState,
    onDateChange: (String) -> Unit,
    onDeleteRecord: (String) -> Unit,
) {
    var viewMode by rememberSaveable { mutableStateOf("day") }
    val cal = remember { Calendar.getInstance() }
    val summary = if (viewMode == "day") {
        summarize(state.timeline)
    } else {
        state.dashboard.weekSummary
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("记录", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = {
                    cal.time = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).parse(state.timelineDateKey)!!
                    cal.add(Calendar.DAY_OF_MONTH, -1)
                    onDateChange(SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(cal.time))
                }) { Text("前一天") }
                Text(
                    state.timelineDateKey,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedButton(onClick = {
                    cal.time = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).parse(state.timelineDateKey)!!
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                    onDateChange(SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(cal.time))
                }) { Text("后一天") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = viewMode == "day", onClick = { viewMode = "day" }, label = { Text("当天") })
                FilterChip(selected = viewMode == "week", onClick = { viewMode = "week" }, label = { Text("近 7 天") })
            }
        }
        item {
            Text(if (viewMode == "day") "当天汇总" else "近 7 天汇总", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("奶粉总量", "${summary.formulaAmountTotal.toInt()}ml", Modifier.weight(1f))
                StatCard("热母乳", "${summary.warmBreastAmountTotal.toInt()}ml", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("喂养次数", "${summary.feedCountTotal}次", Modifier.weight(1f))
                StatCard("排便", "${summary.poopCount}次", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("总记录", "${summary.totalCount}条", Modifier.weight(1f))
            }
        }
        item {
            Text(
                "列表项从右向左滑可删除",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        }
        if (viewMode == "day") {
            if (state.timeline.isEmpty()) {
                item { EmptyCard("这一天还没有记录", "去新增页记一条吧。") }
            } else {
                items(state.timeline, key = { it.id }) { record ->
                    SwipeDeleteRecordRow(record = record, onDelete = { onDeleteRecord(record.id) })
                }
            }
        } else {
            items(state.dashboard.recentRecords, key = { it.id }) { record ->
                SwipeDeleteRecordRow(record = record, onDelete = { onDeleteRecord(record.id) })
            }
        }
    }
}

@Composable
fun TodosPane(
    state: BabyUiState,
    onAdd: (String, String) -> Unit,
    onToggle: (String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }
    var showPending by rememberSaveable { mutableStateOf(true) }
    var showDone by rememberSaveable { mutableStateOf(true) }

    val filtered = state.todos.filter {
        filter == "all" || it.category == filter
    }
    val pending = filtered.filter { !it.isDone }
    val done = filtered.filter { it.isDone }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("待办清单", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("手动记录宝宝和大人的待办事项")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter == "all", onClick = { filter = "all" }, label = { Text("全部") })
                FilterChip(selected = filter == "baby", onClick = { filter = "baby" }, label = { Text("宝宝") })
                FilterChip(selected = filter == "adult", onClick = { filter = "adult" }, label = { Text("大人") })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("新建待办") },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (title.isNotBlank()) {
                        onAdd(title.trim(), if (filter == "adult") "adult" else "baby")
                        title = ""
                    }
                }) { Text("新建") }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPending = !showPending }
                    .padding(vertical = 4.dp),
            ) {
                Text("待完成", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${if (showPending) "收起" else "展开"} · ${pending.size} 条")
            }
        }
        if (showPending) {
            if (pending.isEmpty()) {
                item { EmptyCard("暂无待办", "点击新建，添加第一条待办。") }
            } else {
                items(pending, key = { it.id }) { TodoRow(it) { onToggle(it.id) } }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDone = !showDone }
                    .padding(vertical = 4.dp),
            ) {
                Text("已完成", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${if (showDone) "收起" else "展开"} · ${done.size} 条")
            }
        }
        if (showDone) {
            items(done, key = { it.id }) { TodoRow(it) { onToggle(it.id) } }
        }
    }
}

@Composable
fun ProfilePane(state: BabyUiState, onLogout: () -> Unit, onRefresh: () -> Unit) {
    val ctx = state.context
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("家庭与宝宝", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("邀请码、成员、宝宝档案都在这里维护。")
        }
        item { InfoCard("家庭", ctx?.family?.name ?: "-") }
        item { InfoCard("邀请码", ctx?.family?.inviteCode ?: "-") }
        item { InfoCard("宝宝昵称", ctx?.baby?.nickname ?: "-") }
        item { InfoCard("宝宝生日", ctx?.baby?.birthday ?: "-") }
        item { InfoCard("备注", ctx?.baby?.note ?: "-") }
        item { InfoCard("当前身份", ctx?.member?.displayName ?: "-") }
        item {
            Text("家庭成员", fontWeight = FontWeight.SemiBold)
            ctx?.members?.forEach { Text("· ${it.displayName}（${it.role}）") }
        }
        item {
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("刷新资料") }
            Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("退出登录") }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SurfaceCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = { content() },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowChips(options: List<EventTypeOption>, onClick: (EventTypeOption) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            AssistChip(onClick = { onClick(option) }, label = { Text(option.label) })
        }
    }
}

@Composable
private fun EmptyCard(title: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(desc, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
    }
}

@Composable
private fun SwipeDeleteRecordRow(
    record: BabyRecord,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE85D75))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("删除", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
    ) {
        RecordRow(record)
    }
}

@Composable
private fun RecordRow(record: BabyRecord) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
    ) {
        Row {
            Text(EventTypes.summarize(record), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(formatOccurred(record.occurredAt))
        }
        Text(
            "${EventTypes.labelOf(record.eventType)} · ${record.createdByName.ifBlank { "家人" }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AmountFavoriteChip(
    value: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val bg = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val border = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    }
    Text(
        text = "${value}ml",
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        textAlign = TextAlign.Center,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EventTypeChip(
    option: EventTypeOption,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val bg = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val border = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    }
    Text(
        text = option.label,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun TodoRow(todo: BabyTodo, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Checkbox(checked = todo.isDone, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f)) {
            Text(
                todo.title,
                textDecoration = if (todo.isDone) TextDecoration.LineThrough else null,
            )
            Text(
                if (todo.category == "adult") "大人" else "宝宝",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun InfoCard(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

private data class RealtimeSummary(
    val babyAgeLabel: String,
    val sleepLabel: String,
    val feedLabel: String,
    val buttCleanLabel: String,
)

private fun buildRealtimeSummary(state: BabyUiState): RealtimeSummary {
    val baby = state.context?.baby
    val records = state.dashboard.recentRecords
    val now = System.currentTimeMillis()
    val age = baby?.birthday?.let { birthdayLabel(it, now) } ?: "宝宝记录"
    val lastSleepStart = records.firstOrNull { it.eventType == "sleep_start" }
    val lastSleepEnd = records.firstOrNull { it.eventType == "sleep_end" }
    val sleeping = lastSleepStart != null &&
        (lastSleepEnd == null || lastSleepStart.occurredAt > lastSleepEnd.occurredAt)
    val sleepLabel = if (sleeping) {
        "睡眠状态 已睡 ${elapsedLabel(now - lastSleepStart!!.occurredAt)}"
    } else {
        "睡眠状态 未在睡觉"
    }
    val lastFeed = records.firstOrNull {
        it.eventType == "feeding_formula" ||
            it.eventType == "feeding_breast" ||
            it.eventType == "feeding_warm_breast"
    }
    val feedLabel = if (lastFeed == null) {
        "距离上次吃奶/母乳 暂无记录"
    } else {
        "距离上次吃奶/母乳 已过去 ${elapsedLabel(now - lastFeed.occurredAt)}"
    }
    val lastButt = records.firstOrNull {
        it.eventType == "butt_clean" || it.eventType == "care" || it.eventType == "pee_clean"
    }
    val buttLabel = if (lastButt == null) {
        "上次洗屁屁 暂无记录"
    } else {
        "上次洗屁屁 已过去 ${elapsedLabel(now - lastButt.occurredAt)}"
    }
    return RealtimeSummary(age, sleepLabel, feedLabel, buttLabel)
}

private fun birthdayLabel(birthday: String, now: Long): String {
    return runCatching {
        val birth = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).parse(birthday) ?: return "宝宝"
        var millis = max(0L, now - birth.time)
        val daysTotal = TimeUnit.MILLISECONDS.toDays(millis)
        millis -= TimeUnit.DAYS.toMillis(daysTotal)
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val months = (daysTotal / 30).toInt()
        val days = (daysTotal % 30).toInt()
        "宝宝出生已 ${months}月${days}天${hours}小时"
    }.getOrDefault("宝宝")
}

private fun elapsedLabel(ms: Long): String {
    val totalMin = max(0L, ms) / 60000L
    val days = totalMin / (60 * 24)
    val hours = (totalMin % (60 * 24)) / 60
    val minutes = totalMin % 60
    return when {
        days > 0 -> "${days}天${hours}小时${minutes}分钟"
        hours > 0 -> "${hours}小时${minutes}分钟"
        else -> "${minutes}分钟"
    }
}

private fun summarize(records: List<BabyRecord>): SummaryCounts {
    var formulaAmount = 0.0
    var formulaCount = 0
    var warmBreastAmount = 0.0
    var warmBreastCount = 0
    var breastCount = 0
    var poop = 0
    var care = 0
    var bath = 0
    var sleepStart = 0
    var sleepEnd = 0
    records.forEach { record ->
        when (record.eventType) {
            "feeding_formula" -> {
                formulaCount++
                formulaAmount += record.payload.optDouble("amountMl")
            }
            "feeding_warm_breast" -> {
                warmBreastCount++
                warmBreastAmount += record.payload.optDouble("amountMl")
            }
            "feeding_breast" -> breastCount++
            "poop" -> poop++
            "care", "butt_clean", "pee_clean" -> care++
            "bath" -> bath++
            "sleep_start" -> sleepStart++
            "sleep_end" -> sleepEnd++
        }
    }
    return SummaryCounts(
        formulaAmountTotal = formulaAmount,
        formulaFeedCount = formulaCount,
        warmBreastAmountTotal = warmBreastAmount,
        warmBreastFeedCount = warmBreastCount,
        breastFeedCount = breastCount,
        poopCount = poop,
        careCount = care,
        bathCount = bath,
        sleepStartCount = sleepStart,
        sleepEndCount = sleepEnd,
        totalCount = records.size,
    )
}
