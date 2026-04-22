package com.divyansh.cueats.AnnouncementScreen

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.divyansh.cueats.R
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Swipe direction enum
 */
enum class SwipeDirection {
    LEFT,   // Dismiss
    RIGHT,  // Like/Save
    NONE
}

/**
 * Swipeable Event Card with Tinder-like animations
 */
@Composable
fun SwipeableEventCard(
    event: Event,
    club: Club?,
    onSwipe: (SwipeDirection) -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
    surfaceColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    totalEventsCount: Int = 1, // Total number of events in the queue
    isInterested: Boolean = false, // Whether this event is already marked as interested
    showSwipeIndicators: Boolean = true, // Whether to show swipe direction indicators
    onRemove: (() -> Unit)? = null // Optional callback to remove event (e.g., from interested list)
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val screenWidth = with(density) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }

    // Swipe state - keyed by event ID to reset for each card
    var offsetX by remember(event.eventId) { mutableFloatStateOf(0f) }
    var rotation by remember(event.eventId) { mutableFloatStateOf(0f) }
    var isSwiping by remember(event.eventId) { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Swipe threshold (40% of screen width)
    val swipeThreshold = screenWidth * 0.4f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .graphicsLayer {
                translationX = offsetX
                rotationZ = rotation
            }
            .pointerInput(totalEventsCount) {
                detectDragGestures(
                    onDragEnd = {
                        // Check if there's only one event left
                        if (totalEventsCount == 1) {
                            // Show toast and snap back to center
                            Toast.makeText(
                                context,
                                "This is the only event left",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            // Snap back to center
                            scope.launch {
                                animate(
                                    initialValue = offsetX,
                                    targetValue = 0f,
                                    animationSpec = tween(200, easing = LinearEasing)
                                ) { value, _ ->
                                    offsetX = value
                                    rotation = value / 30f
                                }
                            }
                            return@detectDragGestures
                        }
                        
                        // Calculate swipe direction at the moment of drag end
                        val currentSwipeDirection = when {
                            offsetX > swipeThreshold -> SwipeDirection.RIGHT
                            offsetX < -swipeThreshold -> SwipeDirection.LEFT
                            else -> SwipeDirection.NONE
                        }
                        
                        android.util.Log.d("SwipeCard", "Drag ended: offsetX=$offsetX, threshold=$swipeThreshold, direction=$currentSwipeDirection, isSwiping=$isSwiping")
                        
                        if (currentSwipeDirection != SwipeDirection.NONE && !isSwiping) {
                            isSwiping = true
                            android.util.Log.d("SwipeCard", "Swiping ${event.title} ${currentSwipeDirection}")
                            // Notify parent immediately
                            onSwipe(currentSwipeDirection)

                            // Animate out
                            scope.launch {
                                val targetX = if (offsetX > 0) screenWidth * 1.5f else -screenWidth * 1.5f

                                animate(
                                    initialValue = offsetX,
                                    targetValue = targetX,
                                    animationSpec = tween(250, easing = LinearEasing)
                                ) { value, _ ->
                                    offsetX = value
                                    rotation = (value / 30f).coerceIn(-10f, 10f)
                                }
                            }
                        } else if (!isSwiping) {
                            // Snap back to center
                            scope.launch {
                                animate(
                                    initialValue = offsetX,
                                    targetValue = 0f,
                                    animationSpec = tween(200, easing = LinearEasing)
                                ) { value, _ ->
                                    offsetX = value
                                    rotation = value / 30f
                                }
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (!isSwiping) {
                            change.consume()
                            offsetX += dragAmount.x
                            rotation = (offsetX / 30f).coerceIn(-10f, 10f)
                        }
                    }
                )
            }

    ) {
        // Main Card
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDetailsClick() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Event Image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (event.imageUrl.isNotEmpty()) {
                            val imageUrl = getOptimizedCloudinaryUrl(
                                url = event.imageUrl,
                                width = 800,
                                height = 1000
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
                            // Gradient background
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

                        // Gradient overlay at bottom
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.7f)
                                        ),
                                        startY = 400f
                                    )
                                )
                        )

                        // Category Badge
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp),
                            color = Color(0xE6000000),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.2f)
                            )
                        ) {
                            Text(
                                text = event.category,
                                color = Color(event.getCategoryColor()),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }

                        // Interested Badge (shows if already marked as interested)
                        if (isInterested) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(
                                        top = 16.dp,
                                        end = if (event.prizePool.isNotEmpty()) 16.dp else 16.dp
                                    )
                                    .offset(y = if (event.prizePool.isNotEmpty()) 50.dp else 0.dp),
                                color = Color(0xFFFF6B9D).copy(alpha = 0.95f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Interested",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Interested",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Prize Pool Badge
                        if (event.prizePool.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp),
                                color = Color(0xFFFFC107),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = "🏆", fontSize = 13.sp)
                                    Text(
                                        text = "₹${event.prizePool}",
                                        color = Color(0xFF1A1A1A),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Event Info at Bottom
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            // Date
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.calendarrrr),
                                    contentDescription = "Date",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = event.getFormattedDate(),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Title
                            Text(
                                text = event.title,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 32.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Organizer
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(24.dp),
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        if (club?.logoUrl?.isNotEmpty() == true) {
                                            AsyncImage(
                                                model = club.logoUrl,
                                                contentDescription = "Club logo",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = event.getClubInitials(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = club?.name ?: event.organizer,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Location
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.pin),
                                    contentDescription = "Location",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = event.venue,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            // Remove button (if onRemove callback is provided)
                            onRemove?.let { removeCallback ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = removeCallback,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF44336).copy(alpha = 0.9f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Remove from Interested",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Swipe Direction Indicators - only show if enabled
                if (showSwipeIndicators) {
                    // Right swipe - Interested (Green) - Centered with pulsing animation
                    if (offsetX > 50f) {
                        val alpha = (offsetX / swipeThreshold).coerceIn(0f, 1f)
                        
                        // Pulsing animation for the icon
                        val infiniteTransition = rememberInfiniteTransition(label = "interested_pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(400, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse_scale"
                        )
                        
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF4CAF50).copy(alpha = alpha * 0.9f),
                                    modifier = Modifier
                                        .size(100.dp)
                                        .scale(pulseScale)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = "Interested",
                                            tint = Color.White,
                                            modifier = Modifier.size(50.dp)
                                        )
                                    }
                                }
                                
                                // Instructional text
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFF4CAF50).copy(alpha = alpha * 0.95f)
                                ) {
                                    Text(
                                        text = "Interested!",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Left swipe - Not Interested (Red) - Centered with pulsing animation
                    if (offsetX < -50f) {
                        val alpha = (abs(offsetX) / swipeThreshold).coerceIn(0f, 1f)
                        
                        // Pulsing animation for the icon
                        val infiniteTransition = rememberInfiniteTransition(label = "not_interested_pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(400, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse_scale"
                        )
                        
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF44336).copy(alpha = alpha * 0.9f),
                                    modifier = Modifier
                                        .size(100.dp)
                                        .scale(pulseScale)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Not Interested",
                                            tint = Color.White,
                                            modifier = Modifier.size(50.dp)
                                        )
                                    }
                                }
                                
                                // Instructional text
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFF44336).copy(alpha = alpha * 0.95f)
                                ) {
                                    Text(
                                        text = "Not Interested",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Swipe hint text - subtle overlay
        var showHint by remember { mutableStateOf(true) }
        
        // Hide hint after first interaction or after 2 seconds
        LaunchedEffect(isSwiping) {
            if (isSwiping) {
                showHint = false
            }
        }
        
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2600) // 2 seconds
            showHint = false
        }
        
        
        // Subtle text hint at center - only show if there are multiple events
        // Positioned at center with animation to be noticeable
        if (showHint && totalEventsCount > 1) {
            // Pulsing animation for visibility
            val infiniteTransition = rememberInfiniteTransition(label = "hint_pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .scale(scale)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.2f * alpha),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "← Swipe to explore events →",
                            color = Color.White.copy(alpha = 0.9f * alpha),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card Stack - Manages multiple swipeable cards with infinite looping
 */
@Composable
fun CardStack(
    events: List<Event>,
    clubsMap: Map<String, Club>,
    onEventSwiped: (Event, SwipeDirection) -> Unit,
    onEventDetails: (Event) -> Unit,
    onAllCardsSwiped: () -> Unit,
    modifier: Modifier = Modifier,
    surfaceColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    primaryOrange: Color,
    interestedEventIds: Set<String> = emptySet(), // IDs of events marked as interested
    showSwipeIndicators: Boolean = true, // Whether to show swipe direction indicators
    onRemove: ((Event) -> Unit)? = null // Optional callback to remove event
) {
    // Use a circular queue - maintain order of events
    var eventQueue by remember { mutableStateOf(events) }
    var pendingSwipeId by remember { mutableStateOf<String?>(null) }
    
    // Update queue when events list changes
    LaunchedEffect(events) {
        if (eventQueue.isEmpty() || eventQueue.map { it.eventId }.toSet() != events.map { it.eventId }.toSet()) {
            eventQueue = events
        }
    }
    
    
    // Rotate queue immediately when swipe starts
    // This ensures the next card is ready to show when current card animates away
    LaunchedEffect(pendingSwipeId) {
        pendingSwipeId?.let { id ->
            // Only rotate if there are multiple events
            // For single event, rotation is unnecessary and causes empty state
            if (eventQueue.size > 1) {
                val swipedIndex = eventQueue.indexOfFirst { it.eventId == id }
                if (swipedIndex != -1) {
                    // Rotate queue: move swiped event to back
                    eventQueue = eventQueue.drop(swipedIndex + 1) + 
                                 eventQueue.take(swipedIndex + 1)
                    android.util.Log.d("CardStack", "Rotated queue immediately. Queue size: ${eventQueue.size}")
                }
            } else {
                android.util.Log.d("CardStack", "Single event - skipping rotation")
            }
            
            // Wait for animation to complete before allowing next swipe
            kotlinx.coroutines.delay(300)
            pendingSwipeId = null
        }
    }

    // Show empty state if no events AND no pending swipe
    // This prevents the blank page from showing during the swipe animation
    if (eventQueue.isEmpty() && pendingSwipeId == null) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "🎉", fontSize = 64.sp)
                Text(
                    text = "No events available",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "Check back later for updates",
                    fontSize = 14.sp,
                    color = textSecondaryColor
                )
            }
        }
        
        LaunchedEffect(Unit) {
            onAllCardsSwiped()
        }
        return
    }


    // Card Stack
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        val cardsToShow = eventQueue.take(3)
        
        // Render cards in reverse order so the first card (index 0) is rendered last and appears on top
        cardsToShow.asReversed().forEachIndexed { reversedIndex, event ->
            val index = cardsToShow.size - 1 - reversedIndex // Convert back to original index
            // Increased scale difference to make stacked cards more visible
            val scale = 1f - (index * 0.08f) // Changed from 0.05f to 0.08f
            // Increased offset to show more of the cards underneath
            val offsetY = index * 16.dp // Changed from 12.dp to 16.dp
            val club = clubsMap[event.clubId]

            key(event.eventId) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = offsetY)
                        .scale(scale)
                        .zIndex((cardsToShow.size - index).toFloat())
                ) {
                    if (index == 0) {
                        SwipeableEventCard(
                            event = event,
                            club = club,
                            onSwipe = { direction ->
                                pendingSwipeId = event.eventId
                                onEventSwiped(event, direction)
                            },
                            onDetailsClick = { onEventDetails(event) },
                            surfaceColor = surfaceColor,
                            textColor = textColor,
                            textSecondaryColor = textSecondaryColor,
                            totalEventsCount = eventQueue.size,
                            isInterested = event.eventId in interestedEventIds,
                            showSwipeIndicators = showSwipeIndicators,
                            onRemove = onRemove?.let { { it(event) } }
                        )
                    } else {
                        BackgroundEventCard(
                            event = event,
                            club = club,
                            surfaceColor = surfaceColor,
                            textColor = textColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Background card (non-interactive)
 */
@Composable
fun BackgroundEventCard(
    event: Event,
    club: Club?,
    surfaceColor: Color,
    textColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (event.imageUrl.isNotEmpty()) {
                val imageUrl = getOptimizedCloudinaryUrl(
                    url = event.imageUrl,
                    width = 800,
                    height = 1000
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

            // Slight overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }
    }
}
