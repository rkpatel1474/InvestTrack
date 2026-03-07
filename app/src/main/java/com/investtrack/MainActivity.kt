package com.investtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
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
    val appTheme = prefs.appTheme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.DARK)
    val lockEnabled = prefs.lockEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val pinHash = prefs.pinHash.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val biometricEnabled = prefs.biometricEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    fun setPinHash(hash: String) = viewModelScope.launch { prefs.setPinHash(hash); prefs.setLockEnabled(true) }
}

data class NavItem(val label: String, val icon: ImageVector, val route: String)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MainViewModel = hiltViewModel()
            val appTheme by vm.appTheme.collectAsState()
            InvestTrackTheme(appTheme = appTheme) { MainApp(vm) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(vm: MainViewModel = hiltViewModel()) {
    val lockEnabled by vm.lockEnabled.collectAsState()
    val pinHash by vm.pinHash.collectAsState()
    val biometricEnabled by vm.biometricEnabled.collectAsState()

    var isUnlocked by remember { mutableStateOf(false) }
    var isSettingUpPin by remember { mutableStateOf(false) }

    when {
        isSettingUpPin -> {
            PinLockScreen(
                title = "Set PIN",
                storedPinHash = null,
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
        NavItem("Home",      Icons.Default.Dashboard,     Screen.Dashboard.route),
        NavItem("Portfolio", Icons.Default.PieChart,      Screen.Holdings.route),
        NavItem("Txns",      Icons.Default.SwapHoriz,     Screen.TransactionList.route),
        NavItem("Loans",     Icons.Default.AccountBalance, Screen.LoanList.route),
        NavItem("More",      Icons.Default.GridView,       Screen.More.route),
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val topLevelRoutes = navItems.map { it.route }
    val showBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        shadowElevation = 20.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            navItems.forEach { item ->
                                val selected = navBackStackEntry?.destination?.hierarchy
                                    ?.any { it.route == item.route } == true
                                ModernNavItem(
                                    item = item,
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            when (currentRoute) {
                Screen.TransactionList.route -> SmallFAB { navController.navigate(Screen.AddTransaction.createRoute()) }
                Screen.LoanList.route -> SmallFAB { navController.navigate(Screen.AddEditLoan.createRoute()) }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            InvestTrackNavHost(
                navController = navController,
                onRequestPinSetup = onRequestPinSetup
            )
        }
    }
}

@Composable
fun ModernNavItem(item: NavItem, selected: Boolean, onClick: () -> Unit) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
    val iconColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(item.icon, contentDescription = item.label, tint = iconColor, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun SmallFAB(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
        modifier = Modifier.size(52.dp)
    ) {
        Icon(Icons.Default.Add, "Add", modifier = Modifier.size(26.dp))
    }
}
