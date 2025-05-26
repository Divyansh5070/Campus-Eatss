package com.example.cueats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.work.*
import com.example.cueats.BudgetScreen.BudgetRecommendationScreen
import com.example.cueats.HomeScreen.MealDetailsScreen
import com.example.cueats.HomeScreen.ModernWeeklyMenuApp
import com.example.cueats.LoginScreen.CampusEatsAuthScreen
import com.example.cueats.Notification.NotificationScreen
import com.example.cueats.ShopsScreen.ShopMenuScreen
import java.util.concurrent.TimeUnit


import com.example.cueats.notifirebase.MealNotificationWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.onesignal.OneSignal
import java.util.Calendar




class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OneSignal
        OneSignal.initWithContext(this, "50fef6c1-9302-42b5-b15e-0bc1e260632e")
        FirebaseApp.initializeApp(this)
        // Request Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }

        // Get Firebase Cloud Messaging Token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FCM Token", "FCM Token: ${task.result}")
            }
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // ✅ Request Notification Permission (For Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }



        // ✅ Remove any direct notification triggering (Fixes duplicate issue)
        if (savedInstanceState == null) {  // Prevents re-execution on screen rotation
            enqueueMealNotifications()
        }

        // ✅ Load the UI (Navigation + Screens)
        setContent {
            MealPlannerApp()
        }
    }

    private fun enqueueMealNotifications() {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)

        val breakfastMessages = listOf(
            "Guess what’s cooking this morning? 🍳👀",
            "Something hot and tasty is waiting… come check it out! 🥐🔥",
            "What’s your guess: Paratha or Poha today? 🥘🤔",
            "A delicious surprise awaits you – don’t miss breakfast! 🥞❓",
            "The aroma in the air isn’t lying… breakfast’s calling! ☕️👃"
        )

        val lunchMessages = listOf(
            "Can you guess the main dish today? 🍛👀",
            "There’s something special on your plate today! 🍲🎁",
            "Lunch just got interesting… any guesses? 🥙🔍",
            "A tasty twist awaits your lunchtime – curious? 😋❓",
            "Today's lunch might surprise you… check it out! 🍽️🕵️"
        )

        val snackMessages = listOf(
            "It’s that time… but what’s the snack today? 🤤🧐",
            "Snack hour just dropped – something sweet or salty? 🍪🎯",
            "Bet you didn’t expect *this* as today’s snack! 🍩👀",
            "A mini treat is hiding in plain sight… go find it! 🧁🔍",
            "Something’s waiting to crunch your cravings! 🍿❓"
        )

        val dinnerMessages = listOf(
            "Dinner’s on – but there’s a twist! What could it be? 🥘😮",
            "A cozy meal is ready – want to know what’s special tonight? 🌙🍛",
            "Could tonight be your favorite dish? Only one way to know… 🍽️🤫",
            "Your evening just got tastier… come find out how! 🕯️🍲",
            "Something comforting is waiting to end your day just right… 🛋️🍜"
        )

        val mealTimes = listOf(
            Triple("Breakfast", if (isWeekend) "08:00" else "07:30", breakfastMessages),
            Triple("Lunch", if (isWeekend) "12:30" else "12:00", lunchMessages),
            Triple("Snacks", "16:30", snackMessages),
            Triple("Dinner", "19:30", dinnerMessages)
        )

        for ((mealName, time, messages) in mealTimes) {
            val (hour, minute) = time.split(":").map { it.toInt() }
            val randomMessage = messages.random()
            scheduleMealNotification(mealName, randomMessage, hour, minute)
        }
    }



    private fun scheduleMealNotification(mealName: String, message: String, hour: Int, minute: Int) {
        val now = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (targetTime.before(now)) {
            targetTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        val delay = targetTime.timeInMillis - now.timeInMillis

        val workRequest = OneTimeWorkRequestBuilder<MealNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    "mealName" to mealName,
                    "message" to message
                )
            )
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            mealName,  // ✅ Prevents duplicate notifications
            ExistingWorkPolicy.REPLACE,  // Ensures only one notification per meal
            workRequest
        )
    }
}


// Update your MealPlannerApp with animation configurations
@Composable
fun MealPlannerApp() {
    val navController = rememberNavController()

    // Apply custom animations to NavHost
    NavHost(
        navController = navController,
        startDestination = "meals",
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        // 🔹 Main Meals Screen
        composable("meals") {
            ModernWeeklyMenuApp(navController)
        }

        // 🔹 Meal Details Screen (Handles null mealId safely)
        composable("meal_details/{mealId}") { backStackEntry ->
            val mealId = backStackEntry.arguments?.getString("mealId") ?: "Unknown"
            MealDetailsScreen(mealId = mealId, navController = navController)
        }

        // 🔹 Shops Screen
        composable("shops") {
            ShopMenuScreen(navController)
        }

        composable("budget") {
            BudgetRecommendationScreen(
                navController = navController,
            )
        }
    }
}

// Use this consistent bottom navigation bar in all your screens
@Composable
fun AppBottomNavigation(navController: NavController, currentRoute: String) {
    // Check system theme
    val isLightTheme = !isSystemInDarkTheme()

    // Define colors based on current theme
    val surfaceColor = if (isLightTheme) Color(0xFFEEEEEE) else Color(0xFF202020)
    val primaryOrange = Color(0xFFFF7F24)
    val textSecondaryColor = if (isLightTheme) Color(0xFF757575) else Color(0xFFBBBBBB)
    val indicatorColor = if (isLightTheme) Color(0xFFE0E0E0) else Color(0xFF303030)

    NavigationBar(
        containerColor = surfaceColor,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Mess") },
            label = { Text("Mess", fontSize = 14.sp) },
            selected = currentRoute == "meals",
            onClick = {
                if (currentRoute != "meals") {
                    navController.navigate("meals") {
                        // Clear the back stack up to the start destination
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = primaryOrange,
                selectedTextColor = primaryOrange,
                unselectedIconColor = textSecondaryColor,
                unselectedTextColor = textSecondaryColor,
                indicatorColor = indicatorColor
            )
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Shops") },
            label = { Text("Shops", fontSize = 14.sp) },
            selected = currentRoute == "shops",
            onClick = {
                if (currentRoute != "shops") {
                    navController.navigate("shops") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = primaryOrange,
                selectedTextColor = primaryOrange,
                unselectedIconColor = textSecondaryColor,
                unselectedTextColor = textSecondaryColor,
                indicatorColor = indicatorColor
            )
        )

        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.moneybag),
                    contentDescription = "Budget",
                    modifier = Modifier.size(24.dp),
                    tint = if (currentRoute == "budget") primaryOrange else textSecondaryColor
                )
            },
            label = { Text("Budget", fontSize = 14.sp) },
            selected = currentRoute == "budget",
            onClick = {
                if (currentRoute != "budget") {
                    navController.navigate("budget") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = primaryOrange,
                selectedTextColor = primaryOrange,
                unselectedIconColor = textSecondaryColor,
                unselectedTextColor = textSecondaryColor,
                indicatorColor = indicatorColor
            )
        )
    }
}

