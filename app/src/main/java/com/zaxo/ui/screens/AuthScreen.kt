package com.zaxo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.zaxo.R
import com.zaxo.ui.neumorphic.*
import com.zaxo.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

enum class AuthScreenState {
    WELCOME,
    EMAIL_SIGN_IN,
    ZAXO_NUMBER_SIGN_IN,
    EMAIL_SIGN_UP,
    PHONE_VERIFICATION,
    FORGOT_PASSWORD,
    PNV_TOKEN_LOGIN
}

enum class PasswordStrength {
    WEAK, MEDIUM, STRONG, VERY_STRONG
}

@Composable
fun AuthScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val darkThemeSetting by viewModel.darkTheme.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = darkThemeSetting ?: systemDark

    val bgColor = NeuTheme.getBgColor(darkTheme)
    val textColor = NeuTheme.getTextColor(darkTheme)
    val secondaryText = NeuTheme.getSecondaryTextColor(darkTheme)

    var currentScreen by remember { mutableStateOf(AuthScreenState.WELCOME) }
    var useSmsFallback by remember { mutableStateOf(false) }
    val authError by viewModel.authError.collectAsState()
    val pnvUiState by viewModel.pnvUiState.collectAsState()
    val pnvError by viewModel.pnvError.collectAsState()

    // Form inputs state
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedCountryCode by remember { mutableStateOf("+1") }
    var isTermsAccepted by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }

    // Zaxo number formatting
    var zaxoNumberInput by remember { mutableStateOf("") }

    // Password Visibility states
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    // Logo Shrink State
    val isLogoShrunk = currentScreen != AuthScreenState.WELCOME

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Absolute-positioned Back button (Top Start)
        if (currentScreen != AuthScreenState.WELCOME) {
            NeumorphicIconButton(
                onClick = {
                    currentScreen = when (currentScreen) {
                        AuthScreenState.ZAXO_NUMBER_SIGN_IN -> AuthScreenState.EMAIL_SIGN_IN
                        AuthScreenState.FORGOT_PASSWORD -> AuthScreenState.EMAIL_SIGN_IN
                        AuthScreenState.PHONE_VERIFICATION -> AuthScreenState.EMAIL_SIGN_UP
                        else -> AuthScreenState.WELCOME
                    }
                },
                cornerRadius = 14.dp,
                darkTheme = darkTheme,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Absolute-positioned Day/Night Switcher (Top End)
        ThemeToggle(
            darkTheme = darkTheme,
            onToggle = { viewModel.setDarkTheme(!darkTheme) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Animated Zaxo Logo
            AnimatedLogo(
                shrunk = isLogoShrunk,
                darkTheme = darkTheme
            )

            // Dynamic header label
            if (currentScreen == AuthScreenState.WELCOME) {
                Text(
                    text = "Welcome to Zaxo",
                    color = textColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "The End-to-End Encrypted Soft Soft UI Messenger",
                    color = secondaryText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Auth error display
            authError?.let { errorMsg ->
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    cornerRadius = 12.dp,
                    darkTheme = darkTheme
                ) {
                    Text(
                        text = errorMsg,
                        color = Color(0xFFEC5A4B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Screen Container Transitions
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    slideInHorizontally { width -> if (targetState.ordinal > initialState.ordinal) width else -width } + fadeIn() togetherWith
                    slideOutHorizontally { width -> if (targetState.ordinal > initialState.ordinal) -width else width } + fadeOut()
                },
                label = "auth_screen_content"
            ) { screen ->
                when (screen) {
                    AuthScreenState.WELCOME -> {
                        val context = LocalContext.current
                        WelcomeLayout(
                            darkTheme = darkTheme,
                            textColor = textColor,
                            secondaryText = secondaryText,
                            onGoogleClick = { viewModel.loginWithGoogle(context) },
                            onPnvClick = { currentScreen = AuthScreenState.PNV_TOKEN_LOGIN },
                            onEmailClick = { currentScreen = AuthScreenState.EMAIL_SIGN_IN }
                        )
                    }
                    AuthScreenState.EMAIL_SIGN_IN -> {
                        EmailSignInLayout(
                            darkTheme = darkTheme,
                            textColor = textColor,
                            secondaryText = secondaryText,
                            email = email,
                            onEmailChange = { email = it },
                            password = password,
                            onPasswordChange = { password = it },
                            isPasswordVisible = isPasswordVisible,
                            onPasswordVisibilityToggle = { isPasswordVisible = !isPasswordVisible },
                            rememberMe = rememberMe,
                            onRememberMeChange = { rememberMe = it },
                            onSignIn = { viewModel.loginWithEmail(email, password) },
                            onForgotPasswordClick = { currentScreen = AuthScreenState.FORGOT_PASSWORD },
                            onSignUpClick = { currentScreen = AuthScreenState.EMAIL_SIGN_UP },
                            onZaxoNumberClick = { currentScreen = AuthScreenState.ZAXO_NUMBER_SIGN_IN }
                        )
                    }
                    AuthScreenState.ZAXO_NUMBER_SIGN_IN -> {
                        ZaxoNumberLoginLayout(
                            darkTheme = darkTheme,
                            textColor = textColor,
                            secondaryText = secondaryText,
                            zaxoNumber = zaxoNumberInput,
                            onZaxoNumberChange = { zaxoNumberInput = formatZaxoNumberInput(it) },
                            password = password,
                            onPasswordChange = { password = it },
                            isPasswordVisible = isPasswordVisible,
                            onPasswordVisibilityToggle = { isPasswordVisible = !isPasswordVisible },
                            onLogin = { viewModel.loginWithZaxoNumber(zaxoNumberInput, password) }
                        )
                    }
                    AuthScreenState.EMAIL_SIGN_UP -> {
                        EmailSignUpLayout(
                            darkTheme = darkTheme,
                            textColor = textColor,
                            secondaryText = secondaryText,
                            name = name,
                            onNameChange = { name = it },
                            email = email,
                            onEmailChange = { email = it },
                            phone = phoneNumber,
                            onPhoneChange = { phoneNumber = it },
                            selectedCode = selectedCountryCode,
                            onCodeChange = { selectedCountryCode = it },
                            password = password,
                            onPasswordChange = { password = it },
                            confirmPassword = confirmPassword,
                            onConfirmPasswordChange = { confirmPassword = it },
                            isPasswordVisible = isPasswordVisible,
                            onPasswordVisibilityToggle = { isPasswordVisible = !isPasswordVisible },
                            isConfirmVisible = isConfirmPasswordVisible,
                            onConfirmVisibilityToggle = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                            isTermsAccepted = isTermsAccepted,
                            onTermsChange = { isTermsAccepted = it },
                            onSignUp = {
                                viewModel.startRegistration(email, name, "$selectedCountryCode$phoneNumber", password)
                                useSmsFallback = false
                                viewModel.resetPnvState()
                                currentScreen = AuthScreenState.PHONE_VERIFICATION
                            }
                        )
                    }
                    AuthScreenState.PHONE_VERIFICATION -> {
                        val context = LocalContext.current
                        LaunchedEffect(useSmsFallback) {
                            if (useSmsFallback) {
                                val activity = context as? android.app.Activity
                                if (activity != null) {
                                    viewModel.sendSmsOtp("$selectedCountryCode$phoneNumber", activity)
                                }
                            }
                        }
                        if (!useSmsFallback) {
                            ZaxoPhoneVerifyScreen(
                                viewModel = viewModel,
                                darkTheme = darkTheme,
                                onFallbackToSmsOtp = { useSmsFallback = true }
                            )
                        } else {
                            val cooldownSeconds by viewModel.phoneResendCooldown.collectAsState()
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (darkTheme) Color(0xFF2C1614) else Color(0xFFFFDAD7)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (darkTheme) Color(0xFFFFB4AB) else Color(0xFFBA1A1A),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Carrier retrieval not supported on this device. Using secure SMS OTP fallback verification.",
                                            color = if (darkTheme) Color(0xFFFFDAD7) else Color(0xFF410002),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                PhoneVerificationLayout(
                                    darkTheme = darkTheme,
                                    textColor = textColor,
                                    secondaryText = secondaryText,
                                    phoneNumber = "$selectedCountryCode $phoneNumber",
                                    cooldownSeconds = cooldownSeconds,
                                    onResendClick = { viewModel.startResendTimer(30) },
                                    onVerify = { code ->
                                        viewModel.verifyPhoneCode(code)
                                    }
                                )

                                TextButton(
                                    onClick = {
                                        useSmsFallback = false
                                        viewModel.resetPnvState()
                                    }
                                ) {
                                    Text(
                                        text = "← Back to Carrier Verification",
                                        color = NeuTheme.AccentColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    AuthScreenState.FORGOT_PASSWORD -> {
                        ForgotPasswordLayout(
                            darkTheme = darkTheme,
                            textColor = textColor,
                            secondaryText = secondaryText,
                            email = email,
                            onEmailChange = { email = it },
                            onSendReset = { viewModel.sendPasswordReset(email) }
                        )
                    }
                    AuthScreenState.PNV_TOKEN_LOGIN -> {
                        PnvTokenLoginLayout(
                            darkTheme = darkTheme,
                            textColor = textColor,
                            secondaryText = secondaryText,
                            onBack = { currentScreen = AuthScreenState.WELCOME },
                            onSubmit = { token -> viewModel.loginWithPnvToken(token) },
                            pnvUiState = pnvUiState,
                            pnvError = pnvError
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeToggle(
    darkTheme: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (darkTheme) 180f else 0f,
        animationSpec = tween(500),
        label = "rotation"
    )

    NeumorphicIconButton(
        onClick = onToggle,
        cornerRadius = 14.dp,
        darkTheme = darkTheme,
        modifier = modifier.rotate(rotation)
    ) {
        Icon(
            imageVector = if (darkTheme) Icons.Default.NightsStay else Icons.Default.WbSunny,
            contentDescription = "Toggle Dark Mode",
            tint = NeuTheme.AccentColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun AnimatedLogo(
    shrunk: Boolean,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val entranceScale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entranceScale.animateTo(1.1f, animationSpec = tween(500))
        entranceScale.animateTo(1.0f, animationSpec = tween(200))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "logo_breath")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1250, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val targetSize by animateDpAsState(
        targetValue = if (shrunk) 70.dp else 110.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow),
        label = "logo_size"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(targetSize + 30.dp)
    ) {
        Box(
            modifier = Modifier
                .size(targetSize + 20.dp)
                .background(
                    color = NeuTheme.AccentColor.copy(alpha = glowAlpha),
                    shape = RoundedCornerShape((targetSize + 20.dp) / 2)
                )
        )

        NeumorphicCard(
            modifier = Modifier
                .size(targetSize)
                .scale(entranceScale.value * breathScale),
            cornerRadius = if (shrunk) 18.dp else 26.dp,
            darkTheme = darkTheme
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Zaxo Logo",
                    tint = NeuTheme.AccentColor,
                    modifier = Modifier.size(if (shrunk) 40.dp else 64.dp)
                )
            }
        }
    }
}

@Composable
fun WelcomeLayout(
    darkTheme: Boolean,
    textColor: Color,
    secondaryText: Color,
    onGoogleClick: () -> Unit,
    onPnvClick: () -> Unit,
    onEmailClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        NeumorphicButton(
            onClick = onGoogleClick,
            cornerRadius = 16.dp,
            darkTheme = darkTheme,
            testTag = "google_sign_in_button",
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "G",
                    color = NeuTheme.AccentColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Text(
                    text = "Sign in with Google",
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        NeumorphicButton(
            onClick = onPnvClick,
            cornerRadius = 16.dp,
            darkTheme = darkTheme,
            testTag = "pnv_sign_in_button",
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = NeuTheme.AccentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Sign in with Phone (PNV)",
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(secondaryText.copy(alpha = 0.2f))
            )
            Text(
                text = " or ",
                color = secondaryText,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(secondaryText.copy(alpha = 0.2f))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .neuShadow(cornerRadius = 14.dp, isSunken = false, darkTheme = darkTheme)
                .background(NeuTheme.getBgColor(darkTheme), RoundedCornerShape(14.dp))
                .clickable { onEmailClick() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Sign in with email",
                color = NeuTheme.AccentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "By continuing, you agree to our Terms",
            color = secondaryText.copy(alpha = 0.7f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun EmailSignInLayout(
    darkTheme: Boolean,
    textColor: Color,
    secondaryText: Color,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    rememberMe: Boolean,
    onRememberMeChange: (Boolean) -> Unit,
    onSignIn: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onZaxoNumberClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Sign in with email",
            color = textColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            darkTheme = darkTheme
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NeumorphicInputField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = "Email",
                    icon = Icons.Default.Email,
                    darkTheme = darkTheme,
                    tag = "email_input"
                )

                NeumorphicInputField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = "Password",
                    icon = Icons.Default.Lock,
                    darkTheme = darkTheme,
                    isPassword = true,
                    isPasswordVisible = isPasswordVisible,
                    onPasswordVisibilityToggle = onPasswordVisibilityToggle,
                    tag = "password_input"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onRememberMeChange(!rememberMe) }
                    ) {
                        NeumorphicSwitch(
                            checked = rememberMe,
                            onCheckedChange = onRememberMeChange,
                            darkTheme = darkTheme
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Remember Me", color = secondaryText, fontSize = 13.sp)
                    }

                    Text(
                        text = "Forgot password?",
                        color = NeuTheme.AccentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onForgotPasswordClick() }
                    )
                }
            }
        }

        NeumorphicButton(
            onClick = onSignIn,
            cornerRadius = 16.dp,
            darkTheme = darkTheme,
            testTag = "auth_action_button"
        ) {
            Text(
                text = "Sign in",
                color = NeuTheme.AccentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Don't have an account? ", color = secondaryText, fontSize = 14.sp)
            Text(
                text = "Sign up",
                color = NeuTheme.AccentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onSignUpClick() }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(secondaryText.copy(alpha = 0.2f))
            )
            Text(
                text = " or ",
                color = secondaryText,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(secondaryText.copy(alpha = 0.2f))
            )
        }

        Box(
            modifier = Modifier
                .neuShadow(cornerRadius = 12.dp, isSunken = false, darkTheme = darkTheme)
                .background(NeuTheme.getBgColor(darkTheme), RoundedCornerShape(12.dp))
                .clickable { onZaxoNumberClick() }
                .padding(vertical = 12.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Login with Zaxo Number",
                color = NeuTheme.AccentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun ZaxoNumberLoginLayout(
    darkTheme: Boolean,
    textColor: Color,
    secondaryText: Color,
    zaxoNumber: String,
    onZaxoNumberChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    onLogin: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Login with Zaxo Number",
            color = textColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            darkTheme = darkTheme
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NeumorphicInputField(
                    value = zaxoNumber,
                    onValueChange = onZaxoNumberChange,
                    label = "Zaxo Number",
                    icon = Icons.Default.Tag,
                    darkTheme = darkTheme,
                    tag = "zaxo_number_input"
                )

                NeumorphicInputField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = "Password",
                    icon = Icons.Default.Lock,
                    darkTheme = darkTheme,
                    isPassword = true,
                    isPasswordVisible = isPasswordVisible,
                    onPasswordVisibilityToggle = onPasswordVisibilityToggle,
                    tag = "zaxo_password_input"
                )
            }
        }

        NeumorphicButton(
            onClick = onLogin,
            cornerRadius = 16.dp,
            darkTheme = darkTheme,
            testTag = "zaxo_login_button"
        ) {
            Text(
                text = "Login",
                color = NeuTheme.AccentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Note: Zaxo Number login is only for registered users. If you don't have an account, please go back and Sign Up with Email first.",
            color = secondaryText.copy(alpha = 0.8f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun EmailSignUpLayout(
    darkTheme: Boolean,
    textColor: Color,
    secondaryText: Color,
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    selectedCode: String,
    onCodeChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    isConfirmVisible: Boolean,
    onConfirmVisibilityToggle: () -> Unit,
    isTermsAccepted: Boolean,
    onTermsChange: (Boolean) -> Unit,
    onSignUp: () -> Unit
) {
    val strengthInfo = calculatePasswordStrength(password)
    val strengthColor = getPasswordStrengthColor(strengthInfo.second)
    val isAllValid = name.isNotBlank() &&
            email.contains("@") &&
            phone.replace(Regex("[^0-9]"), "").length >= 8 &&
            password.length >= 6 &&
            password == confirmPassword &&
            isTermsAccepted

    val countryCodes = listOf("+1", "+44", "+91", "+49", "+33", "+81", "+86", "+61", "+971")
    var expandedDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Sign up",
                color = textColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Join Zaxo",
                color = secondaryText,
                fontSize = 13.sp
            )
        }

        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            darkTheme = darkTheme
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                NeumorphicInputField(
                    value = name,
                    onValueChange = onNameChange,
                    label = "Name",
                    icon = Icons.Default.Person,
                    darkTheme = darkTheme,
                    tag = "signup_name_input"
                )

                NeumorphicInputField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = "Email",
                    icon = Icons.Default.Email,
                    darkTheme = darkTheme,
                    tag = "signup_email_input"
                )

                // Mandatory Phone Input Row
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Phone Number (Mandatory)",
                        color = secondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Dropdown Country Selection Box
                        Box(
                            modifier = Modifier
                                .width(84.dp)
                                .height(54.dp)
                                .neuShadow(cornerRadius = 12.dp, isSunken = true, darkTheme = darkTheme)
                                .clickable { expandedDropdown = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = selectedCode,
                                    color = textColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = NeuTheme.AccentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false },
                                modifier = Modifier.background(NeuTheme.getBgColor(darkTheme))
                            ) {
                                countryCodes.forEach { code ->
                                    DropdownMenuItem(
                                        text = { Text(text = code, color = textColor) },
                                        onClick = {
                                            onCodeChange(code)
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Input field
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .neuShadow(cornerRadius = 12.dp, isSunken = true, darkTheme = darkTheme)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = NeuTheme.AccentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (phone.isEmpty()) {
                                        Text(
                                            text = "Enter Phone...",
                                            color = secondaryText.copy(alpha = 0.5f),
                                            fontSize = 15.sp
                                        )
                                    }
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = phone,
                                        onValueChange = { onPhoneChange(it.replace(Regex("[^0-9]"), "").take(10)) },
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(color = textColor, fontSize = 15.sp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        modifier = Modifier.fillMaxWidth().testTag("signup_phone_input")
                                    )
                                }
                            }
                        }
                    }
                }

                NeumorphicInputField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = "Password",
                    icon = Icons.Default.Lock,
                    darkTheme = darkTheme,
                    isPassword = true,
                    isPasswordVisible = isPasswordVisible,
                    onPasswordVisibilityToggle = onPasswordVisibilityToggle,
                    tag = "signup_password_input"
                )

                // Password strength indicator
                if (password.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Password strength:",
                                color = secondaryText,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${getPasswordStrengthText(strengthInfo.second)} (${(strengthInfo.second * 100).toInt()}%)",
                                color = strengthColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(secondaryText.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(strengthInfo.second)
                                    .height(6.dp)
                                    .background(strengthColor, RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }

                NeumorphicInputField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = "Confirm password",
                    icon = Icons.Default.Lock,
                    darkTheme = darkTheme,
                    isPassword = true,
                    isPasswordVisible = isConfirmVisible,
                    onPasswordVisibilityToggle = onConfirmVisibilityToggle,
                    tag = "signup_confirm_password_input"
                )

                // Passwords match validation
                if (confirmPassword.isNotEmpty()) {
                    Text(
                        text = if (password == confirmPassword) "Passwords match ✓" else "Passwords don't match",
                        color = if (password == confirmPassword) Color(0xFF2ECC71) else Color(0xFFEC5A4B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                // Terms Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeumorphicSwitch(
                        checked = isTermsAccepted,
                        onCheckedChange = onTermsChange,
                        darkTheme = darkTheme
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "I agree to the Terms and Privacy Policy",
                        color = secondaryText,
                        fontSize = 12.sp
                    )
                }
            }
        }

        NeumorphicButton(
            onClick = onSignUp,
            cornerRadius = 16.dp,
            darkTheme = darkTheme,
            testTag = "signup_submit_button",
            modifier = Modifier.alpha(if (isAllValid) 1f else 0.5f)
        ) {
            Text(
                text = "Sign up",
                color = if (isAllValid) NeuTheme.AccentColor else secondaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun PhoneVerificationLayout(
    darkTheme: Boolean,
    textColor: Color,
    secondaryText: Color,
    phoneNumber: String,
    cooldownSeconds: Int,
    onResendClick: () -> Unit,
    onVerify: (String) -> Unit
) {
    var enteredCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Verify Your Phone",
            color = textColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "We've sent an SMS verification code to $phoneNumber.",
            color = secondaryText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            darkTheme = darkTheme
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PinEntryFields(
                    darkTheme = darkTheme,
                    onPinEntered = { enteredCode = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Didn't receive SMS?",
                        color = secondaryText,
                        fontSize = 12.sp
                    )

                    if (cooldownSeconds > 0) {
                        Text(
                            text = "Resend SMS in ${cooldownSeconds}s",
                            color = secondaryText.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Resend SMS",
                            color = NeuTheme.AccentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onResendClick() }
                        )
                    }
                }
            }
        }

        NeumorphicButton(
            onClick = { onVerify(enteredCode) },
            cornerRadius = 16.dp,
            darkTheme = darkTheme,
            testTag = "phone_verify_button"
        ) {
            Text(
                text = "Verify",
                color = NeuTheme.AccentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun PinEntryFields(
    darkTheme: Boolean,
    onPinEntered: (String) -> Unit
) {
    val pinLength = 6
    val pinValues = remember { mutableStateListOf("", "", "", "", "", "") }
    val focusRequesters = remember { List(pinLength) { FocusRequester() } }
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        for (i in 0 until pinLength) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .neuShadow(cornerRadius = 10.dp, isSunken = true, darkTheme = darkTheme)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = pinValues[i],
                    onValueChange = { value ->
                        val digitsOnly = value.replace(Regex("[^0-9]"), "")
                        if (digitsOnly.length <= 1) {
                            pinValues[i] = digitsOnly
                            if (digitsOnly.isNotEmpty()) {
                                if (i < pinLength - 1) {
                                    focusRequesters[i + 1].requestFocus()
                                } else {
                                    keyboardController?.hide()
                                }
                            }
                            val fullPin = pinValues.joinToString("")
                            if (fullPin.length == pinLength) {
                                onPinEntered(fullPin)
                            }
                        }
                    },
                    modifier = Modifier
                        .focusRequester(focusRequesters[i])
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Backspace) {
                                if (pinValues[i].isEmpty() && i > 0) {
                                    focusRequesters[i - 1].requestFocus()
                                    pinValues[i - 1] = ""
                                    true
                                } else {
                                    false
                                }
                            } else {
                                false
                            }
                        }
                        .testTag("pin_field_$i"),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        color = NeuTheme.getTextColor(darkTheme),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            LaunchedEffect(Unit) {
                if (i == 0) {
                    focusRequesters[0].requestFocus()
                }
            }
        }
    }
}

@Composable
fun PnvTokenLoginLayout(
    darkTheme: Boolean,
    textColor: Color,
    secondaryText: Color,
    onBack: () -> Unit,
    onSubmit: (String) -> Unit,
    pnvUiState: com.zaxo.ui.viewmodel.PnvUiState,
    pnvError: String?
) {
    var tokenInput by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        NeumorphicCard(
            modifier = Modifier.size(64.dp),
            cornerRadius = 32.dp,
            darkTheme = darkTheme
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = NeuTheme.AccentColor,
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = "Phone SIM Verification",
            color = textColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Paste your carrier-provided PNV token below to sign in instantly.",
            color = secondaryText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (pnvError != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (darkTheme) Color(0xFF2C1614) else Color(0xFFFFDAD7)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Text(
                    text = pnvError,
                    color = if (darkTheme) Color(0xFFFFDAD7) else Color(0xFFBA1A1A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (pnvUiState == com.zaxo.ui.viewmodel.PnvUiState.EXCHANGING_TOKEN) {
            StatusIndicatorScreen(
                title = "Securing Session",
                subtitle = "Exchanging carrier verification signatures with Zaxo gateway...",
                icon = Icons.Default.Refresh,
                isLoading = true,
                darkTheme = darkTheme
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(110.dp)
                    .neuShadow(cornerRadius = 14.dp, isSunken = true, darkTheme = darkTheme)
                    .background(NeuTheme.getBgColor(darkTheme), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    textStyle = LocalTextStyle.current.copy(
                        color = textColor,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.fillMaxSize(),
                    decorationBox = { innerTextField ->
                        if (tokenInput.isEmpty()) {
                            Text(
                                text = "Paste PNV token here...",
                                color = secondaryText.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            NeumorphicButton(
                onClick = { if (tokenInput.trim().isNotEmpty()) onSubmit(tokenInput.trim()) },
                cornerRadius = 16.dp,
                darkTheme = darkTheme,
                testTag = "pnv_token_submit_button",
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text(
                    text = "Authenticate PNV Line",
                    color = NeuTheme.AccentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            TextButton(onClick = onBack) {
                Text(
                    text = "← Back to Welcome Screen",
                    color = NeuTheme.AccentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ForgotPasswordLayout(
    darkTheme: Boolean,
    textColor: Color,
    secondaryText: Color,
    email: String,
    onEmailChange: (String) -> Unit,
    onSendReset: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Reset password",
            color = textColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Enter your registered email address below, and we will send you a password reset link.",
            color = secondaryText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            darkTheme = darkTheme
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NeumorphicInputField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = "Email",
                    icon = Icons.Default.Email,
                    darkTheme = darkTheme,
                    tag = "reset_email_input"
                )
            }
        }

        NeumorphicButton(
            onClick = onSendReset,
            cornerRadius = 16.dp,
            darkTheme = darkTheme,
            testTag = "reset_submit_button"
        ) {
            Text(
                text = "Send reset link",
                color = NeuTheme.AccentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun NeumorphicInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    darkTheme: Boolean,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordVisibilityToggle: (() -> Unit)? = null,
    tag: String = ""
) {
    val textColor = NeuTheme.getTextColor(darkTheme)
    val labelColor = NeuTheme.getSecondaryTextColor(darkTheme)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Text(
            text = label,
            color = labelColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .neuShadow(cornerRadius = 12.dp, isSunken = true, darkTheme = darkTheme)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeuTheme.AccentColor,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "Enter $label...",
                            color = labelColor.copy(alpha = 0.5f),
                            fontSize = 15.sp
                        )
                    }

                    androidx.compose.foundation.text.BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            color = textColor,
                            fontSize = 15.sp
                        ),
                        visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isPassword && onPasswordVisibilityToggle != null) {
                    IconButton(onClick = onPasswordVisibilityToggle) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility",
                            tint = labelColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// Formatting functions
fun formatZaxoNumberInput(input: String): String {
    val digits = input.replace(Regex("[^0-9]"), "").take(9)
    return buildString {
        for (i in digits.indices) {
            append(digits[i])
            if ((i == 2 || i == 5) && i != digits.lastIndex) {
                append(" ")
            }
        }
    }
}

fun calculatePasswordStrength(password: String): Pair<PasswordStrength, Float> {
    if (password.length < 4) return Pair(PasswordStrength.WEAK, 0.2f)
    var score = 0
    if (password.length >= 6) score += 1
    if (password.length >= 10) score += 1
    if (password.any { it.isUpperCase() }) score += 1
    if (password.any { it.isLowerCase() }) score += 1
    if (password.any { it.isDigit() }) score += 1
    if (password.any { !it.isLetterOrDigit() }) score += 1

    val pct = (score.toFloat() / 6f).coerceIn(0.1f, 1f)
    val strength = when {
        score <= 2 -> PasswordStrength.WEAK
        score <= 4 -> PasswordStrength.MEDIUM
        score <= 5 -> PasswordStrength.STRONG
        else -> PasswordStrength.VERY_STRONG
    }
    return Pair(strength, pct)
}

fun getPasswordStrengthColor(score: Float): Color {
    return when {
        score <= 0.35f -> Color(0xFFEC5A4B) // Red
        score <= 0.7f -> Color(0xFFF39C12)  // Amber
        else -> Color(0xFF2ECC71)           // Green
    }
}

fun getPasswordStrengthText(score: Float): String {
    return when {
        score <= 0.35f -> "Weak"
        score <= 0.7f -> "Medium"
        score <= 0.9f -> "Strong"
        else -> "Very Strong"
    }
}
