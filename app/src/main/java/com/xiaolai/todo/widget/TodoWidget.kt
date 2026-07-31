package com.xiaolai.todo.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.ToggleableStateKey
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.xiaolai.todo.MainActivity
import com.xiaolai.todo.TodoApplication
import com.xiaolai.todo.data.Todo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val TodosJsonKey = stringPreferencesKey("todos_json")
internal val TodoIdKey = ActionParameters.Key<Long>("todo_id")

class TodoWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        syncState(context, id)
        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                val todos = decodeTodos(prefs[TodosJsonKey].orEmpty())
                WidgetContent(todos)
            }
        }
    }

    companion object {
        suspend fun updateAll(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            val widget = TodoWidget()
            manager.getGlanceIds(TodoWidget::class.java).forEach { glanceId ->
                widget.syncState(context, glanceId)
                widget.update(context, glanceId)
            }
        }
    }

    private suspend fun syncState(context: Context, id: GlanceId) {
        val todos = withContext(Dispatchers.IO) {
            val app = context.applicationContext as TodoApplication
            app.repository.observeOpen(5).first()
        }
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
            prefs.toMutablePreferences().apply {
                this[TodosJsonKey] = encodeTodos(todos)
            }
        }
    }
}

class TodoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodoWidget()
}

class ToggleTodoAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val todoId = parameters[TodoIdKey] ?: return
        val checked = parameters[ToggleableStateKey] ?: return
        val app = context.applicationContext as TodoApplication
        withContext(Dispatchers.IO) {
            app.repository.setDone(todoId, checked)
        }
        TodoWidget.updateAll(context)
    }
}

@Composable
private fun WidgetContent(todos: List<Todo>) {
    val cream = ColorProvider(Color(0xFFFFF7F0))
    val ink = ColorProvider(Color(0xFF3D2C29))
    val muted = ColorProvider(Color(0xFF7A635C))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(cream)
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text(
            text = "妍妍养成记",
            style = TextStyle(
                color = ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))

        if (todos.isEmpty()) {
            Text(
                text = "暂无待办，点这里打开 App 添加",
                style = TextStyle(color = muted, fontSize = 13.sp),
            )
        } else {
            todos.forEach { todo ->
                CheckBox(
                    checked = todo.isDone,
                    onCheckedChange = actionRunCallback<ToggleTodoAction>(
                        actionParametersOf(TodoIdKey to todo.id),
                    ),
                    text = todo.title,
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                    style = TextStyle(color = ink, fontSize = 13.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

private fun encodeTodos(todos: List<Todo>): String {
    val array = JSONArray()
    todos.forEach { todo ->
        array.put(
            JSONObject()
                .put("id", todo.id)
                .put("title", todo.title)
                .put("isDone", todo.isDone),
        )
    }
    return array.toString()
}

private fun decodeTodos(json: String): List<Todo> {
    if (json.isBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(json)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(
                    Todo(
                        id = obj.getLong("id"),
                        title = obj.getString("title"),
                        isDone = obj.optBoolean("isDone", false),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())
}
