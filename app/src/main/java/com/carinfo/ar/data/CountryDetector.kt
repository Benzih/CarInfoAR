package com.carinfo.ar.data

import android.content.Context
import android.telephony.TelephonyManager
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
        /** Detect country: SIM/network first (physical location), then locale fallback */
        fun detect(context: Context): SupportedCountry? {
            // 1. Try SIM card country (no permission needed)
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                val simCountry = tm.simCountryIso?.uppercase()
                val networkCountry = tm.networkCountryIso?.uppercase()

                // Prefer network (current physical location) over SIM (home country)
                val physicalCountry = networkCountry?.takeIf { it.isNotEmpty() }
                    ?: simCountry?.takeIf { it.isNotEmpty() }

                if (physicalCountry != null) {
                    val match = fromCountryIso(physicalCountry)
                    if (match != null) return match
                }
            }

            // 2. Fallback to device locale
            return fromLocale()
        }

        private fun fromCountryIso(iso: String): SupportedCountry? {
            return when (iso) {
                "IL" -> ISRAEL
                "NL" -> NETHERLANDS
                "GB" -> UK
                else -> null
            }
        }

        fun fromLocale(): SupportedCountry? {
            val language = Locale.getDefault().language
            val country = Locale.getDefault().country

            return when {
                language == "he" || language == "iw" || country == "IL" -> ISRAEL
                language == "nl" || country == "NL" -> NETHERLANDS
                country == "GB" -> UK
                else -> null
            }
        }

        fun fromCode(code: String): SupportedCountry? {
            return entries.find { it.code == code }
        }
    }
}
