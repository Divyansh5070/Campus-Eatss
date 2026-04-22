package com.divyansh.cueats.HomeScreen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
import com.divyansh.cueats.ShopMenuRoute
import com.divyansh.cueats.AppBottomNavigation
import com.divyansh.cueats.Mess.playfairFont
import com.divyansh.cueats.NotificationRoute
import com.divyansh.cueats.ProfileRoute
import com.divyansh.cueats.Notification.NotificationIconWithBadge
import com.divyansh.cueats.Notification.NotificationViewModel
import com.airbnb.lottie.compose.*
import com.divyansh.cueats.ShopsScreen.Shop
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

// Helper functions for meal colors and emojis - Rich deep theme
fun getMealColors(mealType: String): Pair<Color, Color> {
    return when (mealType) {
        "breakfast" -> Pair(Color(0xFF4ECDC4), Color(0xFF45B7AF)) // Bright Teal - Energizing morning
        "lunch" -> Pair(Color(0xFFFF6B6B), Color(0xFFFFA07A)) // Coral/Salmon - Bold midday
        "snacks" -> Pair(Color(0xFFA29BFE), Color(0xFF8B7FE8)) // Soft Purple - Vibrant afternoon
        "dinner" -> Pair(Color(0xFF74B9FF), Color(0xFF5F9FE8)) // Soft Blue - Elegant evening
        else -> Pair(Color(0xFF4ECDC4), Color(0xFF45B7AF)) // Bright Teal - Default
    }
}

fun getMealEmoji(mealType: String): String {
    return when (mealType) {
        "breakfast" -> "🌅"
        "lunch" -> "🍛"
        "snacks" -> "☕"
        "dinner" -> "🌙"
        else -> "🍽️"
    }
}

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

    // Soft Pastel theme - Vibrant, friendly colors with modern appeal
    val backgroundColor = Color(0xFFF5F5F5) // Light gray background
    val surfaceColor = Color.White
    val primaryBlue = Color(0xFFF67249) // Bright teal accent (keeping variable name for compatibility)
    val textColor = Color(0xFF2D3436) // Dark gray for text
    val textSecondaryColor = Color(0xFF636E72) // Medium gray for secondary text

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Campus Life",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = playfairFont,
                            letterSpacing = 1.sp,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.25f),
                                    offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                    blurRadius = 4f
                                )
                            )
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
                                .size(36.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 2.dp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryBlue,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        // Box to overlay navigation on content
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content
            if (state.isLoading) {
                // Shimmer loading instead of circular progress
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Greeting shimmer
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        com.divyansh.cueats.common.ShimmerText(width = 0.5f, height = 28)
                        Spacer(modifier = Modifier.height(8.dp))
                        com.divyansh.cueats.common.ShimmerText(width = 0.7f, height = 16)
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Meal card shimmer
                    com.divyansh.cueats.common.ShimmerMealCard()
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Quick access cards shimmer
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        com.divyansh.cueats.common.ShimmerText(width = 0.4f, height = 20)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            com.divyansh.cueats.common.ShimmerCard(
                                modifier = Modifier.weight(1f),
                                height = 140
                            )
                            com.divyansh.cueats.common.ShimmerCard(
                                modifier = Modifier.weight(1f),
                                height = 140
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 100.dp) // Extra padding for floating nav
                ) {
                    // Orange Gradient Background Section with Greeting (matching top bar)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        primaryBlue, // Orange (same as top bar)
                                        Color(0xFFE85D2F)  // Slightly darker orange
                                    )
                                ),
                                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                            )
                            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                            .padding(bottom = 40.dp) // Extra padding to extend background for overlap
                    ) {
                        // Greeting Section with white text
                        GreetingSection(
                            userName = state.userName.ifEmpty { "Foodie" },
                            textColor = Color.White,
                            textSecondaryColor = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    // Current/Next Meal Card with negative margin for overlap
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-30).dp) // Negative offset to overlap with gradient
                    ) {
                        MealTimingCard(
                            currentMeal = state.currentMeal,
                            nextMeal = state.nextMeal,
                            countdown = state.mealCountdown,
                            navController = navController,
                            primaryBlue = primaryBlue,
                            surfaceColor = surfaceColor,
                            textColor = textColor,
                            textSecondaryColor = textSecondaryColor
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Access Section
                    NavigationCardsSection(
                        navController = navController,
                        primaryBlue = primaryBlue,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        textSecondaryColor = textSecondaryColor
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Featured Shops Section
                    if (state.featuredShops.isNotEmpty()) {
                        FeaturedShopsSection(
                            shops = state.featuredShops,
                            navController = navController,
                            primaryBlue = primaryBlue,
                            surfaceColor = surfaceColor,
                            textColor = textColor,
                            textSecondaryColor = textSecondaryColor
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
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
                    currentRoute = "home"
                )
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
                .padding(top = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Hey ${userName.trim().split(" ").firstOrNull()?.trim()?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Foodie"}! ",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
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
fun FlipClockDigit(
    digit: Char,
    accentColor: Color
) {
    var currentDigit by remember { mutableStateOf(digit) }
    var previousDigit by remember { mutableStateOf(digit) }
    var isFlipping by remember { mutableStateOf(false) }
    
    // Trigger flip animation when digit changes
    LaunchedEffect(digit) {
        if (digit != currentDigit) {
            previousDigit = currentDigit
            isFlipping = true
            delay(350) // Duration of flip animation
            currentDigit = digit
            isFlipping = false
        }
    }
    
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 52.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isFlipping) {
            // Animated flip transition
            FlipAnimation(
                previousDigit = previousDigit,
                currentDigit = currentDigit,
                accentColor = accentColor
            )
        } else {
            // Static split-flap display
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top half
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.18f),
                                    accentColor.copy(alpha = 0.12f)
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            color = accentColor.copy(alpha = 0.25f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentDigit.toString(),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.offset(y = 13.dp)
                    )
                }
                
                // Middle divider line (flip mechanism)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(
                            Color.Black.copy(alpha = 0.08f)
                        )
                )
                
                // Bottom half
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.12f),
                                    accentColor.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            color = accentColor.copy(alpha = 0.25f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentDigit.toString(),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor.copy(alpha = 0.85f),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.offset(y = (-13).dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FlipAnimation(
    previousDigit: Char,
    currentDigit: Char,
    accentColor: Color
) {
    // Use a key to reset animation state when digits change
    var animationKey by remember { mutableStateOf(0) }
    
    LaunchedEffect(currentDigit) {
        animationKey++
    }
    
    // Rotation animation from 0 to 180 degrees - reset with key
    var targetRotation by remember { mutableStateOf(0f) }
    
    LaunchedEffect(animationKey) {
        targetRotation = 0f
        delay(50) // Small delay to ensure reset
        targetRotation = 180f
    }
    
    val rotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(
            durationMillis = 350,
            easing = FastOutSlowInEasing
        ),
        label = "flip_rotation"
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Static top half - current digit (always visible)
        if (rotation >= 90f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.18f),
                                accentColor.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        color = accentColor.copy(alpha = 0.25f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentDigit.toString(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.offset(y = 13.dp)
                )
            }
        }
        
        // Animated top half - previous digit (flips down)
        if (rotation < 90f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        rotationX = rotation
                        cameraDistance = 8f * density
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        shadowElevation = 8f
                    }
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.18f),
                                accentColor.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        color = accentColor.copy(alpha = 0.25f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = previousDigit.toString(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.offset(y = 13.dp)
                )
            }
        }
        
        // Middle divider line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .align(Alignment.Center)
                .background(
                    Color.Black.copy(alpha = 0.08f)
                )
        )
        
        // Static bottom half - previous digit (visible until flip completes)
        if (rotation < 90f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.12f),
                                accentColor.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        color = accentColor.copy(alpha = 0.25f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = previousDigit.toString(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor.copy(alpha = 0.85f),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.offset(y = (-13).dp)
                )
            }
        }
        
        // Animated bottom half - current digit (flips up)
        if (rotation >= 90f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        rotationX = 180f - rotation
                        cameraDistance = 8f * density
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        shadowElevation = 8f
                    }
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.12f),
                                accentColor.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        color = accentColor.copy(alpha = 0.25f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentDigit.toString(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor.copy(alpha = 0.85f),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.offset(y = (-13).dp)
                )
            }
        }
    }
}

