package com.divyansh.cueats.AnnouncementScreen

import android.health.connect.datatypes.ExerciseCompletionGoal
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.divyansh.cueats.EventDetailsRoute
import com.divyansh.cueats.R

/**
 * View mode for events screen
 */
enum class ViewMode {
    CARDS,  // Swipeable card stack
    LIST    // Traditional scrollable list
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun EventsScreen(
    navController: NavController,
    viewModel: EventViewModel = viewModel()
) {
    val state by viewModel.eventsState.collectAsState()
    
    // View mode state
    var viewMode by remember { mutableStateOf(ViewMode.CARDS) }
    
    // Colors
    val backgroundColor = Color(0xFFF6F7FB)
    val surfaceColor = Color.White
    val primaryOrange = Color(0xFFFD7F2B)
    val textColor = Color(0xFF2D2D2D)
    val textSecondaryColor = Color(0xFF8A8A8A)
    
    Scaffold(
        floatingActionButton = {
            // FAB for Completed and Interested Events
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 100.dp)
            ) {
                // Interested Events FAB with animation
                val interestedExpanded = state.selectedTab == EventTab.INTERESTED
                val interestedSize by animateDpAsState(
                    targetValue = if (interestedExpanded) 140.dp else 56.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "interested_size"
                )
                
                FloatingActionButton(
                    onClick = { 
                        viewModel.selectTab(
                            if (state.selectedTab == EventTab.INTERESTED) {
                                EventTab.ACTIVE
                            } else {
                                EventTab.INTERESTED
                            }
                        )
                    },
                    containerColor = if (interestedExpanded) {
                        Color(0xFFFF6B9D) // Pink when active
                    } else {
                        Color(0xFF9C27B0) // Purple when inactive
                    },
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .width(interestedSize)
                ) {
                    AnimatedContent(
                        targetState = interestedExpanded,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) with
                                    fadeOut(animationSpec = tween(300))
                        },
                        label = "interested_content"
                    ) { expanded ->
                        if (expanded) {
                            // Expanded with text when active
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Interested Events",
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Interested",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            // Icon only when inactive
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Interested Events",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                
                // Completed Events FAB with animation
                val completedExpanded = state.selectedTab == EventTab.COMPLETED
                val completedSize by animateDpAsState(
                    targetValue = if (completedExpanded) 150.dp else 56.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "completed_size"
                )
                
                FloatingActionButton(
                    onClick = { 
                        viewModel.selectTab(
                            if (state.selectedTab == EventTab.COMPLETED) {
                                EventTab.ACTIVE
                            } else {
                                EventTab.COMPLETED
                            }
                        )
                    },
                    containerColor = if (completedExpanded) {
                        Color(0xFF4CAF50) // Green when active
                    } else {
                        Color(0xFF2196F3) // Blue when inactive
                    },
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .width(completedSize)
                ) {
                    AnimatedContent(
                        targetState = completedExpanded,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) with
                                    fadeOut(animationSpec = tween(300))
                        },
                        label = "completed_content"
                    ) { expanded ->
                        if (expanded) {
                            // Expanded with text when active
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.checked),
                                    contentDescription = "Completed Events",
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Completed",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            // Icon only when inactive
                            Icon(
                                painter = painterResource(id = R.drawable.checked),
                                contentDescription = "Completed Events",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        // Box to overlay navigation on content
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {            // Search Bar and Date Filter Button
                var showDatePicker by remember { mutableStateOf(false) }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Search Bar
                    SearchBar(
                        query = state.searchQuery,
                        onQueryChange = { viewModel.searchEvents(it) },
                        modifier = Modifier.weight(1f),
                        textColor = textColor,
                        textSecondaryColor = textSecondaryColor
                    )
                    
                    // Calendar Filter Button
                    IconButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                brush = if (state.selectedDate != null) {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF667EEA),
                                            Color(0xFF764BA2)
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(surfaceColor, surfaceColor)
                                    )
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.calendarrrr),
                            contentDescription = "Filter by date",
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Category Filters
                CategoryFilters(
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = { viewModel.filterByCategory(it) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                    primaryOrange = primaryOrange,
                    textColor = textColor
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Events Count Header with View Mode Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = when (state.selectedTab) {
                                EventTab.ACTIVE -> "Upcoming Events"
                                EventTab.COMPLETED -> "Completed Events"
                                EventTab.INTERESTED -> "Interested Events"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "${state.filteredEvents.size} found",
                            fontSize = 13.sp,
                            color = textSecondaryColor
                        )
                    }
                    
                    // View Mode Toggle
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE3F2FD), // Light blue background
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp)
                        ) {
                            // Cards View Button
                            Surface(
                                onClick = { viewMode = ViewMode.CARDS },
                                modifier = Modifier.size(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = if (viewMode == ViewMode.CARDS) {
                                    Color(0xFF2196F3) // Filled blue when selected
                                } else {
                                    Color.Transparent
                                },
                                shadowElevation = if (viewMode == ViewMode.CARDS) 4.dp else 0.dp
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.events),
                                        contentDescription = "Card View",
                                        tint = if (viewMode == ViewMode.CARDS) {
                                            Color.White // White icon when selected
                                        } else {
                                            Color(0xFF757575) // Gray when not selected
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            // List View Button
                            Surface(
                                onClick = { viewMode = ViewMode.LIST },
                                modifier = Modifier.size(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = if (viewMode == ViewMode.LIST) {
                                    Color(0xFF2196F3) // Filled blue when selected
                                } else {
                                    Color.Transparent
                                },
                                shadowElevation = if (viewMode == ViewMode.LIST) 4.dp else 0.dp
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.tool),
                                        contentDescription = "List View",
                                        tint = if (viewMode == ViewMode.LIST) {
                                            Color.White // White icon when selected
                                        } else {
                                            Color(0xFF757575) // Gray when not selected
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Content based on view mode
                if (state.isLoading) {
                    // Shimmer loading for events
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(3) {
                            com.divyansh.cueats.common.ShimmerEventCard()
                        }
                    }
                } else if (state.filteredEvents.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = when (state.selectedTab) {
                                    EventTab.ACTIVE -> "🎉"
                                    EventTab.COMPLETED -> "✅"
                                    EventTab.INTERESTED -> "💡"
                                },
                                fontSize = 64.sp
                            )
                            Text(
                                text = when (state.selectedTab) {
                                    EventTab.ACTIVE -> "No active events"
                                    EventTab.COMPLETED -> "No completed events"
                                    EventTab.INTERESTED -> "No interested events yet"
                                },
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = when (state.selectedTab) {
                                    EventTab.ACTIVE -> "Check back later for updates"
                                    EventTab.COMPLETED -> "Completed events will appear here"
                                    EventTab.INTERESTED -> "Swipe right on events to save them here"
                                },
                                fontSize = 14.sp,
                                color = textSecondaryColor
                            )
                        }
                    }
                } else {
                    // Show card stack or list based on view mode
                    when (viewMode) {
                        ViewMode.CARDS -> {
                            // Swipeable Card Stack - Add bottom padding for floating nav
                            Box(modifier = Modifier.fillMaxSize()) {
                                CardStack(
                                    events = state.filteredEvents,
                                    clubsMap = state.clubsMap,
                                    onEventSwiped = { event, direction ->
                                        // Handle swipe actions only in Active tab
                                        if (state.selectedTab == EventTab.ACTIVE) {
                                            when (direction) {
                                                SwipeDirection.RIGHT -> {
                                                    // Mark as interested
                                                    viewModel.markEventAsInterested(event.eventId)
                                                }
                                                SwipeDirection.LEFT -> {
                                                    // Mark as not interested
                                                    viewModel.markEventAsNotInterested(event.eventId)
                                                }
                                                SwipeDirection.NONE -> {}
                                            }
                                        }
                                    },
                                    onEventDetails = { event ->
                                        navController.navigate(EventDetailsRoute(event.eventId))
                                    },
                                    onAllCardsSwiped = {
                                        // Show empty state or reset
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = 100.dp), // Extra padding for floating nav
                                    surfaceColor = surfaceColor,
                                    textColor = textColor,
                                    textSecondaryColor = textSecondaryColor,
                                    primaryOrange = primaryOrange,
                                    interestedEventIds = state.interestedEventIds,
                                    showSwipeIndicators = state.selectedTab == EventTab.ACTIVE,
                                    onRemove = if (state.selectedTab == EventTab.INTERESTED) {
                                        { event -> viewModel.removeFromInterested(event.eventId) }
                                    } else null
                                )
                            }
                        }
                        
                        ViewMode.LIST -> {
                            // Traditional List View
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 100.dp), // Extra padding for floating nav
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(state.filteredEvents) { event ->
                                    val club = state.clubsMap[event.clubId]
                                    EventCard(
                                        event = event,
                                        club = club,
                                        onClick = { 
                                            navController.navigate(EventDetailsRoute(event.eventId))
                                        },
                                        surfaceColor = surfaceColor,
                                        textColor = textColor,
                                        textSecondaryColor = textSecondaryColor,
                                        primaryOrange = primaryOrange
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Date Picker Dialog
                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState()
                    
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    datePickerState.selectedDateMillis?.let { millis ->
                                        val calendar = java.util.Calendar.getInstance()
                                        calendar.timeInMillis = millis
                                        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                                        val month = calendar.get(java.util.Calendar.MONTH) + 1
                                        val year = calendar.get(java.util.Calendar.YEAR)
                                        val formattedDate = String.format("%02d/%02d/%d", day, month, year)
                                        viewModel.filterByDate(formattedDate)
                                    }
                                    showDatePicker = false
                                }
                            ) {
                                Text("OK", color = primaryOrange)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    viewModel.clearDateFilter()
                                    showDatePicker = false
                                }
                            ) {
                                Text("Clear", color = textSecondaryColor)
                            }
                        }
                    ) {
                        DatePicker(
                            state = datePickerState,
                            colors = DatePickerDefaults.colors(
                                selectedDayContainerColor = primaryOrange,
                                todayContentColor = primaryOrange,
                                todayDateBorderColor = primaryOrange
                            )
                        )
                    }
                }
            }
            
            // Floating navigation overlay (iOS style) - Must be last in Box to be on top
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                com.divyansh.cueats.AppBottomNavigation(
                    navController = navController,
                    currentRoute = "events"
                )
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color,
    textSecondaryColor: Color
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 4.dp else 0.dp
        )
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .onFocusChanged { isFocused = it.isFocused },
            placeholder = { 
                Text(
                    "Search events, hackathons...",
                    color = Color(0xFFAAAAAA),
                    fontSize = 15.sp
                ) 
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(24.dp)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            shape = RoundedCornerShape(28.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp)
        )
    }
}

