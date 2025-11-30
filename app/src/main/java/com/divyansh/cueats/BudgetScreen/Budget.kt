package com.divyansh.cueats.BudgetScreen

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage

import androidx.compose.ui.platform.LocalContext
import com.divyansh.cueats.AppBottomNavigation
import com.divyansh.cueats.Mess.playfairFont
import com.divyansh.cueats.R



@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BudgetRecommendationScreen(navController: NavController) {
    // Check system theme
    val isLightTheme = !isSystemInDarkTheme()

    // Define colors based on current theme
    val primaryOrange = Color(0xFFFF6B01)
    val textPrimaryColor = if (isLightTheme) Color(0xFF212121) else Color(0xFFF0F0F0)
    val textSecondaryColor = if (isLightTheme) Color(0xFF757575) else Color(0xFFBBBBBB)
    val backgroundColor = if (isLightTheme) Color(0xFFF5F5F5) else Color(0xFF121212)
    val cardBackground = if (isLightTheme) Color.White else Color(0xFF1E1E2C)
    val surfaceColor = if (isLightTheme) Color(0xFFEEEEEE) else Color(0xFF202020)

    var budget by remember { mutableStateOf("100") }
    var isVegetarian by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(true) }
    var filteredDishes by remember { mutableStateOf<List<Dish>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableStateOf(0.4f) }

    val currentRoute = navController.currentBackStackEntry?.destination?.route ?: "budget"

    // Add location filters
    var selectedLocation by remember { mutableStateOf("All Locations") }
    var isHostelFilter by remember { mutableStateOf(false) }
    var isCampusFilter by remember { mutableStateOf(false) }

    // Initialize with some dishes when the screen loads
    LaunchedEffect(Unit) {
        // Get initial dishes
        val allDishes = DishRepository.getAllDishes()
        val budgetValue = budget.toIntOrNull() ?: 100

        // Filter by initial budget, vegetarian preference and location filters
        val initialDishes = allDishes.filter { dish ->
            val matchesBudget = dish.price <= budgetValue
            val matchesVegPref = !isVegetarian || dish.isVegetarian
            val matchesLocationFilter = (!isHostelFilter && !isCampusFilter) ||
                    (isHostelFilter && dish.location.contains("NC")) ||
                    (isCampusFilter && dish.location.contains("Zakir"))||
                    (isCampusFilter && dish.location.contains("LC")) ||
                    (isCampusFilter && dish.location.contains("North Campus"))||
                    (isCampusFilter && dish.location.contains("South Campus"))

            matchesBudget && matchesVegPref && matchesLocationFilter
        }.sortedByDescending { it.rating }

        filteredDishes = initialDishes
    }

    // Update filtered dishes when any filter changes
    LaunchedEffect(budget, isVegetarian, isHostelFilter, isCampusFilter) {
        val budgetValue = budget.toIntOrNull() ?: 0
        val allDishes = DishRepository.getAllDishes()

        val filtered = allDishes.filter { dish ->
            val matchesBudget = dish.price <= budgetValue
            val matchesVegPref = !isVegetarian || dish.isVegetarian
            val matchesLocationFilter = (!isHostelFilter && !isCampusFilter) ||
                    (isHostelFilter && dish.location.contains("NC")) ||
                    (isCampusFilter && dish.location.contains("Zakir"))||
                    (isCampusFilter && dish.location.contains("LC")) ||
                    (isCampusFilter && dish.location.contains("North Campus"))||
                    (isCampusFilter && dish.location.contains("South Campus"))

            matchesBudget && matchesVegPref && matchesLocationFilter
        }.sortedByDescending { it.rating }

        filteredDishes = filtered
    }

    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current

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
            )
        },

        bottomBar = {
            AppBottomNavigation(navController = navController, currentRoute = "budget")
        }

    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                // Page Title Section
                Text(
                    text = "Budget Planner",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )

                Text(
                    text = "Find the best food options within your budget",
                    fontSize = 16.sp,
                    color = textSecondaryColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Budget Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardBackground
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Your Budget",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimaryColor
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "₹$budget",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryOrange
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Slider
                        Slider(
                            value = sliderPosition,
                            onValueChange = {
                                sliderPosition = it
                                budget = ((20 + (it * 180)).toInt()).toString()
                            },
                            steps = 17, // Because (180 / 10) - 1 = 17 steps between 20 and 200
                            colors = SliderDefaults.colors(
                                thumbColor = primaryOrange,
                                activeTrackColor = primaryOrange,
                                inactiveTrackColor = if (isLightTheme) Color(0xFFDDDDDD) else Color(0xFF3A3A3A)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "₹20",
                                fontSize = 14.sp,
                                color = textSecondaryColor
                            )
                            Text(
                                text = "₹200",
                                fontSize = 14.sp,
                                color = textSecondaryColor
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Filters section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Filters",
                                tint = textSecondaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Filters:",
                                fontSize = 16.sp,
                                color = textSecondaryColor
                            )
                            Spacer(modifier = Modifier.weight(1f))

                            // Keep the original Vegetarian checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isVegetarian,
                                    onCheckedChange = { isVegetarian = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = primaryOrange,
                                        uncheckedColor = textSecondaryColor
                                    )
                                )
                                Text(
                                    text = "Veg Only",
                                    fontSize = 14.sp,
                                    color = textPrimaryColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Location filter chips row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Hostel filter chip
                            FilterChip(
                                selected = isHostelFilter,
                                onClick = { isHostelFilter = !isHostelFilter },
                                label = {
                                    Text(
                                        "Hostel",
                                        fontSize = 14.sp,
                                        color = if (isHostelFilter) primaryOrange else textPrimaryColor
                                    )
                                },
                                leadingIcon = {
                                    if (isHostelFilter) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryOrange.copy(alpha = 0.2f),
                                    selectedLabelColor = primaryOrange,
                                    selectedLeadingIconColor = primaryOrange
                                )
                            )

                            // Campus filter chip
                            FilterChip(
                                selected = isCampusFilter,
                                onClick = { isCampusFilter = !isCampusFilter },
                                label = {
                                    Text(
                                        "Campus",
                                        fontSize = 14.sp,
                                        color = if (isCampusFilter) primaryOrange else textPrimaryColor
                                    )
                                },
                                leadingIcon = {
                                    if (isCampusFilter) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryOrange.copy(alpha = 0.2f),
                                    selectedLabelColor = primaryOrange,
                                    selectedLeadingIconColor = primaryOrange
                                )
                            )
                        }
                    }
                }

                // Render results or no results message
                if (filteredDishes.isNotEmpty()) {
                    // Use the integrated DishResultsContent to show dishes
                    DishResultsContent(dishes = filteredDishes)
                } else {
                    // Show the NoResultsCard when no dishes match criteria
                    NoResultsCard()
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Show loading indicator when searching
            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = primaryOrange
                    )
                }
            }
        }
    }
}

