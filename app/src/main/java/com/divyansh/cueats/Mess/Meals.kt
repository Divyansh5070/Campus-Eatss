package com.divyansh.cueats.Mess

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import com.divyansh.cueats.AppBottomNavigation
import com.divyansh.cueats.LoginScreen.AuthViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.divyansh.cueats.R
import com.divyansh.cueats.LoginRoute
import com.divyansh.cueats.Notification.NotificationIconWithBadge
import com.divyansh.cueats.Notification.NotificationViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// Font definitions
val montserratFont = FontFamily(
    androidx.compose.ui.text.font.Font(R.font.merriweatherregular)
)
val poppinsFont = FontFamily(
    androidx.compose.ui.text.font.Font(R.font.merriweatherbold)
)
val playfairFont = FontFamily(
    androidx.compose.ui.text.font.Font(R.font.merriweatherregular)
)


@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ModernWeeklyMenuApp(
    navController: NavController,
    authViewModel: AuthViewModel? = null,
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val context = LocalContext.current
    val viewModel: MealViewModel = viewModel(
        factory = MealViewModelFactory(context)
    )

    // Modern color scheme
    val primaryOrange = Color(0xFFFF6B01)
    val lightBackground = Color(0xFFFFF5F0)
    val surfaceColor = Color.White
    val textColor = Color(0xFF2D2D2D)
    val textSecondaryColor = Color(0xFF757575)
    val systemUiController = rememberSystemUiController()

    // Observe notification count
    val unreadNotificationCount by notificationViewModel.unreadCount.observeAsState(0)

    // State for showing logout dialog
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Fetch notifications when the app starts
    LaunchedEffect(Unit) {
        notificationViewModel.fetchNotifications()
    }

    // Set up periodic notification refresh
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
                color = lightBackground,
                darkIcons = true
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
        containerColor = lightBackground
    ) { paddingValues ->

        // Pull-to-refresh state
        val isRefreshing by viewModel.isLoading.observeAsState(false)

        // Box to overlay navigation on content
        Box(modifier = Modifier.fillMaxSize()) {
            // ✨ KEY CHANGE: Everything is now in ONE scrollable LazyColumn
            if (daysData.isNotEmpty()) {
                val dailyGoal by viewModel.dailyGoal.observeAsState(2000)
                
                // Observe calorie update trigger for real-time updates
                val updateTrigger by viewModel.calorieUpdateTrigger.observeAsState(0L)
                
                // Get selected day's date
                val selectedDate = if (selectedDayIndex in daysData.indices) {
                    daysData[selectedDayIndex].date
                } else {
                    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    formatter.format(Date())
                }
                
                // Get calories for selected date - use State for reactive updates
                var selectedDayCalories by remember { mutableStateOf(0) }
                
                // Refresh calories when date changes OR when update trigger fires
                LaunchedEffect(selectedDayIndex, selectedDate, updateTrigger) {
                    selectedDayCalories = viewModel.getCaloriesForDate(selectedDate)
                }
                
                val remaining = (dailyGoal - selectedDayCalories).coerceAtLeast(0)
                val streak = viewModel.getStreak()

                val lazyRowState = rememberLazyListState()
                val coroutineScope = rememberCoroutineScope()
                
                // State for goal customization dialog
                var showGoalDialog by remember { mutableStateOf(false) }

                // Single LazyColumn for everything
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(lightBackground),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    contentPadding = PaddingValues(bottom = 100.dp) // Extra padding for floating nav
                ) {
                    // 1. Today's Fuel Card (now scrollable)
                    item(key = "fuel_card") {
                        TodaysFuelCard(
                            consumed = selectedDayCalories,
                            dailyGoal = dailyGoal,
                            remaining = remaining,
                            streak = streak,
                            primaryOrange = primaryOrange,
                            surfaceColor = surfaceColor,
                            textColor = textColor,
                            onGoalClick = { showGoalDialog = true },
                            selectedDate = selectedDate
                        )
                    }

                    // 2. Calendar Day Selector (now scrollable)
                    item(key = "date_selector") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Arrow
                                IconButton(
                                    onClick = {
                                        if (selectedDayIndex > 0) {
                                            selectedDayIndex--
                                            coroutineScope.launch {
                                                try {
                                                    lazyRowState.animateScrollToItem(
                                                        index = maxOf(0, selectedDayIndex - 2),
                                                        scrollOffset = 0
                                                    )
                                                } catch (e: Exception) {
                                                    Log.e("ScrollError", "Error scrolling: ${e.message}")
                                                }
                                            }
                                        }
                                    },
                                    enabled = selectedDayIndex > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowLeft,
                                        contentDescription = "Previous day",
                                        tint = if (selectedDayIndex > 0) primaryOrange else Color.Gray.copy(alpha = 0.3f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Date List
                                LazyRow(
                                    state = lazyRowState,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                    modifier = Modifier.weight(1f),
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

                                        // Animated day selector
                                        val scale by animateFloatAsState(
                                            targetValue = if (isSelected) 1.05f else 1f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            ), label = "day_scale"
                                        )

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(62.dp)
                                                .scale(scale)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSelected) {
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                Color(0xFFFF6B01),
                                                                Color(0xFFFF8E53)
                                                            )
                                                        )
                                                    } else {
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                Color(0xFFF5F5F5),
                                                                Color(0xFFF5F5F5)
                                                            )
                                                        )
                                                    }
                                                )
                                                .clickable(
                                                    indication = null,
                                                    interactionSource = remember { MutableInteractionSource() }
                                                ) {
                                                    selectedDayIndex = index
                                                    coroutineScope.launch {
                                                        try {
                                                            lazyRowState.animateScrollToItem(
                                                                index = maxOf(0, index - 2),
                                                                scrollOffset = 0
                                                            )
                                                        } catch (e: Exception) {
                                                            Log.e("ScrollError", "Error scrolling: ${e.message}")
                                                        }
                                                    }
                                                }
                                                .padding(vertical = 10.dp, horizontal = 6.dp)
                                        ) {
                                            Text(
                                                text = dayName,
                                                fontSize = 11.sp,
                                                color = if (isSelected) Color.White else textSecondaryColor,
                                                fontFamily = poppinsFont,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = dayDate,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else textColor,
                                                fontFamily = poppinsFont
                                            )
                                            Text(
                                                text = monthAbbrev,
                                                fontSize = 9.sp,
                                                color = if (isSelected) Color.White.copy(alpha = 0.9f) else textSecondaryColor,
                                                fontFamily = playfairFont
                                            )
                                        }
                                    }
                                }

                                // Right Arrow
                                IconButton(
                                    onClick = {
                                        if (selectedDayIndex < daysData.size - 1) {
                                            selectedDayIndex++
                                            coroutineScope.launch {
                                                try {
                                                    lazyRowState.animateScrollToItem(
                                                        index = maxOf(0, selectedDayIndex - 2),
                                                        scrollOffset = 0
                                                    )
                                                } catch (e: Exception) {
                                                    Log.e("ScrollError", "Error scrolling: ${e.message}")
                                                }
                                            }
                                        }
                                    },
                                    enabled = selectedDayIndex < daysData.size - 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = "Next day",
                                        tint = if (selectedDayIndex < daysData.size - 1) primaryOrange else Color.Gray.copy(alpha = 0.3f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Auto-scroll to selected day
                    item {
                        LaunchedEffect(selectedDayIndex, daysData.size) {
                            if (daysData.isNotEmpty() && selectedDayIndex in 0 until daysData.size) {
                                coroutineScope.launch {
                                    try {
                                        lazyRowState.animateScrollToItem(
                                            index = maxOf(0, selectedDayIndex - 2),
                                            scrollOffset = 0
                                        )
                                    } catch (e: Exception) {
                                        Log.e("ScrollError", "Error scrolling: ${e.message}")
                                    }
                                }
                            }
                        }
                    }

                    // 3. Meal Cards (now seamlessly integrated)
                    if (selectedDayIndex in daysData.indices) {
                        val selectedDay = daysData[selectedDayIndex]
                        val groupedMeals = selectedDay.meals.groupBy { it.type }

                        groupedMeals.forEach { (mealType, meals) ->
                            item(key = "${selectedDay.date}_$mealType") {
                                Box(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                                ) {
                                    ModernMealTypeCard(
                                        mealType = mealType,
                                        meals = meals,
                                        navController = navController,
                                        surfaceColor = surfaceColor,
                                        textColor = textColor,
                                        textSecondaryColor = textSecondaryColor,
                                        mealViewModel = mealViewModel,
                                        selectedDate = selectedDay.date,
                                        selectedDayDate = selectedDay.date
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Goal Customization Dialog
                if (showGoalDialog) {
                    GoalCustomizationDialog(
                        currentGoal = dailyGoal,
                        onDismiss = { showGoalDialog = false },
                        onSave = { newGoal ->
                            viewModel.updateDailyGoal(newGoal)
                            showGoalDialog = false
                        },
                        primaryOrange = primaryOrange,
                        surfaceColor = surfaceColor,
                        textColor = textColor
                    )
                }
            } else {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(lightBackground)
                ) {
                    LoadingPlaceholder(surfaceColor = surfaceColor)
                }
            }
            
            // Floating navigation overlay (iOS style) - Must be last in Box to be on top
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                AppBottomNavigation(
                    navController = navController,
                    currentRoute = "meals"
                )
            }
        }
    }
}



@Composable
fun TodaysFuelCard(
    consumed: Int,
    dailyGoal: Int,
    remaining: Int,
    streak: Int,
    primaryOrange: Color,
    surfaceColor: Color,
    textColor: Color,
    onGoalClick: () -> Unit = {},
    selectedDate: String = ""
) {
    // Check if selected date is today
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val today = formatter.format(Date())
    val isToday = selectedDate == today
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = primaryOrange
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🔥",
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isToday) "Today's Fuel" else "Fuel Tracker",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = poppinsFont
                        )
                        if (!isToday && selectedDate.isNotEmpty()) {
                            Text(
                                text = selectedDate,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                fontFamily = playfairFont
                            )
                        }
                    }
                }

                // Streak Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "⚡",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$streak day streak",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontFamily = poppinsFont
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Content Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Progress
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(120.dp)
                ) {
                    // Background Circle
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier.size(120.dp)
                    ) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.2f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12.dp.toPx())
                        )
                    }

                    // Progress Circle
                    val progress = (consumed.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier.size(120.dp)
                    ) {
                        drawArc(
                            color = Color.White,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 12.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }

                    // Center Text
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = consumed.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = poppinsFont
                        )
                        Text(
                            text = "consumed",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            fontFamily = playfairFont
                        )
                    }
                }

                // Stats Column
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Daily Goal - Now Clickable
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable(onClick = onGoalClick)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Daily Goal",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontFamily = playfairFont
                                )
                                Text(
                                    text = "$dailyGoal cal",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = poppinsFont
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit goal",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Remaining
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Remaining",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                fontFamily = playfairFont
                            )
                            Text(
                                text = "$remaining cal",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = poppinsFont
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingPlaceholder(surfaceColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date selection placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            )
        ) {}

        // Meal section placeholders
        repeat(2) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            ) {}
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModernMealTypeCard(
    mealType: String,
    meals: List<Meal>,
    navController: NavController,
    surfaceColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    mealViewModel: MealViewModel? = null,
    selectedDate: String = "",
    selectedDayDate: String = ""
) {
    val mealIcon = when (mealType.lowercase()) {
        "breakfast" -> "🍳"
        "lunch" -> "🍛"
        "snacks" -> "🧁"
        "dinner" -> "🍽️"
        "south indian dinner" -> "🥘"
        else -> "🍴"
    }

    val gradientColors = when (mealType.lowercase()) {
        "breakfast" -> listOf(Color(0xFFFFB74D), Color(0xFFFF9800))
        "lunch" -> listOf(Color(0xFFFF6B6B), Color(0xFFFFB88C))
        "snacks" -> listOf(Color(0xFFBA68C8), Color(0xFFFF6B9D))
        "dinner" -> listOf(Color(0xFFFF6B01), Color(0xFFFF8E53))
        "south indian dinner" -> listOf(Color(0xFFFF7043), Color(0xFFFFAB91))
        else -> listOf(Color(0xFFFF8A65), Color(0xFFFFAB91))
    }

    // NEW: Observe dishes from ViewModel
    val dishesMap by mealViewModel?.dishesMap?.observeAsState(emptyMap()) ?: remember { mutableStateOf(emptyMap()) }

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

    // Get timing
    val timeRange = when {
        meals.isNotEmpty() && meals.first().getActualTiming().isNotBlank() -> meals.first().getActualTiming()
        mealViewModel != null -> mealViewModel.getTimingForMeal(mealType)
        else -> getDefaultTimingForMeal(mealType)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Gradient Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(gradientColors)
                    )
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = mealIcon, fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Meal info
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = mealType.capitalize(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = poppinsFont
                        )
                        Text(
                            text = timeRange.ifEmpty { "Time not available" },
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            fontFamily = playfairFont
                        )
                    }

                    // Item count badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${uniqueItems.size} Items",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontFamily = poppinsFont
                        )
                    }
                }
            }

            // Meal items
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uniqueItems.forEachIndexed { index, item ->
                    key("${mealType}_${item}_$index") {
                        // Convert dish name to ID for lookup
                        val dishId = item.lowercase()
                            .replace(" ", "_")
                            .replace("(", "")
                            .replace(")", "")
                        val dish = dishesMap[dishId]

                        ModernMealItemCard(
                            itemName = item,
                            accentColor = gradientColors[0],
                            index = index,
                            dish = dish,  // NEW: Pass real dish data
                            mealViewModel = mealViewModel,  // NEW: Pass ViewModel
                            selectedDate = selectedDayDate  // NEW: Pass selected date
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernMealItemCard(
    itemName: String,
    accentColor: Color,
    index: Int,
    dish: Dish? = null,
    mealViewModel: MealViewModel? = null,
    selectedDate: String = ""
) {
    // Get data from Firebase if available
    val calories = dish?.calories ?: 0
    val hasDishData = dish != null

    // Track expansion state and item count
    var isExpanded by rememberSaveable(key = "expanded_$itemName") { mutableStateOf(false) }
    val initialCount = if (selectedDate.isNotEmpty()) {
        mealViewModel?.getDishCountForDate(selectedDate, itemName) ?: 0
    } else {
        mealViewModel?.getDishCount(itemName) ?: 0
    }
    var itemCount by rememberSaveable(key = "item_count_${itemName}_$selectedDate") { mutableStateOf(initialCount) }

    // Varying elevation based on index to show visual hierarchy
    val cardElevation = when (index % 5) {
        0 -> 2.dp
        1 -> 3.dp
        2 -> 4.dp
        3 -> 5.dp
        else -> 6.dp
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFBF5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Main row: Dish name and arrow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = itemName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF2D2D2D),
                    fontFamily = poppinsFont,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color(0xFF757575),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expandable section with details
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    // Show Firebase details if available
                    if (hasDishData) {
                        // Nutrition info
                        if (dish?.nutrition != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFFF8F0))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Nutrition Information",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2D2D2D),
                                    fontFamily = poppinsFont
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                if (calories > 0) {
                                    NutritionDetailRow("Calories", "$calories cal")
                                }
                                if (dish.nutrition.protein > 0) {
                                    NutritionDetailRow("Protein", "${dish.nutrition.protein}g")
                                }
                                if (dish.nutrition.carbs > 0) {
                                    NutritionDetailRow("Carbs", "${dish.nutrition.carbs}g")
                                }
                                if (dish.nutrition.fat > 0) {
                                    NutritionDetailRow("Fat", "${dish.nutrition.fat}g")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Add/Remove controls - only if we have calorie data
                        if (calories > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (itemCount == 0) {
                                    // Add button
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color(0xFFFF6B01),
                                                        Color(0xFFFF8E53)
                                                    )
                                                )
                                            )
                                            .clickable {
                                                itemCount++
                                                if (selectedDate.isNotEmpty()) {
                                                    mealViewModel?.addDishToDate(selectedDate, itemName, calories)
                                                } else {
                                                    mealViewModel?.addDishToToday(itemName, calories)
                                                }
                                            }
                                            .padding(horizontal = 24.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Add to Today",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                fontFamily = poppinsFont
                                            )
                                        }
                                    }
                                } else {
                                    // Counter controls
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Minus button
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFFEBEE))
                                                .clickable {
                                                    if (itemCount > 0) {
                                                        itemCount--
                                                        if (selectedDate.isNotEmpty()) {
                                                            mealViewModel?.removeDishFromDate(selectedDate, itemName, calories)
                                                        } else {
                                                            mealViewModel?.removeDishFromToday(itemName, calories)
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Remove",
                                                tint = Color(0xFFFF6B01),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Count
                                        Text(
                                            text = "$itemCount",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2D2D2D),
                                            fontFamily = poppinsFont
                                        )

                                        // Plus button
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    brush = Brush.verticalGradient(
                                                        colors = listOf(
                                                            Color(0xFFFF6B01),
                                                            Color(0xFFFF8E53)
                                                        )
                                                    )
                                                )
                                                .clickable {
                                                    itemCount++
                                                    if (selectedDate.isNotEmpty()) {
                                                        mealViewModel?.addDishToDate(selectedDate, itemName, calories)
                                                    } else {
                                                        mealViewModel?.addDishToToday(itemName, calories)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // No Firebase data available
                        Text(
                            text = "No nutritional information available",
                            fontSize = 12.sp,
                            color = Color(0xFF757575),
                            fontFamily = playfairFont,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NutritionDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF757575),
            fontFamily = playfairFont
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2D2D2D),
            fontFamily = poppinsFont
        )
    }
}


@Composable
fun NutritionItem(
    emoji: String,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = emoji,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF666666),
                fontFamily = playfairFont
            )
        }

        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            fontFamily = poppinsFont
        )
    }
}


// Extension function to capitalize first letter of a string
fun String.capitalize(): String {
    return this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault())
        else it.toString()
    }
}

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

