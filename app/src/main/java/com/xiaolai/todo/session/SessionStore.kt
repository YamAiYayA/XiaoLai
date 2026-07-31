package com.xiaolai.todo.session

import android.content.Context

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("xiaolai_baby_session", Context.MODE_PRIVATE)

    var accessToken: String
        get() = prefs.getString(KEY_TOKEN, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var displayName: String
        get() = prefs.getString(KEY_NAME, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN = "access_token"
        private const val KEY_NAME = "display_name"
    }
}
