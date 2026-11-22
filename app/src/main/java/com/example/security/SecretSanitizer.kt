package com.example.security

object SecretSanitizer {
    private val GOOGLE_API_KEY_REGEX = Regex("AIza[0-9A-Za-z_\\-]{16,}")
    private val AQ_TOKEN_REGEX = Regex("AQ\\.[0-9A-Za-z_.\\-]{16,}")

    fun maskApiKey(key: String?): String {
        val clean = key?.trim().orEmpty()
        if (clean.isEmpty()) return "Not configured"
        if (clean.length <= 8) return "••••••••"
        return "${clean.take(4)}••••••••••••${clean.takeLast(4)}"
    }

    fun redact(text: String, activeCustomKey: String? = null): String {
        var result = text
        val cleanCustom = activeCustomKey?.trim().orEmpty()
        if (cleanCustom.length >= 8) {
            result = result.replace(cleanCustom, "[REDACTED_API_KEY]")
        }
        result = GOOGLE_API_KEY_REGEX.replace(result, "[REDACTED_API_KEY]")
        result = AQ_TOKEN_REGEX.replace(result, "[REDACTED_API_KEY]")
        return result
    }

    fun looksLikeValidApiKey(key: String): Boolean {
        val clean = key.trim()
        return clean.length >= 24 && !clean.any { it.isWhitespace() }
    }
}
