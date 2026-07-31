package com.xiaolai.todo.model

data class EventTypeOption(
    val value: String,
    val label: String,
    val shortLabel: String = label,
    val needsAmount: Boolean = false,
    val needsSide: Boolean = false,
    val needsNote: Boolean = false,
    val needsJaundice: Boolean = false,
    val needsCustom: Boolean = false,
    val instantConfirm: Boolean = false,
)

object EventTypes {
    val all = listOf(
        EventTypeOption("feeding_formula", "奶粉", needsAmount = true),
        EventTypeOption("feeding_breast", "亲喂", needsSide = true),
        EventTypeOption("feeding_warm_breast", "热母乳", needsAmount = true),
        EventTypeOption("sleep_start", "睡觉", shortLabel = "睡", instantConfirm = true),
        EventTypeOption("sleep_end", "醒来", shortLabel = "醒", instantConfirm = true),
        EventTypeOption("poop", "拉粑粑", shortLabel = "便便", instantConfirm = true),
        EventTypeOption("care", "护理", instantConfirm = true),
        EventTypeOption("butt_clean", "洗屁股", shortLabel = "洗屁", instantConfirm = true),
        EventTypeOption("gas_exercise", "排气操", instantConfirm = true),
        EventTypeOption("jaundice_check", "测黄疸", shortLabel = "黄疸", needsJaundice = true),
        EventTypeOption("bath", "洗澡", instantConfirm = true),
        EventTypeOption("note", "备注", needsNote = true),
        EventTypeOption("custom", "自定义", needsCustom = true),
    )

    val formulaQuickValues: List<Int> = (20..200 step 5).toList()
    val defaultFormulaFavorites = listOf(40, 45, 60, 65, 70)

    fun of(type: String): EventTypeOption =
        all.firstOrNull { it.value == type } ?: EventTypeOption(type, type)

    fun labelOf(type: String): String = of(type).label

    fun visible(hidden: Set<String>): List<EventTypeOption> =
        all.filter { it.value !in hidden }

    fun summarize(record: BabyRecord): String {
        val payload = record.payload
        return when (record.eventType) {
            "feeding_formula" -> "奶粉 ${payload.optInt("amountMl")}ml"
            "feeding_warm_breast" -> "热母乳 ${payload.optInt("amountMl")}ml"
            "feeding_breast" -> {
                val side = when (payload.optString("side")) {
                    "left" -> "左侧"
                    "right" -> "右侧"
                    else -> payload.optString("side")
                }
                "亲喂 $side ${payload.optInt("durationMin")}分钟"
            }
            "sleep_start" -> "开始睡觉"
            "sleep_end" -> "醒来"
            "poop" -> "拉粑粑"
            "care" -> "护理"
            "butt_clean" -> "洗屁股"
            "gas_exercise" -> "排气操"
            "jaundice_check" -> "黄疸 额${payload.optDouble("foreheadValue")} / 胸${payload.optDouble("chestValue")}"
            "bath" -> "洗澡"
            "note" -> payload.optString("note").ifBlank { "备注" }
            "custom" -> payload.optString("customLabel").ifBlank { "自定义" }
            else -> labelOf(record.eventType)
        }
    }
}
