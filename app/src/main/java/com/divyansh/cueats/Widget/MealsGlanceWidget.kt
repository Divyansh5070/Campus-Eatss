package com.divyansh.cueats.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AssistChipDefaults.IconSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.unit.ColorProvider
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.action.clickable
import androidx.glance.layout.ContentScale

import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


// Widget Receiver
class MealsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MealsGlanceWidget()
}

// Main Widget Class
class MealsGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                MealsWidgetContent(context)
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
fun MealsWidgetContent(context: Context) {
    val todayMeals = getTodayMealsFromCache(context)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFFF6F7FB)))
            .padding(16.dp)
    ) {
        // Header with app navigation arrow
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Text(
                text = "🍽️",
                style = TextStyle(fontSize = 20.sp)
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Title centered using weight
            Text(
                text = "Today's Meals",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color(0xFFFF6B01))
                ),
                modifier = GlanceModifier.defaultWeight() // ✅ pushes equally left & right
            )

            // Navigation arrow
            Box(
                modifier = GlanceModifier
                    .size(32.dp)
                    .background(ColorProvider(Color(0xFFFF6B01)))
                    .cornerRadius(16.dp)
                    .clickable(actionStartActivity(getLaunchIntent(context))),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(com.divyansh.cueats.R.drawable.ic_arrow_forward),
                    contentDescription = "Next",
                    modifier = GlanceModifier.size(20.dp)
                )
            }
        }


        // Date
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = getCurrentDateString(),
                style = TextStyle(
                    fontSize = 14.sp,
                    color = ColorProvider(Color(0xFF718096))
                )
            )
        }

        // Meals Content
        if (todayMeals.isNotEmpty()) {
            LazyColumn(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                items(todayMeals) { meal ->
                    EnhancedMealWidgetCard(meal = meal, context = context)
                    Spacer(modifier = GlanceModifier.height(12.dp))
                }
            }
        } else {
            EmptyMealsCard(context)
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
fun EnhancedMealWidgetCard(meal: WidgetMealData, context: Context) {
    val mealIcon = getMealIcon(meal.type)
    val accentColor = getMealColor(meal.type)
    val subtleColor = getMealSubtleColor(meal.type)

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(Color.White))
            .cornerRadius(12.dp)
            .padding(16.dp)
    ) {
        // Header Section
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Meal Info
            Column(
                modifier = GlanceModifier.defaultWeight()
            ) {
                Text(
                    text = meal.type.replaceFirstChar { it.uppercase() },
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(accentColor)
                    )
                )
                if (meal.time.isNotEmpty()) {
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = meal.time,
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = ColorProvider(Color(0xFF718096))
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(12.dp))

        // Accent line - Simplified approach
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(2.dp)
                .background(ColorProvider(getMealLightAccentColor(meal.type)))
                .cornerRadius(1.dp)
        ) {}

        Spacer(modifier = GlanceModifier.height(12.dp))

        // Menu Items - Using simple Column layout
        if (meal.items.isNotEmpty()) {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                val itemsToShow = meal.items.take(4) // Limit to 4 items to prevent overflow
                itemsToShow.forEach { item ->
                    MealItemChip(
                        text = item,
                        backgroundColor = ColorProvider(subtleColor)
                    )
                    if (item != itemsToShow.last()) {
                        Spacer(modifier = GlanceModifier.height(4.dp))
                    }
                }
            }

            // Items count if there are many - now clickable
            if (meal.items.size > 4) {
                Spacer(modifier = GlanceModifier.height(8.dp))
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .clickable(actionStartActivity(getLaunchIntent(context))),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "and ${meal.items.size - 4} more items",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = ColorProvider(Color(0xFFFF6B01)),
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Spacer(modifier = GlanceModifier.width(4.dp))

                    Image(
                        provider = ImageProvider(com.divyansh.cueats.R.drawable.ic_arrow_forward), // your material right arrow drawable
                        contentDescription = "More items",
                        modifier = GlanceModifier.size(14.dp)
                    )
                }
            }

        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
fun MealItemChip(
    text: String,
    backgroundColor: ColorProvider
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(backgroundColor)
            .cornerRadius(16.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 13.sp,
                color = ColorProvider(Color(0xFF2D3748)),
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@SuppressLint("RestrictedApi")
@Composable
fun EmptyMealsCard(context: Context) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(Color.White))
            .cornerRadius(12.dp)
            .padding(32.dp)
            .clickable(actionStartActivity(getLaunchIntent(context))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🍽️",
            style = TextStyle(fontSize = 32.sp)
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "No meals available",
            style = TextStyle(
                fontSize = 16.sp,
                color = ColorProvider(Color(0xFF718096)),
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "Tap to open app and check",
            style = TextStyle(
                fontSize = 14.sp,
                color = ColorProvider(Color(0xFFFF6B01))
            )
        )
    }
}

// Helper function to get app launch intent
fun getLaunchIntent(context: Context): Intent {
    return context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?: Intent().apply {
            // Fallback if launch intent is not found
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(context.packageName)
        }
}

// Data class for widget
data class WidgetMealData(
    val type: String,
    val time: String,
    val items: List<String>
)

// Helper functions
fun getMealIcon(mealType: String): String {
    return when (mealType.lowercase()) {
        "breakfast" -> "🍳"
        "lunch" -> "🍛"
        "snacks" -> "🧁"
        "dinner" -> "🍽️"
        else -> "🍴"
    }
}

fun getMealColor(mealType: String): Color {
    return when (mealType.lowercase()) {
        "breakfast" -> Color(0xFFFFB74D)
        "lunch" -> Color(0xFF64B5F6)
        "snacks" -> Color(0xFFBA68C8)
        "dinner" -> Color(0xFF81C784)
        else -> Color(0xFFFF8A65)
    }
}

