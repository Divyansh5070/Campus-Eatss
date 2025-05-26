package com.example.cueats.HomeScreen

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import com.example.cueats.AppBottomNavigation
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material3.placeholder
import com.google.accompanist.placeholder.material3.shimmer
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.example.cueats.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ModernWeeklyMenuApp(navController: NavController) {
    // Get system theme instead of manually tracking theme state
    val systemTheme = isSystemInDarkTheme()
    val isDarkTheme = systemTheme

    // Colors based on theme
    val primaryOrange = Color(0xFFFF6B01)
    val lightBackground = Color(0xFFF6F7FB)
    val darkBackground = Color(0xFF121212)
    val surfaceColor = if (isDarkTheme) Color(0xFF202020) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val textSecondaryColor = if (isDarkTheme) Color.LightGray else Color.Gray
    val headerColor = if (isDarkTheme) Color(0xFF303030) else Color.White
    val dividerColor = if (isDarkTheme) Color(0xFF303030) else Color(0xFFEEEEEE)
    val darkSurfaceColor = Color(0xFF202020)
    val currentRoute = navController.currentBackStackEntry?.destination?.route ?: "meals"

    // Function to find today's index in our days list
    fun findTodayIndex(days: List<DayData>): Int {
        if (days.isEmpty()) return 0

        val calendar = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val todayString = dayFormat.format(calendar.time)

        // First try to find exact date match
        val exactMatch = days.indexOfFirst { it.date == todayString }
        if (exactMatch >= 0) return exactMatch

        // If no exact match, find the closest upcoming day
        try {
            val today = dayFormat.parse(todayString)?.time ?: return 0

            var closestIndex = 0
            var smallestDiff = Long.MAX_VALUE

            days.forEachIndexed { index, dayData ->
                val dayDate = dayFormat.parse(dayData.date)?.time ?: return@forEachIndexed
                val diff = dayDate - today

                // Prioritize future dates (positive diff) over past dates
                if (diff >= 0 && diff < smallestDiff) {
                    smallestDiff = diff
                    closestIndex = index
                }
            }

            return closestIndex
        } catch (e: Exception) {
            Log.e("DateParsing", "Error finding today's index: ${e.message}")
            return 0
        }
    }

    val mealViewModel: MealViewModel = viewModel()
    val daysData by mealViewModel.weeklyMenuData.observeAsState(emptyList())
    var selectedDayIndex by remember { mutableStateOf(0) }

    // Update selected day index when data loads
    LaunchedEffect(daysData) {
        if (daysData.isNotEmpty()) {
            selectedDayIndex = findTodayIndex(daysData)
        }
    }

    val context = LocalContext.current
    val systemUiController = rememberSystemUiController()

    // Set status bar and navigation bar colors based on theme
    SideEffect {
        systemUiController.setStatusBarColor(color = primaryOrange)
        systemUiController.setNavigationBarColor(
            color = if (isDarkTheme) Color(0xFF202020) else Color.White,
            darkIcons = !isDarkTheme
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🍽️",
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Campus Eats",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = playfairFont
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryOrange,
                    titleContentColor = Color.White
                )
                // Settings action has been removed
            )
        },
        //Bottom bar
        bottomBar = {
            AppBottomNavigation(navController = navController, currentRoute = "meals")
        }
    ) { paddingValues ->
        // The rest of the code remains the same
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = if (isDarkTheme) darkBackground else lightBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Location indicator
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    // Card content
                }

                if (daysData.isNotEmpty()) {
                    // Remember states for auto-scrolling to today's date
                    val lazyRowState = rememberLazyListState()
                    val coroutineScope = rememberCoroutineScope()

                    // Function to find today's index in the data
                    fun findTodayIndex(daysData: List<DayData>): Int {
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val todayFormatted = dateFormat.format(Date())

                        return daysData.indexOfFirst { it.date == todayFormatted }.let {
                            if (it != -1) it else 0 // Return 0 if today not found
                        }
                    }

                    // Get today's index and set as initial selection
                    val todayIndex = remember { findTodayIndex(daysData) }

                    // Auto-scroll to today when component loads
                    LaunchedEffect(daysData) {
                        if (daysData.isNotEmpty()) {
                            // Set initial selected day to today
                            selectedDayIndex = todayIndex

                            // Scroll to today
                            coroutineScope.launch {
                                lazyRowState.scrollToItem(todayIndex)
                            }
                        }
                    }

                    // Calendar Day Selector
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LazyRow(
                                state = lazyRowState,  // Add the state here for controlling scroll position
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                itemsIndexed(daysData) { index, dayData ->
                                    // Format day and extract day number + month
                                    val dayName = dayData.day.take(3)

                                    val (dayDate, monthAbbrev) = try {
                                        val inputFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                        val date = inputFormatter.parse(dayData.date)

                                        val day = SimpleDateFormat("dd", Locale.getDefault()).format(date)
                                        val month = SimpleDateFormat("MMM", Locale.getDefault()).format(date) // Ex: Jan, Feb

                                        day to month
                                    } catch (e: Exception) {
                                        "??" to "???"
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (index == selectedDayIndex) primaryOrange else Color.Transparent
                                            )
                                            .clickable { selectedDayIndex = index }
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = dayName,
                                            fontSize = 12.sp,
                                            color = if (index == selectedDayIndex) Color.White else textSecondaryColor,
                                            fontFamily = playfairFont
                                        )
                                        Text(
                                            text = dayDate,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (index == selectedDayIndex) Color.White else textColor,
                                            fontFamily = poppinsFont
                                        )
                                        Text(
                                            text = monthAbbrev,
                                            fontSize = 10.sp,
                                            color = if (index == selectedDayIndex) Color.White else textSecondaryColor,
                                            fontFamily = playfairFont
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Meal Content
                    val selectedDay = daysData[selectedDayIndex]
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Group meals by type (breakfast, lunch, dinner)
                        val groupedMeals = selectedDay.meals.groupBy { it.type }

                        groupedMeals.forEach { (mealType, meals) ->
                            item {
                                MealTypeCard(
                                    mealType = mealType,
                                    meals = meals,
                                    navController = navController,
                                    isDarkTheme = isDarkTheme,
                                    surfaceColor = surfaceColor,
                                    textColor = textColor,
                                    textSecondaryColor = textSecondaryColor,
                                    dividerColor = dividerColor
                                )
                            }
                        }

                        // Add some space at the bottom
                        item {
                            Spacer(modifier = Modifier.height(5.dp))
                        }
                    }
                } else {
                    // Loading placeholders
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Date selection row at top
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFF5F5F5))
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
                                            .placeholder(
                                                visible = true,
                                                highlight = PlaceholderHighlight.shimmer(),
                                                color = if (isDarkTheme) Color(0xFF303030) else Color.LightGray
                                            )
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

                        // Breakfast section placeholder (unchanged)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFF5F5F5))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // Title and time row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Meal icon
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .placeholder(
                                                visible = true,
                                                highlight = PlaceholderHighlight.shimmer(),
                                                color = if (isDarkTheme) Color(0xFF303030) else Color.LightGray
                                            )
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        // Meal title (Breakfast)
                                        Box(
                                            modifier = Modifier
                                                .height(24.dp)
                                                .width(120.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .placeholder(
                                                    visible = true,
                                                    highlight = PlaceholderHighlight.shimmer(),
                                                    color = if (isDarkTheme) Color(0xFF303030) else Color.LightGray
                                                )
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Time (7:30 AM - 9:30 AM)
                                        Box(
                                            modifier = Modifier
                                                .height(16.dp)
                                                .width(160.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .placeholder(
                                                    visible = true,
                                                    highlight = PlaceholderHighlight.shimmer(),
                                                    color = if (isDarkTheme) Color(0xFF303030) else Color.LightGray
                                                )
                                        )
                                    }

                                    // Rating number
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .placeholder(
                                                visible = true,
                                                highlight = PlaceholderHighlight.shimmer(),
                                                color = if (isDarkTheme) Color(0xFF303030) else Color.LightGray
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Divider(color = if (isDarkTheme) Color.DarkGray else Color.LightGray)
                                Spacer(modifier = Modifier.height(16.dp))

                                // Menu items - 2 rows with 2 items each
                                FlowRow(
                                    maxItemsInEachRow = 2,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    repeat(4) {
                                        Box(
                                            modifier = Modifier
                                                .height(40.dp)
                                                .weight(1f)
                                                .clip(RoundedCornerShape(24.dp))
                                                .placeholder(
                                                    visible = true,
                                                    highlight = PlaceholderHighlight.shimmer(),
                                                    color = if (isDarkTheme) Color(0xFF303030) else Color.LightGray
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        // Lunch section placeholder (unchanged)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFF5F5F5))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // Title and time row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Meal icon
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .placeholder(
                                                visible = true,
                                                highlight = PlaceholderHighlight.shimmer(),
                                                color = if (isDarkTheme) Color(0xFF303030) else Color.LightGray
                                            )
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        // Meal title (Lunch)
                                        Box(
                                            modifier = Modifier
                                                .height(24.dp)
                                                .width(90.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .placeholder(
                                                    visible = true,
                                                    highlight = PlaceholderHighlight.shimmer(),
                                                    color = if (isDarkTheme) Color(0xFF303030) else Color.LightGray
                                                )
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Time (12:30 PM - 2:30 PM)
                                        Box(
                                            modifier = Modifier
                                                .height(16.dp)
                                                .width(160.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .placeholder(
                                                    visible = true,
                                                    highlight = PlaceholderHighlight.shimmer(),
                                                    color = if (isDarkTheme) Color(0xFF303030) else Color.LightGray
                                                )
                                        )
                                    }

                                    // Rating number
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .placeholder(
                                                visible = true,
                                                highlight = PlaceholderHighlight.shimmer(),
                                                color = if (isDarkTheme) Color(0xFF303030) else Color.LightGray
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Divider(color = if (isDarkTheme) Color.DarkGray else Color.LightGray)
                                Spacer(modifier = Modifier.height(16.dp))

                                // Menu items - 3 rows with 2 items each
                                FlowRow(
                                    maxItemsInEachRow = 2,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    repeat(6) {
                                        Box(
                                            modifier = Modifier
                                                .height(40.dp)
                                                .weight(1f)
                                                .clip(RoundedCornerShape(24.dp))
                                                .placeholder(
                                                    visible = true,
                                                    highlight = PlaceholderHighlight.shimmer(),
                                                    color = if (isDarkTheme) Color(0xFF303030) else Color.LightGray
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom navigation bar - no shimmer needed as it's usually static
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

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
    dividerColor: Color
) {
    // Enhanced meal icons with more visually appealing emojis
    val mealIcon = when (mealType.lowercase()) {
        "breakfast" -> "🍳"
        "lunch" -> "🍛"
        "snacks" -> "🧁"
        "dinner" -> "🍽️"
        else -> "🍴"
    }

    // Time ranges for each meal type
    val timeRange = when (mealType.lowercase()) {
        "breakfast" -> "7:30 AM - 9:30 AM"
        "lunch" -> "12:30 PM - 2:30 PM"
        "snacks" -> "4:30 PM - 6:00 PM"
        "dinner" -> "7:30 PM - 9:30 PM"
        else -> ""
    }

    // Color scheme based on meal type - improved for dark theme
    val (accentColor, subtleColor) = when (mealType.lowercase()) {
        "breakfast" -> Pair(
            Color(0xFFFFB74D),  // Brighter orange accent for better visibility
            if (isDarkTheme) Color(0xFFE89E76).copy(alpha = 0.7f) else Color(0xFFFFF8E1).copy(alpha = 0.5f)
        )
        "lunch" -> Pair(
            Color(0xFF64B5F6),  // Brighter blue accent
            if (isDarkTheme) Color(0xFF95B7D9).copy(alpha = 0.7f) else Color(0xFFE1F5FE).copy(alpha = 0.5f)
        )
        "snacks" -> Pair(
            Color(0xFFBA68C8),  // Brighter purple accent
            if (isDarkTheme) Color(0xFFD1B4EC).copy(alpha = 0.7f) else Color(0xFFF3E5F5).copy(alpha = 0.5f)
        )
        "dinner" -> Pair(
            Color(0xFF81C784),  // Brighter green accent
            if (isDarkTheme) Color(0xFFA3C9A5).copy(alpha = 0.7f) else Color(0xFFE8F5E9).copy(alpha = 0.5f)
        )
        else -> Pair(
            Color(0xFFFF8A65),  // Brighter default orange accent
            if (isDarkTheme) Color(0xFF3D2200).copy(alpha = 0.7f) else Color(0xFFFBE9E7).copy(alpha = 0.5f)
        )
    }

    // Generate a list of unique food items across all meals
    val allItems = mutableListOf<String>()
    meals.forEach { meal ->
        allItems.addAll(meal.items)
        allItems.addAll(meal.commonItems)
        meal.hostels.forEach { (_, hostelItems) ->
            allItems.addAll(hostelItems)
        }
    }
    val uniqueItems = allItems.distinct().sorted()

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
                // Header section with meal type info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Icon with accent color
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(subtleColor)
                            .border(width = 2.dp, color = accentColor, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mealIcon,
                            fontSize = 24.sp
                        )
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
                            text = timeRange,
                            fontSize = 14.sp,
                            color = textSecondaryColor,
                            fontFamily = playfairFont
                        )
                    }

                    // Item count in a subtle pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(subtleColor)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${uniqueItems.size}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = accentColor,
                            fontFamily = poppinsFont
                        )
                    }
                }

                // Elegant divider
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

                // Menu items - clean vertical list
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
            }
        }
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
                fontSize = 14.sp,
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

@Composable
fun ImprovedMealItemRow(itemName: String, dotColor: Color, textColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Item indicator dot with meal-specific color
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )

        // Item name with improved typography
        Text(
            text = itemName,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            fontFamily = playfairFont,
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        )


    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DishesGrid(
    dishes: List<String>,
    mealType: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 3
    ) {
        dishes.forEach { dish ->
            DishBox(
                dishName = dish,
                isDarkTheme = isDarkTheme,
                modifier = Modifier
            )
        }
    }
}

@Composable
fun DishBox(
    dishName: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (isDarkTheme) Color(0xFFE0E0E0) else Color(0xFF333333)
    val borderColor = if (isDarkTheme) Color(0xFF505050) else Color(0xFFE0E0E0)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = dishName,
            fontSize = 15.sp, // Slightly larger for clarity
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = TextStyle(
                fontFamily = FontFamily.SansSerif
            )
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


data class MealItem(
    val name: String,
    val calories: Int,
    val protein: Int
)

@Composable
fun MealItemRow(item: MealItem, isDarkTheme: Boolean = false) {
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val textSecondaryColor = if (isDarkTheme) Color.LightGray else Color.Gray
    val primaryOrange = Color(0xFFFF6B01)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isDarkTheme) Color(0xFF1E3C1F) else Color(0xFFE6F7E9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(16.dp)
            )
        }

        Text(
            text = item.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            fontFamily = playfairFont,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        )

        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Calories",
                    tint = primaryOrange,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${item.calories} cal",
                    fontSize = 12.sp,
                    color = textSecondaryColor,
                    fontFamily = playfairFont,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Protein",
                    tint = Color(0xFFFFB74D),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${item.protein}g protein",
                    fontSize = 12.sp,
                    color = textSecondaryColor,
                    fontFamily = playfairFont,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}


// Keep the existing viewmodel, data classes, and utility functions
open class MealViewModel : ViewModel() {
    private val _weeklyMenuData = MutableLiveData<List<DayData>>()
    val weeklyMenuData: LiveData<List<DayData>> get() = _weeklyMenuData

    init {
        fetchDailyMenu()
    }

    private fun fetchDailyMenu() {
        val database = FirebaseDatabase.getInstance("https://cu-eats-37fa0-default-rtdb.firebaseio.com/")
            .reference.child("meals")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FirebaseData", "Snapshot value: ${snapshot.value}")

                if (snapshot.exists()) {
                    val mealOrder = listOf("Breakfast", "Lunch", "Snacks", "Dinner")
                    val dayOrder = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

                    // Extract all day entries regardless of week
                    val allDays = mutableListOf<DayData>()

                    snapshot.children.forEach { weekSnapshot ->
                        Log.d("FirebaseData", "Processing Week: ${weekSnapshot.key}")

                        // Process each day in this week
                        dayOrder.forEach { dayName ->
                            val daySnapshot = weekSnapshot.child(dayName)
                            if (daySnapshot.exists()) {
                                // Get the date from the day data or use default
                                val dateStr = daySnapshot.child("date").getValue(String::class.java) ?: "No date"

                                // Get meals for this day
                                val meals = daySnapshot.children.mapNotNull { mealSnapshot ->
                                    // Skip the date field when processing meals
                                    if (mealSnapshot.key == "date") return@mapNotNull null
                                    mealSnapshot.getValue(Meal::class.java)
                                }.sortedBy { mealOrder.indexOf(it.type) }

                                // Only add days that have meal data
                                if (meals.isNotEmpty()) {
                                    allDays.add(DayData(
                                        day = dayName,
                                        date = dateStr,
                                        meals = meals
                                    ))
                                }
                            }
                        }
                    }

                    // Sort days chronologically
                    val sortedDays = allDays.sortedBy { dayData ->
                        // Try to parse date for sorting
                        try {
                            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            formatter.parse(dayData.date)?.time ?: Long.MAX_VALUE
                        } catch (e: Exception) {
                            // If date parsing fails, use a default ordering
                            Log.e("DateParsing", "Error parsing date: ${e.message}")
                            Long.MAX_VALUE
                        }
                    }

                    _weeklyMenuData.value = sortedDays
                    Log.d("FirebaseData", "Processed ${sortedDays.size} days")
                } else {
                    Log.d("FirebaseData", "No data found in snapshot")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error: ${error.message}")
            }
        })
    }
}

// Keep existing data classes
data class DayData(
    val day: String,
    val date: String,
    val meals: List<Meal>
)

data class Meal(
    val type: String = "",
    val timing: String = "",
    val items: List<String> = listOf(),
    val hostels: Map<String, List<String>> = mapOf(),
    val commonItems: List<String> = listOf()
)

data class WeekDay(
    val day: String = "",
    val date: String = "",
    val meals: List<Meal> = listOf()
)

data class WeekData(
    val weekNumber: Int = 0,
    val days: List<WeekDay> = listOf()
)

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

// Helper function to format dates (keeping existing function)
private fun formatDateForDisplay(dateStr: String): String {
    return try {
        // Assuming input format is dd/MM/yyyy
        val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val date = inputFormat.parse(dateStr)
        date?.let { outputFormat.format(it) } ?: dateStr
    } catch (e: Exception) {
        // If formatting fails, return the original string
        dateStr
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


