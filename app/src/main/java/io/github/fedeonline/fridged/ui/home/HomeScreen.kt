package io.github.fedeonline.fridged.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.fedeonline.fridged.R
import io.github.fedeonline.fridged.ui.theme.FridgedTheme

enum class Section(@StringRes val label: Int, val icon: ImageVector) {
    STATUS(R.string.nav_status, Icons.Filled.Home),
    HISTORY(R.string.nav_history, Icons.Filled.DateRange),
    BAND(R.string.nav_band, Icons.Filled.Edit),
}

/** Bottom navigation between the three sections, with the settings menu in the top bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(email: String, onSignOut: () -> Unit) {
    var section by rememberSaveable { mutableStateOf(Section.STATUS) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(section.label)) },
                actions = { SettingsMenu(email = email, onSignOut = onSignOut) },
            )
        },
        bottomBar = {
            NavigationBar {
                Section.entries.forEach {
                    NavigationBarItem(
                        selected = it == section,
                        onClick = { section = it },
                        icon = { Icon(it.icon, contentDescription = null) },
                        label = { Text(stringResource(it.label)) },
                    )
                }
            }
        },
    ) { padding ->
        // Section screens arrive with features 4–6.
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.not_implemented))
        }
    }
}

@Composable
private fun SettingsMenu(email: String, onSignOut: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.menu_more))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_signed_in_as, email)) },
            onClick = {},
            enabled = false,
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_sign_out)) },
            onClick = {
                expanded = false
                onSignOut()
            },
        )
    }
}

@Preview
@Composable
private fun HomePreview() {
    FridgedTheme { HomeScreen(email = "me@example.com", onSignOut = {}) }
}
