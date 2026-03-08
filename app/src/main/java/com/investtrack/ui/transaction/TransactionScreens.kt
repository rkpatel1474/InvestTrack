package com.investtrack.ui.transaction

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investtrack.data.database.entities.FamilyMember
import com.investtrack.data.database.entities.SecurityMaster
import com.investtrack.data.database.entities.SecurityType
import com.investtrack.data.database.entities.Transaction
import com.investtrack.data.database.entities.TransactionType
import com.investtrack.data.repository.FamilyRepository
import com.investtrack.data.repository.SecurityRepository
import com.investtrack.data.repository.TransactionRepository
import com.investtrack.ui.common.DateField
import com.investtrack.ui.common.DropdownField
import com.investtrack.ui.common.EmptyState
import com.investtrack.ui.common.InputField
import com.investtrack.ui.common.PillChip
import com.investtrack.ui.common.TopBarWithBack
import com.investtrack.ui.theme.GainColor
import com.investtrack.ui.theme.LossColor
import com.investtrack.utils.DateUtils.toDisplayDate
import com.investtrack.utils.FinancialUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val securityRepo: SecurityRepository,
    private val familyRepo: FamilyRepository
) : ViewModel() {
    val allTransactions = transactionRepo.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allMembers = familyRepo.getAllMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun searchSecurities(query: String) = securityRepo.searchSecurities(query)
    suspend fun getSecurity(id: Long) = securityRepo.getSecurityById(id)
    suspend fun getTransactionById(id: Long) = transactionRepo.getSecurityById(id)

    fun saveTransaction(t: Transaction, onDone: () -> Unit) {
        viewModelScope.launch {
            if (t.id == 0L) transactionRepo.insert(t) else transactionRepo.update(t)
            onDone()
        }
    }
    fun deleteTransaction(t: Transaction) = viewModelScope.launch { transactionRepo.delete(t) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onBack: () -> Unit,
    vm: TransactionViewModel = hiltViewModel()
) {
    val transactions by vm.allTransactions.collectAsState()
    val members by vm.allMembers.collectAsState()

    Scaffold(topBar = { TopBarWithBack("Transactions", onBack) }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (transactions.isEmpty()) item { EmptyState("No transactions yet. Tap + to add one.") }
            items(transactions, key = { it.id }) { txn ->
                var secName by remember { mutableStateOf("") }
                var memberName by remember { mutableStateOf("") }
                LaunchedEffect(txn.id) {
                    secName = vm.getSecurity(txn.securityId)?.securityName ?: "Unknown"
                    memberName = members.find { it.id == txn.familyMemberId }?.name ?: "Unknown"
                }
                TransactionCard(
                    txn = txn,
                    secName = secName,
                    memberName = memberName,
                    onEdit = { onEditTransaction(txn.id) },
                    onDelete = { vm.deleteTransaction(txn) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionCard(
    txn: Transaction,
    secName: String,
    memberName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isBuy = txn.transactionType in listOf(
        TransactionType.BUY, TransactionType.SIP, TransactionType.INVEST,
        TransactionType.DEPOSIT, TransactionType.PREMIUM
    )
    val amount = txn.amount ?: ((txn.units ?: 0.0) * (txn.price ?: 0.0))
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Type indicator icon
            Surface(
                shape = CircleShape,
                color = if (isBuy) GainColor.copy(0.15f) else LossColor.copy(0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isBuy) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        null,
                        tint = if (isBuy) GainColor else LossColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    secName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    "${txn.transactionType.name.replace("_", " ")} • $memberName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        txn.transactionDate.toDisplayDate(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    txn.units?.let {
                        Text(
                            "${"%.4f".format(it)} units @ ₹${"%.4f".format(txn.price ?: 0.0)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            // Amount
            Text(
                FinancialUtils.formatCurrency(amount),
                fontWeight = FontWeight.Bold,
                color = if (isBuy) MaterialTheme.colorScheme.onSurface else LossColor,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.width(4.dp))
            // Edit button
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    "Edit",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            // Delete button
            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction") },
            text = { Text("Delete this ${txn.transactionType.name} transaction for $secName?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    preSelectedSecurityId: Long?,
    editTransactionId: Long? = null,
    onBack: () -> Unit,
    vm: TransactionViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val members by vm.allMembers.collectAsState()
    val isEditing = editTransactionId != null

    var selectedMember by remember { mutableStateOf<FamilyMember?>(null) }
    var selectedSecurity by remember { mutableStateOf<SecurityMaster?>(null) }
    var securityQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<SecurityMaster>()) }
    var showSearch by remember { mutableStateOf(false) }
    var txnDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var txnType by remember { mutableStateOf(TransactionType.BUY) }
    var units by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var stampDuty by remember { mutableStateOf("") }
    var brokerage by remember { mutableStateOf("") }
    var stt by remember { mutableStateOf("") }
    var folioNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var existingTransaction by remember { mutableStateOf<Transaction?>(null) }

    // Load existing transaction for editing
    LaunchedEffect(editTransactionId) {
        editTransactionId?.let { id ->
            vm.getTransactionById(id)?.let { t ->
                existingTransaction = t
                selectedSecurity = vm.getSecurity(t.securityId)
                selectedSecurity?.let { securityQuery = it.securityName }
                txnDate = t.transactionDate
                txnType = t.transactionType
                units = t.units?.toString() ?: ""
                price = t.price?.toString() ?: ""
                amount = t.amount?.toString() ?: ""
                stampDuty = t.stampDuty.toString()
                brokerage = t.brokerage.toString()
                stt = t.stt.toString()
                folioNumber = t.folioNumber
                notes = t.notes
            }
        }
    }

    // Load pre-selected security (add mode)
    LaunchedEffect(preSelectedSecurityId) {
        if (editTransactionId == null) {
            preSelectedSecurityId?.let { id ->
                selectedSecurity = vm.getSecurity(id)
                selectedSecurity?.let { securityQuery = it.securityName }
            }
        }
    }

    // Set member after members load
    LaunchedEffect(members, existingTransaction) {
        if (selectedMember == null) {
            selectedMember = if (existingTransaction != null) {
                members.find { it.id == existingTransaction!!.familyMemberId } ?: members.firstOrNull()
            } else {
                members.firstOrNull()
            }
        }
    }

    val isUnitBased = selectedSecurity?.securityType in listOf(
        SecurityType.MUTUAL_FUND, SecurityType.SHARES, SecurityType.BOND, SecurityType.GOI_BOND
    )

    val validTxnTypes = selectedSecurity?.let { sec ->
        when (sec.securityType) {
            SecurityType.MUTUAL_FUND -> listOf(TransactionType.BUY, TransactionType.SELL, TransactionType.SIP, TransactionType.SWP, TransactionType.DIVIDEND)
            SecurityType.SHARES -> listOf(TransactionType.BUY, TransactionType.SELL, TransactionType.BONUS, TransactionType.DIVIDEND)
            SecurityType.BOND, SecurityType.GOI_BOND -> listOf(TransactionType.BUY, TransactionType.SELL, TransactionType.COUPON, TransactionType.MATURITY)
            SecurityType.NPS -> listOf(TransactionType.INVEST, TransactionType.REDEEM)
            SecurityType.PF -> listOf(TransactionType.INVEST, TransactionType.REDEEM, TransactionType.INTEREST)
            SecurityType.FD -> listOf(TransactionType.DEPOSIT, TransactionType.WITHDRAWAL, TransactionType.MATURITY, TransactionType.INTEREST)
            SecurityType.INSURANCE -> listOf(TransactionType.PREMIUM, TransactionType.MATURITY)
            SecurityType.PROPERTY -> listOf(TransactionType.BUY, TransactionType.SELL)
            else -> TransactionType.values().toList()
        }
    } ?: TransactionType.values().toList()

    val calcAmount = if (isUnitBased && units.isNotBlank() && price.isNotBlank()) {
        ((units.toDoubleOrNull() ?: 0.0) * (price.toDoubleOrNull() ?: 0.0)).toString()
    } else amount

    val canSave = selectedMember != null && selectedSecurity != null && txnDate > 0 &&
            (if (isUnitBased) units.isNotBlank() && price.isNotBlank() else amount.isNotBlank())

    Scaffold(
        topBar = { TopBarWithBack(if (isEditing) "Edit Transaction" else "Add Transaction", onBack) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                androidx.compose.material3.Button(
                    onClick = {
                        val t = Transaction(
                            id = existingTransaction?.id ?: 0L,
                            familyMemberId = selectedMember!!.id,
                            securityId = selectedSecurity!!.id,
                            transactionDate = txnDate,
                            transactionType = txnType,
                            units = units.toDoubleOrNull(),
                            price = price.toDoubleOrNull(),
                            amount = calcAmount.toDoubleOrNull(),
                            stampDuty = stampDuty.toDoubleOrNull() ?: 0.0,
                            brokerage = brokerage.toDoubleOrNull() ?: 0.0,
                            stt = stt.toDoubleOrNull() ?: 0.0,
                            folioNumber = folioNumber,
                            notes = notes
                        )
                        vm.saveTransaction(t) { onBack() }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    enabled = canSave,
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (isEditing) "Update Transaction" else "Save Transaction") }
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
                        Text("Transaction Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        DropdownField("Family Member *", members, selectedMember, { selectedMember = it }, { it.name })
                        OutlinedTextField(
                            value = securityQuery,
                            onValueChange = { q ->
                                securityQuery = q
                                showSearch = true
                                scope.launch { searchResults = vm.searchSecurities(q) }
                            },
                            label = { Text("Search Security *") },
                            trailingIcon = { Icon(Icons.Default.Search, null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            readOnly = isEditing
                        )
                        if (showSearch && searchResults.isNotEmpty() && !isEditing) {
                            Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(8.dp)) {
                                searchResults.take(5).forEach { sec ->
                                    ListItem(
                                        headlineContent = { Text(sec.securityName) },
                                        supportingContent = { Text(sec.securityCode) },
                                        modifier = Modifier.clickable {
                                            selectedSecurity = sec
                                            securityQuery = sec.securityName
                                            showSearch = false
                                            txnType = validTxnTypes.first()
                                        }
                                    )
                                    Divider()
                                }
                            }
                        }
                        selectedSecurity?.let {
                            PillChip(it.securityType.name.replace("_", " "), MaterialTheme.colorScheme.primary)
                        }
                        DropdownField("Transaction Type *", validTxnTypes, txnType, { txnType = it }, { it.name.replace("_", " ") })
                        DateField("Transaction Date *", txnDate, { txnDate = it })
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Amount Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (isUnitBased) {
                            InputField("Units *", units, { units = it }, keyboardType = KeyboardType.Decimal)
                            InputField("Price / NAV (₹) *", price, { price = it }, keyboardType = KeyboardType.Decimal)
                            if (units.isNotBlank() && price.isNotBlank()) {
                                val totalAmt = (units.toDoubleOrNull() ?: 0.0) * (price.toDoubleOrNull() ?: 0.0)
                                Text(
                                    "Total: ${FinancialUtils.formatCurrencyFull(totalAmt)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            InputField("Amount (₹) *", amount, { amount = it }, keyboardType = KeyboardType.Decimal)
                        }
                        if (selectedSecurity?.securityType in listOf(SecurityType.MUTUAL_FUND, SecurityType.SHARES)) {
                            InputField("Folio / DP No.", folioNumber, { folioNumber = it })
                        }
                    }
                }
            }
            if (isUnitBased && selectedSecurity?.securityType in listOf(SecurityType.SHARES, SecurityType.BOND)) {
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Charges (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                InputField("Brokerage (₹)", brokerage, { brokerage = it }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
                                InputField("STT (₹)", stt, { stt = it }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
                            }
                            InputField("Stamp Duty (₹)", stampDuty, { stampDuty = it }, keyboardType = KeyboardType.Decimal)
                        }
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InputField("Notes (Optional)", notes, { notes = it })
                    }
                }
            }
        }
    }
}
