package com.investtrack.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.investtrack.ui.dashboard.DashboardScreen
import com.investtrack.ui.family.FamilyListScreen
import com.investtrack.ui.family.AddEditMemberScreen
import com.investtrack.ui.security.SecurityListScreen
import com.investtrack.ui.security.AddEditSecurityScreen
import com.investtrack.ui.transaction.TransactionListScreen
import com.investtrack.ui.transaction.AddTransactionScreen
import com.investtrack.ui.price.PriceUpdateScreen
import com.investtrack.ui.loan.LoanListScreen
import com.investtrack.ui.loan.AddEditLoanScreen
import com.investtrack.ui.loan.LoanDetailScreen
import com.investtrack.ui.holdings.HoldingsScreen
import com.investtrack.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Dashboard       : Screen("dashboard")
    object Holdings        : Screen("holdings")
    object TransactionList : Screen("transaction_list")
    object LoanList        : Screen("loan_list")
    object More            : Screen("more")
    object FamilyList      : Screen("family_list")
    object SecurityList    : Screen("security_list")
    object Settings        : Screen("settings")
    object AddEditFamily   : Screen("add_edit_member?memberId={memberId}") {
        fun createRoute(memberId: Long? = null) = "add_edit_member?memberId=${memberId ?: -1L}"
    }
    object AddEditSecurity : Screen("add_edit_security?securityId={securityId}") {
        fun createRoute(securityId: Long? = null) = "add_edit_security?securityId=${securityId ?: -1L}"
    }
    object AddTransaction  : Screen("add_transaction?securityId={securityId}") {
        fun createRoute(securityId: Long? = null) = "add_transaction?securityId=${securityId ?: -1L}"
    }
    object PriceUpdate     : Screen("price_update?securityId={securityId}") {
        fun createRoute(securityId: Long? = null) = "price_update?securityId=${securityId ?: -1L}"
    }
    object AddEditLoan     : Screen("add_edit_loan?loanId={loanId}") {
        fun createRoute(loanId: Long? = null) = "add_edit_loan?loanId=${loanId ?: -1L}"
    }
    object LoanDetail      : Screen("loan_detail/{loanId}") {
        fun createRoute(loanId: Long) = "loan_detail/$loanId"
    }
}

@Composable
fun InvestTrackNavHost(navController: NavHostController, onRequestPinSetup: () -> Unit) {
    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToHoldings       = { navController.navigate(Screen.Holdings.route) },
                onNavigateToTransactions   = { navController.navigate(Screen.TransactionList.route) },
                onNavigateToAddTransaction = { navController.navigate(Screen.AddTransaction.createRoute()) },
                onNavigateToSecurity       = { sid -> if (sid > 0L) navController.navigate(Screen.AddTransaction.createRoute(sid)) }
            )
        }

        composable(Screen.Holdings.route) {
            HoldingsScreen(
                onHoldingClick = { sid -> navController.navigate(Screen.AddTransaction.createRoute(sid)) },
                onUpdatePrice  = { sid -> navController.navigate(Screen.PriceUpdate.createRoute(sid)) },
                onBack         = { navController.popBackStack() }
            )
        }

        composable(Screen.TransactionList.route) {
            TransactionListScreen(
                onAddTransaction  = { navController.navigate(Screen.AddTransaction.createRoute()) },
                onEditTransaction = { navController.navigate(Screen.AddTransaction.createRoute()) },
                onBack            = { navController.popBackStack() }
            )
        }

        composable(
            Screen.AddTransaction.route,
            arguments = listOf(navArgument("securityId") { type = NavType.LongType; defaultValue = -1L })
        ) { back ->
            AddTransactionScreen(
                preSelectedSecurityId = back.arguments?.getLong("securityId")?.takeIf { it != -1L },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Screen.PriceUpdate.route,
            arguments = listOf(navArgument("securityId") { type = NavType.LongType; defaultValue = -1L })
        ) { back ->
            PriceUpdateScreen(
                preSelectedSecurityId = back.arguments?.getLong("securityId")?.takeIf { it != -1L },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LoanList.route) {
            LoanListScreen(
                onAddLoan    = { navController.navigate(Screen.AddEditLoan.createRoute()) },
                onLoanDetail = { id -> navController.navigate(Screen.LoanDetail.createRoute(id)) },
                onBack       = { navController.popBackStack() }
            )
        }

        composable(
            Screen.AddEditLoan.route,
            arguments = listOf(navArgument("loanId") { type = NavType.LongType; defaultValue = -1L })
        ) { back ->
            AddEditLoanScreen(
                loanId = back.arguments?.getLong("loanId")?.takeIf { it != -1L },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Screen.LoanDetail.route,
            arguments = listOf(navArgument("loanId") { type = NavType.LongType })
        ) { back ->
            LoanDetailScreen(
                loanId = back.arguments!!.getLong("loanId"),
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Screen.AddEditLoan.createRoute(id)) }
            )
        }

        composable(Screen.More.route) {
            SettingsScreen(
                onSetupPin = onRequestPinSetup,
                onBack     = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onSetupPin = onRequestPinSetup,
                onBack     = { navController.popBackStack() }
            )
        }

        composable(Screen.FamilyList.route) {
            FamilyListScreen(
                onAddMember  = { navController.navigate(Screen.AddEditFamily.createRoute()) },
                onEditMember = { id -> navController.navigate(Screen.AddEditFamily.createRoute(id)) },
                onBack       = { navController.popBackStack() }
            )
        }

        composable(
            Screen.AddEditFamily.route,
            arguments = listOf(navArgument("memberId") { type = NavType.LongType; defaultValue = -1L })
        ) { back ->
            AddEditMemberScreen(
                editMemberId = back.arguments?.getLong("memberId")?.takeIf { it != -1L },
                onBack       = { navController.popBackStack() }
            )
        }

        composable(Screen.SecurityList.route) {
            SecurityListScreen(
                onAddSecurity  = { navController.navigate(Screen.AddEditSecurity.createRoute()) },
                onEditSecurity = { id -> navController.navigate(Screen.AddEditSecurity.createRoute(id)) },
                onBack         = { navController.popBackStack() }
            )
        }

        composable(
            Screen.AddEditSecurity.route,
            arguments = listOf(navArgument("securityId") { type = NavType.LongType; defaultValue = -1L })
        ) { back ->
            AddEditSecurityScreen(
                editSecurityId = back.arguments?.getLong("securityId")?.takeIf { it != -1L },
                onBack         = { navController.popBackStack() }
            )
        }
    }
}
