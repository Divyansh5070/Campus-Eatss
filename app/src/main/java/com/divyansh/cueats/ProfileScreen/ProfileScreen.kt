package com.divyansh.cueats.ProfileScreen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.divyansh.cueats.LoginRoute
import com.divyansh.cueats.LoginScreen.AuthViewModel
import com.divyansh.cueats.Notification.AboutScreen
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState
    val user = authState.user
    
    // State for showing logout dialog
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAboutSection by remember { mutableStateOf(false) }
    
    // Light theme colors
    val backgroundColor = Color(0xFFF6F7FB)
    val surfaceColor = Color.White
    val primaryOrange = Color(0xFFFF6B01)
    val textColor = Color(0xFF2D2D2D)
    val textSecondaryColor = Color(0xFF8A8A8A)
    
    // Animation states
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }
    
    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Sign Out",
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out?",
                    color = textSecondaryColor
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.signOut()
                        navController.navigate(LoginRoute) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text("Sign Out", color = primaryOrange)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Cancel", color = textSecondaryColor)
                }
            },
            containerColor = surfaceColor
        )
    }
    
    // About Section Dialog
    if (showAboutSection) {
        AboutScreen(navController = navController)
        showAboutSection = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryOrange,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Profile Picture with animation
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600)) + scaleIn(
                    animationSpec = tween(600),
                    initialScale = 0.8f
                )
            ) {
                ProfilePictureSection(
                    photoUrl = user?.photoUrl?.toString(),
                    primaryOrange = primaryOrange,
                    surfaceColor = surfaceColor
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // User Info Card with animation
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) + 
                        slideInVertically(
                            animationSpec = tween(600, delayMillis = 200),
                            initialOffsetY = { 50 }
                        )
            ) {
                UserInfoCard(
                    name = user?.displayName ?: "User",
                    email = user?.email ?: "No email",
                    uid = user?.uid ?: "N/A",
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor,
                    primaryOrange = primaryOrange
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Bio Section
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 300)) + 
                        slideInVertically(
                            animationSpec = tween(600, delayMillis = 300),
                            initialOffsetY = { 50 }
                        )
            ) {
                BioSection(
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor,
                    primaryOrange = primaryOrange
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Quick Actions
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 400)) + 
                        slideInVertically(
                            animationSpec = tween(600, delayMillis = 400),
                            initialOffsetY = { 50 }
                        )
            ) {
                QuickActionsSection(
                    onAboutClick = { navController.navigate(com.divyansh.cueats.AboutRoute) },
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor,
                    primaryOrange = primaryOrange
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Logout Button with animation
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 500)) + 
                        slideInVertically(
                            animationSpec = tween(600, delayMillis = 500),
                            initialOffsetY = { 50 }
                        )
            ) {
                LogoutButton(
                    onClick = { showLogoutDialog = true },
                    primaryOrange = primaryOrange
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ProfilePictureSection(
    photoUrl: String?,
    primaryOrange: Color,
    surfaceColor: Color
) {
    // Pulse animation for the outer ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    Box(
        contentAlignment = Alignment.Center
    ) {
        // Outer animated ring
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(pulseScale)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primaryOrange.copy(alpha = 0.3f),
                            primaryOrange.copy(alpha = 0.1f)
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Profile picture container
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(surfaceColor, CircleShape)
                .border(4.dp, primaryOrange, CircleShape)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback icon
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(60.dp),
                    tint = primaryOrange
                )
            }
        }
    }
}

@Composable
fun UserInfoCard(
    name: String,
    email: String,
    uid: String,
    surfaceColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    primaryOrange: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Name
            Text(
                text = name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Email
            Text(
                text = email,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = textSecondaryColor
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(2.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                primaryOrange.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // UID Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "User ID",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondaryColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uid.take(20) + "...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = textColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Account type badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = primaryOrange.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = primaryOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Google Account",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryOrange
                    )
                }
            }
        }
    }
}

@Composable
fun BioSection(
    surfaceColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    primaryOrange: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Bio",
                    tint = primaryOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "About Me",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Campus food enthusiast 🍽️\nAlways on the hunt for the best meals on campus!",
                fontSize = 15.sp,
                color = textSecondaryColor,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun QuickActionsSection(
    onAboutClick: () -> Unit,
    surfaceColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    primaryOrange: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // About App
            ActionItem(
                icon = Icons.Default.Info,
                title = "About Campus Eats",
                subtitle = "Learn more about the app",
                primaryOrange = primaryOrange,
                textColor = textColor,
                textSecondaryColor = textSecondaryColor,
                onClick = onAboutClick
            )
        }
    }
}

@Composable
fun ActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    primaryOrange: Color,
    textColor: Color,
    textSecondaryColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(primaryOrange.copy(alpha = 0.05f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(primaryOrange.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = primaryOrange,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = textSecondaryColor
            )
        }
        
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "Go",
            tint = textSecondaryColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun LogoutButton(
    onClick: () -> Unit,
    primaryOrange: Color
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = primaryOrange
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Icon(
            imageVector = Icons.Default.ExitToApp,
            contentDescription = "Logout",
            modifier = Modifier.size(24.dp),
            tint = Color.White
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = "Sign Out",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
