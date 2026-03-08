#!/bin/bash
cd /workspaces/InvestTrack

# ══════════════════════════════════════════════════════════════════
# FILE 1: FamilyScreens.kt — complete clean rewrite
# ══════════════════════════════════════════════════════════════════
cat > app/src/main/java/com/investtrack/ui/family/FamilyScreens.kt << 'EOF'
package com.investtrack.ui.family

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investtrack.data.database.entities.FamilyMember
import com.investtrack.data.database.entities.Nominee
import com.investtrack.data.database.entities.Relationship
import com.investtrack.data.repository.FamilyRepository
import com.investtrack.ui.common.DateField
import com.investtrack.ui.common.DropdownField
import com.investtrack.ui.common.EmptyState
import com.investtrack.ui.common.InputField
import com.investtrack.ui.common.TopBarWithBack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FamilyViewModel @Inject constructor(private val repo: FamilyRepository) : ViewModel() {
    val members: StateFlow<List<FamilyMember>> =
        repo.getAllMembers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getNominees(memberId: Long) = repo.getNomineesForMember(memberId)

    fun saveMemberWithNominees(member: FamilyMember, nominees: List<Nominee>, onDone: () -> Unit) {
        viewModelScope.launch {
            val id = if (member.id == 0L) repo.insertMember(member)
                     else { repo.updateMember(member); member.id }
            repo.replaceNominees(id, nominees)
            onDone()
        }
    }

    fun deleteMember(id: Long) = viewModelScope.launch { repo.deleteMember(id) }
    suspend fun getMember(id: Long) = repo.getMemberById(id)
}

// Navigation.kt calls: onAddMember / onEditMember
@Composable
fun FamilyListScreen(
    onAddMember: () -> Unit,
    onEditMember: (Long) -> Unit,
    onBack: () -> Unit,
    vm: FamilyViewModel = hiltViewModel()
) {
    val members by vm.members.collectAsState()
    Scaffold(
        topBar = {
            TopBarWithBack("Family Members", onBack) {
                IconButton(onClick = onAddMember) { Icon(Icons.Default.Add, "Add") }
            }
        },
        floatingActionButton = { FloatingActionButton(onClick = onAddMember) { Icon(Icons.Default.Add, null) } }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (members.isEmpty()) item { EmptyState("No family members added yet.\nTap + to add one.") }
            items(members) { member ->
                FamilyMemberCard(
                    member = member,
                    onEdit = { onEditMember(member.id) },
                    onDelete = { vm.deleteMember(member.id) }
                )
            }
        }
    }
}

