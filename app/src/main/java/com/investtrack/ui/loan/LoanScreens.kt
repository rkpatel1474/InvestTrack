package com.investtrack.ui.loan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investtrack.data.database.entities.FamilyMember
import com.investtrack.data.database.entities.InterestType
import com.investtrack.data.database.entities.Loan
import com.investtrack.data.database.entities.LoanAdjustment
import com.investtrack.data.database.entities.LoanPayment
import com.investtrack.data.database.entities.LoanRateChange
import com.investtrack.data.database.entities.LoanType
import com.investtrack.data.repository.FamilyRepository
import com.investtrack.data.repository.LoanRepository
import com.investtrack.ui.common.*
import com.investtrack.ui.theme.GainColor
import com.investtrack.ui.theme.LossColor
import com.investtrack.utils.DateUtils.toDisplayDate
import com.investtrack.utils.FinancialUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class LoanViewModel @Inject constructor(
    private val loanRepo: LoanRepository,
    private val familyRepo: FamilyRepository
) : ViewModel() {
    val allLoans = loanRepo.getAllLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allMembers = familyRepo.getAllMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getLoanById(id: Long, callback: (Loan?) -> Unit) = viewModelScope.launch {
        callback(loanRepo.getLoanById(id))
    }
    fun getPayments(loanId: Long) = loanRepo.getPayments(loanId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun getRateChanges(loanId: Long) = loanRepo.getRateChanges(loanId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveLoan(loan: Loan, onDone: (Long) -> Unit) = viewModelScope.launch {
        val id = if (loan.id == 0L) loanRepo.insertLoan(loan) else { loanRepo.updateLoan(loan); loan.id }
        onDone(id)
    }
    fun deleteLoan(id: Long) = viewModelScope.launch { loanRepo.deleteLoan(id) }
    fun addPayment(payment: LoanPayment) = viewModelScope.launch { loanRepo.insertPayment(payment) }
    fun deletePayment(payment: LoanPayment) = viewModelScope.launch { loanRepo.deletePayment(payment) }
    fun addRateChange(change: LoanRateChange) = viewModelScope.launch { loanRepo.insertRateChange(change) }
    fun deleteRateChange(change: LoanRateChange) = viewModelScope.launch { loanRepo.deleteRateChange(change) }

    suspend fun getTotalPrincipalPaid(loanId: Long) = loanRepo.getTotalPrincipalPaid(loanId)
    suspend fun getTotalInterestPaid(loanId: Long) = loanRepo.getTotalInterestPaid(loanId)
}

// ─── Loan List Screen ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanListScreen(onAddLoan: () -> Unit, onLoanDetail: (Long) -> Unit, onBack: () -> Unit, vm: LoanViewModel = hiltViewModel()) {
    val loans by vm.allLoans.collectAsState()
    val members by vm.allMembers.collectAsState()

    val totalOutstanding = loans.sumOf { loan ->
        val paidInstPct = 0.0 // simplified
        loan.loanAmount
    }

    Scaffold(
        topBar = { AppTopBar("Loans & Liabilities") },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (loans.isNotEmpty()) {
                item {
                    GradientHeaderCard(
                        title = "Total Loans",
                        value = formatAmount(loans.sumOf { it.loanAmount }),
                        subtitle = "${loans.size} active loan(s)",
                        gainLoss = null
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard(
                            label = "Total EMI/month",
                            value = formatAmount(loans.sumOf { it.emiAmount }),
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.CalendarToday,
                            iconColor = MaterialTheme.colorScheme.secondary
                        )
                        StatCard(
                            label = "Loans count",
                            value = "${loans.size}",
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.AccountBalance,
                            iconColor = LossColor
                        )
                    }
                }
            }

            if (loans.isEmpty()) {
                item { EmptyState("No loans added yet.\nTap + to add your first loan.", Icons.Default.AccountBalance) }
            } else {
                items(loans, key = { it.id }) { loan ->
                    val memberName = members.find { it.id == loan.familyMemberId }?.name ?: "Unknown"
                    LoanCard(loan = loan, memberName = memberName, onClick = { onLoanDetail(loan.id) }, onDelete = { vm.deleteLoan(loan.id) })
                }
            }
        }
    }
}

