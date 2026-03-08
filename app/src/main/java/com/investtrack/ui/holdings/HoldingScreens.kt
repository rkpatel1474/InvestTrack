package com.investtrack.ui.holdings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investtrack.data.repository.HoldingSummary
import com.investtrack.data.repository.PortfolioRepository
import com.investtrack.data.repository.PortfolioSummary
import com.investtrack.data.repository.TransactionRepository
import com.investtrack.ui.common.*
import com.investtrack.ui.theme.GainColor
import com.investtrack.ui.theme.LossColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HoldingsViewModel @Inject constructor(
    private val portfolioRepo: PortfolioRepository,
    private val txnRepo: TransactionRepository
) : ViewModel() {
    private val _summary = MutableStateFlow<PortfolioSummary?>(null)
    val summary = _summary.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Listen for any transaction changes to auto-refresh
    private val txnChangeTrigger = txnRepo.getRecentTransactions(1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refresh()
        // Auto-refresh whenever transactions change
        viewModelScope.launch {
            txnChangeTrigger.collect { refresh() }
        }
    }

    fun refresh() = viewModelScope.launch {
        _summary.value = portfolioRepo.getPortfolioSummary()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoldingsScreen(onAddTransaction: (Long) -> Unit, onBack: () -> Unit, vm: HoldingsViewModel = hiltViewModel()) {
    val summary by vm.summary.collectAsState()
    var filter by remember { mutableStateOf("ALL") }
    val filters = listOf("ALL","MF","SHARES","FD","BONDS","NPS","GOLD","OTHER")

    Scaffold(
        topBar = {
            TopBarWithBack("Portfolio", onBack) {
                IconButton(onClick = { vm.refresh() }) { Icon(Icons.Default.Refresh, null) }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            summary?.let { s ->
                item {
                    GradientHeaderCard(
                        title = "Total Portfolio Value",
                        value = formatAmount(s.totalMarketValue),
                        subtitle = "Invested: ${formatAmount(s.totalCost)}",
                        gainLoss = s.absoluteReturn,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                item {
                    Row(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("P&L", formatAmount(s.totalGain), modifier = Modifier.weight(1f),
                            valueColor = if (s.totalGain >= 0) GainColor else LossColor)
                        StatCard("Return", "${"%.2f".format(s.absoluteReturn)}%", modifier = Modifier.weight(1f),
                            valueColor = if (s.absoluteReturn >= 0) GainColor else LossColor)
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Filter chips
                item {
                    androidx.compose.foundation.lazy.LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        items(filters) { f ->
                            FilterChip(
                                selected = filter == f,
                                onClick = { filter = f },
                                label = { Text(f, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                val filtered = if (filter == "ALL") vm.holdings.value
                else vm.holdings.value.filter { h ->
                    when (filter) {
                        "MF"     -> h.securityType.name == "MUTUAL_FUND"
                        "SHARES" -> h.securityType.name == "SHARES"
                        "FD"     -> h.securityType.name == "FD"
                        "BONDS"  -> h.securityType.name in listOf("BOND","GOI_BOND")
                        "NPS"    -> h.securityType.name in listOf("NPS","PF")
                        "GOLD"   -> h.securityType.name == "GOLD"
                        else     -> h.securityType.name in listOf("INSURANCE","PROPERTY","CRYPTO","OTHER")
                        }
                }

                if (filtered.isEmpty()) {
                    item { EmptyState("No holdings for this filter.") }
                } else {
                    items(filtered.sortedByDescending { it.marketValue }, key = { h -> h.securityId }) { h ->
                        HoldingCard(h, onClick = { onAddTransaction(h.securityId) }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    }
                }
            } ?: run {
                item {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun HoldingCard(h: HoldingSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val isGain = h.unrealizedGain >= 0
    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(h.securityName.take(2).uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(h.securityName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    PillChip(h.securityType.name.replace("_"," "), MaterialTheme.colorScheme.secondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatAmount(h.marketValue), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    GainLossBadge(h.absoluteReturn)
                }
            }
            Divider(color = MaterialTheme.colorScheme.outline.copy(0.2f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoCol("Units", "${"%.4f".format(h.totalUnits)}")
                InfoCol("Avg Price", formatAmount(h.avgCostPrice))
                InfoCol("Invested", formatAmount(h.totalCost))
                InfoCol("P&L", formatAmount(h.unrealizedGain), if (isGain) GainColor else LossColor)
            }
        }
    }
}

@Composable
fun InfoCol(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
