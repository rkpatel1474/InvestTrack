package com.investtrack

import android.os.Bundle
import androidx.activity.compose.setContent
import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.investtrack.data.preferences.PreferencesManager
import com.investtrack.navigation.InvestTrackNavHost
import com.investtrack.navigation.Screen
import com.investtrack.ui.lock.PinLockScreen
import com.investtrack.ui.lock.hashPin
import com.investtrack.ui.theme.AppTheme
import com.investtrack.ui.theme.InvestTrackTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val prefs: PreferencesManager) : ViewModel() {
    val appTheme       = prefs.appTheme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.DARK)
    val lockEnabled    = prefs.lockEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val pinHash        = prefs.pinHash.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val biometricEnabled = prefs.biometricEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoLockMins   = prefs.autoLockMins.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    fun setPinHash(hash: String) = viewModelScope.launch {
        prefs.setPinHash(hash)
        prefs.setLockEnabled(true)
    }
}

data class NavItem(val label: String, val icon: ImageVector, val route: String)

// ── CRITICAL FIX: FragmentActivity required for BiometricPrompt ──────────────
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    // Track when app went to background for auto-lock
    private var backgroundedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MainViewModel = hiltViewModel()
            val appTheme by vm.appTheme.collectAsState()
            InvestTrackTheme(appTheme = appTheme) {
                MainApp(vm, activity = this)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    fun getBackgroundedAt() = backgroundedAt
}

@Composable
fun MainApp(vm: MainViewModel, activity: MainActivity) {
    val lockEnabled      by vm.lockEnabled.collectAsState()
    val pinHash          by vm.pinHash.collectAsState()
    val biometricEnabled by vm.biometricEnabled.collectAsState()
    val autoLockMins     by vm.autoLockMins.collectAsState()

    var isUnlocked       by remember { mutableStateOf(false) }
    var isSettingUpPin   by remember { mutableStateOf(false) }

    // Auto-lock: re-lock when app resumes after timeout
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, lockEnabled, autoLockMins) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && lockEnabled && isUnlocked) {
                val backgroundedAt = activity.getBackgroundedAt()
                if (backgroundedAt > 0) {
                    val elapsedMs = SystemClock.elapsedRealtime() - backgroundedAt
                    val timeoutMs = autoLockMins * 60_000L
                    if (elapsedMs >= timeoutMs) {
                        isUnlocked = false
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when {
        isSettingUpPin -> {
            PinLockScreen(
                title = "Set PIN",
                storedPinHash = null,
                activity = activity,
                onSuccess = {},
                onSetupPin = { hash ->
                    vm.setPinHash(hash)
                    isSettingUpPin = false
                    isUnlocked = true
                }
            )
        }
        lockEnabled && !isUnlocked -> {
            PinLockScreen(
                storedPinHash = pinHash,
                biometricEnabled = biometricEnabled,
                activity = activity,
                onSuccess = { isUnlocked = true }
            )
        }
        else -> {
            AppScaffold(onRequestPinSetup = { isSettingUpPin = true })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(onRequestPinSetup: () -> Unit) {
    val navController = rememberNavController()
    val navItems = listOf(
        NavItem("Home",      Icons.Default.Dashboard,      Screen.Dashboard.route),
        NavItem("Portfolio", Icons.Default.PieChart,       Screen.Holdings.route),
        NavItem("Txns",      Icons.Default.SwapHoriz,      Screen.TransactionList.route),
        NavItem("Loans",     Icons.Default.AccountBalance, Screen.LoanList.route),
        NavItem("More",      Icons.Default.GridView,       Screen.More.route),
    )
    val navBackStackEntry  by navController.currentBackStackEntryAsState()
    val currentRoute       = navBackStackEntry?.destination?.route
    val topLevelRoutes     = navItems.map { it.route }
    val showBottomBar      = currentRoute in topLevelRoutes

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it } + fadeIn(),
                exit  = slideOutVertically { it } + fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Surface(
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(28.dp),
                        color         = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        shadowElevation = 16.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            navItems.forEach { item ->
                                val selected = navBackStackEntry?.destination?.hierarchy
                                    ?.any { it.route == item.route } == true
                                PillNavItem(item = item, selected = selected) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState    = true
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            when (currentRoute) {
                Screen.TransactionList.route ->
                    FloatingActionButton(onClick = { navController.navigate(Screen.AddTransaction.createRoute()) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.size(52.dp)
                    ) { Icon(Icons.Default.Add, "Add") }
                Screen.LoanList.route ->
                    FloatingActionButton(onClick = { navController.navigate(Screen.AddEditLoan.createRoute()) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.size(52.dp)
                    ) { Icon(Icons.Default.Add, "Add") }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            InvestTrackNavHost(navController = navController, onRequestPinSetup = onRequestPinSetup)
        }
    }
}

@Composable
fun PillNavItem(item: NavItem, selected: Boolean, onClick: () -> Unit) {
    val bg    = if (selected) MaterialTheme.colorScheme.primary.copy(0.15f) else Color.Transparent
    val tint  = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = if (selected) 12.dp else 14.dp, vertical = 8.dp)
            .semantics { contentDescription = item.label },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            AnimatedVisibility(visible = selected) {
                Row {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = tint,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
