package com.divyansh.cueats

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.divyansh.cueats.LoginScreen.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController? = null,
    authViewModel: AuthViewModel? = null
) {
    val isDarkTheme = isSystemInDarkTheme()

    // Modern gradient colors that adapt to theme
    val gradientColors = if (isDarkTheme) {
        listOf(
            Color(0xFF1A1A2E),
            Color(0xFF16213E),
            Color(0xFF0F3460)
        )
    } else {
        listOf(
            Color(0xFFFF6B35), // Your brand orange
            Color(0xFFFF8A50),
            Color(0xFFFFA365)
        )
    }

    val accentColor = if (isDarkTheme) Color(0xFFFF6B35) else Color(0xFFFFFFFF)
    val textColor = if (isDarkTheme) Color(0xFFE8E8E8) else Color(0xFFFFFFFF)
    val subtitleColor = if (isDarkTheme) Color(0xFFB0B0B0) else Color(0xFFF0F0F0)

    // Animation states - FIXED: Initialize as true so animations start immediately
    var startAnimation by remember { mutableStateOf(false) }

    // Logo animations
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "logo_alpha"
    )

    // Text animations with staggered delays
    val titleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 500),
        label = "title_alpha"
    )

    val subtitleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 800),
        label = "subtitle_alpha"
    )

    // Pulsing effect for logo background
    val infiniteTransition = rememberInfiniteTransition(label = "infinite")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // FIXED: Start animations immediately and handle navigation properly
    LaunchedEffect(Unit) {
        startAnimation = true // Start animations immediately

        // Wait for animations to complete and splash duration
        delay(3000) // Total splash duration

        navController?.let { nav ->
            authViewModel?.let { auth ->
                val authState = auth.authState.value

                // Navigate based on auth state
                if (authState.isLoggedIn && authState.user != null) {
                    nav.navigate(HomeRoute) {
                        popUpTo<SplashRoute> { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    nav.navigate(LoginRoute) {
                        popUpTo<SplashRoute> { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } ?: run {
                // Fallback if authViewModel is null
                nav.navigate(LoginRoute) {
                    popUpTo<SplashRoute> { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    // FIXED: Use Column with proper padding instead of Box to avoid status bar issues
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = gradientColors,
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
            .statusBarsPadding(), // This ensures proper status bar handling
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo container with pulsing background
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .scale(pulseScale)
                .background(
                    color = accentColor.copy(alpha = 0.1f),
                    shape = CircleShape
                )
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    ambientColor = accentColor.copy(alpha = 0.3f),
                    spotColor = accentColor.copy(alpha = 0.3f)
                )
        ) {
            // Logo with rounded corners
            Image(
                painter = painterResource(id = R.drawable.logo33),
                contentDescription = "CuEats Logo",
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(24.dp)) // Rounded corners
                    .scale(logoScale)
                    .alpha(logoAlpha),
                contentScale = ContentScale.Crop // Changed to Crop for better rounded corner effect
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // App title with modern typography
        Text(
            text = "Campus Eats",
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            color = accentColor,
            modifier = Modifier.alpha(titleAlpha),
            textAlign = TextAlign.Center,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Subtitle
        Text(
            text = "Discover Your Campus Cuisine",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = subtitleColor,
            modifier = Modifier.alpha(subtitleAlpha),
            textAlign = TextAlign.Center,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Secondary subtitle
        Text(
            text = "Fresh • Fast • Delicious",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = subtitleColor.copy(alpha = 0.8f),
            modifier = Modifier.alpha(subtitleAlpha),
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(64.dp))

        // Loading indicator at bottom
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .alpha(titleAlpha)
                .padding(16.dp)
        ) {
            repeat(3) { index ->
                val dotAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(index * 200)
                    ),
                    label = "dot_$index"
                )

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = accentColor.copy(alpha = dotAlpha),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    MaterialTheme {
        SplashScreen()
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SplashScreenDarkPreview() {
    MaterialTheme {
        SplashScreen()
    }
}