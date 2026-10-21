package io.github.fedeonline.fridged.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fedeonline.fridged.R
import io.github.fedeonline.fridged.data.fake.FakeData
import io.github.fedeonline.fridged.data.fake.FakeFridgeRepository
import io.github.fedeonline.fridged.model.Device
import io.github.fedeonline.fridged.ui.band.BandScreen
import io.github.fedeonline.fridged.ui.band.BandViewModel
import io.github.fedeonline.fridged.ui.history.HistoryScreen
import io.github.fedeonline.fridged.ui.history.HistoryViewModel
import io.github.fedeonline.fridged.ui.status.StatusScreen
import io.github.fedeonline.fridged.ui.status.StatusViewModel
import io.github.fedeonline.fridged.ui.theme.FridgedIcons
import io.github.fedeonline.fridged.ui.theme.FridgedTheme
import io.github.fedeonline.fridged.ui.theme.Palette
import java.time.Instant

enum class Section(@StringRes val label: Int, val icon: ImageVector) {
    STATUS(R.string.nav_status, FridgedIcons.Thermometer),
    HISTORY(R.string.nav_history, FridgedIcons.Chart),
    BAND(R.string.nav_band, FridgedIcons.Sliders),
}

/** Header with the settings menu, the current section, and the pill navigation at the bottom. */
@Composable
fun HomeScreen(
    email: String,
    onSignOut: () -> Unit,
    device: Device,
    /** Null when the device cannot be switched (release builds). */
    onSelectDevice: ((Device) -> Unit)?,
    statusViewModel: @Composable () -> StatusViewModel,
    historyViewModel: @Composable () -> HistoryViewModel,
    bandViewModel: @Composable () -> BandViewModel,
) {
    var section by rememberSaveable { mutableStateOf(Section.STATUS) }
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Header(section, email, onSignOut, device, onSelectDevice)
            Box(Modifier.weight(1f)) {
                when (section) {
                    Section.STATUS -> StatusScreen(
                        viewModel = statusViewModel(),
                        onOpenHistory = { section = Section.HISTORY },
                    )
                    Section.HISTORY -> HistoryScreen(viewModel = historyViewModel())
                    Section.BAND -> BandScreen(viewModel = bandViewModel())
                }
            }
            PillNavigation(section) { section = it }
        }
    }
}

@Composable
private fun Header(
    section: Section,
    email: String,
    onSignOut: () -> Unit,
    device: Device,
    onSelectDevice: ((Device) -> Unit)?,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            // Debug builds can show either fridge: say which one, always.
            val fridge = stringResource(R.string.header_fridge)
            Text(
                if (onSelectDevice != null) "$fridge · ${device.id}" else fridge,
                style = MaterialTheme.typography.labelMedium,
                color = Palette.Muted,
            )
            Text(stringResource(section.label), style = MaterialTheme.typography.titleLarge)
        }
        SettingsMenu(email, onSignOut, device, onSelectDevice)
    }
}

@Composable
private fun PillNavigation(current: Section, onSelect: (Section) -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Palette.Surface),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Section.entries.forEach {
            val selected = it == current
            val color = if (selected) Palette.Mint else Palette.Muted
            Column(
                Modifier
                    .widthIn(min = 72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .semantics {
                        role = Role.Tab
                        this.selected = selected
                    }
                    .clickable { onSelect(it) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(it.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                Text(
                    stringResource(it.label),
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun SettingsMenu(email: String, onSignOut: () -> Unit, device: Device, onSelectDevice: ((Device) -> Unit)?) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            shape = CircleShape,
            colors = IconButtonDefaults.iconButtonColors(containerColor = Palette.Surface, contentColor = Palette.OnSurface),
        ) {
            Icon(FridgedIcons.More, contentDescription = stringResource(R.string.menu_more))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Palette.SurfaceHigh,
            shape = RoundedCornerShape(16.dp),
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_signed_in_as, email)) },
                onClick = {},
                enabled = false,
            )
            if (onSelectDevice != null) {
                HorizontalDivider(color = Palette.Outline)
                Device.entries.forEach {
                    DropdownMenuItem(
                        text = { Text(stringResource(it.label())) },
                        onClick = {
                            expanded = false
                            onSelectDevice(it)
                        },
                        trailingIcon = {
                            if (it == device) Icon(FridgedIcons.Check, contentDescription = null, tint = Palette.Mint)
                        },
                    )
                }
                HorizontalDivider(color = Palette.Outline)
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_sign_out)) },
                onClick = {
                    expanded = false
                    onSignOut()
                },
            )
        }
    }
}

@StringRes
private fun Device.label(): Int = when (this) {
    Device.FRIDGE -> R.string.menu_device_real
    Device.SIMULATED -> R.string.menu_device_simulated
}

@Preview
@Composable
private fun HomePreview() {
    val repository = FakeFridgeRepository(FakeData.history(Instant.parse("2026-10-20T08:00:00Z")))
    FridgedTheme {
        HomeScreen(
            email = "me@example.com",
            onSignOut = {},
            device = Device.SIMULATED,
            onSelectDevice = {},
            statusViewModel = { viewModel { StatusViewModel(repository, Device.SIMULATED) } },
            historyViewModel = { viewModel { HistoryViewModel(repository, Device.SIMULATED) } },
            bandViewModel = { viewModel { BandViewModel(repository, Device.SIMULATED) } },
        )
    }
}
