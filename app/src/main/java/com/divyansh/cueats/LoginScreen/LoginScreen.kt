package com.divyansh.cueats.LoginScreen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val authState by viewModel.authState

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoginMode by remember { mutableStateOf(true) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    val idToken = account.idToken
                    if (idToken != null) {
                        viewModel.signInWithGoogle(idToken)
                    } else {
                        viewModel.setError("Failed to get authentication token from Google")
                    }
                } catch (e: ApiException) {
                    when (e.statusCode) {
                        12501 -> viewModel.clearError()
                        12502 -> viewModel.setError("Network error during Google Sign-In")
                        12500 -> viewModel.setError("Google Sign-In service error")
                        else -> viewModel.setError("Google Sign-In failed: Code ${e.statusCode}")
                    }
                }
            }
            Activity.RESULT_CANCELED -> viewModel.clearError()
            else -> {
                if (result.resultCode != Activity.RESULT_CANCELED) {
                    viewModel.setError("Google Sign-In failed unexpectedly")
                }
            }
        }
    }

    // Optimized navigation effect - removed delay
    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            onNavigateToHome()
        }
    }

    // Clear form when switching modes
    LaunchedEffect(isLoginMode) {
        name = ""
        email = ""
        password = ""
        confirmPassword = ""
        viewModel.clearError()
    }

    LoginScreenContent(
        authState = authState,
        name = name,
        email = email,
        password = password,
        confirmPassword = confirmPassword,
        isPasswordVisible = isPasswordVisible,
        isConfirmPasswordVisible = isConfirmPasswordVisible,
        isLoginMode = isLoginMode,
        resetEmail = resetEmail,
        showForgotPasswordDialog = showForgotPasswordDialog,
        onNameChange = { name = it },
        onEmailChange = { email = it },
        onPasswordChange = { password = it },
        onConfirmPasswordChange = { confirmPassword = it },
        onPasswordVisibilityToggle = { isPasswordVisible = !isPasswordVisible },
        onConfirmPasswordVisibilityToggle = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
        onModeToggle = { isLoginMode = !isLoginMode },
        onResetEmailChange = { resetEmail = it },
        onShowForgotPasswordDialog = { showForgotPasswordDialog = it },
        onGoogleSignIn = {
            try {
                val googleSignInClient = viewModel.getGoogleSignInClient(context)
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            } catch (e: Exception) {
                viewModel.setError("Failed to initialize Google Sign-In: ${e.message}")
            }
        },
        onLogin = { viewModel.loginWithEmail(email, password) },
        onRegister = { viewModel.registerWithEmail(email, password, name) },
        onResetPassword = { viewModel.resetPassword(resetEmail) },
        onClearError = { viewModel.clearError() },
        onClearSuccess = { viewModel.clearSuccess() }
    )
}