@Composable
fun FamilyMemberCard(member: FamilyMember, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        member.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(member.relationship.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Entity field is 'pan' (not panNumber)
                if (member.pan.isNotBlank()) {
                    Text("PAN: ${member.pan}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit") }
            IconButton(onClick = { showDelete = true }) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete Member") },
            text = { Text("Remove ${member.name}? All related data will be deleted.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDelete = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
    }
}

// Navigation.kt calls: AddEditMemberScreen(editMemberId = ..., onBack = ...)
@Composable
fun AddEditMemberScreen(
    editMemberId: Long? = null,
    onBack: () -> Unit,
    vm: FamilyViewModel = hiltViewModel()
) {
    var name        by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf(Relationship.SELF) }
    var pan         by remember { mutableStateOf("") }   // entity field: pan
    var email       by remember { mutableStateOf("") }
    var phone       by remember { mutableStateOf("") }   // entity field: phone
    var aadhaar     by remember { mutableStateOf("") }   // entity field: aadhaar
    var dob         by remember { mutableStateOf<Long?>(null) }
    var nominees    by remember { mutableStateOf(listOf<Nominee>()) }

    LaunchedEffect(editMemberId) {
        if (editMemberId != null) {
            vm.getMember(editMemberId)?.let { m ->
                name         = m.name
                relationship = m.relationship
                pan          = m.pan        // entity field: pan
                email        = m.email
                phone        = m.phone      // entity field: phone
                aadhaar      = m.aadhaar    // entity field: aadhaar
                dob          = m.dateOfBirth
            }
            nominees = vm.getNominees(editMemberId).first()
        }
    }

    Scaffold(
        topBar = { TopBarWithBack(if (editMemberId != null) "Edit Member" else "Add Member", onBack) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        val m = FamilyMember(
                            id           = editMemberId ?: 0L,
                            name         = name.trim(),
                            relationship = relationship,
                            pan          = pan.trim(),    // entity field: pan
                            email        = email.trim(),
                            phone        = phone.trim(),  // entity field: phone
                            aadhaar      = aadhaar.trim(),// entity field: aadhaar
                            dateOfBirth  = dob
                        )
                        vm.saveMemberWithNominees(m, nominees) { onBack() }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (editMemberId != null) "Update Member" else "Save Member") }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Personal Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        InputField("Full Name *", name, { name = it })
                        DropdownField("Relationship", Relationship.values().toList(), relationship, { relationship = it }, { it.name })
                        DateField("Date of Birth", dob, { dob = it })
                        InputField("PAN", pan, { pan = it.uppercase() })
                        InputField("Phone", phone, { phone = it }, keyboardType = KeyboardType.Phone)
                        InputField("Email", email, { email = it }, keyboardType = KeyboardType.Email)
                        InputField("Aadhaar", aadhaar, { aadhaar = it }, keyboardType = KeyboardType.Number)
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Nominees", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            TextButton(onClick = {
                                nominees = nominees + Nominee(
                                    familyMemberId = editMemberId ?: 0L,
                                    nomineeName    = "",
                                    relationship   = Relationship.SPOUSE,
                                    percentage     = if (nominees.isEmpty()) 100.0 else 0.0
                                )
                            }) { Icon(Icons.Default.Add, null); Text("Add") }
                        }
                        if (nominees.isEmpty()) {
                            Text("No nominees added.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        nominees.forEachIndexed { idx, nominee ->
                            NomineeForm(
                                nominee  = nominee,
                                onUpdate = { nominees = nominees.toMutableList().also { l -> l[idx] = it } },
                                onDelete = { nominees = nominees.toMutableList().also { l -> l.removeAt(idx) } }
                            )
                            if (idx < nominees.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NomineeForm(nominee: Nominee, onUpdate: (Nominee) -> Unit, onDelete: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Nominee", style = MaterialTheme.typography.labelLarge)
            IconButton(onClick = onDelete) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
        }
        InputField("Nominee Name", nominee.nomineeName, { onUpdate(nominee.copy(nomineeName = it)) })
        DropdownField("Relationship", Relationship.values().toList(), nominee.relationship, { onUpdate(nominee.copy(relationship = it)) }, { it.name })
        InputField("Share %", nominee.percentage.toString(), { onUpdate(nominee.copy(percentage = it.toDoubleOrNull() ?: 0.0)) }, keyboardType = KeyboardType.Decimal)
        InputField("PAN (optional)", nominee.pan, { onUpdate(nominee.copy(pan = it)) })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = nominee.isMinor, onCheckedChange = { onUpdate(nominee.copy(isMinor = it)) })
            Text("Minor Nominee")
        }
        if (nominee.isMinor) InputField("Guardian Name", nominee.guardianName, { onUpdate(nominee.copy(guardianName = it)) })
    }
}
EOF
echo "FamilyScreens.kt written"

