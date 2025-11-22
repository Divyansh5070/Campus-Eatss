
@file:OptIn(ExperimentalMaterial3Api::class)


package com.divyansh.cueats.Notification


import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
    val systemTheme = isSystemInDarkTheme()
    val systemUiController = rememberSystemUiController()

    // Dialog states
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var feedbackType by remember { mutableStateOf("General") }
    var isSubmitting by remember { mutableStateOf(false) }

    // Navigation guard to prevent multiple rapid clicks
    var navigationInProgress by remember { mutableStateOf(false) }

    // Theme colors
    val primaryOrange = Color(0xFFFF6B01)
    val lightBackground = Color(0xFFF6F7FB)
    val darkBackground = Color(0xFF121212)
    val surfaceColor = if (systemTheme) Color(0xFF202020) else Color.White
    val textColor = if (systemTheme) Color.White else Color.Black
    val textSecondaryColor = if (systemTheme) Color.LightGray else Color.Gray
    val cardBackground = if (systemTheme) Color(0xFF2A2A2A) else Color(0xFFF8F9FA)

    // Feedback data class
    data class FeedbackData(
        val name: String = "",
        val email: String = "",
        val message: String = "",
        val type: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val appVersion: String = "1.0.0",
        val deviceInfo: String = "",
        val status: String = "new" // new, in-progress, resolved
    )

    // Safe navigation function
    fun navigateBack() {
        if (!navigationInProgress) {
            navigationInProgress = true
            try {
                if (navController.previousBackStackEntry != null) {
                    navController.popBackStack()
                }
            } catch (e: Exception) {
                Log.e("AboutScreen", "Navigation error", e)
            }
        }
    }

    // Function to handle feedback submission via Firestore
    fun submitFeedback(name: String, email: String, message: String, type: String) {
        isSubmitting = true
        val db = FirebaseFirestore.getInstance()

        val feedback = FeedbackData(
            name = name,
            email = email,
            message = message,
            type = type,
            timestamp = System.currentTimeMillis(),
            appVersion = "1.0.0",
            deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})",
            status = "new"
        )

        db.collection("feedback")
            .add(feedback)
            .addOnSuccessListener { documentReference ->
                isSubmitting = false
                android.widget.Toast.makeText(
                    context,
                    "Thank you! Your feedback has been submitted successfully.",
                    android.widget.Toast.LENGTH_LONG
                ).show()

                // Log success for debugging
                Log.d("AboutScreen", "Feedback submitted with ID: ${documentReference.id}")
            }
            .addOnFailureListener { exception ->
                isSubmitting = false
                android.widget.Toast.makeText(
                    context,
                    "Failed to submit feedback. Please check your internet connection and try again.",
                    android.widget.Toast.LENGTH_LONG
                ).show()

                // Log error for debugging
                Log.e("AboutScreen", "Error submitting feedback", exception)

                // Fallback to email if Firestore fails
                showEmailFallback(context, name, email, message, type)
            }
    }

    // Handle back press using BackHandler
    BackHandler(enabled = true) {
        navigateBack()
    }

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
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "About",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "About",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navigateBack()
                        },
                        enabled = !navigationInProgress
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = if (navigationInProgress) Color.White.copy(alpha = 0.5f) else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryOrange,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = if (systemTheme) darkBackground else lightBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Information Card
            InfoCard(
                title = "Campus Eats",
                icon = Icons.Default.Star,
                backgroundColor = cardBackground,
                textColor = textColor,
                secondaryTextColor = textSecondaryColor
            ) {
                InfoText(
                    text = "Still playing the daily guessing game with the mess menu?\n" +
                            "Campus Eats lets you finally plan your meals, discover food spots around campus, and avoid those heartbreaking thali surprises — because you deserve better than mystery meals.",
                    color = textSecondaryColor
                )


                InfoText(
                    text = "Version: 1.0.0",
                    color = textSecondaryColor,
                    fontSize = 14
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoText(
                    text = "Built with ❤️ for students",
                    color = primaryOrange,
                    fontSize = 14
                )
            }

            // Developer Information Card
            InfoCard(
                title = "About Developer",
                icon = Icons.Default.Person,
                backgroundColor = cardBackground,
                textColor = textColor,
                secondaryTextColor = textSecondaryColor
            ) {
                InfoText(
                    text = "Hi! I’m Divyansh — the brain, hands, and late-night coder behind *Campus Eats*. After years of suffering through random mess menus, mystery dinners, and apps on Play Store that only show meals (and not very well, honestly), I hit my breaking point and thought: 'If no one’s fixing this… fine, I’ll build it myself.'\n" +
                            "\n" +
                            "That’s how *Campus Eats* came to life. Not just another 'meal menu' app, but something actually useful — from daily menus to food spots around campus that every student wishes they knew about before hunger strikes.\n" +
                            "\n" +
                            "Of course, it wasn’t all smooth. This app is powered by Maggi at 2 AM, endless cups of chai, and a fair share of coding meltdowns. And I’ve got to give credit where it’s due — *Anurag* helps keep the menus fresh and hunts down food spots, so you don’t have to wander cluelessly when you’re starving.\n" +
                            "\n" +
                            "P.S. To my iPhone friends feeling left out — your version is simmering nicely. Soon, you’ll get your taste too.\n" +
                            "\n" +
                            "Built by student, for students — because knowing what’s for dinner is just the beginning of making campus life less disappointing.",
                    color = textSecondaryColor
                )







                Spacer(modifier = Modifier.height(12.dp))

                // Developer details
                DeveloperDetail("Name", "Divyansh Sharma", textSecondaryColor, primaryOrange)
                Spacer(modifier = Modifier.height(8.dp))
                DeveloperDetail("Email", "cueats2025@gmail.com", textSecondaryColor, primaryOrange)
            }
            // App Features Card
            InfoCard(
                title = "Key Features",
                icon = Icons.Default.Star,
                backgroundColor = cardBackground,
                textColor = textColor,
                secondaryTextColor = textSecondaryColor
            ) {
                FeatureItem("🍽️", "Weekly Meal Plans", "Browse and plan your weekly meals", textSecondaryColor)
                Spacer(modifier = Modifier.height(12.dp))
                FeatureItem("🏪", "Campus Shops", "Discover food outlets on campus", textSecondaryColor)
                Spacer(modifier = Modifier.height(12.dp))
                FeatureItem("🌓", "Theme Support", "Automatic dark/light mode", textSecondaryColor)
                Spacer(modifier = Modifier.height(12.dp))
                FeatureItem("🔔", "Notifications", "Stay updated with latest news", textSecondaryColor)
            }

            // Version History Card
            InfoCard(
                title = "Version History",
                icon = Icons.Default.Notifications,
                backgroundColor = cardBackground,
                textColor = textColor,
                secondaryTextColor = textSecondaryColor
            ) {
                UpdateItem(
                    version = "v1.0.0 - Initial Release",
                    date = "January 2025",
                    description = "• Weekly meal planning feature\n• Campus shop directory\n• Dark/Light theme support\n• Instant feedback system",
                    textColor = textSecondaryColor,
                    primaryOrange = primaryOrange
                )
            }

            // Tech Stack Card
//            InfoCard(
//                title = "Built With",
//                icon = Icons.Default.Star,
//                backgroundColor = cardBackground,
//                textColor = textColor,
//                secondaryTextColor = textSecondaryColor
//            ) {
//                TechItem("🎨", "Jetpack Compose", "Modern Android UI toolkit", textSecondaryColor)
//                Spacer(modifier = Modifier.height(8.dp))
//                TechItem("🔥", "Firebase", "Backend services and notifications", textSecondaryColor)
//                Spacer(modifier = Modifier.height(8.dp))
//                TechItem("📱", "Material 3", "Google's design system", textSecondaryColor)
//                Spacer(modifier = Modifier.height(8.dp))
//                TechItem("🏗️", "MVVM", "Clean architecture pattern", textSecondaryColor)
//                Spacer(modifier = Modifier.height(8.dp))
//                TechItem("📊", "Firestore", "Real-time feedback system", textSecondaryColor)
//            }

            // Contact & Support Card - Updated with instant feedback
            InfoCard(
                title = "Contact & Support",
                icon = Icons.Default.Info,
                backgroundColor = cardBackground,
                textColor = textColor,
                secondaryTextColor = textSecondaryColor
            ) {
                InfoText(
                    text = "Have questions, suggestions, or found a bug? Your feedback is sent instantly to our team!",
                    color = textSecondaryColor
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Clickable feedback options
                ClickableContactItem(
                    emoji = "💬",
                    title = "Send Feedback",
                    value = "Share your thoughts with us instantly",
                    textColor = textSecondaryColor,
                    primaryColor = primaryOrange,
                    onClick = {
                        feedbackType = "General Feedback"
                        showFeedbackDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                ClickableContactItem(
                    emoji = "🐛",
                    title = "Report Bug",
                    value = "Help us improve the app quickly",
                    textColor = textSecondaryColor,
                    primaryColor = primaryOrange,
                    onClick = {
                        feedbackType = "Bug Report"
                        showFeedbackDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                ClickableContactItem(
                    emoji = "💡",
                    title = "Feature Request",
                    value = "Suggest new features",
                    textColor = textSecondaryColor,
                    primaryColor = primaryOrange,
                    onClick = {
                        feedbackType = "Feature Request"
                        showFeedbackDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                ClickableContactItem(
                    emoji = "📧",
                    title = "Direct Email",
                    value = "cueats2025@gmail.com",
                    textColor = textSecondaryColor,
                    primaryColor = primaryOrange,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:cueats2025@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Campus Eats - General Inquiry")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // Footer
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = primaryOrange.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Thank you for using Campus Eats!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = primaryOrange,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Making campus dining better, one meal at a time.",
                        fontSize = 14.sp,
                        color = textSecondaryColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "© 2025 Campus Eats. All rights reserved.",
                        fontSize = 12.sp,
                        color = textSecondaryColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Feedback Dialog
        if (showFeedbackDialog) {
            FeedbackDialog(
                feedbackType = feedbackType,
                isSubmitting = isSubmitting,
                onDismiss = {
                    if (!isSubmitting) {
                        showFeedbackDialog = false
                    }
                },
                onSubmit = { name, email, message ->
                    submitFeedback(name, email, message, feedbackType)
                    showFeedbackDialog = false
                }
            )
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    icon: ImageVector,
    backgroundColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFFFF6B01),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }
            content()
        }
    }
}
fun showEmailFallback(context: Context, name: String, email: String, message: String, type: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        this.type = "text/plain"
        putExtra(Intent.EXTRA_EMAIL, arrayOf("cueats2025@gmail.com"))
        putExtra(Intent.EXTRA_SUBJECT, "Campus Eats - $type from $name")
        putExtra(Intent.EXTRA_TEXT, """
                📱 Campus Eats App Feedback
                
                Type: $type
                Name: $name
                Email: ${if (email.isNotBlank()) email else "Not provided"}
                
                Message:
                $message
                
                ---
                Sent from Campus Eats Mobile App
                Version: 1.1.1
                Date: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}
                Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})
                
                Note: This was sent via email fallback due to network issues.
            """.trimIndent())
    }

    try {
        context.startActivity(Intent.createChooser(intent, "Send Feedback via Email"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "No email app found. Please install an email app.", android.widget.Toast.LENGTH_LONG).show()
    }
}


@Composable
private fun InfoText(
    text: String,
    color: Color,
    fontSize: Int = 16
) {
    Text(
        text = text,
        fontSize = fontSize.sp,
        color = color,
        lineHeight = (fontSize + 4).sp
    )
}

@Composable
private fun DeveloperDetail(
    label: String,
    value: String,
    textColor: Color,
    primaryColor: Color
) {
    Row {
        Text(
            text = "$label: ",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = primaryColor
        )
    }
}

@Composable
private fun UpdateItem(
    version: String,
    date: String,
    description: String,
    textColor: Color,
    primaryOrange: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = version,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = primaryOrange
            )
            Text(
                text = date,
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            fontSize = 14.sp,
            color = textColor,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun FeatureItem(
    emoji: String,
    title: String,
    description: String,
    textColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 20.sp,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFF6B01)
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = textColor
            )
        }
    }
}

@Composable
private fun TechItem(
    emoji: String,
    title: String,
    description: String,
    textColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 16.sp,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFF6B01)
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = textColor
            )
        }
    }
}

