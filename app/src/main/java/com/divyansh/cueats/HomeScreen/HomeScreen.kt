package com.divyansh.cueats.HomeScreen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.divyansh.cueats.CampusMapRoute
import com.divyansh.cueats.LoginScreen.AuthViewModel
import com.divyansh.cueats.MealsRoute
import com.divyansh.cueats.R
import com.divyansh.cueats.ShopsRoute
import com.divyansh.cueats.AppBottomNavigation
import com.divyansh.cueats.NotificationRoute
import com.divyansh.cueats.ProfileRoute
import com.divyansh.cueats.Notification.NotificationIconWithBadge
import com.divyansh.cueats.Notification.NotificationViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val state by homeViewModel.state.collectAsState()
    val authState by authViewModel.authState

    // Set user name when available
    LaunchedEffect(authState.user) {
        authState.user?.displayName?.let { name ->
            homeViewModel.setUserName(name)
        }
    }

    // Observe notification count
    val unreadNotificationCount by notificationViewModel.unreadCount.observeAsState(0)

    // Light theme colors
    val backgroundColor = Color(0xFFF6F7FB)
    val surfaceColor = Color.White
    val primaryOrange = Color(0xFFFF6B01)
    val textColor = Color(0xFF2D2D2D)
    val textSecondaryColor = Color(0xFF8A8A8A)

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
                actions = {
                    // Notification icon with badge
                    NotificationIconWithBadge(
                        unreadCount = unreadNotificationCount,
                        onClick = {
                            navController.navigate(NotificationRoute) {
                                launchSingleTop = true
                            }
                        }
                    )

                    // Profile icon
                    IconButton(
                        onClick = {
                            navController.navigate(ProfileRoute) {
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryOrange,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            AppBottomNavigation(
                navController = navController,
                currentRoute = "home"
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primaryOrange)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Greeting Section
                GreetingSection(
                    userName = state.userName.ifEmpty { "Foodie" },
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Current/Next Meal Card
                MealTimingCard(
                    currentMeal = state.currentMeal,
                    nextMeal = state.nextMeal,
                    countdown = state.mealCountdown,
                    navController = navController,
                    primaryOrange = primaryOrange,
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Quick Access Section
                NavigationCardsSection(
                    navController = navController,
                    primaryOrange = primaryOrange,
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Today's Special Section
                state.topShop?.let { topShop ->
                    TodaysSpecialSection(
                        shop = topShop,
                        navController = navController,
                        primaryOrange = primaryOrange,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        textSecondaryColor = textSecondaryColor
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Featured Shops Section
                if (state.featuredShops.isNotEmpty()) {
                    FeaturedShopsSection(
                        shops = state.featuredShops,
                        navController = navController,
                        primaryOrange = primaryOrange,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        textSecondaryColor = textSecondaryColor
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun GreetingSection(
    userName: String,
    textColor: Color,
    textSecondaryColor: Color
) {
    // Waving hand animation
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveRotation by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_rotation"
    )

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
            animationSpec = tween(600),
            initialOffsetY = { -40 }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hey $userName! ",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "👋",
                    fontSize = 28.sp,
                    modifier = Modifier.rotate(waveRotation)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "What would you like to eat today?",
                fontSize = 16.sp,
                color = textSecondaryColor,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun MealTimingCard(
    currentMeal: MealInfo?,
    nextMeal: MealInfo?,
    countdown: String,
    navController: NavController,
    primaryOrange: Color,
    surfaceColor: Color,
    textColor: Color,
    textSecondaryColor: Color
) {
    val meal = currentMeal ?: nextMeal

    if (meal != null) {
        val mealColors = getMealColors(meal.backgroundType)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clickable { navController.navigate(MealsRoute) },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top section with gradient background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    mealColors.first,
                                    mealColors.second
                                )
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            // Status and icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.mess),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (currentMeal != null) "Now Serving" else "Next Meal",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Meal name
                            Text(
                                text = meal.name,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Timing
                            Text(
                                text = "${meal.startTime} - ${meal.endTime}",
                                fontSize = 15.sp,
                                color = Color.White.copy(alpha = 0.95f),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Arrow icon
                        Icon(
                            painter = painterResource(id = R.drawable.delivery),
                            contentDescription = "View details",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom section with white background
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceColor)
                        .padding(20.dp)
                ) {
                    // Countdown with pulse animation (if available)
                    if (countdown.isNotEmpty()) {
                        val scale by rememberInfiniteTransition(label = "countdown").animateFloat(
                            initialValue = 1f,
                            targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "countdown_scale"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = mealColors.first.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "⏱️",
                                fontSize = 16.sp,
                                modifier = Modifier.scale(scale)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = countdown,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = mealColors.first,
                                modifier = Modifier.scale(scale)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Available Items header
                    Text(
                        text = "Available Items",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Items in chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(meal.items.take(3)) { item ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFFF0F0F0),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = item,
                                    fontSize = 13.sp,
                                    color = textColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // View full menu and See All
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View full menu",
                            fontSize = 14.sp,
                            color = textSecondaryColor,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "See All",
                                fontSize = 14.sp,
                                color = primaryOrange,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "→",
                                fontSize = 16.sp,
                                color = primaryOrange,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationCardsSection(
    navController: NavController,
    primaryOrange: Color,
    surfaceColor: Color,
    textColor: Color,
    textSecondaryColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Quick Access",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2x2 Grid of square cards
        val cards = listOf(
            Triple("Today's Mess", R.drawable.mess, false),
            Triple("Campus Shops", R.drawable.shopsicon, false),
            Triple("Maps", R.drawable.delivery, false),
            Triple("Announcements", 0, true)
        )

        val routes = listOf(MealsRoute, ShopsRoute, CampusMapRoute, NotificationRoute)

        // Grid layout - 2 columns
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // First row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                cards.take(2).forEachIndexed { index, (title, iconRes, useMatIcon) ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(100L * index)
                        visible = true
                    }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(400)) + scaleIn(
                            animationSpec = tween(400),
                            initialScale = 0.8f
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        SquareNavigationCard(
                            title = title,
                            iconRes = iconRes,
                            useMatIcon = useMatIcon,
                            gradient = getCardGradient(index),
                            onClick = { navController.navigate(routes[index]) },
                            surfaceColor = surfaceColor,
                            textColor = textColor
                        )
                    }
                }
            }

            // Second row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                cards.drop(2).forEachIndexed { idx, (title, iconRes, useMatIcon) ->
                    val index = idx + 2
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(100L * index)
                        visible = true
                    }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(400)) + scaleIn(
                            animationSpec = tween(400),
                            initialScale = 0.8f
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        SquareNavigationCard(
                            title = title,
                            iconRes = iconRes,
                            useMatIcon = useMatIcon,
                            gradient = getCardGradient(index),
                            onClick = { navController.navigate(routes[index]) },
                            surfaceColor = surfaceColor,
                            textColor = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SquareNavigationCard(
    title: String,
    iconRes: Int,
    useMatIcon: Boolean = false,
    gradient: Pair<Color, Color>,
    onClick: () -> Unit,
    surfaceColor: Color,
    textColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                gradient.first.copy(alpha = 0.15f),
                                gradient.second.copy(alpha = 0.25f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(gradient.first, gradient.second)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (useMatIcon) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = title,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = title,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TodaysSpecialSection(
    shop: ShopClickData,
    navController: NavController,
    primaryOrange: Color,
    surfaceColor: Color,
    textColor: Color,
    textSecondaryColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Special",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "🔥", fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pulsing glow effect
        val infiniteTransition = rememberInfiniteTransition(label = "glow")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_alpha"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clickable { navController.navigate(ShopsRoute) },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Glow effect
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryOrange.copy(alpha = glowAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shop image placeholder
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                color = primaryOrange.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.shopsicon),
                            contentDescription = shop.shopName,
                            tint = primaryOrange,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = shop.shopName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "⭐", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (shop.rating > 0) String.format("%.1f", shop.rating) else "New",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "👁️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${shop.totalClicks} views today",
                                fontSize = 13.sp,
                                color = textSecondaryColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturedShopsSection(
    shops: List<ShopClickData>,
    navController: NavController,
    primaryOrange: Color,
    surfaceColor: Color,
    textColor: Color,
    textSecondaryColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Featured Shops",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(shops) { index, shop ->
                FeaturedShopCard(
                    shop = shop,
                    rank = index + 2,
                    onClick = { navController.navigate(ShopsRoute) },
                    primaryOrange = primaryOrange,
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor
                )
            }
        }
    }
}

@Composable
fun FeaturedShopCard(
    shop: ShopClickData,
    rank: Int,
    onClick: () -> Unit,
    primaryOrange: Color,
    surfaceColor: Color,
    textColor: Color,
    textSecondaryColor: Color
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Ranking badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = primaryOrange.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryOrange
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Shop image placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        color = primaryOrange.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.shopsicon),
                    contentDescription = shop.shopName,
                    tint = primaryOrange,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = shop.shopName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "⭐", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (shop.rating > 0) String.format("%.1f", shop.rating) else "New",
                    fontSize = 12.sp,
                    color = textSecondaryColor
                )
            }
        }
    }
}

// Helper functions
private fun getMealColors(mealType: String): Pair<Color, Color> {
    return when (mealType) {
        "breakfast" -> Pair(Color(0xFFFFB84D), Color(0xFFFF8C42))
        "lunch" -> Pair(Color(0xFF4CAF50), Color(0xFF8BC34A))
        "snacks" -> Pair(Color(0xFFFF6B6B), Color(0xFFFF8E53))
        "dinner" -> Pair(Color(0xFF5C6BC0), Color(0xFF7E57C2))
        else -> Pair(Color(0xFFFF6B35), Color(0xFFFF8C42))
    }
}

private fun getMealEmoji(mealType: String): String {
    return when (mealType) {
        "breakfast" -> "🌅"
        "lunch" -> "🍛"
        "snacks" -> "☕"
        "dinner" -> "🌙"
        else -> "🍽️"
    }
}

private fun getCardGradient(index: Int): Pair<Color, Color> {
    return when (index) {
        0 -> Pair(Color(0xFFFF6B35), Color(0xFFFF8C42))
        1 -> Pair(Color(0xFF4CAF50), Color(0xFF8BC34A))
        2 -> Pair(Color(0xFF5C6BC0), Color(0xFF7E57C2))
        3 -> Pair(Color(0xFFFF6B35), Color(0xFFFF8C42))
        else -> Pair(Color(0xFFFF6B35), Color(0xFFFF8C42))
    }
}
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = primaryOrange.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryOrange
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "⭐", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format("%.1f", shop.rating),
                    fontSize = 12.sp,
                    color = textSecondaryColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = shop.cuisine,
                fontSize = 11.sp,
                color = textSecondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Helper functions
private fun getMealColors(mealType: String): Pair<Color, Color> {
    return when (mealType) {
        "breakfast" -> Pair(Color(0xFFFFB84D), Color(0xFFFF8C42))
        "lunch" -> Pair(Color(0xFF4CAF50), Color(0xFF8BC34A))
        "snacks" -> Pair(Color(0xFFFF6B6B), Color(0xFFFF8E53))
        "dinner" -> Pair(Color(0xFF5C6BC0), Color(0xFF7E57C2))
        else -> Pair(Color(0xFFFF6B35), Color(0xFFFF8C42))
    }
}

private fun getMealEmoji(mealType: String): String {
    return when (mealType) {
        "breakfast" -> "🌅"
        "lunch" -> "🍛"
        "snacks" -> "☕"
        "dinner" -> "🌙"
        else -> "🍽️"
    }
}

private fun getCardGradient(index: Int): Pair<Color, Color> {
    return when (index) {
        0 -> Pair(Color(0xFFFF6B35), Color(0xFFFF8C42))
        1 -> Pair(Color(0xFF4CAF50), Color(0xFF8BC34A))
        2 -> Pair(Color(0xFF5C6BC0), Color(0xFF7E57C2))
        3 -> Pair(Color(0xFFFF6B35), Color(0xFFFF8C42))
        else -> Pair(Color(0xFFFF6B35), Color(0xFFFF8C42))
    }
}