package com.divyansh.cueats.SettingScreen

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val coroutineScope = rememberCoroutineScope()

    // State variables
    val isHosteller by userPreferences.isHosteller.collectAsState(initial = false)
    val userName by userPreferences.userName.collectAsState(initial = "")
    val universityId by userPreferences.universityId.collectAsState(initial = "")

    var tempUserName by remember { mutableStateOf("") }
    var tempUniversityId by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    // Update temp values when actual values change
    LaunchedEffect(userName, universityId) {
        tempUserName = userName
        tempUniversityId = universityId
    }

    val isLightTheme = !isSystemInDarkTheme()
    val backgroundColor = if (isLightTheme) Color(0xFFF8F9FA) else Color(0xFF0D1117)
    val surfaceColor = if (isLightTheme) Color.White else Color(0xFF161B22)
    val primaryColor = Color(0xFFFF6B35)
    val textPrimary = if (isLightTheme) Color(0xFF2D2D2D) else Color(0xFFE8E8E8)
    val textSecondary = if (isLightTheme) Color(0xFF6B7280) else Color(0xFF8B949E)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Custom Top App Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = surfaceColor,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = primaryColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = primaryColor
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "Settings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Type Section
                item {
                    SettingsSection(
                        title = "User Type",
                        surfaceColor = surfaceColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Hosteller",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textPrimary
                                )
                                Text(
                                    text = if (isHosteller) "Mess will be your default screen"
                                    else "Shops will be your default screen",
                                    fontSize = 14.sp,
                                    color = textSecondary
                                )
                            }

                            ModernSwitch(
                                checked = isHosteller,
                                onCheckedChange = { newValue ->
                                    coroutineScope.launch {
                                        userPreferences.setHostellerStatus(newValue)
                                    }
                                },
                                primaryColor = primaryColor
                            )
                        }
                    }
                }

                // Profile Section
                item {
                    SettingsSection(
                        title = "Profile Information",
                        surfaceColor = surfaceColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ModernTextField(
                                value = tempUserName,
                                onValueChange = { tempUserName = it },
                                label = "Full Name",
                                leadingIcon = Icons.Default.Person,
                                primaryColor = primaryColor,
                                textColor = textPrimary,
                                isLightTheme = isLightTheme
                            )

                            ModernTextField(
                                value = tempUniversityId,
                                onValueChange = { tempUniversityId = it },
                                label = "University ID",
                                leadingIcon = Icons.Default.Person,
                                primaryColor = primaryColor,
                                textColor = textPrimary,
                                isLightTheme = isLightTheme,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                            )

                            if (tempUserName != userName || tempUniversityId != universityId) {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + slideInVertically(),
                                    exit = fadeOut() + slideOutVertically()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                tempUserName = userName
                                                tempUniversityId = universityId
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = textSecondary
                                            ),
                                            border = BorderStroke(1.dp, textSecondary.copy(alpha = 0.3f))
                                        ) {
                                            Text("Cancel")
                                        }

                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    userPreferences.setUserName(tempUserName)
                                                    userPreferences.setUniversityId(tempUniversityId)
                                                    showSaveDialog = true
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = primaryColor
                                            )
                                        ) {
                                            Text("Save", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Actions Section
                item {
                    SettingsSection(
                        title = "Actions",
                        surfaceColor = surfaceColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    ) {
                        SettingsItem(
                            icon = Icons.Default.ExitToApp,
                            title = "Logout",
                            subtitle = "Sign out of your account",
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            iconTint = Color(0xFFDC2626),
                            onClick = { showLogoutDialog = true }
                        )
                    }
                }

                // Add some bottom padding for the last item
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Logout",
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to logout? You'll need to sign in again.",
                    color = textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            userPreferences.setLoggedIn(false)
                            userPreferences.clearAllData()
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626)
                    )
                ) {
                    Text("Logout", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Cancel", color = textSecondary)
                }
            },
            containerColor = surfaceColor
        )
    }

    // Save Success Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF059669)
                    )
                    Text(
                        text = "Success",
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
            },
            text = {
                Text(
                    text = "Your profile information has been updated successfully.",
                    color = textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { showSaveDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor
                    )
                ) {
                    Text("OK", color = Color.White)
                }
            },
            containerColor = surfaceColor
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    surfaceColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = textSecondary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = surfaceColor,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 2.dp
        ) {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    textPrimary: Color,
    textSecondary: Color,
    iconTint: Color = textPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = textSecondary
            )
        }
    }
}

@Composable
fun ModernSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    primaryColor: Color
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = primaryColor,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    primaryColor: Color,
    textColor: Color,
    isLightTheme: Boolean,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = primaryColor
            )
        },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = primaryColor,
            focusedLabelColor = primaryColor,
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            unfocusedBorderColor = if (isLightTheme) Color.Gray.copy(alpha = 0.3f)
            else Color.Gray.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = keyboardOptions,
        singleLine = true
    )
}