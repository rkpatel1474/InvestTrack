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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investtrack.data.database.entities.*
import com.investtrack.data.repository.*
import com.investtrack.ui.theme.GainColor
import com.investtrack.ui.theme.LossColor
import com.investtrack.utils.DateUtils.toDisplayDate
import com.investtrack.utils.FinancialUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── Data classes ─────────────────────────────────────────────────────────────
data class DashboardUiState(
    val isLoading: Boolean = true,
    val portfolioSummary: PortfolioSummary? = null,
    val recentTransactions: List<RecentTxn> = emptyList(),
    val totalLoanOutstanding: Double = 0.0,
    val totalMonthlyEMI: Double = 0.0,
    val memberBreakdown: List<MemberPortfolioRow> = emptyList()
)

data class RecentTxn(
    val transaction: Transaction,
    val securityName: String,
    val memberName: String
)

data class MemberPortfolioRow(
    val memberId: Long,
    val memberName: String,
    val invested: Double,
    val marketValue: Double,
    val gain: Double,
    val gainPercent: Double
)

// ─── ViewModel ────────────────────────────────────────────────────────────────
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

    init {
        loadDashboard()
        // Auto-refresh when transactions change
        viewModelScope.launch {
            transactionRepository.getRecentTransactions(1).collect { loadDashboard() }
        }
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val summary     = portfolioRepository.getPortfolioSummary()
                val recentTxns  = buildRecentTransactions()
                val loanTotals  = getLoanTotals()
                val breakdown   = buildMemberBreakdown()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        portfolioSummary = summary,
                        recentTransactions = recentTxns,
                        totalLoanOutstanding = loanTotals.first,
                        totalMonthlyEMI = loanTotals.second,
                        memberBreakdown = breakdown
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun buildRecentTransactions(): List<RecentTxn> {
        return try {
            val txns = transactionRepository.getRecentTransactions(5).first()
            val allMembers = familyRepository.getAllMembers().first()
            txns.mapNotNull { txn ->
                // Guard: skip if securityId is 0 or invalid
                if (txn.securityId <= 0L) return@mapNotNull null
                val sec    = securityRepository.getSecurityById(txn.securityId)
                val member = allMembers.find { it.id == txn.familyMemberId }
                RecentTxn(txn, sec?.securityName ?: "Unknown", member?.name ?: "Unknown")
            }
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun getLoanTotals(): Pair<Double, Double> {
        return try {
            val loans = loanRepository.getAllLoans().first()
            var outstanding = 0.0
            var emi = 0.0
            for (loan in loans) {
                outstanding += (loan.loanAmount - loanRepository.getTotalPrincipalPaid(loan.id)).coerceAtLeast(0.0)
                emi += loan.emiAmount
            }
            outstanding to emi
        } catch (e: Exception) { 0.0 to 0.0 }
    }

    // ── Per-member portfolio breakdown ────────────────────────────────────────
    private suspend fun buildMemberBreakdown(): List<MemberPortfolioRow> {
        return try {
            val members = familyRepository.getAllMembers().first()
            if (members.size <= 1) return emptyList()

            members.mapNotNull { member ->
                val holdings = portfolioRepository.getHoldings(member.id)
                if (holdings.isEmpty()) return@mapNotNull null
                val invested = holdings.sumOf { it.totalCost }
                val mv       = holdings.sumOf { it.marketValue }
                val gain     = mv - invested
                val pct      = if (invested > 0) (gain / invested) * 100 else 0.0
                MemberPortfolioRow(member.id, member.name, invested, mv, gain, pct)
            }
        } catch (e: Exception) { emptyList() }
    }
}

// ─── Dashboard Screen ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToHoldings: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToSecurity: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppLogoIcon()
                    Column {
                        Text("InvestTrack", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text("Portfolio Dashboard", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onNavigateToAddTransaction) {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.loadDashboard() }) {
                        Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (uiState.isLoading) {
            item { Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        } else {
            // ── Hero Card ────────────────────────────────────────────────────
            uiState.portfolioSummary?.let { s ->
                item { HeroCard(s, uiState.totalLoanOutstanding) }

                // ── Metric Row ───────────────────────────────────────────────
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricTile("Invested", FinancialUtils.formatCurrency(s.totalCost), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        MetricTile("P&L", FinancialUtils.formatCurrency(s.totalGain), if (s.totalGain >= 0) GainColor else LossColor, Modifier.weight(1f))
                        MetricTile("Return", "%.2f%%".format(s.absoluteReturn), if (s.absoluteReturn >= 0) GainColor else LossColor, Modifier.weight(1f))
                    }
                }

                // ── Return detail card ───────────────────────────────────────
                item { ReturnDetailCard(s) }

                // ── Asset allocation donut ───────────────────────────────────
                if (s.assetClassBreakdown.isNotEmpty()) {
                    item {
                        SectionHeader("Asset Allocation") { TextButton(onClick = onNavigateToHoldings) { Text("View All") } }
                        Spacer(Modifier.height(4.dp))
                        AllocationCard(s.assetClassBreakdown)
                    }
                }

                // ── Security type chips ──────────────────────────────────────
                if (s.securityTypeBreakdown.isNotEmpty()) {
                    item {
                        SectionHeader("By Instrument")
                        Spacer(Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(s.securityTypeBreakdown.entries.sortedByDescending { it.value }.toList(), key = { it.key.name }) { (type, value) ->
                                InstrumentChip(type, value)
                            }
                        }
                    }
                }
            }

            // ── Loan summary ─────────────────────────────────────────────────
            if (uiState.totalLoanOutstanding > 0 || uiState.totalMonthlyEMI > 0) {
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = LossColor.copy(0.08f))) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            LoanStat("Loan Outstanding", FinancialUtils.formatCurrency(uiState.totalLoanOutstanding))
                            LoanStat("Monthly EMI", FinancialUtils.formatCurrency(uiState.totalMonthlyEMI))
                        }
                    }
                }
            }

            // ── Family member breakdown ──────────────────────────────────────
            if (uiState.memberBreakdown.size > 1) {
                item {
                    SectionHeader("Family Portfolio")
                    Spacer(Modifier.height(4.dp))
                    FamilyBreakdownCard(uiState.memberBreakdown)
                }
            }

            // ── Recent transactions ──────────────────────────────────────────
            if (uiState.recentTransactions.isNotEmpty()) {
                item { SectionHeader("Recent Activity") { TextButton(onClick = onNavigateToTransactions) { Text("All") } } }
                // Use index-based key to avoid duplicate key crashes
                items(uiState.recentTransactions, key = { it.transaction.id }) { recent ->
                    RecentTxnRow(
                        recent = recent,
                        onClick = {
                            // Guard: only navigate if valid securityId
                            if (recent.transaction.securityId > 0L) {
                                onNavigateToSecurity(recent.transaction.securityId)
                            }
                        }
                    )
                }
            }
        }
    }
}

