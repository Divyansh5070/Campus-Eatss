package com.divyansh.cueats.AnnouncementScreen

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.divyansh.cueats.R
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(
    navController: NavController,
    eventId: String,
    viewModel: EventViewModel = viewModel()
) {
    val state by viewModel.eventDetailsState.collectAsState()
    var showClubProfileDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Load event details
    LaunchedEffect(eventId) {
        viewModel.loadEventDetails(eventId)
    }
    
    // Load club profile when event is loaded
    LaunchedEffect(state.event?.clubId) {
        state.event?.clubId?.let { clubId ->
            if (clubId.isNotBlank()) {
                viewModel.loadClubProfile(clubId)
            }
        }
    }
    
    // Colors - Light theme matching reference
    val backgroundColor = Color(0xFFE8EAF6) // Light purple-blue background
    val surfaceColor = Color.White
    val primaryBlue = Color(0xFF4285F4) // Blue for primary actions
    val textColor = Color(0xFF2D2D2D)
    val textSecondaryColor = Color(0xFF8A8A8A)
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (state.isLoading) {
            com.divyansh.cueats.common.ShimmerEventDetails()
        } else if (state.event != null) {
            val event = state.event!!
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .verticalScroll(rememberScrollState())
            ) {
                // Banner Image with Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp)
                ) {
                    // Banner Image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2D2D2D))
                    ) {
                        if (event.bannerUrl.isNotEmpty()) {
                            AsyncImage(
                                model = event.bannerUrl,
                                contentDescription = event.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (event.imageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = event.imageUrl,
                                contentDescription = event.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        // Gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.3f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.5f)
                                        )
                                    )
                                )
                        )
                    }
                    
                    // Top Bar Icons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Back Button
                        IconButton(
                            onClick = { navController.navigateUp() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Share Button
                            IconButton(
                                onClick = { 
                                    // Share event details
                                    val shareText = buildString {
                                        appendLine("📢 ${event.title}")
                                        appendLine()
                                        appendLine("📅 Date: ${event.getFormattedDate()}")
                                        appendLine("⏰ Time: ${event.startTime} - ${event.endTime}")
                                        appendLine("📍 Venue: ${event.venue}")
                                        if (event.prizePool.isNotEmpty()) {
                                            appendLine("🏆 Prize Pool: ₹${event.prizePool}")
                                        }
                                        appendLine()
                                        appendLine("📝 ${event.description.take(150)}${if (event.description.length > 150) "..." else ""}")
                                        appendLine()
                                        if (event.registrationUrl.isNotEmpty()) {
                                            val url = if (!event.registrationUrl.startsWith("http://") && 
                                                        !event.registrationUrl.startsWith("https://")) {
                                                "https://${event.registrationUrl}"
                                            } else {
                                                event.registrationUrl
                                            }
                                            appendLine("🔗 Register: $url")
                                            appendLine()
                                        }
                                        appendLine("━━━━━━━━━━━━━━━━")
                                        appendLine("📱 View in CUEats App:")
                                        appendLine("cueats://event/${event.eventId}")
                                        appendLine()
                                        appendLine("Don't have CUEats? Download now! 🚀")
                                    }
                                    
                                    val shareIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(
                                        android.content.Intent.createChooser(shareIntent, "Share Event")
                                    )
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Event",
                                    tint = Color.White
                                )
                            }
                            
                            // Download Button
                            IconButton(
                                onClick = { 
                                    // Download the event poster
                                    val imageUrl = if (event.bannerUrl.isNotEmpty()) {
                                        event.bannerUrl
                                    } else {
                                        event.imageUrl
                                    }
                                    
                                    if (imageUrl.isNotEmpty()) {
                                        try {
                                            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                            val uri = Uri.parse(imageUrl)
                                            val request = DownloadManager.Request(uri).apply {
                                                setTitle("${event.title} - Poster")
                                                setDescription("Downloading event poster...")
                                                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                setDestinationInExternalPublicDir(
                                                    Environment.DIRECTORY_DOWNLOADS,
                                                    "${event.title.replace(" ", "_")}_poster.jpg"
                                                )
                                                setAllowedOverMetered(true)
                                                setAllowedOverRoaming(true)
                                            }
                                            downloadManager.enqueue(request)
                                            Toast.makeText(context, "Downloading poster...", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "No poster available", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download Poster",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    
                    // Category Tag
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 16.dp, top = 100.dp),
                        color = Color(event.getCategoryColor()),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = event.category,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Content Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Event Title
                    Text(
                        text = event.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        lineHeight = 32.sp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Date Card - Light blue theme
                        InfoCard(
                            iconRes = R.drawable.calendarrrr,
                            label = "DATE",
                            value = event.getFormattedDate(),
                            backgroundColor = Color(0xFFE3F2FD), // Light blue
                            iconTint = Color(0xFF4285F4),
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Time Card - Light cyan theme
                        InfoCard(
                            iconRes = R.drawable.clockkkk,
                            label = "TIME",
                            value = "${event.startTime} - ${event.endTime}",
                            backgroundColor = Color(0xFFE0F7FA), // Light cyan
                            iconTint = Color(0xFF00ACC1),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Organizer Section - Redesigned
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfaceColor, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Store club data in local variable to avoid smart cast issues
                            val clubData = state.club
                            
                            // Club Logo (circular)
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF0F0F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                // Load actual club logo from Firestore
                                when {
                                    // Show club logo if available
                                    clubData?.logoUrl?.isNotEmpty() == true -> {
                                        AsyncImage(
                                            model = clubData.logoUrl,
                                            contentDescription = "Club logo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    // Fallback to club initials
                                    else -> {
                                        Text(
                                            text = (clubData?.name ?: event.organizer).split(" ")
                                                .take(2)
                                                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                                .joinToString("")
                                                .ifEmpty { (clubData?.name ?: event.organizer).take(1).uppercase() },
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryBlue
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(14.dp))
                            
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "ORGANIZED BY",
                                    fontSize = 11.sp,
                                    color = textSecondaryColor,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = clubData?.name?.takeIf { it.isNotEmpty() } ?: event.organizer,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Info Icon Button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE8EAF6))
                                .clickable { showClubProfileDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "View Profile",
                                tint = primaryBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Venue Section - New Design
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfaceColor, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.pin),
                            contentDescription = "Venue",
                            tint = Color(0xFF8A8A8A),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "VENUE",
                                fontSize = 11.sp,
                                color = textSecondaryColor,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = event.venue,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Location Section (commented out)
//                    Text(
//                        text = "Location",
//                        fontSize = 18.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = textColor
//                    )
//
//                    Spacer(modifier = Modifier.height(12.dp))
//
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.delivery),
//                            contentDescription = null,
//                            tint = primaryOrange,
//                            modifier = Modifier.size(20.dp)
//                        )
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text(
//                            text = event.venue,
//                            fontSize = 14.sp,
//                            color = textColor
//                        )
//                    }
                    
//                    Spacer(modifier = Modifier.height(12.dp))
                    
//                    // View on Map Button
//                    Surface(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(120.dp)
//                            .clickable { /* TODO: Open map */ },
//                        color = Color(0xFFF0F0F0),
//                        shape = RoundedCornerShape(12.dp)
//                    ) {
//                        Box(
//                            modifier = Modifier.fillMaxSize(),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Text(
//                                text = "View on Map",
//                                fontSize = 14.sp,
//                                color = textSecondaryColor,
//                                fontWeight = FontWeight.Medium
//                            )
//                        }
//                    }
                    
                    
//                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Target Audience Section
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .background(surfaceColor, RoundedCornerShape(16.dp))
//                            .padding(16.dp),
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(12.dp)
//                    ) {
//                        // Icon based on target audience
//                        val (audienceIcon, audienceColor) = when (event.targetAudience) {
//                            "For Students" -> "🎓" to Color(0xFF667EEA)
//                            "For Developers" -> "💻" to Color(0xFF5C7CFA)
//                            "For Designers" -> "🎨" to Color(0xFFFF6B9D)
//                            "For Entrepreneurs" -> "💼" to Color(0xFFFFB800)
//                            "For Researchers" -> "🔬" to Color(0xFF51CF66)
//                            else -> "🌍" to primaryOrange
//                        }
//
//                        Box(
//                            modifier = Modifier
//                                .size(48.dp)
//                                .clip(CircleShape)
//                                .background(audienceColor.copy(alpha = 0.15f)),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Text(
//                                text = audienceIcon,
//                                fontSize = 24.sp
//                            )
//                        }
//
//                        Column(
//                            modifier = Modifier.weight(1f)
//                        ) {
//                            Text(
//                                text = "Who is this for?",
//                                fontSize = 12.sp,
//                                color = textSecondaryColor
//                            )
//                            Text(
//                                text = event.targetAudience,
//                                fontSize = 16.sp,
//                                fontWeight = FontWeight.Bold,
//                                color = textColor
//                            )
//                        }
//                    }
                    
//                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Prize Pool Section (if available) - Blue Design
                    if (event.prizePool.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF4285F4),
                                            Color(0xFF5E92F3)
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = "Prize",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Prize Pool",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "₹${event.prizePool}",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    } else {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                    
                    // About Event Section - Enhanced Design
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = surfaceColor,
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Header with gradient background
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF667EEA),
                                                Color(0xFF764BA2)
                                            )
                                        ),
                                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                                    )
                                    .padding(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Decorative icon
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "About",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    
                                    Column {
                                        Text(
                                            text = "About Event",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Event Details & Information",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            
                            // Content area with description
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                // Decorative divider
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF667EEA),
                                                    Color(0xFF764BA2)
                                                )
                                            )
                                        )
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Clickable description with links
                                ClickableDescriptionText(
                                    text = event.description,
                                    fontSize = 15.sp,
                                    color = textColor,
                                    lineHeight = 24.sp,
                                    linkColor = primaryBlue,
                                    context = context
                                )
                            }
                        }
                    }

                    
                    Spacer(modifier = Modifier.height(80.dp)) // Space for button
                }
            }
            
            // Register Now Button (Fixed at bottom)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Check if custom registration is enabled
                    if (event.registrationEnabled) {
                        // Custom registration button
                        val registrationViewModel: RegistrationViewModel = viewModel()
                        
                        LaunchedEffect(event.eventId) {
                            registrationViewModel.checkRegistrationStatus(event.eventId)
                        }
                        
                        if (registrationViewModel.isAlreadyRegistered) {
                            // Already registered - show disabled button
                            Button(
                                onClick = { },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50),
                                    disabledContainerColor = Color(0xFF4CAF50)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                enabled = false
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Registered",
                                        tint = Color.White
                                    )
                                    Text(
                                        text = "Already Registered",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            // Not registered - show register button
                            Button(
                                onClick = {
                                    navController.navigate(
                                        com.divyansh.cueats.EventRegistrationRoute(
                                            eventId = event.eventId,
                                            eventTitle = event.title
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryBlue
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "Register Now",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        // External registration URL button
                        Button(
                            onClick = {
                                if (event.registrationUrl.isNotEmpty()) {
                                    try {
                                        // Normalize URL - add https:// if no protocol is specified
                                        val url = if (!event.registrationUrl.startsWith("http://") && 
                                                    !event.registrationUrl.startsWith("https://")) {
                                            "https://${event.registrationUrl}"
                                        } else {
                                            event.registrationUrl
                                        }
                                        
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            Uri.parse(url)
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Invalid registration URL: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Registration not available for this event",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryBlue
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "Register Now",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    
                    // Registration close date text
                    if (event.registrationCloseDate.isNotEmpty()) {
                        Text(
                            text = "Registration closes on ${event.getFormattedRegistrationCloseDate()}",
                            fontSize = 12.sp,
                            color = textSecondaryColor,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            
            // Club Profile Dialog
            if (showClubProfileDialog && state.event != null) {
                ClubProfileDialog(
                    club = state.club ?: Club(
                        name = state.event!!.organizer,
                        description = "Club information will be loaded from Firestore when clubId is available.",
                        logoUrl = "",
                        contactEmail = "",
                        contactPhone = "",
                        website = ""
                    ),
                    onDismiss = { showClubProfileDialog = false }
                )
            }
        } else if (state.error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "❌",
                        fontSize = 64.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.error ?: "Unknown error",
                        fontSize = 16.sp,
                        color = textSecondaryColor
                    )
                }
            }
        }
    }
}

@Composable
fun InfoCard(
    iconRes: Int,
    label: String,
    value: String,
    backgroundColor: Color,
    iconTint: Color = Color(0xFF4285F4),
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.heightIn(min = 100.dp),
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    modifier = Modifier.size(18.dp),
                    tint = iconTint
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = iconTint,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D2D2D)
            )
        }
    }
}


@Composable
fun ClickableDescriptionText(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    color: Color,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    linkColor: Color,
    context: Context
) {
    // Regex pattern to detect URLs
    val urlPattern = Regex(
        """(https?://[\w\-._~:/?#\[\]@!$&'()*+,;=%]+)"""
    )
    
    val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
        var lastIndex = 0
        
        // Find all URLs in the text
        urlPattern.findAll(text).forEach { matchResult ->
            val url = matchResult.value
            val startIndex = matchResult.range.first
            val endIndex = matchResult.range.last + 1
            
            // Append text before URL
            if (startIndex > lastIndex) {
                append(text.substring(lastIndex, startIndex))
            }
            
            // Add clickable URL
            pushStyle(
                androidx.compose.ui.text.SpanStyle(
                    color = linkColor,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                )
            )
            pushStringAnnotation(
                tag = "URL",
                annotation = url
            )
            append(url)
            pop()
            pop()
            
            lastIndex = endIndex
        }
        
        // Append remaining text
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
    
    androidx.compose.foundation.text.ClickableText(
        text = annotatedString,
        style = androidx.compose.ui.text.TextStyle(
            fontSize = fontSize,
            color = color,
            lineHeight = lineHeight
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations(
                tag = "URL",
                start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                // Open URL in browser
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    Uri.parse(annotation.item)
                )
                context.startActivity(intent)
            }
        }
    )
}

@Composable
fun ClubProfileDialog(
    club: Club,
    onDismiss: () -> Unit
) {
    val primaryBlue = Color(0xFF4285F4)
    val textColor = Color(0xFF2D2D2D)
    val textSecondaryColor = Color(0xFF8A8A8A)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Club Logo
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    primaryBlue.copy(alpha = 0.2f),
                                    primaryBlue.copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (club.logoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = club.logoUrl,
                            contentDescription = "Club logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Show club initials
                        Text(
                            text = club.name.split(" ")
                                .take(2)
                                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                .joinToString("")
                                .ifEmpty { club.name.take(1).uppercase() },
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryBlue
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = club.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Description
                if (club.description.isNotEmpty()) {
                    Column {
                        Text(
                            text = "About",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = club.description,
                            fontSize = 14.sp,
                            color = textColor,
                            lineHeight = 20.sp
                        )
                    }
                }
                
                // Contact Email
                if (club.contactEmail.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = primaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = club.contactEmail,
                            fontSize = 14.sp,
                            color = textColor
                        )
                    }
                }
                
                // Contact Phone
                if (club.contactPhone.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Phone",
                            tint = primaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = club.contactPhone,
                            fontSize = 14.sp,
                            color = textColor
                        )
                    }
                }
                
                // Website
                if (club.website.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Website",
                            tint = primaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = club.website,
                            fontSize = 14.sp,
                            color = primaryBlue
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = primaryBlue
                )
            ) {
                Text(
                    text = "Close",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
