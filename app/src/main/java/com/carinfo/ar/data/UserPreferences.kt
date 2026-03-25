package com.carinfo.ar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object UserPreferences {
    private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    private val SELECTED_COUNTRY = stringPreferencesKey("selected_country")
    private val DVLA_API_KEY = stringPreferencesKey("dvla_api_key")
    private val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    private val APP_LANGUAGE = stringPreferencesKey("app_language") // "", "iw", "nl", "en"

    fun isOnboardingComplete(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[ONBOARDING_COMPLETE] ?: false }

    fun getSelectedCountry(context: Context): Flow<String?> =
        context.dataStore.data.map { it[SELECTED_COUNTRY] }

    fun getDvlaApiKey(context: Context): Flow<String> =
        context.dataStore.data.map { it[DVLA_API_KEY] ?: "" }

    fun isSoundEnabled(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[SOUND_ENABLED] ?: true }

    fun getAppLanguage(context: Context): Flow<String> =
        context.dataStore.data.map { it[APP_LANGUAGE] ?: "" } // "" = device default

    suspend fun setOnboardingComplete(context: Context, complete: Boolean) {
        context.dataStore.edit { it[ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setSelectedCountry(context: Context, countryCode: String) {
        context.dataStore.edit { it[SELECTED_COUNTRY] = countryCode }
    }

    suspend fun setDvlaApiKey(context: Context, key: String) {
        context.dataStore.edit { it[DVLA_API_KEY] = key }
    }

    suspend fun setSoundEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[SOUND_ENABLED] = enabled }
    }

    suspend fun setAppLanguage(context: Context, langCode: String) {
        context.dataStore.edit { it[APP_LANGUAGE] = langCode }
        // Also write to SharedPreferences for fast access in attachBaseContext
        // (DataStore is too slow for attachBaseContext — causes ANR)
        context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("app_language", langCode)
            .apply()
    }
}