// ─── Hero Card ────────────────────────────────────────────────────────────────
@Composable
fun HeroCard(s: PortfolioSummary, totalLoan: Double) {
    val netWorth = s.totalMarketValue - totalLoan
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(0.9f), MaterialTheme.colorScheme.secondary.copy(0.7f))))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Net Worth", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(0.7f))
            Text(FinancialUtils.formatCurrencyFull(netWorth), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Divider(color = Color.White.copy(0.2f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HeroStat("Market Value", FinancialUtils.formatCurrency(s.totalMarketValue))
                HeroStat("Invested", FinancialUtils.formatCurrency(s.totalCost))
                HeroStat("Gain/Loss", "${if (s.totalGain >= 0) "+" else ""}${FinancialUtils.formatCurrency(s.totalGain)}", if (s.totalGain >= 0) Color(0xFF7EFFD4) else Color(0xFFFFB3BA))
            }
        }
    }
}

@Composable
fun HeroStat(label: String, value: String, valueColor: Color = Color.White) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

// ─── Metric Tile ──────────────────────────────────────────────────────────────
@Composable
fun MetricTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─── Return Detail Card ───────────────────────────────────────────────────────
@Composable
fun ReturnDetailCard(s: PortfolioSummary) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Return Metrics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ReturnItem("Cost", FinancialUtils.formatCurrency(s.totalCost), MaterialTheme.colorScheme.onSurface)
                ReturnItem("Value", FinancialUtils.formatCurrency(s.totalMarketValue), MaterialTheme.colorScheme.primary)
                ReturnItem("Abs Gain", FinancialUtils.formatCurrency(s.totalGain), if (s.totalGain >= 0) GainColor else LossColor)
                ReturnItem("Return %", "%.2f%%".format(s.absoluteReturn), if (s.absoluteReturn >= 0) GainColor else LossColor)
            }
            Divider(color = MaterialTheme.colorScheme.outline.copy(0.2f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ReturnItem("XIRR", s.xirr?.let { "%.2f%%".format(it) } ?: "—", if ((s.xirr ?: 0.0) >= 0) GainColor else LossColor)
                ReturnItem("CAGR", s.cagr?.let { "%.2f%%".format(it) } ?: "—", if ((s.cagr ?: 0.0) >= 0) GainColor else LossColor)
                ReturnItem("Holdings", "${s.assetClassBreakdown.values.count()}", MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ReturnItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold, color = color, textAlign = TextAlign.Center)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

// ─── Allocation Donut Card ────────────────────────────────────────────────────
val PIE_COLORS = listOf(
    Color(0xFF00C896), Color(0xFF4A90E2), Color(0xFFFFAB00),
    Color(0xFFFF6B6B), Color(0xFF9B59B6), Color(0xFF1ABC9C),
    Color(0xFFE67E22), Color(0xFF3498DB)
)

@Composable
fun AllocationCard(breakdown: Map<AssetClass, AssetClassSummary>) {
    val entries = breakdown.values.sortedByDescending { it.marketValue }
    val total   = entries.sumOf { it.marketValue }.takeIf { it > 0 } ?: 1.0
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(110.dp)) {
                val stroke = 26.dp.toPx()
                val r = (size.minDimension - stroke) / 2
                val cx = size.width / 2; val cy = size.height / 2
                var start = -90f
                entries.forEachIndexed { i, ac ->
                    val sweep = (ac.marketValue / total * 360f).toFloat()
                    drawArc(
                        color = PIE_COLORS[i % PIE_COLORS.size],
                        startAngle = start, sweepAngle = sweep - 1f, useCenter = false,
                        topLeft = Offset(cx - r, cy - r),
                        size = Size(r * 2, r * 2),
                        style = Stroke(width = stroke, cap = StrokeCap.Butt)
                    )
                    start += sweep
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.weight(1f)) {
                entries.forEachIndexed { i, ac ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PIE_COLORS[i % PIE_COLORS.size]))
                        Spacer(Modifier.width(6.dp))
                        Text(ac.assetClass.name.replace("_"," "), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        Text("%.1f%%".format(ac.percentage), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PIE_COLORS[i % PIE_COLORS.size])
                    }
                }
            }
        }
    }
}

