package com.investtrack.ui.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

fun hashPin(pin: String): String {
    var h = 5381L
    pin.forEach { h = h * 33 + it.code }
    return h.toString(16)
}

fun triggerBiometric(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: () -> Unit = {}
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricManager = BiometricManager.from(activity)
    val canAuthenticate = biometricManager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.BIOMETRIC_WEAK
    )
    if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
        onError()
        return
    }
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onSuccess()
        }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            onError()
        }
        override fun onAuthenticationFailed() {}
    }
    val prompt = BiometricPrompt(activity, executor, callback)
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("InvestTrack")
        .setSubtitle("Authenticate to access your portfolio")
        .setNegativeButtonText("Use PIN")
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        .build()
    prompt.authenticate(promptInfo)
}

@Composable
fun PinLockScreen(
    title: String = "Enter PIN",
    subtitle: String = "Enter your PIN to access InvestTrack",
    storedPinHash: String?,
    biometricEnabled: Boolean = false,
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onSetupPin: ((String) -> Unit)? = null
) {
    val isSetupMode = onSetupPin != null && storedPinHash == null
    var pin          by remember { mutableStateOf("") }
    var confirmPin   by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    var errorMsg     by remember { mutableStateOf("") }
    var attempts     by remember { mutableStateOf(0) }

    // Shake animation on error
    val shakeOffset by animateFloatAsState(
        targetValue = if (errorMsg.isNotEmpty()) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "shake"
    )

    // Auto-trigger biometric on first load
    LaunchedEffect(Unit) {
        if (biometricEnabled && !isSetupMode) {
            triggerBiometric(activity, onSuccess = onSuccess)
        }
    }

    fun handleDigit(d: String) {
        if (pin.length >= 4) return
        pin += d
        errorMsg = ""

        if (pin.length == 4) {
            if (isSetupMode) {
                if (!isConfirming) {
                    confirmPin   = pin
                    isConfirming = true
                    pin          = ""
                } else {
                    if (pin == confirmPin) {
                        onSetupPin!!(hashPin(pin))
                    } else {
                        errorMsg     = "PINs don't match. Try again."
                        pin          = ""
                        confirmPin   = ""
                        isConfirming = false
                    }
                }
            } else {
                if (hashPin(pin) == storedPinHash) {
                    onSuccess()
                } else {
                    attempts++
                    errorMsg = if (attempts >= 3) "Too many attempts. Try again." else "Wrong PIN"
                    pin = ""
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(0.6f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Lock icon with glow
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            }

            // App name + instructions
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("InvestTrack", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Text(
                    when {
                        isSetupMode && isConfirming -> "Confirm your PIN"
                        isSetupMode                 -> "Choose a 4-digit PIN"
                        else                        -> subtitle
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // PIN dots with animation
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { i ->
                    val filled = i < pin.length
                    val scale by animateFloatAsState(
                        targetValue = if (filled) 1.2f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "dot_$i"
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(
                                if (filled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (filled) 0.dp else 1.5.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                    )
                }
            }

            // Error message
            if (errorMsg.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
                    Text(errorMsg, color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                }
            }

            // Number pad
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val rows = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("bio","0","del"))
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { key ->
                            KeypadButton(
                                key = key,
                                showBiometric = key == "bio" && biometricEnabled && !isSetupMode,
                                onClick = {
                                    when (key) {
                                        "del" -> if (pin.isNotEmpty()) { pin = pin.dropLast(1); errorMsg = "" }
                                        "bio" -> triggerBiometric(activity, onSuccess = onSuccess)
                                        else  -> handleDigit(key)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(key: String, showBiometric: Boolean, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "scale")

    LaunchedEffect(pressed) {
        if (pressed) { kotlinx.coroutines.delay(80); pressed = false }
    }

    val isVisible = key != "bio" || showBiometric

    Box(
        modifier = Modifier
            .size(70.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (!isVisible) Color.Transparent
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .then(if (isVisible) Modifier.clickable { pressed = true; onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        when {
            key == "del" -> Icon(Icons.Default.Backspace, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
            showBiometric && key == "bio" -> Icon(Icons.Default.Fingerprint, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            key.isNotEmpty() && key != "bio" -> Text(key, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
