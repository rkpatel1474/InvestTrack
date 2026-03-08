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
import com.investtrack.ui.holdings.HoldingsScreen
import com.investtrack.ui.loan.AddEditLoanScreen
import com.investtrack.ui.loan.LoanDetailScreen
import com.investtrack.ui.loan.LoanListScreen
import com.investtrack.ui.more.MoreScreen
import com.investtrack.ui.price.PriceUpdateScreen
import com.investtrack.ui.security.SecurityListScreen
import com.investtrack.ui.security.AddEditSecurityScreen
import com.investtrack.ui.settings.SettingsScreen
import com.investtrack.ui.transaction.AddTransactionScreen
import com.investtrack.ui.transaction.TransactionListScreen

sealed class Screen(val route: String) {
    object Dashboard        : Screen("dashboard")
    object Holdings         : Screen("holdings")
    object TransactionList  : Screen("transactions")
    object LoanList         : Screen("loans")
    object More             : Screen("more")
    object FamilyList       : Screen("family_list")
    object SecurityList     : Screen("security_list")
    object PriceUpdate      : Screen("price_update/{securityId}") {
        fun createRoute(securityId: Long = -1L) = "price_update/$securityId"
    }
    object AddEditMember    : Screen("add_edit_member/{memberId}") {
        fun createRoute(memberId: Long = -1L) = "add_edit_member/$memberId"
    }
    object AddEditSecurity  : Screen("add_edit_security/{securityId}") {
        fun createRoute(securityId: Long = -1L) = "add_edit_security/$securityId"
    }
    object AddTransaction   : Screen("add_transaction?securityId={securityId}&editId={editId}") {
        fun createRoute(securityId: Long? = null, editId: Long? = null) =
            "add_transaction?securityId=${securityId ?: -1}&editId=${editId ?: -1}"
    }
    object AddEditLoan      : Screen("add_edit_loan/{loanId}") {
        fun createRoute(loanId: Long = -1L) = "add_edit_loan/$loanId"
    }
    object LoanDetail       : Screen("loan_detail/{loanId}") {
        fun createRoute(loanId: Long) = "loan_detail/$loanId"
    }
    object Settings         : Screen("settings")
}

@Composable
fun InvestTrackNavHost(
    navController: NavHostController,
    onRequestPinSetup: () -> Unit
) {
    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToHoldings = { navController.navigate(Screen.Holdings.route) },
                onNavigateToTransactions = { navController.navigate(Screen.TransactionList.route) },
                onNavigateToAddTransaction = { navController.navigate(Screen.AddTransaction.createRoute()) },
                onNavigateToSecurity = { sid -> navController.navigate(Screen.HoldingDetail.createRoute(sid)) }
            ) },
                onNavigateToTransactions = { navController.navigate(Screen.TransactionList.route) },
                onNavigateToLoans        = { navController.navigate(Screen.LoanList.route) }
            )
        }

        composable(Screen.Holdings.route) {
            HoldingsScreen(
                onAddTransaction = { sid -> navController.navigate(Screen.AddTransaction.createRoute(securityId = sid)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TransactionList.route) {
            TransactionListScreen(
                onAddTransaction    = { navController.navigate(Screen.AddTransaction.createRoute()) },
                onEditTransaction   = { id -> navController.navigate(Screen.AddTransaction.createRoute(editId = id)) },
                onBack              = { navController.popBackStack() }
            )
        }

        composable(Screen.LoanList.route) {
            LoanListScreen(
                onAddLoan    = { navController.navigate(Screen.AddEditLoan.createRoute()) },
                onLoanDetail = { id -> navController.navigate(Screen.LoanDetail.createRoute(id)) },
                onBack       = { navController.popBackStack() }
            )
        }

        composable(Screen.More.route) {
            MoreScreen(
                onNavigateToFamily    = { navController.navigate(Screen.FamilyList.route) },
                onNavigateToSecurity  = { navController.navigate(Screen.SecurityList.route) },
                onNavigateToPriceUpdate = { navController.navigate(Screen.PriceUpdate.createRoute()) },
                onNavigateToSettings  = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.FamilyList.route) {
            FamilyListScreen(
                onAddMember  = { navController.navigate(Screen.AddEditMember.createRoute()) },
                onEditMember = { id -> navController.navigate(Screen.AddEditMember.createRoute(id)) },
                onBack       = { navController.popBackStack() }
            )
        }

        composable(
            Screen.AddEditMember.route,
            arguments = listOf(navArgument("memberId") { type = NavType.LongType; defaultValue = -1L })
        ) { back ->
            val id = back.arguments?.getLong("memberId")?.takeIf { it != -1L }
            AddEditMemberScreen(editMemberId = id, onBack = { navController.popBackStack() })
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
            val id = back.arguments?.getLong("securityId")?.takeIf { it != -1L }
            AddEditSecurityScreen(editSecurityId = id, onBack = { navController.popBackStack() })
        }

        composable(
            Screen.PriceUpdate.route,
            arguments = listOf(navArgument("securityId") { type = NavType.LongType; defaultValue = -1L })
        ) { back ->
            val id = back.arguments?.getLong("securityId")?.takeIf { it != -1L }
            PriceUpdateScreen(preSelectedSecurityId = id, onBack = { navController.popBackStack() })
        }

        composable(
            Screen.AddTransaction.route,
            arguments = listOf(
                navArgument("securityId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("editId")     { type = NavType.LongType; defaultValue = -1L }
            )
        ) { back ->
            val secId  = back.arguments?.getLong("securityId")?.takeIf { it != -1L }
            val editId = back.arguments?.getLong("editId")?.takeIf { it != -1L }
            AddTransactionScreen(
                preSelectedSecurityId = secId,
                editTransactionId = editId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Screen.AddEditLoan.route,
            arguments = listOf(navArgument("loanId") { type = NavType.LongType; defaultValue = -1L })
        ) { back ->
            val id = back.arguments?.getLong("loanId")?.takeIf { it != -1L }
            AddEditLoanScreen(onEdit = id, onBack = { navController.popBackStack() })
        }

        composable(
            Screen.LoanDetail.route,
            arguments = listOf(navArgument("loanId") { type = NavType.LongType })
        ) { back ->
            val id = back.arguments!!.getLong("loanId")
            LoanDetailScreen(
                loanId = id,
                onEdit = { navController.navigate(Screen.AddEditLoan.createRoute(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onSetupPin = onRequestPinSetup
            )
        }
    }
}
