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
                if (member.panNumber.isNotBlank()) {
                    Text("PAN: ${member.panNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                pan          = m.panNumber        // entity field: pan
                email        = m.email
                phone        = m.phoneNumber      // entity field: phone
                aadhaar      = m.aadhaarNumber    // entity field: aadhaar
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
                            panNumber    = pan.trim(),    // entity field: pan
                            email        = email.trim(),
                            phoneNumber  = phone.trim(),  // entity field: phone
                            aadhaarNumber = aadhaar.trim(),// entity field: aadhaar
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
                                    familyMemberId = editMemberId ?: 0L,) 100.0 else 0.0
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
    }
}