// ─── Instrument Chip ──────────────────────────────────────────────────────────
@Composable
fun InstrumentChip(type: SecurityType, value: Double) {
    val color = secTypeColor(type)
    Surface(shape = RoundedCornerShape(12.dp), color = color.copy(0.1f)) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(type.name.replace("_"," "), style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
            Text(FinancialUtils.formatCurrency(value), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Family Breakdown Card ────────────────────────────────────────────────────
@Composable
fun FamilyBreakdownCard(rows: List<MemberPortfolioRow>) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Table header
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Member",   modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Text("Invested", modifier = Modifier.weight(1f),   style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                Text("Value",    modifier = Modifier.weight(1f),   style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                Text("P&L%",    modifier = Modifier.weight(0.8f),  style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }
            Divider(color = MaterialTheme.colorScheme.outline.copy(0.3f))
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(row.memberName, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(FinancialUtils.formatCurrency(row.invested),     modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                    Text(FinancialUtils.formatCurrency(row.marketValue),  modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
                    Text("%.1f%%".format(row.gainPercent),
                        modifier = Modifier.weight(0.8f),
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold,
                        color = if (row.gainPercent >= 0) GainColor else LossColor,
                        textAlign = TextAlign.End)
                }
            }
        }
    }
}

// ─── Loan Stat ────────────────────────────────────────────────────────────────
@Composable
fun LoanStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = LossColor)
    }
}

