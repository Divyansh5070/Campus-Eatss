package com.divyansh.cueats.ShopsScreen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.material.icons.filled.*
import coil.compose.AsyncImage
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import coil.request.ImageRequest
import com.divyansh.cueats.AppBottomNavigation
import com.divyansh.cueats.CampusMapRoute
import com.divyansh.cueats.R
import com.divyansh.cueats.ShopMenuRoute
import com.google.accompanist.systemuicontroller.rememberSystemUiController

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.lifecycle.viewmodel.compose.viewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopMenuScreen(navController: NavController) {
    // Add offers state
    var showOffersSheet by remember { mutableStateOf(false) }
    val offerViewModel: OfferViewModel = viewModel()
    val offers by offerViewModel.offers.observeAsState(emptyList())

    // Use system theme colors properly
    val colorScheme =  lightColorScheme()
    val primaryColor = Color(0xFF47B44C)

    // Fix: Use proper theme-aware colors instead of hardcoded ones
    val backgroundColor =  Color.White
    val surfaceColor = colorScheme.surface
    val onSurfaceColor = colorScheme.onSurface
    val onSurfaceVariantColor = colorScheme.onSurfaceVariant
    val systemUiController = rememberSystemUiController()

    val primaryBlue = Color(0xFF42A5F5)


    val ratingViewModel: RatingViewModel = viewModel() // Add this line
    val shopRatings by ratingViewModel.shopRatings.collectAsState() // Add this line

    // Optimize: Use key to prevent re-initialization on recomposition
    LaunchedEffect("rating_init") {
        Log.d("ShopMenuScreen", "Initializing rating system")
        ratingViewModel.initializeRatingSystem()
    }

    val shopViewModel: ShopViewModel = viewModel()
    val shops by shopViewModel.shops.observeAsState(emptyList())

    // UPDATED FAVORITE VIEWMODEL INITIALIZATION
    val context = LocalContext.current
    val favoriteViewModel: FavoriteViewModel = viewModel(
        factory = FavoriteViewModelFactory(context)
    )
    val favoriteShops by favoriteViewModel.favoriteShops.collectAsState()
    var showFavoritesOnly by remember { mutableStateOf(false) }

    SideEffect {
        systemUiController.setStatusBarColor(color = primaryBlue)
    }

    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var isVegMode by remember { mutableStateOf(false) }

    // Add address filter state
    var selectedAddressFilter by remember { mutableStateOf("All") }

    // Address filter options
    val addressFilters = listOf(
        "All", "NC Boys","Zakir Boys","Near C3", "Near C1", "Food Republic","Near B3","Near A1","Near Fountain"

    )

    // Updated filtering logic to include address filtering
    // In your ShopMenuScreen Composable, replace the existing filteredShops with:

    // PERFORMANCE FIX: Use derivedStateOf to reduce recompositions
    val filteredShops by remember {
        derivedStateOf {
            shopViewModel.getFilteredAndRecommendedShops(
                shops = shops,
                favoriteShops = favoriteShops,
                selectedFilter = selectedFilter,
                selectedAddressFilter = selectedAddressFilter,
                searchQuery = searchQuery,
                showFavoritesOnly = showFavoritesOnly
            )
        }
    }
    // PERFORMANCE FIX: Use derivedStateOf for expensive category calculation
    val availableCategories by remember {
        derivedStateOf {
            val allMenuCategories = shops.flatMap { shop ->
                shop.menuItems.map { it.category }
            }.distinct()

            val predefinedCategoriesWithImages = listOf(
                CategoryItem("Biryani", "https://img.freepik.com/free-psd/bowl-biryani-with-chicken-pieces-transparent-background_84443-1312.jpg?ga=GA1.1.443889646.1730560302&semt=ais_hybrid&w=740"),
                CategoryItem("Pizza", "https://img.freepik.com/free-psd/top-view-delicious-pizza_23-2151868906.jpg?ga=GA1.1.443889646.1730560302&semt=ais_hybrid&w=740"),
                CategoryItem("Burger", "https://img.freepik.com/free-psd/close-up-hamburger-isolated_23-2151604195.jpg?ga=GA1.1.443889646.1730560302&semt=ais_hybrid&w=740"),
                CategoryItem("Thali", "https://img.freepik.com/premium-psd/india-watercolor-frame-festive-designs-indian-culture-seasons-food-drink_1305733-8337.jpg?ga=GA1.1.443889646.1730560302&semt=ais_hybrid&w=740"),
                CategoryItem("North Indian", "https://img.freepik.com/premium-psd/indian-thali-thali-indian_396469-32.jpg?ga=GA1.1.443889646.1730560302&semt=ais_hybrid&w=740"),
                CategoryItem("South Indian", "https://img.freepik.com/free-psd/delicious-goldenbrown-masala-dosa-with-vibrant-chutneys-tempting-south-indian-breakfast_84443-34188.jpg?ga=GA1.1.443889646.1730560302&semt=ais_hybrid&w=740"),
                CategoryItem("Chinese", "https://img.freepik.com/free-psd/roasted-chicken-with-rosemary-sage_191095-83727.jpg?ga=GA1.1.443889646.1730560302&semt=ais_hybrid&w=740"),
                CategoryItem("Paneer", "https://img.freepik.com/premium-psd/stir-fried-tofu-white-plate-top-view-isolated-transparent-background_1232542-71477.jpg?ga=GA1.1.443889646.1730560302&semt=ais_hybrid&w=740"),
                CategoryItem("Chicken", "https://images.unsplash.com/photo-1598515214211-89d3c73ae83b?w=400"),
                CategoryItem("Rolls", "https://img.freepik.com/free-psd/new-mexican-flat-enchiladas-isolated-transparent-background_191095-32406.jpg?ga=GA1.1.443889646.1730560302&semt=ais_hybrid&w=740"),
                CategoryItem("Momos", "https://img.freepik.com/premium-psd/steamed-bao-buns-wooden-steamer-delicious-asian-food_84443-47747.jpg?ga=GA1.1.443889646.1730560302&semt=ais_hybrid&w=740"),
                CategoryItem("Pasta", "https://img.freepik.com/free-psd/delicious-fusilli-pasta-with-tomato-sauce-basil_84443-37005.jpg?ga=GA1.1.443889646.1730560302&semt=ais_hybrid&w=740"),
                CategoryItem("Sandwiches", "https://img.freepik.com/premium-psd/sandwich-with-cheese-meat-plate_949261-18970.jpg?ga=GA1.1.443889646.1730560302&semt=ais_hybrid&w=740"),
                CategoryItem("Desserts", "https://img.freepik.com/free-psd/delicious-vanilla-ice-cream-with-chocolate-drizzle-shavings_632498-24904.jpg?ga=GA1.1.443889646.1730560302&semt=ais_hybrid&w=740"),
                CategoryItem("Beverages", "https://img.freepik.com/free-psd/refreshing-fruit-juices-delightful-citrus-blend-healthy-lifestyle-choice_191095-90526.jpg?ga=GA1.1.443889646.1730560302&semt=ais_hybrid&w=740"),
                CategoryItem("Street Food", "https://images.unsplash.com/photo-1606491956689-2ea866880c84?w=400"),
                CategoryItem("Healthy", "https://img.freepik.com/free-psd/healthy-balanced-meal-grilled-chicken-broccoli-cheese-grapes-tomatoes_632498-26043.jpg?ga=GA1.1.443889646.1730560302&semt=ais_hybrid&w=740")
            )

            // Only show categories that exist in the menu items and are in our predefined list
            val filteredCategories = predefinedCategoriesWithImages.filter { categoryItem ->
                allMenuCategories.contains(categoryItem.name)
            }

            listOf(CategoryItem("All", "https://img.freepik.com/premium-psd/thanksgiving-dinner-dishes-plate-transparent-background_1324646-10039.jpg?ga=GA1.1.443889646.1730560302&semt=ais_hybrid&w=740")) + filteredCategories
        }
    }

    Scaffold(
        containerColor = backgroundColor,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 100.dp)
            ) {
                // Map FAB
                FloatingActionButton(
                    onClick = {
                        navController.navigate(CampusMapRoute)
                    },
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Open Map",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Your existing Favorites FAB
                ExtendedFloatingActionButton(
                    onClick = { showFavoritesOnly = !showFavoritesOnly },
                    containerColor = if (showFavoritesOnly) Color.Red else primaryBlue,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = if (showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (showFavoritesOnly) "Show All" else "Show Favorites"
                    )
                    AnimatedVisibility(
                        visible = favoriteShops.isNotEmpty() || showFavoritesOnly,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        Row {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (showFavoritesOnly) {
                                    "Show All"
                                } else {
                                    "Favorites (${favoriteShops.size})"
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        // Box to overlay navigation on content
        Box(modifier = Modifier.fillMaxSize()) {
            // Show shimmer while initializing
            if (shops.isEmpty() && favoriteShops.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Search bar placeholder
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        com.divyansh.cueats.common.ShimmerCard(height = 50)
                    }
                    
                    // Category row placeholder
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        repeat(4) {
                            com.divyansh.cueats.common.ShimmerCircle(size = 64)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Shop cards placeholder
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(4) {
                            com.divyansh.cueats.common.ShimmerShopCard()
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    color = backgroundColor
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // Fixed Top Section with Search and Offers (Non-scrollable)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Search Bar and Offers Button Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Custom Search Bar using Box instead of OutlinedTextField
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .background(
                                            color = surfaceColor,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = colorScheme.outline.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search",
                                            tint = primaryBlue,
                                            modifier = Modifier.size(20.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                color = onSurfaceColor,
                                                fontSize = 14.sp
                                            ),
                                            decorationBox = { innerTextField ->
                                                Box(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    contentAlignment = Alignment.CenterStart
                                                ) {
                                                    if (searchQuery.isEmpty()) {
                                                        Text(
                                                            text = "Search restaurants or dishes...",
                                                            color = onSurfaceVariantColor,
                                                            fontSize = 14.sp
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            }
                                        )
                                    }
                                }

                                // Simple Offers Button
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .background(
                                            color = surfaceColor,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = colorScheme.outline.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { showOffersSheet = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.offer),
                                        contentDescription = "Offers",
                                        tint = primaryBlue,
                                        modifier = Modifier.size(24.dp)
                                    )

                                    // Simple red dot instead of Badge if there are offers
                                    if (offers.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color.Red, CircleShape)
                                                .align(Alignment.TopEnd)
                                                .offset(x = (-4).dp, y = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Unified Scrollable Content (Categories, Address Filters, and Shops)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp) // Extra padding for floating nav
                        ) {
                            // Category Row Section
                            item {
                                Column {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        contentPadding = PaddingValues(horizontal = 28.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(
                                            items = availableCategories,
                                            key = { it.name } // Add key for better performance
                                        ) { category ->
                                            CategoryCard(
                                                category = category,
                                                isSelected = selectedFilter == category.name,
                                                primaryColor = primaryBlue,
                                                surfaceColor = surfaceColor,
                                                onSurfaceColor = onSurfaceColor,
                                                onSurfaceVariantColor = onSurfaceVariantColor,
                                                onClick = { selectedFilter = category.name }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }

                            // Divider Line
                            item {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 25.dp),
                                    thickness = 1.dp,
                                    color = colorScheme.outline.copy(alpha = 0.3f)
                                )
                            }

                            // Address Filter Row Section
                            item {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(
                                        items = addressFilters,
                                        key = { it } // Add key for better performance
                                    ) { filter ->
                                        AddressFilterChip(
                                            text = filter,
                                            isSelected = selectedAddressFilter == filter,
                                            onClick = { selectedAddressFilter = filter },
                                            primaryColor = primaryBlue,
                                            surfaceColor = surfaceColor,
                                            onSurfaceColor = onSurfaceColor,
                                            onSurfaceVariantColor = onSurfaceVariantColor
                                        )
                                    }
                                }
                            }

                            // Shop Cards or Empty State
                            if (filteredShops.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(300.dp), // Give it some height for better UX
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = when {
                                                    selectedFilter != "All" && selectedAddressFilter != "All" ->
                                                        "No restaurants found with $selectedFilter in $selectedAddressFilter"
                                                    selectedFilter != "All" ->
                                                        "No restaurants found with $selectedFilter"
                                                    selectedAddressFilter != "All" ->
                                                        "No restaurants found in $selectedAddressFilter"
                                                    else -> "No restaurants found"
                                                },
                                                color = primaryColor,
                                                style = MaterialTheme.typography.bodyLarge,
                                                textAlign = TextAlign.Center
                                            )
                                            if (selectedFilter != "All" || selectedAddressFilter != "All") {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                TextButton(
                                                    onClick = {
                                                        selectedFilter = "All"
                                                        selectedAddressFilter = "All"
                                                    }
                                                ) {
                                                    Text(
                                                        "Show all restaurants",
                                                        color = primaryBlue
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Shop Cards
                                items(
                                    items = filteredShops,
                                    key = { it.id } // Add key for better performance
                                ) { shop ->
                                    EnhancedShopCard(
                                        shop = shop,
                                        navController = navController,
                                        surfaceColor = surfaceColor,
                                        primaryColor = primaryColor,
                                        onSurfaceColor = onSurfaceColor,
                                        onSurfaceVariantColor = onSurfaceVariantColor,
                                        favoriteViewModel = favoriteViewModel // ADD THIS LINE
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
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
                    currentRoute = "shops"
                )
            }
        }
    }

    // Offers Bottom Sheet
    if (showOffersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOffersSheet = false },
            containerColor = surfaceColor
        ) {
            OffersBottomSheetContent(
                offers = offers,
                onDismiss = { showOffersSheet = false },
                primaryColor = primaryColor,
                onSurfaceColor = onSurfaceColor,
                onSurfaceVariantColor = onSurfaceVariantColor
            )
        }
    }
}
@Composable
fun AddressFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    surfaceColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color
) {
    val backgroundColor = if (isSelected) primaryColor else surfaceColor
    val textColor = if (isSelected) Color.White else onSurfaceColor
    val borderColor = if (isSelected) primaryColor else onSurfaceVariantColor.copy(alpha = 0.3f)
    val primaryBlue = Color(0xFF42A5F5)
    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (text != "All") {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

data class Offer(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val shopName: String = "",
    val discount: String = "",
    val validUntil: Timestamp? = null,
    val validUntilString: String? = null, // For string format dates
    val imageUrl: String? = null,
    val isActive: Boolean = true
)

@Composable
fun OffersBottomSheetContent(
    offers: List<Offer>,
    onDismiss: () -> Unit,
    primaryColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎉 Current Offers",
                style = MaterialTheme.typography.headlineSmall,
                color = onSurfaceColor,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = onSurfaceVariantColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (offers.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBox,
                    contentDescription = null,
                    tint = onSurfaceVariantColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No offers available right now",
                    color = onSurfaceVariantColor,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Check back later for exciting deals!",
                    color = onSurfaceVariantColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            // Offers list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(400.dp)
            ) {
                items(offers) { offer ->
                    OfferCard(
                        offer = offer,
                        primaryColor = primaryColor,
                        onSurfaceColor = onSurfaceColor,
                        onSurfaceVariantColor = onSurfaceVariantColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun OfferCard(
    offer: Offer,
    primaryColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = primaryColor.copy(alpha = 0.05f)
        ),
        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = offer.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = onSurfaceColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = offer.shopName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = onSurfaceVariantColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = primaryColor
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = offer.discount,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = offer.description,
                style = MaterialTheme.typography.bodyMedium,
                color = onSurfaceVariantColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Handle both date formats for display
                val validUntilText = when {
                    offer.validUntil != null -> {
                        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        "Valid until: ${formatter.format(offer.validUntil.toDate())}"
                    }
                    offer.validUntilString != null -> {
                        try {
                            val inputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val outputFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            val date = inputFormatter.parse(offer.validUntilString)
                            "Valid until: ${outputFormatter.format(date)}"
                        } catch (e: Exception) {
                            "Valid until: ${offer.validUntilString}"
                        }
                    }
                    else -> "Valid until: Not specified"
                }

                Text(
                    text = validUntilText,
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariantColor.copy(alpha = 0.8f)
                )


            }
        }
    }
}


@Composable
fun CategoryCard(
    category: CategoryItem,
    isSelected: Boolean,
    primaryColor: Color,
    surfaceColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        // Circular Image Container (removed border and background)
        Box(
            modifier = Modifier.size(50.dp), // Fixed size without conditional sizing
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(category.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = category.name,
                modifier = Modifier
                    .size(64.dp) // Fixed size without conditional sizing
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.loading), // Add your placeholder
                error = painterResource(id = R.drawable.loading)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category Name (still changes color and weight when selected)
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (isSelected) primaryColor else onSurfaceColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 80.dp)
        )
    }
}




// Data class for category items
data class CategoryItem(
    val name: String,
    val imageUrl: String
)

@Composable
fun EnhancedShopCard(
    shop: Shop,
    navController: NavController,
    surfaceColor: Color,
    primaryColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color,
    favoriteViewModel: FavoriteViewModel
) {
    // Use collectAsState for reactive updates
    val favoriteShops by favoriteViewModel.favoriteShops.collectAsState()
    val isFavorite = favoriteShops.contains(shop.id)
    val context = LocalContext.current



    // Add rating dialog state

    // Add image loading state
    var isImageLoading by remember { mutableStateOf(true) }
    var hasImageLoadError by remember { mutableStateOf(false) }



    val primaryBlue = Color(0xFF42A5F5)
    val deliveryGreen = Color(0xFF4CAF50)
    val callBlue = Color(0xFF2196F3)
    val whatsappGreen = Color(0xFF25D366)
    val starYellow = Color(0xFFFFC107)

    Card(
        modifier = Modifier
            .padding(horizontal = 15.dp)
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                try {
                    navController.navigate(ShopMenuRoute(shopId = shop.id))
                } catch (e: Exception) {
                    Log.e("Navigation", "Error navigating to shop menu: ${e.message}")
                }
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Shop Image with Overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (shop.imageUrl.isNotEmpty() && !hasImageLoadError) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(shop.imageUrl)
                            .crossfade(false)
                            .listener(
                                onStart = {
                                    isImageLoading = true
                                },
                                onSuccess = { _, _ ->
                                    isImageLoading = false
                                    hasImageLoadError = false
                                    Log.d("ImageLoad", "Successfully loaded shop image: ${shop.imageUrl}")
                                },
                                onError = { _, _ ->
                                    isImageLoading = false
                                    hasImageLoadError = true
                                    Log.e("ImageLoad", "Failed to load shop image: ${shop.imageUrl}")
                                }
                            )
                            .build(),
                        placeholder = null,
                        error = null,
                        contentDescription = "${shop.name} image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    )

                    if (isImageLoading) {
                        ImprovedShimmerOverlay(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        onSurfaceVariantColor.copy(alpha = 0.3f),
                                        onSurfaceVariantColor.copy(alpha = 0.1f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = shop.name.take(2).uppercase(),
                                style = MaterialTheme.typography.headlineLarge,
                                color = primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                            if (hasImageLoadError) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = "Image failed to load",
                                    tint = onSurfaceVariantColor.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Delivery Badge (Bottom Left of Image)
                if (shop.hasDelivery) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .background(
                                color = deliveryGreen.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeliveryDining,
                                contentDescription = "Delivery Available",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Delivery",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Favorite Button (Top Right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(40.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            CircleShape
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            favoriteViewModel.toggleFavorite(shop.id)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) primaryBlue else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Shop Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Shop Name Row with Rating and Contact
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Left side: Shop Name and Rating
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = shop.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = onSurfaceColor
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Right side: Contact Actions
                    if (shop.contactNumber.isNotEmpty()) {
                        ContactActions(
                            contactNumber = shop.contactNumber,
                            callBlue = callBlue,
                            whatsappGreen = whatsappGreen,
                            context = context
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Shop Address
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = primaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = shop.address,
                        color = onSurfaceColor.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }


}


@Composable
fun ImprovedShimmerOverlay(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")

    // More refined shimmer animation
    val shimmerTranslateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    // Subtle opacity pulse
    val shimmerOpacity by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_opacity"
    )

    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = shimmerOpacity),
                        Color.Transparent
                    ),
                    start = Offset(shimmerTranslateAnim - 200f, 0f),
                    end = Offset(shimmerTranslateAnim, 300f)
                )
            )
    )
}


@Composable
private fun ContactActions(
    contactNumber: String,
    callBlue: Color,
    whatsappGreen: Color,
    context: Context
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Call Button
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = callBlue.copy(alpha = 0.1f),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = callBlue.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$contactNumber")
                    }
                    context.startActivity(intent)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "Call Shop",
                tint = callBlue,
                modifier = Modifier.size(18.dp)
            )
        }

        // WhatsApp Button
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = whatsappGreen.copy(alpha = 0.1f),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = whatsappGreen.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://wa.me/91$contactNumber")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback to SMS if WhatsApp not available
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("sms:$contactNumber")
                        }
                        context.startActivity(intent)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "WhatsApp",
                tint = whatsappGreen,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// Add this loading screen composable first
@Composable
fun ShopMenuLoadingScreen(
    shopName: String,
    shopId: String
) {

    val ratingViewModel: RatingViewModel = viewModel()
    val shopRating = ratingViewModel.getShopRating(shopId)
    var showRatingDialog by remember { mutableStateOf(false) }

    // Theme-aware oranges
    val primaryBlue = Color(0xFF42A5F5)
    val secondaryOrange = Color(0xFFFF8533)

    // Background adapts to theme
    val backgroundColors =
        listOf(
            primaryBlue.copy(alpha = 0.1f),
            Color.White,
            secondaryOrange.copy(alpha = 0.05f)
        )


    // Text color adapts to theme
    val textColor =  Color(0xFF2C2C2C)
    val subtitleColor =  Color(0xFF555555)
    val cardColor =Color.White

    val foodQuotes = listOf(
        "Nourish your dreams, one bite at a time. ✨🍴",
        "Fuel up and let today’s possibilities grow. 🌱🥗",
        "Great things are cooked up from small beginnings. 👩‍🍳🔥",
        "Meals shared are memories made. Start your story here. 📖🍲",
        "You’re one step closer to your best self—keep going! 💪🍛",
        "Every meal is a new chapter—write it deliciously. 📝🥘",
        "Energy for your hustle, comfort for your soul. ⚡❤️",
        "Feed your potential—every day, every plate. 🚀🍽️",
        "Gather strength, savor joy, then conquer the day. 🥇🍞",
        "Good food inspires big dreams. Dare to dream bigger! 🌟🍔"
    )

    val currentQuote = remember { foodQuotes.random() }

    val infiniteTransition = rememberInfiniteTransition(label = "loading_animations")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val fadeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fade"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = backgroundColors)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ... [the animated circles and loading dots — unchanged, use theme-aware oranges]
            // Shop name with fade animation
            Text(
                text = "Loading $shopName Menu",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(fadeAlpha)
                    .padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .alpha(fadeAlpha),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = currentQuote,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = subtitleColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Loading dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { index ->
                    val dotScale by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_$index"
                    )

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .scale(dotScale)
                            .background(primaryBlue, CircleShape)
                    )
                }
            }
        }
    }
}


