package com.investtrack.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investtrack.data.database.entities.SecurityType
import com.investtrack.data.database.entities.Transaction
import com.investtrack.data.database.entities.TransactionType
import com.investtrack.data.repository.FamilyRepository
import com.investtrack.data.repository.LoanRepository
import com.investtrack.data.repository.PortfolioRepository
import com.investtrack.data.repository.PortfolioSummary
import com.investtrack.data.repository.TransactionRepository
import com.investtrack.ui.common.*
import com.investtrack.ui.theme.GainColor
import com.investtrack.ui.theme.GoldColor
import com.investtrack.ui.theme.LossColor
import com.investtrack.utils.DateUtils.toDisplayDate
import com.investtrack.utils.FinancialUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val portfolioRepo: PortfolioRepository,
    private val txnRepo: TransactionRepository,
    private val loanRepo: LoanRepository,
    private val familyRepo: FamilyRepository
) : ViewModel() {
    // These flows auto-update whenever DB changes — fixing the stale data issue
    val recentTransactions = txnRepo.getRecentTransactions(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allLoans = loanRepo.getAllLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allMembers = familyRepo.getAllMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _portfolioSummary = kotlinx.coroutines.flow.MutableStateFlow<PortfolioSummary?>(null)
    val portfolioSummary = _portfolioSummary.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init { refreshPortfolio() }

    fun refreshPortfolio() = viewModelScope.launch {
        _portfolioSummary.value = portfolioRepo.getPortfolioSummary()
    }

    suspend fun getSecurityName(id: Long): String = portfolioRepo.let {
        // get from repo
        ""
    }
}

// ─── Dashboard Screen ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToPortfolio: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToLoans: () -> Unit,
    vm: DashboardViewModel = hiltViewModel()
) {
    val summary by vm.portfolioSummary.collectAsState()
    val recentTxns by vm.recentTransactions.collectAsState()
    val allLoans by vm.allLoans.collectAsState()

    // Auto-refresh portfolio whenever transactions change
    LaunchedEffect(recentTxns) { vm.refreshPortfolio() }

    val totalLoanAmount = allLoans.sumOf { it.loanAmount }
    val netWorth = (summary?.totalMarketValue ?: 0.0) - totalLoanAmount

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("InvestTrack", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                        Text("Your Financial Dashboard", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refreshPortfolio() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── Net Worth Hero ────────────────────────────────────────────
            item {
                NetWorthCard(
                    netWorth = netWorth,
                    invested = summary?.totalCost ?: 0.0,
                    currentValue = summary?.totalMarketValue ?: 0.0,
                    gainPercent = summary?.gainPercent ?: 0.0
                )
            }

            // ─── Quick Stats Row ───────────────────────────────────────────
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        label = "Invested",
                        value = formatAmount(summary?.totalCost ?: 0.0),
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.TrendingUp,
                        iconColor = GainColor
                    )
                    StatCard(
                        label = "Liabilities",
                        value = formatAmount(totalLoanAmount),
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AccountBalance,
                        iconColor = LossColor
                    )
                }
            }

            // ─── Gain/Loss Card ────────────────────────────────────────────
            if ((summary?.totalGain ?: 0.0) != 0.0) {
                item {
                    val gain = summary!!.totalGain
                    val isGain = gain >= 0
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isGain) GainColor.copy(0.08f) else LossColor.copy(0.08f)
                        )
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isGain) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                null,
                                tint = if (isGain) GainColor else LossColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Overall P&L", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(formatAmount(gain), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = if (isGain) GainColor else LossColor)
                                    GainLossBadge(summary!!.gainPercent)
                                }
                            }
                        }
                    }
                }
            }

            // ─── Quick Actions ─────────────────────────────────────────────
            item {
                SectionHeader("Quick Actions")
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { QuickActionChip(Icons.Default.ShowChart, "Portfolio", onNavigateToPortfolio) }
                    item { QuickActionChip(Icons.Default.SwapHoriz, "Transactions", onNavigateToTransactions) }
                    item { QuickActionChip(Icons.Default.AccountBalance, "Loans", onNavigateToLoans) }
                }
            }

            // ─── Asset Allocation ──────────────────────────────────────────
            summary?.let { s ->
                if (s.holdings.isNotEmpty()) {
                    item {
                        SectionHeader("Asset Allocation", "View All") { onNavigateToPortfolio() }
                        Spacer(Modifier.height(8.dp))
                        AssetAllocationCard(s.holdings.groupBy { it.security.securityType }.map { (type, h) ->
                            type to h.sumOf { it.marketValue }
                        })
                    }
                }
            }

            // ─── Top Holdings ──────────────────────────────────────────────
            summary?.let { s ->
                if (s.holdings.isNotEmpty()) {
                    item { SectionHeader("Top Holdings", "See All") { onNavigateToPortfolio() } }
                    items(s.holdings.sortedByDescending { it.marketValue }.take(5), key = { it.security.id }) { h ->
                        MiniHoldingRow(holding = h)
                    }
                }
            }

            // ─── Recent Transactions ───────────────────────────────────────
            if (recentTxns.isNotEmpty()) {
                item { SectionHeader("Recent Activity", "All") { onNavigateToTransactions() } }
                items(recentTxns.take(5), key = { it.id }) { txn ->
                    MiniTransactionRow(txn)
                }
            }

            // ─── Active Loans ──────────────────────────────────────────────
            if (allLoans.isNotEmpty()) {
                item { SectionHeader("Active Loans", "All") { onNavigateToLoans() } }
                items(allLoans.take(3), key = { it.id }) { loan ->
                    MiniLoanRow(loan, onClick = onNavigateToLoans)
                }
            }
        }
    }
}

