package com.xiaolai.todo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xiaolai.todo.data.Todo
import com.xiaolai.todo.data.TodoRepository
import com.xiaolai.todo.widget.TodoWidget
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(
    private val repository: TodoRepository,
    private val appContext: android.content.Context,
) : ViewModel() {
    val todos: StateFlow<List<Todo>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(title: String) {
        viewModelScope.launch {
            repository.add(title)
            TodoWidget.updateAll(appContext)
        }
    }

    fun toggle(todo: Todo) {
        viewModelScope.launch {
            repository.toggle(todo)
            TodoWidget.updateAll(appContext)
        }
    }

    fun delete(todo: Todo) {
        viewModelScope.launch {
            repository.delete(todo)
            TodoWidget.updateAll(appContext)
        }
    }

    class Factory(
        private val repository: TodoRepository,
        private val appContext: android.content.Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TodoViewModel(repository, appContext) as T
        }
    }
}
