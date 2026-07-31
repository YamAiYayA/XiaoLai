package com.xiaolai.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaolai.todo.alarm.AlertNotifier
import com.xiaolai.todo.ui.AppRoot
import com.xiaolai.todo.ui.TodoViewModel
import com.xiaolai.todo.ui.theme.XiaoLaiTodoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as TodoApplication
        val openTimer = intent?.getBooleanExtra(AlertNotifier.EXTRA_OPEN_TIMER, false) == true

        setContent {
            XiaoLaiTodoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: TodoViewModel = viewModel(
                        factory = TodoViewModel.Factory(app.repository, applicationContext),
                    )
                    AppRoot(
                        todoViewModel = viewModel,
                        initialTab = if (openTimer) 1 else 0,
                    )
                }
            }
        }
    }
}