// New Clickable Contact Item Composable
@Composable
private fun ClickableContactItem(
    emoji: String,
    title: String,
    value: String,
    textColor: Color,
    primaryColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = primaryColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                fontSize = 16.sp,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                Text(
                    text = value,
                    fontSize = 12.sp,
                    color = primaryColor
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Open",
                tint = primaryColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Updated Feedback Dialog Composable with submission state
@Composable
private fun FeedbackDialog(
    feedbackType: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = feedbackType,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B01)
                    )
                    if (!isSubmitting) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                }

                if (isSubmitting) {
                    // Submission in progress
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF6B01),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Submitting your feedback...",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    // Form fields
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = {
                            Text(when (feedbackType) {
                                "Bug Report" -> "Describe the bug *"
                                "Feature Request" -> "Describe your feature idea *"
                                else -> "Your message *"
                            })
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        )
                    )

                    // Info text
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFF6B01).copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "✨ Your feedback will be sent instantly to our team and helps us improve Campus Eats!",
                            fontSize = 12.sp,
                            color = Color(0xFFFF6B01),
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Text(
                        text = "* Required fields",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            enabled = !isSubmitting
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (name.isNotBlank() && message.isNotBlank()) {
                                    onSubmit(name, email, message)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = name.isNotBlank() && message.isNotBlank() && !isSubmitting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6B01)
                            )
                        ) {
                            Text("Send Instantly")
                        }
                    }
                }
            }
        }
    }
}