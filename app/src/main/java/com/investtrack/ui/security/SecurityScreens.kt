package com.investtrack.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investtrack.data.database.entities.AssetClass
import com.investtrack.data.database.entities.CouponFrequency
import com.investtrack.data.database.entities.InsuranceType
import com.investtrack.data.database.entities.MFSchemeType
import com.investtrack.data.database.entities.SecurityMaster
import com.investtrack.data.database.entities.SecurityType
import com.investtrack.data.repository.SecurityRepository
import com.investtrack.ui.common.DateField
import com.investtrack.ui.common.DropdownField
import com.investtrack.ui.common.EmptyState
import com.investtrack.ui.common.InputField
import com.investtrack.ui.common.PillChip
import com.investtrack.ui.common.TopBarWithBack
import com.investtrack.ui.dashboard.securityTypeIcon
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(private val repo: SecurityRepository) : ViewModel() {
    val allSecurities = repo.getAllSecurities().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _filterType = MutableStateFlow<SecurityType?>(null)
    val filterType: StateFlow<SecurityType?> = _filterType

    val filteredSecurities = combine(allSecurities, _filterType) { list, type ->
        if (type == null) list else list.filter { it.securityType == type }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(type: SecurityType?) { _filterType.value = type }
    suspend fun getSecurity(id: Long) = repo.getSecurityById(id)
    fun saveSecurity(security: SecurityMaster, onDone: () -> Unit) {
        viewModelScope.launch {
            if (security.id == 0L) repo.insertSecurity(security) else repo.updateSecurity(security)
            onDone()
        }
    }
    fun deleteSecurity(id: Long) = viewModelScope.launch { repo.deleteSecurity(id) }
}

@Composable
fun SecurityListScreen(onAddSecurity: () -> Unit, onEditSecurity: (Long) -> Unit, onBack: () -> Unit, vm: SecurityViewModel = hiltViewModel()) {
    val securities by vm.filteredSecurities.collectAsState()
    val filterType by vm.filterType.collectAsState()
    Scaffold(
        topBar = { TopBarWithBack("Security Master", onBack) { IconButton(onClick = onAddSecurity) { Icon(Icons.Default.Add, "Add") } } },
        floatingActionButton = { FloatingActionButton(onClick = onAddSecurity) { Icon(Icons.Default.Add, null) } }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = filterType == null, onClick = { vm.setFilter(null) }, label = { Text("All") }) }
                items(SecurityType.values()) { type ->
                    FilterChip(selected = filterType == type, onClick = { vm.setFilter(if (filterType == type) null else type) }, label = { Text(type.name.replace("_", " ")) })
                }
            }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (securities.isEmpty()) item { EmptyState("No securities found. Tap + to add one.") }
                items(securities) { sec ->
                    SecurityCard(sec, onEdit = { onEditSecurity(sec.id) }, onDelete = { vm.deleteSecurity(sec.id) })
                }
            }
        }
    }
}