@Composable
fun NoResultsCard() {
    val isLightTheme = !isSystemInDarkTheme()
    val textColorPrimary = if (isLightTheme) Color(0xFF212121) else Color(0xFFF0F0F0)
    val textColorSecondary = if (isLightTheme) Color(0xFF757575) else Color(0xFFBBBBBB)
    val cardBackground = if (isLightTheme) Color.White else Color(0xFF1E1E2C)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "No results",
                modifier = Modifier
                    .size(64.dp)
                    .padding(8.dp),
                tint = Color(0xFFFF7F24)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No dishes found",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColorPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Try increasing your budget or changing filters",
                fontSize = 15.sp,
                color = textColorSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
// Helper function to perform the search operation
// Helper function to perform the search operation
private fun performSearch(
    budget: String,
    isVegetarian: Boolean,
    selectedLocation: String,
    keyboardController: SoftwareKeyboardController?,
    setSearching: (Boolean) -> Unit,
    setFilteredDishes: (List<Dish>) -> Unit,
    setShowResults: (Boolean) -> Unit
) {
    keyboardController?.hide()
    setSearching(true)

    // Parse budget safely
    val budgetValue = budget.toIntOrNull() ?: 0

    // Get dishes from your repository or data source
    val allDishes = DishRepository.getAllDishes() // Adjust based on your implementation

    // Filter dishes by budget, veg status, AND location
    val filtered = allDishes.filter { dish ->
        val matchesBudget = dish.price <= budgetValue
        val matchesVegPref = !isVegetarian || dish.isVegetarian
        val matchesLocation = selectedLocation == "All Locations" || dish.location == selectedLocation

        matchesBudget && matchesVegPref && matchesLocation
    }

    // Sort the filtered dishes by rating in descending order (highest rating first)
    val sortedDishes = filtered.sortedByDescending { it.rating }

    setFilteredDishes(sortedDishes)
    setShowResults(true)
    setSearching(false)
}

@Composable
fun DishResultsContent(dishes: List<Dish>) {
    val textColorPrimary = if (isSystemInDarkTheme()) Color(0xFFE0E0E0) else Color(0xFF333333)
    val textColorSecondary = if (isSystemInDarkTheme()) Color(0xFFB0B0B0) else Color(0xFF666666)

    // State for tracking the current sort option
    var sortOption by remember { mutableStateOf(SortOption.NONE) }

    // Apply sorting based on the selected option
    val sortedDishes = when (sortOption) {
        SortOption.RATING_HIGH_TO_LOW -> dishes.sortedByDescending { it.rating }
        SortOption.RATING_LOW_TO_HIGH -> dishes.sortedBy { it.rating }
        SortOption.PRICE_HIGH_TO_LOW -> dishes.sortedByDescending { it.price }
        SortOption.PRICE_LOW_TO_HIGH -> dishes.sortedBy { it.price }
        SortOption.NONE -> dishes
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Results header with count and sort options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Found ${dishes.size} dishes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColorPrimary
            )

            SortFilterButton(
                currentSortOption = sortOption,
                onSortOptionSelected = { sortOption = it }
            )
        }

        // Dish items list
        sortedDishes.forEach { dish ->
            DishItem(dish = dish)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Enum to represent sorting options
enum class SortOption {
    NONE,
    RATING_HIGH_TO_LOW,
    RATING_LOW_TO_HIGH,
    PRICE_HIGH_TO_LOW,
    PRICE_LOW_TO_HIGH
}

@Composable
fun SortFilterButton(
    currentSortOption: SortOption,
    onSortOptionSelected: (SortOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val iconTint = if (isSystemInDarkTheme()) Color(0xFFE0E0E0) else Color(0xFF333333)

    Box {
        // Sort button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Sort",
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when (currentSortOption) {
                    SortOption.NONE -> "Sort"
                    SortOption.RATING_HIGH_TO_LOW -> "Rating: High to Low"
                    SortOption.RATING_LOW_TO_HIGH -> "Rating: Low to High"
                    SortOption.PRICE_HIGH_TO_LOW -> "Price: High to Low"
                    SortOption.PRICE_LOW_TO_HIGH -> "Price: Low to High"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = iconTint
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        // Dropdown menu - using Material3 surface colors to ensure proper contrast
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(200.dp)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            SortMenuItem("Rating: High to Low", SortOption.RATING_HIGH_TO_LOW, currentSortOption, onSortOptionSelected) {
                expanded = false
            }
            SortMenuItem("Rating: Low to High", SortOption.RATING_LOW_TO_HIGH, currentSortOption, onSortOptionSelected) {
                expanded = false
            }
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            SortMenuItem("Price: High to Low", SortOption.PRICE_HIGH_TO_LOW, currentSortOption, onSortOptionSelected) {
                expanded = false
            }
            SortMenuItem("Price: Low to High", SortOption.PRICE_LOW_TO_HIGH, currentSortOption, onSortOptionSelected) {
                expanded = false
            }
        }
    }
}

@Composable
fun SortMenuItem(
    text: String,
    option: SortOption,
    currentOption: SortOption,
    onSelect: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    val isSelected = option == currentOption

    // Use Material3 colorScheme for proper visibility in both themes
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        onClick = {
            onSelect(option)
            onDismiss()
        },
        colors = MenuDefaults.itemColors(
            textColor = textColor,
            leadingIconColor = textColor,
            trailingIconColor = textColor
        )
    )
}

data class Seller(
    val id: Int,
    val name: String,
    val phoneNumber: String,
    val whatsappNumber: String
)


data class Dish(
    val id: Int,
    val name: String,
    val shop: String,
    val price: Double,
    val isVegetarian: Boolean,
    val rating: Float,
    val location: String,
    val description: String, // Dish description
    val imageUrl: String,
    val seller: Seller
)



@Composable
fun DishItem(dish: Dish) {
    var isExpanded by remember { mutableStateOf(false) }
    val isLightTheme = !isSystemInDarkTheme()
    val backgroundColor = if (isLightTheme) Color.White else Color(0xFF1D1D1D)
    val textColorPrimary = if (isLightTheme) Color(0xFF212121) else Color(0xFFF5F5F5)
    val textColorSecondary = if (isLightTheme) Color(0xFF757575) else Color(0xFFBDBDBD)
    val cardElevation = animateDpAsState(
        targetValue = if (isExpanded) 8.dp else 4.dp,
        animationSpec = tween(durationMillis = 200),
        label = "cardElevation"
    )
    val context = LocalContext.current


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .shadow(
                elevation = cardElevation.value,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color(0x40000000)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
        ) {
            // Dish image at the top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                AsyncImage(
                    model = dish.imageUrl,
                    contentDescription = dish.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Rating badge at top-right corner
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x88000000))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Rating",
                            tint = when {
                                dish.rating >= 4.5 -> Color(0xFFFFD700)
                                dish.rating >= 4.0 -> Color(0xFFFFC107)
                                else -> Color(0xFFFFE082)
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = dish.rating.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Veg/Non-veg indicator at top-left
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                        .border(
                            width = 1.dp,
                            color = if (dish.isVegetarian) Color(0xFF4CAF50) else Color(0xFFF44336),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (dish.isVegetarian) Color(0xFF4CAF50) else Color(0xFFF44336))
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Dish name
                Text(
                    text = dish.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.25.sp,
                    color = textColorPrimary,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Price and shop name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${dish.price}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLightTheme) MaterialTheme.colorScheme.primary else Color(0xFF81C784)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "Location",
                            modifier = Modifier.size(16.dp),
                            tint = textColorSecondary
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = dish.shop,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColorSecondary
                        )
                    }
                }
            }

            // Dropdown Content - Now using animateContentSize instead of AnimatedVisibility
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Divider(
                        color = if (isSystemInDarkTheme()) Color(0xFF444444) else Color(0xFFEEEEEE)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Popular tags
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PopularityTag("Popular")
                        if (dish.rating >= 4.5) {
                            PopularityTag("Highly Rated")
                        }
                        if (dish.price <= 70) {
                            PopularityTag("Budget Friendly")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description text
                    Text(
                        text = dish.description,
                        fontSize = 14.sp,
                        color = textColorSecondary,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))


                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { isExpanded = false },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Got it")
                        }

                        // Action icons in a row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Call button
                            IconButton(
                                onClick = {
                                    try {
                                        // Get phone number from seller
                                        val phoneNumber = dish.seller.phoneNumber.trim()

                                        // Create intent to open the dialer with the number
                                        val intent = Intent(Intent.ACTION_DIAL)
                                        intent.data = android.net.Uri.parse("tel:$phoneNumber")

                                        // Start the activity
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.img_4),  // Using built-in call icon
                                    contentDescription = "Call Seller",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // WhatsApp button
                            IconButton(
                                onClick = {
                                    try {
                                        // Format the number (remove spaces and special characters)
                                        val formattedNumber = dish.seller.whatsappNumber.trim()
                                            .replace(" ", "")
                                            .replace("-", "")
                                            .replace("+", "")

                                        // Create intent with a direct package reference
                                        val intent = Intent(Intent.ACTION_VIEW)
                                        intent.setPackage("com.whatsapp")
                                        intent.data = android.net.Uri.withAppendedPath(
                                            android.net.Uri.parse("smsto:$formattedNumber"),
                                            ""
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.whatsapp_logo),
                                    contentDescription = "Chat on WhatsApp",
                                    tint = Color.Unspecified,  // Keep original icon colors
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                    }
                }
            }
        }
    }


@Composable
fun PopularityTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(
                color = if (isSystemInDarkTheme())
                    Color(0xFF333333)
                else
                    Color(0xFFF5F5F5)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSystemInDarkTheme())
                Color(0xFFE0E0E0)
            else
                Color(0xFF616161)
        )
    }
}

@Composable
fun FancyChip(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    iconVector: ImageVector
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}