@Composable
private fun LoginScreenContent(
    authState: AuthState,
    name: String,
    email: String,
    password: String,
    confirmPassword: String,
    isPasswordVisible: Boolean,
    isConfirmPasswordVisible: Boolean,
    isLoginMode: Boolean,
    resetEmail: String,
    showForgotPasswordDialog: Boolean,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onConfirmPasswordVisibilityToggle: () -> Unit,
    onModeToggle: () -> Unit,
    onResetEmailChange: (String) -> Unit,
    onShowForgotPasswordDialog: (Boolean) -> Unit,
    onGoogleSignIn: () -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onResetPassword: () -> Unit,
    onClearError: () -> Unit,
    onClearSuccess: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = remember(configuration) { configuration.screenHeightDp.dp < 700.dp }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppGradients.backgroundGradient)
    ) {
        // Background decoration
        BackgroundDecoration()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(
                    top = if (isSmallScreen) 20.dp else 40.dp,
                    bottom = 24.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppLogoSection(isSmallScreen = isSmallScreen)

            LoginFormCard(
                authState = authState,
                name = name,
                email = email,
                password = password,
                confirmPassword = confirmPassword,
                isPasswordVisible = isPasswordVisible,
                isConfirmPasswordVisible = isConfirmPasswordVisible,
                isLoginMode = isLoginMode,
                isSmallScreen = isSmallScreen,
                onNameChange = onNameChange,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onConfirmPasswordChange = onConfirmPasswordChange,
                onPasswordVisibilityToggle = onPasswordVisibilityToggle,
                onConfirmPasswordVisibilityToggle = onConfirmPasswordVisibilityToggle,
                onModeToggle = onModeToggle,
                onShowForgotPasswordDialog = onShowForgotPasswordDialog,
                onGoogleSignIn = onGoogleSignIn,
                onLogin = onLogin,
                onRegister = onRegister
            )
        }

        // Error and success messages
        MessageDisplay(
            authState = authState,
            isSmallScreen = isSmallScreen,
            onClearError = onClearError,
            onClearSuccess = onClearSuccess
        )
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            resetEmail = resetEmail,
            authState = authState,
            onEmailChange = onResetEmailChange,
            onDismiss = { onShowForgotPasswordDialog(false) },
            onResetPassword = {
                if (resetEmail.isNotBlank()) {
                    onResetPassword()
                    onShowForgotPasswordDialog(false)
                }
            }
        )
    }
}

@Composable
private fun BackgroundDecoration() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppGradients.sweepGradient)
    )
}

@Composable
private fun AppLogoSection(isSmallScreen: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = if (isSmallScreen) 32.dp else 48.dp)
    ) {
        Spacer(modifier = Modifier.height(15.dp))

        Image(
            painter = painterResource(id = com.divyansh.cueats.R.drawable.logo33),
            contentDescription = "Campus Eats Logo",
            modifier = Modifier
                .size(if (isSmallScreen) 90.dp else 110.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = AppColors.primaryOrange.copy(alpha = 0.4f),
                    spotColor = AppColors.primaryOrange.copy(alpha = 0.6f)
                )
                .clip(RoundedCornerShape(28.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(if (isSmallScreen) 14.dp else 20.dp))

        Text(
            text = "Campus Eats",
            fontSize = if (isSmallScreen) 32.sp else 40.sp,
            fontWeight = FontWeight.Black,
            color = AppColors.textPrimary,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Discover what's cooking on campus",
            fontSize = if (isSmallScreen) 14.sp else 16.sp,
            color = AppColors.textSecondary,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            letterSpacing = 0.3.sp
        )

        DecorativeElement()
    }
}

@Composable
private fun DecorativeElement() {
    Box(
        modifier = Modifier
            .width(80.dp)
            .height(2.dp)
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(AppGradients.decorativeGradient)
    )
}

@Composable
private fun LoginFormCard(
    authState: AuthState,
    name: String,
    email: String,
    password: String,
    confirmPassword: String,
    isPasswordVisible: Boolean,
    isConfirmPasswordVisible: Boolean,
    isLoginMode: Boolean,
    isSmallScreen: Boolean,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onConfirmPasswordVisibilityToggle: () -> Unit,
    onModeToggle: () -> Unit,
    onShowForgotPasswordDialog: (Boolean) -> Unit,
    onGoogleSignIn: () -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = AppColors.cardBackground),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FormHeader(isLoginMode = isLoginMode, isSmallScreen = isSmallScreen)

            GoogleSignInButton(
                isLoading = authState.isLoading,
                isSmallScreen = isSmallScreen,
                onClick = onGoogleSignIn
            )

            Spacer(modifier = Modifier.height(24.dp))
            FormDivider()
            Spacer(modifier = Modifier.height(24.dp))

            FormFields(
                name = name,
                email = email,
                password = password,
                confirmPassword = confirmPassword,
                isPasswordVisible = isPasswordVisible,
                isConfirmPasswordVisible = isConfirmPasswordVisible,
                isLoginMode = isLoginMode,
                onNameChange = onNameChange,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onConfirmPasswordChange = onConfirmPasswordChange,
                onPasswordVisibilityToggle = onPasswordVisibilityToggle,
                onConfirmPasswordVisibilityToggle = onConfirmPasswordVisibilityToggle
            )

            if (isLoginMode) {
                ForgotPasswordLink(
                    email = email,
                    onShowDialog = onShowForgotPasswordDialog
                )
            }

            Spacer(modifier = Modifier.height(if (isSmallScreen) 32.dp else 40.dp))

            ActionButton(
                isLoginMode = isLoginMode,
                isLoading = authState.isLoading,
                isFormValid = if (isLoginMode) {
                    email.isNotBlank() && password.isNotBlank()
                } else {
                    name.isNotBlank() && email.isNotBlank() && password.isNotBlank() &&
                            confirmPassword.isNotBlank() && password == confirmPassword
                },
                isSmallScreen = isSmallScreen,
                onLogin = onLogin,
                onRegister = onRegister
            )

            Spacer(modifier = Modifier.height(if (isSmallScreen) 24.dp else 32.dp))

            ModeToggleCard(
                isLoginMode = isLoginMode,
                isSmallScreen = isSmallScreen,
                onToggle = onModeToggle
            )
        }
    }

    Spacer(modifier = Modifier.height(if (isSmallScreen) 20.dp else 32.dp))
}

