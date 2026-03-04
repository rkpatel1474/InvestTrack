package com.investtrack.ui.price

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.investtrack.data.database.entities.PriceHistory
import com.investtrack.data.database.entities.SecurityMaster
import com.investtrack.data.repository.PriceRepository
import com.investtrack.data.repository.SecurityRepository
import com.investtrack.ui.common.DateField
import com.investtrack.ui.common.InputField
import com.investtrack.ui.common.SectionHeader
import com.investtrack.ui.common.TopBarWithBack
import com.investtrack.utils.DateUtils.toDisplayDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PriceViewModel @Inject constructor(
    private val priceRepo: PriceRepository,
    private val securityRepo: SecurityRepository
) : ViewModel() {

    private val _selectedSecurityId = MutableStateFlow<Long?>(null)
    val selectedSecurityId: StateFlow<Long?> = _selectedSecurityId.asStateFlow()

    fun selectSecurity(id: Long) { _selectedSecurityId.value = id }
    fun getPriceHistory(securityId: Long): Flow<List<PriceHistory>> = priceRepo.getPriceHistory(securityId)
    suspend fun getSecurity(id: Long) = securityRepo.getSecurityById(id)
    suspend fun searchSecurities(q: String) = securityRepo.searchSecurities(q)

    fun savePrice(securityId: Long, date: Long, price: Double, onDone: () -> Unit) {
        viewModelScope.launch {
            priceRepo.insertPrice(PriceHistory(securityId = securityId, priceDate = date, price = price))
            onDone()
        }
    }

    fun deletePrice(securityId: Long, date: Long) = viewModelScope.launch {
        priceRepo.deletePrice(securityId, date)
    }
}

@Composable
fun PriceUpdateScreen(
    preSelectedSecurityId: Long?,
    onBack: () -> Unit,
    vm: PriceViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    var selectedSecurity by remember { mutableStateOf<SecurityMaster?>(null) }
    var securityQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<SecurityMaster>()) }
    var showSearch by remember { mutableStateOf(false) }
    var priceDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var priceValue by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    var priceHistory by remember { mutableStateOf(listOf<PriceHistory>()) }

    LaunchedEffect(preSelectedSecurityId) {
        preSelectedSecurityId?.let { id ->
            selectedSecurity = vm.getSecurity(id)
            selectedSecurity?.let { securityQuery = it.securityName }
        }
    }

    LaunchedEffect(selectedSecurity) {
        selectedSecurity?.let { sec ->
            vm.getPriceHistory(sec.id).collect { history ->
                priceHistory = history
            }
        }
    }

    Scaffold(topBar = { TopBarWithBack("Update Price / NAV", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Add / Update Price", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = securityQuery,
                            onValueChange = { q ->
                                securityQuery = q
                                showSearch = true
                                scope.launch { searchResults = vm.searchSecurities(q) }
                            },
                            label = { Text("Search Security") },
                            trailingIcon = { Icon(Icons.Default.Search, null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        if (showSearch && searchResults.isNotEmpty()) {
                            Card(elevation = CardDefaults.cardElevation(8.dp)) {
                                searchResults.take(5).forEach { sec ->
                                    ListItem(
                                        headlineContent = { Text(sec.securityName) },
                                        supportingContent = { Text(sec.securityCode) },
                                        modifier = Modifier.clickable {
                                            selectedSecurity = sec
                                            securityQuery = sec.securityName
                                            showSearch = false
                                        }
                                    )
                                    Divider()
                                }
                            }
                        }
                        DateField("Price Date *", priceDate, { priceDate = it })
                        InputField("Price / NAV (₹) *", priceValue, { priceValue = it; showSuccess = false }, keyboardType = KeyboardType.Decimal)
                        if (showSuccess) {
                            Text("✅ Price saved!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                val sec = selectedSecurity ?: return@Button
                                val p = priceValue.toDoubleOrNull() ?: return@Button
                                vm.savePrice(sec.id, priceDate, p) { showSuccess = true; priceValue = "" }
                            },
                            enabled = selectedSecurity != null && priceValue.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Save Price") }
                    }
                }
            }
            if (priceHistory.isNotEmpty()) {
                item { SectionHeader("Price History for ${selectedSecurity?.securityName ?: ""}") }
                items(priceHistory.take(20)) { ph ->
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ph.priceDate.toDisplayDate(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Source: ${ph.source}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("₹${"%.4f".format(ph.price)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { vm.deletePrice(ph.securityId, ph.priceDate) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
