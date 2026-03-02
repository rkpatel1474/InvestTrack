package com.investtrack.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investtrack.data.database.entities.AssetClass
import com.investtrack.data.database.entities.SecurityType
import com.investtrack.data.database.entities.Transaction
import com.investtrack.data.database.entities.TransactionType
import com.investtrack.data.repository.FamilyRepository
import com.investtrack.data.repository.LoanRepository
import com.investtrack.data.repository.PortfolioRepository
import com.investtrack.data.repository.PortfolioSummary
import com.investtrack.data.repository.SecurityRepository
import com.investtrack.data.repository.TransactionRepository
import com.investtrack.ui.common.MetricCard
import com.investtrack.ui.common.SectionHeader
import com.investtrack.ui.theme.GainColor
import com.investtrack.ui.theme.LossColor
import com.investtrack.utils.DateUtils.toDisplayDate
import com.investtrack.utils.FinancialUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val portfolioSummary: PortfolioSummary? = null,
    val recentTransactions: List<RecentTxn> = emptyList(),
    val totalLoanOutstanding: Double = 0.0,
    val totalMonthlyEMI: Double = 0.0
)

data class RecentTxn(
    val transaction: Transaction,
    val securityName: String,
    val memberName: String
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val portfolioRepository: PortfolioRepository,
    private val transactionRepository: TransactionRepository,
    private val securityRepository: SecurityRepository,
    private val familyRepository: FamilyRepository,
    private val loanRepository: LoanRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { loadDashboard() }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val summary = portfolioRepository.getPortfolioSummary()
                val recentTxns = buildRecentTransactions()
                val (outstanding, emi) = getLoanTotals()
                _uiState.update { it.copy(isLoading = false, portfolioSummary = summary, recentTransactions = recentTxns, totalLoanOutstanding = outstanding, totalMonthlyEMI = emi) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun buildRecentTransactions(): List<RecentTxn> {
        val txns = transactionRepository.getRecentTransactions(5).first()
        val allMembers = familyRepository.getAllMembers().first()
        return txns.map { txn ->
            val sec = securityRepository.getSecurityById(txn.securityId)
            val member = allMembers.find { it.id == txn.familyMemberId }
            RecentTxn(txn, sec?.securityName ?: "Unknown", member?.name ?: "Unknown")
        }
    }

    private suspend fun getLoanTotals(): Pair<Double, Double> {
        val loans = loanRepository.getAllLoans().first()
        var outstanding = 0.0
        var emi = 0.0
        for (loan in loans) {
            outstanding += (loan.loanAmount - loanRepository.getTotalPrincipalPaid(loan.id)).coerceAtLeast(0.0)
            emi += loan.emiAmount
        }
        return outstanding to emi
    }
}

fun assetClassColor(ac: AssetClass): Color = when (ac) {
    AssetClass.EQUITY -> Color(0xFF1565C0)
    AssetClass.DEBT -> Color(0xFF558B2F)
    AssetClass.HYBRID -> Color(0xFF6A1B9A)
    AssetClass.REAL_ESTATE -> Color(0xFFBF360C)
    AssetClass.GOLD -> Color(0xFFF9A825)
    AssetClass.CASH -> Color(0xFF00695C)
    AssetClass.COMMODITY -> Color(0xFF4E342E)
    else -> Color(0xFF546E7A)
}

fun securityTypeIcon(type: SecurityType): ImageVector = when (type) {
    SecurityType.MUTUAL_FUND -> Icons.Default.PieChart
    SecurityType.SHARES -> Icons.Default.TrendingUp
    SecurityType.BOND, SecurityType.GOI_BOND -> Icons.Default.AccountBalance
    SecurityType.NPS -> Icons.Default.Elderly
    SecurityType.PF -> Icons.Default.Work
    SecurityType.FD -> Icons.Default.Savings
    SecurityType.INSURANCE -> Icons.Default.Security
    SecurityType.PROPERTY -> Icons.Default.Home
    SecurityType.GOLD -> Icons.Default.Star
    else -> Icons.Default.AttachMoney
}

@Composable
fun DashboardScreen(
    onNavigateToHoldings: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToSecurity: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadDashboard() }

    LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("InvestTrack", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                    Text("Portfolio Dashboard", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onNavigateToAddTransaction, modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)) {
                        Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.loadDashboard() }) { Icon(Icons.Default.Refresh, "Refresh") }
                }
            }
        }
        if (uiState.isLoading) {
            item { Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        } else {
            uiState.portfolioSummary?.let { summary ->
                item { PortfolioHeaderCard(summary) }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard("Total Invested", FinancialUtils.formatCurrency(summary.totalCost), modifier = Modifier.weight(1f))
                        MetricCard("Current Value", FinancialUtils.formatCurrency(summary.totalMarketValue), modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard("Absolute Return", FinancialUtils.formatPercent(summary.absoluteReturn), modifier = Modifier.weight(1f), valueColor = if (summary.absoluteReturn >= 0) GainColor else LossColor)
                        MetricCard("XIRR", summary.xirr?.let { FinancialUtils.formatPercent(it) } ?: "N/A", modifier = Modifier.weight(1f), valueColor = if ((summary.xirr ?: 0.0) >= 0) GainColor else LossColor)
                    }
                }
                if (summary.assetClassBreakdown.isNotEmpty()) {
                    item {
                        SectionHeader("Asset Allocation") { TextButton(onClick = onNavigateToHoldings) { Text("View All") } }
                        AssetBreakdownCard(summary, onNavigateToHoldings)
                    }
                }
                item { SectionHeader("Investment Categories"); SecurityTypeRow(summary) }
            }
            item {
                Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Loan Outstanding", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(FinancialUtils.formatCurrency(uiState.totalLoanOutstanding), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = LossColor)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Monthly EMI", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(FinancialUtils.formatCurrency(uiState.totalMonthlyEMI), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (uiState.recentTransactions.isNotEmpty()) {
                item { SectionHeader("Recent Transactions") { TextButton(onClick = onNavigateToTransactions) { Text("View All") } } }
                items(uiState.recentTransactions) { recent ->
                    RecentTxnCard(recent) { onNavigateToSecurity(recent.transaction.securityId) }
                }
            }
        }
    }
}

