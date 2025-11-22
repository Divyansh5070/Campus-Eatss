//import android.annotation.SuppressLint
//import android.util.Log
//import android.widget.Toast
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.WindowInsets
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.safeDrawing
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.layout.windowInsetsPadding
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.LazyRow
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.lazy.itemsIndexed
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material.icons.filled.Notifications
//import androidx.compose.material3.Card
//import androidx.compose.material3.CardDefaults
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Surface
//import androidx.compose.material3.Text
//import androidx.compose.material3.TopAppBar
//import androidx.compose.material3.TopAppBarDefaults
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.SideEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.livedata.observeAsState
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontFamily
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavController
//import com.example.cueats.R
//import com.google.accompanist.placeholder.PlaceholderHighlight
//import com.google.accompanist.placeholder.material3.placeholder
//import com.google.accompanist.placeholder.material3.shimmer
//import com.google.accompanist.systemuicontroller.rememberSystemUiController
//import com.google.firebase.database.DataSnapshot
//import com.google.firebase.database.DatabaseError
//import com.google.firebase.database.FirebaseDatabase
//import com.google.firebase.database.ValueEventListener
//import java.text.SimpleDateFormat
//import java.util.Calendar
//import java.util.Locale
//
////package com.example.cueats
////
//@SuppressLint("NewApi")
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ModernWeeklyMenuApp(navController: NavController) {
//    // Function to find today's index in our days list
//    fun findTodayIndex(days: List<DayData>): Int {
//        if (days.isEmpty()) return 0
//
//        val calendar = Calendar.getInstance()
//        val dayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
//        val todayString = dayFormat.format(calendar.time)
//
//        // First try to find exact date match
//        val exactMatch = days.indexOfFirst { it.date == todayString }
//        if (exactMatch >= 0) return exactMatch
//
//        // If no exact match, find the closest upcoming day
//        try {
//            val today = dayFormat.parse(todayString)?.time ?: return 0
//
//            var closestIndex = 0
//            var smallestDiff = Long.MAX_VALUE
//
//            days.forEachIndexed { index, dayData ->
//                val dayDate = dayFormat.parse(dayData.date)?.time ?: return@forEachIndexed
//                val diff = dayDate - today
//
//                // Prioritize future dates (positive diff) over past dates
//                if (diff >= 0 && diff < smallestDiff) {
//                    smallestDiff = diff
//                    closestIndex = index
//                }
//            }
//
//            return closestIndex
//        } catch (e: Exception) {
//            Log.e("DateParsing", "Error finding today's index: ${e.message}")
//            return 0
//        }
//    }
//
//    val mealViewModel: MealViewModel = viewModel()
//    val daysData by mealViewModel.weeklyMenuData.observeAsState(emptyList())
//    var selectedDayIndex by remember { mutableStateOf(0) }
//
//    // Update selected day index when data loads
//    LaunchedEffect(daysData) {
//        if (daysData.isNotEmpty()) {
//            selectedDayIndex = findTodayIndex(daysData)
//        }
//    }
//
//    val context = LocalContext.current
//    val systemUiController = rememberSystemUiController()
//    val primaryOrange = Color(0xFFFF6B01)
//    val lightBackground = Color(0xFFF6F7FB)
//
//    // Set status bar color to match the TopAppBar background
//    SideEffect {
//        systemUiController.setStatusBarColor(color = primaryOrange)
//    }
//
//    var expandedMenu by remember { mutableStateOf("Block A Mess Menu") }
//    var showDropdown by remember { mutableStateOf(false) }
//
//    Surface(
//        modifier = Modifier.fillMaxSize(),
//        color = lightBackground
//    ) {
//        Column(modifier = Modifier.fillMaxSize()) {
//            // Top App Bar - Orange header
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .background(primaryOrange)
//                    .padding(vertical = 16.dp, horizontal = 20.dp)
//            ) {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Box(
//                        modifier = Modifier
//                            .size(32.dp)
//                            .clip(CircleShape)
//                            .background(Color.White.copy(alpha = 0.3f)),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Text(
//                            text = "🍽️",
//                            fontSize = 16.sp,
//                            color = Color.White
//                        )
//                    }
//                    Spacer(modifier = Modifier.width(12.dp))
//                    Text(
//                        "CU Eats",
//                        color = Color.White,
//                        fontSize = 22.sp,
//                        fontWeight = FontWeight.Bold,
//                        fontFamily = playfairFont
//                    )
//                }
//            }
//
//            // Menu Selection Dropdown
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .background(lightBackground)
//                    .padding(horizontal = 20.dp, vertical = 16.dp)
//            ) {
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .clickable { showDropdown = !showDropdown },
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        text = expandedMenu,
//                        fontSize = 18.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color.DarkGray,
//                        fontFamily = poppinsFont
//                    )
//                    Icon(
//                        imageVector = if (showDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
//                        contentDescription = "Toggle dropdown",
//                        tint = Color.DarkGray
//                    )
//                }
//
//                DropdownMenu(
//                    expanded = showDropdown,
//                    onDismissRequest = { showDropdown = false },
//                    modifier = Modifier
//                        .fillMaxWidth(0.8f)
//                        .background(Color.White)
//                ) {
//                    DropdownMenuItem(
//                        text = { Text("Block A Mess Menu", fontFamily = playfairFont) },
//                        onClick = {
//                            expandedMenu = "Block A Mess Menu"
//                            showDropdown = false
//                        }
//                    )
//                    DropdownMenuItem(
//                        text = { Text("Block B Mess Menu", fontFamily = playfairFont) },
//                        onClick = {
//                            expandedMenu = "Block B Mess Menu"
//                            showDropdown = false
//                        }
//                    )
//                    DropdownMenuItem(
//                        text = { Text("Cafeteria Menu", fontFamily = playfairFont) },
//                        onClick = {
//                            expandedMenu = "Cafeteria Menu"
//                            showDropdown = false
//                        }
//                    )
//                }
//            }
//
//            if (daysData.isNotEmpty()) {
//                // Calendar Day Selector
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 20.dp, vertical = 8.dp),
//                    colors = CardDefaults.cardColors(containerColor = Color.White),
//                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//                ) {
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(vertical = 8.dp),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        IconButton(onClick = { /* Navigate to previous week */ }) {
//                            Icon(
//                                imageVector = Icons.Default.KeyboardArrowLeft,
//                                contentDescription = "Previous",
//                                tint = Color.Gray
//                            )
//                        }
//
//                        LazyRow(
//                            horizontalArrangement = Arrangement.spacedBy(4.dp),
//                            modifier = Modifier.weight(1f)
//                        ) {
//                            itemsIndexed(daysData) { index, dayData ->
//                                // Format day and extract day number
//                                val dayName = dayData.day.take(3)
//                                val dayDate = try {
//                                    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
//                                    val date = formatter.parse(dayData.date)
//                                    SimpleDateFormat("dd", Locale.getDefault()).format(date)
//                                } catch (e: Exception) {
//                                    "??"
//                                }
//
//                                Column(
//                                    horizontalAlignment = Alignment.CenterHorizontally,
//                                    modifier = Modifier
//                                        .width(48.dp)
//                                        .clip(RoundedCornerShape(8.dp))
//                                        .background(
//                                            if (index == selectedDayIndex) primaryOrange else Color.Transparent
//                                        )
//                                        .clickable { selectedDayIndex = index }
//                                        .padding(vertical = 8.dp)
//                                ) {
//                                    Text(
//                                        text = dayName,
//                                        fontSize = 12.sp,
//                                        color = if (index == selectedDayIndex) Color.White else Color.Gray,
//                                        fontFamily = playfairFont
//                                    )
//                                    Text(
//                                        text = dayDate,
//                                        fontSize = 16.sp,
//                                        fontWeight = FontWeight.Bold,
//                                        color = if (index == selectedDayIndex) Color.White else Color.Black,
//                                        fontFamily = poppinsFont
//                                    )
//                                    Text(
//                                        text = "Apr",
//                                        fontSize = 10.sp,
//                                        color = if (index == selectedDayIndex) Color.White else Color.Gray,
//                                        fontFamily = playfairFont
//                                    )
//                                }
//                            }
//                        }
//
//                        IconButton(onClick = { /* Navigate to next week */ }) {
//                            Icon(
//                                imageVector = Icons.Default.KeyboardArrowRight,
//                                contentDescription = "Next",
//                                tint = Color.Gray
//                            )
//                        }
//                    }
//                }
//
//                // Meal Content
//                val selectedDay = daysData[selectedDayIndex]
//                LazyColumn(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 20.dp, vertical = 8.dp),
//                    verticalArrangement = Arrangement.spacedBy(16.dp)
//                ) {
//                    // Group meals by type (breakfast, lunch, dinner)
//                    val groupedMeals = selectedDay.meals.groupBy { it.type }
//
//                    groupedMeals.forEach { (mealType, meals) ->
//                        item {
//                            MealTypeCard(mealType = mealType, meals = meals, navController = navController)
//                        }
//                    }
//                }
//            } else {
//                // Loading placeholders
//                Column(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(16.dp),
//                    verticalArrangement = Arrangement.spacedBy(16.dp)
//                ) {
//                    repeat(3) { // Show 3 shimmer placeholders for meal cards
//                        Card(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .height(120.dp)
//                                .clip(RoundedCornerShape(12.dp))
//                                .placeholder(
//                                    visible = true,
//                                    highlight = PlaceholderHighlight.shimmer()
//                                ),
//                            colors = CardDefaults.cardColors(containerColor = Color.White)
//                        ) {
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxSize()
//                                    .padding(12.dp),
//                                verticalAlignment = Alignment.CenterVertically
//                            ) {
//                                Box(
//                                    modifier = Modifier
//                                        .size(80.dp)
//                                        .clip(RoundedCornerShape(10.dp))
//                                        .background(Color.Gray)
//                                        .placeholder(
//                                            visible = true,
//                                            highlight = PlaceholderHighlight.shimmer()
//                                        )
//                                )
//
//                                Spacer(modifier = Modifier.width(16.dp))
//
//                                Column(
//                                    modifier = Modifier.weight(1f)
//                                ) {
//                                    Box(
//                                        modifier = Modifier
//                                            .fillMaxWidth(0.7f)
//                                            .height(16.dp)
//                                            .clip(RoundedCornerShape(4.dp))
//                                            .background(Color.Gray)
//                                            .placeholder(
//                                                visible = true,
//                                                highlight = PlaceholderHighlight.shimmer()
//                                            )
//                                    )
//                                    Spacer(modifier = Modifier.height(8.dp))
//                                    Box(
//                                        modifier = Modifier
//                                            .fillMaxWidth(0.5f)
//                                            .height(14.dp)
//                                            .clip(RoundedCornerShape(4.dp))
//                                            .background(Color.Gray)
//                                            .placeholder(
//                                                visible = true,
//                                                highlight = PlaceholderHighlight.shimmer()
//                                            )
//                                    )
//                                }
//                            }
//                        }
//                    }
//
//                    Text(
//                        text = "Fetching the latest meal plan... 🍽️",
//                        color = Color.DarkGray,
//                        fontSize = 18.sp,
//                        fontWeight = FontWeight.Bold,
//                        fontFamily = poppinsFont,
//                        modifier = Modifier
//                            .padding(top = 16.dp)
//                            .align(Alignment.CenterHorizontally)
//                            .placeholder(
//                                visible = true,
//                                highlight = PlaceholderHighlight.shimmer()
//                            )
//                    )
//                }
//            }
//
//            // Bottom Navigation
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .background(Color.White)
//            ) {
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(8.dp),
//                    horizontalArrangement = Arrangement.SpaceEvenly
//                ) {
//                    BottomNavItem(
//                        icon = Icons.Default.Home,
//                        label = "Hostel Mess",
//                        isSelected = true,
//                        onClick = { }
//                    )
//                    BottomNavItem(
//                        icon = Icons.Default.ShoppingCart,
//                        label = "Campus Shops",
//                        isSelected = false,
//                        onClick = { navController.navigate("shops") }
//                    )
//                    BottomNavItem(
//                        icon = Icons.Default.AccountBalance,
//                        label = "Budget",
//                        isSelected = false,
//                        onClick = { navController.navigate("budget") }
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun MealTypeCard(mealType: String, meals: List<Meal>, navController: NavController) {
//    val mealIcon = when (mealType.lowercase()) {
//        "breakfast" -> "🥗"
//        "lunch" -> "🍚"
//        "snacks" -> "🌮"
//        "dinner" -> "🍽️"
//        else -> "🍽️"
//    }
//
//    val timeRange = when (mealType.lowercase()) {
//        "breakfast" -> "7:30 AM - 9:30 AM"
//        "lunch" -> "12:30 PM - 2:30 PM"
//        "snacks" -> "4:30 PM - 6:00 PM"
//        "dinner" -> "7:30 PM - 9:30 PM"
//        else -> ""
//    }
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth(),
//        colors = CardDefaults.cardColors(containerColor = Color.White),
//        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp)
//        ) {
//            // Meal Type Header
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                modifier = Modifier.padding(bottom = 12.dp)
//            ) {
//                Box(
//                    modifier = Modifier
//                        .size(40.dp)
//                        .clip(CircleShape)
//                        .background(Color(0xFFFFF2ED)),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text(
//                        text = mealIcon,
//                        fontSize = 20.sp
//                    )
//                }
//
//                Column(
//                    modifier = Modifier
//                        .padding(start = 12.dp)
//                ) {
//                    Text(
//                        text = mealType,
//                        fontSize = 18.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color.Black,
//                        fontFamily = poppinsFont
//                    )
//                    Text(
//                        text = timeRange,
//                        fontSize = 14.sp,
//                        color = Color.Gray,
//                        fontFamily = playfairFont
//                    )
//                }
//            }
//
//            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
//
//            // Menu Items
//            Column(
//                modifier = Modifier.padding(top = 8.dp)
//            ) {
//                // Convert meal items to the format expected by MealItemRow
//                // (assumes each meal has items, commonItems or hostels)
//                val allItems = mutableListOf<MealItem>()
//
//                meals.forEach { meal ->
//                    meal.items.forEach { item ->
//                        allItems.add(
//                            MealItem(
//                                name = item,
//                                calories = (120..350).random(),
//                                protein = (0..10).random()
//                            )
//                        )
//                    }
//
//                    meal.commonItems.forEach { item ->
//                        allItems.add(
//                            MealItem(
//                                name = item,
//                                calories = (120..350).random(),
//                                protein = (0..10).random()
//                            )
//                        )
//                    }
//
//                    meal.hostels.forEach { (_, hostelItems) ->
//                        hostelItems.forEach { item ->
//                            allItems.add(
//                                MealItem(
//                                    name = item,
//                                    calories = (120..350).random(),
//                                    protein = (0..10).random()
//                                )
//                            )
//                        }
//                    }
//                }
//
//                allItems.sortedBy { it.name }.forEach { item ->
//                    MealItemRow(item)
//                }
//            }
//        }
//    }
//}
//
//data class MealItem(
//    val name: String,
//    val calories: Int,
//    val protein: Int
//)
//
//@Composable
//fun MealItemRow(item: MealItem) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 8.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Box(
//            modifier = Modifier
//                .size(24.dp)
//                .clip(CircleShape)
//                .background(Color(0xFFE6F7E9)),
//            contentAlignment = Alignment.Center
//        ) {
//            Icon(
//                imageVector = Icons.Default.CheckCircle,
//                contentDescription = null,
//                tint = Color(0xFF4CAF50),
//                modifier = Modifier.size(16.dp)
//            )
//        }
//
//        Text(
//            text = item.name,
//            fontSize = 16.sp,
//            fontWeight = FontWeight.Medium,
//            color = Color.Black,
//            fontFamily = playfairFont,
//            modifier = Modifier
//                .weight(1f)
//                .padding(start = 12.dp)
//        )
//
//        Row(
//            horizontalArrangement = Arrangement.End,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                modifier = Modifier.padding(end = 12.dp)
//            ) {
//                Icon(
//                    imageVector = Icons.Default.LocalFireDepartment,
//                    contentDescription = "Calories",
//                    tint = Color(0xFFFF6B01),
//                    modifier = Modifier.size(16.dp)
//                )
//                Text(
//                    text = "${item.calories} cal",
//                    fontSize = 12.sp,
//                    color = Color.Gray,
//                    fontFamily = playfairFont,
//                    modifier = Modifier.padding(start = 4.dp)
//                )
//            }
//
//            Row(
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Icon(
//                    imageVector = Icons.Default.FitnessCenter,
//                    contentDescription = "Protein",
//                    tint = Color(0xFFFFB74D),
//                    modifier = Modifier.size(16.dp)
//                )
//                Text(
//                    text = "${item.protein}g protein",
//                    fontSize = 12.sp,
//                    color = Color.Gray,
//                    fontFamily = playfairFont,
//                    modifier = Modifier.padding(start = 4.dp)
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun BottomNavItem(
//    icon: ImageVector,
//    label: String,
//    isSelected: Boolean,
//    onClick: () -> Unit
//) {
//    val primaryOrange = Color(0xFFFF6B01)
//
//    Column(
//        horizontalAlignment = Alignment.CenterHorizontally,
//        modifier = Modifier
//            .clickable(onClick = onClick)
//            .padding(8.dp)
//    ) {
//        Icon(
//            imageVector = icon,
//            contentDescription = label,
//            tint = if (isSelected) primaryOrange else Color.Gray,
//            modifier = Modifier.size(24.dp)
//        )
//        Text(
//            text = label,
//            fontSize = 12.sp,
//            color = if (isSelected) primaryOrange else Color.Gray,
//            fontFamily = playfairFont
//        )
//    }
//}
//
//// Keep the existing viewmodel, data classes, and utility functions
//open class MealViewModel : ViewModel() {
//    private val _weeklyMenuData = MutableLiveData<List<DayData>>()
//    val weeklyMenuData: LiveData<List<DayData>> get() = _weeklyMenuData
//
//    init {
//        fetchDailyMenu()
//    }
//
//    private fun fetchDailyMenu() {
//        val database = FirebaseDatabase.getInstance("https://cu-eats-37fa0-default-rtdb.firebaseio.com/")
//            .reference.child("meals")
//
//        database.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                Log.d("FirebaseData", "Snapshot value: ${snapshot.value}")
//
//                if (snapshot.exists()) {
//                    val mealOrder = listOf("Breakfast", "Lunch", "Snacks", "Dinner")
//                    val dayOrder = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
//
//                    // Extract all day entries regardless of week
//                    val allDays = mutableListOf<DayData>()
//
//                    snapshot.children.forEach { weekSnapshot ->
//                        Log.d("FirebaseData", "Processing Week: ${weekSnapshot.key}")
//
//                        // Process each day in this week
//                        dayOrder.forEach { dayName ->
//                            val daySnapshot = weekSnapshot.child(dayName)
//                            if (daySnapshot.exists()) {
//                                // Get the date from the day data or use default
//                                val dateStr = daySnapshot.child("date").getValue(String::class.java) ?: "No date"
//
//                                // Get meals for this day
//                                val meals = daySnapshot.children.mapNotNull { mealSnapshot ->
//                                    // Skip the date field when processing meals
//                                    if (mealSnapshot.key == "date") return@mapNotNull null
//                                    mealSnapshot.getValue(Meal::class.java)
//                                }.sortedBy { mealOrder.indexOf(it.type) }
//
//                                // Only add days that have meal data
//                                if (meals.isNotEmpty()) {
//                                    allDays.add(DayData(
//                                        day = dayName,
//                                        date = dateStr,
//                                        meals = meals
//                                    ))
//                                }
//                            }
//                        }
//                    }
//
//                    // Sort days chronologically
//                    val sortedDays = allDays.sortedBy { dayData ->
//                        // Try to parse date for sorting
//                        try {
//                            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
//                            formatter.parse(dayData.date)?.time ?: Long.MAX_VALUE
//                        } catch (e: Exception) {
//                            // If date parsing fails, use a default ordering
//                            Log.e("DateParsing", "Error parsing date: ${e.message}")
//                            Long.MAX_VALUE
//                        }
//                    }
//
//                    _weeklyMenuData.value = sortedDays
//                    Log.d("FirebaseData", "Processed ${sortedDays.size} days")
//                } else {
//                    Log.d("FirebaseData", "No data found in snapshot")
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("FirebaseError", "Error: ${error.message}")
//            }
//        })
//    }
//}
//
//// Keep existing data classes
//data class DayData(
//    val day: String,
//    val date: String,
//    val meals: List<Meal>
//)
//
//data class Meal(
//    val type: String = "",
//    val timing: String = "",
//    val items: List<String> = listOf(),
//    val hostels: Map<String, List<String>> = mapOf(),
//    val commonItems: List<String> = listOf()
//)
//
//data class WeekDay(
//    val day: String = "",
//    val date: String = "",
//    val meals: List<Meal> = listOf()
//)
//
//data class WeekData(
//    val weekNumber: Int = 0,
//    val days: List<WeekDay> = listOf()
//)
//
//// Keep existing font definitions
//val montserratFont = FontFamily(
//    androidx.compose.ui.text.font.Font(R.font.merriweatherregular)
//)
//val poppinsFont = FontFamily(
//    androidx.compose.ui.text.font.Font(R.font.merriweatherbold)
//)
//val playfairFont = FontFamily(
//    androidx.compose.ui.text.font.Font(R.font.merriweatherregular)
//)
//
//// Helper function to format dates (keeping existing function)
//private fun formatDateForDisplay(dateStr: String): String {
//    return try {
//        // Assuming input format is dd/MM/yyyy
//        val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
//        val outputFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
//        val date = inputFormat.parse(dateStr)
//        date?.let { outputFormat.format(it) } ?: dateStr
//    } catch (e: Exception) {
//        // If formatting fails, return the original string
//        dateStr
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun MealDetailsScreen(mealId: String?, navController: NavController) {
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    Text(
//                        mealId ?: "Meal Details",
//                        fontFamily = montserratFont
//                    )
//                },
//                navigationIcon = {
//                    IconButton(onClick = { /* Handle back navigation */ }) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
//        }
//    ) { padding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .padding(16.dp)
//        ) {
//            Text(
//                text = "Details for $mealId",
//                style = MaterialTheme.typography.headlineMedium.copy(
//                    fontWeight = FontWeight.Bold,
//                    fontFamily = poppinsFont
//                )
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//            Text(
//                text = "Here you can display more information about the selected meal.",
//                style = MaterialTheme.typography.bodyLarge.copy(
//                    fontFamily = playfairFont
//                )
//            )
//        }
//    }
//}
//
//@SuppressLint("NewApi")
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ModernWeeklyMenuApp(navController: NavController) {
//    // Function to find today's index in our days list
//    fun findTodayIndex(days: List<DayData>): Int {
//        if (days.isEmpty()) return 0
//
//        val calendar = Calendar.getInstance()
//        val dayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
//        val todayString = dayFormat.format(calendar.time)
//
//        // First try to find exact date match
//        val exactMatch = days.indexOfFirst { it.date == todayString }
//        if (exactMatch >= 0) return exactMatch
//
//        // If no exact match, find the closest upcoming day
//        try {
//            val today = dayFormat.parse(todayString)?.time ?: return 0
//
//            var closestIndex = 0
//            var smallestDiff = Long.MAX_VALUE
//
//            days.forEachIndexed { index, dayData ->
//                val dayDate = dayFormat.parse(dayData.date)?.time ?: return@forEachIndexed
//                val diff = dayDate - today
//
//                // Prioritize future dates (positive diff) over past dates
//                if (diff >= 0 && diff < smallestDiff) {
//                    smallestDiff = diff
//                    closestIndex = index
//                }
//            }
//
//            return closestIndex
//        } catch (e: Exception) {
//            Log.e("DateParsing", "Error finding today's index: ${e.message}")
//            return 0
//        }
//    }
//
//    val mealViewModel: MealViewModel = viewModel()
//    val daysData by mealViewModel.weeklyMenuData.observeAsState(emptyList())
//    var selectedDayIndex by remember { mutableStateOf(0) }
//
//    // Update selected day index when data loads
//    LaunchedEffect(daysData) {
//        if (daysData.isNotEmpty()) {
//            selectedDayIndex = findTodayIndex(daysData)
//        }
//    }
//
//    val context = LocalContext.current
//    val systemUiController = rememberSystemUiController()
//
//    // Set status bar color to match the TopAppBar background
//    SideEffect {
//        systemUiController.setStatusBarColor(color = Color(0xFF2E1D7D))
//    }
//
//    Surface(
//        modifier = Modifier.fillMaxSize(),
//        color = Color(0xFF1A103E)
//    ) {
//        Scaffold(
//            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
//            topBar = {
//                TopAppBar(
//                    title = {
//                        Text(
//                            "🍽️ CU Eats",
//                            style = MaterialTheme.typography.headlineMedium.copy(
//                                fontWeight = FontWeight.ExtraBold,
//                                fontSize = 28.sp,
//                                letterSpacing = 0.5.sp,
//                                fontFamily = playfairFont
//                            ),
//                            color = Color.White
//                        )
//                    },
//                    colors = TopAppBarDefaults.topAppBarColors(
//                        containerColor = Color(0xFF2E1D7D)
//                    ),
//                    actions = {
//                        IconButton(
//                            onClick = {
//                                try {
//                                    navController.navigate("notifications")
//                                } catch (e: Exception) {
//                                    Log.e("Navigation", "Error navigating to notifications: ${e.message}")
//                                    Toast.makeText(context, "Navigation error", Toast.LENGTH_SHORT).show()
//                                }
//                            }
//                        ) {
//                            Icon(
//                                Icons.Default.Notifications,
//                                contentDescription = "Notifications",
//                                tint = Color(0xFFFFD700),
//                                modifier = Modifier.size(28.dp)
//                            )
//                        }
//                        IconButton(
//                            onClick = {
//                                try {
//                                    navController.navigate("shops") {
//                                        launchSingleTop = true
//                                        restoreState = true
//                                    }
//                                } catch (e: Exception) {
//                                    Log.e("Navigation", "Detailed navigation error: ${e.message}")
//                                    Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
//                                }
//                            }
//                        ) {
//                            Icon(
//                                painter = painterResource(id = R.drawable.menu),
//                                contentDescription = "Shops",
//                                tint = Color(0xFFFFD700),
//                                modifier = Modifier.size(28.dp)
//                            )
//                        }
//                        IconButton(onClick = { navController.navigate("budget") }) {
//                            Icon(
//                                painter = painterResource(id = R.drawable.img),
//                                contentDescription = "Budget",
//                                tint = Color(0xFFFFD700),
//                                modifier = Modifier.size(28.dp)
//                            )
//                        }
//                    }
//                )
//            }
//        ) { padding ->
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(padding)
//                    .background(Color(0xFF1A103E))
//            ) {
//                Spacer(modifier = Modifier.height(16.dp))
//
//                if (daysData.isNotEmpty()) {
//                    // Scrollable Day Selector that starts from the closest current/future day
//                    LazyRow(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(horizontal = 16.dp),
//                        horizontalArrangement = Arrangement.spacedBy(8.dp)
//                    ) {
//                        itemsIndexed(daysData) { index, dayData ->
//                            DayCard(
//                                day = dayData.day,
//                                date = dayData.date,
//                                isSelected = index == selectedDayIndex,
//                                onClick = { selectedDayIndex = index }
//                            )
//                        }
//                    }
//
//                    LazyColumn(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(16.dp),
//                        verticalArrangement = Arrangement.spacedBy(16.dp)
//                    ) {
//                        val selectedDay = daysData[selectedDayIndex]
//                        items(selectedDay.meals) { meal ->
//                            MealCard(meal, navController)
//                        }
//                    }
//                } else {
//                    // Loading placeholders
//                    Column(
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .padding(16.dp),
//                        verticalArrangement = Arrangement.spacedBy(16.dp)
//                    ) {
//                        repeat(3) { // Show 3 shimmer placeholders for meal cards
//                            Card(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .height(120.dp)
//                                    .clip(RoundedCornerShape(12.dp))
//                                    .placeholder(
//                                        visible = true,
//                                        highlight = PlaceholderHighlight.shimmer()
//                                    ),
//                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1D7D))
//                            ) {
//                                Row(
//                                    modifier = Modifier
//                                        .fillMaxSize()
//                                        .padding(12.dp),
//                                    verticalAlignment = Alignment.CenterVertically
//                                ) {
//                                    Box(
//                                        modifier = Modifier
//                                            .size(80.dp)
//                                            .clip(RoundedCornerShape(10.dp))
//                                            .background(Color.Gray)
//                                            .placeholder(
//                                                visible = true,
//                                                highlight = PlaceholderHighlight.shimmer()
//                                            )
//                                    )
//
//                                    Spacer(modifier = Modifier.width(16.dp))
//
//                                    Column(
//                                        modifier = Modifier.weight(1f)
//                                    ) {
//                                        Box(
//                                            modifier = Modifier
//                                                .fillMaxWidth(0.7f)
//                                                .height(16.dp)
//                                                .clip(RoundedCornerShape(4.dp))
//                                                .background(Color.Gray)
//                                                .placeholder(
//                                                    visible = true,
//                                                    highlight = PlaceholderHighlight.shimmer()
//                                                )
//                                        )
//                                        Spacer(modifier = Modifier.height(8.dp))
//                                        Box(
//                                            modifier = Modifier
//                                                .fillMaxWidth(0.5f)
//                                                .height(14.dp)
//                                                .clip(RoundedCornerShape(4.dp))
//                                                .background(Color.Gray)
//                                                .placeholder(
//                                                    visible = true,
//                                                    highlight = PlaceholderHighlight.shimmer()
//                                                )
//                                        )
//                                    }
//                                }
//                            }
//                        }
//
//                        Text(
//                            text = "Fetching the latest meal plan... 🍽️",
//                            color = Color.White,
//                            fontSize = 18.sp,
//                            fontWeight = FontWeight.Bold,
//                            modifier = Modifier
//                                .padding(top = 16.dp)
//                                .align(Alignment.CenterHorizontally)
//                                .placeholder(
//                                    visible = true,
//                                    highlight = PlaceholderHighlight.shimmer()
//                                )
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//open class MealViewModel : ViewModel() {
//    private val _weeklyMenuData = MutableLiveData<List<DayData>>()
//    val weeklyMenuData: LiveData<List<DayData>> get() = _weeklyMenuData
//
//    init {
//        fetchDailyMenu()
//    }
//
//    private fun fetchDailyMenu() {
//        val database = FirebaseDatabase.getInstance("https://cu-eats-37fa0-default-rtdb.firebaseio.com/")
//            .reference.child("meals")
//
//        database.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                Log.d("FirebaseData", "Snapshot value: ${snapshot.value}")
//
//                if (snapshot.exists()) {
//                    val mealOrder = listOf("Breakfast", "Lunch", "Snacks", "Dinner")
//                    val dayOrder = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
//
//                    // Extract all day entries regardless of week
//                    val allDays = mutableListOf<DayData>()
//
//                    snapshot.children.forEach { weekSnapshot ->
//                        Log.d("FirebaseData", "Processing Week: ${weekSnapshot.key}")
//
//                        // Process each day in this week
//                        dayOrder.forEach { dayName ->
//                            val daySnapshot = weekSnapshot.child(dayName)
//                            if (daySnapshot.exists()) {
//                                // Get the date from the day data or use default
//                                val dateStr = daySnapshot.child("date").getValue(String::class.java) ?: "No date"
//
//                                // Get meals for this day
//                                val meals = daySnapshot.children.mapNotNull { mealSnapshot ->
//                                    // Skip the date field when processing meals
//                                    if (mealSnapshot.key == "date") return@mapNotNull null
//                                    mealSnapshot.getValue(Meal::class.java)
//                                }.sortedBy { mealOrder.indexOf(it.type) }
//
//                                // Only add days that have meal data
//                                if (meals.isNotEmpty()) {
//                                    allDays.add(
//                                        DayData(
//                                            day = dayName,
//                                            date = dateStr,
//                                            meals = meals
//                                        )
//                                    )
//                                }
//                            }
//                        }
//                    }
//
//                    // Sort days chronologically
//                    val sortedDays = allDays.sortedBy { dayData ->
//                        // Try to parse date for sorting
//                        try {
//                            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
//                            formatter.parse(dayData.date)?.time ?: Long.MAX_VALUE
//                        } catch (e: Exception) {
//                            // If date parsing fails, use a default ordering
//                            Log.e("DateParsing", "Error parsing date: ${e.message}")
//                            Long.MAX_VALUE
//                        }
//                    }
//
//                    _weeklyMenuData.value = sortedDays
//                    Log.d("FirebaseData", "Processed ${sortedDays.size} days")
//                } else {
//                    Log.d("FirebaseData", "No data found in snapshot")
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("FirebaseError", "Error: ${error.message}")
//            }
//        })
//    }
//}
//
//// New data class to replace WeekData and WeekDay
//data class DayData(
//    val day: String,
//    val date: String,
//    val meals: List<Meal>
//)
//
//@Composable
//fun DayCard(
//    day: String,
//    date: String,
//    isSelected: Boolean,
//    onClick: () -> Unit
//) {
//    Card(
//        modifier = Modifier
//            .width(100.dp)  // Increased width to accommodate date
//            .height(60.dp)  // Increased height to fit both day and date
//            .clickable(onClick = onClick),
//        colors = CardDefaults.cardColors(
//            containerColor = if (isSelected) Color(0xFFFF6B6B) else Color(0xFF3D2A8E)
//        ),
//        elevation = CardDefaults.cardElevation(
//            defaultElevation = if (isSelected) 8.dp else 2.dp
//        ),
//        shape = RoundedCornerShape(20.dp)
//    ) {
//        Column(
//            modifier = Modifier.fillMaxSize(),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center
//        ) {
//            Text(
//                text = day.uppercase(),
//                style = MaterialTheme.typography.bodyMedium.copy(
//                    fontWeight = FontWeight.Bold,
//                    fontSize = 16.sp,
//                    letterSpacing = 1.sp
//                ),
//                color = if (isSelected) Color.White else Color(0xFFB8A8FF)
//            )
//            Spacer(modifier = Modifier.height(4.dp))
//            Text(
//                text = formatDateForDisplay(date),
//                style = MaterialTheme.typography.bodySmall.copy(
//                    fontSize = 12.sp
//                ),
//                color = if (isSelected) Color.White else Color(0xFFB8A8FF)
//            )
//        }
//    }
//}
//
//// Helper function to format dates
//private fun formatDateForDisplay(dateStr: String): String {
//    return try {
//        // Assuming input format is dd/MM/yyyy
//        val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
//        val outputFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
//        val date = inputFormat.parse(dateStr)
//        date?.let { outputFormat.format(it) } ?: dateStr
//    } catch (e: Exception) {
//        // If formatting fails, return the original string
//        dateStr
//    }
//}
//
//
//data class Meal(
//    val type: String = "",
//    val timing: String = "",
//    val items: List<String> = listOf(),
//    val hostels: Map<String, List<String>> = mapOf(),
//    val commonItems: List<String> = listOf()
//)
//
//data class WeekDay(
//    val day: String = "",
//    val date: String = "",
//    val meals: List<Meal> = listOf()
//)
//
//data class WeekData(
//    val weekNumber: Int = 0,
//    val days: List<WeekDay> = listOf()
//)
//
//
//
//val montserratFont = FontFamily(
//    androidx.compose.ui.text.font.Font(R.font.merriweatherregular)
//)
//val poppinsFont = FontFamily(
//    androidx.compose.ui.text.font.Font(R.font.merriweatherbold)
//)
//val playfairFont =  FontFamily(
//    androidx.compose.ui.text.font.Font(R.font.merriweatherregular)
//)
//
//@Composable
//fun MealCard(meal: Meal, navController: NavController) {
//    val gradientColors = when (meal.type) {
//        "Breakfast" -> listOf(Color(0xFFB2DFDB), Color(0xFF009688)) // Softer teal
//        "Lunch" -> listOf(Color(0xFFFFE0B2), Color(0xFFFFA726))     // Gentle orange
//        "Snacks" -> listOf(Color(0xFFE1BEE7), Color(0xFF7B1FA2))    // Pastel purple
//        "Dinner" -> listOf(Color(0xFFBBDEFB), Color(0xFF1976D2))    // Light blue
//        else -> listOf(Color(0xFFCFD8DC), Color(0xFF607D8B))        // Neutral gray
//    }
//
//    val textColor = when (meal.type) {
//        "Breakfast" -> Color(0xFF00335D) // Dark teal for contrast
//        "Lunch" -> Color(0xFF4E343E)     // Dark brown
//        "Snacks" -> Color(0xFF311B92)    // Deep indigo
//        "Dinner" -> Color(0xFF0D47A1)    // Dark blue
//        else -> Color(0xFF263238)        // Dark gray
//    }
//
//    val mealEmoji = when (meal.type) {
//        "Breakfast" -> "🥗"
//        "Lunch" -> "🍚"
//        "Snacks" -> "🌮"
//        else -> "🍽️"
//    }
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clip(RoundedCornerShape(24.dp)),
//        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//    ) {
//        Box(
//            modifier = Modifier
//                .background(Brush.horizontalGradient(gradientColors))
//                .padding(16.dp)
//        ) {
//            Column {
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        Text(
//                            text = mealEmoji,
//                            fontSize = 32.sp
//                        )
//                        Spacer(modifier = Modifier.width(12.dp))
//                        Column {
//                            Text(
//                                text = meal.type.uppercase(),
//                                style = MaterialTheme.typography.titleLarge.copy(
//                                    fontWeight = FontWeight.ExtraBold,
//                                    letterSpacing = 1.sp,
//                                    color = textColor,
//                                    fontSize = 20.sp,
//                                    fontFamily = montserratFont // Applied Built-in Font
//                                ),
//                                color = textColor
//                            )
//                            Text(
//                                text = meal.timing,
//                                style = MaterialTheme.typography.bodyMedium.copy(
//                                    fontWeight = FontWeight.SemiBold,
//                                    fontSize = 14.sp,
//                                    fontFamily = poppinsFont // Applied Built-in Font
//                                ),
//                                color = textColor.copy(alpha = 0.7f)
//                            )
//                        }
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                val sortedMeals = meal.items.sorted()
//                sortedMeals.forEach { item ->
//                    Row(
//                        modifier = Modifier.padding(vertical = 4.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Box(
//                            modifier = Modifier
//                                .size(8.dp)
//                                .clip(RoundedCornerShape(4.dp))
//                                .background(textColor)
//                        )
//                        Spacer(modifier = Modifier.width(12.dp))
//                        Text(
//                            text = item,
//                            style = MaterialTheme.typography.bodyLarge.copy(
//                                fontWeight = FontWeight.Medium,
//                                fontSize = 18.sp,
//                                fontFamily = playfairFont // Applied Built-in Font
//                            ),
//                            color = textColor
//                        )
//                    }
//                }
//
//                if (meal.hostels.isNotEmpty()) {
//                    meal.hostels.forEach { (hostelName, hostelItems) ->
//                        Text(
//                            text = "$hostelName:",
//                            style = MaterialTheme.typography.bodyLarge.copy(
//                                fontWeight = FontWeight.Bold,
//                                color = textColor,
//                                fontSize = 18.sp,
//                                fontFamily = poppinsFont // Applied Built-in Font
//                            )
//                        )
//                        hostelItems.forEach { item ->
//                            Text(
//                                text = "- $item",
//                                style = MaterialTheme.typography.bodyLarge.copy(
//                                    color = textColor,
//                                    fontSize = 17.sp,
//                                    fontFamily = playfairFont // Applied Built-in Font
//                                ),
//                                modifier = Modifier.padding(start = 16.dp)
//                            )
//                        }
//                    }
//                }
//
//                if (meal.commonItems.isNotEmpty()) {
//                    Text(
//                        text = "Common Items:",
//                        style = MaterialTheme.typography.bodyLarge.copy(
//                            fontWeight = FontWeight.Bold,
//                            color = textColor,
//                            fontSize = 18.sp,
//                            fontFamily = montserratFont // Applied Built-in Font
//                        )
//                    )
//                    meal.commonItems.forEach { item ->
//                        Text(
//                            text = "- $item",
//                            style = MaterialTheme.typography.bodyLarge.copy(
//                                color = textColor,
//                                fontSize = 17.sp,
//                                fontFamily = playfairFont // Applied Built-in Font
//                            ),
//                            modifier = Modifier.padding(start = 16.dp)
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun MealDetailsScreen(mealId: String?, navController: NavController) {
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    Text(
//                        mealId ?: "Meal Details",
//                        fontFamily = montserratFont // Applied Built-in Font
//                    )
//                },
//                navigationIcon = {
//                    IconButton(onClick = { /* Handle back navigation */ }) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
//        }
//    ) { padding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .padding(16.dp)
//        ) {
//            Text(
//                text = "Details for $mealId",
//                style = MaterialTheme.typography.headlineMedium.copy(
//                    fontWeight = FontWeight.Bold,
//                    fontFamily = poppinsFont // Applied Built-in Font
//                )
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//            Text(
//                text = "Here you can display more information about the selected meal.",
//                style = MaterialTheme.typography.bodyLarge.copy(
//                    fontFamily = playfairFont // Applied Built-in Font
//                )
//            )
//        }
//    }
//}