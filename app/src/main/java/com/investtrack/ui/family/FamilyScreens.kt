package com.investtrack.ui.family

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
import com.investtrack.data.database.entities.FamilyMember
import com.investtrack.data.database.entities.Relationship
import com.investtrack.data.repository.FamilyRepository
import com.investtrack.ui.common.*
import com.investtrack.ui.theme.LossColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FamilyViewModel @Inject constructor(private val repo: FamilyRepository) : ViewModel() {
    val allMembers = repo.getAllMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getMemberById(id: Long, cb: (FamilyMember?) -> Unit) = viewModelScope.launch { cb(repo.getMemberById(id)) }
    fun saveMember(m: FamilyMember, onDone: () -> Unit) = viewModelScope.launch {
        if (m.id == 0L) repo.insertMember(m) else repo.updateMember(m)
        onDone()
    }
    fun deleteMember(id: Long) = viewModelScope.launch { repo.deleteMember(id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyListScreen(onAddMember: () -> Unit, onEditMember: (Long) -> Unit, onBack: () -> Unit, vm: FamilyViewModel = hiltViewModel()) {
    val members by vm.allMembers.collectAsState()
    Scaffold(
        topBar = { TopBarWithBack("Family Members", onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMember, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, "Add")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (members.isEmpty()) item { EmptyState("No family members added yet.", Icons.Default.People) }
            items(members, key = { it.id }) { m ->
                MemberCard(m, onEdit = { onEditMember(m.id) }, onDelete = { vm.deleteMember(m.id) })
            }
        }
    }
}

@Composable
fun MemberCard(member: FamilyMember, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Default.Person, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                PillChip(member.relationship.name, MaterialTheme.colorScheme.secondary)
                if (member.panNumberNumber.isNotEmpty()) Text("PAN: ${member.panNumberNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = { showConfirm = true }) { Icon(Icons.Default.Delete, null, tint = LossColor) }
        }
    }
    if (showConfirm) AlertDialog(
        onDismissRequest = { showConfirm = false },
        title = { Text("Delete Member") },
        text = { Text("Remove ${member.name} and all their data?") },
        confirmButton = { TextButton(onClick = { onDelete(); showConfirm = false }) { Text("Delete", color = LossColor) } },
        dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMemberScreen(editMemberId: Long? = null, onBack: () -> Unit, vm: FamilyViewModel = hiltViewModel()) {
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf(Relationship.SELF) }
    var pan by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(editMemberId) {
        editMemberId?.let { id ->
            vm.getMemberById(id) { m ->
                m?.let {
                    name = it.name
                    relationship = it.relationship
                    panNumber = it.pan
                    email = it.email
                    phoneNumber = it.phone
                    dob = it.dateOfBirth
                }
            }
        }
    }

    Scaffold(
        topBar = { TopBarWithBack(if (editMemberId != null) "Edit Member" else "Add Member", onBack) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.background) {
                Button(
                    onClick = {
                        val m = FamilyMember(id = editMemberId ?: 0L, name = name.trim(), relationship = relationship, panNumber = pan.trim(), email = email.trim(), phoneNumber = phone.trim(), dateOfBirth = dob)
                        vm.saveMember(m) { onBack() }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp), enabled = name.isNotBlank(), shape = RoundedCornerShape(12.dp)
                ) { Text(if (editMemberId != null) "Update" else "Save") }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                FormCard("Personal Details") {
                    InputField("Full Name *", name, { name = it })
                    DropdownField("Relationship *", Relationship.values().toList(), relationship, { relationship = it }, { it.name })
                    dob?.let {
                        DateField("Date of Birth", it, { d -> dob = d })
                    } ?: run {
                        OutlinedButton(onClick = { dob = System.currentTimeMillis() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.CalendarToday, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Set Date of Birth")
                        }
                    }
                }
            }
            item {
                FormCard("Contact & Identity") {
                    InputField("PAN Number", pan, { panNumber = it.uppercase() })
                    InputField("Email", email, { email = it }, keyboardType = KeyboardType.Email)
                    InputField("Phone", phone, { phoneNumber = it }, keyboardType = KeyboardType.Phone)
                }
            }
        }
    }
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
