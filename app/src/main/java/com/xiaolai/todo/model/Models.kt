package com.xiaolai.todo.model

import org.json.JSONArray
import org.json.JSONObject

data class FamilyMember(
    val id: String,
    val familyId: String,
    val openId: String,
    val displayName: String,
    val role: String,
    val accessToken: String,
) {
    companion object {
        fun fromJson(json: JSONObject) = FamilyMember(
            id = json.optString("id"),
            familyId = json.optString("familyId"),
            openId = json.optString("openId"),
            displayName = json.optString("displayName"),
            role = json.optString("role"),
            accessToken = json.optString("accessToken"),
        )
    }
}

data class BabyProfile(
    val id: String,
    val familyId: String,
    val nickname: String,
    val birthday: String,
    val gender: String,
    val note: String,
) {
    companion object {
        fun fromJson(json: JSONObject) = BabyProfile(
            id = json.optString("id"),
            familyId = json.optString("familyId"),
            nickname = json.optString("nickname"),
            birthday = json.optString("birthday"),
            gender = json.optString("gender"),
            note = json.optString("note"),
        )
    }
}

data class FamilyInfo(
    val id: String,
    val name: String,
    val inviteCode: String,
    val babyId: String,
) {
    companion object {
        fun fromJson(json: JSONObject) = FamilyInfo(
            id = json.optString("id"),
            name = json.optString("name"),
            inviteCode = json.optString("inviteCode"),
            babyId = json.optString("babyId"),
        )
    }
}

data class AppContextData(
    val member: FamilyMember,
    val family: FamilyInfo?,
    val baby: BabyProfile?,
    val members: List<FamilyMember>,
) {
    companion object {
        fun fromJson(json: JSONObject): AppContextData {
            val membersArr = json.optJSONArray("members") ?: JSONArray()
            val members = (0 until membersArr.length()).map {
                FamilyMember.fromJson(membersArr.getJSONObject(it))
            }
            return AppContextData(
                member = FamilyMember.fromJson(json.getJSONObject("member")),
                family = json.optJSONObject("family")?.let(FamilyInfo::fromJson),
                baby = json.optJSONObject("baby")?.let(BabyProfile::fromJson),
                members = members,
            )
        }
    }
}

data class SummaryCounts(
    val formulaAmountTotal: Double = 0.0,
    val formulaFeedCount: Int = 0,
    val breastFeedCount: Int = 0,
    val poopCount: Int = 0,
    val careCount: Int = 0,
    val sleepStartCount: Int = 0,
    val sleepEndCount: Int = 0,
    val bathCount: Int = 0,
    val totalCount: Int = 0,
) {
    companion object {
        fun fromJson(json: JSONObject?) = SummaryCounts(
            formulaAmountTotal = json?.optDouble("formulaAmountTotal") ?: 0.0,
            formulaFeedCount = json?.optInt("formulaFeedCount") ?: 0,
            breastFeedCount = json?.optInt("breastFeedCount") ?: 0,
            poopCount = json?.optInt("poopCount") ?: 0,
            careCount = json?.optInt("careCount") ?: 0,
            sleepStartCount = json?.optInt("sleepStartCount") ?: 0,
            sleepEndCount = json?.optInt("sleepEndCount") ?: 0,
            bathCount = json?.optInt("bathCount") ?: 0,
            totalCount = json?.optInt("totalCount") ?: 0,
        )
    }
}

data class BabyRecord(
    val id: String,
    val eventType: String,
    val occurredAt: Long,
    val dateKey: String,
    val payload: JSONObject,
    val createdByName: String,
) {
    companion object {
        fun fromJson(json: JSONObject) = BabyRecord(
            id = json.optString("id"),
            eventType = json.optString("eventType"),
            occurredAt = json.optLong("occurredAt"),
            dateKey = json.optString("dateKey"),
            payload = json.optJSONObject("payload") ?: JSONObject(),
            createdByName = json.optString("createdByName"),
        )
    }
}

data class DashboardData(
    val todaySummary: SummaryCounts,
    val weekSummary: SummaryCounts,
    val recentRecords: List<BabyRecord>,
) {
    companion object {
        fun fromJson(json: JSONObject): DashboardData {
            val arr = json.optJSONArray("recentRecords") ?: JSONArray()
            val recent = (0 until arr.length()).map { BabyRecord.fromJson(arr.getJSONObject(it)) }
            return DashboardData(
                todaySummary = SummaryCounts.fromJson(json.optJSONObject("todaySummary")),
                weekSummary = SummaryCounts.fromJson(json.optJSONObject("weekSummary")),
                recentRecords = recent,
            )
        }
    }
}

data class BabyTodo(
    val id: String,
    val title: String,
    val category: String,
    val status: String,
    val note: String,
) {
    val isDone: Boolean get() = status == "done"

    companion object {
        fun fromJson(json: JSONObject) = BabyTodo(
            id = json.optString("id"),
            title = json.optString("title"),
            category = json.optString("category"),
            status = json.optString("status"),
            note = json.optString("note"),
        )
    }
}

data class EventTypeOption(
    val value: String,
    val label: String,
    val needsAmount: Boolean = false,
    val needsSide: Boolean = false,
    val needsNote: Boolean = false,
)

object EventTypes {
    val quickActions = listOf(
        EventTypeOption("feeding_formula", "奶粉", needsAmount = true),
        EventTypeOption("feeding_breast", "母乳", needsSide = true),
        EventTypeOption("sleep_start", "睡觉"),
        EventTypeOption("sleep_end", "醒来"),
        EventTypeOption("poop", "拉粑粑"),
        EventTypeOption("care", "护理"),
        EventTypeOption("butt_clean", "洗屁股"),
        EventTypeOption("gas_exercise", "排气操"),
        EventTypeOption("jaundice_check", "测黄疸"),
        EventTypeOption("bath", "洗澡"),
        EventTypeOption("note", "备注", needsNote = true),
    )

    fun labelOf(type: String): String =
        quickActions.firstOrNull { it.value == type }?.label ?: type
}
