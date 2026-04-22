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
import com.divyansh.cueats.HomeScreen.HomeScreen
import com.divyansh.cueats.Mess.MealDetailsScreen
import com.divyansh.cueats.Mess.ModernWeeklyMenuApp
import com.divyansh.cueats.ShopsScreen.ShopMenuDetailScreen
import com.divyansh.cueats.ShopsScreen.ShopMenuScreen
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.onesignal.OneSignal
import com.divyansh.cueats.Notification.NotificationPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.divyansh.cueats.Maps.EnhancedCampusMap
import com.divyansh.cueats.ProfileScreen.ProfileScreen
import kotlinx.coroutines.delay
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
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


        // Get Firebase Cloud Messaging Token and save it
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM Token", "FCM Token: $token")
                
                // Save token to Firestore
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val userId = FirebaseAuth.getInstance().currentUser?.uid
                        if (userId != null && token != null) {
                            val tokenData = hashMapOf(
                                "token" to token,
                                "device" to Build.MODEL,
                                "platform" to "android",
                                "lastUpdated" to System.currentTimeMillis()
                            )
                            
                            FirebaseFirestore.getInstance().collection("users")
                                .document(userId)
                                .collection("fcmTokens")
                                .document(token)
                                .set(tokenData)
                                .addOnSuccessListener {
                                    Log.d("FCM Token", "Token saved to Firestore successfully")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("FCM Token", "Error saving token to Firestore", e)
                                }
                        }
                    } catch (e: Exception) {
                        Log.e("FCM Token", "Error in token save", e)
                    }
                }
            } else {
                Log.e("FCM Token", "Failed to get FCM token", task.exception)
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

        // Initialize notification preferences for new users
        if (savedInstanceState == null) {  // Prevents re-execution on screen rotation
            initializeNotificationPreferences()
            setupFirebaseDishes()  // Setup Firebase dishes on first launch
        }

        // Handle deep links - extract event ID from intent
        val deepLinkEventId = intent?.data?.let { uri ->
            if (uri.scheme == "cueats" && uri.host == "event") {
                uri.lastPathSegment // Extract event ID from the URI path
            } else null
        }

        // IMPORTANT: Load the UI immediately with splash screen as start destination
        setContent {
            MealPlannerAppWithAuth(deepLinkEventId = deepLinkEventId)
        }
    }


    private fun initializeNotificationPreferences() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notificationPrefs = NotificationPreferences()
                notificationPrefs.initializeForNewUser()
                Log.d("MainActivity", "Notification preferences initialized")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error initializing notification preferences", e)
            }
        }
    }

    private fun setupFirebaseDishes() {
        val prefs = getSharedPreferences("app_setup", MODE_PRIVATE)
        val isSetupDone = prefs.getBoolean("dishes_setup_done", false)

        if (!isSetupDone) {
            Log.d("FirebaseSetup", "Setting up dishes for the first time...")
            com.divyansh.cueats.Mess.FirebaseSetup.setupSampleDishes { success ->
                if (success) {
                    prefs.edit().putBoolean("dishes_setup_done", true).apply()
                    Log.d("FirebaseSetup", "✅ Dishes setup completed successfully!")
                } else {
                    Log.e("FirebaseSetup", "❌ Dishes setup failed, will retry next time")
                }
            }
        } else {
            Log.d("FirebaseSetup", "Dishes already set up, skipping...")
        }
    }
}

