package com.investtrack.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.investtrack.ui.dashboard.DashboardScreen
import com.investtrack.ui.family.FamilyListScreen
import com.investtrack.ui.family.AddEditFamilyScreen
import com.investtrack.ui.security.SecurityListScreen
import com.investtrack.ui.security.AddEditSecurityScreen
import com.investtrack.ui.transaction.TransactionListScreen
import com.investtrack.ui.transaction.AddTransactionScreen
import com.investtrack.ui.price.PriceUpdateScreen
import com.investtrack.ui.loan.LoanListScreen
import com.investtrack.ui.loan.AddEditLoanScreen
import com.investtrack.ui.loan.LoanDetailScreen
import com.investtrack.ui.holdings.HoldingsScreen
import com.investtrack.ui.holdings.HoldingDetailScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object FamilyList : Screen("family_list")
    object AddEditFamily : Screen("add_edit_family?memberId={memberId}") {
        fun createRoute(memberId: Long? = null) = if (memberId != null) "add_edit_family?memberId=$memberId" else "add_edit_family?memberId=-1"
    }
    object SecurityList : Screen("security_list")
    object AddEditSecurity : Screen("add_edit_security?securityId={securityId}") {
        fun createRoute(securityId: Long? = null) = if (securityId != null) "add_edit_security?securityId=$securityId" else "add_edit_security?securityId=-1"
    }
    object TransactionList : Screen("transaction_list")
    object AddTransaction : Screen("add_transaction?securityId={securityId}") {
        fun createRoute(securityId: Long? = null) = if (securityId != null) "add_transaction?securityId=$securityId" else "add_transaction?securityId=-1"
    }
    object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long) = "edit_transaction/$transactionId"
    }
    object PriceUpdate : Screen("price_update?securityId={securityId}") {
        fun createRoute(securityId: Long? = null) = if (securityId != null) "price_update?securityId=$securityId" else "price_update?securityId=-1"
    }
    object LoanList : Screen("loan_list")
    object AddEditLoan : Screen("add_edit_loan?loanId={loanId}") {
        fun createRoute(loanId: Long? = null) = if (loanId != null) "add_edit_loan?loanId=$loanId" else "add_edit_loan?loanId=-1"
    }
    object LoanDetail : Screen("loan_detail/{loanId}") {
        fun createRoute(loanId: Long) = "loan_detail/$loanId"
    }
    object Holdings : Screen("holdings")
    object HoldingDetail : Screen("holding_detail/{securityId}") {
        fun createRoute(securityId: Long) = "holding_detail/$securityId"
    }
}

@Composable
fun InvestTrackNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToHoldings = { navController.navigate(Screen.Holdings.route) },
                onNavigateToTransactions = { navController.navigate(Screen.TransactionList.route) },
                onNavigateToAddTransaction = { navController.navigate(Screen.AddTransaction.createRoute()) },
                onNavigateToSecurity = { sid -> navController.navigate(Screen.HoldingDetail.createRoute(sid)) }
            )
        }
        composable(Screen.FamilyList.route) {
            FamilyListScreen(
                onAddFamily = { navController.navigate(Screen.AddEditFamily.createRoute()) },
                onEditFamily = { id -> navController.navigate(Screen.AddEditFamily.createRoute(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.AddEditFamily.route,
            arguments = listOf(navArgument("memberId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStack ->
            val memberId = backStack.arguments?.getLong("memberId")?.takeIf { it != -1L }
            AddEditFamilyScreen(memberId = memberId, onBack = { navController.popBackStack() })
        }
        composable(Screen.SecurityList.route) {
            SecurityListScreen(
                onAddSecurity = { navController.navigate(Screen.AddEditSecurity.createRoute()) },
                onEditSecurity = { id -> navController.navigate(Screen.AddEditSecurity.createRoute(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.AddEditSecurity.route,
            arguments = listOf(navArgument("securityId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStack ->
            val securityId = backStack.arguments?.getLong("securityId")?.takeIf { it != -1L }
            AddEditSecurityScreen(securityId = securityId, onBack = { navController.popBackStack() })
        }
        composable(Screen.TransactionList.route) {
            TransactionListScreen(
                onAddTransaction = { navController.navigate(Screen.AddTransaction.createRoute()) },
                onEditTransaction = { id -> navController.navigate(Screen.EditTransaction.createRoute(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.AddTransaction.route,
            arguments = listOf(navArgument("securityId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStack ->
            val securityId = backStack.arguments?.getLong("securityId")?.takeIf { it != -1L }
            AddTransactionScreen(preSelectedSecurityId = securityId, onBack = { navController.popBackStack() })
        }
        composable(
            Screen.EditTransaction.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
        ) { backStack ->
            AddTransactionScreen(
                preSelectedSecurityId = null,
                editTransactionId = backStack.arguments!!.getLong("transactionId"),
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.PriceUpdate.route,
            arguments = listOf(navArgument("securityId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStack ->
            val securityId = backStack.arguments?.getLong("securityId")?.takeIf { it != -1L }
            PriceUpdateScreen(preSelectedSecurityId = securityId, onBack = { navController.popBackStack() })
        }
        composable(Screen.LoanList.route) {
            LoanListScreen(
                onAddLoan = { navController.navigate(Screen.AddEditLoan.createRoute()) },
                onLoanDetail = { id -> navController.navigate(Screen.LoanDetail.createRoute(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.AddEditLoan.route,
            arguments = listOf(navArgument("loanId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStack ->
            val loanId = backStack.arguments?.getLong("loanId")?.takeIf { it != -1L }
            AddEditLoanScreen(loanId = loanId, onBack = { navController.popBackStack() })
        }
        composable(
            Screen.LoanDetail.route,
            arguments = listOf(navArgument("loanId") { type = NavType.LongType })
        ) { backStack ->
            LoanDetailScreen(loanId = backStack.arguments!!.getLong("loanId"), onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Screen.AddEditLoan.createRoute(id)) })
        }
        composable(Screen.Holdings.route) {
            HoldingsScreen(
                onHoldingClick = { sid -> navController.navigate(Screen.HoldingDetail.createRoute(sid)) },
                onUpdatePrice = { sid -> navController.navigate(Screen.PriceUpdate.createRoute(sid)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.HoldingDetail.route,
            arguments = listOf(navArgument("securityId") { type = NavType.LongType })
        ) { backStack ->
            HoldingDetailScreen(
                securityId = backStack.arguments!!.getLong("securityId"),
                onAddTransaction = { sid -> navController.navigate(Screen.AddTransaction.createRoute(sid)) },
                onUpdatePrice = { sid -> navController.navigate(Screen.PriceUpdate.createRoute(sid)) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}