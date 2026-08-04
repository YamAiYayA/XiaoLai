package com.xiaolai.todo.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.xiaolai.todo.MainActivity
import com.xiaolai.todo.network.ApiClient
import com.xiaolai.todo.session.LastFeedSnapshot
import com.xiaolai.todo.session.LastFeedStore
import com.xiaolai.todo.session.SessionStore
import com.xiaolai.todo.ui.todayKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val TitleKey = stringPreferencesKey("last_feed_title")
private val TimeLabelKey = stringPreferencesKey("last_feed_time")
private val OccurredAtKey = longPreferencesKey("last_feed_occurred_at")
private val StatusKey = stringPreferencesKey("last_feed_status")

class TodoWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        syncState(context, id)
        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                val status = prefs[StatusKey].orEmpty()
                val occurredAt = prefs[OccurredAtKey] ?: 0L
                val snapshot = if (occurredAt > 0L) {
                    LastFeedSnapshot(
                        title = prefs[TitleKey].orEmpty().ifBlank { "上次吃奶" },
                        occurredAt = occurredAt,
                        timeLabel = prefs[TimeLabelKey].orEmpty(),
                    )
                } else {
                    null
                }
                WidgetContent(status = status, snapshot = snapshot)
            }
        }
    }

    companion object {
        suspend fun updateAll(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            val widget = TodoWidget()
            manager.getGlanceIds(TodoWidget::class.java).forEach { glanceId ->
                widget.syncState(context, glanceId)
                widget.update(context, glanceId)
            }
        }
    }

    private suspend fun syncState(context: Context, id: GlanceId) {
        val snapshot = withContext(Dispatchers.IO) { refreshLastFeed(context) }
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
            prefs.toMutablePreferences().apply {
                if (snapshot == null) {
                    val session = SessionStore(context)
                    this[StatusKey] = if (session.accessToken.isBlank()) {
                        "not_logged_in"
                    } else {
                        "empty"
                    }
                    this[TitleKey] = ""
                    this[TimeLabelKey] = ""
                    this[OccurredAtKey] = 0L
                } else {
                    this[StatusKey] = "ok"
                    this[TitleKey] = snapshot.title
                    this[TimeLabelKey] = snapshot.timeLabel
                    this[OccurredAtKey] = snapshot.occurredAt
                }
            }
        }
    }
}

class TodoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodoWidget()
}

@Composable
private fun WidgetContent(status: String, snapshot: LastFeedSnapshot?) {
    val cream = ColorProvider(Color(0xFFFFF7F0))
    val ink = ColorProvider(Color(0xFF3D2C29))
    val muted = ColorProvider(Color(0xFF7A635C))
    val accent = ColorProvider(Color(0xFFC45C26))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(cream)
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text(
            text = "妍妍养成记",
            style = TextStyle(
                color = ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        Text(
            text = "上次吃奶",
            style = TextStyle(color = muted, fontSize = 12.sp),
        )
        Spacer(modifier = GlanceModifier.height(4.dp))

        when {
            snapshot != null -> {
                Text(
                    text = snapshot.title,
                    style = TextStyle(
                        color = ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 2,
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = "${snapshot.timeLabel} 吃的",
                    style = TextStyle(color = muted, fontSize = 13.sp),
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = "已过 ${snapshot.elapsedLabel()}",
                    style = TextStyle(
                        color = accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            status == "not_logged_in" -> {
                Text(
                    text = "登录 App 后可显示上次吃奶",
                    style = TextStyle(color = muted, fontSize = 13.sp),
                )
            }
            else -> {
                Text(
                    text = "暂无吃奶记录",
                    style = TextStyle(color = muted, fontSize = 13.sp),
                )
            }
        }
    }
}

private suspend fun refreshLastFeed(context: Context): LastFeedSnapshot? {
    val cache = LastFeedStore(context)
    val session = SessionStore(context)
    val token = session.accessToken
    val babyId = session.babyId
    if (token.isBlank() || babyId.isBlank()) {
        return cache.read()
    }

    return runCatching {
        val api = ApiClient()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        val cal = Calendar.getInstance()
        val today = todayKey()
        cal.add(Calendar.DAY_OF_MONTH, -1)
        val yesterday = fmt.format(cal.time)
        val records = buildList {
            addAll(api.listRecordsByDate(token, babyId, today))
            addAll(api.listRecordsByDate(token, babyId, yesterday))
            addAll(api.dashboard(token, babyId, today).recentRecords)
        }.distinctBy { it.id }
        val latest = LastFeedStore.pickLatest(records)
        if (latest != null) {
            cache.save(latest)
            latest
        } else {
            cache.read()
        }
    }.getOrElse { cache.read() }
}
