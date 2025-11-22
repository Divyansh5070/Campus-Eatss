package com.divyansh.cueats

import kotlinx.serialization.Serializable

@Serializable
object SplashRoute

@Serializable
object LoginRoute

@Serializable
object MealsRoute

@Serializable
object ShopsRoute

@Serializable
object NotificationRoute

@Serializable
object AboutRoute

@Serializable
data class MealDetailsRoute(val mealId: String)

@Serializable
data class ShopMenuRoute(val shopId: String)

@Serializable
object CampusMapRoute