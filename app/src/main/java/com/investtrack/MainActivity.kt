package com.investtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.investtrack.navigation.InvestTrackNavHost
import com.investtrack.navigation.Screen
import com.investtrack.ui.theme.InvestTrackTheme
import dagger.hilt.android.AndroidEntryPoint

data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InvestTrackTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        BottomNavItem("Dashboard", Icons.Default.Dashboard, Screen.Dashboard.route),
        BottomNavItem("Holdings", Icons.Default.PieChart, Screen.Holdings.route),
        BottomNavItem("Transactions", Icons.Default.Receipt, Screen.TransactionList.route),
        BottomNavItem("Loans", Icons.Default.AccountBalance, Screen.LoanList.route),
        BottomNavItem("More", Icons.Default.MoreHoriz, Screen.More.route),
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val topLevelRoutes = bottomNavItems.map { it.route }
    val showBottomBar = currentDestination?.route in topLevelRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
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
        },
        floatingActionButton = {
            when (currentDestination?.route) {
                Screen.TransactionList.route -> FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddTransaction.createRoute()) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) { Icon(Icons.Default.Add, "Add Transaction") }
                Screen.LoanList.route -> FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddEditLoan.createRoute()) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) { Icon(Icons.Default.Add, "Add Loan") }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            InvestTrackNavHost(navController = navController)
        }
    }
}
