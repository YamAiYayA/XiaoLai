package com.xiaolai.todo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xiaolai.todo.model.AppContextData
import com.xiaolai.todo.model.BabyRecord
import com.xiaolai.todo.model.BabyTodo
import com.xiaolai.todo.model.DashboardData
import com.xiaolai.todo.model.SummaryCounts
import com.xiaolai.todo.network.ApiClient
import com.xiaolai.todo.session.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BabyUiState(
    val booting: Boolean = true,
    val loggedIn: Boolean = false,
    val loading: Boolean = false,
    val message: String = "",
    val context: AppContextData? = null,
    val dashboard: DashboardData = DashboardData(SummaryCounts(), SummaryCounts(), emptyList()),
    val timelineDateKey: String = todayKey(),
    val timeline: List<BabyRecord> = emptyList(),
    val todos: List<BabyTodo> = emptyList(),
)

class BabyViewModel(
    app: Application,
) : AndroidViewModel(app) {
    private val api = ApiClient()
    private val session = SessionStore(app)

    private val _state = MutableStateFlow(BabyUiState())
    val state: StateFlow<BabyUiState> = _state.asStateFlow()

    init {
        bootstrap()
    }

    fun bootstrap() {
        val token = session.accessToken
        if (token.isBlank()) {
            _state.update { it.copy(booting = false, loggedIn = false) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(booting = true, message = "") }
            runCatching {
                withContext(Dispatchers.IO) { api.bootstrap(token) }
            }.onSuccess { context ->
                _state.update {
                    it.copy(booting = false, loggedIn = true, context = context, message = "")
                }
                refreshAll()
            }.onFailure { error ->
                session.clear()
                _state.update {
                    it.copy(
                        booting = false,
                        loggedIn = false,
                        message = error.message ?: "登录失效",
                    )
                }
            }
        }
    }

    fun loginWithToken(token: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = "") }
            runCatching {
                withContext(Dispatchers.IO) { api.loginWithToken(token.trim()) }
            }.onSuccess { member ->
                session.accessToken = member.accessToken
                session.displayName = member.displayName
                _state.update { it.copy(loading = false, loggedIn = true) }
                bootstrap()
            }.onFailure { error ->
                _state.update {
                    it.copy(loading = false, message = error.message ?: "登录失败")
                }
            }
        }
    }

    fun loginWithInvite(inviteCode: String, displayName: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = "") }
            runCatching {
                withContext(Dispatchers.IO) {
                    api.loginWithInvite(inviteCode.trim(), displayName.trim())
                }
            }.onSuccess { member ->
                session.accessToken = member.accessToken
                session.displayName = member.displayName
                _state.update { it.copy(loading = false, loggedIn = true) }
                bootstrap()
            }.onFailure { error ->
                _state.update {
                    it.copy(loading = false, message = error.message ?: "登录失败")
                }
            }
        }
    }

    fun logout() {
        session.clear()
        _state.value = BabyUiState(booting = false, loggedIn = false)
    }

    fun refreshAll() {
        val token = session.accessToken
        val babyId = _state.value.context?.baby?.id.orEmpty()
        if (token.isBlank() || babyId.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = "") }
            runCatching {
                withContext(Dispatchers.IO) {
                    val dash = api.dashboard(token, babyId, todayKey())
                    val timeline = api.listRecordsByDate(token, babyId, _state.value.timelineDateKey)
                    val todos = api.listTodos(token)
                    Triple(dash, timeline, todos)
                }
            }.onSuccess { (dash, timeline, todos) ->
                _state.update {
                    it.copy(
                        loading = false,
                        dashboard = dash,
                        timeline = timeline,
                        todos = todos,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(loading = false, message = error.message ?: "刷新失败")
                }
            }
        }
    }

    fun setTimelineDate(dateKey: String) {
        _state.update { it.copy(timelineDateKey = dateKey) }
        val token = session.accessToken
        val babyId = _state.value.context?.baby?.id.orEmpty()
        if (token.isBlank() || babyId.isBlank()) return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.listRecordsByDate(token, babyId, dateKey) }
            }.onSuccess { list ->
                _state.update { it.copy(timeline = list) }
            }.onFailure { error ->
                _state.update { it.copy(message = error.message ?: "加载失败") }
            }
        }
    }

    fun createRecord(
        eventType: String,
        payload: JSONObject,
        occurredAt: Long = System.currentTimeMillis(),
        dateKey: String = todayKey(),
        onDone: () -> Unit = {},
    ) {
        val token = session.accessToken
        val babyId = _state.value.context?.baby?.id.orEmpty()
        if (token.isBlank() || babyId.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = "") }
            runCatching {
                withContext(Dispatchers.IO) {
                    api.createRecord(
                        token = token,
                        babyId = babyId,
                        eventType = eventType,
                        dateKey = dateKey,
                        occurredAt = occurredAt,
                        payload = payload,
                    )
                }
            }.onSuccess {
                _state.update { state -> state.copy(loading = false, message = "已记录") }
                refreshAll()
                onDone()
            }.onFailure { error ->
                _state.update {
                    it.copy(loading = false, message = error.message ?: "保存失败")
                }
            }
        }
    }

    fun createTodo(title: String, category: String) {
        val token = session.accessToken
        val babyId = _state.value.context?.baby?.id
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.createTodo(token, title, category, babyId) }
            }.onSuccess {
                refreshAll()
            }.onFailure { error ->
                _state.update { it.copy(message = error.message ?: "添加失败") }
            }
        }
    }

    fun toggleTodo(id: String) {
        val token = session.accessToken
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.toggleTodo(token, id) }
            }.onSuccess {
                refreshAll()
            }.onFailure { error ->
                _state.update { it.copy(message = error.message ?: "更新失败") }
            }
        }
    }

    fun deleteRecord(id: String) {
        val token = session.accessToken
        if (token.isBlank()) return
        viewModelScope.launch {
            // Optimistic remove for snappy swipe UX.
            _state.update { state ->
                state.copy(
                    timeline = state.timeline.filterNot { it.id == id },
                    dashboard = state.dashboard.copy(
                        recentRecords = state.dashboard.recentRecords.filterNot { it.id == id },
                    ),
                )
            }
            runCatching {
                withContext(Dispatchers.IO) { api.deleteRecord(token, id) }
            }.onSuccess {
                refreshAll()
            }.onFailure { error ->
                refreshAll()
                _state.update { it.copy(message = error.message ?: "删除失败") }
            }
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BabyViewModel(app) as T
        }
    }
}

fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())

fun formatOccurred(ms: Long): String =
    SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(ms))
