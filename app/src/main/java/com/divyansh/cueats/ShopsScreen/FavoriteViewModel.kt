package com.divyansh.cueats.ShopsScreen

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FavoriteViewModel(private val context: Context) : ViewModel() {

    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
    }

    private val _favoriteShops = MutableStateFlow<Set<String>>(emptySet())
    val favoriteShops: StateFlow<Set<String>> = _favoriteShops.asStateFlow()

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch(Dispatchers.IO) {
            val favoritesSet = sharedPrefs.getStringSet("favorite_shops", emptySet()) ?: emptySet()
            withContext(Dispatchers.Main) {
                _favoriteShops.value = favoritesSet
            }
        }
    }

    fun toggleFavorite(shopId: String) {
        viewModelScope.launch {
            val currentFavorites = _favoriteShops.value.toMutableSet()
            val isCurrentlyFavorite = currentFavorites.contains(shopId)

            if (isCurrentlyFavorite) {
                currentFavorites.remove(shopId)
            } else {
                currentFavorites.add(shopId)
            }

            // Update UI immediately
            _favoriteShops.value = currentFavorites

            // Save to SharedPreferences in background
            launch(Dispatchers.IO) {
                sharedPrefs.edit()
                    .putStringSet("favorite_shops", currentFavorites)
                    .apply()
            }
        }
    }

    fun isFavorite(shopId: String): Boolean {
        return _favoriteShops.value.contains(shopId)
    }

    fun getFavoriteCount(): Int {
        return _favoriteShops.value.size
    }

    fun clearAllFavorites() {
        viewModelScope.launch {
            _favoriteShops.value = emptySet()
            launch(Dispatchers.IO) {
                sharedPrefs.edit().clear().apply()
            }
        }
    }
}

class FavoriteViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavoriteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FavoriteViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}