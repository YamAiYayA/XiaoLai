package com.xiaolai.todo.data

import kotlinx.coroutines.flow.Flow

class TodoRepository(private val dao: TodoDao) {
    fun observeAll(): Flow<List<Todo>> = dao.observeAll()

    fun observeOpen(limit: Int = 5): Flow<List<Todo>> = dao.observeOpen(limit)

    suspend fun add(title: String) {
        val cleaned = title.trim()
        if (cleaned.isEmpty()) return
        dao.insert(Todo(title = cleaned))
    }

    suspend fun toggle(todo: Todo) {
        dao.setDone(todo.id, !todo.isDone)
    }

    suspend fun setDone(id: Long, done: Boolean) {
        dao.setDone(id, done)
    }

    suspend fun delete(todo: Todo) {
        dao.delete(todo)
    }
}