fun getMealSubtleColor(mealType: String): Color {
    return when (mealType.lowercase()) {
        "breakfast" -> Color(0xFFFFF8E1)
        "lunch" -> Color(0xFFE1F5FE)
        "snacks" -> Color(0xFFF3E5F5)
        "dinner" -> Color(0xFFE8F5E9)
        else -> Color(0xFFFBE9E7)
    }
}

fun getMealLightAccentColor(mealType: String): Color {
    return when (mealType.lowercase()) {
        "breakfast" -> Color(0xFFFFE0B2)
        "lunch" -> Color(0xFFBBDEFB)
        "snacks" -> Color(0xFFE1BEE7)
        "dinner" -> Color(0xFFC8E6C9)
        else -> Color(0xFFFFCCBC)
    }
}

fun getCurrentDateString(): String {
    val calendar = Calendar.getInstance()
    val formatter = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
    return formatter.format(calendar.time)
}

// Enhanced cache mechanism for widget
fun getTodayMealsFromCache(context: Context): List<WidgetMealData> {
    val prefs = context.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
    val todayDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)

    // Try to get cached data for today
    val cachedData = prefs.getString("meals_$todayDate", null)

    if (cachedData != null) {
        return parseCachedMeals(cachedData)
    }

    // If no cache, trigger data fetch and return empty list
    fetchTodayMealsData(context)
    return emptyList()
}

fun parseCachedMeals(cachedData: String): List<WidgetMealData> {
    val meals = mutableListOf<WidgetMealData>()

    try {
        // Enhanced parsing: "MealType|Time|Item1,Item2,Item3;NextMeal..."
        cachedData.split(";").forEach { mealString ->
            if (mealString.isNotBlank()) {
                val parts = mealString.split("|")
                if (parts.size >= 3) {
                    val type = parts[0]
                    val time = parts[1]
                    val items = parts[2].split(",").filter { it.isNotBlank() }
                    meals.add(WidgetMealData(type, time, items))
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("WidgetParser", "Error parsing cached meals: ${e.message}")
        return emptyList()
    }

    // Sort meals by typical meal order
    val mealOrder = listOf("breakfast", "lunch", "snacks", "dinner")
    return meals.sortedBy { meal ->
        mealOrder.indexOf(meal.type.lowercase()).takeIf { it >= 0 } ?: Int.MAX_VALUE
    }
}

fun fetchTodayMealsData(context: Context) {
    val database = FirebaseDatabase.getInstance("https://cu-eats-37fa0-default-rtdb.firebaseio.com/")
    val mealsRef = database.reference.child("meals")

    val todayDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
    val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(Calendar.getInstance().time)

    mealsRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val meals = mutableListOf<WidgetMealData>()

            try {
                // Find today's meals
                snapshot.children.forEach { weekSnapshot ->
                    val daySnapshot = weekSnapshot.child(dayOfWeek)
                    if (daySnapshot.exists()) {
                        val date = daySnapshot.child("date").getValue(String::class.java)

                        if (date == todayDate) {
                            // Extract meals for today
                            daySnapshot.children.forEach { mealSnapshot ->
                                if (mealSnapshot.key != "date") {
                                    val type = mealSnapshot.key ?: ""

                                    // Get timing from multiple possible fields
                                    val time = mealSnapshot.child("time").getValue(String::class.java)
                                        ?: mealSnapshot.child("timing").getValue(String::class.java)
                                        ?: ""

                                    val items = mutableListOf<String>()

                                    // Get all items from different sources
                                    mealSnapshot.child("items").children.forEach { item ->
                                        item.getValue(String::class.java)?.let {
                                            if (it.isNotBlank()) items.add(it)
                                        }
                                    }

                                    mealSnapshot.child("commonItems").children.forEach { item ->
                                        item.getValue(String::class.java)?.let {
                                            if (it.isNotBlank()) items.add(it)
                                        }
                                    }

                                    // Get hostel-specific items
                                    mealSnapshot.child("hostels").children.forEach { hostel ->
                                        hostel.children.forEach { item ->
                                            item.getValue(String::class.java)?.let {
                                                if (it.isNotBlank()) items.add(it)
                                            }
                                        }
                                    }

                                    if (items.isNotEmpty()) {
                                        meals.add(WidgetMealData(
                                            type = type.replaceFirstChar { it.uppercase() },
                                            time = time,
                                            items = items.distinct() // Remove duplicates
                                        ))
                                    }
                                }
                            }
                        }
                    }
                }

                // Cache the enhanced data
                cacheMealsData(context, todayDate, meals)

            } catch (e: Exception) {
                android.util.Log.e("WidgetFetch", "Error processing meal data: ${e.message}")
            }
        }

        override fun onCancelled(error: DatabaseError) {
            android.util.Log.e("WidgetFetch", "Database error: ${error.message}")
        }
    })
}

fun cacheMealsData(context: Context, date: String, meals: List<WidgetMealData>) {
    val prefs = context.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
    val editor = prefs.edit()

    try {
        // Convert meals to enhanced string format
        val cacheString = meals.joinToString(";") { meal ->
            "${meal.type}|${meal.time}|${meal.items.joinToString(",")}"
        }

        editor.putString("meals_$date", cacheString)
        editor.putLong("cache_timestamp_$date", System.currentTimeMillis())
        editor.apply()

        android.util.Log.d("WidgetCache", "Cached ${meals.size} meals for $date")
    } catch (e: Exception) {
        android.util.Log.e("WidgetCache", "Error caching meals: ${e.message}")
    }
}