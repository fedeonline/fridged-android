package io.github.fedeonline.fridged.ui.band

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.fedeonline.fridged.R
import io.github.fedeonline.fridged.model.Band
import io.github.fedeonline.fridged.ui.format.formatBand
import io.github.fedeonline.fridged.ui.format.formatCelsius
import io.github.fedeonline.fridged.ui.theme.FridgedTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun BandScreen(viewModel: BandViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BandContent(
        state = state,
        actions = BandActions(
            lowerMin = viewModel::lowerMin,
            raiseMin = viewModel::raiseMin,
            lowerMax = viewModel::lowerMax,
            raiseMax = viewModel::raiseMax,
            save = viewModel::save,
            restore = viewModel::restoreDefaults,
        ),
        modifier = modifier,
    )
}

private class BandActions(
    val lowerMin: () -> Unit,
    val raiseMin: () -> Unit,
    val lowerMax: () -> Unit,
    val raiseMax: () -> Unit,
    val save: () -> Unit,
    val restore: () -> Unit,
)

@Composable
private fun BandContent(state: BandUiState, actions: BandActions, modifier: Modifier = Modifier) {
    when (state) {
        BandUiState.Loading -> Box(modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        BandUiState.NotAuthorized -> Message(R.string.status_not_authorized, modifier)
        BandUiState.Unavailable -> Message(R.string.status_unavailable, modifier)
        is BandUiState.Ready -> Ready(state, actions, modifier)
    }
}

@Composable
private fun Ready(state: BandUiState.Ready, actions: BandActions, modifier: Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.band_applied),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    state.applied?.let { formatBand(it, locale) } ?: "—",
                    style = MaterialTheme.typography.headlineSmall,
                )
                // While restoring, the applied band is still the old custom one.
                if (state.sync !is Sync.Restoring) {
                    Text(stringResource(if (state.custom != null) R.string.band_source_app else R.string.band_source_default))
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Stepper(R.string.band_min, state.draft.min, locale, state.canLowerMin, state.canRaiseMin, actions.lowerMin, actions.raiseMin)
                Stepper(R.string.band_max, state.draft.max, locale, state.canLowerMax, state.canRaiseMax, actions.lowerMax, actions.raiseMax)
                state.problems.forEach {
                    Text(stringResource(it.message()), color = MaterialTheme.colorScheme.error)
                }
                Button(onClick = actions.save, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.band_save))
                }
                Text(stringResource(R.string.band_hint), style = MaterialTheme.typography.bodySmall)
            }
        }

        SyncLine(state.sync, locale)

        TextButton(onClick = actions.restore, enabled = state.canRestore, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.band_restore))
        }
    }
}

@Composable
private fun Stepper(
    @StringRes label: Int,
    value: Double,
    locale: Locale,
    canLower: Boolean,
    canRaise: Boolean,
    onLower: () -> Unit,
    onRaise: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = onLower, enabled = canLower) { Text("−", style = MaterialTheme.typography.titleLarge) }
        Text(
            formatCelsius(value, locale),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            // Fixed width keeps the buttons aligned between "8,0 °C" and "11,0 °C".
            modifier = Modifier.width(112.dp),
        )
        FilledTonalIconButton(onClick = onRaise, enabled = canRaise) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}

@Composable
private fun SyncLine(sync: Sync, locale: Locale) {
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).withZone(ZoneId.systemDefault())
    val (text, isError) = when (sync) {
        Sync.Idle -> return
        is Sync.Waiting -> stringResource(R.string.band_waiting, formatBand(sync.target, locale)) to false
        is Sync.Restoring -> stringResource(R.string.band_restoring) to false
        is Sync.Applied -> stringResource(R.string.band_applied_at, time.format(sync.at)) to false
        Sync.Failed -> stringResource(R.string.band_failed) to true
    }
    Text(
        text,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Message(@StringRes text: Int, modifier: Modifier) {
    Box(modifier.fillMaxSize(), Alignment.Center) {
        Text(stringResource(text), textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
    }
}

@StringRes
private fun Band.Problem.message(): Int = when (this) {
    Band.Problem.NOT_FINITE -> R.string.band_problem_not_finite
    Band.Problem.MIN_OUT_OF_RANGE -> R.string.band_problem_min_range
    Band.Problem.MAX_OUT_OF_RANGE -> R.string.band_problem_max_range
    Band.Problem.TOO_NARROW -> R.string.band_problem_narrow
}

// Previews on fake data.

private val noActions = BandActions({}, {}, {}, {}, {}, {})

@Composable
private fun BandPreview(state: BandUiState) {
    FridgedTheme { Surface { BandContent(state, noActions) } }
}

@Preview(showBackground = true)
@Composable
private fun BandWaitingPreview() = BandPreview(
    BandUiState.Ready(
        applied = Band(3.0, 5.0),
        custom = Band(2.5, 4.5),
        draft = Band(2.5, 4.5),
        problems = emptySet(),
        sync = Sync.Waiting(Band(2.5, 4.5)),
    ),
)

@Preview(showBackground = true)
@Composable
private fun BandInvalidPreview() = BandPreview(
    BandUiState.Ready(
        applied = Band(3.0, 5.0),
        custom = null,
        draft = Band(4.0, 4.0),
        problems = Band.problems(4.0, 4.0),
        sync = Sync.Applied(Band(3.0, 5.0), Instant.parse("2026-10-21T09:05:00Z")),
    ),
)
