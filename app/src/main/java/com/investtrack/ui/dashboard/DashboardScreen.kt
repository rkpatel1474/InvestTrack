package com.investtrack.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investtrack.data.database.entities.*
import com.investtrack.data.repository.*
import com.investtrack.ui.common.*
import com.investtrack.ui.theme.*
import com.investtrack.utils.DateUtils.toDisplayDate
import com.investtrack.utils.FinancialUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*

// ─── ViewModel ────────────────────────────────────────────────────────────────
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val portfolioRepo: PortfolioRepository,
    private val txnRepo:       TransactionRepository,
    private val loanRepo:      LoanRepository,
    private val familyRepo:    FamilyRepository
) : ViewModel() {

    val recentTxns = txnRepo.getRecentTransactions(8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allLoans   = loanRepo.getAllLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allMembers = familyRepo.getAllMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _summary = MutableStateFlow<PortfolioSummary?>(null)
    val summary = _summary.asStateFlow()

    init {
        // Auto-refresh whenever transactions change
        viewModelScope.launch {
            txnRepo.getRecentTransactions(1).collect { refresh() }
        }
    }

    fun refresh() = viewModelScope.launch { _summary.value = portfolioRepo.getPortfolioSummary() }
}

// ─── Dashboard Screen ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToPortfolio:     () -> Unit,
    onNavigateToTransactions:  () -> Unit,
    onNavigateToLoans:         () -> Unit,
    vm: DashboardViewModel = hiltViewModel()
) {
    val summary    by vm.summary.collectAsState()
    val recentTxns by vm.recentTxns.collectAsState()
    val allLoans   by vm.allLoans.collectAsState()
    val allMembers by vm.allMembers.collectAsState()

    val totalLoanAmt = allLoans.sumOf { it.loanAmount }
    val totalEMI     = allLoans.sumOf { it.emiAmount }
    val netWorth     = (summary?.totalMarketValue ?: 0.0) - totalLoanAmt

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary.copy(0.15f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ShowChart, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text("InvestTrack", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                            Text("Portfolio Overview", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── 1. HERO METRICS CARD ────────────────────────────────────────
            item { HeroMetricsCard(summary, netWorth, totalLoanAmt) }

            // ── 2. QUICK STATS ROW ──────────────────────────────────────────
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickStatTile("Invested",   formatAmount(summary?.totalCost ?: 0.0),      Icons.Default.TrendingUp,     MaterialTheme.colorScheme.primary,   Modifier.weight(1f))
                    QuickStatTile("P&L",        formatAmount(summary?.totalGain ?: 0.0),       Icons.Default.BarChart,       if ((summary?.totalGain ?: 0.0) >= 0) GainColor else LossColor, Modifier.weight(1f))
                    QuickStatTile("EMI/month",  formatAmount(totalEMI),                        Icons.Default.CalendarToday,  LossColor,                           Modifier.weight(1f))
                }
            }

            // ── 3. RETURN METRICS ───────────────────────────────────────────
            summary?.let { s ->
                if (s.totalCost > 0) {
                    item { ReturnMetricsCard(s) }
                }
            }

            // ── 4. ASSET ALLOCATION PIE ─────────────────────────────────────
            summary?.let { s ->
                if (s.holdings.isNotEmpty()) {
                    item {
                        SectionHeader("Asset Allocation", "Portfolio →") { onNavigateToPortfolio() }
                        Spacer(Modifier.height(6.dp))
                        AllocationPieCard(
                            data = s.holdings
                                .groupBy { it.security.assetClass }
                                .map { (cls, h) -> cls.name.replace("_"," ") to h.sumOf { it.marketValue } }
                                .sortedByDescending { it.second }
                        )
                    }
                }
            }

            // ── 5. SECURITY TYPE BREAKDOWN ──────────────────────────────────
            summary?.let { s ->
                if (s.holdings.isNotEmpty()) {
                    item {
                        SectionHeader("By Instrument Type")
                        Spacer(Modifier.height(6.dp))
                        SecurityTypeBarCard(
                            s.holdings.groupBy { it.security.securityType }
                                .map { (t, h) -> t to h.sumOf { it.marketValue } }
                                .sortedByDescending { it.second }
                        )
                    }
                }
            }

            // ── 6. FAMILY MEMBER BIFURCATION ───────────────────────────────
            if (allMembers.size > 1 && (summary?.holdings?.isNotEmpty() == true)) {
                item {
                    SectionHeader("Family Portfolio Split")
                    Spacer(Modifier.height(6.dp))
                    FamilyBifurcationCard(allMembers, summary!!)
                }
            }

            // ── 7. TOP HOLDINGS ─────────────────────────────────────────────
            summary?.let { s ->
                if (s.holdings.isNotEmpty()) {
                    item { SectionHeader("Top Holdings", "All →") { onNavigateToPortfolio() } }
                    items(s.holdings.sortedByDescending { it.marketValue }.take(5), key = { it.security.id }) { h ->
                        HoldingMiniCard(h)
                    }
                }
            }

            // ── 8. RECENT ACTIVITY ──────────────────────────────────────────
            if (recentTxns.isNotEmpty()) {
                item { SectionHeader("Recent Activity", "All →") { onNavigateToTransactions() } }
                items(recentTxns.take(4), key = { it.id }) { txn ->
                    TxnMiniRow(txn)
                }
            }

            // ── 9. ACTIVE LOANS ─────────────────────────────────────────────
            if (allLoans.isNotEmpty()) {
                item { SectionHeader("Liabilities", "All →") { onNavigateToLoans() } }
                items(allLoans.take(3), key = { it.id }) { loan ->
                    LoanMiniCard(loan, onClick = onNavigateToLoans)
                }
            }
        }
    }
}

