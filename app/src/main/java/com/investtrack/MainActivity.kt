package com.investtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.investtrack.navigation.InvestTrackNavHost
import com.investtrack.navigation.Screen
import com.investtrack.ui.theme.Emerald500
import com.investtrack.ui.theme.InvestTrackTheme
import com.investtrack.ui.theme.Navy700
import com.investtrack.ui.theme.Navy800
import dagger.hilt.android.AndroidEntryPoint

data class NavItem(val label: String, val icon: ImageVector, val route: String)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { InvestTrackTheme { MainApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()

    val navItems = listOf(
        NavItem("Home",     Icons.Default.Dashboard,      Screen.Dashboard.route),
        NavItem("Portfolio",Icons.Default.PieChart,       Screen.Holdings.route),
        NavItem("Txns",     Icons.Default.SwapHoriz,      Screen.TransactionList.route),
        NavItem("Loans",    Icons.Default.AccountBalance,  Screen.LoanList.route),
        NavItem("More",     Icons.Default.GridView,        Screen.More.route),
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
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background)),
                            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        shadowElevation = 16.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            navItems.forEach { item ->
                                val selected = navBackStackEntry?.destination?.hierarchy
                                    ?.any { it.route == item.route } == true

                                NavPill(
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
                Screen.TransactionList.route -> FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddTransaction.createRoute()) },
                    containerColor = Emerald500,
                    contentColor = Navy800,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) { Icon(Icons.Default.Add, "Add", modifier = Modifier.size(28.dp)) }

                Screen.LoanList.route -> FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddEditLoan.createRoute()) },
                    containerColor = Emerald500,
                    contentColor = Navy800,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) { Icon(Icons.Default.Add, "Add", modifier = Modifier.size(28.dp)) }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            InvestTrackNavHost(navController = navController)
        }
    }
}

@Composable
fun NavPill(item: NavItem, selected: Boolean, onClick: () -> Unit) {
    val bgColor = if (selected) Emerald500.copy(alpha = 0.15f) else Color.Transparent
    val iconColor = if (selected) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(24.dp)) {
            Icon(item.icon, contentDescription = item.label, tint = iconColor, modifier = Modifier.size(24.dp))
        }
    }
}