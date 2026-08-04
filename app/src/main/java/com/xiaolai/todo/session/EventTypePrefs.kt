package com.xiaolai.todo.session

import android.content.Context

class EventTypePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("xiaolai_event_types", Context.MODE_PRIVATE)

    fun hiddenTypes(): Set<String> =
        prefs.getStringSet(KEY_HIDDEN, emptySet())?.toSet().orEmpty()

    fun hide(type: String) {
        val next = hiddenTypes().toMutableSet().apply { add(type) }
        prefs.edit().putStringSet(KEY_HIDDEN, next).apply()
    }

    fun restoreAll() {
        prefs.edit().remove(KEY_HIDDEN).apply()
    }

    companion object {
        private const val KEY_HIDDEN = "hidden_event_types"
    }
}