// ─── Recent Transaction Row ───────────────────────────────────────────────────
@Composable
fun RecentTxnRow(recent: RecentTxn, onClick: () -> Unit) {
    val txn     = recent.transaction
    val isDebit = txn.transactionType in listOf(TransactionType.BUY, TransactionType.SIP, TransactionType.INVEST, TransactionType.DEPOSIT, TransactionType.PREMIUM)
    val amount  = txn.amount ?: ((txn.units ?: 0.0) * (txn.price ?: 0.0))
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape)
                .background(if (isDebit) GainColor.copy(0.12f) else LossColor.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SwapHoriz, null, tint = if (isDebit) GainColor else LossColor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(recent.securityName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("${txn.transactionType.name.replace("_"," ")} • ${recent.memberName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(txn.transactionDate.toDisplayDate(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "${if (!isDebit) "+" else "-"}${FinancialUtils.formatCurrency(amount)}",
            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold,
            color = if (!isDebit) GainColor else MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─── App Logo Icon ────────────────────────────────────────────────────────────
@Composable
fun AppLogoIcon() {
    Box(
        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.ShowChart, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
fun secTypeColor(type: SecurityType): Color = when (type) {
    SecurityType.MUTUAL_FUND -> Color(0xFF4A90E2)
    SecurityType.SHARES      -> Color(0xFF00C896)
    SecurityType.BOND        -> Color(0xFF9B59B6)
    SecurityType.GOI_BOND    -> Color(0xFF1ABC9C)
    SecurityType.NPS         -> Color(0xFFE67E22)
    SecurityType.PF          -> Color(0xFF3498DB)
    SecurityType.FD          -> Color(0xFFFFAB00)
    SecurityType.INSURANCE   -> Color(0xFFE74C3C)
    SecurityType.PROPERTY    -> Color(0xFF795548)
    SecurityType.GOLD        -> Color(0xFFFFBF00)
    SecurityType.CRYPTO      -> Color(0xFFFF6B35)
    SecurityType.OTHER       -> Color(0xFF95A5A6)
}

fun assetClassColor(ac: AssetClass): Color = when (ac) {
    AssetClass.EQUITY       -> Color(0xFF1565C0)
    AssetClass.DEBT         -> Color(0xFF558B2F)
    AssetClass.HYBRID       -> Color(0xFF6A1B9A)
    AssetClass.REAL_ESTATE  -> Color(0xFFBF360C)
    AssetClass.GOLD         -> Color(0xFFF9A825)
    AssetClass.CASH         -> Color(0xFF00695C)
    AssetClass.COMMODITY    -> Color(0xFF4E342E)
    else                    -> Color(0xFF546E7A)
}

fun securityTypeIcon(type: SecurityType): ImageVector = when (type) {
    SecurityType.MUTUAL_FUND -> Icons.Default.PieChart
    SecurityType.SHARES      -> Icons.Default.TrendingUp
    SecurityType.BOND, SecurityType.GOI_BOND -> Icons.Default.AccountBalance
    SecurityType.NPS         -> Icons.Default.Elderly
    SecurityType.PF          -> Icons.Default.Work
    SecurityType.FD          -> Icons.Default.Savings
    SecurityType.INSURANCE   -> Icons.Default.Security
    SecurityType.PROPERTY    -> Icons.Default.Home
    SecurityType.GOLD        -> Icons.Default.Star
    else                     -> Icons.Default.AttachMoney
}
