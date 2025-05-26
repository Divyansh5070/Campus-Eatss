package com.example.cueats.LoginScreen


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Define custom colors
object CampusEatsColors {
    val Primary = Color(0xFFFF6B35) // Orange color from design
    val Background = Color(0xFFF5F5F5)
    val Surface = Color.White
    val OnSurface = Color(0xFF333333)
    val OnSurfaceVariant = Color(0xFF666666)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusEatsAuthScreen() {
    var isSignUp by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    CampusEatsHeader()
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CampusEatsColors.Surface
                )
            )
        },
        containerColor = CampusEatsColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp)) // Minor spacing below top bar

            // Auth Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CampusEatsColors.Surface),
//                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                if (isSignUp) {
                    SignUpForm(onSwitchToSignIn = { isSignUp = false })
                } else {
                    SignInForm(onSwitchToSignUp = { isSignUp = true })
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer
            Text(
                text = "© 2023 Campus Eats. All rights reserved.",
                color = CampusEatsColors.OnSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
fun CampusEatsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "CAMPUS" inside orange box
        Box(
            modifier = Modifier
                .background(
                    CampusEatsColors.Primary,
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "CAMPUS",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = "Eat's",
            color = CampusEatsColors.OnSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.width(6.dp))

        // "Multi-Campus" badge
        Box(
            modifier = Modifier
                .background(
                    Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 5.dp, vertical = 0.dp)
        ) {
            Text(
                text = "Multi-Campus",
                color = CampusEatsColors.Primary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInForm(onSwitchToSignUp: () -> Unit) {
    var selectedUniversity by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val universities = listOf(
        "Choose your university",
        "Panjab University",
        "Chandigarh University",
        "PEC (Punjab Engineering College)",
        "Post Graduate Government College (PGGC)",
        "Goswami Ganesh Dutta Sanatan Dharma College (GGDSD)",
        "DAV College, Chandigarh",
        "Government College of Commerce and Business Administration",
        "Sri Guru Gobind Singh College",
        "Government College for Girls, Sector 11"
    )


    Column(
        modifier = Modifier.padding(24.dp)
    ) {
        // Welcome Back Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    CampusEatsColors.Primary,
                    RoundedCornerShape(12.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Welcome Back",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sign in to continue to Campus Eats",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Sign In",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CampusEatsColors.OnSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // University Dropdown
        Text(
            text = "Select Your University",
            fontSize = 14.sp,
            color = CampusEatsColors.OnSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedUniversity.ifEmpty { "Choose your university" },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(8.dp)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                universities.drop(1).forEach { university ->
                    DropdownMenuItem(
                        text = { Text(university) },
                        onClick = {
                            selectedUniversity = university
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Email Field
        CustomTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email Address",
            placeholder = "Enter your email address",
            leadingIcon = Icons.Default.Email,
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        CustomTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            placeholder = "Enter your password",
            leadingIcon = Icons.Default.Lock,
            isPassword = true,
            passwordVisible = passwordVisible,
            onPasswordVisibilityChange = { passwordVisible = !passwordVisible }
        )

        // Forgot Password
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { /* Handle forgot password */ }
            ) {
                Text(
                    text = "Forgot password?",
                    color = CampusEatsColors.Primary,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign In Button
        Button(
            onClick = { /* Handle sign in */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CampusEatsColors.Primary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Sign In",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Switch to Sign Up
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "New to Campus Eats? ",
                color = CampusEatsColors.OnSurfaceVariant,
                fontSize = 14.sp
            )
            TextButton(
                onClick = onSwitchToSignUp
            ) {
                Text(
                    text = "Create an account",
                    color = CampusEatsColors.Primary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpForm(onSwitchToSignIn: () -> Unit) {
    var selectedUniversity by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val universities = listOf(
        "Choose your university",
        "Harvard University",
        "Stanford University",
        "MIT",
        "University of California, Berkeley",
        "Yale University",
        "Princeton University",
        "Columbia University",
        "University of Chicago",
        "University of Pennsylvania"
    )

    // Password validation states
    val hasMinLength = password.length >= 8
    val hasNumber = password.any { it.isDigit() }
    val hasSpecialChar = password.any { !it.isLetterOrDigit() }
    val hasUppercase = password.any { it.isUpperCase() }

    Column(
        modifier = Modifier.padding(24.dp)
    ) {
        // Join Campus Eats Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    CampusEatsColors.Primary,
                    RoundedCornerShape(12.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Join Campus Eats",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Create your account to get started",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Create Account",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CampusEatsColors.OnSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // University Dropdown
        Text(
            text = "Select Your University",
            fontSize = 14.sp,
            color = CampusEatsColors.OnSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedUniversity.ifEmpty { "Choose your university" },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(8.dp)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                universities.drop(1).forEach { university ->
                    DropdownMenuItem(
                        text = { Text(university) },
                        onClick = {
                            selectedUniversity = university
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Full Name Field
        CustomTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = "Full Name",
            placeholder = "Enter your full name",
            leadingIcon = Icons.Default.Person
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email Field
        CustomTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email Address",
            placeholder = "Enter your email address",
            leadingIcon = Icons.Default.Email,
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        CustomTextField(
            value = password,
            onValueChange = { password = it },
            label = "Create Password",
            placeholder = "Create a strong password",
            leadingIcon = Icons.Default.Lock,
            isPassword = true,
            passwordVisible = passwordVisible,
            onPasswordVisibilityChange = { passwordVisible = !passwordVisible }
        )

        // Password Requirements
        if (password.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(top = 8.dp)
            ) {
                PasswordRequirement(
                    text = "At least 8 characters",
                    isMet = hasMinLength
                )
                PasswordRequirement(
                    text = "Contains a number",
                    isMet = hasNumber
                )
                PasswordRequirement(
                    text = "Contains a special character",
                    isMet = hasSpecialChar
                )
                PasswordRequirement(
                    text = "Contains uppercase letter",
                    isMet = hasUppercase
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Create Account Button
        Button(
            onClick = { /* Handle sign up */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CampusEatsColors.Primary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Create Account",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Switch to Sign In
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Already have an account? ",
                color = CampusEatsColors.OnSurfaceVariant,
                fontSize = 14.sp
            )
            TextButton(
                onClick = onSwitchToSignIn
            ) {
                Text(
                    text = "Sign in",
                    color = CampusEatsColors.Primary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityChange: (() -> Unit)? = null
) {
    Column {
        Text(
            text = label,
            fontSize = 14.sp,
            color = CampusEatsColors.OnSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = CampusEatsColors.OnSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = CampusEatsColors.OnSurfaceVariant
                )
            },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { onPasswordVisibilityChange?.invoke() }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Check else Icons.Default.Lock,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = CampusEatsColors.OnSurfaceVariant
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CampusEatsColors.Primary,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun PasswordRequirement(
    text: String,
    isMet: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = if (isMet) "✓" else "✗",
            color = if (isMet) Color.Green else Color.Red,
            fontSize = 12.sp,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            color = if (isMet) Color.Green else CampusEatsColors.OnSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCampusEatsAuthScreen() {
    MaterialTheme {
        CampusEatsAuthScreen()
    }
}