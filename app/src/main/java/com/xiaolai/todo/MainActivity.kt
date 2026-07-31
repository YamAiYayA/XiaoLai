package com.xiaolai.todo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaolai.todo.alarm.AlertNotifier
import com.xiaolai.todo.ui.AppRoot
import com.xiaolai.todo.ui.LaunchTarget
import com.xiaolai.todo.ui.TodoViewModel
import com.xiaolai.todo.ui.theme.XiaoLaiTodoTheme

class MainActivity : ComponentActivity() {
    private var launchTarget by mutableStateOf(LaunchTarget.Todos)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        launchTarget = resolveLaunchTarget(intent)
        val app = application as TodoApplication

        setContent {
            XiaoLaiTodoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: TodoViewModel = viewModel(
                        factory = TodoViewModel.Factory(app.repository, applicationContext),
                    )
                    AppRoot(
                        todoViewModel = viewModel,
                        launchTarget = launchTarget,
                        onLaunchTargetConsumed = {
                            if (launchTarget.focusComposer) {
                                launchTarget = launchTarget.copy(focusComposer = false)
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchTarget = resolveLaunchTarget(intent)
    }

    private fun resolveLaunchTarget(intent: Intent?): LaunchTarget {
        return when {
            intent?.action == ShortcutActions.NEW_TODO -> LaunchTarget.NewTodo
            intent?.action == ShortcutActions.OPEN_TIMER ||
                intent?.getBooleanExtra(AlertNotifier.EXTRA_OPEN_TIMER, false) == true -> {
                LaunchTarget.Timer
            }
            intent?.action == ShortcutActions.OPEN_TODOS -> LaunchTarget.Todos
            else -> LaunchTarget.Todos
        }
    }
}
