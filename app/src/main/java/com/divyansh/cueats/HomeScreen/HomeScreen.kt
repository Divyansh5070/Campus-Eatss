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
import com.divyansh.cueats.Mess.playfairFont
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

        Spacer(modifier = Modifier.height(16.dp))

        // Card data with descriptions
        val cards = listOf(
            QuickAccessCard(
                title = "Today's Mess",
                description = "Check what's cooking",
                iconRes = R.drawable.mess,
                useMatIcon = false,
                emoji = "🍽️"
            ),
            QuickAccessCard(
                title = "Campus Shops",
                description = "Explore nearby options",
                iconRes = R.drawable.shopsicon,
                useMatIcon = false,
                emoji = "🏪"
            ),
            QuickAccessCard(
                title = "Maps",
                description = "Navigate campus",
                iconRes = R.drawable.delivery,
                useMatIcon = false,
                emoji = "🗺️"
            ),
            QuickAccessCard(
                title = "Announcements",
                description = "Stay updated",
                iconRes = 0,
                useMatIcon = true,
                emoji = "📢"
            )
        )

        val routes = listOf(MealsRoute, ShopsRoute, CampusMapRoute, NotificationRoute)

        // Grid layout - 2 columns
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // First row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                cards.take(2).forEachIndexed { index, card ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(100L * index)
                        visible = true
                    }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(500)) + scaleIn(
                            animationSpec = tween(500),
                            initialScale = 0.85f
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        ImprovedNavigationCard(
                            card = card,
                            gradient = getCardGradient(index),
                            onClick = { navController.navigate(routes[index]) },
                            surfaceColor = surfaceColor,
                            textColor = textColor,
                            textSecondaryColor = textSecondaryColor
                        )
                    }
                }
            }

            // Second row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                cards.drop(2).forEachIndexed { idx, card ->
                    val index = idx + 2
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(100L * index)
                        visible = true
                    }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(500)) + scaleIn(
                            animationSpec = tween(500),
                            initialScale = 0.85f
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        ImprovedNavigationCard(
                            card = card,
                            gradient = getCardGradient(index),
                            onClick = { navController.navigate(routes[index]) },
                            surfaceColor = surfaceColor,
                            textColor = textColor,
                            textSecondaryColor = textSecondaryColor
                        )
                    }
                }
            }
        }
    }
}

data class QuickAccessCard(
    val title: String,
    val description: String,
    val iconRes: Int,
    val useMatIcon: Boolean,
    val emoji: String
)

@Composable
fun ImprovedNavigationCard(
    card: QuickAccessCard,
    gradient: Pair<Color, Color>,
    onClick: () -> Unit,
    surfaceColor: Color,
    textColor: Color,
    textSecondaryColor: Color
) {
    // Hover/press animation
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clickable {
                isPressed = true
                onClick()
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            gradient.first.copy(alpha = 0.85f),
                            gradient.second.copy(alpha = 0.95f)
                        )
                    )
                )
        ) {
            // Decorative circles in background
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = (-30).dp, y = (-30).dp)
                    .background(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(60.dp)
                    )
            )

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 20.dp, y = 20.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(40.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon section
                Column {
                    // Icon with background
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (card.useMatIcon) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = card.title,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = card.iconRes),
                                contentDescription = card.title,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Text section
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = card.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = card.description,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// Keep the existing getCardGradient function
private fun getCardGradient(index: Int): Pair<Color, Color> {
    return when (index) {
        0 -> Pair(Color(0xFFFF6B35), Color(0xFFFF8C42)) // Orange gradient
        1 -> Pair(Color(0xFF5C7CFA), Color(0xFF748FFC)) // Blue-Purple gradient
        2 -> Pair(Color(0xFF7950F2), Color(0xFF9775FA)) // Purple gradient
        3 -> Pair(Color(0xFFFF6B35), Color(0xFFFF8C42)) // Orange gradient
        else -> Pair(Color(0xFFFF6B35), Color(0xFFFF8C42))
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

