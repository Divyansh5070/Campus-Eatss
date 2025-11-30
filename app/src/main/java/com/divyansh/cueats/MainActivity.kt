package com.divyansh.cueats

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
import android.view.View
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.work.*
import com.divyansh.cueats.HomeScreen.HomeScreen
import com.divyansh.cueats.HomeScreen.MealDetailsScreen
import com.divyansh.cueats.HomeScreen.ModernWeeklyMenuApp
import com.divyansh.cueats.ShopsScreen.ShopMenuDetailScreen
import com.divyansh.cueats.ShopsScreen.ShopMenuScreen
import java.util.concurrent.TimeUnit
import com.divyansh.cueats.notifirebase.MealNotificationWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.onesignal.OneSignal
import java.util.Calendar
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.times
import androidx.lifecycle.viewmodel.compose.viewModel
import com.divyansh.cueats.LoginScreen.AuthViewModel
import com.divyansh.cueats.LoginScreen.LoginScreen
import com.divyansh.cueats.Notification.AboutScreen
import com.divyansh.cueats.Notification.NotificationScreen
import com.divyansh.cueats.R
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.divyansh.cueats.Maps.EnhancedCampusMap
import com.divyansh.cueats.ProfileScreen.ProfileScreen
import kotlinx.coroutines.delay
import androidx.lifecycle.ViewModelProvider
import com.divyansh.cueats.ShopsScreen.RatingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)



        // Request Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
        }

        // Get Firebase Cloud Messaging Token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FCM Token", "FCM Token: ${task.result}")
            }
        }
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Request Notification Permission (For Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Remove any direct notification triggering (Fixes duplicate issue)
        if (savedInstanceState == null) {  // Prevents re-execution on screen rotation
            enqueueMealNotifications()
        }

        // IMPORTANT: Load the UI immediately with splash screen as start destin   nation
        setContent {
            MealPlannerAppWithAuth()
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

@Composable
fun MealPlannerAppWithAuth() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState
    val systemTheme = isSystemInDarkTheme()

    val backgroundColor = if (systemTheme) Color(0xFF121212) else Color(0xFFF6F7FB)

    // Don't handle navigation automatically - let splash screen handle initial navigation
    // Only handle auth state changes after splash is done
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route

    LaunchedEffect(authState.isLoggedIn, authState.user, currentDestination) {
        // Only handle auth navigation if we're not on splash screen
        if (currentDestination != "SplashRoute" && currentDestination != null) {
            if (authState.isLoggedIn && authState.user != null) {
                if (currentDestination == "LoginRoute") {
                    navController.navigate(HomeRoute) {
                        popUpTo<LoginRoute> { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } else if (!authState.isLoggedIn && !authState.isLoading) {
                if (currentDestination != "LoginRoute") {
                    navController.navigate(LoginRoute) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        NavHost(
            navController = navController,
            startDestination = SplashRoute,
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            composable<SplashRoute> {
                SplashScreen(
                    navController = navController,
                    authViewModel = authViewModel
                )
            }

            composable<LoginRoute> {
                LoginScreen(
                    onNavigateToHome = {
                        if (authState.isLoggedIn && authState.user != null) {
                            navController.navigate(HomeRoute) {
                                popUpTo<LoginRoute> { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    viewModel = authViewModel
                )
            }

            composable<HomeRoute> {
                if (authState.isLoggedIn && authState.user != null) {
                    HomeScreen(navController, authViewModel)
                } else {
                    LoadingScreen()
                }
            }

            composable<MealsRoute> {
                if (authState.isLoggedIn && authState.user != null) {
                    ModernWeeklyMenuApp(navController, authViewModel)
                } else {
                    LoadingScreen()
                }
            }

            composable<CampusMapRoute> {
                EnhancedCampusMap(
                    context = LocalContext.current,
                    navController = navController
                )
            }

            composable<NotificationRoute> {
                NotificationScreen(navController)
            }

            composable<AboutRoute> {
                AboutScreen(navController)
            }

            composable<MealDetailsRoute> { backStackEntry ->
                val mealDetailsRoute = backStackEntry.toRoute<MealDetailsRoute>()
                MealDetailsScreen(
                    mealId = mealDetailsRoute.mealId,
                    navController = navController
                )
            }

            composable<ShopsRoute> {
                ShopsScreenWithLoading(navController)
            }

            composable<ShopMenuRoute> { backStackEntry ->
                val shopMenuRoute = backStackEntry.toRoute<ShopMenuRoute>()
                ShopMenuDetailScreen(
                    navController = navController,
                    shopId = shopMenuRoute.shopId
                )
            }

            composable<ProfileRoute> {
                if (authState.isLoggedIn && authState.user != null) {
                    ProfileScreen(navController, authViewModel)
                } else {
                    LoadingScreen()
                }
            }

        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFFFF6B35))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Checking authentication...")
        }
    }
}

@Composable
fun ShopsScreenWithLoading(navController: NavController) {
    var isLoading by remember { mutableStateOf(true) }
    val isLightTheme = !isSystemInDarkTheme()

    val backgroundColor = if (isLightTheme) Color(0xFFF6F7FB) else Color(0xFF121212)
    val primaryOrange = Color(0xFFFF6B35)
    val textColor = if (isLightTheme) Color(0xFF2D2D2D) else Color(0xFFE8E8E8)

    // Simulate loading shops data
    LaunchedEffect(Unit) {
        delay(1000) // Simulate API call or data loading

        isLoading = false
    }

    if (isLoading) {
        // Modern loading screen for shops - FIXED: Use Box with proper constraints
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(24.dp)
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Animated shop icon
                val infiniteTransition = rememberInfiniteTransition(label = "loading")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )

                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                // Shop icon background
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(120.dp)
                        .scale(pulseScale)
                        .background(
                            color = primaryOrange.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                        .padding(24.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.shopsicon),
                        contentDescription = "Loading Shops",
                        modifier = Modifier
                            .size(48.dp)
                            .rotate(rotation),
                        tint = primaryOrange
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Loading text
                Text(
                    text = "Loading Shops",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Finding the best food spots for you...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = textColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Progress indicator
                LinearProgressIndicator(
                    modifier = Modifier
                        .width(200.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = primaryOrange,
                    trackColor = primaryOrange.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Loading dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { index ->
                        val dotAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600),
                                repeatMode = RepeatMode.Reverse,
                                initialStartOffset = StartOffset(index * 200)
                            ),
                            label = "dot_$index"
                        )

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = primaryOrange.copy(alpha = dotAlpha),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    } else {
        // Show actual shops screen
        ShopMenuScreen(navController)
    }
}
// Optimized Bottom Navigation
@Composable
fun AppBottomNavigation(navController: NavController, currentRoute: String?) {
    val isLightTheme = !isSystemInDarkTheme()

    // Memoized colors to prevent recomposition
    val colors = remember(isLightTheme) {
        BottomNavColors(
            surface = if (isLightTheme) Color(0xFFFAFAFA) else Color(0xFF1A1A1A),
            primaryOrange = Color(0xFFFF6B35),
            textPrimary = if (isLightTheme) Color(0xFF2D2D2D) else Color(0xFFE8E8E8),
            textSecondary = if (isLightTheme) Color(0xFF8A8A8A) else Color(0xFF9A9A9A),
            divider = if (isLightTheme) Color(0xFFE0E0E0) else Color(0xFF404040)
        )
    }

    // Determine current tab based on route
    val currentTab = remember(currentRoute) {
        when {
            currentRoute?.contains("HomeRoute") == true ||
                    currentRoute == "home" -> BottomNavTab.HOME
            currentRoute?.contains("MealsRoute") == true ||
                    currentRoute?.contains("MealDetailsRoute") == true ||
                    currentRoute == "meals" -> BottomNavTab.MEALS
            currentRoute?.contains("ShopsRoute") == true ||
                    currentRoute?.contains("ShopMenuRoute") == true ||
                    currentRoute == "shops" -> BottomNavTab.SHOPS
            else -> BottomNavTab.HOME
        }
    }

    // Animated indicator offset
    val indicatorOffset by animateFloatAsState(
        targetValue = when (currentTab) {
            BottomNavTab.HOME -> 0.17f
            BottomNavTab.MEALS -> 0.5f
            BottomNavTab.SHOPS -> 0.83f
        },
        animationSpec = tween(
            durationMillis = 200,
            easing = FastOutSlowInEasing
        ),
        label = "indicator_offset"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        color = colors.surface,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(colors.surface)
        ) {
            // Animated top indicator
            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(3.dp)
            ) {
                val maxWidth = this.maxWidth
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(3.dp)
                        .offset(x = (indicatorOffset * maxWidth) - 30.dp)
                        .background(
                            color = colors.primaryOrange,
                            shape = RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp)
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Tab
                OptimizedNavItem(
                    iconRes = R.drawable.home,
                    label = "Home",
                    isSelected = currentTab == BottomNavTab.HOME,
                    colors = colors,
                    onClick = {
                        if (currentTab != BottomNavTab.HOME) {
                            navController.navigate(HomeRoute) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(colors.divider)
                )

                // Meals Tab
                OptimizedNavItem(
                    iconRes = R.drawable.mess,
                    label = "Mess",
                    isSelected = currentTab == BottomNavTab.MEALS,
                    colors = colors,
                    onClick = {
                        if (currentTab != BottomNavTab.MEALS) {
                            navController.navigate(MealsRoute) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(colors.divider)
                )

                // Shops Tab
                OptimizedNavItem(
                    iconRes = R.drawable.shopsicon,
                    label = "Shops",
                    isSelected = currentTab == BottomNavTab.SHOPS,
                    colors = colors,
                    onClick = {
                        if (currentTab != BottomNavTab.SHOPS) {
                            navController.navigate(ShopsRoute) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun OptimizedNavItem(
    iconRes: Int,
    label: String,
    isSelected: Boolean,
    colors: BottomNavColors,
    onClick: () -> Unit
) {
    // Animated colors for smooth transitions
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) colors.primaryOrange else colors.textSecondary,
        animationSpec = tween(durationMillis = 150),
        label = "icon_color"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) colors.textPrimary else colors.textSecondary,
        animationSpec = tween(durationMillis = 150),
        label = "text_color"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    bounded = true,
                    radius = 40.dp,
                    color = colors.primaryOrange.copy(alpha = 0.2f)
                )
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = iconColor
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor,
            maxLines = 1
        )
    }
}

// Data classes for better organization
private enum class BottomNavTab {
    HOME, MEALS, SHOPS
}

private data class BottomNavColors(
    val surface: Color,
    val primaryOrange: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color
)

// Optimized Extension Functions
fun NavController.navigateToMealDetails(mealId: String) {
    navigate(MealDetailsRoute(mealId)) {
        launchSingleTop = true
    }
}

fun NavController.navigateToShopMenu(shopId: String) {
    navigate(ShopMenuRoute(shopId)) {
        launchSingleTop = true
    }
}

fun NavController.navigateToMeals() {
    navigate(MealsRoute) {
        popUpTo<ShopsRoute> { inclusive = true }
        launchSingleTop = true
    }
}

fun NavController.navigateToShops() {
    navigate(ShopsRoute) {
        launchSingleTop = true
        restoreState = true
    }
}

fun NavController.navigateToNotifications() {
    navigate(NotificationRoute) {
        launchSingleTop = true
    }
}

fun NavController.navigateToAbout() {
    navigate(AboutRoute) {
        launchSingleTop = true
    }
}
// Budget Tab (using custom icon)
//                ModernNavItemCustomIcon(
//                    iconRes = R.drawable.moneybag,
//                    label = "Budget",
//                    isSelected = currentRoute == "budget",
//                    primaryColor = primaryOrange,
//                    primaryLight = primaryLight,
//                    textSecondary = textSecondaryColor,
//                    animationSpec = fastAnimationSpec,
//                    colorAnimationSpec = colorAnimationSpec,
//                    onClick = {
//                        if (currentRoute != "budget") {
//                            navController.navigate("budget") {
//                                popUpTo(navController.graph.findStartDestination().id) {
//                                    saveState = true
//                                }
//                                launchSingleTop = true
//                                restoreState = true
//                            }
//                        }
//                    }
//                )