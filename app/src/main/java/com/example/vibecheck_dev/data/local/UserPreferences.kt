package com.example.vibecheck_dev.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Inisialisasi file penyimpanan lokal bernama "vibecheck_prefs"
val Context.dataStore by preferencesDataStore(name = "vibecheck_prefs")

class UserPreferences(private val context: Context) {

    // Kunci buat nyari data di laci memori
    companion object {
        val IS_FIRST_TIME = booleanPreferencesKey("is_first_time")
        val PLAYER_NAME = stringPreferencesKey("player_name")

        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
    }

    // Fungsi untuk BACA data secara real-time (Flow)
    val isFirstTimeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_FIRST_TIME] ?: true // Defaultnya 'true' kalau belum pernah buka
    }

    val playerNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PLAYER_NAME] ?: ""
    }

    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_LOGGED_IN] ?: false }
    val authTokenFlow: Flow<String> = context.dataStore.data.map { it[AUTH_TOKEN] ?: "" }

    // Fungsi untuk SIMPAN data
    suspend fun setFirstTimeCompleted() {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_TIME] = false
        }
    }

    suspend fun savePlayerName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PLAYER_NAME] = name
        }
    }

    suspend fun saveAuthSession(token: String, isLogged: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = token
            preferences[IS_LOGGED_IN] = isLogged
        }
    }
}