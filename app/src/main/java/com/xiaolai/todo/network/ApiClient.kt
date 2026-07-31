package com.xiaolai.todo.network

import com.xiaolai.todo.model.AppContextData
import com.xiaolai.todo.model.BabyRecord
import com.xiaolai.todo.model.BabyTodo
import com.xiaolai.todo.model.DashboardData
import com.xiaolai.todo.model.FamilyMember
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ApiClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun loginWithToken(token: String): FamilyMember {
        val body = JSONObject().put("token", token)
        return FamilyMember.fromJson(post("auth/login", body))
    }

    fun loginWithInvite(inviteCode: String, displayName: String): FamilyMember {
        val body = JSONObject()
            .put("inviteCode", inviteCode)
            .put("displayName", displayName)
        return FamilyMember.fromJson(post("auth/login", body))
    }

    fun bootstrap(token: String): AppContextData {
        return AppContextData.fromJson(get("bootstrap/context", token))
    }

    fun dashboard(token: String, babyId: String, dateKey: String): DashboardData {
        return DashboardData.fromJson(
            get(
                "stats/dashboard&babyId=${enc(babyId)}&dateKey=${enc(dateKey)}&recentLimit=8",
                token,
            ),
        )
    }

    fun listRecordsByDate(token: String, babyId: String, dateKey: String): List<BabyRecord> {
        val arr = getArray(
            "records/listByDate&babyId=${enc(babyId)}&dateKey=${enc(dateKey)}",
            token,
        )
        return (0 until arr.length()).map { BabyRecord.fromJson(arr.getJSONObject(it)) }
    }

    fun createRecord(
        token: String,
        babyId: String,
        eventType: String,
        dateKey: String,
        occurredAt: Long,
        payload: JSONObject,
    ): BabyRecord {
        val body = JSONObject()
            .put("babyId", babyId)
            .put("eventType", eventType)
            .put("dateKey", dateKey)
            .put("occurredAt", occurredAt)
            .put("payload", payload)
        return BabyRecord.fromJson(post("records/create", body, token))
    }

    fun listTodos(token: String): List<BabyTodo> {
        val arr = getArray("todos/list", token)
        return (0 until arr.length()).map { BabyTodo.fromJson(arr.getJSONObject(it)) }
    }

    fun createTodo(token: String, title: String, category: String, babyId: String?): BabyTodo {
        val body = JSONObject()
            .put("title", title)
            .put("category", category)
        if (!babyId.isNullOrBlank()) body.put("babyId", babyId)
        return BabyTodo.fromJson(post("todos/create", body, token))
    }

    fun toggleTodo(token: String, id: String): BabyTodo {
        val body = JSONObject().put("id", id)
        return BabyTodo.fromJson(post("todos/toggle", body, token))
    }

    private fun get(route: String, token: String): JSONObject {
        val request = Request.Builder()
            .url(url(route))
            .header("X-Access-Token", token)
            .get()
            .build()
        return executeObject(request)
    }

    private fun getArray(route: String, token: String): JSONArray {
        val request = Request.Builder()
            .url(url(route))
            .header("X-Access-Token", token)
            .get()
            .build()
        return executeArray(request)
    }

    private fun post(route: String, body: JSONObject, token: String? = null): JSONObject {
        val builder = Request.Builder()
            .url(url(route))
            .post(body.toString().toRequestBody(jsonType))
        if (!token.isNullOrBlank()) {
            builder.header("X-Access-Token", token)
        }
        return executeObject(builder.build())
    }

    private fun executeObject(request: Request): JSONObject {
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val root = JSONObject(text.ifBlank { "{}" })
            if (!root.optBoolean("success", false)) {
                throw IllegalStateException(root.optString("message", "请求失败"))
            }
            return root.getJSONObject("data")
        }
    }

    private fun executeArray(request: Request): JSONArray {
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val root = JSONObject(text.ifBlank { "{}" })
            if (!root.optBoolean("success", false)) {
                throw IllegalStateException(root.optString("message", "请求失败"))
            }
            return root.getJSONArray("data")
        }
    }

    private fun url(route: String): String = "${baseUrl.trimEnd('/')}/index.php?r=$route"

    private fun enc(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")

    companion object {
        const val DEFAULT_BASE_URL = "https://api.guoziai.com/20260801"
    }
}
