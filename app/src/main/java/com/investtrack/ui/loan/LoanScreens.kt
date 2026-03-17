package com.investtrack.ui.loan

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investtrack.data.database.entities.FamilyMember
import com.investtrack.data.database.entities.Loan
import com.investtrack.data.database.entities.LoanPayment
import com.investtrack.data.database.entities.LoanType
import com.investtrack.data.repository.FamilyRepository
import com.investtrack.data.repository.LoanRepository
import com.investtrack.ui.common.AppDimens
import com.investtrack.ui.common.DateField
import com.investtrack.ui.common.DropdownField
import com.investtrack.ui.common.EmptyState
import com.investtrack.ui.common.InputField
import com.investtrack.ui.common.SectionHeader
import com.investtrack.ui.common.TopBarWithBack
import com.investtrack.ui.theme.LossColor
import com.investtrack.utils.FinancialUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoanSummary(
    val loan: Loan,
    val memberName: String,
    val paidInstallments: Int,
    val totalPrincipalPaid: Double,
    val totalInterestPaid: Double,
    val outstandingPrincipal: Double,
    val remainingInstallments: Int
)

@HiltViewModel
class LoanViewModel @Inject constructor(
    private val loanRepo: LoanRepository,
    private val familyRepo: FamilyRepository
) : ViewModel() {
    val allLoans = loanRepo.getAllLoans().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allMembers = familyRepo.getAllMembers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getPayments(loanId: Long) = loanRepo.getPayments(loanId)

    suspend fun buildLoanSummary(loan: Loan): LoanSummary {
        val paid = loanRepo.getPaidInstallments(loan.id)
        val principalPaid = loanRepo.getTotalPrincipalPaid(loan.id)
        val interestPaid = loanRepo.getTotalInterestPaid(loan.id)
        val outstanding = (loan.loanAmount - principalPaid).coerceAtLeast(0.0)
        val remaining = (loan.tenureMonths - paid).coerceAtLeast(0)
        val member = allMembers.value.find { it.id == loan.familyMemberId }
        return LoanSummary(loan, member?.name ?: "Unknown", paid, principalPaid, interestPaid, outstanding, remaining)
    }

    fun saveLoan(loan: Loan, onDone: () -> Unit) {
        viewModelScope.launch {
            if (loan.id == 0L) loanRepo.insertLoan(loan) else loanRepo.updateLoan(loan)
            onDone()
        }
    }

    fun deleteLoan(id: Long) = viewModelScope.launch { loanRepo.deleteLoan(id) }

    fun markEMIPaid(loan: Loan, installmentNumber: Int, schedule: List<FinancialUtils.AmortisationRow>, onDone: () -> Unit) {
        viewModelScope.launch {
            val row = schedule.getOrNull(installmentNumber - 1) ?: return@launch
            loanRepo.insertPayment(LoanPayment(loanId = loan.id, paymentDate = System.currentTimeMillis(), installmentNumber = installmentNumber, emiPaid = row.emi, principalPaid = row.principal, interestPaid = row.interest, outstandingBalance = row.balance))
            onDone()
        }
    }

    suspend fun getLoan(id: Long) = loanRepo.getLoanById(id)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanListScreen(onAddLoan: () -> Unit, onLoanDetail: (Long) -> Unit, onBack: () -> Unit, vm: LoanViewModel = hiltViewModel()) {
    val loans by vm.allLoans.collectAsState()
    var loanSummaries by remember { mutableStateOf(listOf<LoanSummary>()) }
    LaunchedEffect(loans) { loanSummaries = loans.map { vm.buildLoanSummary(it) } }

    val totalOutstanding = loanSummaries.sumOf { it.outstandingPrincipal }
    val totalEMI = loans.sumOf { it.emiAmount }

    Scaffold(topBar = { TopBarWithBack("Loans", onBack) }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(AppDimens.ScreenPadding),
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
        ) {
            if (loans.isNotEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = LossColor.copy(0.1f))) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Total Outstanding", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(FinancialUtils.formatCurrency(totalOutstanding), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LossColor)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Monthly EMI", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(FinancialUtils.formatCurrency(totalEMI), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            if (loanSummaries.isEmpty()) {
                item {
                    EmptyState(
                        title = "No loans yet",
                        message = "Add your first loan to track outstanding balance, EMI progress, and amortisation.",
                        icon = Icons.Default.AccountBalance,
                        actionLabel = "Add loan",
                        onAction = onAddLoan
                    )
                }
            }
            items(loanSummaries) { summary ->
                LoanCard(summary, onClick = { onLoanDetail(summary.loan.id) }, onDelete = { vm.deleteLoan(summary.loan.id) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanCard(summary: LoanSummary, onClick: () -> Unit, onDelete: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }
    val loan = summary.loan
    val progress = (loan.loanAmount - summary.outstandingPrincipal) / loan.loanAmount
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalance, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(loan.loanName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("${loan.loanType.name.replace("_", " ")} • ${summary.memberName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { showDelete = true }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LoanDetailItem("Outstanding", FinancialUtils.formatCurrency(summary.outstandingPrincipal), LossColor)
                LoanDetailItem("EMI", FinancialUtils.formatCurrency(loan.emiAmount))
                LoanDetailItem("Rate", "${loan.interestRate}%")
                LoanDetailItem("Remaining", "${summary.remainingInstallments} EMIs")
            }
            LinearProgressIndicator(progress = progress.toFloat().coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth().height(6.dp))
            Text("${(progress * 100).toInt()}% repaid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (showDelete) {
        AlertDialog(onDismissRequest = { showDelete = false },
            title = { Text("Delete Loan") }, text = { Text("Delete ${loan.loanName}?") },
            confirmButton = { TextButton(onClick = { onDelete(); showDelete = false }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLoanScreen(loanId: Long?, onBack: () -> Unit, vm: LoanViewModel = hiltViewModel()) {
    val members by vm.allMembers.collectAsState()
    var selectedMember by remember { mutableStateOf<FamilyMember?>(null) }
    var loanName by remember { mutableStateOf("") }
    var loanType by remember { mutableStateOf(LoanType.HOME_LOAN) }
    var lenderName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var loanAmount by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var tenureMonths by remember { mutableStateOf("") }
    var disbursementDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var processingFee by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var emiAmount by remember { mutableStateOf("") }

    val computedEMI = remember(loanAmount, interestRate, tenureMonths) {
        val p = loanAmount.toDoubleOrNull() ?: 0.0
        val r = interestRate.toDoubleOrNull() ?: 0.0
        val n = tenureMonths.toIntOrNull() ?: 0
        if (p > 0 && r > 0 && n > 0) FinancialUtils.calculateEMI(p, r, n) else 0.0
    }
    LaunchedEffect(computedEMI) { if (computedEMI > 0 && emiAmount.isBlank()) emiAmount = "%.2f".format(computedEMI) }
    LaunchedEffect(members) { if (selectedMember == null && members.isNotEmpty()) selectedMember = members.first() }
    LaunchedEffect(loanId) {
        loanId?.let { id ->
            vm.getLoan(id)?.let { l ->
                loanName = l.loanName; loanType = l.loanType; lenderName = l.lenderName
                accountNumber = l.accountNumber; loanAmount = l.loanAmount.toString()
                interestRate = l.interestRate.toString(); tenureMonths = l.tenureMonths.toString()
                disbursementDate = l.disbursementDate; processingFee = l.processingFee.toString()
                emiAmount = l.emiAmount.toString(); notes = l.notes
                selectedMember = members.find { it.id == l.familyMemberId }
            }
        }
    }

    Scaffold(
        topBar = { TopBarWithBack(if (loanId != null) "Edit Loan" else "Add Loan", onBack) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        try {
                            val emi = emiAmount.trim().trimEnd('.').toDoubleOrNull() ?: computedEMI
                            val loan = Loan(id = loanId ?: 0L, familyMemberId = selectedMember!!.id, loanName = loanName, loanType = loanType, lenderName = lenderName, accountNumber = accountNumber, loanAmount = loanAmount.toDoubleOrNull() ?: 0.0, interestRate = interestRate.toDoubleOrNull() ?: 0.0, tenureMonths = tenureMonths.toIntOrNull() ?: 0, disbursementDate = disbursementDate, emiAmount = if (emi > 0) emi else computedEMI, processingFee = processingFee.toDoubleOrNull() ?: 0.0, notes = notes)
                            vm.saveLoan(loan) { onBack() }
                        } catch (e: Exception) { /* ignore malformed input */ }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    enabled = selectedMember != null && loanName.isNotBlank() && loanAmount.isNotBlank() && interestRate.isNotBlank() && tenureMonths.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (loanId != null) "Update Loan" else "Save Loan") }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Loan Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        DropdownField("Family Member *", members, selectedMember, { selectedMember = it }, { it.name })
                        InputField("Loan Name *", loanName, { loanName = it })
                        DropdownField("Loan Type *", LoanType.values().toList(), loanType, { loanType = it }, { it.name.replace("_", " ") })
                        InputField("Lender Name", lenderName, { lenderName = it })
                        InputField("Account / Loan Number", accountNumber, { accountNumber = it })
                        DateField("Disbursement Date *", disbursementDate, { disbursementDate = it })
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Loan Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        InputField("Loan Amount (₹) *", loanAmount, { loanAmount = it; emiAmount = "" }, keyboardType = KeyboardType.Decimal)
                        InputField("Interest Rate (% p.a.) *", interestRate, { interestRate = it; emiAmount = "" }, keyboardType = KeyboardType.Decimal)
                        InputField("Tenure (Months) *", tenureMonths, { tenureMonths = it; emiAmount = "" }, keyboardType = KeyboardType.Number)
                        if (computedEMI > 0) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("Computed EMI", style = MaterialTheme.typography.labelMedium)
                                        Text(FinancialUtils.formatCurrencyFull(computedEMI), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Column {
                                        Text("Total Interest", style = MaterialTheme.typography.labelMedium)
                                        val months = tenureMonths.toIntOrNull() ?: 0
                                        Text(FinancialUtils.formatCurrency(computedEMI * months - (loanAmount.toDoubleOrNull() ?: 0.0)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LossColor)
                                    }
                                }
                            }
                        }
                        InputField("Actual EMI (editable) *", emiAmount, { emiAmount = it }, keyboardType = KeyboardType.Decimal)
                        InputField("Processing Fee (₹)", processingFee, { processingFee = it }, keyboardType = KeyboardType.Decimal)
                        InputField("Notes", notes, { notes = it })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(loanId: Long, onEdit: (Long) -> Unit, onBack: () -> Unit, vm: LoanViewModel = hiltViewModel()) {
    var loan by remember { mutableStateOf<Loan?>(null) }
    var summary by remember { mutableStateOf<LoanSummary?>(null) }
    var schedule by remember { mutableStateOf(listOf<FinancialUtils.AmortisationRow>()) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(loanId, refreshKey) {
        loan = vm.getLoan(loanId)
        loan?.let { l ->
            summary = vm.buildLoanSummary(l)
            try {
                    schedule = if (l.loanAmount > 0 && l.interestRate > 0 && l.tenureMonths > 0 && l.emiAmount > 0)
                        FinancialUtils.generateAmortisationSchedule(l.loanAmount, l.interestRate, l.tenureMonths, l.emiAmount)
                    else emptyList()
                } catch (e: Exception) { schedule = emptyList() }
        }
    }

    val payments by (loan?.let { vm.getPayments(it.id) } ?: flowOf(emptyList())).collectAsState(initial = emptyList())
    val paidInstallments = payments.map { it.installmentNumber }.toSet()

    Scaffold(topBar = { TopBarWithBack("Loan Detail", onBack) { IconButton(onClick = { onEdit(loanId) }) { Icon(Icons.Default.Edit, "Edit") } } }) { padding ->
        loan?.let { l ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(l.loanName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("${l.loanType.name.replace("_", " ")} • ${l.lenderName}".trim(' ', '•'), style = MaterialTheme.typography.bodySmall)
                            Divider()
                            summary?.let { s ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    LoanDetailItem("Loan Amount", FinancialUtils.formatCurrency(l.loanAmount))
                                    LoanDetailItem("Outstanding", FinancialUtils.formatCurrency(s.outstandingPrincipal), LossColor)
                                    LoanDetailItem("EMI", FinancialUtils.formatCurrency(l.emiAmount))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    LoanDetailItem("Rate", "${l.interestRate}% p.a.")
                                    LoanDetailItem("Paid EMIs", "${s.paidInstallments}/${l.tenureMonths}")
                                    LoanDetailItem("Remaining", "${s.remainingInstallments} EMIs")
                                }
                                val progress = (l.loanAmount - s.outstandingPrincipal) / l.loanAmount
                                LinearProgressIndicator(progress = progress.toFloat().coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth().height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Interest Paid: ${FinancialUtils.formatCurrency(s.totalInterestPaid)}", style = MaterialTheme.typography.labelSmall, color = LossColor)
                                    Text("Principal Paid: ${FinancialUtils.formatCurrency(s.totalPrincipalPaid)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
                item { SectionHeader("Amortisation Schedule") }
                items(schedule) { row ->
                    val isPaid = row.installment in paidInstallments
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (isPaid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("#${row.installment}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(32.dp), fontWeight = FontWeight.Bold)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("P: ${FinancialUtils.formatCurrency(row.principal)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    Text("I: ${FinancialUtils.formatCurrency(row.interest)}", style = MaterialTheme.typography.bodySmall, color = LossColor)
                                }
                                Text("Balance: ${FinancialUtils.formatCurrency(row.balance)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(FinancialUtils.formatCurrency(row.emi), fontWeight = FontWeight.Bold)
                            if (!isPaid) {
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(onClick = { vm.markEMIPaid(l, row.installment, schedule) { refreshKey++ } }, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                                    Text("Pay", style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}

@Composable
fun LoanDetailItem(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
