package com.investtrack.ui.holdings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investtrack.data.database.entities.AssetClass
import com.investtrack.data.database.entities.SecurityMaster
import com.investtrack.data.database.entities.SecurityType
import com.investtrack.data.database.entities.TransactionType
import com.investtrack.data.repository.FamilyRepository
import com.investtrack.data.repository.HoldingSummary
import com.investtrack.data.repository.PortfolioRepository
import com.investtrack.data.repository.PriceRepository
import com.investtrack.data.repository.SecurityRepository
import com.investtrack.data.repository.TransactionRepository
import com.investtrack.ui.common.EmptyState
import com.investtrack.ui.common.PillChip
import com.investtrack.ui.common.SectionHeader
import com.investtrack.ui.common.TopBarWithBack
import com.investtrack.ui.dashboard.assetClassColor
import com.investtrack.ui.theme.GainColor
import com.investtrack.ui.theme.LossColor
import com.investtrack.utils.DateUtils.toDisplayDate
import com.investtrack.utils.FinancialUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HoldingsViewModel @Inject constructor(
    private val portfolioRepo: PortfolioRepository,
    private val transactionRepo: TransactionRepository,
    private val securityRepo: SecurityRepository,
    private val priceRepo: PriceRepository,
    private val familyRepo: FamilyRepository
) : ViewModel() {
    private val _holdings = MutableStateFlow(listOf<HoldingSummary>())
    val holdings: StateFlow<List<HoldingSummary>> = _holdings
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _filterType = MutableStateFlow<SecurityType?>(null)
    val filterType: StateFlow<SecurityType?> = _filterType
    private val _filterAssetClass = MutableStateFlow<AssetClass?>(null)

    val filteredHoldings = combine(_holdings, _filterType, _filterAssetClass) { h, type, ac ->
        h.filter { (type == null || it.securityType == type) && (ac == null || it.assetClass == ac) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { loadHoldings() }

    fun loadHoldings() {
        viewModelScope.launch {
            _isLoading.value = true
            _holdings.value = portfolioRepo.getHoldings()
            _isLoading.value = false
        }
    }

    fun setTypeFilter(type: SecurityType?) { _filterType.value = type }

    fun getTransactionsBySecurity(securityId: Long) = transactionRepo.getTransactionsBySecurity(securityId)
    fun getPriceHistory(securityId: Long) = priceRepo.getPriceHistory(securityId)
    suspend fun getSecurity(id: Long) = securityRepo.getSecurityById(id)
    fun getMembers() = familyRepo.getAllMembers()
}

@Composable
fun HoldingsScreen(onHoldingClick: (Long) -> Unit, onUpdatePrice: (Long) -> Unit, onBack: () -> Unit, vm: HoldingsViewModel = hiltViewModel()) {
    val holdings by vm.filteredHoldings.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val filterType by vm.filterType.collectAsState()
    val totalCost = holdings.sumOf { it.totalCost }
    val totalMV = holdings.sumOf { it.marketValue }
    val totalGain = totalMV - totalCost

    Scaffold(topBar = { TopBarWithBack("My Holdings", onBack) { IconButton(onClick = { vm.loadHoldings() }) { Icon(Icons.Default.Refresh, "Refresh") } } }) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (holdings.isNotEmpty()) {
                    item {
                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Portfolio Holdings", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary.copy(0.8f))
                                Text(FinancialUtils.formatCurrencyFull(totalMV), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("Cost: ${FinancialUtils.formatCurrency(totalCost)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(0.8f))
                                    Text("Gain: ${if (totalGain >= 0) "+" else ""}${FinancialUtils.formatCurrency(totalGain)}", color = if (totalGain >= 0) Color(0xFF80FF80) else Color(0xFFFF8080), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(selected = filterType == null, onClick = { vm.setTypeFilter(null) }, label = { Text("All") }) }
                        items(holdings.map { it.securityType }.distinct()) { type ->
                            FilterChip(selected = filterType == type, onClick = { vm.setTypeFilter(if (filterType == type) null else type) }, label = { Text(type.name.replace("_", " ")) })
                        }
                    }
                }
                if (holdings.isEmpty()) item { EmptyState("No holdings found. Add transactions to see your portfolio.") }
                items(holdings) { holding ->
                    HoldingCard(holding, onClick = { onHoldingClick(holding.securityId) }, onUpdatePrice = { onUpdatePrice(holding.securityId) })
                }
            }
        }
    }
}

@Composable
fun HoldingCard(holding: HoldingSummary, onClick: () -> Unit, onUpdatePrice: () -> Unit) {
    val gainColor = if (holding.unrealizedGain >= 0) GainColor else LossColor
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(holding.securityName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(holding.securityCode, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PillChip(holding.securityType.name.replace("_", " "), MaterialTheme.colorScheme.primary)
                        PillChip(holding.assetClass.name, assetClassColor(holding.assetClass))
                    }
                }
                IconButton(onClick = onUpdatePrice, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Update Price", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HoldingMetric("Units", if (holding.totalUnits > 0) "%.4f".format(holding.totalUnits) else "—")
                HoldingMetric("Avg Cost", if (holding.avgCostPrice > 0) "₹%.4f".format(holding.avgCostPrice) else "—")
                HoldingMetric("Curr Price", if (holding.currentPrice > 0) "₹%.4f".format(holding.currentPrice) else "—")
            }
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HoldingMetric("Invested", FinancialUtils.formatCurrency(holding.totalCost))
                HoldingMetric("Market Value", FinancialUtils.formatCurrency(holding.marketValue), MaterialTheme.colorScheme.primary)
                HoldingMetric("Gain/Loss", FinancialUtils.formatCurrency(holding.unrealizedGain), gainColor)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HoldingMetric("Abs Return", FinancialUtils.formatPercent(holding.absoluteReturn), gainColor)
                HoldingMetric("XIRR", holding.xirr?.let { FinancialUtils.formatPercent(it) } ?: "N/A", gainColor)
                HoldingMetric("CAGR", holding.cagr?.let { FinancialUtils.formatPercent(it) } ?: "N/A", gainColor)
            }
        }
    }
}