@Composable
fun PortfolioHeaderCard(summary: PortfolioSummary) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Total Portfolio Value", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
            Text(FinancialUtils.formatCurrencyFull(summary.totalMarketValue), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Gain/Loss", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(0.7f))
                    Text("${if (summary.totalGain >= 0) "+" else ""}${FinancialUtils.formatCurrency(summary.totalGain)}", color = if (summary.totalGain >= 0) Color(0xFF80FF80) else Color(0xFFFF8080), fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("CAGR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(0.7f))
                    Text(summary.cagr?.let { FinancialUtils.formatPercent(it) } ?: "N/A", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AssetBreakdownCard(summary: PortfolioSummary, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            summary.assetClassBreakdown.values.sortedByDescending { it.marketValue }.forEach { ac ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(assetClassColor(ac.assetClass), CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(ac.assetClass.name.replace("_", " "), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text(FinancialUtils.formatCurrency(ac.marketValue), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Text("%.1f%%".format(ac.percentage), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LinearProgressIndicator(progress = { (ac.percentage / 100f).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(4.dp), color = assetClassColor(ac.assetClass), trackColor = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}

@Composable
fun SecurityTypeRow(summary: PortfolioSummary) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(summary.securityTypeBreakdown.entries.sortedByDescending { it.value }) { (type, value) ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(securityTypeIcon(type), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(type.name.replace("_", " "), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                    Text(FinancialUtils.formatCurrency(value), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RecentTxnCard(recent: RecentTxn, onClick: () -> Unit) {
    val txn = recent.transaction
    val isCredit = txn.transactionType in listOf(TransactionType.SELL, TransactionType.REDEEM, TransactionType.DIVIDEND, TransactionType.COUPON, TransactionType.MATURITY)
    val amount = txn.amount ?: ((txn.units ?: 0.0) * (txn.price ?: 0.0))
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.SwapHoriz, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(recent.securityName, fontWeight = FontWeight.SemiBold, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                Text("${txn.transactionType.name} • ${recent.memberName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(txn.transactionDate.toDisplayDate(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${if (isCredit) "+" else "-"}${FinancialUtils.formatCurrency(amount)}", color = if (isCredit) GainColor else LossColor, fontWeight = FontWeight.Bold)
        }
    }
}
