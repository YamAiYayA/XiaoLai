package com.xiaolai.todo

import android.app.Application
import com.xiaolai.todo.alarm.AlertNotifier
import com.xiaolai.todo.data.TodoDatabase
import com.xiaolai.todo.data.TodoRepository

class TodoApplication : Application() {
    lateinit var repository: TodoRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = TodoDatabase.get(this)
        repository = TodoRepository(db.todoDao())
        AlertNotifier.ensureChannel(this)
    }
}
