package com.xiaolai.todo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.json.JSONObject

@Composable
fun AppRoot(viewModel: BabyViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Match miniprogram tab order: 首页 / 记录 / 新增 / 待办 / 我的
    var tab by rememberSaveable { mutableIntStateOf(0) }

    when {
        state.booting -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        !state.loggedIn -> {
            LoginScreen(
                loading = state.loading,
                message = state.message,
                onLoginToken = viewModel::loginWithToken,
                onLoginInvite = viewModel::loginWithInvite,
            )
        }

        else -> {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text("首页") },
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                            label = { Text("记录") },
                        )
                        NavigationBarItem(
                            selected = tab == 2,
                            onClick = { tab = 2 },
                            icon = { Icon(Icons.Default.AddCircle, contentDescription = null) },
                            label = { Text("新增") },
                        )
                        NavigationBarItem(
                            selected = tab == 3,
                            onClick = { tab = 3 },
                            icon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                            label = { Text("待办") },
                        )
                        NavigationBarItem(
                            selected = tab == 4,
                            onClick = { tab = 4 },
                            icon = { Icon(Icons.Default.Person, contentDescription = null) },
                            label = { Text("我的") },
                        )
                    }
                },
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    when (tab) {
                        0 -> HomePane(
                            state = state,
                            onRefresh = viewModel::refreshAll,
                            onQuickAdd = { type ->
                                val option = com.xiaolai.todo.model.EventTypes.of(type)
                                if (option.instantConfirm || option.needsAmount || option.needsSide) {
                                    tab = 2
                                }
                                if (option.instantConfirm) {
                                    viewModel.createRecord(type, JSONObject())
                                } else {
                                    tab = 2
                                }
                            },
                            onOpenEditor = { tab = 2 },
                            onOpenTimeline = { tab = 1 },
                        )
                        1 -> TimelinePane(
                            state = state,
                            onDateChange = viewModel::setTimelineDate,
                        )
                        2 -> EditorPane(
                            state = state,
                            onCreate = { type, payload, occurredAt, dateKey ->
                                viewModel.createRecord(
                                    eventType = type,
                                    payload = payload,
                                    occurredAt = occurredAt,
                                    dateKey = dateKey,
                                )
                            },
                        )
                        3 -> TodosPane(
                            state = state,
                            onAdd = viewModel::createTodo,
                            onToggle = viewModel::toggleTodo,
                        )
                        else -> ProfilePane(
                            state = state,
                            onLogout = viewModel::logout,
                            onRefresh = viewModel::refreshAll,
                        )
                    }
                }
            }
        }
    }
}