@Composable
fun FlipClockUnit(
    value: String,
    label: String,
    accentColor: Color,
    textColor: Color
) {
    // Subtle pulse animation for seconds
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (label == "Seconds") 1.03f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(if (label == "Seconds") pulse else 1f)
    ) {
        // Digit display with flip animation
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Split into individual digits for flip animation
            value.forEach { digit ->
                FlipClockDigit(
                    digit = digit,
                    accentColor = accentColor
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Label
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor.copy(alpha = 0.6f),
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun MealTimingCard(
    currentMeal: MealInfo?,
    nextMeal: MealInfo?,
    countdown: String,
    navController: NavController,
    primaryBlue: Color,
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
                        .height(80.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
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
                            .padding(16.dp),
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
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentMeal != null) "Now Serving" else "Next Meal",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Meal name
                            Text(
                                text = meal.name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // Timing
                            Text(
                                text = "${meal.startTime} - ${meal.endTime}",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.95f),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Animated food icon using Lottie
                        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.preparefood))
                        val progress by animateLottieCompositionAsState(
                            composition = composition,
                            iterations = LottieConstants.IterateForever
                        )
                        
                        Box(
                            modifier = Modifier.size(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LottieAnimation(
                                composition = composition,
                                progress = { progress },
                                modifier = Modifier.size(200.dp)
                            )
                        }
                    }
                }

                // Bottom section with white background
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                        .background(surfaceColor)
                        .padding(20.dp)
                ) {
                    // Flip-clock style countdown (if available)
                    if (countdown.isNotEmpty()) {
                        // Parse countdown text to extract time components
                        val timePattern = "(\\d{2}):(\\d{2}):(\\d{2})".toRegex()
                        val matchResult = timePattern.find(countdown)
                        
                        if (matchResult != null) {
                            val (hours, minutes, seconds) = matchResult.destructured
                            val statusText = if (countdown.startsWith("Ends")) "Ends in" else "Starts in"
                            
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Status text
                                Text(
                                    text = statusText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textSecondaryColor,
                                    letterSpacing = 0.5.sp
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Flip clock display
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Hours
                                    FlipClockUnit(
                                        value = hours,
                                        label = "Hours",
                                        accentColor = mealColors.first,
                                        textColor = textColor
                                    )
                                    
                                    // Separator
                                    Text(
                                        text = ":",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = mealColors.first,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    
                                    // Minutes
                                    FlipClockUnit(
                                        value = minutes,
                                        label = "Minutes",
                                        accentColor = mealColors.first,
                                        textColor = textColor
                                    )
                                    
                                    // Separator
                                    Text(
                                        text = ":",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = mealColors.first,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    
                                    // Seconds
                                    FlipClockUnit(
                                        value = seconds,
                                        label = "Seconds",
                                        accentColor = mealColors.first,
                                        textColor = textColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // View full menu footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tap to view full menu",
                            fontSize = 14.sp,
                            color = textSecondaryColor,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "View Menu",
                                fontSize = 14.sp,
                                color = primaryBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "→",
                                fontSize = 16.sp,
                                color = primaryBlue,
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
    primaryBlue: Color,
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
                iconRes = R.drawable.map,
                useMatIcon = false,
                emoji = "🗺️"
            ),
            QuickAccessCard(
                title = "Events",
                description = "Stay updated",
                iconRes = R.drawable.events,
                useMatIcon = true,
                emoji = "📢"
            )
        )

        val routes = listOf(MealsRoute, ShopsRoute, CampusMapRoute, com.divyansh.cueats.EventsRoute)

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
            .height(160.dp)
            .scale(scale)
            .clickable {
                isPressed = true
                onClick()
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 10.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            gradient.first,
                            gradient.second
                        )
                    )
                )
        ) {
            // Decorative circles in background - larger and more prominent
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .offset(x = (-40).dp, y = (-40).dp)
                    .background(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(70.dp)
                    )
            )

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 30.dp, y = 30.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(50.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon section - larger and more prominent
                Column {
                    // Icon with background
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (card.useMatIcon) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = card.title,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = card.iconRes),
                                contentDescription = card.title,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
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
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = card.description,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.95f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// Soft pastel gradients - Vibrant, friendly colors
private fun getCardGradient(index: Int): Pair<Color, Color> {
    return when (index) {
        0 -> Pair(Color(0xFF4ECDC4), Color(0xFF45B7AF)) // Bright Teal - Today's Mess
        1 -> Pair(Color(0xFFFF6B6B), Color(0xFFFFA07A)) // Coral/Salmon - Campus Shops
        2 -> Pair(Color(0xFFA29BFE), Color(0xFF8B7FE8)) // Soft Purple - Maps
        3 -> Pair(Color(0xFF74B9FF), Color(0xFF5F9FE8)) // Soft Blue - Events
        else -> Pair(Color(0xFF4ECDC4), Color(0xFF45B7AF))
    }
}

@Composable
fun FeaturedShopsSection(
    shops: List<Shop>,
    navController: NavController,
    primaryBlue: Color,
    surfaceColor: Color,
    textColor: Color,
    textSecondaryColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // Header with "See All" button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Featured Shops",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    navController.navigate(ShopsRoute)
                }
            ) {
                Text(
                    text = "See All",
                    fontSize = 14.sp,
                    color = primaryBlue,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "→",
                    fontSize = 16.sp,
                    color = primaryBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Horizontal scrolling shop cards
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(shops) { shop ->
                FeaturedShopCard(
                    shop = shop,
                    onClick = {
                        navController.navigate(ShopMenuRoute(shop.id))
                    },
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor,
                    primaryBlue = primaryBlue
                )
            }
        }
    }
}

@Composable
fun FeaturedShopCard(
    shop: Shop,
    onClick: () -> Unit,
    surfaceColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    primaryBlue: Color
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Shop Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                // Load shop image using Coil
                AsyncImage(
                    model = shop.imageUrl,
                    contentDescription = shop.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Delivery badge if applicable
                if (shop.hasDelivery) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(
                                color = Color(0xFF4CAF50),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.delivery),
                                contentDescription = "Delivery",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Delivery",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Shop Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Shop Name
                Text(
                    text = shop.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Location
                Text(
                    text = shop.location,
                    fontSize = 13.sp,
                    color = textSecondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Cuisine type badge
                Box(
                    modifier = Modifier
                        .background(
                            color = primaryBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = shop.cuisine,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = primaryBlue
                    )
                }
            }
        }
    }
}


