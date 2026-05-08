package com.jingqu.visitor.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jingqu_prefs")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        private val KEY_VISITOR_ID = stringPreferencesKey("visitor_id")
        private val KEY_SESSION_ID = stringPreferencesKey("session_id")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    }

    @SuppressLint("HardwareIds")
    suspend fun getVisitorId(): String {
        val stored = dataStore.data.map { preferences ->
            preferences[KEY_VISITOR_ID]
        }.first()

        if (stored != null) {
            return stored
        }

        val deviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) + "_" + System.currentTimeMillis()

        dataStore.edit { preferences ->
            preferences[KEY_VISITOR_ID] = deviceId
        }

        return deviceId
    }

    suspend fun getSessionId(): String {
        val stored = dataStore.data.map { preferences ->
            preferences[KEY_SESSION_ID]
        }.first()

        if (stored != null) {
            return stored
        }

        val sessionId = java.util.UUID.randomUUID().toString()
        dataStore.edit { preferences ->
            preferences[KEY_SESSION_ID] = sessionId
        }
        return sessionId
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun getNotificationsEnabled(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
        }.first()
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SOUND_ENABLED] = enabled
        }
    }

    suspend fun getSoundEnabled(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[KEY_SOUND_ENABLED] ?: true
        }.first()
    }
}
