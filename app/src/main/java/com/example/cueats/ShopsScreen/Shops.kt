package com.example.cueats.ShopsScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.alpha
import coil.compose.AsyncImage
import androidx.lifecycle.viewModelScope
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material3.placeholder
import com.google.accompanist.placeholder.material3.shimmer
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cueats.AppBottomNavigation
import com.example.cueats.HomeScreen.playfairFont
import com.example.cueats.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopMenuScreen(navController: NavController) {
    val shopViewModel: ShopViewModel = viewModel()
    val shops by shopViewModel.shops.observeAsState(emptyList())
    val isDarkTheme = isSystemInDarkTheme()
    val primaryOrange = Color(0xFFFF6B01)
    val textSecondaryColor = if (isDarkTheme) Color(0xFFAAAAAA) else Color(0xFF57727C)
    val darkSurfaceColor = Color(0xFF202020)
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    val currentRoute = navController.currentBackStackEntry?.destination?.route ?: "shops"


    val filteredShops = shops.filter { shop ->
        (selectedFilter == "All" || shop.location == selectedFilter) &&
                (searchQuery.isEmpty() || shop.name.contains(searchQuery, ignoreCase = true))
    }

    val locations = listOf("All", "Boys Hostel", "Girls Hostel", "Campus")

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
            AppBottomNavigation(navController = navController, currentRoute = "shops")
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = if (isDarkTheme) Color(0xFF121212) else Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Title and subtitle
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Campus Shops",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White else Color(0xFF1A2C38)
                        )
                    )
                    Text(
                        text = "Explore food options around campus",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = textSecondaryColor
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search shops...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = if (isDarkTheme) Color(0xFF444444) else Color.LightGray,
                        unfocusedBorderColor = if (isDarkTheme) Color(0xFF333333) else Color.LightGray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )

                // Filter chips
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    locations.forEachIndexed { index, location ->
                        SegmentedButton(
                            shape = when (index) {
                                0 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                                locations.lastIndex -> RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                                else -> RectangleShape
                            },
                            selected = selectedFilter == location,
                            onClick = { selectedFilter = location },
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = primaryOrange,
                                activeBorderColor = primaryOrange,
                                activeContentColor = Color.White,
                                inactiveContainerColor = if (isDarkTheme) Color(0xFF202020) else Color.White,
                                inactiveBorderColor = if (isDarkTheme) Color(0xFF444444) else Color(0xFFDDDDDD),
                                inactiveContentColor = textSecondaryColor
                            )
                        ) {
                            Text(location)
                        }
                    }
                }

                // Shop list or empty state
                if (filteredShops.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No shops found.",
                            color = textSecondaryColor,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        items(filteredShops) { shop ->
                            ShopCard(shop, navController, isDarkTheme)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShopCard(shop: Shop, navController: NavController, isDarkTheme: Boolean) {
    val primaryOrange = Color(0xFFFF7F00)
    val textPrimaryColor = if (isDarkTheme) Color.White else Color(0xFF1A2C38)
    val textSecondaryColor = if (isDarkTheme) Color(0xFFAAAAAA) else Color(0xFF57727C)
    val cardBgColor = if (isDarkTheme) Color(0xFF202020) else Color.White

    // State for full-screen image dialog
    var isFullScreen by remember { mutableStateOf(false) }
    var isImageLoading by remember { mutableStateOf(true) }

    // State for zooming and panning
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Shop image (clickable to full screen)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable {
                        isFullScreen = true
                        scale = 1f
                        offset = Offset.Zero
                    }
            ) {
                AsyncImage(
                    model = shop.imageUrl,
                    contentDescription = "${shop.name} image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .then(if (isImageLoading) {
                            Modifier.placeholder(
                                visible = true,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                        } else Modifier),
                    onLoading = { isImageLoading = true },
                    onSuccess = { isImageLoading = false }
                )
            }

            // Shop details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = shop.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor
                    )
                )

                Text(
                    text = "Near ${shop.address.split(",").first()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondaryColor,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Opening Hours",
                        tint = primaryOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "8:30 AM - 11:00 PM",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondaryColor
                    )
                }
            }
        }
    }

    // Full Screen Dialog for expanded image
    if (isFullScreen) {
        Dialog(
            onDismissRequest = {
                isFullScreen = false
                scale = 1f
                offset = Offset.Zero
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Image with zoom & pan functionality
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, rotation ->
                                scale = (scale * zoom).coerceIn(1f, 3f)
                                if (scale > 1f) {
                                    val maxX = (size.width * (scale - 1)) / 2
                                    val maxY = (size.height * (scale - 1)) / 2
                                    offset = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                        y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                    )
                                } else {
                                    offset = Offset.Zero
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = shop.imageUrl,
                        contentDescription = "Full Screen Shop Image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                }

                // Close button
                IconButton(
                    onClick = {
                        isFullScreen = false
                        scale = 1f
                        offset = Offset.Zero
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Pan and zoom instructions
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Pinch to zoom • Drag to move",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
// Keep the existing Shop data class and ViewModel


data class Shop(
    val id: String,
    val name: String,
    val imageUrl: String, // 🔥 Changed from Int to String (for URL)
    val cuisine: String,
    val address: String,
    val location: String
)




class ShopViewModel : ViewModel() {
    private val _shops = MutableLiveData<List<Shop>>()
    val shops: LiveData<List<Shop>> = _shops

    init {
        fetchShops()
    }

    private fun fetchShops() {
        viewModelScope.launch {
            _shops.postValue(
                listOf(
                    Shop(
                        id = "1",
                        name = "Punjabi Rasoi",
                        imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/w_1000,h_1000,f_webp,q_auto:good/PunjabiRasoi_cblep2.png", // ✅ Optimized
                        cuisine = "",
                        address = "Boys Hostel, Near NC-1",
                        location = "Boys Hostel"
                    ),
                    Shop(
                        id = "2",
                        name = "Catch Up Cafe",
                        imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/w_1000,h_1000,f_webp,q_auto:good/catchup_x5gn7h.png",
                        cuisine = "",
                        address = "Boys Hostel, Near NC-1",
                        location = "Boys Hostel"
                    ),
                    Shop(
                        id = "3",
                        name = "Chatori Chaat And Kulcha Corner",
                        imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/w_1000,h_1000,f_webp,q_auto:good/chatorichaat_raxtl1.png",
                        cuisine = "",
                        address = "Boys Hostel, Near NC-1",
                        location = "Boys Hostel"
                    ),
                    Shop(
                        id = "4",
                        name = "Paratha House",
                        imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/w_1000,h_1000,f_webp,q_auto:good/ParathaHouse_jezfsk.png",
                        cuisine = "",
                        address = "Boys Hostel, Near NC-1",
                        location = "Boys Hostel"
                    ),
                    Shop(
                        id = "5",
                        name = "Baker'z Hub",
                        imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/w_1000,h_1000,f_webp,q_auto:good/bakershub_bbcinu.png",
                        cuisine = "",
                        address = "Boys Hostel, NC-2",
                        location = "Boys Hostel"
                    )
                )
            )
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewShopMenuScreen() {
    val navController = rememberNavController()
    ShopMenuScreen(navController)
}
