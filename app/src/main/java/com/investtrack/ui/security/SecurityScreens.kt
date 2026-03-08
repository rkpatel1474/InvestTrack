package com.investtrack.ui.security

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investtrack.data.database.entities.*
import com.investtrack.data.repository.SecurityRepository
import com.investtrack.ui.common.*
import com.investtrack.ui.theme.LossColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(private val repo: SecurityRepository) : ViewModel() {
    val allSecurities = repo.getAllSecurities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getById(id: Long, cb: (SecurityMaster?) -> Unit) = viewModelScope.launch { cb(repo.getSecurityById(id)) }
    fun save(s: SecurityMaster, onDone: () -> Unit) = viewModelScope.launch {
        if (s.id == 0L) repo.insertSecurity(s) else repo.updateSecurity(s)
        onDone()
    }
    fun delete(id: Long) = viewModelScope.launch { repo.deleteSecurity(id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityListScreen(onAddSecurity: () -> Unit, onEditSecurity: (Long) -> Unit, onBack: () -> Unit, vm: SecurityViewModel = hiltViewModel()) {
    val securities by vm.allSecurities.collectAsState()
    var search by remember { mutableStateOf("") }
    val filtered = securities.filter { search.isEmpty() || it.securityName.contains(search, true) || it.securityCode.contains(search, true) }

    Scaffold(
        topBar = { TopBarWithBack("Security Master", onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSecurity, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, "Add")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                OutlinedTextField(
                    value = search, onValueChange = { search = it },
                    placeholder = { Text("Search security...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                )
            }
            if (filtered.isEmpty()) item { EmptyState("No securities found.", Icons.Default.Shield) }
            items(filtered, key = { it.id }) { s ->
                SecurityCard(s, onEdit = { onEditSecurity(s.id) }, onDelete = { vm.delete(s.id) })
            }
        }
    }
}

@Composable
fun SecurityCard(sec: SecurityMaster, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Default.BarChart, securityTypeColor(sec.securityType))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(sec.securityName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PillChip(sec.securityType.name.replace("_"," "), securityTypeColor(sec.securityType))
                    if (sec.securityCode.isNotEmpty()) Text(sec.securityCode, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (sec.sector.isNotEmpty()) Text(sec.sector, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
            IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, null, tint = LossColor, modifier = Modifier.size(18.dp)) }
        }
    }
    if (showConfirm) AlertDialog(onDismissRequest = { showConfirm = false },
        title = { Text("Delete Security") }, text = { Text("Delete ${sec.securityName}? Transactions will be affected.") },
        confirmButton = { TextButton(onClick = { onDelete(); showConfirm = false }) { Text("Delete", color = LossColor) } },
        dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancel") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSecurityScreen(editSecurityId: Long? = null, onBack: () -> Unit, vm: SecurityViewModel = hiltViewModel()) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(SecurityType.MUTUAL_FUND) }
    var assetClass by remember { mutableStateOf(AssetClass.EQUITY) }
    var isin by remember { mutableStateOf("") }
    var amc by remember { mutableStateOf("") }
    var schemeType by remember { mutableStateOf(MFSchemeType.OTHER) }
    var exitLoad by remember { mutableStateOf("") }
    var expenseRatio by remember { mutableStateOf("") }
    var couponRate by remember { mutableStateOf("") }
    var couponFreq by remember { mutableStateOf(CouponFrequency.ANNUALLY) }
    var maturityDate by remember { mutableStateOf<Long?>(null) }
    var faceValue by remember { mutableStateOf("") }
    var creditRating by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var fdTenure by remember { mutableStateOf("") }
    var pfAccountNo by remember { mutableStateOf("") }
    var uanNumber by remember { mutableStateOf("") }
    var insType by remember { mutableStateOf(InsuranceType.TERM) }
    var sumAssured by remember { mutableStateOf("") }
    var policyNo by remember { mutableStateOf("") }
    var insurerName by remember { mutableStateOf("") }
    var policyTerm by remember { mutableStateOf("") }
    var premTerm by remember { mutableStateOf("") }
    var premFreq by remember { mutableStateOf(CouponFrequency.ANNUALLY) }
    var propertyAddress by remember { mutableStateOf("") }
    var propertyType by remember { mutableStateOf("") }
    var carpetArea by remember { mutableStateOf("") }
    var goldPurity by remember { mutableStateOf("") }
    var goldForm by remember { mutableStateOf("") }
    var cryptoSymbol by remember { mutableStateOf("") }

    LaunchedEffect(editSecurityId) {
        editSecurityId?.let { id ->
            vm.getById(id) { s ->
                s?.let {
                    name = it.securityName; code = it.securityCode; type = it.securityType
                    assetClass = it.assetClass; isin = it.isin; amc = it.sector
                    schemeType = it.mfSchemeType ?: MFSchemeType.OTHER
                    exitLoad = ""; expenseRatio = ""
                    couponRate = it.couponRate?.toString() ?: ""
                    couponFreq = it.couponFrequency ?: CouponFrequency.ANNUALLY
                    faceValue = it.faceValue?.toString() ?: ""; creditRating = ""
                    interestRate = ""; fdTenure = ""
                    pfAccountNo = ""; uanNumber = ""
                    insType = it.insuranceType ?: InsuranceType.TERM
                    sumAssured = it.sumAssured?.toString() ?: ""; policyNo = ""; insurerName = ""
                    policyTerm = ""; premTerm = ""
                    premFreq = it.couponFrequency ?: CouponFrequency.ANNUALLY
                    propertyAddress = ""; propertyType = ""
                    carpetArea = ""
                    goldPurity = ""; goldForm = it.goldForm; cryptoSymbol = it.cryptoSymbol
                    maturityDate = it.maturityDate
                }
            }
        }
    }

    Scaffold(
        topBar = { TopBarWithBack(if (editSecurityId != null) "Edit Security" else "Add Security", onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.background) {
                Button(
                    onClick = {
                        val s = SecurityMaster(
                            id = editSecurityId ?: 0L, securityName = name.trim(), securityCode = code.trim(),
                            securityType = type, assetClass = assetClass, isin = isin.trim(), sector = amc.trim(),
                            mfSchemeType = if (type == SecurityType.MUTUAL_FUND) schemeType else null,
                            exitLoadPercent = exitLoad.toDoubleOrNull(), expenseRatio = expenseRatio.toDoubleOrNull(),
                            couponRate = couponRate.toDoubleOrNull(), couponFrequency = if (couponRate.isNotEmpty()) couponFreq else null,
                            maturityDate = maturityDate, faceValue = faceValue.toDoubleOrNull(), creditRating = creditRating,
                            interestRate = interestRate.toDoubleOrNull(), fdTenureMonths = fdTenure.toIntOrNull(),
                            pfAccountNumber = pfAccountNo, uanNumber = uanNumber,
                            insuranceType = if (type == SecurityType.INSURANCE) insType else null,
                            sumAssured = sumAssured.toDoubleOrNull(), policyNumber = policyNo, insurerName = insurerName,
                            policyTerm = policyTerm.toIntOrNull(), premiumTerm = premTerm.toIntOrNull(),
                            premiumFrequency = if (type == SecurityType.INSURANCE) premFreq else null,
                            propertyAddress = propertyAddress, propertyType = propertyType,
                            carpetArea = carpetArea.toDoubleOrNull(),
                            goldPurity = goldPurity, goldForm = goldForm, cryptoSymbol = cryptoSymbol
                        )
                        vm.save(s) { onBack() }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp), enabled = name.isNotBlank() && code.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (editSecurityId != null) "Update" else "Save") }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                FormCard("Basic Details") {
                    InputField("Security Name *", name, { name = it })
                    InputField("Security Code *", code, { code = it.uppercase() })
                    DropdownField("Type *", SecurityType.values().toList(), type, { type = it; assetClass = defaultAssetClass(it) }, { it.name.replace("_"," ") })
                    DropdownField("Asset Class", AssetClass.values().toList(), assetClass, { assetClass = it }, { it.name.replace("_"," ") })
                    InputField("ISIN Code", isin, { isin = it.uppercase() })
                }
            }

            // Type-specific fields
            if (type == SecurityType.MUTUAL_FUND) {
                item {
                    FormCard("Mutual Fund Details") {
                        InputField("AMC Name", amc, { amc = it })
                        DropdownField("Scheme Type", MFSchemeType.values().toList(), schemeType, { mfSchemeType = it }, { it.name.replace("_"," ") })
                        InputField("Exit Load (%)", exitLoad, { exitLoad = it }, keyboardType = KeyboardType.Decimal)
                        InputField("Expense Ratio (%)", expenseRatio, { expenseRatio = it }, keyboardType = KeyboardType.Decimal)
                    }
                }
            }
            if (type in listOf(SecurityType.BOND, SecurityType.GOI_BOND)) {
                item {
                    FormCard("Bond Details") {
                        InputField("Coupon Rate (%)", couponRate, { couponRate = it }, keyboardType = KeyboardType.Decimal)
                        DropdownField("Coupon Frequency", CouponFrequency.values().toList(), couponFreq, { couponFreq = it }, { it.name.replace("_"," ") })
                        InputField("Face Value (₹)", faceValue, { faceValue = it }, keyboardType = KeyboardType.Decimal)
                        InputField("Credit Rating", creditRating, { creditRating = it.uppercase() })
                        maturityDate?.let { DateField("Maturity Date", it, { d -> maturityDate = d }) }
                            ?: OutlinedButton(onClick = { maturityDate = System.currentTimeMillis() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Set Maturity Date") }
                    }
                }
            }
            if (type == SecurityType.FD) {
                item {
                    FormCard("FD Details") {
                        InputField("Interest Rate (%)", interestRate, { interestRate = it }, keyboardType = KeyboardType.Decimal)
                        InputField("Tenure (months)", fdTenure, { fdTenure = it }, keyboardType = KeyboardType.Number)
                        maturityDate?.let { DateField("Maturity Date", it, { d -> maturityDate = d }) }
                            ?: OutlinedButton(onClick = { maturityDate = System.currentTimeMillis() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Set Maturity Date") }
                    }
                }
            }
            if (type in listOf(SecurityType.NPS, SecurityType.PF)) {
                item {
                    FormCard("NPS / PF Details") {
                        InputField("Account Number", pfAccountNo, { pfAccountNo = it })
                        if (type == SecurityType.PF) InputField("UAN Number", uanNumber, { uanNumber = it })
                    }
                }
            }
            if (type == SecurityType.INSURANCE) {
                item {
                    FormCard("Insurance Details") {
                        DropdownField("Insurance Type", InsuranceType.values().toList(), insType, { insType = it }, { it.name })
                        InputField("Insurer Name", insurerName, { insurerName = it })
                        InputField("Policy Number", policyNo, { policyNo = it })
                        InputField("Sum Assured (₹)", sumAssured, { sumAssured = it }, keyboardType = KeyboardType.Decimal)
                        InputField("Policy Term (years)", policyTerm, { policyTerm = it }, keyboardType = KeyboardType.Number)
                        InputField("Premium Term (years)", premTerm, { premTerm = it }, keyboardType = KeyboardType.Number)
                        DropdownField("Premium Frequency", CouponFrequency.values().toList(), premFreq, { premFreq = it }, { it.name.replace("_"," ") })
                    }
                }
            }
            if (type == SecurityType.PROPERTY) {
                item {
                    FormCard("Property Details") {
                        InputField("Property Type", propertyType, { propertyType = it })
                        InputField("Address", propertyAddress, { propertyAddress = it }, singleLine = false)
                        InputField("Carpet Area (sq ft)", carpetArea, { carpetArea = it }, keyboardType = KeyboardType.Decimal)
                    }
                }
            }
            if (type == SecurityType.GOLD) {
                item {
                    FormCard("Gold Details") {
                        InputField("Purity (e.g. 24K, 22K)", goldPurity, { goldPurity = it })
                        InputField("Form (Coin/Bar/Jewellery/SGB)", goldForm, { goldForm = it })
                    }
                }
            }
            if (type == SecurityType.CRYPTO) {
                item {
                    FormCard("Crypto Details") {
                        InputField("Symbol (e.g. BTC, ETH)", cryptoSymbol, { cryptoSymbol = it.uppercase() })
                    }
                }
            }
        }
    }
}

fun defaultAssetClass(type: SecurityType) = when (type) {
    SecurityType.MUTUAL_FUND -> AssetClass.EQUITY
    SecurityType.SHARES -> AssetClass.EQUITY
    SecurityType.BOND, SecurityType.GOI_BOND -> AssetClass.DEBT
    SecurityType.NPS -> AssetClass.HYBRID
    SecurityType.PF -> AssetClass.DEBT
    SecurityType.FD -> AssetClass.DEBT
    SecurityType.INSURANCE -> AssetClass.OTHER
    SecurityType.PROPERTY -> AssetClass.REAL_ESTATE
    SecurityType.GOLD -> AssetClass.GOLD
    SecurityType.CRYPTO -> AssetClass.COMMODITY
    SecurityType.OTHER -> AssetClass.OTHER
}

fun securityTypeColor(type: SecurityType) = when (type) {
    SecurityType.MUTUAL_FUND -> androidx.compose.ui.graphics.Color(0xFF4A90E2)
    SecurityType.SHARES      -> androidx.compose.ui.graphics.Color(0xFF00C896)
    SecurityType.BOND        -> androidx.compose.ui.graphics.Color(0xFF9B59B6)
    SecurityType.GOI_BOND    -> androidx.compose.ui.graphics.Color(0xFF1ABC9C)
    SecurityType.NPS         -> androidx.compose.ui.graphics.Color(0xFFE67E22)
    SecurityType.PF          -> androidx.compose.ui.graphics.Color(0xFF3498DB)
    SecurityType.FD          -> androidx.compose.ui.graphics.Color(0xFFFFAB00)
    SecurityType.INSURANCE   -> androidx.compose.ui.graphics.Color(0xFFE74C3C)
    SecurityType.PROPERTY    -> androidx.compose.ui.graphics.Color(0xFF795548)
    SecurityType.GOLD        -> androidx.compose.ui.graphics.Color(0xFFFFBF00)
    SecurityType.CRYPTO      -> androidx.compose.ui.graphics.Color(0xFFFF6B35)
    SecurityType.OTHER       -> androidx.compose.ui.graphics.Color(0xFF95A5A6)
}

@Composable
private fun FormCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}
