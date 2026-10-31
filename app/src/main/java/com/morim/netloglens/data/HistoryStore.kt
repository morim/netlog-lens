package com.morim.netloglens.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** 解析1回分の記録。伏せ字の対応表（mapping）はスマホ内にだけ保存する。 */
data class HistoryEntry(
    val id: Long,
    val profileId: String,
    val purpose: String,
    val memo: String,
    val prompt: String,
    val mapping: Map<String, String>,
    val answer: String,
) {
    fun toJson() = JSONObject().apply {
        put("id", id)
        put("profileId", profileId)
        put("purpose", purpose)
        put("memo", memo)
        put("prompt", prompt)
        put("mapping", JSONObject(mapping))
        put("answer", answer)
    }

    companion object {
        fun fromJson(o: JSONObject): HistoryEntry {
            val m = o.optJSONObject("mapping") ?: JSONObject()
            return HistoryEntry(
                id = o.getLong("id"),
                profileId = o.optString("profileId"),
                purpose = o.optString("purpose"),
                memo = o.optString("memo"),
                prompt = o.optString("prompt"),
                mapping = m.keys().asSequence().associateWith { m.getString(it) },
                answer = o.optString("answer"),
            )
        }
    }
}

class HistoryStore(context: Context) {
    private val file = File(context.filesDir, "history.json")

    fun load(): List<HistoryEntry> = runCatching {
        if (!file.exists()) return emptyList()
        val arr = JSONArray(file.readText())
        (0 until arr.length()).map { HistoryEntry.fromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    fun save(entries: List<HistoryEntry>) {
        val arr = JSONArray()
        entries.sortedByDescending { it.id }.take(MAX).forEach { arr.put(it.toJson()) }
        val tmp = File(file.parentFile, "history.json.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(file)
    }

    companion object {
        const val MAX = 200
    }
}