@Composable
fun LoanCard(loan: Loan, memberName: String, onClick: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val typeColor = loanTypeColor(loan.loanType)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Default.AccountBalance, typeColor)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(loan.loanName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PillChip(loan.loanType.name.replace("_", " "), typeColor)
                        PillChip(memberName, MaterialTheme.colorScheme.secondary)
                    }
                }
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null, tint = LossColor, modifier = Modifier.size(18.dp))
                }
            }
            Divider(color = MaterialTheme.colorScheme.outline.copy(0.3f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Loan Amount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatAmount(loan.loanAmount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${loan.interestRate}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LossColor)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("EMI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatAmount(loan.emiAmount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            // Progress bar
            val disbursed = loan.disbursementDate
            val totalMs = loan.tenureMonths * 30L * 24 * 3600 * 1000
            val elapsed = System.currentTimeMillis() - disbursed
            val progress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Progress", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Loan") },
            text = { Text("Delete \"${loan.loanName}\"? This will remove all payments too.") },
            confirmButton = { TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("Delete", color = LossColor) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}

// ─── Add/Edit Loan Screen ─────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLoanScreen(editLoanId: Long? = null, onBack: () -> Unit, vm: LoanViewModel = hiltViewModel()) {
    val members by vm.allMembers.collectAsState()
    var selectedMember by remember { mutableStateOf<FamilyMember?>(null) }
    var loanName by remember { mutableStateOf("") }
    var loanType by remember { mutableStateOf(LoanType.HOME_LOAN) }
    var interestType by remember { mutableStateOf(InterestType.FIXED) }
    var lenderName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var loanAmount by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var tenureMonths by remember { mutableStateOf("") }
    var disbDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var emiAmount by remember { mutableStateOf("") }
    var emiDay by remember { mutableStateOf("1") }
    var processingFee by remember { mutableStateOf("") }
    var moratoriumMonths by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }
    var emiOverridden by remember { mutableStateOf(false) }

    // Load existing loan
    LaunchedEffect(editLoanId) {
        editLoanId?.let { id ->
            vm.getLoanById(id) { loan ->
                loan?.let {
                    loanName = it.loanName
                    loanType = it.loanType
                    interestType = it.interestType
                    lenderName = it.lenderName
                    accountNumber = it.accountNumber
                    loanAmount = it.loanAmount.toString()
                    interestRate = it.interestRate.toString()
                    tenureMonths = it.tenureMonths.toString()
                    disbDate = it.disbursementDate
                    emiAmount = it.emiAmount.toString()
                    emiDay = it.emiDay.toString()
                    processingFee = it.processingFee.toString()
                    moratoriumMonths = it.moratoriumMonths.toString()
                    notes = it.notes
                }
            }
        }
    }
    LaunchedEffect(members) { if (selectedMember == null && members.isNotEmpty()) selectedMember = members.first() }

    // Auto-calculate EMI
    val calcEmi = remember(loanAmount, interestRate, tenureMonths) {
        val p = loanAmount.toDoubleOrNull() ?: 0.0
        val r = interestRate.toDoubleOrNull() ?: 0.0
        val n = tenureMonths.toIntOrNull() ?: 0
        if (p > 0 && r > 0 && n > 0) FinancialUtils.calculateEMI(p, r, n) else 0.0
    }
    // When user changes EMI manually, recalculate tenure
    val effectiveTenure = remember(emiAmount, loanAmount, interestRate, emiOverridden) {
        if (!emiOverridden) return@remember tenureMonths.toIntOrNull() ?: 0
        val p = loanAmount.toDoubleOrNull() ?: return@remember 0
        val r = interestRate.toDoubleOrNull() ?: return@remember 0
        val emi = emiAmount.toDoubleOrNull() ?: return@remember 0
        if (p <= 0 || r <= 0 || emi <= 0) return@remember 0
        val monthlyRate = r / (12 * 100)
        if (monthlyRate == 0.0) return@remember (p / emi).roundToInt()
        val lnVal = Math.log(emi / (emi - p * monthlyRate))
        val lnRate = Math.log(1 + monthlyRate)
        (lnVal / lnRate).roundToInt()
    }

    // Show calc EMI and derived tenure
    LaunchedEffect(calcEmi) {
        if (!emiOverridden && calcEmi > 0) emiAmount = "%.2f".format(calcEmi)
    }
    LaunchedEffect(effectiveTenure) {
        if (emiOverridden && effectiveTenure > 0) tenureMonths = effectiveTenure.toString()
    }

    val canSave = selectedMember != null && loanName.isNotBlank() && loanAmount.isNotBlank() &&
            interestRate.isNotBlank() && tenureMonths.isNotBlank()

    Scaffold(
        topBar = { TopBarWithBack(if (editLoanId != null) "Edit Loan" else "Add Loan", onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.background) {
                Button(
                    onClick = {
                        val loan = Loan(
                            id = editLoanId ?: 0L,
                            familyMemberId = selectedMember!!.id,
                            loanName = loanName,
                            loanType = loanType,
                            interestType = interestType,
                            lenderName = lenderName,
                            accountNumber = accountNumber,
                            loanAmount = loanAmount.toDouble(),
                            interestRate = interestRate.toDouble(),
                            tenureMonths = tenureMonths.toInt(),
                            disbursementDate = disbDate,
                            emiAmount = emiAmount.toDoubleOrNull() ?: calcEmi,
                            emiDay = emiDay.toIntOrNull() ?: 1,
                            processingFee = processingFee.toDoubleOrNull() ?: 0.0,
                            moratoriumMonths = moratoriumMonths.toIntOrNull() ?: 0,
                            notes = notes
                        )
                        vm.saveLoan(loan) { onBack() }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    enabled = canSave, shape = RoundedCornerShape(12.dp)
                ) { Text(if (editLoanId != null) "Update Loan" else "Save Loan") }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FormCard("Loan Details") {
                    DropdownField("Family Member *", members, selectedMember, { selectedMember = it }, { it.name })
                    InputField("Loan Name *", loanName, { loanName = it })
                    DropdownField("Loan Type *", LoanType.values().toList(), loanType, { loanType = it }, { it.name.replace("_"," ") })
                    DropdownField("Interest Type", InterestType.values().toList(), interestType, { interestType = it }, { it.name })
                    InputField("Lender / Bank Name", lenderName, { lenderName = it })
                    InputField("Loan Account Number", accountNumber, { accountNumber = it })
                }
            }
            item {
                FormCard("Loan Financials") {
                    InputField("Loan Amount (₹) *", loanAmount, { loanAmount = it; emiOverridden = false }, keyboardType = KeyboardType.Decimal)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InputField("Annual Rate (%) *", interestRate, { interestRate = it; emiOverridden = false }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
                        InputField("Tenure (months) *", tenureMonths, { tenureMonths = it; emiOverridden = false }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    }
                    DateField("Disbursement Date *", disbDate, { disbDate = it })

                    // EMI Section
                    if (calcEmi > 0) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(0.3f), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("EMI Calculator", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Calculated EMI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(formatAmount(calcEmi), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    if (emiOverridden && effectiveTenure > 0) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Revised Tenure", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("$effectiveTenure months", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                // Total interest
                                val totalPayable = (emiAmount.toDoubleOrNull() ?: calcEmi) * (tenureMonths.toIntOrNull() ?: 0)
                                val totalInterest = totalPayable - (loanAmount.toDoubleOrNull() ?: 0.0)
                                if (totalInterest > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Interest", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(formatAmount(totalInterest), style = MaterialTheme.typography.labelSmall, color = LossColor, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InputField(
                            "EMI Amount (₹) — edit to adjust tenure",
                            emiAmount,
                            {
                                emiAmount = it
                                emiOverridden = it != "%.2f".format(calcEmi)
                            },
                            keyboardType = KeyboardType.Decimal
                        )
                        if (emiOverridden) {
                            Text("⟳ Tenure will auto-adjust based on your EMI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InputField("EMI Day of Month", emiDay, { emiDay = it }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number)
                        InputField("Moratorium (months)", moratoriumMonths, { moratoriumMonths = it }, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    }
                    InputField("Processing Fee (₹)", processingFee, { processingFee = it }, keyboardType = KeyboardType.Decimal)
                }
            }
            item { FormCard("Notes") { InputField("Notes (Optional)", notes, { notes = it }, singleLine = false) } }
        }
    }
}

// ─── Loan Detail Screen ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(loanId: Long, onEdit: () -> Unit, onBack: () -> Unit, vm: LoanViewModel = hiltViewModel()) {
    var loan by remember { mutableStateOf<Loan?>(null) }
    val payments by vm.getPayments(loanId).collectAsState(emptyList())
    val rateChanges by vm.getRateChanges(loanId).collectAsState(emptyList())
    val members by vm.allMembers.collectAsState()
    var showAddPayment by remember { mutableStateOf(false) }
    var showAddRateChange by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }

    LaunchedEffect(loanId) { vm.getLoanById(loanId) { loan = it } }

    val currentLoan = loan ?: return

    Scaffold(
        topBar = {
            TopBarWithBack(currentLoan.loanName, onBack) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit") }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Summary card
                val totalPaid = payments.sumOf { it.emiPaid }
                val totalPrincipalPaid = payments.sumOf { it.principalPaid }
                val outstanding = currentLoan.loanAmount - totalPrincipalPaid
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Outstanding", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatAmount(outstanding.coerceAtLeast(0.0)), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = LossColor)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("EMI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatAmount(currentLoan.emiAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        LinearProgressIndicator(
                            progress = (totalPrincipalPaid / currentLoan.loanAmount).toFloat().coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = GainColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            InfoRow("Principal Paid", formatAmount(totalPrincipalPaid))
                        }
                        Divider(color = MaterialTheme.colorScheme.outline.copy(0.3f))
                        InfoRow("Rate", "${currentLoan.interestRate}%  (${currentLoan.interestType.name})")
                        InfoRow("Tenure", "${currentLoan.tenureMonths} months")
                        InfoRow("Lender", currentLoan.lenderName.ifEmpty { "—" })
                        InfoRow("Disbursement", currentLoan.disbursementDate.toDisplayDate())
                        if (currentLoan.moratoriumMonths > 0) InfoRow("Moratorium", "${currentLoan.moratoriumMonths} months")
                    }
                }
            }

            // Tab bar
            item {
                TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Schedule") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Rate Changes") })
                    Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Amortisation") })
                }
            }

            when (tab) {
                0 -> {
                    // Payments list
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("EMI Payments", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Button(onClick = { showAddPayment = true }, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    if (payments.isEmpty()) {
                        item { EmptyState("No payments recorded yet.") }
                    } else {
                        items(payments.reversed(), key = { it.id }) { p ->
                            PaymentRow(p, onDelete = { vm.deletePayment(p) })
                        }
                    }
                }
                1 -> {
                    // Rate change history
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Rate Change History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Button(onClick = { showAddRateChange = true }, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    if (rateChanges.isEmpty()) {
                        item { EmptyState("No rate changes recorded.") }
                    } else {
                        items(rateChanges, key = { it.id }) { rc ->
                            RateChangeRow(rc, onDelete = { vm.deleteRateChange(rc) })
                        }
                    }
                }
                2 -> {
                    // Amortisation schedule
                    val outstanding = currentLoan.loanAmount - payments.sumOf { it.principalPaid }.coerceAtLeast(0.0)
                    val remainingMonths = currentLoan.tenureMonths - payments.count { !it.isPrepayment }
                    val schedule = FinancialUtils.generateAmortisationSchedule(
                        principal = outstanding.coerceAtLeast(0.0),
                        annualRate = currentLoan.interestRate,
                        tenureMonths = remainingMonths.coerceAtLeast(1),
                        emiAmount = currentLoan.emiAmount
                    )
                    item { Text("Remaining Schedule (${schedule.size} installments)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                    item {
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Header
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text("#", modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("EMI", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Principal", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Interest", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Balance", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                schedule.take(24).forEach { row ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                        Text("${row.installment}", modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.labelSmall)
                                        Text(formatAmount(row.emi), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                                        Text(formatAmount(row.principal), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = GainColor)
                                        Text(formatAmount(row.interest), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = LossColor)
                                        Text(formatAmount(row.balance), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                if (schedule.size > 24) {
                                    Text("...and ${schedule.size - 24} more installments", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPayment) {
        AddPaymentDialog(
            loan = currentLoan,
            paidCount = payments.count { !it.isPrepayment },
            onDismiss = { showAddPayment = false },
            onAdd = { vm.addPayment(it); showAddPayment = false }
        )
    }

    if (showAddRateChange) {
        AddRateChangeDialog(
            loan = currentLoan,
            outstandingBalance = currentLoan.loanAmount - payments.sumOf { it.principalPaid }.coerceAtLeast(0.0),
            onDismiss = { showAddRateChange = false },
            onAdd = { change ->
                vm.addRateChange(change)
                // Update the loan's current rate and EMI/tenure based on adjustment
                val updatedLoan = currentLoan.copy(
                    interestRate = change.newRate,
                    emiAmount = change.newEmi ?: currentLoan.emiAmount,
                    tenureMonths = change.newTenure ?: currentLoan.tenureMonths
                )
                vm.saveLoan(updatedLoan) {}
                showAddRateChange = false
            }
        )
    }
}

@Composable
fun PaymentRow(payment: LoanPayment, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(payment.paymentDate.toDisplayDate(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text("Inst #${payment.installmentNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                Text(formatAmount(payment.emiPaid), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("P:${formatAmount(payment.principalPaid)}", style = MaterialTheme.typography.labelSmall, color = GainColor)
                    Text("I:${formatAmount(payment.interestPaid)}", style = MaterialTheme.typography.labelSmall, color = LossColor)
                }
            }
            IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, null, tint = LossColor, modifier = Modifier.size(16.dp))
            }
        }
    }
    if (showConfirm) {
        AlertDialog(onDismissRequest = { showConfirm = false },
            title = { Text("Delete Payment") }, text = { Text("Delete this EMI payment?") },
            confirmButton = { TextButton(onClick = { onDelete(); showConfirm = false }) { Text("Delete", color = LossColor) } },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancel") } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentDialog(loan: Loan, paidCount: Int, onDismiss: () -> Unit, onAdd: (LoanPayment) -> Unit) {
    var payDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var emiPaid by remember { mutableStateOf("%.2f".format(loan.emiAmount)) }
    var isPrepayment by remember { mutableStateOf(false) }
    var prepayAmount by remember { mutableStateOf("") }

    val r = loan.interestRate / (12 * 100)
    val outstandingEst = remember { loan.loanAmount * (1 + r).pow(paidCount.toDouble()) - loan.emiAmount * ((1 + r).pow(paidCount.toDouble()) - 1) / r }
    val interestPart = outstandingEst * r
    val principalPart = (emiPaid.toDoubleOrNull() ?: 0.0) - interestPart

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record EMI Payment", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField("Payment Date", payDate, { payDate = it })
                InputField("EMI Paid (₹)", emiPaid, { emiPaid = it }, keyboardType = KeyboardType.Decimal)
                if (principalPart > 0) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            InfoRow("Est. Interest", formatAmount(interestPart.coerceAtLeast(0.0)))
                            InfoRow("Est. Principal", formatAmount(principalPart.coerceAtLeast(0.0)))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPrepayment, onCheckedChange = { isPrepayment = it })
                    Text("Include Prepayment", style = MaterialTheme.typography.bodyMedium)
                }
                if (isPrepayment) {
                    InputField("Prepayment Amount (₹)", prepayAmount, { prepayAmount = it }, keyboardType = KeyboardType.Decimal)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val emi = emiPaid.toDoubleOrNull() ?: loan.emiAmount
                val interest = interestPart.coerceAtLeast(0.0)
                val principal = (emi - interest).coerceAtLeast(0.0)
                onAdd(LoanPayment(
                    loanId = loan.id,
                    paymentDate = payDate,
                    installmentNumber = paidCount + 1,
                    emiPaid = emi,
                    principalPaid = principal,
                    interestPaid = interest,
                    outstandingBalance = (outstandingEst - principal).coerceAtLeast(0.0),
                    isPrepayment = isPrepayment,
                    prepaymentAmount = prepayAmount.toDoubleOrNull() ?: 0.0,
                    rateApplied = loan.interestRate
                ))
            }) { Text("Record") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRateChangeDialog(loan: Loan, outstandingBalance: Double, onDismiss: () -> Unit, onAdd: (LoanRateChange) -> Unit) {
    var effectiveDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var newRate by remember { mutableStateOf("") }
    var adjustment by remember { mutableStateOf(LoanAdjustment.REDUCE_EMI) }
    var notes by remember { mutableStateOf("") }

    val newRateD = newRate.toDoubleOrNull() ?: 0.0
    val remainingMonths = loan.tenureMonths // simplified
    val newCalcEmi = if (newRateD > 0) FinancialUtils.calculateEMI(outstandingBalance.coerceAtLeast(0.0), newRateD, remainingMonths) else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Interest Rate", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        InfoRow("Current Rate", "${loan.interestRate}%")
                        InfoRow("Outstanding", formatAmount(outstandingBalance))
                    }
                }
                DateField("Effective From", effectiveDate, { effectiveDate = it })
                InputField("New Rate (%)", newRate, { newRate = it }, keyboardType = KeyboardType.Decimal)
                if (newRateD > 0 && loan.interestRate > 0) {
                    val rateDiff = newRateD - loan.interestRate
                    val sign = if (rateDiff >= 0) "▲" else "▼"
                    val color = if (rateDiff >= 0) LossColor else GainColor
                    Text("$sign ${"%.2f".format(Math.abs(rateDiff))}% change from current rate",
                        style = MaterialTheme.typography.labelMedium, color = color)
                }
                Text("Adjust by:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LoanAdjustment.values().forEach { adj ->
                        FilterChip(
                            selected = adjustment == adj,
                            onClick = { adjustment = adj },
                            label = { Text(adj.name.replace("_", " "), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                if (newCalcEmi > 0) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(0.3f), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            when (adjustment) {
                                LoanAdjustment.REDUCE_EMI -> {
                                    InfoRow("New EMI", formatAmount(newCalcEmi))
                                    InfoRow("Old EMI", formatAmount(loan.emiAmount))
                                }
                                LoanAdjustment.REDUCE_TENURE -> {
                                    val monthlyRate = newRateD / (12 * 100)
                                    val newTenure = if (monthlyRate > 0) {
                                        val ln = Math.log(loan.emiAmount / (loan.emiAmount - outstandingBalance * monthlyRate))
                                        (ln / Math.log(1 + monthlyRate)).roundToInt()
                                    } else (outstandingBalance / loan.emiAmount).roundToInt()
                                    InfoRow("New Tenure", "$newTenure months")
                                    InfoRow("Old Tenure", "${remainingMonths} months")
                                }
                            }
                        }
                    }
                }
                InputField("Notes", notes, { notes = it })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val monthlyRate = newRateD / (12 * 100)
                    val newTenure = if (adjustment == LoanAdjustment.REDUCE_TENURE && monthlyRate > 0) {
                        val ln = Math.log(loan.emiAmount / (loan.emiAmount - outstandingBalance * monthlyRate))
                        (ln / Math.log(1 + monthlyRate)).roundToInt()
                    } else null
                    onAdd(LoanRateChange(
                        loanId = loan.id,
                        effectiveDate = effectiveDate,
                        newRate = newRateD,
                        previousRate = loan.interestRate,
                        outstandingAtChange = outstandingBalance,
                        adjustment = adjustment,
                        newEmi = if (adjustment == LoanAdjustment.REDUCE_EMI) newCalcEmi else null,
                        newTenure = newTenure,
                        notes = notes
                    ))
                },
                enabled = newRateD > 0
            ) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun RateChangeRow(rc: LoanRateChange, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rc.effectiveDate.toDisplayDate(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text("${rc.previousRate}% → ${rc.newRate}%", style = MaterialTheme.typography.labelSmall)
                Text(rc.adjustment.name.replace("_", " "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                rc.newEmi?.let { Text("New EMI: ${formatAmount(it)}", style = MaterialTheme.typography.labelSmall, color = GainColor) }
                rc.newTenure?.let { Text("New Tenure: $it months", style = MaterialTheme.typography.labelSmall, color = GainColor) }
            }
            val rateDiff = rc.newRate - rc.previousRate
            PillChip(
                "${if (rateDiff >= 0) "+" else ""}${"%.2f".format(rateDiff)}%",
                if (rateDiff >= 0) LossColor else GainColor
            )
            IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, null, tint = LossColor, modifier = Modifier.size(16.dp))
            }
        }
    }
    if (showConfirm) {
        AlertDialog(onDismissRequest = { showConfirm = false },
            title = { Text("Delete Rate Change") }, text = { Text("Delete this rate change record?") },
            confirmButton = { TextButton(onClick = { onDelete(); showConfirm = false }) { Text("Delete", color = LossColor) } },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancel") } })
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
@Composable
fun FormCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

fun loanTypeColor(type: LoanType): Color = when (type) {
    LoanType.HOME_LOAN      -> Color(0xFF4A90E2)
    LoanType.CAR_LOAN       -> Color(0xFFE67E22)
    LoanType.PERSONAL_LOAN  -> Color(0xFF9B59B6)
    LoanType.EDUCATION_LOAN -> Color(0xFF27AE60)
    LoanType.GOLD_LOAN      -> Color(0xFFFFAB00)
    LoanType.BUSINESS_LOAN  -> Color(0xFF1ABC9C)
    LoanType.CREDIT_CARD    -> Color(0xFFE74C3C)
    LoanType.OTHER          -> Color(0xFF95A5A6)
}

private fun Double.pow(n: Double) = Math.pow(this, n)
