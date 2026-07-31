package com.xiaolai.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaolai.todo.ui.AppRoot
import com.xiaolai.todo.ui.BabyViewModel
import com.xiaolai.todo.ui.theme.XiaoLaiTodoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            XiaoLaiTodoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: BabyViewModel = viewModel(
                        factory = BabyViewModel.Factory(application),
                    )
                    AppRoot(viewModel = viewModel)
                }
            }
        }
    }
}
