package com.morim.netloglens

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.morim.netloglens.core.DeviceProfile
import com.morim.netloglens.core.DeviceProfiles
import com.morim.netloglens.core.MaskCategory
import com.morim.netloglens.core.MaskResult
import com.morim.netloglens.core.Masker
import com.morim.netloglens.core.PromptBuilder
import com.morim.netloglens.core.Purpose
import com.morim.netloglens.data.HistoryEntry
import com.morim.netloglens.data.HistoryStore

enum class Screen { HOME, CAMERA, REVIEW, ANSWER, HISTORY }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val store = HistoryStore(app)

    var screen by mutableStateOf(Screen.HOME)

    var profileId by mutableStateOf(prefs.getString("profile", null) ?: DeviceProfiles.all.first().id)
        private set
    val profile: DeviceProfile get() = DeviceProfiles.byId(profileId)

    var purpose by mutableStateOf(Purpose.TROUBLE)
    var memo by mutableStateOf("")

    /** OCR結果（ユーザーが手で直せる） */
    var ocrText by mutableStateOf("")
    var shotCount by mutableStateOf(0)

    var enabled by mutableStateOf(
        MaskCategory.entries.filter { prefs.getBoolean("mask_${it.name}", true) }.toSet(),
    )
        private set

    /** 今表示中の解析（履歴から開いた場合も含む） */
    var current by mutableStateOf<HistoryEntry?>(null)
        private set

    val history = mutableStateListOf<HistoryEntry>().apply { addAll(store.load()) }

    fun selectProfile(id: String) {
        profileId = id
        prefs.edit().putString("profile", id).apply()
    }

    fun toggle(cat: MaskCategory, on: Boolean) {
        enabled = if (on) enabled + cat else enabled - cat
        prefs.edit().putBoolean("mask_${cat.name}", on).apply()
    }

    fun appendOcr(text: String) {
        ocrText = if (ocrText.isBlank()) text else ocrText.trimEnd() + "\n" + text
        shotCount++
    }

    fun mask(): MaskResult = Masker(profile, enabled).mask(ocrText)

    fun buildPrompt(result: MaskResult) = PromptBuilder.build(profile, purpose, memo, result.text)

    /** Claudeに送る直前に呼ぶ。履歴に保存し、回答の貼り付け先として覚えておく。 */
    fun startAnalysis(prompt: String, result: MaskResult): HistoryEntry {
        val entry = HistoryEntry(
            id = System.currentTimeMillis(),
            profileId = profileId,
            purpose = purpose.label,
            memo = memo,
            prompt = prompt,
            mapping = result.mapping,
            answer = "",
        )
        history.add(0, entry)
        current = entry
        persist()
        return entry
    }

    fun saveAnswer(answer: String) {
        val c = current ?: return
        val updated = c.copy(answer = answer)
        val i = history.indexOfFirst { it.id == c.id }
        if (i >= 0) history[i] = updated else history.add(0, updated)
        current = updated
        persist()
    }

    fun open(entry: HistoryEntry) {
        current = entry
        screen = Screen.ANSWER
    }

    fun delete(entry: HistoryEntry) {
        history.removeAll { it.id == entry.id }
        if (current?.id == entry.id) current = null
        persist()
    }

    fun newSession() {
        ocrText = ""
        shotCount = 0
        memo = ""
        current = null
        screen = Screen.HOME
    }

    private fun persist() = store.save(history.toList())
}