@Composable
fun SecurityCard(sec: SecurityMaster, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(securityTypeIcon(sec.securityType), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(sec.securityName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(sec.securityCode, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PillChip(sec.securityType.name.replace("_", " "), MaterialTheme.colorScheme.primary)
                    PillChip(sec.assetClass.name, MaterialTheme.colorScheme.secondary)
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit") }
            IconButton(onClick = { showDelete = true }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
        }
    }
    if (showDelete) {
        AlertDialog(onDismissRequest = { showDelete = false },
            title = { Text("Delete Security") }, text = { Text("Delete ${sec.securityName}?") },
            confirmButton = { TextButton(onClick = { onDelete(); showDelete = false }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } })
    }
}

@Composable
fun AddEditSecurityScreen(securityId: Long?, onBack: () -> Unit, vm: SecurityViewModel = hiltViewModel()) {
    var securityCode by remember { mutableStateOf("") }
    var securityName by remember { mutableStateOf("") }
    var securityType by remember { mutableStateOf(SecurityType.MUTUAL_FUND) }
    var assetClass by remember { mutableStateOf(AssetClass.EQUITY) }
    var schemeType by remember { mutableStateOf<MFSchemeType?>(null) }
    var amcName by remember { mutableStateOf("") }
    var isinCode by remember { mutableStateOf("") }
    var couponRate by remember { mutableStateOf("") }
    var couponFrequency by remember { mutableStateOf<CouponFrequency?>(null) }
    var firstCouponDate by remember { mutableStateOf<Long?>(null) }
    var maturityDate by remember { mutableStateOf<Long?>(null) }
    var faceValue by remember { mutableStateOf("") }
    var npsSubType by remember { mutableStateOf("") }
    var pfAccountNumber by remember { mutableStateOf("") }
    var fdInterestRate by remember { mutableStateOf("") }
    var fdTenure by remember { mutableStateOf("") }
    var insuranceType by remember { mutableStateOf<InsuranceType?>(null) }
    var premiumFrequency by remember { mutableStateOf<CouponFrequency?>(null) }
    var sumAssured by remember { mutableStateOf("") }
    var policyTerm by remember { mutableStateOf("") }
    var premiumTerm by remember { mutableStateOf("") }
    var policyNumber by remember { mutableStateOf("") }
    var insurerName by remember { mutableStateOf("") }
    var propertyAddress by remember { mutableStateOf("") }
    var propertyType by remember { mutableStateOf("") }

    LaunchedEffect(securityId) {
        securityId?.let { id ->
            vm.getSecurity(id)?.let { s ->
                securityCode = s.securityCode; securityName = s.securityName; securityType = s.securityType
                assetClass = s.assetClass; schemeType = s.schemeType; amcName = s.amcName; isinCode = s.isinCode
                couponRate = s.couponRate?.toString() ?: ""; couponFrequency = s.couponFrequency
                firstCouponDate = s.firstCouponDate; maturityDate = s.maturityDate; faceValue = s.faceValue?.toString() ?: ""
                npsSubType = s.npsSubType; pfAccountNumber = s.pfAccountNumber
                fdInterestRate = s.interestRate?.toString() ?: ""; fdTenure = s.fdTenureMonths?.toString() ?: ""
                insuranceType = s.insuranceType; premiumFrequency = s.premiumFrequency
                sumAssured = s.sumAssured?.toString() ?: ""; policyTerm = s.policyTerm?.toString() ?: ""
                premiumTerm = s.premiumTerm?.toString() ?: ""; policyNumber = s.policyNumber; insurerName = s.insurerName
                propertyAddress = s.propertyAddress; propertyType = s.propertyType
            }
        }
    }

    Scaffold(
        topBar = { TopBarWithBack(if (securityId != null) "Edit Security" else "Add Security", onBack) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                androidx.compose.material3.Button(
                    onClick = {
                        val sec = SecurityMaster(id = securityId ?: 0L, securityCode = securityCode, securityName = securityName, securityType = securityType, assetClass = assetClass, schemeType = schemeType, amcName = amcName, isinCode = isinCode, couponRate = couponRate.toDoubleOrNull(), couponFrequency = couponFrequency, firstCouponDate = firstCouponDate, maturityDate = maturityDate, faceValue = faceValue.toDoubleOrNull(), npsSubType = npsSubType, pfAccountNumber = pfAccountNumber, interestRate = fdInterestRate.toDoubleOrNull(), fdTenureMonths = fdTenure.toIntOrNull(), insuranceType = insuranceType, premiumFrequency = premiumFrequency, sumAssured = sumAssured.toDoubleOrNull(), policyTerm = policyTerm.toIntOrNull(), premiumTerm = premiumTerm.toIntOrNull(), policyNumber = policyNumber, insurerName = insurerName, propertyAddress = propertyAddress, propertyType = propertyType)
                        vm.saveSecurity(sec) { onBack() }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    enabled = securityCode.isNotBlank() && securityName.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (securityId != null) "Update Security" else "Save Security") }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Basic Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        DropdownField("Security Type *", SecurityType.values().toList(), securityType, { securityType = it; assetClass = when (it) { SecurityType.MUTUAL_FUND, SecurityType.SHARES -> AssetClass.EQUITY; SecurityType.BOND, SecurityType.GOI_BOND, SecurityType.FD -> AssetClass.DEBT; SecurityType.PROPERTY -> AssetClass.REAL_ESTATE; SecurityType.GOLD -> AssetClass.GOLD; else -> AssetClass.OTHER } }, { it.name.replace("_", " ") })
                        InputField("Security Code *", securityCode, { securityCode = it.uppercase() })
                        InputField("Security Name *", securityName, { securityName = it })
                        DropdownField("Asset Class", AssetClass.values().toList(), assetClass, { assetClass = it }, { it.name.replace("_", " ") })
                    }
                }
            }
            when (securityType) {
                SecurityType.MUTUAL_FUND -> item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Mutual Fund Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            DropdownField("Scheme Type", MFSchemeType.values().toList(), schemeType, { schemeType = it }, { it.name.replace("_", " ") })
                            InputField("AMC Name", amcName, { amcName = it })
                            InputField("ISIN Code", isinCode, { isinCode = it.uppercase() })
                        }
                    }
                }
                SecurityType.BOND, SecurityType.GOI_BOND -> item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Bond Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            InputField("Face Value", faceValue, { faceValue = it }, keyboardType = KeyboardType.Decimal)
                            InputField("Coupon Rate (%)", couponRate, { couponRate = it }, keyboardType = KeyboardType.Decimal)
                            DropdownField("Coupon Frequency", CouponFrequency.values().toList(), couponFrequency, { couponFrequency = it }, { it.name.replace("_", " ") })
                            DateField("First Coupon Date", firstCouponDate, { firstCouponDate = it })
                            DateField("Maturity Date", maturityDate, { maturityDate = it })
                        }
                    }
                }
                SecurityType.NPS -> item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("NPS Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            InputField("Account / PRAN", npsSubType, { npsSubType = it })
                            InputField("Fund Manager / Tier", pfAccountNumber, { pfAccountNumber = it })
                        }
                    }
                }
                SecurityType.PF -> item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("PF Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            InputField("UAN / Account No", pfAccountNumber, { pfAccountNumber = it })
                        }
                    }
                }
                SecurityType.FD -> item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("FD Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            InputField("Interest Rate (%)", fdInterestRate, { fdInterestRate = it }, keyboardType = KeyboardType.Decimal)
                            InputField("Tenure (Months)", fdTenure, { fdTenure = it }, keyboardType = KeyboardType.Number)
                            DateField("Maturity Date", maturityDate, { maturityDate = it })
                        }
                    }
                }
                SecurityType.INSURANCE -> item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Insurance Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            DropdownField("Insurance Type", InsuranceType.values().toList(), insuranceType, { insuranceType = it }, { it.name.replace("_", " ") })
                            InputField("Policy Number", policyNumber, { policyNumber = it })
                            InputField("Insurer Name", insurerName, { insurerName = it })
                            InputField("Sum Assured (₹)", sumAssured, { sumAssured = it }, keyboardType = KeyboardType.Decimal)
                            InputField("Policy Term (Years)", policyTerm, { policyTerm = it }, keyboardType = KeyboardType.Number)
                            InputField("Premium Paying Term (Years)", premiumTerm, { premiumTerm = it }, keyboardType = KeyboardType.Number)
                            DropdownField("Premium Frequency", CouponFrequency.values().toList(), premiumFrequency, { premiumFrequency = it }, { it.name.replace("_", " ") })
                            DateField("Maturity Date", maturityDate, { maturityDate = it })
                        }
                    }
                }
                SecurityType.PROPERTY -> item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Property Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            InputField("Property Address", propertyAddress, { propertyAddress = it })
                            InputField("Property Type (Residential/Commercial/Plot)", propertyType, { propertyType = it })
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
