package com.xiaolai.todo.ui

data class LaunchTarget(
    val tab: Int = 0,
    val focusComposer: Boolean = false,
) {
    companion object {
        val Todos = LaunchTarget(tab = 0, focusComposer = false)
        val NewTodo = LaunchTarget(tab = 0, focusComposer = true)
        val Timer = LaunchTarget(tab = 1, focusComposer = false)
    }
}