@Composable
private fun FormHeader(isLoginMode: Boolean, isSmallScreen: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isSmallScreen) 28.dp else 36.dp)
    ) {
        Text(
            text = if (isLoginMode) "Welcome Back!" else "Join the Community",
            fontSize = if (isSmallScreen) 24.sp else 28.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isLoginMode) "Sign in to explore campus dining" else "Create your account to get started",
            fontSize = if (isSmallScreen) 14.sp else 16.sp,
            color = AppColors.textSecondary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )

        Box(
            modifier = Modifier
                .width(120.dp)
                .height(3.dp)
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(AppGradients.headerUnderlineGradient)
        )
    }
}

@Composable
private fun GoogleSignInButton(
    isLoading: Boolean,
    isSmallScreen: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            disabledContainerColor = Color.White.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isSmallScreen) 56.dp else 64.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(20.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = com.divyansh.cueats.R.drawable.google_logo),
                contentDescription = "Google Logo",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Continue with Google",
                fontSize = if (isSmallScreen) 16.sp else 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                letterSpacing = 0.3.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

        }
    }
}

@Composable
private fun FormDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(AppColors.surfaceVariant)
        )
        Text(
            text = "OR",
            color = AppColors.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(AppColors.surfaceVariant)
        )
    }
}

@Composable
private fun PasswordCriteriaSection(
    password: String
) {
    val lengthCriteria = password.length >= 8
    val upperCaseCriteria = password.any { it.isUpperCase() }
    val numberCriteria = password.any { it.isDigit() }
    val specialCharCriteria = password.any { "!@#\$%^&*()_+-=[]{}|;:',.<>/?".contains(it) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp)
    ) {
        CriteriaText("At least 8 characters", lengthCriteria)
        CriteriaText("Contains an uppercase letter", upperCaseCriteria)
        CriteriaText("Contains a number", numberCriteria)
        CriteriaText("Contains a special character", specialCharCriteria)
    }
}

