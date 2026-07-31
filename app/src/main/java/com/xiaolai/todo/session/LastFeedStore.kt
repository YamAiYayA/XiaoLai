package com.xiaolai.todo.session

import android.content.Context
import com.xiaolai.todo.model.BabyRecord
import com.xiaolai.todo.model.EventTypes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class LastFeedSnapshot(
    val title: String,
    val occurredAt: Long,
    val timeLabel: String,
) {
    fun elapsedLabel(now: Long = System.currentTimeMillis()): String {
        val totalMin = max(0L, now - occurredAt) / 60_000L
        val days = totalMin / (60 * 24)
        val hours = (totalMin % (60 * 24)) / 60
        val minutes = totalMin % 60
        return when {
            days > 0 -> "${days}天${hours}小时${minutes}分钟"
            hours > 0 -> "${hours}小时${minutes}分钟"
            else -> "${minutes}分钟"
        }
    }

    companion object {
        fun fromRecord(record: BabyRecord): LastFeedSnapshot {
            val timeFmt = SimpleDateFormat("HH:mm", Locale.CHINA)
            val dayFmt = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
            val timeLabel = if (record.dateKey == today) {
                timeFmt.format(Date(record.occurredAt))
            } else {
                dayFmt.format(Date(record.occurredAt))
            }
            return LastFeedSnapshot(
                title = EventTypes.summarize(record),
                occurredAt = record.occurredAt,
                timeLabel = timeLabel,
            )
        }
    }
}

class LastFeedStore(context: Context) {
    private val prefs = context.getSharedPreferences("xiaolai_last_feed", Context.MODE_PRIVATE)

    fun save(snapshot: LastFeedSnapshot) {
        prefs.edit()
            .putBoolean(KEY_HAS_DATA, true)
            .putString(KEY_TITLE, snapshot.title)
            .putLong(KEY_OCCURRED_AT, snapshot.occurredAt)
            .putString(KEY_TIME_LABEL, snapshot.timeLabel)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun read(): LastFeedSnapshot? {
        if (!prefs.getBoolean(KEY_HAS_DATA, false)) return null
        val occurredAt = prefs.getLong(KEY_OCCURRED_AT, 0L)
        if (occurredAt <= 0L) return null
        return LastFeedSnapshot(
            title = prefs.getString(KEY_TITLE, "").orEmpty().ifBlank { "上次吃奶" },
            occurredAt = occurredAt,
            timeLabel = prefs.getString(KEY_TIME_LABEL, "").orEmpty().ifBlank {
                SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(occurredAt))
            },
        )
    }

    companion object {
        private const val KEY_HAS_DATA = "has_data"
        private const val KEY_TITLE = "title"
        private const val KEY_OCCURRED_AT = "occurred_at"
        private const val KEY_TIME_LABEL = "time_label"

        fun isFeedType(eventType: String): Boolean =
            eventType == "feeding_formula" ||
                eventType == "feeding_breast" ||
                eventType == "feeding_warm_breast"

        fun pickLatest(records: List<BabyRecord>): LastFeedSnapshot? =
            records
                .filter { isFeedType(it.eventType) }
                .maxByOrNull { it.occurredAt }
                ?.let { LastFeedSnapshot.fromRecord(it) }
    }
}
