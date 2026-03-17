package com.investtrack.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investtrack.data.preferences.LockType
import com.investtrack.data.preferences.PreferencesManager
import com.investtrack.ui.common.AppDimens
import com.investtrack.ui.common.TopBarWithBack
import com.investtrack.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesManager
) : ViewModel() {
    val currentTheme = prefs.appTheme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.DARK)
    val lockEnabled = prefs.lockEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val lockType = prefs.lockType.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LockType.PIN)
    val biometricEnabled = prefs.biometricEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val hideAmounts = prefs.hideAmounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoLockMins = prefs.autoLockMins.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)
    val pinHash = prefs.pinHash.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setTheme(theme: AppTheme) = viewModelScope.launch { prefs.setTheme(theme) }
    fun setLockEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setLockEnabled(enabled) }
    fun setLockType(type: LockType) = viewModelScope.launch { prefs.setLockType(type) }
    fun setPinHash(hash: String) = viewModelScope.launch { prefs.setPinHash(hash) }
    fun setBiometricEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setBiometricEnabled(enabled) }
    fun setHideAmounts(hide: Boolean) = viewModelScope.launch { prefs.setHideAmounts(hide) }
    fun setAutoLockMins(mins: Int) = viewModelScope.launch { prefs.setAutoLockMins(mins) }
    fun clearLock() = viewModelScope.launch { prefs.clearLock() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSetupPin: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val currentTheme by vm.currentTheme.collectAsState()
    val lockEnabled by vm.lockEnabled.collectAsState()
    val lockType by vm.lockType.collectAsState()
    val biometricEnabled by vm.biometricEnabled.collectAsState()
    val hideAmounts by vm.hideAmounts.collectAsState()
    val autoLockMins by vm.autoLockMins.collectAsState()
    val pinHash by vm.pinHash.collectAsState()

    var showThemePicker by remember { mutableStateOf(false) }
    var showAutoLockPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopBarWithBack("Settings", onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(AppDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ─── Appearance ──────────────────────────────────────────────────
            item { SettingsSectionHeader("Appearance") }
            item {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.Palette,
                        iconColor = MaterialTheme.colorScheme.primary,
                        title = "Theme",
                        subtitle = currentTheme.displayName,
                        onClick = { showThemePicker = true }
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        icon = Icons.Default.VisibilityOff,
                        iconColor = MaterialTheme.colorScheme.secondary,
                        title = "Hide Amounts",
                        subtitle = "Mask sensitive numbers",
                        checked = hideAmounts,
                        onCheckedChange = { vm.setHideAmounts(it) }
                    )
                }
            }

            // ─── Security ────────────────────────────────────────────────────
            item { SettingsSectionHeader("Security") }
            item {
                SettingsCard {
                    SettingsToggleRow(
                        icon = Icons.Default.Lock,
                        iconColor = Color(0xFFFF6B6B),
                        title = "App Lock",
                        subtitle = "Require authentication on open",
                        checked = lockEnabled,
                        onCheckedChange = {
                            if (it && pinHash == null) {
                                onSetupPin()
                            } else {
                                vm.setLockEnabled(it)
                                if (!it) vm.clearLock()
                            }
                        }
                    )
                    if (lockEnabled) {
                        SettingsDivider()
                        SettingsToggleRow(
                            icon = Icons.Default.Fingerprint,
                            iconColor = Color(0xFF4CAF50),
                            title = "Biometric Auth",
                            subtitle = "Use fingerprint / face unlock",
                            checked = biometricEnabled,
                            onCheckedChange = { vm.setBiometricEnabled(it) }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Timer,
                            iconColor = MaterialTheme.colorScheme.tertiary,
                            title = "Auto-lock",
                            subtitle = "After $autoLockMins minutes",
                            onClick = { showAutoLockPicker = true }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Password,
                            iconColor = MaterialTheme.colorScheme.secondary,
                            title = "Change PIN",
                            subtitle = "Update your security PIN",
                            onClick = onSetupPin
                        )
                    }
                }
            }

            // ─── About ───────────────────────────────────────────────────────
            item { SettingsSectionHeader("About") }
            item {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.Info,
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        title = "Version",
                        subtitle = "InvestTrack 2.0"
                    )
                }
            }
        }
    }

    // Theme Picker Dialog
    if (showThemePicker) {
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            title = { Text("Choose Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppTheme.values().forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { vm.setTheme(theme); showThemePicker = false }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                ThemeColorDot(theme)
                                Text(theme.displayName, style = MaterialTheme.typography.bodyLarge)
                            }
                            if (theme == currentTheme) {
                                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemePicker = false }) { Text("Close") } }
        )
    }

    // Auto-lock Picker
    if (showAutoLockPicker) {
        val options = listOf(1, 2, 5, 10, 15, 30)
        AlertDialog(
            onDismissRequest = { showAutoLockPicker = false },
            title = { Text("Auto-lock After", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    options.forEach { mins ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { vm.setAutoLockMins(mins); showAutoLockPicker = false }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("$mins minutes")
                            if (mins == autoLockMins) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAutoLockPicker = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun ThemeColorDot(theme: AppTheme) {
    val color = when (theme) {
        AppTheme.DARK    -> Color(0xFF00E5A0)
        AppTheme.LIGHT   -> Color(0xFF006C4C)
        AppTheme.OCEAN   -> Color(0xFF00B4D8)
        AppTheme.FOREST  -> Color(0xFF52B788)
        AppTheme.SUNSET  -> Color(0xFFFF8C42)
        AppTheme.MIDNIGHT-> Color(0xFFBB86FC)
        AppTheme.SYSTEM  -> Color(0xFF90A4AE)
    }
    Box(modifier = Modifier.size(20.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color))
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(0.dp), content = content)
    }
}

@Composable
fun SettingsDivider() {
    Divider(
        modifier = Modifier.padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String = "",
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (onClick != null) {
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