@Composable
private fun CriteriaText(text: String, fulfilled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (fulfilled) Icons.Default.Check else Icons.Default.Clear,
            contentDescription = null,
            tint = if (fulfilled) AppColors.successColor else AppColors.errorColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = if (fulfilled) AppColors.successColor else AppColors.errorColor,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun FormFields(
    name: String,
    email: String,
    password: String,
    confirmPassword: String,
    isPasswordVisible: Boolean,
    isConfirmPasswordVisible: Boolean,
    isLoginMode: Boolean,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onConfirmPasswordVisibilityToggle: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        AnimatedVisibility(
            visible = !isLoginMode,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            CustomTextField(
                value = name,
                onValueChange = onNameChange,
                label = "Full Name",
                leadingIcon = Icons.Default.Person
            )
        }

        CustomTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email Address",
            leadingIcon = Icons.Default.Email,
            keyboardType = KeyboardType.Email
        )

        CustomTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password",
            leadingIcon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            isPassword = true,
            isPasswordVisible = isPasswordVisible,
            onPasswordVisibilityToggle = onPasswordVisibilityToggle
        )

        // Password criteria below the password field (registration only)
        AnimatedVisibility(visible = !isLoginMode) {
            PasswordCriteriaSection(password = password)
        }

        // Confirm Password Field (only for registration)
        AnimatedVisibility(
            visible = !isLoginMode,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            CustomTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "Confirm Password",
                leadingIcon = Icons.Default.Lock,
                keyboardType = KeyboardType.Password,
                isPassword = true,
                isPasswordVisible = isConfirmPasswordVisible,
                onPasswordVisibilityToggle = onConfirmPasswordVisibilityToggle,
                isError = confirmPassword.isNotBlank() && password != confirmPassword,
                supportingText = if (confirmPassword.isNotBlank() && password != confirmPassword) {
                    "Passwords do not match"
                } else null
            )
        }
    }
}


@Composable
private fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordVisibilityToggle: (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = AppColors.textSecondary, fontSize = 14.sp) },
        leadingIcon = {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint = AppColors.primaryOrange,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = if (isPassword && onPasswordVisibilityToggle != null) {
            {
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(
                        if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                        tint = AppColors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !isPasswordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = if (isPassword) ImeAction.Done else ImeAction.Next
        ),
        colors = AppTextFieldColors.getColors(),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = isError,
        supportingText = supportingText?.let { text ->
            { Text(text, color = AppColors.errorColor, fontSize = 12.sp) }
        }
    )
}

@Composable
private fun ForgotPasswordLink(
    email: String,
    onShowDialog: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = "Forgot Password?",
            color = AppColors.primaryOrange,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable {
                onShowDialog(true)
            }
        )
    }
}

