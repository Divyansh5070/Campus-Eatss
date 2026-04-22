package com.divyansh.cueats.Mess

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

/**
 * One-time setup script to populate Firestore with sample dish data
 * Call this once to initialize the dishes collection
 */
object FirebaseSetup {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    /**
     * Populate Firestore with sample dishes
     * This only needs to be run once
     */
    fun setupSampleDishes(onComplete: (Boolean) -> Unit) {
        val dishes = getSampleDishes()
        var successCount = 0
        var failCount = 0
        val totalDishes = dishes.size
        
        Log.d("FirebaseSetup", "Starting to upload $totalDishes dishes...")
        
        dishes.forEach { dish ->
            firestore.collection("dishes")
                .document(dish.dishId)
                .set(dish)
                .addOnSuccessListener {
                    successCount++
                    Log.d("FirebaseSetup", "✅ Uploaded: ${dish.name} ($successCount/$totalDishes)")
                    
                    if (successCount + failCount == totalDishes) {
                        Log.d("FirebaseSetup", "✅ Setup complete! $successCount/$totalDishes dishes uploaded")
                        onComplete(true)
                    }
                }
                .addOnFailureListener { e ->
                    failCount++
                    Log.e("FirebaseSetup", "❌ Failed to upload ${dish.name}: ${e.message}")
                    
                    if (successCount + failCount == totalDishes) {
                        Log.d("FirebaseSetup", "Setup finished with errors: $successCount success, $failCount failed")
                        onComplete(false)
                    }
                }
        }
    }
    
    /**
     * Sample dishes matching your UI designs
     */
    private fun getSampleDishes(): List<Dish> {
        return listOf(
            Dish(
                dishId = "dhaba_chutney",
                name = "Dhaba Chutney",
                calories = 45,
                rating = 4.2,
                ratingCount = 156,
                isVeg = true,
                isHot = false,
                nutrition = Nutrition(protein = 2, carbs = 8, fat = 1)
            ),
            Dish(
                dishId = "masala_aloo_sandwich",
                name = "Masala Aloo Sandwich",
                calories = 320,
                rating = 4.5,
                ratingCount = 234,
                isVeg = true,
                isHot = true,
                nutrition = Nutrition(protein = 8, carbs = 45, fat = 12)
            ),
            Dish(
                dishId = "milk",
                name = "Milk",
                calories = 150,
                rating = 4.0,
                ratingCount = 189,
                isVeg = true,
                isHot = false,
                nutrition = Nutrition(protein = 8, carbs = 12, fat = 8)
            ),
            Dish(
                dishId = "tea",
                name = "Tea",
                calories = 80,
                rating = 4.3,
                ratingCount = 312,
                isVeg = true,
                isHot = true,
                nutrition = Nutrition(protein = 1, carbs = 15, fat = 2)
            ),
            Dish(
                dishId = "veg_poha_peanuts",
                name = "Veg Poha (Peanuts)",
                calories = 250,
                rating = 4.6,
                ratingCount = 278,
                isVeg = true,
                isHot = true,
                nutrition = Nutrition(protein = 6, carbs = 38, fat = 8)
            ),
            Dish(
                dishId = "kadhi_pakora",
                name = "Kadhi Pakora",
                calories = 280,
                rating = 4.4,
                ratingCount = 201,
                isVeg = true,
                isHot = true,
                nutrition = Nutrition(protein = 7, carbs = 32, fat = 14)
            ),
            Dish(
                dishId = "zakiriya_paratha",
                name = "Zakiriya Paratha",
                calories = 180,
                rating = 4.7,
                ratingCount = 156,
                isVeg = true,
                isHot = true,
                nutrition = Nutrition(protein = 5, carbs = 28, fat = 6)
            ),
            Dish(
                dishId = "rice",
                name = "Rice",
                calories = 200,
                rating = 4.1,
                ratingCount = 145,
                isVeg = true,
                isHot = true,
                nutrition = Nutrition(protein = 4, carbs = 45, fat = 0)
            ),
            Dish(
                dishId = "dal",
                name = "Dal",
                calories = 180,
                rating = 4.3,
                ratingCount = 198,
                isVeg = true,
                isHot = true,
                nutrition = Nutrition(protein = 9, carbs = 20, fat = 6)
            ),
            Dish(
                dishId = "roti",
                name = "Roti",
                calories = 70,
                rating = 4.2,
                ratingCount = 167,
                isVeg = true,
                isHot = true,
                nutrition = Nutrition(protein = 3, carbs = 15, fat = 0)
            ),
            Dish(
                dishId = "paratha",
                name = "Paratha",
                calories = 150,
                rating = 4.4,
                ratingCount = 189,
                isVeg = true,
                isHot = true,
                nutrition = Nutrition(protein = 4, carbs = 22, fat = 6)
            ),
            Dish(
                dishId = "aloo_paratha",
                name = "Aloo Paratha",
                calories = 200,
                rating = 4.5,
                ratingCount = 234,
                isVeg = true,
                isHot = true,
                nutrition = Nutrition(protein = 5, carbs = 28, fat = 8)
            ),
            Dish(
                dishId = "curd",
                name = "Curd",
                calories = 60,
                rating = 4.1,
                ratingCount = 123,
                isVeg = true,
                isHot = false,
                nutrition = Nutrition(protein = 3, carbs = 5, fat = 3)
            ),
            Dish(
                dishId = "paneer_curry",
                name = "Paneer Curry",
                calories = 280,
                rating = 4.6,
                ratingCount = 267,
                isVeg = true,
                isHot = true,
                nutrition = Nutrition(protein = 12, carbs = 18, fat = 18)
            ),
            Dish(
                dishId = "mixed_veg",
                name = "Mixed Veg",
                calories = 120,
                rating = 4.2,
                ratingCount = 145,
                isVeg = true,
                isHot = true,
                nutrition = Nutrition(protein = 4, carbs = 15, fat = 5)
            )
        )
    }
}
