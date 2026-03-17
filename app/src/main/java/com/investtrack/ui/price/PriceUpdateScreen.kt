package com.investtrack.ui.price

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
import com.investtrack.data.database.entities.PriceHistory
import com.investtrack.data.database.entities.SecurityMaster
import com.investtrack.data.database.entities.SecurityType
import com.investtrack.data.repository.PriceRepository
import com.investtrack.data.repository.SecurityRepository
import com.investtrack.data.repository.PriceAutoFetchers
import com.investtrack.ui.common.*
import com.investtrack.utils.DateUtils.toDisplayDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PriceUpdateViewModel @Inject constructor(
    private val secRepo: SecurityRepository,
    private val priceRepo: PriceRepository
) : ViewModel() {
    val allSecurities = secRepo.getAllSecurities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getPriceHistory(secId: Long) = priceRepo.getPriceHistory(secId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun savePrice(secId: Long, price: Double, date: Long, onDone: () -> Unit) = viewModelScope.launch {
        priceRepo.insertPrice(PriceHistory(securityId = secId, priceDate = date, price = price))
        onDone()
    }
    fun deletePrice(secId: Long, date: Long) = viewModelScope.launch { priceRepo.deletePrice(secId, date) }

    suspend fun getSecurityById(id: Long) = secRepo.getSecurityById(id)

    fun autoFetchLatestPrice(sec: SecurityMaster, onDone: (Result<PriceHistory>) -> Unit) {
        viewModelScope.launch {
            try {
                val fetched = when (sec.securityType) {
                    SecurityType.MUTUAL_FUND -> PriceAutoFetchers.fetchAmfiNav(sec.amfiSchemeCode)
                    SecurityType.SHARES -> PriceAutoFetchers.fetchYahooQuote(sec.yahooSymbol)
                    else -> throw IllegalStateException("Auto fetch supported only for Mutual Funds and Shares")
                }

                val priceDate = fetched.epochMillis ?: System.currentTimeMillis()
                val ph = PriceHistory(
                    securityId = sec.id,
                    priceDate = priceDate,
                    price = fetched.price,
                    source = fetched.source
                )
                priceRepo.insertPrice(ph)
                onDone(Result.success(ph))
            } catch (e: Exception) {
                onDone(Result.failure(e))
            }
        }
    }

    fun autoFetchAll(securities: List<SecurityMaster>, onProgress: (done: Int, total: Int) -> Unit, onDone: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            try {
                val eligible = securities.filter { s ->
                    (s.securityType == SecurityType.MUTUAL_FUND && s.amfiSchemeCode.isNotBlank()) ||
                        (s.securityType == SecurityType.SHARES && s.yahooSymbol.isNotBlank())
                }
                val total = eligible.size
                var done = 0
                var savedCount = 0
                onProgress(done, total)
                for (sec in eligible) {
                    val fetched = when (sec.securityType) {
                        SecurityType.MUTUAL_FUND -> PriceAutoFetchers.fetchAmfiNav(sec.amfiSchemeCode)
                        SecurityType.SHARES -> PriceAutoFetchers.fetchYahooQuote(sec.yahooSymbol)
                        else -> continue
                    }
                    val priceDate = fetched.epochMillis ?: System.currentTimeMillis()
                    val ph = PriceHistory(
                        securityId = sec.id,
                        priceDate = priceDate,
                        price = fetched.price,
                        source = fetched.source
                    )
                    priceRepo.insertPrice(ph)
                    savedCount++
                    done++
                    onProgress(done, total)
                }
                onDone(Result.success(savedCount))
            } catch (e: Exception) {
                onDone(Result.failure(e))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceUpdateScreen(preSelectedSecurityId: Long? = null, onBack: () -> Unit, vm: PriceUpdateViewModel = hiltViewModel()) {
    val securities by vm.allSecurities.collectAsState()
    var selectedSecurity by remember { mutableStateOf<SecurityMaster?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var priceInput by remember { mutableStateOf("") }
    var priceDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showSuccess by remember { mutableStateOf(false) }
    var isAutoFetching by remember { mutableStateOf(false) }
    var autoFetchError by remember { mutableStateOf<String?>(null) }
    var isAutoFetchingAll by remember { mutableStateOf(false) }
    var autoAllProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) } // done/total
    var autoAllMessage by remember { mutableStateOf<String?>(null) }

    val priceHistory by remember(selectedSecurity) {
        if (selectedSecurity != null) vm.getPriceHistory(selectedSecurity!!.id)
        else kotlinx.coroutines.flow.MutableStateFlow(emptyList<com.investtrack.data.database.entities.PriceHistory>())
    }.collectAsState(emptyList())

    LaunchedEffect(preSelectedSecurityId, securities) {
        if (preSelectedSecurityId != null && selectedSecurity == null) {
            selectedSecurity = securities.find { it.id == preSelectedSecurityId }
            selectedSecurity?.let { searchQuery = it.securityName }
        }
    }

    val filteredSecurities = securities.filter { searchQuery.isEmpty() || it.securityName.contains(searchQuery, true) || it.securityCode.contains(searchQuery, true) }

    Scaffold(
        topBar = { TopBarWithBack("Update Prices / NAV", onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(AppDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
        ) {
            if (securities.isEmpty()) {
                item {
                    EmptyState(
                        title = "No securities found",
                        message = "Create securities first, then you can update their price/NAV history here.",
                        icon = Icons.Default.Shield
                    )
                }
                return@LazyColumn
            }

            // Global auto-update all configured securities
            item {
                val eligibleCount = securities.count { s ->
                    (s.securityType == SecurityType.MUTUAL_FUND && s.amfiSchemeCode.isNotBlank()) ||
                        (s.securityType == SecurityType.SHARES && s.yahooSymbol.isNotBlank())
                }
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Auto Update", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Configured securities: $eligibleCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                isAutoFetchingAll = true
                                autoAllMessage = null
                                autoFetchError = null
                                vm.autoFetchAll(
                                    securities = securities,
                                    onProgress = { done, total -> autoAllProgress = done to total },
                                    onDone = { result ->
                                        isAutoFetchingAll = false
                                        result.fold(
                                            onSuccess = { saved ->
                                                autoAllMessage = "Updated $saved securities."
                                            },
                                            onFailure = { err ->
                                                autoFetchError = err.message ?: "Auto update all failed"
                                            }
                                        )
                                    }
                                )
                            },
                            enabled = !isAutoFetchingAll && eligibleCount > 0,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isAutoFetchingAll) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(10.dp))
                                val (d, t) = autoAllProgress ?: (0 to 0)
                                Text(if (t > 0) "Updating… ($d/$t)" else "Updating…")
                            } else {
                                Icon(Icons.Default.Sync, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Auto Update All")
                            }
                        }
                        autoAllMessage?.let { msg ->
                            Surface(color = MaterialTheme.colorScheme.primary.copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(msg, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Select Security", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it; showSearch = true },
                            label = { Text("Search Security") },
                            trailingIcon = { Icon(Icons.Default.Search, null) },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )
                        if (showSearch) {
                            if (filteredSecurities.isEmpty() && searchQuery.trim().length >= 2) {
                                EmptyState(
                                    title = "No results",
                                    message = "Try a different name or code.",
                                    icon = Icons.Default.Search
                                )
                            } else if (filteredSecurities.isNotEmpty()) {
                            Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(8.dp)) {
                                Column {
                                    filteredSecurities.take(6).forEach { sec ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                selectedSecurity = sec; searchQuery = sec.securityName; showSearch = false
                                            }.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(sec.securityName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                                Text(sec.securityType.name.replace("_"," "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Divider()
                                    }
                                }
                            }
                            }
                        }
                        selectedSecurity?.let { sec ->
                            PillChip(sec.securityType.name.replace("_"," "), MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            selectedSecurity?.let { sec ->
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Enter Price / NAV", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                            // Auto fetch latest price/NAV (AMFI for MF, Yahoo for Shares)
                            val canAutoFetch = when (sec.securityType) {
                                SecurityType.MUTUAL_FUND -> sec.amfiSchemeCode.isNotBlank()
                                SecurityType.SHARES -> sec.yahooSymbol.isNotBlank()
                                else -> false
                            }
                            if (canAutoFetch) {
                                OutlinedButton(
                                    onClick = {
                                        isAutoFetching = true
                                        autoFetchError = null
                                        vm.autoFetchLatestPrice(sec) { result ->
                                            isAutoFetching = false
                                            result.fold(
                                                onSuccess = { saved ->
                                                    showSuccess = true
                                                    priceInput = ""
                                                    priceDate = saved.priceDate
                                                },
                                                onFailure = { err ->
                                                    autoFetchError = err.message ?: "Auto update failed"
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isAutoFetching,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (isAutoFetching) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(10.dp))
                                        Text("Fetching…")
                                    } else {
                                        Icon(Icons.Default.CloudDownload, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Auto Fetch Latest")
                                    }
                                }
                            } else if (sec.securityType == SecurityType.MUTUAL_FUND || sec.securityType == SecurityType.SHARES) {
                                EmptyState(
                                    title = "Auto update not configured",
                                    message = if (sec.securityType == SecurityType.MUTUAL_FUND)
                                        "Add AMFI Scheme Code in Security Master to auto-fetch NAV."
                                    else
                                        "Add Yahoo Symbol (e.g., TCS.NS) in Security Master to auto-fetch price.",
                                    icon = Icons.Default.Info
                                )
                            }

                            autoFetchError?.let { msg ->
                                ErrorBanner(
                                    message = msg,
                                    actionLabel = "Dismiss",
                                    onAction = { autoFetchError = null }
                                )
                            }

                            DateField("Price Date", priceDate, { priceDate = it })
                            InputField("Price / NAV (₹)", priceInput, { priceInput = it; showSuccess = false }, keyboardType = KeyboardType.Decimal)
                            Button(
                                onClick = {
                                    val p = priceInput.toDoubleOrNull() ?: return@Button
                                    vm.savePrice(sec.id, p, priceDate) { priceInput = ""; showSuccess = true }
                                },
                                modifier = Modifier.fillMaxWidth(), enabled = priceInput.isNotBlank(), shape = RoundedCornerShape(12.dp)
                            ) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Save Price") }
                            if (showSuccess) {
                                Surface(color = MaterialTheme.colorScheme.primary.copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Price saved!", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }

                if (priceHistory.isNotEmpty()) {
                    item { SectionHeader("Price History") }
                    items(priceHistory.take(10), key = { it.id }) { ph ->
                        Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ph.priceDate.toDisplayDate(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text(ph.source, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("₹${"%.4f".format(ph.price)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { vm.deletePrice(sec.id, ph.priceDate) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