// Updated ShopMenuDetailScreen with data accuracy warning and report functionality
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopMenuDetailScreen(
    navController: NavController,
    shopId: String
) {
    val shopViewModel: ShopViewModel = viewModel()
    val shop = shopViewModel.getShopById(shopId)
    val primaryGreen = Color(0xFF47B44C)
    val primaryBlue = Color(0xFF42A5F5)
    val context = LocalContext.current

    // Add loading state
    var isLoading by remember { mutableStateOf(true) }

    // Add report dialog state
    var showReportDialog by remember { mutableStateOf(false) }
    var isSubmittingReport by remember { mutableStateOf(false) }

    // Add data accuracy banner state
    var showDataAccuracyBanner by remember { mutableStateOf(true) }

    // Simulate loading time for menu data
    LaunchedEffect(shopId) {
        delay(2500) // 2.5 seconds loading time
        isLoading = false
    }

    // Show loading screen while loading - FIX: Pass shopId parameter
    if (isLoading) {
        ShopMenuLoadingScreen(
            shopName = shop?.name ?: "Restaurant",
            shopId = shopId // Add this parameter
        )
        return
    }

    // Improved color scheme for better dark theme support
    val backgroundColor = Color(0xFFF5F5F5)
    val surfaceColor =  Color.White
    val textPrimaryColor = Color(0xFF1A2C38)
    val textSecondaryColor =  Color(0xFF57727C)
    val cardBgColor =Color.White
    val borderColor =  Color(0xFFE0E0E0)

    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var expandedCategories by remember { mutableStateOf(setOf<String>()) }

    // Function to handle report submission
    fun submitReport(name: String, email: String, message: String, reportType: String) {
        isSubmittingReport = true
        val db = FirebaseFirestore.getInstance()

        val report = hashMapOf(
            "shopId" to shopId,
            "shopName" to (shop?.name ?: "Unknown"),
            "reporterName" to name,
            "reporterEmail" to email,
            "message" to message,
            "reportType" to reportType,
            "timestamp" to System.currentTimeMillis(),
            "appVersion" to "1.0.0",
            "deviceInfo" to "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})",
            "status" to "new"
        )

        db.collection("shop_reports")
            .add(report)
            .addOnSuccessListener { documentReference ->
                isSubmittingReport = false
                android.widget.Toast.makeText(
                    context,
                    "Thank you! Your report has been submitted successfully.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                showReportDialog = false
            }
            .addOnFailureListener { exception ->
                isSubmittingReport = false
                android.widget.Toast.makeText(
                    context,
                    "Failed to submit report. Please try again.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
    }

    // Better error handling for missing shop
    if (shop == null) {
        Scaffold(
            containerColor = backgroundColor,
            topBar = {
                TopAppBar(
                    title = { Text("Shop Not Found") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = primaryBlue,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }

        ) { paddingValues ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Shop not found",
                        style = MaterialTheme.typography.headlineSmall,
                        color = textSecondaryColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { navController.navigateUp() },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                    ) {
                        Text("Go Back", color = Color.White)
                    }
                }
            }
        }
        return
    }

    // Create categories list
    val categories = listOf("All") + shop.menuItems.map { it.category }.distinct()

    val filteredItems = shop.menuItems.filter { item ->
        (selectedCategory == "All" || item.category == selectedCategory) &&
                (searchQuery.isEmpty() || item.name.contains(searchQuery, ignoreCase = true))
    }

    // Group filtered items by category for section display
    val groupedItems = if (selectedCategory == "All") {
        filteredItems.groupBy { it.category }
    } else {
        mapOf(selectedCategory to filteredItems)
    }

    // Initialize expanded categories
    LaunchedEffect(selectedCategory) {
        if (selectedCategory == "All") {
            expandedCategories = groupedItems.keys.toSet()
        } else {
            expandedCategories = setOf(selectedCategory)
        }
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = shop.name,
                            modifier = Modifier.weight(1f)
                        )

                        // Report button
                        IconButton(
                            onClick = { showReportDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Report,
                                contentDescription = "Report Issue",
                                tint = Color.White
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showFilterSheet = true },
                containerColor = primaryBlue,
                contentColor = Color.White,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painterResource(R.drawable.menu1),
                        contentDescription = "Filter Menu",
                        modifier = Modifier.size(25.dp)
                    )
                    Text(
                        text = "MENU",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
        ) {
            // Data Accuracy Warning Banner - reduced bottom padding
            if (showDataAccuracyBanner) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .padding(bottom = 4.dp), // Reduced bottom padding
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0) // Light orange background
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFFF8F00),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Menu data may not be fully accurate. Found an error?",
                            fontSize = 13.sp,
                            color = Color(0xFFE65100),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { showReportDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Report",
                                fontSize = 12.sp,
                                color = primaryBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(
                            onClick = { showDataAccuracyBanner = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color(0xFFFF8F00),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Search bar with improved dark theme styling - reduced top padding
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search menu items...",
                        color = textSecondaryColor
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = textSecondaryColor
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 16.dp), // Reduced top padding from 16dp to 4dp
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryBlue,
                    unfocusedBorderColor = borderColor,
                    focusedTextColor = textPrimaryColor,
                    unfocusedTextColor = textPrimaryColor,
                    cursorColor = primaryBlue,
                    focusedContainerColor = surfaceColor,
                    unfocusedContainerColor = surfaceColor
                )
            )

            // Horizontal Category Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(categories) { category ->
                    CategoryChip(
                        category = category,
                        isSelected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        primaryBlue = primaryBlue,
                        textPrimaryColor = textPrimaryColor,
                        textSecondaryColor = textSecondaryColor,

                    )
                }
            }

            // Menu items grouped by categories
            if (groupedItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterVertically as Alignment.Horizontal
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = textSecondaryColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No items found",
                            color = textSecondaryColor,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedItems.forEach { (category, items) ->
                        // Only show category headers when "All" is selected
                        if (selectedCategory == "All") {
                            item {
                                CategoryHeader(
                                    category = category,
                                    itemCount = items.size,
                                    isExpanded = expandedCategories.contains(category),
                                    onToggle = {
                                        expandedCategories = if (expandedCategories.contains(category)) {
                                            expandedCategories - category
                                        } else {
                                            expandedCategories + category
                                        }
                                    },
                                    textPrimaryColor = textPrimaryColor,
                                    textSecondaryColor = textSecondaryColor,
                                    primaryBlue = primaryBlue
                                )
                            }
                        }

                        // Category Items (if expanded or specific category is selected)
                        if (selectedCategory != "All" || expandedCategories.contains(category)) {
                            items(items) { menuItem ->
                                MenuItemCard(
                                    menuItem = menuItem,
                                    primaryBlue = primaryBlue,
                                    cardBgColor = cardBgColor,
                                    textPrimaryColor = textPrimaryColor,
                                    textSecondaryColor = textSecondaryColor,
                                    modifier = if (selectedCategory == "All") {
                                        Modifier.padding(start = 8.dp)
                                    } else {
                                        Modifier
                                    }
                                )
                            }

                            // Add spacing after each category
                            if (selectedCategory == "All") {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Filter Bottom Sheet like in the image
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                containerColor = surfaceColor,
                contentColor = textPrimaryColor,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                textSecondaryColor.copy(alpha = 0.5f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Create categories with counts like in the image
                    val categoryItems = categories.associateWith { category ->
                        if (category == "All") {
                            shop.menuItems.size
                        } else {
                            shop.menuItems.count { it.category == category }
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categoryItems.toList()) { (category, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCategory = category
                                        showFilterSheet = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (selectedCategory == category) {
                                        primaryBlue
                                    } else {
                                        textPrimaryColor
                                    },
                                    fontWeight = if (selectedCategory == category) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    }
                                )
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selectedCategory == category) {
                                        primaryBlue
                                    } else {
                                        textPrimaryColor
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Report Dialog
        if (showReportDialog) {
            ShopReportDialog(
                shopName = shop.name,
                isSubmitting = isSubmittingReport,
                onDismiss = {
                    if (!isSubmittingReport) {
                        showReportDialog = false
                    }
                },
                onSubmit = { name, email, message, reportType ->
                    submitReport(name, email, message, reportType)
                }
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShopReportDialog(
    shopName: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var reportType by remember { mutableStateOf("Incorrect Menu Information") }

    val reportTypes = listOf(
        "Incorrect Menu Information",
        "Wrong Prices",
        "Unavailable Items",
        "Shop Closed/Not Found",
        "Other Issue"
    )

    var showTypeDropdown by remember { mutableStateOf(false) }


    // Enhanced color scheme with better contrast
    val dialogBackgroundColor = Color.White
    val textPrimaryColor = Color(0xFF212121)
    val textSecondaryColor =Color(0xFF757575)
    val primaryBlue = Color(0xFF42A5F5)
    val surfaceColor = Color(0xFFFAFAFA)
    val borderColor = Color(0xFFE0E0E0)
    val errorColor = Color(0xFFFF4444)

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.85f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = dialogBackgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                if (isSubmitting) {
                    // Improved loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(
                                        primaryBlue.copy(alpha = 0.1f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = primaryBlue,
                                    modifier = Modifier.size(48.dp),
                                    strokeWidth = 4.dp
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Text(
                                text = "Submitting Report...",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimaryColor,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Please wait while we process your feedback",
                                fontSize = 14.sp,
                                color = textSecondaryColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp),
                                lineHeight = 20.sp
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Improved Header with better spacing
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = primaryBlue.copy(alpha = 0.05f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                primaryBlue.copy(alpha = 0.15f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Report,
                                            contentDescription = null,
                                            tint = primaryBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = "Report Issue",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimaryColor,
                                            letterSpacing = (-0.3).sp
                                        )
                                        Text(
                                            text = shopName,
                                            fontSize = 13.sp,
                                            color = primaryBlue,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(top = 1.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = textSecondaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Form content with better structure
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            // Report Type Section
                            item {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "What's the issue?",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textPrimaryColor,
                                        letterSpacing = (-0.1).sp
                                    )

                                    ExposedDropdownMenuBox(
                                        expanded = showTypeDropdown,
                                        onExpandedChange = { showTypeDropdown = !showTypeDropdown }
                                    ) {
                                        OutlinedTextField(
                                            value = reportType,
                                            onValueChange = { },
                                            readOnly = true,
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = primaryBlue,
                                                unfocusedBorderColor = borderColor,
                                                focusedTextColor = textPrimaryColor,
                                                unfocusedTextColor = textPrimaryColor,
                                                cursorColor = primaryBlue,
                                                focusedContainerColor = surfaceColor,
                                                unfocusedContainerColor = surfaceColor
                                            ),
                                            textStyle = LocalTextStyle.current.copy(
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = if (showTypeDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = primaryBlue
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Category,
                                                    contentDescription = null,
                                                    tint = primaryBlue,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        )

                                        // Custom styled dropdown menu
                                        if (showTypeDropdown) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = dialogBackgroundColor
                                                ),
                                                border = BorderStroke(1.dp, borderColor),
                                                elevation = CardDefaults.cardElevation(
                                                    defaultElevation = 8.dp
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    reportTypes.forEachIndexed { index, type ->
                                                        Surface(
                                                            onClick = {
                                                                reportType = type
                                                                showTypeDropdown = false
                                                            },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            color = Color.Transparent
                                                        ) {
                                                            Text(
                                                                text = type,
                                                                color = textPrimaryColor,
                                                                fontSize = 15.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(
                                                                        horizontal = 16.dp,
                                                                        vertical = 12.dp
                                                                    )
                                                            )
                                                        }

                                                        if (index < reportTypes.size - 1) {
                                                            HorizontalDivider(
                                                                modifier = Modifier.padding(horizontal = 12.dp),
                                                                thickness = 0.5.dp,
                                                                color = borderColor.copy(alpha = 0.5f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Name Field Section
                            item {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Your Name",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimaryColor,
                                            letterSpacing = (-0.1).sp
                                        )
                                        Text(
                                            text = " *",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = errorColor
                                        )
                                    }

                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = {
                                            Text(
                                                "Enter your full name",
                                                color = textSecondaryColor,
                                                fontSize = 15.sp
                                            )
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = primaryBlue,
                                            unfocusedBorderColor = if (name.isEmpty()) borderColor else primaryBlue.copy(
                                                alpha = 0.5f
                                            ),
                                            focusedTextColor = textPrimaryColor,
                                            unfocusedTextColor = textPrimaryColor,
                                            cursorColor = primaryBlue,
                                            focusedContainerColor = surfaceColor,
                                            unfocusedContainerColor = surfaceColor
                                        ),
                                        textStyle = LocalTextStyle.current.copy(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = if (name.isNotEmpty()) primaryBlue else textSecondaryColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Text,
                                            imeAction = ImeAction.Next,
                                            capitalization = KeyboardCapitalization.Words
                                        )
                                    )
                                }
                            }

                            // Email Field Section
                            item {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Email Address (Optional)",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textPrimaryColor,
                                        letterSpacing = (-0.1).sp
                                    )

                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = {
                                            Text(
                                                "your.email@example.com",
                                                color = textSecondaryColor,
                                                fontSize = 15.sp
                                            )
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = primaryBlue,
                                            unfocusedBorderColor = if (email.isEmpty()) borderColor else primaryBlue.copy(
                                                alpha = 0.5f
                                            ),
                                            focusedTextColor = textPrimaryColor,
                                            unfocusedTextColor = textPrimaryColor,
                                            cursorColor = primaryBlue,
                                            focusedContainerColor = surfaceColor,
                                            unfocusedContainerColor = surfaceColor
                                        ),
                                        textStyle = LocalTextStyle.current.copy(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = null,
                                                tint = if (email.isNotEmpty()) primaryBlue else textSecondaryColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Email,
                                            imeAction = ImeAction.Next
                                        )
                                    )
                                }
                            }

                            // Message Field Section
                            item {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Describe the Issue",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimaryColor,
                                            letterSpacing = (-0.1).sp
                                        )
                                        Text(
                                            text = " *",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = errorColor
                                        )
                                    }

                                    OutlinedTextField(
                                        value = message,
                                        onValueChange = { message = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp),
                                        placeholder = {
                                            Text(
                                                "Please provide details about the issue you found...",
                                                color = textSecondaryColor,
                                                fontSize = 14.sp,
                                                lineHeight = 18.sp
                                            )
                                        },
                                        maxLines = 5,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = primaryBlue,
                                            unfocusedBorderColor = if (message.isEmpty()) borderColor else primaryBlue.copy(
                                                alpha = 0.5f
                                            ),
                                            focusedTextColor = textPrimaryColor,
                                            unfocusedTextColor = textPrimaryColor,
                                            cursorColor = primaryBlue,
                                            focusedContainerColor = surfaceColor,
                                            unfocusedContainerColor = surfaceColor
                                        ),
                                        textStyle = LocalTextStyle.current.copy(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Normal,
                                            lineHeight = 20.sp
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Text,
                                            imeAction = ImeAction.Done,
                                            capitalization = KeyboardCapitalization.Sentences
                                        )
                                    )
                                }
                            }

                            // Improved Info Card
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = primaryBlue.copy(alpha = 0.08f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = primaryBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Your feedback helps us maintain accurate information for everyone.",
                                            fontSize = 12.sp,
                                            color = primaryBlue,
                                            lineHeight = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        // Improved Bottom Actions
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = dialogBackgroundColor,
                            shadowElevation = 4.dp
                        ) {
                            Column {
                                HorizontalDivider(
                                    color = borderColor,
                                    thickness = 0.5.dp
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = onDismiss,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, borderColor),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = textPrimaryColor
                                        )
                                    ) {
                                        Text(
                                            "Cancel",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (name.isNotBlank() && message.isNotBlank()) {
                                                onSubmit(name, email, message, reportType)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        enabled = name.isNotBlank() && message.isNotBlank(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = primaryBlue,
                                            disabledContainerColor = textSecondaryColor.copy(alpha = 0.3f),
                                            contentColor = Color.White,
                                            disabledContentColor = Color.White.copy(alpha = 0.6f)
                                        ),
                                        elevation = ButtonDefaults.buttonElevation(
                                            defaultElevation = 2.dp,
                                            pressedElevation = 4.dp,
                                            disabledElevation = 0.dp
                                        ),
                                        contentPadding = PaddingValues(
                                            horizontal = 16.dp,
                                            vertical = 8.dp
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Send,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Submit",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Clip
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    category: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryBlue: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,

) {
    val backgroundColor = if (isSelected) primaryBlue else Color(0xFFE0E0E0)



    val textColor = when {
        isSelected -> Color.White
        else -> textPrimaryColor
    }

    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Text(
            text = category,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun CategoryHeader(
    category: String,
    itemCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    primaryBlue: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$category ($itemCount)",
            style = MaterialTheme.typography.titleLarge,
            color = textPrimaryColor,
            fontWeight = FontWeight.Bold
        )

        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = textSecondaryColor,
            modifier = Modifier.size(24.dp)
        )
    }
}


@Composable
fun MenuItemCard(
    menuItem: MenuItem,
    primaryBlue: Color,
    cardBgColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation =  0.5.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side indicators
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Veg/Non-veg indicator
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .border(
                            1.5.dp,
                            if (menuItem.isVeg) Color(0xFF4CAF50) else Color(0xFFF44336),
                            RoundedCornerShape(3.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (menuItem.isVeg) Color(0xFF4CAF50) else Color(0xFFF44336),
                                CircleShape
                            )
                    )
                }

                if (menuItem.isPopular) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                primaryBlue,
                                CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = menuItem.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = textPrimaryColor
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "₹${menuItem.price.toInt()}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = primaryBlue
                        )
                    )
                }

                if (menuItem.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = menuItem.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = textSecondaryColor,
                            lineHeight = 16.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


// Data classes remain the same
data class Shop(
    val id: String,
    val name: String,
    val imageUrl: String,
    val cuisine: String,
    val address: String,
    val location: String,
    val rating: Double,
    val deliveryTime: String,
    val popularItems: List<String>,
    val menuItems: List<MenuItem>,
    val hasDelivery: Boolean = false,
    val contactNumber: String = ""
)

data class MenuItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val category: String,
    val isVeg: Boolean,
    val isPopular: Boolean = false
)

data class Rating(
    val id: String = "",
    val shopId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Float = 0f,
    val review: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isVerified: Boolean = false
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", "", "", 0f, "", Timestamp.now(), false)
}

data class ShopRating(
    val shopId: String = "",
    val averageRating: Float = 0f,
    val totalRatings: Int = 0,
    val ratingBreakdown: Map<Int, Int> = mapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0),
    val lastUpdated: Timestamp = Timestamp.now()
) {
    // No-argument constructor for Firestore
    constructor() : this("", 0f, 0, mapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0), Timestamp.now())
}


// Preview Functions
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewShopMenuScreen() {
    val navController = rememberNavController()
    ShopMenuScreen(navController)
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewShopMenuDetailScreen() {
    val navController = rememberNavController()
    ShopMenuDetailScreen(navController, "1")
}
