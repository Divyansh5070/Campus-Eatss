package com.divyansh.cueats.HomeScreen

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModelProvider
import com.divyansh.cueats.AppBottomNavigation
import com.divyansh.cueats.LoginScreen.AuthViewModel
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material3.placeholder
import com.google.accompanist.placeholder.material3.shimmer
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.divyansh.cueats.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.scale
import com.divyansh.cueats.AboutRoute
import com.divyansh.cueats.LoginRoute
import com.divyansh.cueats.Notification.NotificationIconWithBadge
import com.divyansh.cueats.Notification.NotificationViewModel
import com.divyansh.cueats.NotificationRoute
import com.divyansh.cueats.widget.MealsWidgetUpdater
import com.google.common.reflect.TypeToken
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch



@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ModernWeeklyMenuApp(
    navController: NavController,
    authViewModel: AuthViewModel? = null,
    notificationViewModel: NotificationViewModel = viewModel() // Add NotificationViewModel
) {
    val context = LocalContext.current
    val viewModel: MealViewModel = viewModel(
        factory = MealViewModelFactory(context)
    )

    val systemTheme = isSystemInDarkTheme()

    // Colors based on theme
    val primaryOrange = Color(0xFFFF6B01)
    val lightBackground = Color(0xFFF6F7FB)
    val darkBackground = Color(0xFF121212)
    val surfaceColor = if (systemTheme) Color(0xFF202020) else Color.White
    val textColor = if (systemTheme) Color.White else Color.Black
    val textSecondaryColor = if (systemTheme) Color.LightGray else Color.Gray
    val dividerColor = if (systemTheme) Color(0xFF303030) else Color(0xFFEEEEEE)
    val systemUiController = rememberSystemUiController()

    // Observe notification count
    val unreadNotificationCount by notificationViewModel.unreadCount.observeAsState(0)

    // State for showing logout dialog
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Fetch notifications when the app starts
    LaunchedEffect(Unit) {
        notificationViewModel.fetchNotifications()
    }

    // Set up periodic notification refresh (every 5 minutes)
    LaunchedEffect(Unit) {
        while (true) {
            delay(100000) // 1 minutes
            notificationViewModel.fetchNotifications()
        }
    }

    SideEffect {
        systemUiController.setStatusBarColor(color = primaryOrange)
    }

    // Function to find today's index in our days list
    fun findTodayIndex(days: List<DayData>): Int {
        if (days.isEmpty()) return 0

        val calendar = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val todayString = dayFormat.format(calendar.time)

        return try {
            val exactMatch = days.indexOfFirst { it.date == todayString }
            if (exactMatch >= 0) {
                exactMatch
            } else {
                // Find closest future date
                val today = dayFormat.parse(todayString)?.time ?: return 0
                days.indexOfFirst { dayData ->
                    try {
                        val dayDate = dayFormat.parse(dayData.date)?.time ?: return@indexOfFirst false
                        dayDate >= today
                    } catch (e: Exception) {
                        false
                    }
                }.takeIf { it >= 0 } ?: 0
            }
        } catch (e: Exception) {
            Log.e("DateParsing", "Error finding today's index: ${e.message}")
            0
        }
    }

    val mealViewModel: MealViewModel = viewModel()
    val daysData by mealViewModel.weeklyMenuData.observeAsState(emptyList())
    var selectedDayIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(daysData.size) {
        if (daysData.isNotEmpty() && selectedDayIndex == 0) {
            selectedDayIndex = findTodayIndex(daysData)
        }
    }

    LaunchedEffect(Unit) {
        try {
            systemUiController.setStatusBarColor(color = primaryOrange)
            systemUiController.setNavigationBarColor(
                color = if (systemTheme) darkBackground else lightBackground,
                darkIcons = !systemTheme
            )
        } catch (e: Exception) {
            Log.e("SystemUI", "Error setting system UI colors: ${e.message}")
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Sign Out",
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out?",
                    color = textSecondaryColor
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel?.signOut()
                        navController.navigate(LoginRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text("Sign Out", color = primaryOrange)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Cancel", color = textSecondaryColor)
                }
            },
            containerColor = surfaceColor
        )
    }

    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Box(
//                            modifier = Modifier
//                                .size(32.dp)
//                                .clip(CircleShape)
//                                .background(Color.White.copy(alpha = 0.3f)),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Text(
//                                text = "🍽️",
//                                fontSize = 16.sp,
//                                color = Color.White
//                            )
//                        }
//                        Spacer(modifier = Modifier.width(12.dp))
//                        Text(
//                            "Campus Eats",
//                            fontSize = 22.sp,
//                            fontWeight = FontWeight.Bold,
//                            fontFamily = playfairFont
//                        )
//                    }
//                },
//                actions = {
//                    // UPDATED: Notification icon with badge
//                    NotificationIconWithBadge(
//                        unreadCount = unreadNotificationCount,
//                        onClick = {
//                            try {
//                                val currentRoute = navController.currentBackStackEntry?.destination?.route
//                                if (currentRoute != "NotificationRoute") {
//                                    navController.navigate(NotificationRoute) {
//                                        launchSingleTop = true
//                                    }
//                                }
//                            } catch (e: Exception) {
//                                Log.e("Navigation", "Error navigating to notification: ${e.message}")
//                            }
//                        }
//                    )
//
//                    // About icon
//                    IconButton(
//                        onClick = {
//                            try {
//                                navController.navigate(AboutRoute) {
//                                    launchSingleTop = true
//                                }
//                            } catch (e: Exception) {
//                                Log.e("Navigation", "Error navigating to about: ${e.message}")
//                            }
//                        }
//                    ) {
//                        Box(
//                            modifier = Modifier
//                                .size(50.dp)
//                                .clip(CircleShape),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Icon(
//                                imageVector = Icons.Default.Info,
//                                contentDescription = "About",
//                                tint = Color.White,
//                                modifier = Modifier.size(22.dp)
//                            )
//                        }
//                    }
//
//                    // Logout icon
//                    if (authViewModel != null) {
//                        IconButton(
//                            onClick = { showLogoutDialog = true }
//                        ) {
//                            Box(
//                                modifier = Modifier
//                                    .size(50.dp)
//                                    .clip(CircleShape),
//                                contentAlignment = Alignment.Center
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Default.ExitToApp,
//                                    contentDescription = "Sign Out",
//                                    tint = Color.White,
//                                    modifier = Modifier.size(22.dp)
//                                )
//                            }
//                        }
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = primaryOrange,
//                    titleContentColor = Color.White
//                )
//            )
//        },
        bottomBar = {
            AppBottomNavigation(
                navController = navController,
                currentRoute = "meals"
            )
        },
        containerColor = if (systemTheme) darkBackground else lightBackground
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(if (systemTheme) darkBackground else lightBackground)
        ) {
            if (daysData.isNotEmpty()) {
                // OPTIMIZED: Simplified Calendar Day Selector - removed heavy animations
                val lazyRowState = rememberLazyListState()
                val coroutineScope = rememberCoroutineScope()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    LazyRow(
                        state = lazyRowState,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        // PERFORMANCE: Enable fling behavior for smoother scrolling
                        flingBehavior = ScrollableDefaults.flingBehavior()
                    ) {
                        itemsIndexed(
                            items = daysData,
                            key = { index, dayData -> "${dayData.date}_$index" }
                        ) { index, dayData ->
                            val dayName = dayData.day.take(3)
                            val isSelected = index == selectedDayIndex

                            val (dayDate, monthAbbrev) = try {
                                val inputFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                val date = inputFormatter.parse(dayData.date)

                                val day = SimpleDateFormat("dd", Locale.getDefault()).format(date as Date)
                                val month = SimpleDateFormat("MMM", Locale.getDefault()).format(date)

                                day to month
                            } catch (e: Exception) {
                                "??" to "???"
                            }

                            // OPTIMIZED: Removed heavy animations, using simple background color change
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) primaryOrange else Color.Transparent)
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        selectedDayIndex = index
                                        // OPTIMIZED: Simple scroll without animation for better performance
                                        coroutineScope.launch {
                                            try {
                                                lazyRowState.scrollToItem(
                                                    index = maxOf(0, index - 2),
                                                    scrollOffset = 0
                                                )
                                            } catch (e: Exception) {
                                                Log.e("ScrollError", "Error scrolling: ${e.message}")
                                            }
                                        }
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = dayName,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else textSecondaryColor,
                                    fontFamily = playfairFont,
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dayDate,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else textColor,
                                    fontFamily = poppinsFont
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = monthAbbrev,
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.9f) else textSecondaryColor,
                                    fontFamily = playfairFont
                                )
                            }
                        }
                    }
                }

                // OPTIMIZED: Removed auto-scroll animation for better performance
                LaunchedEffect(selectedDayIndex, daysData.size) {
                    if (daysData.isNotEmpty() && selectedDayIndex in 0 until daysData.size) {
                        coroutineScope.launch {
                            try {
                                lazyRowState.scrollToItem(
                                    index = maxOf(0, selectedDayIndex - 2),
                                    scrollOffset = 0
                                )
                            } catch (e: Exception) {
                                Log.e("ScrollError", "Error scrolling: ${e.message}")
                            }
                        }
                    }
                }

                // PERFORMANCE OPTIMIZED: Meals LazyColumn with maximum performance settings
                if (selectedDayIndex in daysData.indices) {
                    val selectedDay = daysData[selectedDayIndex]

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        // PERFORMANCE: Optimize for high refresh rate scrolling
                        flingBehavior = ScrollableDefaults.flingBehavior(),
                        // PERFORMANCE: Enable item reuse for better memory management
                        userScrollEnabled = true
                    ) {
                        val groupedMeals = selectedDay.meals.groupBy { it.type }

                        groupedMeals.forEach { (mealType, meals) ->
                            item(key = "${selectedDay.date}_$mealType") {
                                // OPTIMIZED: Removed all entrance animations for smooth 120fps scrolling
                                MealTypeCard(
                                    mealType = mealType,
                                    meals = meals,
                                    navController = navController,
                                    isDarkTheme = systemTheme,
                                    surfaceColor = surfaceColor,
                                    textColor = textColor,
                                    textSecondaryColor = textSecondaryColor,
                                    dividerColor = dividerColor,
                                    mealViewModel = mealViewModel,
                                    selectedDate = selectedDay.date
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            } else {
                // OPTIMIZED: Simplified loading placeholders without shimmer animations
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .background(if (systemTheme) darkBackground else lightBackground),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Date selection row at top
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (systemTheme) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(5) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (systemTheme) Color(0xFF303030) else Color.LightGray)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .height(20.dp)
                                            .width(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .height(24.dp)
                                            .width(30.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .height(16.dp)
                                            .width(30.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Meal section placeholders
                    repeat(2) { mealIndex ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (systemTheme) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // Title and time row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Meal icon
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(if (systemTheme) Color(0xFF303030) else Color.LightGray)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        // Meal title
                                        Box(
                                            modifier = Modifier
                                                .height(24.dp)
                                                .width(if (mealIndex == 0) 120.dp else 90.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (systemTheme) Color(0xFF303030) else Color.LightGray)
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Time
                                        Box(
                                            modifier = Modifier
                                                .height(16.dp)
                                                .width(160.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (systemTheme) Color(0xFF303030) else Color.LightGray)
                                        )
                                    }

                                    // Rating
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (systemTheme) Color(0xFF303030) else Color.LightGray)
                                    )
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                // Menu items
                                FlowRow(
                                    maxItemsInEachRow = 2,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    repeat(if (mealIndex == 0) 4 else 6) {
                                        Box(
                                            modifier = Modifier
                                                .height(40.dp)
                                                .weight(1f)
                                                .clip(RoundedCornerShape(24.dp))
                                                .background(if (systemTheme) Color(0xFF303030) else Color.LightGray)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
// Replace your existing MealTypeCard function with this simplified version
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealTypeCard(
    mealType: String,
    meals: List<Meal>,
    navController: NavController,
    isDarkTheme: Boolean,
    surfaceColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    dividerColor: Color,
    mealViewModel: MealViewModel? = null,
    selectedDate: String = ""
) {
    // UPDATED: Added South Indian Dinner to the meal icon mapping
    val mealIcon = when (mealType.lowercase()) {
        "breakfast" -> "🍳"
        "lunch" -> "🍛"
        "snacks" -> "🧁"
        "dinner" -> "🍽️"
        "south indian dinner" -> "🥘" // NEW: Curry bowl icon
        else -> "🍴"
    }

    // UPDATED: Added South Indian Dinner to color scheme
    val (accentColor, subtleColor) = when (mealType.lowercase()) {
        "breakfast" -> Pair(
            Color(0xFFFFB74D),
            if (isDarkTheme) Color(0xFFE89E76).copy(alpha = 0.7f) else Color(0xFFFFF8E1).copy(alpha = 0.5f)
        )
        "lunch" -> Pair(
            Color(0xFF64B5F6),
            if (isDarkTheme) Color(0xFF95B7D9).copy(alpha = 0.7f) else Color(0xFFE1F5FE).copy(alpha = 0.5f)
        )
        "snacks" -> Pair(
            Color(0xFFBA68C8),
            if (isDarkTheme) Color(0xFFD1B4EC).copy(alpha = 0.7f) else Color(0xFFF3E5F5).copy(alpha = 0.5f)
        )
        "dinner" -> Pair(
            Color(0xFF81C784),
            if (isDarkTheme) Color(0xFFA3C9A5).copy(alpha = 0.7f) else Color(0xFFE8F5E9).copy(alpha = 0.5f)
        )
        "south indian dinner" -> Pair(  // NEW: Orange-red theme for South Indian
            Color(0xFFFF7043),
            if (isDarkTheme) Color(0xFFB8A699).copy(alpha = 0.7f) else Color(0xFFFFF3E0).copy(alpha = 0.5f)
        )
        else -> Pair(
            Color(0xFFFF8A65),
            if (isDarkTheme) Color(0xFF3D2200).copy(alpha = 0.7f) else Color(0xFFFBE9E7).copy(alpha = 0.5f)
        )
    }

    // Rest of the MealTypeCard code remains the same...
    // Generate a list of unique food items
    val allItems = mutableListOf<String>()
    meals.forEach { meal ->
        allItems.addAll(meal.items)
        allItems.addAll(meal.commonItems)
        meal.hostels.forEach { (_, hostelItems) ->
            allItems.addAll(hostelItems)
        }
    }
    val uniqueItems = allItems.distinct().sorted()

    // Get timing - SAFE: Will fallback to default timing for South Indian Dinner
    val timeRange = when {
        meals.isNotEmpty() && meals.first().getActualTiming().isNotBlank() -> meals.first().getActualTiming()
        mealViewModel != null -> mealViewModel.getTimingForMeal(mealType)
        else -> getDefaultTimingForMeal(mealType)
    }

    // FIXED: Simplified observeAsState usage with null safety
    val mealLikes = mealViewModel?.mealLikes?.observeAsState(emptyMap())?.value ?: emptyMap()
    val userLikes = mealViewModel?.userLikes?.observeAsState(emptyMap())?.value ?: emptyMap()
    val votingInProgress = mealViewModel?.votingInProgress?.observeAsState(emptySet())?.value ?: emptySet()

    // Get current values for this specific meal
    val mealKey = "${mealType}_$selectedDate"
    val currentCounts = mealLikes[mealKey] ?: Pair(0, 0)
    val likesCount = currentCounts.first
    val dislikesCount = currentCounts.second
    val userVoteStatus = userLikes[mealKey]
    val isCurrentlyVoting = votingInProgress.contains(mealKey)
    val isVotingActive = mealViewModel?.isFeedbackTimeActive(mealType) ?: false

    // State for user feedback
    val context = LocalContext.current
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(subtleColor)
                            .border(width = 2.dp, color = accentColor, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = mealIcon, fontSize = 24.sp)
                    }

                    // Meal info
                    Column(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = mealType.capitalize(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            fontFamily = poppinsFont
                        )
                        Text(
                            text = timeRange.ifEmpty { "Time not available" },
                            fontSize = 14.sp,
                            color = textSecondaryColor,
                            fontFamily = playfairFont
                        )
                    }
                }

                // Divider with enhanced gradient
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.1f),
                                    accentColor.copy(alpha = 0.5f),
                                    accentColor.copy(alpha = 0.1f)
                                )
                            )
                        )
                )

                // Menu items with enhanced layout
                FlowRow(
                    maxItemsInEachRow = 3,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uniqueItems.forEach { item ->
                        ImprovedFoodItemChip(
                            text = item,
                            accentColor = accentColor,
                            backgroundColor = subtleColor,
                            textColor = if (isDarkTheme) Color.White.copy(alpha = 0.9f) else textColor,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }

                // Total votes indicator (optional)
                if (mealViewModel != null && (likesCount > 0 || dislikesCount > 0)) {
                    val totalVotes = likesCount + dislikesCount
                    val likePercentage = if (totalVotes > 0) (likesCount.toFloat() / totalVotes * 100).toInt() else 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$totalVotes votes • $likePercentage% liked",
                            fontSize = 11.sp,
                            color = textSecondaryColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Show snackbar for user feedback
    if (showSnackbar) {
        LaunchedEffect(snackbarMessage) {
            // You can implement your snackbar logic here
            Log.d("MealVote", snackbarMessage)
            delay(2000)
            showSnackbar = false
        }
    }
}


@Composable
private fun getDefaultTimingForMeal(mealType: String): String {
    val isWeekend = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)
    return if (isWeekend) {
        when (mealType.lowercase()) {
            "breakfast" -> "8:00 AM - 9:30 AM"
            "lunch" -> "12:30 PM - 2:00 PM"
            "snacks" -> "4:30 PM - 5:15 PM"
            "dinner" -> "7:30 PM - 9:00 PM"
            "south indian dinner" -> "7:30 PM - 9:00 PM" // NEW
            else -> ""
        }
    } else {
        when (mealType.lowercase()) {
            "breakfast" -> "7:30 AM - 9:00 AM"
            "lunch" -> "12:00 PM - 1:45 PM"
            "snacks" -> "4:30 PM - 5:15 PM"
            "dinner" -> "7:30 PM - 9:00 PM"
            "south indian dinner" -> "7:30 PM - 9:00 PM" // NEW
            else -> ""
        }
    }
}




// Extension function to capitalize first letter of a string (if missing)
fun String.capitalize(): String {
    return this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault())
        else it.toString()
    }
}


@Composable
fun ImprovedFoodItemChip(
    text: String,
    accentColor: Color,
    backgroundColor: Color,
    textColor: Color,
    isDarkTheme: Boolean
) {
    // Improved background for dark theme to enhance visibility
    val chipBackground = if (isDarkTheme) {
        // In dark theme, use a slightly brighter background
        backgroundColor.copy(alpha = 0.8f)
    } else {
        backgroundColor
    }

    // Add subtle highlight for better visibility in dark theme
    val borderColor = if (isDarkTheme) {
        accentColor.copy(alpha = 0.7f) // More visible border in dark theme
    } else {
        accentColor.copy(alpha = 0.3f)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = chipBackground,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.padding(end = 4.dp, bottom = 4.dp),
        shadowElevation = if (isDarkTheme) 2.dp else 0.dp // Subtle elevation in dark theme
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Small colored dot indicator - more visible in dark mode
            if (isDarkTheme) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                        .padding(end = 8.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = if (isDarkTheme) FontWeight.SemiBold else FontWeight.Medium, // Bolder in dark theme
                color = textColor,
                fontFamily = playfairFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = if (isDarkTheme) 0.3.sp else 0.sp // Slightly increased spacing for dark theme
            )
        }
    }
}

class MealViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MealViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MealViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Replace your existing MealViewModel like/dislike related code with this simplified version
class MealViewModel(private val context: Context) : ViewModel() {
    // Keep ALL your existing properties
    private val _weeklyMenuData = MutableLiveData<List<DayData>>()
    val weeklyMenuData: LiveData<List<DayData>> get() = _weeklyMenuData

    // FIXED like/dislike state with proper initialization
    private val _mealLikes = MutableLiveData<Map<String, Pair<Int, Int>>>()
    val mealLikes: LiveData<Map<String, Pair<Int, Int>>> get() = _mealLikes

    private val _userLikes = MutableLiveData<Map<String, Boolean?>>()
    val userLikes: LiveData<Map<String, Boolean?>> get() = _userLikes

    // Add loading state for individual votes
    private val _votingInProgress = MutableLiveData<Set<String>>()
    val votingInProgress: LiveData<Set<String>> get() = _votingInProgress

    // Keep your existing properties
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance() // FIXED: Use getInstance()

    private val _mealTimings = MutableLiveData<Map<String, String>>()
    val mealTimings: LiveData<Map<String, String>> get() = _mealTimings

    private val sharedPrefs = context.getSharedPreferences("meal_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    // Store listener reference for cleanup
    private var databaseListener: ValueEventListener? = null
    private var databaseReference: DatabaseReference? = null

    // Real-time listeners for likes
    private var likesListener: ListenerRegistration? = null
    private var userVotesListener: ListenerRegistration? = null

    private companion object {
        const val CACHE_KEY_MEAL_DATA = "cached_meal_data"
        const val CACHE_KEY_MEAL_TIMINGS = "cached_meal_timings"
        const val CACHE_KEY_TIMESTAMP = "cache_timestamp"
        const val CACHE_DURATION_HOURS = 11
        const val CACHE_DURATION_MS = CACHE_DURATION_HOURS * 60 * 60 * 1000L
    }

    init {
        // FIXED: Initialize empty states first
        _mealLikes.value = emptyMap()
        _userLikes.value = emptyMap()
        _votingInProgress.value = emptySet()
        _mealTimings.value = emptyMap()

        loadMealDataWithCache()
        setupRealTimeLikesListener()
    }

    override fun onCleared() {
        super.onCleared()
        databaseListener?.let { listener ->
            databaseReference?.removeEventListener(listener)
        }
        // Clean up real-time listeners
        likesListener?.remove()
        userVotesListener?.remove()
    }



    // FIXED Real-time likes listener with null safety
    private fun setupRealTimeLikesListener() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("MealLikes", "User not authenticated, skipping real-time listeners")
            return
        }

