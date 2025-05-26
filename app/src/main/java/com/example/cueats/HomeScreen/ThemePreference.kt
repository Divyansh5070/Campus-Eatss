package com.example.cueats.HomeScreen

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object ThemePreference {
    private val Context.dataStore by preferencesDataStore(name = "settings")

    private val THEME_KEY = booleanPreferencesKey("dark_theme")

    suspend fun saveTheme(context: Context, isDarkTheme: Boolean) {
        context.dataStore.edit { settings ->
            settings[THEME_KEY] = isDarkTheme
        }
    }

    fun getTheme(context: Context): Flow<Boolean?> {
        return context.dataStore.data
            .map { preferences ->
                preferences[THEME_KEY]
            }
    }
}
