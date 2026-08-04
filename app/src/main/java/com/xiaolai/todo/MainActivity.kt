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
import com.xiaolai.todo.ui.AppRoot
import com.xiaolai.todo.ui.BabyViewModel
import com.xiaolai.todo.ui.theme.XiaoLaiTodoTheme

class MainActivity : ComponentActivity() {
    private var shortcutAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        shortcutAction = normalizeAction(intent)

        setContent {
            XiaoLaiTodoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: BabyViewModel = viewModel(
                        factory = BabyViewModel.Factory(application),
                    )
                    AppRoot(
                        viewModel = viewModel,
                        shortcutAction = shortcutAction,
                        onShortcutConsumed = { shortcutAction = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shortcutAction = normalizeAction(intent)
    }

    private fun normalizeAction(intent: Intent?): String? {
        val action = intent?.action ?: return null
        return when (action) {
            ShortcutActions.QUICK_FEED,
            ShortcutActions.QUICK_DIAPER,
            -> action
            else -> null
        }
    }
}