@Composable
fun HoldingMetric(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
fun HoldingDetailScreen(securityId: Long, onAddTransaction: (Long) -> Unit, onUpdatePrice: (Long) -> Unit, onBack: () -> Unit, vm: HoldingsViewModel = hiltViewModel()) {
    var security by remember { mutableStateOf<SecurityMaster?>(null) }
    val transactions by vm.getTransactionsBySecurity(securityId).collectAsState(initial = emptyList())
    val priceHistory by vm.getPriceHistory(securityId).collectAsState(initial = emptyList())
    val members by vm.getMembers().collectAsState(initial = emptyList())
    val holdings by vm.holdings.collectAsState()
    val holding = holdings.find { it.securityId == securityId }

    LaunchedEffect(securityId) { security = vm.getSecurity(securityId) }

    Scaffold(topBar = {
        TopBarWithBack(security?.securityName ?: "Holding", onBack) {
            IconButton(onClick = { onUpdatePrice(securityId) }) { Icon(Icons.Default.Edit, "Update Price") }
            IconButton(onClick = { onAddTransaction(securityId) }) { Icon(Icons.Default.Add, "Add Transaction") }
        }
    }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            holding?.let { h ->
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(h.securityName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("${h.securityCode} • ${h.securityType.name.replace("_", " ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                HoldingMetric("Total Units", if (h.totalUnits > 0) "%.4f".format(h.totalUnits) else "—")
                                HoldingMetric("Avg Cost", "₹%.4f".format(h.avgCostPrice))
                                HoldingMetric("Current Price", "₹%.4f".format(h.currentPrice), MaterialTheme.colorScheme.primary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                HoldingMetric("Invested", FinancialUtils.formatCurrencyFull(h.totalCost))
                                HoldingMetric("Market Value", FinancialUtils.formatCurrencyFull(h.marketValue), MaterialTheme.colorScheme.primary)
                            }
                            val gc = if (h.unrealizedGain >= 0) GainColor else LossColor
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                HoldingMetric("Gain/Loss", "${if (h.unrealizedGain >= 0) "+" else ""}${FinancialUtils.formatCurrencyFull(h.unrealizedGain)}", gc)
                                HoldingMetric("Abs Return", FinancialUtils.formatPercent(h.absoluteReturn), gc)
                                HoldingMetric("XIRR", h.xirr?.let { FinancialUtils.formatPercent(it) } ?: "N/A", gc)
                            }
                        }
                    }
                }
            }
            if (priceHistory.isNotEmpty()) {
                item { SectionHeader("Recent Prices") { TextButton(onClick = { onUpdatePrice(securityId) }) { Text("Update") } } }
                items(priceHistory.take(5)) { ph ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(ph.priceDate.toDisplayDate(), style = MaterialTheme.typography.bodySmall)
                        Text("₹${"%.4f".format(ph.price)}", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item { SectionHeader("Transaction History") { TextButton(onClick = { onAddTransaction(securityId) }) { Text("Add") } } }
            if (transactions.isEmpty()) item { EmptyState("No transactions yet.") }
            items(transactions) { txn ->
                val memberName = members.find { it.id == txn.familyMemberId }?.name ?: "Unknown"
                val isBuy = txn.transactionType in listOf(TransactionType.BUY, TransactionType.SIP, TransactionType.INVEST)
                val amount = txn.amount ?: ((txn.units ?: 0.0) * (txn.price ?: 0.0))
                Card(shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${txn.transactionType.name} • $memberName", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            Text(txn.transactionDate.toDisplayDate(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            txn.units?.let { Text("${"%.4f".format(it)} units @ ₹${"%.4f".format(txn.price ?: 0.0)}", style = MaterialTheme.typography.labelSmall) }
                        }
                        Text(FinancialUtils.formatCurrency(amount), fontWeight = FontWeight.Bold, color = if (isBuy) MaterialTheme.colorScheme.primary else LossColor)
                    }
                }
            }
        }
    }
}
