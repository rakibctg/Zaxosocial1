package com.zaxo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaxo.ui.neumorphic.*
import com.zaxo.ui.viewmodel.MainViewModel
import com.zaxo.ui.viewmodel.PnvUiState
import kotlinx.coroutines.delay

@Composable
fun ZaxoPhoneVerifyScreen(
    viewModel: MainViewModel,
    darkTheme: Boolean,
    onFallbackToSmsOtp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pnvUiState by viewModel.pnvUiState.collectAsState()
    val pnvError by viewModel.pnvError.collectAsState()
    val verifiedPhoneNumber by viewModel.verifiedPhoneNumber.collectAsState()

    val textColor = NeuTheme.getTextColor(darkTheme)
    val secondaryText = NeuTheme.getSecondaryTextColor(darkTheme)

    // Trigger check compatibility automatically on first entry if in IDLE state
    LaunchedEffect(pnvUiState) {
        if (pnvUiState == PnvUiState.IDLE) {
            viewModel.checkPnvCompatibilityAndInitiate()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            when (pnvUiState) {
                PnvUiState.IDLE, PnvUiState.CHECKING_COMPATIBILITY -> {
                    StatusIndicatorScreen(
                        title = "Checking Connection...",
                        subtitle = "We are checking if your carrier and device support single-tap secure verification.",
                        icon = Icons.Default.Refresh,
                        isLoading = true,
                        darkTheme = darkTheme
                    )
                }

                PnvUiState.UNSUPPORTED_FALLBACK -> {
                    LaunchedEffect(Unit) {
                        onFallbackToSmsOtp()
                    }
                }

                PnvUiState.EXPLAINER -> {
                    PnvExplainerLayout(
                        textColor = textColor,
                        secondaryText = secondaryText,
                        darkTheme = darkTheme,
                        onVerifyCarrier = { viewModel.startPnvCarrierVerification() },
                        onVerifySms = onFallbackToSmsOtp
                    )
                }

                PnvUiState.PROMPTING_CARRIER -> {
                    StatusIndicatorScreen(
                        title = "Verifying with Carrier",
                        subtitle = "Connecting to your mobile operator to confirm SIM security details...",
                        icon = Icons.Default.Phone,
                        isLoading = true,
                        darkTheme = darkTheme
                    )
                }

                PnvUiState.EXCHANGING_TOKEN -> {
                    StatusIndicatorScreen(
                        title = "Securing Session",
                        subtitle = "Exchanging carrier verification signatures with Zaxo authentication gateway...",
                        icon = Icons.Default.Lock,
                        isLoading = true,
                        darkTheme = darkTheme
                    )
                }

                PnvUiState.VERIFIED_SUCCESS -> {
                    StatusIndicatorScreen(
                        title = "Verification Complete!",
                        subtitle = "Successfully authenticated ${verifiedPhoneNumber ?: "your line"} via modern carrier-based retrieval.",
                        icon = Icons.Default.CheckCircle,
                        iconColor = Color(0xFF2ECC71),
                        isLoading = false,
                        darkTheme = darkTheme
                    )
                }

                PnvUiState.ERROR -> {
                    PnvErrorLayout(
                        errorMsg = pnvError ?: "An unexpected error occurred during carrier verification.",
                        textColor = textColor,
                        secondaryText = secondaryText,
                        darkTheme = darkTheme,
                        onRetry = { viewModel.startPnvCarrierVerification() },
                        onVerifySms = onFallbackToSmsOtp
                    )
                }
            }
        }
    }
}

@Composable
fun StatusIndicatorScreen(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isLoading: Boolean,
    darkTheme: Boolean,
    iconColor: Color = NeuTheme.AccentColor
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        NeumorphicCard(
            modifier = Modifier.size(96.dp),
            cornerRadius = 48.dp,
            darkTheme = darkTheme
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .size(40.dp)
                    .then(if (isLoading) Modifier.scale(1.1f) else Modifier)
            )
        }

        Text(
            text = title,
            color = NeuTheme.getTextColor(darkTheme),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = subtitle,
            color = NeuTheme.getSecondaryTextColor(darkTheme),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(
                color = NeuTheme.AccentColor,
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun PnvExplainerLayout(
    textColor: Color,
    secondaryText: Color,
    darkTheme: Boolean,
    onVerifyCarrier: () -> Unit,
    onVerifySms: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App header / Shield Icon
        NeumorphicCard(
            modifier = Modifier.size(72.dp),
            cornerRadius = 36.dp,
            darkTheme = darkTheme
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = NeuTheme.AccentColor,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = "Single-Tap Verification",
            color = textColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Secure your Zaxo account instantly using modern carrier-based SIM verification.",
            color = secondaryText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Benefits Cards
        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            darkTheme = darkTheme
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BenefitItem(
                    icon = Icons.Default.Star,
                    title = "No SMS OTP code required",
                    desc = "Directly extracts and verifies cellular tokens from your SIM card.",
                    darkTheme = darkTheme
                )
                BenefitItem(
                    icon = Icons.Default.Shield,
                    title = "Carrier-grade security",
                    desc = "Protects against modern SIM swapping, phishing, and intercept attacks.",
                    darkTheme = darkTheme
                )
                BenefitItem(
                    icon = Icons.Default.Done,
                    title = "Verified in seconds",
                    desc = "A single-tap verification flow that connects directly with your cellular provider.",
                    darkTheme = darkTheme
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Actions
        NeumorphicButton(
            onClick = onVerifyCarrier,
            cornerRadius = 16.dp,
            darkTheme = darkTheme,
            testTag = "pnv_carrier_button"
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = NeuTheme.AccentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Verify with Carrier SIM",
                    color = NeuTheme.AccentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        TextButton(
            onClick = onVerifySms,
            modifier = Modifier.testTag("pnv_sms_fallback_button")
        ) {
            Text(
                text = "Use Standard SMS OTP instead",
                color = NeuTheme.AccentColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun BenefitItem(
    icon: ImageVector,
    title: String,
    desc: String,
    darkTheme: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NeumorphicCard(
            modifier = Modifier.size(36.dp),
            cornerRadius = 18.dp,
            darkTheme = darkTheme
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeuTheme.AccentColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = NeuTheme.getTextColor(darkTheme),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = desc,
                color = NeuTheme.getSecondaryTextColor(darkTheme),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun PnvErrorLayout(
    errorMsg: String,
    textColor: Color,
    secondaryText: Color,
    darkTheme: Boolean,
    onRetry: () -> Unit,
    onVerifySms: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        NeumorphicCard(
            modifier = Modifier.size(72.dp),
            cornerRadius = 36.dp,
            darkTheme = darkTheme
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFEC5A4B),
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = "Verification Failed",
            color = textColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = errorMsg,
            color = Color(0xFFEC5A4B),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        NeumorphicButton(
            onClick = onRetry,
            cornerRadius = 16.dp,
            darkTheme = darkTheme,
            testTag = "pnv_retry_button"
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = NeuTheme.AccentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Try Carrier Verify Again",
                    color = NeuTheme.AccentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        TextButton(
            onClick = onVerifySms,
            modifier = Modifier.testTag("pnv_error_sms_fallback")
        ) {
            Text(
                text = "Fallback to SMS OTP",
                color = NeuTheme.AccentColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}
