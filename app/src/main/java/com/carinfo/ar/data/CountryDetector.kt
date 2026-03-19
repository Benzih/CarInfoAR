package com.carinfo.ar.data

import java.util.Locale

enum class SupportedCountry(
    val code: String,
    val displayName: String,
    val plateRegex: Regex
) {
    ISRAEL(
        code = "IL",
        displayName = "Israel",
        // 7-8 digits
        plateRegex = Regex("^\\d{7,8}$")
    ),
    NETHERLANDS(
        code = "NL",
        displayName = "Netherlands",
        // Sidecode formats: XX-99-XX, 99-XX-XX, XX-XX-99, 99-XXX-9, etc.
        // After cleaning: 6 alphanumeric chars
        plateRegex = Regex("^[A-Z0-9]{6}$")
    ),
    UK(
        code = "GB",
        displayName = "United Kingdom",
        // Current format: AB12 CDE (2 letters, 2 digits, 3 letters)
        // Also older formats
        plateRegex = Regex("^[A-Z]{2}\\d{2}[A-Z]{3}$|^[A-Z]\\d{1,3}[A-Z]{3}$|^[A-Z]{3}\\d{1,3}[A-Z]$|^[A-Z]{2}\\d{2,4}$|^\\d{1,4}[A-Z]{1,3}$")
    );

    companion object {
        fun fromLocale(): SupportedCountry? {
            val language = Locale.getDefault().language    // "he", "nl", "en"
            val country = Locale.getDefault().country      // "IL", "NL", "GB", "US"

            return when {
                language == "he" || country == "IL" -> ISRAEL
                language == "nl" || country == "NL" -> NETHERLANDS
                country == "GB" -> UK
                else -> {
                    // Try language-based fallback
                    when (language) {
                        "iw" -> ISRAEL  // Old Java code for Hebrew
                        else -> null
                    }
                }
            }
        }

        fun fromCode(code: String): SupportedCountry? {
            return entries.find { it.code == code }
        }
    }
}
