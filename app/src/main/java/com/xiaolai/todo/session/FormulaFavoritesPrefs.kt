package com.xiaolai.todo.session

import android.content.Context
import com.xiaolai.todo.model.EventTypes

class FormulaFavoritesPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("xiaolai_formula_favorites", Context.MODE_PRIVATE)

    fun get(): List<Int> {
        val raw = prefs.getString(KEY_FAVORITES, null) ?: return EventTypes.defaultFormulaFavorites
        val parsed = raw.split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in EventTypes.formulaQuickValues }
            .distinct()
            .sorted()
        return parsed.ifEmpty { EventTypes.defaultFormulaFavorites }
    }

    fun set(values: List<Int>) {
        val next = values
            .filter { it in EventTypes.formulaQuickValues }
            .distinct()
            .sorted()
            .take(MAX_FAVORITES)
        prefs.edit().putString(KEY_FAVORITES, next.joinToString(",")).apply()
    }

    fun add(value: Int): List<Int> {
        if (value !in EventTypes.formulaQuickValues) return get()
        val next = (get() + value).distinct().sorted().take(MAX_FAVORITES)
        set(next)
        return next
    }

    fun remove(value: Int): List<Int> {
        val next = get().filter { it != value }
        set(next)
        return next
    }

    companion object {
        private const val KEY_FAVORITES = "favorites_ml"
        const val MAX_FAVORITES = 12
    }
}