@Composable
private fun ActionButton(
    isLoginMode: Boolean,
    isLoading: Boolean,
    isFormValid: Boolean,
    isSmallScreen: Boolean,
    onLogin: () -> Unit,
    onRegister: () -> Unit
) {
    Button(
        onClick = { if (isLoginMode) onLogin() else onRegister() },
        enabled = !isLoading && isFormValid,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isSmallScreen) 56.dp else 64.dp)
            .shadow(
                elevation = if (isFormValid && !isLoading) 12.dp else 0.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = AppColors.primaryOrange.copy(alpha = 0.3f),
                spotColor = AppColors.primaryOrange.copy(alpha = 0.5f)
            )
            .background(
                brush = if (isFormValid && !isLoading) {
                    AppGradients.actionButtonGradient
                } else {
                    AppGradients.disabledButtonGradient
                },
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = AppColors.textPrimary,
                    modifier = Modifier.size(if (isSmallScreen) 20.dp else 24.dp),
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = if (isLoginMode) "Signing In..." else "Creating Account...",
                    fontSize = if (isSmallScreen) 16.sp else 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textPrimary
                )
            }
        } else {
            Text(
                text = if (isLoginMode) "Sign In" else "Create Account",
                fontSize = if (isSmallScreen) 16.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun ModeToggleCard(
    isLoginMode: Boolean,
    isSmallScreen: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = AppColors.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildAnnotatedString {
                    append(if (isLoginMode) "New to Campus Eats? " else "Already have an account? ")
                    withStyle(
                        style = SpanStyle(
                            color = AppColors.primaryOrange,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(if (isLoginMode) "Create Account" else "Sign In")
                    }
                },
                color = AppColors.textSecondary,
                fontSize = if (isSmallScreen) 14.sp else 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BoxScope.MessageDisplay(
    authState: AuthState,
    isSmallScreen: Boolean,
    onClearError: () -> Unit,
    onClearSuccess: () -> Unit
) {
    authState.error?.let { error ->
        MessageCard(
            message = error,
            isError = true,
            isSmallScreen = isSmallScreen,
            onDismiss = onClearError
        )
    }

    authState.successMessage?.let { message ->
        MessageCard(
            message = message,
            isError = false,
            isSmallScreen = isSmallScreen,
            onDismiss = onClearSuccess
        )
     }
}

@Composable
private fun BoxScope.MessageCard(
    message: String,
    isError: Boolean,
    isSmallScreen: Boolean,
    onDismiss: () -> Unit
) {
    val color = if (isError) AppColors.errorColor else AppColors.successColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .align(Alignment.TopCenter)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = color.copy(alpha = 0.3f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isError) Icons.Default.Error else Icons.Default.Check,
                contentDescription = if (isError) "Error" else "Success",
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = color,
                fontSize = if (isSmallScreen) 14.sp else 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ForgotPasswordDialog(
    resetEmail: String,
    authState: AuthState,
    onEmailChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onResetPassword: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.cardBackground,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Reset Password",
                color = AppColors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter your email address and we'll send you a link to reset your password.",
                    color = AppColors.textSecondary,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                CustomTextField(
                    value = resetEmail,
                    onValueChange = onEmailChange,
                    label = "Email Address",
                    leadingIcon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onResetPassword,
                enabled = resetEmail.isNotBlank() && !authState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.primaryOrange,
                    disabledContainerColor = AppColors.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (authState.isLoading) {
                    CircularProgressIndicator(
                        color = AppColors.textPrimary,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Send Reset Email",
                        color = AppColors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = AppColors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

// Theme constants for better performance
private object AppColors {
    val primaryOrange = Color(0xFFFF6B35)
    val primaryOrangeLight = Color(0xFFFF8F65)
    val primaryOrangeDark = Color(0xFFE55A2E)
    val backgroundDark = Color(0xFF0D0D0D)
    val cardBackground = Color(0xFF222222)
    val surfaceVariant = Color(0xFF2C2C2C)
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB8B8B8)
    val errorColor = Color(0xFFFF4444)
    val successColor = Color(0xFF00C853)
}

private object AppGradients {
    val backgroundGradient = Brush.radialGradient(
        colors = listOf(
            AppColors.backgroundDark,
            Color(0xFF151515),
            AppColors.backgroundDark
        ),
        radius = 1000f
    )

    val sweepGradient = Brush.sweepGradient(
        colors = listOf(
            Color.Transparent,
            AppColors.primaryOrange.copy(alpha = 0.03f),
            Color.Transparent,
            AppColors.primaryOrangeLight.copy(alpha = 0.02f)
        )
    )

    val decorativeGradient = Brush.horizontalGradient(
        colors = listOf(
            Color.Transparent,
            AppColors.primaryOrange,
            AppColors.primaryOrangeLight,
            Color.Transparent
        )
    )

    val headerUnderlineGradient = Brush.horizontalGradient(
        colors = listOf(
            AppColors.primaryOrange,
            AppColors.primaryOrangeLight,
            AppColors.primaryOrange
        )
    )

    val actionButtonGradient = Brush.horizontalGradient(
        colors = listOf(
            AppColors.primaryOrange,
            AppColors.primaryOrangeLight,
            AppColors.primaryOrangeDark
        )
    )

    val disabledButtonGradient = Brush.horizontalGradient(
        colors = listOf(
            AppColors.surfaceVariant,
            AppColors.surfaceVariant.copy(alpha = 0.7f)
        )
    )
}

private object AppTextFieldColors {
    @Composable
    fun getColors() = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AppColors.primaryOrange,
        unfocusedBorderColor = AppColors.surfaceVariant,
        focusedTextColor = AppColors.textPrimary,
        unfocusedTextColor = AppColors.textPrimary,
        cursorColor = AppColors.primaryOrange,
        focusedLabelColor = AppColors.primaryOrange,
        unfocusedLabelColor = AppColors.textSecondary,
        focusedContainerColor = AppColors.surfaceVariant.copy(alpha = 0.2f),
        unfocusedContainerColor = AppColors.surfaceVariant.copy(alpha = 0.1f)
    )
}