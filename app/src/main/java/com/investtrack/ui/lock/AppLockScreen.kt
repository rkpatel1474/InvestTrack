package com.investtrack.ui.lock

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.investtrack.data.preferences.LockType

fun hashPin(pin: String): String {
    var hash = 0L
    pin.forEach { hash = hash * 31 + it.code }
    return hash.toString(16)
}

@Composable
fun PinLockScreen(
    title: String = "Enter PIN",
    subtitle: String = "Enter your 4-digit PIN to continue",
    lockType: LockType = LockType.PIN,
    storedPinHash: String?,
    biometricEnabled: Boolean = false,
    onSuccess: () -> Unit,
    onSetupPin: ((String) -> Unit)? = null  // non-null = setup mode
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var shakeKey by remember { mutableStateOf(0) }

    val isSetupMode = onSetupPin != null && storedPinHash == null

    // Biometric prompt
    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled && !isSetupMode && context is FragmentActivity) {
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(context, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onSuccess()
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        // fallback to PIN
                    }
                }
            )
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("InvestTrack")
                .setSubtitle("Authenticate to continue")
                .setNegativeButtonText("Use PIN")
                .build()
            biometricPrompt.authenticate(promptInfo)
        }
    }

    fun handleDigit(d: String) {
        if (pin.length >= 4) return
        pin += d
        errorMessage = ""

        if (pin.length == 4) {
            if (isSetupMode) {
                if (!isConfirming) {
                    isConfirming = true
                    confirmPin = pin
                    pin = ""
                } else {
                    if (pin == confirmPin) {
                        onSetupPin!!(hashPin(pin))
                    } else {
                        errorMessage = "PINs do not match. Try again."
                        pin = ""
                        confirmPin = ""
                        isConfirming = false
                        shakeKey++
                    }
                }
            } else {
                if (hashPin(pin) == storedPinHash) {
                    onSuccess()
                } else {
                    errorMessage = "Incorrect PIN"
                    pin = ""
                    shakeKey++
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Logo / App name
            Text(
                "InvestTrack",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (isSetupMode && isConfirming) "Confirm PIN" else if (isSetupMode) "Set PIN" else title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    if (isSetupMode && isConfirming) "Re-enter your PIN to confirm"
                    else if (isSetupMode) "Choose a 4-digit PIN"
                    else subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // PIN dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { i ->
                    val filled = i < pin.length
                    val dotColor by animateColorAsState(
                        if (filled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        label = "dot"
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                            .border(
                                width = if (filled) 0.dp else 2.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                    )
                }
            }

            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            // Keypad
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val rows = listOf(
                    listOf("1","2","3"),
                    listOf("4","5","6"),
                    listOf("7","8","9"),
                    listOf("","0","⌫")
                )
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        row.forEach { key ->
                            PinKey(
                                label = key,
                                showBiometric = key == "" && biometricEnabled && !isSetupMode,
                                onClick = {
                                    when (key) {
                                        "⌫" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                        "" -> { /* biometric handled separately */ }
                                        else -> handleDigit(key)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Biometric button
            if (biometricEnabled && !isSetupMode) {
                OutlinedButton(
                    onClick = {
                        if (context is FragmentActivity) {
                            val executor = ContextCompat.getMainExecutor(context)
                            val biometricPrompt = BiometricPrompt(context, executor,
                                object : BiometricPrompt.AuthenticationCallback() {
                                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                        onSuccess()
                                    }
                                }
                            )
                            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                .setTitle("InvestTrack")
                                .setSubtitle("Authenticate to continue")
                                .setNegativeButtonText("Use PIN")
                                .build()
                            biometricPrompt.authenticate(promptInfo)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Fingerprint, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Use Biometric")
                }
            }
        }
    }
}

@Composable
fun PinKey(label: String, showBiometric: Boolean, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(), label = "scale")

    Box(
        modifier = Modifier
            .size(72.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (label.isEmpty() && !showBiometric) Color.Transparent
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable {
                pressed = true
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (label == "⌫") {
            Icon(Icons.Default.Backspace, "Delete", tint = MaterialTheme.colorScheme.onSurface)
        } else if (showBiometric) {
            Icon(Icons.Default.Fingerprint, "Biometric", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        } else if (label.isNotEmpty()) {
            Text(label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }

    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(100)
            pressed = false
        }
    }
}
