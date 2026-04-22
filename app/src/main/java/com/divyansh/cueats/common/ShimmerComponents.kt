package com.divyansh.cueats.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Reusable shimmer loading components for all screens
 * Provides consistent loading experience across the app
 */

@Composable
fun ShimmerBrush(): Brush {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    
    val shimmerTranslate by infiniteTransition.animateFloat(
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
    
    val shimmerOpacity by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_opacity"
    )
    
    return Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = shimmerOpacity),
            Color.LightGray.copy(alpha = 0.6f)
        ),
        start = Offset(shimmerTranslate - 200f, 0f),
        end = Offset(shimmerTranslate, 300f)
    )
}

// Shimmer for card-based layouts (Home, Events, Shops)
@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    height: Int = 200
) {
    val shimmerBrush = ShimmerBrush()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(shimmerBrush)
    )
}

// Shimmer for list items
@Composable
fun ShimmerListItem(
    modifier: Modifier = Modifier
) {
    val shimmerBrush = ShimmerBrush()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Image placeholder
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(shimmerBrush)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Title placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subtitle placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

// Shimmer for circular items (categories, profile pics)
@Composable
fun ShimmerCircle(
    modifier: Modifier = Modifier,
    size: Int = 60
) {
    val shimmerBrush = ShimmerBrush()
    
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(shimmerBrush)
    )
}

// Shimmer for text lines
@Composable
fun ShimmerText(
    modifier: Modifier = Modifier,
    width: Float = 1f,
    height: Int = 16
) {
    val shimmerBrush = ShimmerBrush()
    
    Box(
        modifier = modifier
            .fillMaxWidth(width)
            .height(height.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(shimmerBrush)
    )
}

// Shimmer for meal/menu cards (Mess screen)
@Composable
fun ShimmerMealCard() {
    val shimmerBrush = ShimmerBrush()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(shimmerBrush)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Content
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            ShimmerText(width = 0.6f, height = 20)
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerText(width = 0.8f, height = 16)
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerText(width = 0.4f, height = 16)
        }
    }
}

// Shimmer for event cards
@Composable
fun ShimmerEventCard() {
    val shimmerBrush = ShimmerBrush()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        // Event image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(shimmerBrush)
        )
        
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title
            ShimmerText(width = 0.8f, height = 24)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerText(modifier = Modifier.weight(1f), width = 0.5f, height = 16)
                Spacer(modifier = Modifier.width(8.dp))
                ShimmerText(modifier = Modifier.weight(1f), width = 0.5f, height = 16)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Description
            ShimmerText(width = 1f, height = 14)
            Spacer(modifier = Modifier.height(4.dp))
            ShimmerText(width = 0.9f, height = 14)
        }
    }
}

// Shimmer for shop cards
@Composable
fun ShimmerShopCard() {
    val shimmerBrush = ShimmerBrush()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shop image
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(shimmerBrush)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            ShimmerText(width = 0.7f, height = 20)
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerText(width = 0.5f, height = 16)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row {
                ShimmerCircle(size = 20)
                Spacer(modifier = Modifier.width(4.dp))
                ShimmerText(width = 0.3f, height = 16)
            }
        }
    }
}

// Full screen shimmer loading
@Composable
fun ShimmerLoadingScreen(
    itemCount: Int = 5,
    itemType: ShimmerItemType = ShimmerItemType.CARD
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        repeat(itemCount) {
            when (itemType) {
                ShimmerItemType.CARD -> ShimmerCard()
                ShimmerItemType.LIST_ITEM -> ShimmerListItem()
                ShimmerItemType.EVENT_CARD -> ShimmerEventCard()
                ShimmerItemType.SHOP_CARD -> ShimmerShopCard()
                ShimmerItemType.MEAL_CARD -> ShimmerMealCard()
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

enum class ShimmerItemType {
    CARD,
    LIST_ITEM,
    EVENT_CARD,
    SHOP_CARD,
    MEAL_CARD
}

// Shimmer for event details screen
@Composable
fun ShimmerEventDetails() {
    val shimmerBrush = ShimmerBrush()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F7FB))
    ) {
        // Banner shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(shimmerBrush)
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Title shimmer
            ShimmerText(width = 0.9f, height = 28)
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Date and Time cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(shimmerBrush)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(shimmerBrush)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Organizer section shimmer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerCircle(size = 48)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerText(width = 0.4f, height = 12)
                    Spacer(modifier = Modifier.height(4.dp))
                    ShimmerText(width = 0.6f, height = 16)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Location section
            ShimmerText(width = 0.3f, height = 18)
            Spacer(modifier = Modifier.height(12.dp))
            ShimmerText(width = 0.7f, height = 14)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Map placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmerBrush)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // About section
            ShimmerText(width = 0.4f, height = 18)
            Spacer(modifier = Modifier.height(12.dp))
            ShimmerText(width = 1f, height = 14)
            Spacer(modifier = Modifier.height(4.dp))
            ShimmerText(width = 0.95f, height = 14)
            Spacer(modifier = Modifier.height(4.dp))
            ShimmerText(width = 0.8f, height = 14)
        }
    }
}
