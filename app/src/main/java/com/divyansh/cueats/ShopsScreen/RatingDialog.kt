package com.divyansh.cueats.ShopsScreen

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RatingDialog(
    shopName: String,
    shopId: String,
    userId: String = "user_${System.currentTimeMillis()}",
    onDismiss: () -> Unit,
    onRatingSubmitted: () -> Unit
) {
    val ratingViewModel: RatingViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme()

    var selectedRating by remember { mutableStateOf(0f) }
    var userName by remember { mutableStateOf("") }
    var reviewText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showReviews by remember { mutableStateOf(false) }

    val ratings by ratingViewModel.ratings.collectAsState()
    val shopRating = ratingViewModel.getShopRating(shopId)

    // Load ratings when dialog opens
    LaunchedEffect(shopId) {
        Log.d("RatingDialog", "Loading data for shop: $shopId")
        ratingViewModel.loadRatingsForShop(shopId)
    }

    // Colors
    val primaryOrange = Color(0xFFFF6B01)
    val starYellow = Color(0xFFFFC107)
    val dialogBackgroundColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
    val textPrimaryColor = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF212121)
    val textSecondaryColor = if (isDarkTheme) Color(0xFFB0B0B0) else Color(0xFF757575)
    val surfaceColor = if (isDarkTheme) Color(0xFF2A2A2A) else Color(0xFFFAFAFA)
    val borderColor = if (isDarkTheme) Color(0xFF3A3A3A) else Color(0xFFE0E0E0)

    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBackgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = primaryOrange.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (showReviews) "Reviews & Ratings" else "Rate Restaurant",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimaryColor
                            )
                            Text(
                                text = shopName,
                                fontSize = 14.sp,
                                color = primaryOrange,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    showReviews = !showReviews
                                    if (showReviews) {
                                        ratingViewModel.loadRatingsForShop(shopId)
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = primaryOrange
                                )
                            ) {
                                Text(
                                    text = if (showReviews) "Rate" else "Reviews",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                enabled = !isSubmitting
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = textSecondaryColor
                                )
                            }
                        }
                    }
                }

                if (showReviews) {
                    // Reviews List
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Rating Summary
                        shopRating?.let { rating ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = surfaceColor
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = String.format("%.1f", rating.averageRating),
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryOrange
                                        )
                                        RatingStars(
                                            rating = rating.averageRating,
                                            starSize = 16.dp,
                                            starColor = starYellow
                                        )
                                        Text(
                                            text = "${rating.totalRatings} reviews",
                                            fontSize = 12.sp,
                                            color = textSecondaryColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(24.dp))

                                    // Rating breakdown
                                    Column(modifier = Modifier.weight(1f)) {
                                        (5 downTo 1).forEach { stars ->
                                            val count = rating.ratingBreakdown[stars] ?: 0
                                            val percentage = if (rating.totalRatings > 0) {
                                                (count.toFloat() / rating.totalRatings.toFloat())
                                            } else 0f

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "$stars",
                                                    fontSize = 12.sp,
                                                    color = textSecondaryColor,
                                                    modifier = Modifier.width(20.dp)
                                                )

                                                LinearProgressIndicator(
                                                    progress = percentage,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(3.dp)),
                                                    color = starYellow,
                                                    trackColor = borderColor
                                                )

                                                Text(
                                                    text = count.toString(),
                                                    fontSize = 12.sp,
                                                    color = textSecondaryColor,
                                                    modifier = Modifier.width(30.dp),
                                                    textAlign = TextAlign.End
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Individual Reviews
                        if (ratings.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = textSecondaryColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "No reviews yet",
                                        color = textSecondaryColor,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "Be the first to leave a review!",
                                        color = textSecondaryColor.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(ratings) { rating ->
                                    ReviewCard(
                                        rating = rating,
                                        textPrimaryColor = textPrimaryColor,
                                        textSecondaryColor = textSecondaryColor,
                                        starYellow = starYellow,
                                        surfaceColor = surfaceColor
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Rating Form
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Current shop rating display
                        shopRating?.let { rating ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Text(
                                    text = "Current Rating: ",
                                    color = textSecondaryColor,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = String.format("%.1f", rating.averageRating),
                                    color = primaryOrange,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                RatingStars(
                                    rating = rating.averageRating,
                                    starSize = 16.dp,
                                    starColor = starYellow
                                )
                                Text(
                                    text = " (${rating.totalRatings})",
                                    color = textSecondaryColor,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Star Rating Input
                        Text(
                            text = "How was your experience?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimaryColor,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        InteractiveRatingStars(
                            selectedRating = selectedRating,
                            onRatingChanged = { selectedRating = it },
                            starColor = starYellow
                        )

                        if (selectedRating > 0) {
                            Text(
                                text = when (selectedRating.toInt()) {
                                    1 -> "Poor"
                                    2 -> "Fair"
                                    3 -> "Good"
                                    4 -> "Very Good"
                                    5 -> "Excellent"
                                    else -> ""
                                },
                                color = primaryOrange,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Name Input
                        OutlinedTextField(
                            value = userName,
                            onValueChange = { userName = it },
                            label = { Text("Your Name", color = textSecondaryColor) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryOrange,
                                unfocusedBorderColor = borderColor,
                                focusedTextColor = textPrimaryColor,
                                unfocusedTextColor = textPrimaryColor,
                                cursorColor = primaryOrange
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (userName.isNotEmpty()) primaryOrange else textSecondaryColor
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next,
                                capitalization = KeyboardCapitalization.Words
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Review Input
                        OutlinedTextField(
                            value = reviewText,
                            onValueChange = { reviewText = it },
                            label = { Text("Write a review (optional)", color = textSecondaryColor) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryOrange,
                                unfocusedBorderColor = borderColor,
                                focusedTextColor = textPrimaryColor,
                                unfocusedTextColor = textPrimaryColor,
                                cursorColor = primaryOrange
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done,
                                capitalization = KeyboardCapitalization.Sentences
                            )
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Submit Button - FIXED to properly handle submission
                        Button(
                            onClick = {
                                if (selectedRating > 0 && userName.isNotBlank()) {
                                    isSubmitting = true
                                    Log.d("RatingDialog", "Submitting rating: $selectedRating stars for shop $shopId")

                                    scope.launch {
                                        try {
                                            val success = ratingViewModel.submitRating(
                                                shopId = shopId,
                                                userId = userId,
                                                userName = userName.trim(),
                                                rating = selectedRating,
                                                review = reviewText.trim()
                                            )

                                            isSubmitting = false

                                            if (success) {
                                                Log.d("RatingDialog", "Rating submitted successfully")
                                                onRatingSubmitted()
                                                onDismiss()
                                            } else {
                                                Log.e("RatingDialog", "Failed to submit rating")
                                                // You could show an error message here
                                            }
                                        } catch (e: Exception) {
                                            Log.e("RatingDialog", "Exception during rating submission", e)
                                            isSubmitting = false
                                        }
                                    }
                                }
                            },
                            enabled = selectedRating > 0 && userName.isNotBlank() && !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryOrange,
                                disabledContainerColor = textSecondaryColor.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Submit Rating",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun InteractiveRatingStars(
    selectedRating: Float,
    onRatingChanged: (Float) -> Unit,
    starColor: Color,
    starSize: androidx.compose.ui.unit.Dp = 40.dp
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..5).forEach { index ->
            val isSelected = index <= selectedRating
            val animatedScale by animateFloatAsState(
                targetValue = if (isSelected) 1.2f else 1.0f,
                animationSpec = tween(200),
                label = "star_scale"
            )

            Icon(
                imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Star $index",
                tint = if (isSelected) starColor else Color.Gray.copy(alpha = 0.3f),
                modifier = Modifier
                    .size(starSize)
                    .scale(animatedScale)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onRatingChanged(index.toFloat())
                    }
            )
        }
    }
}

@Composable
fun RatingStars(
    rating: Float,
    starSize: androidx.compose.ui.unit.Dp = 16.dp,
    starColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..5).forEach { index ->
            val isSelected = index <= rating
            Icon(
                imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = if (isSelected) starColor else Color.Gray.copy(alpha = 0.3f),
                modifier = Modifier.size(starSize)
            )
        }
    }
}

@Composable
fun ReviewCard(
    rating: Rating,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    starYellow: Color,
    surfaceColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rating.userName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimaryColor
                    )

                    RatingStars(
                        rating = rating.rating,
                        starSize = 14.dp,
                        starColor = starYellow
                    )
                }

                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(rating.timestamp.toDate()),
                    fontSize = 12.sp,
                    color = textSecondaryColor
                )
            }

            if (rating.review.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = rating.review,
                    fontSize = 14.sp,
                    color = textSecondaryColor,
                    lineHeight = 18.sp
                )
            }
        }
    }
}