        // Listen to user's votes in real-time
        userVotesListener = firestore.collection("userVotes")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MealLikes", "Error listening to user votes: ${error.message}")
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    val userVotes = mutableMapOf<String, Boolean?>()
                    querySnapshot.documents.forEach { doc ->
                        val vote = doc.data
                        if (vote != null) {
                            val mealKey = "${vote["mealType"]}_${vote["date"]}"
                            userVotes[mealKey] = vote["isLike"] as? Boolean
                        }
                    }
                    _userLikes.value = userVotes
                    Log.d("MealLikes", "Real-time user votes updated: ${userVotes.size} votes")
                }
            }

        // Listen to vote counts in real-time
        likesListener = firestore.collection("voteCounts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MealLikes", "Error listening to vote counts: ${error.message}")
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    val voteCounts = mutableMapOf<String, Pair<Int, Int>>()
                    querySnapshot.documents.forEach { doc ->
                        val data = doc.data
                        if (data != null) {
                            val mealKey = doc.id
                            val likes = (data["likes"] as? Long)?.toInt() ?: 0
                            val dislikes = (data["dislikes"] as? Long)?.toInt() ?: 0
                            voteCounts[mealKey] = Pair(likes, dislikes)
                        }
                    }
                    _mealLikes.value = voteCounts
                    Log.d("MealLikes", "Real-time vote counts updated: ${voteCounts.size} meals")
                }
            }
    }

    // FIXED submit function with proper null checks
    fun submitLikeDislike(
        mealType: String,
        date: String,
        isLike: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onError("Please login to vote")
            return
        }

        val mealKey = "${mealType}_$date"

        // Prevent double-clicking
        val currentVotingInProgress = _votingInProgress.value ?: emptySet()
        if (currentVotingInProgress.contains(mealKey)) {
            Log.d("MealLikes", "Vote already in progress for $mealKey")
            return
        }

        // Mark as voting in progress
        _votingInProgress.value = currentVotingInProgress + mealKey

        val userVoteId = "${userId}_${mealKey}"
        val currentUserStatus = _userLikes.value?.get(mealKey)

        // IMMEDIATE UI UPDATE for responsiveness
        updateUIImmediately(mealKey, currentUserStatus, isLike)

        // If user clicked same button, remove vote
        if (currentUserStatus == isLike) {
            removeVoteOptimized(userVoteId, mealKey, currentUserStatus, onSuccess, onError)
        } else {
            // Add or change vote
            addOrChangeVoteOptimized(userVoteId, mealKey, userId, mealType, date, isLike, currentUserStatus, onSuccess, onError)
        }
    }



    // FIXED UI update with null safety
    private fun updateUIImmediately(mealKey: String, previousStatus: Boolean?, newStatus: Boolean?) {
        // Update user status immediately
        val newUserLikes = _userLikes.value?.toMutableMap() ?: mutableMapOf()

        if (previousStatus == newStatus) {
            // User is removing their vote
            newUserLikes[mealKey] = null
        } else {
            // User is adding/changing vote
            newUserLikes[mealKey] = newStatus
        }
        _userLikes.value = newUserLikes

        // Update counts immediately for UI responsiveness
        val currentCounts = _mealLikes.value?.get(mealKey) ?: Pair(0, 0)
        var (likes, dislikes) = currentCounts

        // Adjust counts based on status change
        when (previousStatus) {
            true -> likes-- // Remove previous like
            false -> dislikes-- // Remove previous dislike
            null -> { /* No previous vote */ }
        }

        when (newStatus) {
            true -> likes++ // Add new like
            false -> dislikes++ // Add new dislike
            null -> { /* Vote removed */ }
        }

        // If removing same vote
        if (previousStatus == newStatus) {
            when (previousStatus) {
                true -> likes--
                false -> dislikes--
                else -> {}
            }
        }

        // Ensure counts don't go negative
        likes = maxOf(0, likes)
        dislikes = maxOf(0, dislikes)

        // Update UI immediately
        val newCounts = _mealLikes.value?.toMutableMap() ?: mutableMapOf()
        newCounts[mealKey] = Pair(likes, dislikes)
        _mealLikes.value = newCounts

        Log.d("MealLikes", "UI updated immediately for $mealKey: likes=$likes, dislikes=$dislikes")
    }

    // FIXED remove vote with proper error handling
    private fun removeVoteOptimized(
        userVoteId: String,
        mealKey: String,
        currentUserStatus: Boolean?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val batch = firestore.batch()

        try {
            // Remove user vote
            val userVoteRef = firestore.collection("userVotes").document(userVoteId)
            batch.delete(userVoteRef)

            // Update vote count
            val currentCounts = _mealLikes.value?.get(mealKey) ?: Pair(0, 0)
            var (likes, dislikes) = currentCounts

            when (currentUserStatus) {
                true -> likes = maxOf(0, likes - 1)
                false -> dislikes = maxOf(0, dislikes - 1)
                else -> {}
            }

            val countRef = firestore.collection("voteCounts").document(mealKey)
            val countData = hashMapOf(
                "likes" to likes,
                "dislikes" to dislikes,
                "lastUpdated" to System.currentTimeMillis()
            )
            batch.set(countRef, countData)

            // Execute batch
            batch.commit()
                .addOnSuccessListener {
                    removeFromVotingProgress(mealKey)
                    onSuccess()
                    Log.d("MealLikes", "Vote removed successfully for $mealKey")
                }
                .addOnFailureListener { e ->
                    // Revert UI changes on failure
                    revertUIChanges(mealKey, currentUserStatus, null)
                    removeFromVotingProgress(mealKey)
                    Log.e("MealLikes", "Failed to remove vote: ${e.message}")
                    onError("Failed to remove vote. Please try again.")
                }
        } catch (e: Exception) {
            removeFromVotingProgress(mealKey)
            revertUIChanges(mealKey, currentUserStatus, null)
            Log.e("MealLikes", "Error in removeVoteOptimized: ${e.message}")
            onError("An error occurred. Please try again.")
        }
    }

    // FIXED add/change vote with proper error handling
    private fun addOrChangeVoteOptimized(
        userVoteId: String,
        mealKey: String,
        userId: String,
        mealType: String,
        date: String,
        isLike: Boolean,
        previousStatus: Boolean?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val batch = firestore.batch()

        try {
            // Add/update user vote
            val voteData = hashMapOf(
                "userId" to userId,
                "mealType" to mealType,
                "date" to date,
                "isLike" to isLike,
                "timestamp" to System.currentTimeMillis()
            )
            val userVoteRef = firestore.collection("userVotes").document(userVoteId)
            batch.set(userVoteRef, voteData)

            // Calculate new counts
            val currentCounts = _mealLikes.value?.get(mealKey) ?: Pair(0, 0)
            var (likes, dislikes) = currentCounts

            // Remove previous vote count
            when (previousStatus) {
                true -> likes = maxOf(0, likes - 1)
                false -> dislikes = maxOf(0, dislikes - 1)
                else -> {}
            }

            // Add new vote count
            when (isLike) {
                true -> likes++
                false -> dislikes++
            }

            val countRef = firestore.collection("voteCounts").document(mealKey)
            val countData = hashMapOf(
                "likes" to likes,
                "dislikes" to dislikes,
                "lastUpdated" to System.currentTimeMillis()
            )
            batch.set(countRef, countData)

            // Execute batch
            batch.commit()
                .addOnSuccessListener {
                    removeFromVotingProgress(mealKey)
                    onSuccess()
                    Log.d("MealLikes", "Vote ${if (isLike) "like" else "dislike"} saved successfully for $mealKey")
                }
                .addOnFailureListener { e ->
                    // Revert UI changes on failure
                    revertUIChanges(mealKey, isLike, previousStatus)
                    removeFromVotingProgress(mealKey)
                    Log.e("MealLikes", "Failed to save vote: ${e.message}")
                    onError("Failed to save vote. Please try again.")
                }
        } catch (e: Exception) {
            removeFromVotingProgress(mealKey)
            revertUIChanges(mealKey, isLike, previousStatus)
            Log.e("MealLikes", "Error in addOrChangeVoteOptimized: ${e.message}")
            onError("An error occurred. Please try again.")
        }
    }

    // Helper to revert UI changes on failure
    private fun revertUIChanges(mealKey: String, currentStatus: Boolean?, previousStatus: Boolean?) {
        try {
            // Revert user status
            val userLikes = _userLikes.value?.toMutableMap() ?: mutableMapOf()
            userLikes[mealKey] = previousStatus
            _userLikes.value = userLikes

            // Revert counts
            val currentCounts = _mealLikes.value?.get(mealKey) ?: Pair(0, 0)
            var (likes, dislikes) = currentCounts

            // Undo the immediate changes
            when (currentStatus) {
                true -> likes = maxOf(0, likes - 1)
                false -> dislikes = maxOf(0, dislikes - 1)
                null -> { /* Was removing vote, add it back */ }
            }

            when (previousStatus) {
                true -> likes++
                false -> dislikes++
                null -> { /* No previous vote */ }
            }

            val newCounts = _mealLikes.value?.toMutableMap() ?: mutableMapOf()
            newCounts[mealKey] = Pair(likes, dislikes)
            _mealLikes.value = newCounts

            Log.d("MealLikes", "UI changes reverted for $mealKey")
        } catch (e: Exception) {
            Log.e("MealLikes", "Error reverting UI changes: ${e.message}")
        }
    }

    // Helper to remove from voting progress
    private fun removeFromVotingProgress(mealKey: String) {
        try {
            val currentProgress = _votingInProgress.value ?: emptySet()
            _votingInProgress.value = currentProgress - mealKey
        } catch (e: Exception) {
            Log.e("MealLikes", "Error removing from voting progress: ${e.message}")
        }
    }

    // Simple getter functions
    fun getLikesCount(mealType: String, date: String): Int {
        val mealKey = "${mealType}_$date"
        return _mealLikes.value?.get(mealKey)?.first ?: 0
    }

    fun getDislikesCount(mealType: String, date: String): Int {
        val mealKey = "${mealType}_$date"
        return _mealLikes.value?.get(mealKey)?.second ?: 0
    }

    fun getUserVoteStatus(mealType: String, date: String): Boolean? {
        val mealKey = "${mealType}_$date"
        return _userLikes.value?.get(mealKey)
    }

    // Check if voting is in progress for a meal
    fun isVotingInProgress(mealType: String, date: String): Boolean {
        val mealKey = "${mealType}_$date"
        return _votingInProgress.value?.contains(mealKey) ?: false
    }

    // Time checking function
    fun isFeedbackTimeActive(mealType: String): Boolean {
        val timing = _mealTimings.value?.get(mealType) ?: getDefaultTiming(mealType)

        if (timing.isEmpty()) {
            Log.w("TimeCheck", "No timing found for $mealType")
            return false
        }

        return try {
            val timeParts = timing.split(" - ")
            if (timeParts.size != 2) {
                Log.e("TimeCheck", "Invalid timing format: $timing")
                return false
            }

            val startTime = parseTimeToMinutes(timeParts[0].trim())
            val endTime = parseTimeToMinutes(timeParts[1].trim())

            if (startTime == -1 || endTime == -1) {
                return false
            }

            val currentTime = Calendar.getInstance()
            val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
            val currentMinute = currentTime.get(Calendar.MINUTE)
            val currentTimeInMinutes = currentHour * 60 + currentMinute

            val isActive = currentTimeInMinutes in startTime..endTime
            Log.d("TimeCheck", "Meal: $mealType, IsActive: $isActive")
            isActive
        } catch (e: Exception) {
            Log.e("TimeCheck", "Error checking feedback time: ${e.message}")
            false
        }
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val time = timeFormat.parse(timeStr) ?: return -1
            val calendar = Calendar.getInstance()
            calendar.time = time
            calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        } catch (e: Exception) {
            Log.e("TimeCheck", "Error parsing time: ${e.message}")
            -1
        }
    }

    private fun getDefaultTiming(mealType: String): String {
        val isWeekend = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)
        return if (isWeekend) {
            when (mealType.lowercase()) {
                "breakfast" -> "8:00 AM - 9:30 AM"
                "lunch" -> "12:30 PM - 2:00 PM"
                "snacks" -> "4:30 PM - 5:15 PM"
                "dinner" -> "7:30 PM - 9:00 PM"
                "south indian dinner" -> "7:30 PM - 9:00 PM" // NEW: Slightly later timing
                else -> ""
            }
        } else {
            when (mealType.lowercase()) {
                "breakfast" -> "7:30 AM - 9:00 AM"
                "lunch" -> "12:00 PM - 1:45 PM"
                "snacks" -> "4:30 PM - 5:15 PM"
                "dinner" -> "7:30 PM - 9:00 PM"
                "south indian dinner" -> "7:30 PM - 9:00 PM" // NEW: Slightly later timing
                else -> ""
            }
        }
    }

    fun getTimingForMeal(mealType: String): String {
        return _mealTimings.value?.get(mealType) ?: getDefaultTiming(mealType)
    }

    // Keep all your existing meal data functions
    private fun fetchDailyMenuFromServer() {
        _isLoading.value = true
        databaseReference =
            FirebaseDatabase.getInstance("https://cu-eats-37fa0-default-rtdb.firebaseio.com/")
                .reference.child("meals")

        databaseListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FirebaseData", "Snapshot value: ${snapshot.value}")
                _isLoading.value = false

                if (snapshot.exists()) {
                    // UPDATED: Added South Indian Dinner to the meal order
                    val mealOrder = listOf("Breakfast", "Lunch", "Snacks", "Dinner", "South Indian Dinner")
                    val dayOrder = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    val timingsMap = mutableMapOf<String, String>()
                    val allDays = mutableListOf<DayData>()

                    snapshot.children.forEach { weekSnapshot ->
                        Log.d("FirebaseData", "Processing Week: ${weekSnapshot.key}")

                        dayOrder.forEach { dayName ->
                            val daySnapshot = weekSnapshot.child(dayName)
                            if (daySnapshot.exists()) {
                                val dateStr = daySnapshot.child("date").getValue(String::class.java)
                                    ?: "No date"

                                val meals = daySnapshot.children.mapNotNull { mealSnapshot ->
                                    if (mealSnapshot.key == "date") return@mapNotNull null
                                    val meal = mealSnapshot.getValue(Meal::class.java)
                                    meal?.let {
                                        val mealTiming = it.getActualTiming()
                                        if (mealTiming.isNotEmpty()) {
                                            timingsMap[it.type] = mealTiming
                                            Log.d(
                                                "TimingExtracted",
                                                "Meal: ${it.type}, Timing: $mealTiming"
                                            )
                                        }
                                        it
                                    }
                                }.sortedBy { meal ->
                                    // SAFE: Use indexOf with fallback to prevent crashes
                                    val index = mealOrder.indexOf(meal.type)
                                    if (index == -1) mealOrder.size else index // Put unknown meals at the end
                                }

                                if (meals.isNotEmpty()) {
                                    allDays.add(
                                        DayData(
                                            day = dayName,
                                            date = dateStr,
                                            meals = meals
                                        )
                                    )
                                }
                            }
                        }
                    }

                    _mealTimings.value = timingsMap
                    Log.d("AllTimings", "Stored timings: $timingsMap")

                    val sortedDays = allDays.sortedBy { dayData ->
                        try {
                            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            formatter.parse(dayData.date)?.time ?: Long.MAX_VALUE
                        } catch (e: Exception) {
                            Log.e("DateParsing", "Error parsing date: ${e.message}")
                            Long.MAX_VALUE
                        }
                    }

                    _weeklyMenuData.value = sortedDays
                    saveToCache(sortedDays, timingsMap)

                    // Update widget after successful data load
                    updateWidget()

                    Log.d(
                        "FirebaseData",
                        "Processed ${sortedDays.size} days with timings and cached"
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error: ${error.message}")
                _isLoading.value = false
            }
        }

        databaseReference?.addValueEventListener(databaseListener!!)
    }

    // ✅ Add this to MealViewModel
    private fun updateWidget() {
        try {
            MealsWidgetUpdater.updateWidgets(context)
        } catch (e: Exception) {
            Log.e("WidgetUpdate", "Error updating widget: ${e.message}")
        }
    }


    // Keep all your existing cache functions
    private fun loadMealDataWithCache() {
        val cachedTimestamp = sharedPrefs.getLong(CACHE_KEY_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()
        val isCacheValid = (currentTime - cachedTimestamp) < CACHE_DURATION_MS

        if (isCacheValid && hasCachedData()) {
            Log.d("MealCache", "Loading from cache...")
            loadFromCache()
        } else {
            Log.d("MealCache", "Cache expired or empty, fetching from server...")
            fetchDailyMenuFromServer()
        }
    }

    private fun hasCachedData(): Boolean {
        return sharedPrefs.contains(CACHE_KEY_MEAL_DATA) &&
                sharedPrefs.contains(CACHE_KEY_MEAL_TIMINGS)
    }

    private fun loadFromCache() {
        try {
            val cachedMealDataJson = sharedPrefs.getString(CACHE_KEY_MEAL_DATA, null)
            if (cachedMealDataJson != null) {
                val type = object : TypeToken<List<DayData>>() {}.type
                val cachedMealData: List<DayData> = gson.fromJson(cachedMealDataJson, type)
                _weeklyMenuData.value = cachedMealData
            }

            val cachedTimingsJson = sharedPrefs.getString(CACHE_KEY_MEAL_TIMINGS, null)
            if (cachedTimingsJson != null) {
                val type = object : TypeToken<Map<String, String>>() {}.type
                val cachedTimings: Map<String, String> = gson.fromJson(cachedTimingsJson, type)
                _mealTimings.value = cachedTimings
            }

            Log.d("MealCache", "Successfully loaded data from cache")
        } catch (e: Exception) {
            Log.e("MealCache", "Error loading from cache: ${e.message}")
            fetchDailyMenuFromServer()
        }
    }

    private fun saveToCache(mealData: List<DayData>, timings: Map<String, String>) {
        try {
            val editor = sharedPrefs.edit()
            val mealDataJson = gson.toJson(mealData)
            editor.putString(CACHE_KEY_MEAL_DATA, mealDataJson)
            val timingsJson = gson.toJson(timings)
            editor.putString(CACHE_KEY_MEAL_TIMINGS, timingsJson)
            editor.putLong(CACHE_KEY_TIMESTAMP, System.currentTimeMillis())
            editor.apply()
            Log.d("MealCache", "Data saved to cache successfully")
        } catch (e: Exception) {
            Log.e("MealCache", "Error saving to cache: ${e.message}")
        }
    }

    fun forceRefreshMealData() {
        Log.d("MealCache", "Force refreshing meal data...")
        clearCache()
        fetchDailyMenuFromServer()
    }

    private fun clearCache() {
        val editor = sharedPrefs.edit()
        editor.remove(CACHE_KEY_MEAL_DATA)
        editor.remove(CACHE_KEY_MEAL_TIMINGS)
        editor.remove(CACHE_KEY_TIMESTAMP)
        editor.apply()
        Log.d("MealCache", "Cache cleared")
    }

    fun isCacheValid(): Boolean {
        val cachedTimestamp = sharedPrefs.getLong(CACHE_KEY_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()
        return (currentTime - cachedTimestamp) < CACHE_DURATION_MS
    }

    fun getCacheAgeHours(): Long {
        val cachedTimestamp = sharedPrefs.getLong(CACHE_KEY_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()
        return (currentTime - cachedTimestamp) / (1000 * 60 * 60)
    }
}

// Data class to hold detailed rating information
data class MealLike(
    val userId: String = "",
    val mealType: String = "",
    val date: String = "",
    val isLike: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class MealLikeStats(
    val mealType: String = "",
    val date: String = "",
    val likesCount: Int = 0,
    val dislikesCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class MealLikeDetail(
    val likesCount: Int = 0,
    val dislikesCount: Int = 0,
    val mealType: String = "",
    val date: String = ""
)

// Keep existing data classes
data class DayData(
    val day: String,
    val date: String,
    val meals: List<Meal>
)

data class Meal(
    val type: String = "",
    val timing: String = "", // Keep for backward compatibility
    val time: String = "",   // Add to match your database structure
    val items: List<String> = listOf(),
    val hostels: Map<String, List<String>> = mapOf(),
    val commonItems: List<String> = listOf()
) {
    // Helper to get timing from either field
    fun getActualTiming(): String = if (time.isNotEmpty()) time else timing
}

// Keep existing font definitions
val montserratFont = FontFamily(
    androidx.compose.ui.text.font.Font(R.font.merriweatherregular)
)
val poppinsFont = FontFamily(
    androidx.compose.ui.text.font.Font(R.font.merriweatherbold)
)
val playfairFont = FontFamily(
    androidx.compose.ui.text.font.Font(R.font.merriweatherregular)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDetailsScreen(mealId: String?, navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        mealId ?: "Meal Details",
                        fontFamily = montserratFont
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* Handle back navigation */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Details for $mealId",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = poppinsFont
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Here you can display more information about the selected meal.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = playfairFont
                )
            )
        }
    }
}


