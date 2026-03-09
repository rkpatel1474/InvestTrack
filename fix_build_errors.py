#!/usr/bin/env python3
"""
InvestTrack build-error patch script
Fixes 5 compilation errors across 2 files:
  - CommonComponents.kt : add @Composable SectionHeader overload + DateField Long? fix
  - FamilyScreens.kt    : HorizontalDivider -> Divider
"""

import re, sys
from pathlib import Path

# ── Locate repo root ───────────────────────────────────────────────────────────
candidates = [Path("."), Path("InvestTrack"), Path("app")]
repo = None
for c in candidates:
    if (c / "app/src/main/java/com/investtrack").exists():
        repo = c; break
if repo is None:
    sys.exit("ERROR: Could not find repo root. Run this script from your InvestTrack project root.")

base = repo / "app/src/main/java/com/investtrack"
print(f"Repo root: {repo.resolve()}\n")

# ── Helper ─────────────────────────────────────────────────────────────────────
def patch(filepath: Path, old: str, new: str, label: str):
    text = filepath.read_text()
    if old not in text:
        print(f"  [SKIP] '{label}' – pattern not found (already patched?)")
        return
    filepath.write_text(text.replace(old, new, 1))
    print(f"  [OK]   {label}")

# ══════════════════════════════════════════════════════════════════════════════
# 1. CommonComponents.kt
# ══════════════════════════════════════════════════════════════════════════════
cc = base / "ui/common/CommonComponents.kt"
print(f"Patching {cc.relative_to(repo)} ...")

# 1a. Add @Composable overload of SectionHeader
OLD_SECTION_HEADER = '''// ─── Section Header ───────────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, action: String = "", onAction: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (action.isNotEmpty()) {
            Text(action, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onAction() })
        }
    }
}'''

NEW_SECTION_HEADER = '''// ─── Section Header ───────────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, action: String = "", onAction: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (action.isNotEmpty()) {
            Text(action, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onAction() })
        }
    }
}

// Overload that accepts a composable trailing action slot (fixes @Composable-in-lambda errors)
@Composable
fun SectionHeader(title: String, trailingContent: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        trailingContent()
    }
}'''

patch(cc, OLD_SECTION_HEADER, NEW_SECTION_HEADER, "SectionHeader – add @Composable overload")

# 1b. DateField: Long -> Long?
patch(cc,
    'fun DateField(label: String, value: Long, onValueChange: (Long) -> Unit, modifier: Modifier = Modifier) {\n    var showPicker by remember { mutableStateOf(false) }\n    val displayText = remember(value) {\n        java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(value))\n    }',
    'fun DateField(label: String, value: Long?, onValueChange: (Long) -> Unit, modifier: Modifier = Modifier) {\n    var showPicker by remember { mutableStateOf(false) }\n    val displayText = remember(value) {\n        if (value != null)\n            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(value))\n        else ""\n    }',
    "DateField – value: Long -> Long?"
)

# ══════════════════════════════════════════════════════════════════════════════
# 2. FamilyScreens.kt
# ══════════════════════════════════════════════════════════════════════════════
fs = base / "ui/family/FamilyScreens.kt"
print(f"\nPatching {fs.relative_to(repo)} ...")

patch(fs,
    "if (idx < nominees.lastIndex) HorizontalDivider()",
    "if (idx < nominees.lastIndex) Divider()",
    "HorizontalDivider -> Divider (not in M3 1.1.x)"
)

print("\nDone! All patches applied. Run: ./gradlew assembleDebug --no-build-cache")
