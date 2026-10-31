package com.morim.netloglens.core

/** 解析を頼むAIアプリ。先頭（Claude）が既定。 */
enum class AiTarget(val label: String, val packageName: String) {
    CLAUDE("Claude", "com.anthropic.claude"),
    CHATGPT("ChatGPT", "com.openai.chatgpt"),
    GEMINI("Gemini", "com.google.android.apps.bard"),
    COPILOT("Copilot", "com.microsoft.copilot"),
    ;

    companion object {
        val DEFAULT = CLAUDE

        fun fromName(name: String?): AiTarget = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