@Composable
fun CategoryFilters(
    selectedCategory: EventCategory,
    onCategorySelected: (EventCategory) -> Unit,
    modifier: Modifier = Modifier,
    primaryOrange: Color,
    textColor: Color
) {
    // Filter out COMPLETED category from the horizontal chips
    val visibleCategories = EventCategory.values().filter { it != EventCategory.COMPLETED }
    
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(visibleCategories) { category ->
            CategoryChip(
                category = category,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                primaryOrange = primaryOrange,
                textColor = textColor
            )
        }
    }
}

@Composable
fun CategoryChip(
    category: EventCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryOrange: Color,
    textColor: Color
) {
    val backgroundColor = if (isSelected) {
        Color(0xFF2196F3) // Solid blue
    } else {
        Color.White
    }
    val contentColor = if (isSelected) Color.White else Color(0xFF9E9E9E)
    
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(15.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(15.dp),
        shadowElevation = if (isSelected) 8.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = category.iconRes),
                contentDescription = category.displayName,
                tint = if (isSelected) Color.Unspecified else Color(0xFF9E9E9E),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = category.displayName,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun EventCard(
    event: Event,
    club: Club?,
    onClick: () -> Unit,
    surfaceColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    primaryOrange: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Banner Image with Overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                // Event Banner Image with Cloudinary optimization
                if (event.imageUrl.isNotEmpty()) {
                    val imageUrl = getOptimizedCloudinaryUrl(
                        url = event.imageUrl,
                        width = 800,
                        height = 450
                    )
                    
                    AsyncImage(
                        model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = event.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Gradient background if no image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF667EEA),
                                        Color(0xFF764BA2)
                                    )
                                )
                            )
                    )
                }
                
                // Subtle gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.2f)
                                ),
                                startY = 200f
                            )
                        )
                )
                
                // Category Badge (Top Left) - Category-specific colors
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    color = Color(0xE6000000), // 90% opacity black
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.2f)
                    )
                ) {
                    Text(
                        text = event.category,
                        color = Color(event.getCategoryColor()), // Dynamic color based on category
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                
                // Prize Money Badge (Top Right)
                if (event.prizePool.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        color = Color(0xFFFFC107),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "🏆",
                                fontSize = 12.sp
                            )
                            Text(
                                text = "₹${event.prizePool} Prize Pool",
                                color = Color(0xFF1A1A1A),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Date Badge (Bottom Left) - Enhanced Contrast
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    color = Color(0xE6000000), // 90% opacity black
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.calendarrrr),
                            contentDescription = "Date",
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = event.getFormattedDate(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                }
            }
            
            // Content Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Organizer with club logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(26.dp),
                        shape = CircleShape,
                        color = Color.White
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF2196F3).copy(alpha = 0.15f))
                        ) {
                            // Show club logo if available, otherwise show club initials
                            if (club?.logoUrl?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = club.logoUrl,
                                    contentDescription = "Club logo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = (club?.name ?: event.organizer).split(" ")
                                        .take(2)
                                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                        .joinToString("")
                                        .ifEmpty { (club?.name ?: event.organizer).take(1).uppercase() },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = club?.name ?: event.organizer,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondaryColor
                    )
                }
                
                // Event Title
                Text(
                    text = event.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 30.sp
                )
                
                Spacer(modifier = Modifier.height(7.dp))
                
                // Description
                Text(
                    text = event.description,
                    fontSize = 14.sp,
                    color = textSecondaryColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFECEAEA)),

                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Location and Details Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Location
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.pin),
                            contentDescription = "Location",
                            tint = textSecondaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = event.venue,
                            fontSize = 14.sp,
                            color = textSecondaryColor,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Details Button
                    TextButton(
                        onClick = onClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Details",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }





@Composable
fun DateBadge(
    month: String,
    day: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(50.dp)
            .height(60.dp),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = month,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFD7F2B)
            )
            Text(
                text = day,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D2D2D)
            )
        }
    }
}

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    textColor: Color
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.noevent),
                contentDescription = "No events",
                tint = Color(0xFFBDBDBD),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No events found",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Try adjusting your filters",
                fontSize = 14.sp,
                color = textColor.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Generate optimized Cloudinary URL with transformations
 * 
 * Transformations applied:
 * - w_X,h_Y: Resize to specified dimensions
 * - c_fill: Crop to fill dimensions while maintaining aspect ratio
 * - q_auto: Automatic quality optimization
 * - f_auto: Automatic format selection (WebP for supported devices)
 * 
 * @param url Original image URL (Cloudinary or any URL)
 * @param width Target width in pixels
 * @param height Target height in pixels
 * @return Optimized Cloudinary URL or original URL if not a Cloudinary image
 */
fun getOptimizedCloudinaryUrl(
    url: String,
    width: Int = 800,
    height: Int = 450
): String {
    // Check if it's a Cloudinary URL
    if (!url.contains("cloudinary.com") || !url.contains("/upload/")) {
        return url // Return original URL if not Cloudinary
    }
    
    // Split URL at /upload/ to insert transformations
    val parts = url.split("/upload/")
    if (parts.size != 2) return url
    
    // Build transformation string
    val transformations = "w_$width,h_$height,c_fill,q_auto,f_auto"
    
    // Reconstruct URL with transformations
    return "${parts[0]}/upload/$transformations/${parts[1]}"
}
