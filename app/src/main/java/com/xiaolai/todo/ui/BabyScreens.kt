package com.xiaolai.todo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.xiaolai.todo.model.BabyRecord
import com.xiaolai.todo.model.BabyTodo
import com.xiaolai.todo.model.EventTypes
import com.xiaolai.todo.model.SummaryCounts
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HomePane(state: BabyUiState, onRefresh: () -> Unit) {
    val baby = state.context?.baby
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = baby?.nickname?.let { "${it}的今天" } ?: "今天",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = state.context?.member?.displayName?.let { "当前身份：$it" }.orEmpty(),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            )
        }
        item { SummaryCard("今日汇总", state.dashboard.todaySummary) }
        item { SummaryCard("近7日汇总", state.dashboard.weekSummary) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("最近记录", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onRefresh) { Text("刷新") }
            }
        }
        items(state.dashboard.recentRecords, key = { it.id }) { record ->
            RecordRow(record)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorPane(state: BabyUiState, onCreate: (String, JSONObject) -> Unit) {
    var selected by remember { mutableStateOf(EventTypes.quickActions.first()) }
    var amount by rememberSaveable { mutableStateOf("40") }
    var side by rememberSaveable { mutableStateOf("left") }
    var note by rememberSaveable { mutableStateOf("") }
    var duration by rememberSaveable { mutableStateOf("15") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("快速记录", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("和微信小程序一样，先选类型再保存。")
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EventTypes.quickActions.forEach { option ->
                    FilterChip(
                        selected = selected.value == option.value,
                        onClick = { selected = option },
                        label = { Text(option.label) },
                    )
                }
            }
        }
        if (selected.needsAmount) {
            item {
                Text("奶量 ml")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 40, 50, 60, 70, 80, 100).forEach { value ->
                        AssistChip(
                            onClick = { amount = value.toString() },
                            label = { Text("$value") },
                        )
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("自定义奶量") },
                )
            }
        }
        if (selected.needsSide) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = side == "left", onClick = { side = "left" }, label = { Text("左侧") })
                    FilterChip(selected = side == "right", onClick = { side = "right" }, label = { Text("右侧") })
                }
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("时长（分钟）") },
                )
            }
        }
        if (selected.needsNote || selected.value == "poop" || selected.value == "custom") {
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("备注") },
                )
            }
        }
        item {
            Button(
                onClick = {
                    val payload = JSONObject()
                    when {
                        selected.needsAmount -> payload.put("amountMl", amount.toIntOrNull() ?: 0)
                        selected.needsSide -> {
                            payload.put("side", side)
                            payload.put("durationMin", duration.toIntOrNull() ?: 0)
                        }
                        selected.needsNote -> payload.put("note", note.trim())
                        note.isNotBlank() -> payload.put("note", note.trim())
                    }
                    onCreate(selected.value, payload)
                },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存${selected.label}")
            }
            if (state.message.isNotBlank()) {
                Text(state.message, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun TimelinePane(
    state: BabyUiState,
    onDateChange: (String) -> Unit,
) {
    val cal = remember { Calendar.getInstance() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("记录", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    cal.time = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).parse(state.timelineDateKey)!!
                    cal.add(Calendar.DAY_OF_MONTH, -1)
                    onDateChange(SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(cal.time))
                }) { Text("前一天") }
                Text(
                    state.timelineDateKey,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .weight(1f),
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedButton(onClick = {
                    cal.time = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).parse(state.timelineDateKey)!!
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                    onDateChange(SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(cal.time))
                }) { Text("后一天") }
            }
        }
        if (state.timeline.isEmpty()) {
            item { Text("这一天还没有记录") }
        }
        items(state.timeline, key = { it.id }) { RecordRow(it) }
    }
}

@Composable
fun TodosPane(
    state: BabyUiState,
    onAdd: (String, String) -> Unit,
    onToggle: (String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("baby") }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("待办", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = category == "baby", onClick = { category = "baby" }, label = { Text("宝宝") })
                FilterChip(selected = category == "adult", onClick = { category = "adult" }, label = { Text("大人") })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("新待办") },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onAdd(title.trim(), category)
                            title = ""
                        }
                    },
                ) { Text("添加") }
            }
        }
        items(state.todos, key = { it.id }) { todo ->
            TodoRow(todo, onToggle = { onToggle(todo.id) })
        }
    }
}

@Composable
fun ProfilePane(state: BabyUiState, onLogout: () -> Unit, onRefresh: () -> Unit) {
    val ctx = state.context
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("我的", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        InfoCard("家庭", ctx?.family?.name ?: "-")
        InfoCard("邀请码", ctx?.family?.inviteCode ?: "-")
        InfoCard("宝宝", ctx?.baby?.nickname ?: "-")
        InfoCard("生日", ctx?.baby?.birthday ?: "-")
        InfoCard("当前身份", ctx?.member?.displayName ?: "-")
        Text("家庭成员", fontWeight = FontWeight.SemiBold)
        ctx?.members?.forEach {
            Text("· ${it.displayName}（${it.role}）")
        }
        OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("刷新资料") }
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("退出登录") }
        if (state.message.isNotBlank()) {
            Text(state.message)
        }
    }
}

@Composable
private fun SummaryCard(title: String, summary: SummaryCounts) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("奶粉 ${summary.formulaFeedCount} 次 / ${summary.formulaAmountTotal.toInt()} ml")
        Text("母乳 ${summary.breastFeedCount} 次 · 便便 ${summary.poopCount} 次")
        Text("睡觉 ${summary.sleepStartCount} · 醒来 ${summary.sleepEndCount} · 洗澡 ${summary.bathCount}")
        Text("合计 ${summary.totalCount} 条")
    }
}

@Composable
private fun RecordRow(record: BabyRecord) {
    val detail = buildString {
        val payload = record.payload
        when (record.eventType) {
            "feeding_formula" -> append("${payload.optInt("amountMl")}ml")
            "feeding_breast" -> append("${payload.optString("side")} ${payload.optInt("durationMin")}分钟")
            "note" -> append(payload.optString("note"))
            else -> if (payload.has("note")) append(payload.optString("note"))
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
    ) {
        Row {
            Text(EventTypes.labelOf(record.eventType), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(formatOccurred(record.occurredAt))
        }
        if (detail.isNotBlank()) {
            Text(detail, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
        if (record.createdByName.isNotBlank()) {
            Text("by ${record.createdByName}", style = MaterialTheme.typography.bodySmall)
        }
    }
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
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
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
