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
import kotlinx.coroutines.delay

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
    if (activity.isFinishing || activity.isDestroyed) return
    val biometricManager = BiometricManager.from(activity)
    val canAuth = biometricManager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.BIOMETRIC_WEAK
    )
    if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) { onError(); return }

    val executor = ContextCompat.getMainExecutor(activity)
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            try {
                if (!activity.isFinishing && !activity.isDestroyed) onSuccess()
            } catch (e: Exception) { /* ignore */ }
        }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            try {
                if (!activity.isFinishing && !activity.isDestroyed) onError()
            } catch (e: Exception) { /* ignore */ }
        }
        override fun onAuthenticationFailed() {}
    }
    try {
        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("InvestTrack")
            .setSubtitle("Use biometric to unlock")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()
        prompt.authenticate(info)
    } catch (e: Exception) { onError() }
}

@Composable
fun PinLockScreen(
    title: String = "Enter PIN",
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

    // Auto-trigger biometric on open
    LaunchedEffect(Unit) {
        if (biometricEnabled && !isSetupMode) {
            delay(300) // small delay so screen is visible first
            triggerBiometric(activity, onSuccess = onSuccess)
        }
    }

    fun handleKey(key: String) {
        when (key) {
            "del" -> { if (pin.isNotEmpty()) { pin = pin.dropLast(1); errorMsg = "" } }
            "bio" -> triggerBiometric(activity, onSuccess = onSuccess)
            else  -> {
                if (pin.length >= 4) return
                pin += key
                errorMsg = ""
                if (pin.length == 4) {
                    if (isSetupMode) {
                        if (!isConfirming) {
                            confirmPin = pin; isConfirming = true; pin = ""
                        } else {
                            if (pin == confirmPin) onSetupPin!!(hashPin(pin))
                            else { errorMsg = "PINs don't match. Try again."; pin = ""; confirmPin = ""; isConfirming = false }
                        }
                    } else {
                        if (hashPin(pin) == storedPinHash) onSuccess()
                        else { errorMsg = "Wrong PIN. Try again."; pin = "" }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface.copy(0.5f)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Icon
            Box(modifier = Modifier.size(72.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }

            // Title
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("InvestTrack", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Text(
                    when { isSetupMode && isConfirming -> "Confirm your PIN"; isSetupMode -> "Choose a 4-digit PIN"; else -> "Enter PIN to unlock" },
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { i ->
                    val filled = i < pin.length
                    val scale by animateFloatAsState(if (filled) 1.2f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "dot$i")
                    Box(modifier = Modifier.size(14.dp).scale(scale).clip(CircleShape)
                        .background(if (filled) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .border(1.5.dp, if (filled) Color.Transparent else MaterialTheme.colorScheme.outline, CircleShape)
                    )
                }
            }

            // Error
            if (errorMsg.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
                    Text(errorMsg, color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                }
            }

            // Keypad
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("bio","0","del")).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { key ->
                            val showBio = key == "bio" && biometricEnabled && !isSetupMode
                            val isVisible = key != "bio" || showBio
                            Box(
                                modifier = Modifier.size(68.dp).clip(CircleShape)
                                    .background(if (isVisible) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                    .then(if (isVisible) Modifier.clickable { handleKey(key) } else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    key == "del"  -> Icon(Icons.Default.Backspace, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                                    showBio       -> Icon(Icons.Default.Fingerprint, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                    isVisible     -> Text(key, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