// ─── Hero Metrics Card ────────────────────────────────────────────────────────
@Composable
fun HeroMetricsCard(summary: PortfolioSummary?, netWorth: Double, totalLoanAmt: Double) {
    val animNet by animateFloatAsState(netWorth.toFloat(), tween(800), label = "net")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(0.9f),
                        MaterialTheme.colorScheme.secondary.copy(0.7f)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("Net Worth", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(0.7f))
                    Text(formatAmount(netWorth), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                val gain = summary?.totalGain ?: 0.0
                val gainPct = summary?.gainPercent ?: 0.0
                if (gain != 0.0) {
                    Surface(color = (if (gain >= 0) Color(0xFF00C853) else Color(0xFFFF1744)).copy(0.25f), shape = RoundedCornerShape(10.dp)) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.End) {
                            Text(if (gain >= 0) "▲" else "▼", color = Color.White, style = MaterialTheme.typography.labelSmall)
                            Text(formatAmount(gain), style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${"%.2f".format(gainPct)}%", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.8f))
                        }
                    }
                }
            }
            Divider(color = Color.White.copy(0.2f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HeroStat("Invested",      formatAmount(summary?.totalCost ?: 0.0))
                HeroStat("Market Value",  formatAmount(summary?.totalMarketValue ?: 0.0))
                HeroStat("Liabilities",   formatAmount(totalLoanAmt))
            }
        }
    }
}

@Composable
fun HeroStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

