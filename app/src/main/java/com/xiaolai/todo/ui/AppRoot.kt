package com.xiaolai.todo.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun AppRoot(
    todoViewModel: TodoViewModel,
    launchTarget: LaunchTarget,
    onLaunchTargetConsumed: () -> Unit = {},
) {
    var tab by rememberSaveable { mutableIntStateOf(launchTarget.tab) }
    var focusComposer by rememberSaveable { mutableStateOf(launchTarget.focusComposer) }

    LaunchedEffect(launchTarget) {
        tab = launchTarget.tab
        focusComposer = launchTarget.focusComposer
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Checklist, contentDescription = "待办") },
                    label = { Text("待办") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Alarm, contentDescription = "提示音") },
                    label = { Text("提示音") },
                )
            }
        },
    ) { padding ->
        when (tab) {
            0 -> TodoScreen(
                viewModel = todoViewModel,
                modifier = Modifier.padding(padding),
                requestFocusComposer = focusComposer,
                onFocusComposerHandled = {
                    focusComposer = false
                    onLaunchTargetConsumed()
                },
            )
            else -> TimerTestScreen(modifier = Modifier.padding(padding))
        }
    }
}