@Composable
fun MealPlannerAppWithAuth(deepLinkEventId: String? = null) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState
    val systemTheme = isSystemInDarkTheme()

    val backgroundColor = if (systemTheme) Color(0xFF121212) else Color(0xFFF6F7FB)

    // Handle deep link navigation
    LaunchedEffect(deepLinkEventId, authState.isLoggedIn) {
        if (deepLinkEventId != null && authState.isLoggedIn) {
            // Wait a bit for the nav graph to be ready
            delay(1000)
            navController.navigate(EventDetailsRoute(deepLinkEventId)) {
                launchSingleTop = true
            }
        }
    }

    // Don't handle navigation automatically - let splash screen handle initial navigation
    // Only handle auth state changes after splash is done
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route

    LaunchedEffect(authState.isLoggedIn, authState.user,  currentDestination) {
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
                ShopMenuScreen(navController)
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

            composable<EventsRoute> {
                com.divyansh.cueats.AnnouncementScreen.EventsScreen(navController)
            }

            composable<EventDetailsRoute> { backStackEntry ->
                val eventDetailsRoute = backStackEntry.toRoute<EventDetailsRoute>()
                com.divyansh.cueats.AnnouncementScreen.EventDetailsScreen(
                    navController = navController,
                    eventId = eventDetailsRoute.eventId
                )
            }

            composable<EventRegistrationRoute> { backStackEntry ->
                val eventRegistrationRoute = backStackEntry.toRoute<EventRegistrationRoute>()
                com.divyansh.cueats.AnnouncementScreen.EventRegistrationScreen(
                    navController = navController,
                    eventId = eventRegistrationRoute.eventId,
                    eventTitle = eventRegistrationRoute.eventTitle
                )
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


// Modern Pill-Shaped Bottom Navigation with Glassmorphism

@Composable
fun AppBottomNavigation(navController: NavController, currentRoute: String?) {
    // Always use light theme colors
    val colors = remember {
        BottomNavColors(
            surface = Color(0xFFF5F5F5),
            primaryOrange = Color(0xFF42A5F5), // Lighter, vibrant blue
            textPrimary = Color(0xFF2D2D2D),
            textSecondary = Color(0xFF757575),
            divider = Color(0xFFE0E0E0)
        )
    }

    // Use derivedStateOf to only recompute when currentRoute actually changes
    val currentTab by remember {
        derivedStateOf {
            when {
                currentRoute?.contains("HomeRoute") == true ||
                        currentRoute == "home" -> BottomNavTab.HOME
                currentRoute?.contains("EventsRoute") == true ||
                        currentRoute?.contains("EventDetailsRoute") == true ||
                        currentRoute == "events" -> BottomNavTab.EVENTS
                currentRoute?.contains("MealsRoute") == true ||
                        currentRoute?.contains("MealDetailsRoute") == true ||
                        currentRoute == "meals" -> BottomNavTab.MEALS
                currentRoute?.contains("ShopsRoute") == true ||
                        currentRoute?.contains("ShopMenuRoute") == true ||
                        currentRoute == "shops" -> BottomNavTab.SHOPS
                else -> BottomNavTab.HOME
            }
        }
    }

    // Floating overlay container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Pill-shaped navigation container (always light theme)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            shape = RoundedCornerShape(34.dp), // Fully rounded pill shape
            color = Color.White,
            shadowElevation = 12.dp,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Tab
                ModernNavItem(
                    iconRes = R.drawable.home,
                    label = "Home",
                    isSelected = currentTab == BottomNavTab.HOME,
                    colors = colors,
                    onClick = {
                        if (currentTab == BottomNavTab.HOME) return@ModernNavItem
                        navController.navigate(HomeRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                // Events Tab
                ModernNavItem(
                    iconRes = R.drawable.events,
                    label = "Events",
                    isSelected = currentTab == BottomNavTab.EVENTS,
                    colors = colors,
                    onClick = {
                        if (currentTab == BottomNavTab.EVENTS) return@ModernNavItem
                        navController.navigate(EventsRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                // Meals Tab
                ModernNavItem(
                    iconRes = R.drawable.mess,
                    label = "Mess",
                    isSelected = currentTab == BottomNavTab.MEALS,
                    colors = colors,
                    onClick = {
                        if (currentTab == BottomNavTab.MEALS) return@ModernNavItem
                        navController.navigate(MealsRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                // Shops Tab
                ModernNavItem(
                    iconRes = R.drawable.shopsicon,
                    label = "Shops",
                    isSelected = currentTab == BottomNavTab.SHOPS,
                    colors = colors,
                    onClick = {
                        if (currentTab == BottomNavTab.SHOPS) return@ModernNavItem
                        navController.navigate(ShopsRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

// Modern navigation item with clean design and animations
@Composable
private fun ModernNavItem(
    iconRes: Int,
    label: String,
    isSelected: Boolean,
    colors: BottomNavColors,
    onClick: () -> Unit
) {
    // Animated colors for smooth transitions
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) colors.primaryOrange else colors.textSecondary,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "iconColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) colors.primaryOrange else colors.textSecondary,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "textColor"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) colors.primaryOrange.copy(alpha = 0.1f) else Color.Transparent,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "backgroundColor"
    )

    // Scale animation for selected state
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    // Bounce animation on tap
    var isTapped by remember { mutableStateOf(false) }
    val tapScale by animateFloatAsState(
        targetValue = if (isTapped) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tapScale"
    )

    // Dancing animation for selected icon
    val infiniteTransition = rememberInfiniteTransition(label = "dance")
    val danceY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isSelected) -3f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "danceY"
    )
    val danceRotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = if (isSelected) 5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "danceRotation"
    )

    // Get dancing icon for each tab (use custom drawable icons)
    val dancingIconRes = when (label) {
        "Home" -> R.drawable.house  // Replace with your custom dancing home icon
        "Events" -> R.drawable.party  // Replace with your custom dancing events icon
        "Mess" -> R.drawable.spoonandfork  // Replace with your custom dancing mess icon
        "Shops" -> R.drawable.shopping // Replace with your custom dancing shops icon
        else -> iconRes
    }

    LaunchedEffect(isTapped) {
        if (isTapped) {
            delay(150)
            isTapped = false
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isTapped = true
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .scale(scale * tapScale)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .offset(y = danceY.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                // Dancing icon when selected (using drawable with original colors)
                Icon(
                    painter = painterResource(id = dancingIconRes),
                    contentDescription = label,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(danceRotation),
                    tint = Color.Unspecified  // No tint - show original icon colors
                )
            } else {
                // Regular icon when not selected
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    modifier = Modifier.size(22.dp),
                    tint = iconColor
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Visible
        )
    }
}



// Data classes for better organization
private enum class BottomNavTab {
    HOME, EVENTS, MEALS, SHOPS
}

// @Stable annotation helps Compose skip recompositions when values haven't changed
@Stable
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