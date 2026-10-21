package io.github.fedeonline.fridged.ui.history

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.fedeonline.fridged.R
import io.github.fedeonline.fridged.data.fake.FakeData
import io.github.fedeonline.fridged.model.HistoryStats
import io.github.fedeonline.fridged.ui.format.formatCelsius
import io.github.fedeonline.fridged.ui.theme.FridgedTheme
import io.github.fedeonline.fridged.ui.theme.Palette
import java.time.Instant
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(state, onSelect = viewModel::select, modifier = modifier)
}

@Composable
private fun HistoryContent(state: HistoryUiState, onSelect: (HistoryRange) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RangeSelector(state.range, onSelect)
        when (state) {
            is HistoryUiState.Loading -> Box(Modifier.fillMaxWidth().height(280.dp), Alignment.Center) {
                CircularProgressIndicator()
            }
            is HistoryUiState.NotAuthorized -> Message(R.string.status_not_authorized)
            is HistoryUiState.Unavailable -> Message(R.string.status_unavailable)
            is HistoryUiState.Ready -> Ready(state)
        }
    }
}

@Composable
private fun Ready(state: HistoryUiState.Ready) {
    if (state.stats.min == null) {
        Message(R.string.history_empty)
        return
    }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Palette.Surface).padding(8.dp)) {
        HistoryChart(
            samples = state.samples,
            end = state.end,
            range = state.range,
            modifier = Modifier.fillMaxWidth().height(260.dp),
        )
    }
    Legend()
    Stats(state.stats)
}

/** Pill-shaped single choice: the selected range in mint. */
@Composable
private fun RangeSelector(selected: HistoryRange, onSelect: (HistoryRange) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Palette.Surface)
            .selectableGroup()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HistoryRange.entries.forEach { range ->
            val isSelected = range == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isSelected) Palette.Mint else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(range) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(range.label()),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Palette.OnMint else Palette.Muted,
                )
            }
        }
    }
}

@Composable
private fun Legend() {
    val colors = historyChartColors()
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem(colors.band, R.string.history_legend_band)
        LegendItem(colors.compressor, R.string.history_legend_compressor)
        LegendItem(colors.safe, R.string.history_legend_safe)
    }
}

@Composable
private fun LegendItem(color: Color, @StringRes label: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(stringResource(label), style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
    }
}

@Composable
private fun Stats(stats: HistoryStats) {
    val locale = LocalConfiguration.current.locales[0]
    fun celsius(v: Double?) = v?.let { formatCelsius(it, locale) } ?: "—"
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(R.string.history_min, celsius(stats.min), Palette.Blue, Modifier.weight(1f))
            StatTile(R.string.history_max, celsius(stats.max), Palette.Peach, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(R.string.history_mean, celsius(stats.mean), Palette.Lilac, Modifier.weight(1f))
            StatTile(
                R.string.history_duty,
                stats.dutyCycle?.let { "${(it * 100).roundToInt()} %" } ?: "—",
                Palette.Mint,
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatTile(@StringRes label: Int, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(Palette.Surface).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = Palette.Muted)
        Text(value, style = MaterialTheme.typography.titleLarge, color = color)
    }
}

@Composable
private fun Message(@StringRes text: Int) {
    Text(
        stringResource(text),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    )
}

@StringRes
private fun HistoryRange.label(): Int = when (this) {
    HistoryRange.SIX_HOURS -> R.string.history_range_6h
    HistoryRange.DAY -> R.string.history_range_24h
    HistoryRange.WEEK -> R.string.history_range_7d
}

// Previews on fake data.

private val previewEnd = Instant.parse("2026-10-20T08:00:00Z")

@Composable
private fun HistoryPreview(range: HistoryRange) {
    val state = HistoryUiState.ready(range, FakeData.history(previewEnd), previewEnd)
    FridgedTheme { Surface { HistoryContent(state, onSelect = {}) } }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun HistoryDayPreview() = HistoryPreview(HistoryRange.DAY)

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun HistoryWeekPreview() = HistoryPreview(HistoryRange.WEEK)
