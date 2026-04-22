package com.divyansh.cueats.Mess

/**
 * Simple data class for dish information
 * Start with basic fields, can add more later
 */
data class Dish(
    val dishId: String = "",
    val name: String = "",
    val calories: Int = 0,
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val isVeg: Boolean = true,
    val isHot: Boolean = false,
    val nutrition: Nutrition = Nutrition()
)

/**
 * Nutrition information for a dish
 * Keep it simple - just the main macros
 */
data class Nutrition(
    val protein: Int = 0,  // in grams
    val carbs: Int = 0,    // in grams
    val fat: Int = 0       // in grams
)
