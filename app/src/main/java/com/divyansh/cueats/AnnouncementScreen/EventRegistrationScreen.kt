package com.divyansh.cueats.AnnouncementScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

/**
 * Event Registration Screen
 * Phase 1: Basic registration form with name, email, and phone
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventRegistrationScreen(
    navController: NavController,
    eventId: String,
    eventTitle: String,
    viewModel: RegistrationViewModel = viewModel()
) {
    // Check if user is already registered
    LaunchedEffect(eventId) {
        viewModel.checkRegistrationStatus(eventId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Registration") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0D0D0D)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (viewModel.isRegistrationSuccess) {
                // Success state
                SuccessScreen(
                    eventTitle = eventTitle,
                    onDone = { navController.popBackStack() }
                )
            } else if (viewModel.isAlreadyRegistered) {
                // Already registered state
                AlreadyRegisteredScreen(
                    eventTitle = eventTitle,
                    onBack = { navController.popBackStack() }
                )
            } else {
                // Registration form
                RegistrationForm(
                    eventTitle = eventTitle,
                    viewModel = viewModel,
                    onSubmit = { viewModel.submitRegistration(eventId, eventTitle) }
                )
            }
        }
    }
}

@Composable
private fun RegistrationForm(
    eventTitle: String,
    viewModel: RegistrationViewModel,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Event title
        Text(
            text = "Register for",
            fontSize = 14.sp,
            color = Color.Gray
        )
        Text(
            text = eventTitle,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Name field
        OutlinedTextField(
            value = viewModel.userName,
            onValueChange = { viewModel.updateUserName(it) },
            label = { Text("Full Name") },
            placeholder = { Text("Enter your full name") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF4285F4),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF4285F4),
                unfocusedLabelColor = Color.Gray,
                cursorColor = Color(0xFF4285F4)
            ),
            singleLine = true
        )
        
        // Email field
        OutlinedTextField(
            value = viewModel.userEmail,
            onValueChange = { viewModel.updateUserEmail(it) },
            label = { Text("Email") },
            placeholder = { Text("Enter your email") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF4285F4),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF4285F4),
                unfocusedLabelColor = Color.Gray,
                cursorColor = Color(0xFF4285F4)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )
        
        // Phone field
        OutlinedTextField(
            value = viewModel.userPhone,
            onValueChange = { viewModel.updateUserPhone(it) },
            label = { Text("Phone Number") },
            placeholder = { Text("Enter your phone number") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF4285F4),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF4285F4),
                unfocusedLabelColor = Color.Gray,
                cursorColor = Color(0xFF4285F4)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true
        )
        
        // Error message
        if (viewModel.errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = viewModel.errorMessage ?: "",
                    color = Color(0xFFC62828),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Submit button
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4285F4)
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = !viewModel.isLoading
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Submit Registration",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SuccessScreen(
    eventTitle: String,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success",
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Registration Successful!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "You have successfully registered for",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        Text(
            text = eventTitle,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4285F4)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Done",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AlreadyRegisteredScreen(
    eventTitle: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Already Registered",
            tint = Color(0xFF4285F4),
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Already Registered",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "You are already registered for",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        Text(
            text = eventTitle,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4285F4)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Back to Event",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
