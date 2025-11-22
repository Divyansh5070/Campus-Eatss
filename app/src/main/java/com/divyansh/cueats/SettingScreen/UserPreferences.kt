package com.divyansh.cueats.SettingScreen

// UserPreferences.kt
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {
    companion object {
        val IS_HOSTELLER = booleanPreferencesKey("is_hosteller")
        val USER_NAME = stringPreferencesKey("user_name")
        val UNIVERSITY_ID = stringPreferencesKey("university_id")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    val isHosteller: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_HOSTELLER] ?: false
    }

    val userName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME] ?: ""
    }

    val universityId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[UNIVERSITY_ID] ?: ""
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN] ?: true
    }

    suspend fun setHostellerStatus(isHosteller: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_HOSTELLER] = isHosteller
        }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = name
        }
    }

    suspend fun setUniversityId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[UNIVERSITY_ID] = id
        }
    }

    suspend fun setLoggedIn(isLoggedIn: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = isLoggedIn
        }
    }

    suspend fun clearAllData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
