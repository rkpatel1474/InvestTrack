package com.investtrack.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.investtrack.ui.common.AppTopBar
import com.investtrack.ui.settings.SettingsSectionHeader

@Composable
fun MoreScreen(
    onNavigateToFamily: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToPriceUpdate: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        topBar = { AppTopBar("More") },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SettingsSectionHeader("Portfolio Management") }
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column {
                        MoreRow(icon = Icons.Default.People, iconColor = Color(0xFF4A90E2), title = "Family Members", subtitle = "Manage your family portfolio", onClick = onNavigateToFamily)
                        Divider(modifier = Modifier.padding(start = 60.dp), color = MaterialTheme.colorScheme.outline.copy(0.3f))
                        MoreRow(icon = Icons.Default.Shield, iconColor = Color(0xFF9B59B6), title = "Security Master", subtitle = "Add & manage investment instruments", onClick = onNavigateToSecurity)
                        Divider(modifier = Modifier.padding(start = 60.dp), color = MaterialTheme.colorScheme.outline.copy(0.3f))
                        MoreRow(icon = Icons.Default.Update, iconColor = Color(0xFF00C896), title = "Update Prices / NAV", subtitle = "Update current market prices", onClick = onNavigateToPriceUpdate)
                    }
                }
            }

            item { SettingsSectionHeader("App") }
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    MoreRow(icon = Icons.Default.Settings, iconColor = Color(0xFF95A5A6), title = "Settings", subtitle = "Theme, security, preferences", onClick = onNavigateToSettings)
                }
            }
        }
    }
}

@Composable
fun MoreRow(icon: ImageVector, iconColor: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).let {
                it.then(Modifier.clip(RoundedCornerShape(12.dp)))
            },
            contentAlignment = Alignment.Center
        ) {
            Surface(color = iconColor.copy(0.15f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}