# ══════════════════════════════════════════════════════════════════
# FILE 2: HoldingScreens.kt — complete clean rewrite
# ══════════════════════════════════════════════════════════════════
cat > app/src/main/java/com/investtrack/ui/holdings/HoldingScreens.kt << 'EOF'
package com.investtrack.ui.holdings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    val filteredHoldings = combine(_holdings, _filterType) { h, type ->
        if (type == null) h else h.filter { it.securityType == type }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoldingsScreen(
    onHoldingClick: (Long) -> Unit,
    onUpdatePrice: (Long) -> Unit,
    onBack: () -> Unit,
    vm: HoldingsViewModel = hiltViewModel()
) {
    val holdings by vm.filteredHoldings.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val filterType by vm.filterType.collectAsState()
    val totalCost = holdings.sumOf { it.totalCost }
    val totalMV   = holdings.sumOf { it.marketValue }
    val totalGain = totalMV - totalCost

    Scaffold(
        topBar = {
            TopBarWithBack("My Holdings", onBack) {
                IconButton(onClick = { vm.loadHoldings() }) { Icon(Icons.Default.Refresh, "Refresh") }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (holdings.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Portfolio Holdings", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary.copy(0.8f))
                                Text(FinancialUtils.formatCurrencyFull(totalMV), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("Cost: ${FinancialUtils.formatCurrency(totalCost)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(0.8f))
                                    Text(
                                        "Gain: ${if (totalGain >= 0) "+" else ""}${FinancialUtils.formatCurrency(totalGain)}",
                                        color = if (totalGain >= 0) Color(0xFF80FF80) else Color(0xFFFF8080),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(selected = filterType == null, onClick = { vm.setTypeFilter(null) }, label = { Text("All") })
                        }
                        val types = holdings.map { it.securityType }.distinct()
                        items(types) { type ->
                            FilterChip(
                                selected = filterType == type,
                                onClick  = { vm.setTypeFilter(if (filterType == type) null else type) },
                                label    = { Text(type.name.replace("_", " ")) }
                            )
                        }
                    }
                }
                if (holdings.isEmpty()) {
                    item { EmptyState("No holdings found. Add transactions to see your portfolio.") }
                }
                items(holdings, key = { it.securityId }) { holding ->
                    HoldingCard(
                        holding     = holding,
                        onClick     = { onHoldingClick(holding.securityId) },
                        onUpdatePrice = { onUpdatePrice(holding.securityId) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
                HoldingMetric("Units",      if (holding.totalUnits > 0) "%.4f".format(holding.totalUnits) else "—")
                HoldingMetric("Avg Cost",   if (holding.avgCostPrice > 0) "₹%.4f".format(holding.avgCostPrice) else "—")
                HoldingMetric("Curr Price", if (holding.currentPrice > 0) "₹%.4f".format(holding.currentPrice) else "—")
            }
            Divider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HoldingMetric("Invested",     FinancialUtils.formatCurrency(holding.totalCost))
                HoldingMetric("Market Value", FinancialUtils.formatCurrency(holding.marketValue), MaterialTheme.colorScheme.primary)
                HoldingMetric("Gain/Loss",    FinancialUtils.formatCurrency(holding.unrealizedGain), gainColor)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HoldingMetric("Abs Return", FinancialUtils.formatPercent(holding.absoluteReturn), gainColor)
                HoldingMetric("XIRR",  holding.xirr?.let  { FinancialUtils.formatPercent(it) } ?: "N/A", gainColor)
                HoldingMetric("CAGR",  holding.cagr?.let  { FinancialUtils.formatPercent(it) } ?: "N/A", gainColor)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoldingDetailScreen(
    securityId: Long,
    onAddTransaction: (Long) -> Unit,
    onUpdatePrice: (Long) -> Unit,
    onBack: () -> Unit,
    vm: HoldingsViewModel = hiltViewModel()
) {
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
            if (holding != null) {
                val h = holding
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(h.securityName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("${h.securityCode} • ${h.securityType.name.replace("_", " ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Divider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                HoldingMetric("Total Units",   if (h.totalUnits > 0) "%.4f".format(h.totalUnits) else "—")
                                HoldingMetric("Avg Cost",      "₹%.4f".format(h.avgCostPrice))
                                HoldingMetric("Current Price", "₹%.4f".format(h.currentPrice), MaterialTheme.colorScheme.primary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                HoldingMetric("Invested",     FinancialUtils.formatCurrencyFull(h.totalCost))
                                HoldingMetric("Market Value", FinancialUtils.formatCurrencyFull(h.marketValue), MaterialTheme.colorScheme.primary)
                            }
                            val gc = if (h.unrealizedGain >= 0) GainColor else LossColor
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                HoldingMetric("Gain/Loss",  "${if (h.unrealizedGain >= 0) "+" else ""}${FinancialUtils.formatCurrencyFull(h.unrealizedGain)}", gc)
                                HoldingMetric("Abs Return", FinancialUtils.formatPercent(h.absoluteReturn), gc)
                                HoldingMetric("XIRR",       h.xirr?.let { FinancialUtils.formatPercent(it) } ?: "N/A", gc)
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
EOF
echo "HoldingScreens.kt written"

# ══════════════════════════════════════════════════════════════════
# FILE 3: SecurityScreens.kt — fix only the broken constructor parts
# Entity fields are: schemeType, amcName, isinCode (NOT mfSchemeType/sector/isin)
# Fields NOT in entity: exitLoadPercent, expenseRatio, creditRating
# ══════════════════════════════════════════════════════════════════
python3 << 'PYEOF'
path = 'app/src/main/java/com/investtrack/ui/security/SecurityScreens.kt'
c = open(path).read()

# 1. Fix function param name to match Navigation.kt call: editSecurityId
c = c.replace('fun AddEditSecurityScreen(securityId: Long?,', 'fun AddEditSecurityScreen(editSecurityId: Long?,')
# Make sure the parameter is used consistently inside the function
# Replace usages of 'securityId' param (not entity field) with 'editSecurityId'
# In the Scaffold and LaunchedEffect
c = c.replace('LaunchedEffect(securityId) {', 'LaunchedEffect(editSecurityId) {')
c = c.replace('securityId?.let { id ->', 'editSecurityId?.let { id ->')
c = c.replace('id = securityId ?: 0L,', 'id = editSecurityId ?: 0L,')
c = c.replace('if (securityId != null) "Edit Security"', 'if (editSecurityId != null) "Edit Security"')
c = c.replace('if (securityId != null) "Update Security"', 'if (editSecurityId != null) "Update Security"')

# 2. Remove non-existent constructor params (exitLoadPercent, expenseRatio, creditRating)
# These appear as "exitLoadPercent = ..., expenseRatio = ...," patterns
import re
# Remove lines with these invalid params from the SecurityMaster(...) constructor call
def remove_bad_params(match):
    txt = match.group()
    bad = ['exitLoadPercent =', 'expenseRatio =', 'creditRating =']
    lines = txt.split('\n')
    cleaned = [l for l in lines if not any(b in l for b in bad)]
    return '\n'.join(cleaned)

c = re.sub(r'SecurityMaster\([\s\S]*?\)', remove_bad_params, c)

# 3. Fix any mfSchemeType references back to schemeType (entity has schemeType)
c = c.replace('mfSchemeType =', 'schemeType =')
c = c.replace('.mfSchemeType', '.schemeType')

open(path, 'w').write(c)
print("SecurityScreens.kt fixed")
PYEOF

# ══════════════════════════════════════════════════════════════════
# FILE 4: DashboardScreen.kt — fix portfolioSummary?.let{} composable issue
# Convert let{} to explicit null-check to ensure composable context works
# ══════════════════════════════════════════════════════════════════
python3 << 'PYEOF'
path = 'app/src/main/java/com/investtrack/ui/dashboard/DashboardScreen.kt'
c = open(path).read()

# The issue: uiState.portfolioSummary?.let { summary -> item { SectionHeader(...) } }
# In some Kotlin/Compose compiler versions, calling composable DSL functions 
# inside a let{} lambda can fail. Convert to explicit if-check.

old = '''            uiState.portfolioSummary?.let { summary ->
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
            }'''

new = '''            val summary = uiState.portfolioSummary
            if (summary != null) {
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
                item {
                    SectionHeader("Investment Categories")
                    SecurityTypeRow(summary)
                }
            }'''

if old in c:
    c = c.replace(old, new)
    print("DashboardScreen: let{} converted to if-check")
else:
    # The code may already have been partially patched. Just ensure MetricCard import exists.
    print("WARNING: exact pattern not found, attempting partial fix")
    # Replace any remaining let { summary -> with explicit check
    import re
    c = re.sub(
        r'uiState\.portfolioSummary\?\.let \{ summary ->',
        'val summary = uiState.portfolioSummary\n            if (summary != null) {',
        c
    )
    # Fix closing brace of let
    print("Partial fix applied")

# Ensure MetricCard import is present (was removed by a previous patch)
if 'import com.investtrack.ui.common.MetricCard' not in c:
    c = c.replace(
        'import com.investtrack.ui.common.SectionHeader',
        'import com.investtrack.ui.common.MetricCard\nimport com.investtrack.ui.common.SectionHeader'
    )
    print("MetricCard import re-added")

open(path, 'w').write(c)
print("DashboardScreen.kt done")
PYEOF

# ══════════════════════════════════════════════════════════════════
# FILE 5: Navigation.kt — fix HoldingsScreen call to use correct param names
# ══════════════════════════════════════════════════════════════════
python3 << 'PYEOF'
path = 'app/src/main/java/com/investtrack/navigation/Navigation.kt'
c = open(path).read()

# HoldingsScreen actual params: onHoldingClick, onUpdatePrice, onBack
# Our Navigation.kt was calling with onAddTransaction (wrong)
old = '''            HoldingsScreen(
                onAddTransaction = { sid -> navController.navigate(Screen.AddTransaction.createRoute(sid)) },
                onBack           = { navController.popBackStack() }
            )'''
new = '''            HoldingsScreen(
                onHoldingClick = { sid -> navController.navigate(Screen.AddTransaction.createRoute(sid)) },
                onUpdatePrice  = { sid -> navController.navigate(Screen.PriceUpdate.createRoute(sid)) },
                onBack         = { navController.popBackStack() }
            )'''

if old in c:
    c = c.replace(old, new)
    print("Navigation: HoldingsScreen params fixed")
else:
    print("WARNING: HoldingsScreen call pattern not found - checking existing")
    import re
    m = re.search(r'composable\(Screen\.Holdings\.route\) \{[\s\S]*?\}', c)
    if m: print("Found Holdings composable:", m.group()[:200])

open(path, 'w').write(c)
PYEOF

echo ""
echo "All fixes applied. Committing..."
git add .
git commit -m "Fix: complete clean rewrite FamilyScreens/HoldingScreens, SecurityScreens constructor, DashboardScreen let->if"
git push origin main
echo "DONE"