// ─── Quick Stat Tile ──────────────────────────────────────────────────────────
@Composable
fun QuickStatTile(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─── Return Metrics Card ──────────────────────────────────────────────────────
@Composable
fun ReturnMetricsCard(s: PortfolioSummary) {
    val absReturn = s.totalGain
    val pctReturn = s.gainPercent
    val isPos     = absReturn >= 0

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Return Metrics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("Cost of Holdings", formatAmount(s.totalCost), MaterialTheme.colorScheme.onSurface)
                MetricItem("Market Value",     formatAmount(s.totalMarketValue), MaterialTheme.colorScheme.primary)
                MetricItem("Abs. Gain/Loss",   formatAmount(absReturn), if (isPos) GainColor else LossColor)
                MetricItem("Return %",         "${"%.2f".format(pctReturn)}%", if (isPos) GainColor else LossColor)
            }
            // XIRR placeholder row
            Divider(color = MaterialTheme.colorScheme.outline.copy(0.2f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("XIRR (Est.)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("—", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Holdings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${s.holdings.size}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

// ─── Allocation Pie Chart Card ─────────────────────────────────────────────────
@Composable
fun AllocationPieCard(data: List<Pair<String, Double>>) {
    if (data.isEmpty()) return
    val total = data.sumOf { it.second }
    val pieColors = listOf(
        Color(0xFF00C896), Color(0xFF4A90E2), Color(0xFFFFAB00),
        Color(0xFFFF6B6B), Color(0xFF9B59B6), Color(0xFF1ABC9C),
        Color(0xFFE67E22), Color(0xFF3498DB)
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Pie chart
            DonutChart(
                data = data.map { (it.second / total).toFloat() },
                colors = pieColors.take(data.size),
                modifier = Modifier.size(120.dp)
            )
            Spacer(Modifier.width(16.dp))
            // Legend
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                data.forEachIndexed { i, (name, value) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(pieColors[i % pieColors.size]))
                        Spacer(Modifier.width(6.dp))
                        Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        Text("${"%.1f".format((value / total) * 100)}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = pieColors[i % pieColors.size])
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(data: List<Float>, colors: List<Color>, modifier: Modifier = Modifier) {
    val animProgress by animateFloatAsState(1f, tween(1000, easing = FastOutSlowInEasing), label = "pie")
    Canvas(modifier = modifier) {
        val stroke   = 28.dp.toPx()
        val diameter = size.minDimension
        val radius   = (diameter - stroke) / 2
        val center   = Offset(size.width / 2, size.height / 2)
        val rect     = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)

        var startAngle = -90f
        data.forEachIndexed { i, fraction ->
            val sweep = (fraction * 360f * animProgress)
            drawArc(color = colors.getOrElse(i) { Color.Gray }, startAngle = startAngle, sweepAngle = sweep - 1f,
                useCenter = false, topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                style = Stroke(width = stroke, cap = StrokeCap.Round))
            startAngle += sweep
        }
    }
}

// ─── Security Type Horizontal Bar ─────────────────────────────────────────────
@Composable
fun SecurityTypeBarCard(data: List<Pair<SecurityType, Double>>) {
    if (data.isEmpty()) return
    val total = data.sumOf { it.second }
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Stacked bar
            Row(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
                data.forEachIndexed { i, (type, v) ->
                    Box(modifier = Modifier.weight((v / total).toFloat()).fillMaxHeight().background(secTypeColor(type)))
                }
            }
            // Rows
            data.forEach { (type, value) ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(secTypeColor(type)))
                    Spacer(Modifier.width(8.dp))
                    Text(type.name.replace("_"," "), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text(formatAmount(value), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Text("${"%.1f".format((value / total) * 100)}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ─── Family Bifurcation Table ─────────────────────────────────────────────────
@Composable
fun FamilyBifurcationCard(members: List<com.investtrack.data.database.entities.FamilyMember>, summary: PortfolioSummary) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Member", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Text("Invested", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                Text("Value", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                Text("P&L%", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
            Divider(color = MaterialTheme.colorScheme.outline.copy(0.3f))
            // Simplified: show equal split per member (real impl would query per member)
            val perMember = if (members.isEmpty()) 1 else members.size
            members.forEach { m ->
                val mCost = summary.totalCost / perMember
                val mMV   = summary.totalMarketValue / perMember
                val mPct  = if (mCost > 0) (mMV - mCost) / mCost * 100 else 0.0
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(m.name, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(formatAmount(mCost), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    Text(formatAmount(mMV), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    Text("${"%.1f".format(mPct)}%", modifier = Modifier.weight(0.8f),
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold,
                        color = if (mPct >= 0) GainColor else LossColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
            }
        }
    }
}

// ─── Holding Mini Card ────────────────────────────────────────────────────────
@Composable
fun HoldingMiniCard(h: HoldingSummary) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(secTypeColor(h.security.securityType).copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(h.security.securityName.take(2).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = secTypeColor(h.security.securityType))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(h.security.securityName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(h.security.securityType.name.replace("_"," "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatAmount(h.marketValue), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            GainLossBadge(h.gainPercent)
        }
    }
}

// ─── Transaction Mini Row ─────────────────────────────────────────────────────
@Composable
fun TxnMiniRow(txn: Transaction) {
    val isBuy   = txn.transactionType.name in listOf("BUY","SIP","INVEST","DEPOSIT","PREMIUM","BONUS")
    val amount  = txn.amount ?: ((txn.units ?: 0.0) * (txn.price ?: 0.0))
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(
                if (isBuy) GainColor.copy(0.12f) else LossColor.copy(0.12f)
            ), contentAlignment = Alignment.Center
        ) {
            Icon(if (isBuy) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                null, tint = if (isBuy) GainColor else LossColor, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(txn.transactionType.name.replace("_"," "), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(txn.transactionDate.toDisplayDate(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(formatAmount(amount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (isBuy) MaterialTheme.colorScheme.onSurface else LossColor)
    }
}

// ─── Loan Mini Card ───────────────────────────────────────────────────────────
@Composable
fun LoanMiniCard(loan: Loan, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).clickable { onClick() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(LossColor.copy(0.12f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AccountBalance, null, tint = LossColor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(loan.loanName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("${loan.interestRate}% • ${loan.tenureMonths}m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("EMI ${formatAmount(loan.emiAmount)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(formatAmount(loan.loanAmount), style = MaterialTheme.typography.labelSmall, color = LossColor)
        }
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