// ─── Net Worth Hero Card ──────────────────────────────────────────────────────
@Composable
fun NetWorthCard(netWorth: Double, invested: Double, currentValue: Double, gainPercent: Double) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(0.85f),
                        MaterialTheme.colorScheme.secondary.copy(0.65f)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Net Worth", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(0.8f))
            Text(
                formatAmount(netWorth),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Invested", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))
                    Text(formatAmount(invested), style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Current Value", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))
                    Text(formatAmount(currentValue), style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                if (gainPercent != 0.0) {
                    Column {
                        Text("Returns", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))
                        val isGain = gainPercent >= 0
                        Text(
                            "${if (isGain) "▲" else "▼"} ${"%.2f".format(Math.abs(gainPercent))}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isGain) Color(0xFF7EFFD4) else Color(0xFFFFB3BA),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AssetAllocationCard(data: List<Pair<SecurityType, Double>>) {
    val total = data.sumOf { it.second }
    if (total <= 0) return
    val colors = listOf(
        Color(0xFF00C896), Color(0xFF4A90E2), Color(0xFFFFAB00),
        Color(0xFFFF6B6B), Color(0xFF9B59B6), Color(0xFF1ABC9C), Color(0xFFE67E22)
    )
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Segmented bar
            Row(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))) {
                data.forEachIndexed { i, (_, v) ->
                    Box(modifier = Modifier.weight((v / total).toFloat()).fillMaxHeight().background(colors[i % colors.size]))
                }
            }
            // Legend
            data.forEachIndexed { i, (type, v) ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(colors[i % colors.size]))
                    Spacer(Modifier.width(8.dp))
                    Text(type.name.replace("_", " "), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text("${"%.1f".format((v / total) * 100)}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Text(formatAmount(v), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun MiniHoldingRow(holding: com.investtrack.data.repository.HoldingSummary) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(holding.security.securityName.take(2).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(holding.security.securityName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(holding.security.securityType.name.replace("_"," "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatAmount(holding.marketValue), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                GainLossBadge(holding.gainPercent)
            }
        }
    }
}

@Composable
fun MiniTransactionRow(txn: Transaction) {
    val isBuy = txn.transactionType in listOf(TransactionType.BUY, TransactionType.SIP, TransactionType.INVEST, TransactionType.DEPOSIT, TransactionType.PREMIUM)
    val amount = txn.amount ?: ((txn.units ?: 0.0) * (txn.price ?: 0.0))
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(
                if (isBuy) GainColor.copy(0.12f) else LossColor.copy(0.12f)
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isBuy) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                null,
                tint = if (isBuy) GainColor else LossColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(txn.transactionType.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(txn.transactionDate.toDisplayDate(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(formatAmount(amount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (isBuy) MaterialTheme.colorScheme.onSurface else LossColor)
    }
}

@Composable
fun MiniLoanRow(loan: com.investtrack.data.database.entities.Loan, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccountBalance, null, tint = LossColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(loan.loanName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text("${loan.interestRate}% • ${loan.tenureMonths}m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("EMI ${formatAmount(loan.emiAmount)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(formatAmount(loan.loanAmount), style = MaterialTheme.typography.labelSmall, color = LossColor)
            }
        }
    }
}
