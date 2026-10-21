package io.github.fedeonline.fridged.ui.band

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.fedeonline.fridged.R
import io.github.fedeonline.fridged.model.Band
import io.github.fedeonline.fridged.ui.dial.Dial
import io.github.fedeonline.fridged.ui.dial.DialScale
import io.github.fedeonline.fridged.ui.format.formatBand
import io.github.fedeonline.fridged.ui.theme.FridgedIcons
import io.github.fedeonline.fridged.ui.theme.FridgedTheme
import io.github.fedeonline.fridged.ui.theme.Palette
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
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // The scale follows the applied band, so it stays put while the draft moves.
        Dial(
            scale = DialScale.around(state.applied ?: state.draft, null),
            band = state.draft,
            knobs = true,
            modifier = Modifier.size(240.dp).align(Alignment.CenterHorizontally),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    String.format(locale, "%.1f – %.1f", state.draft.min, state.draft.max),
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(
                    stringResource(if (state.draft == state.applied) R.string.band_dial_applied else R.string.band_dial_new),
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Muted,
                )
            }
        }

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Palette.Surface).padding(horizontal = 16.dp, vertical = 4.dp)) {
            Stepper(R.string.band_min, Palette.Blue, state.draft.min, locale, state.canLowerMin, state.canRaiseMin, actions.lowerMin, actions.raiseMin)
            HorizontalDivider(color = Palette.SurfaceHigh)
            Stepper(R.string.band_max, Palette.Peach, state.draft.max, locale, state.canLowerMax, state.canRaiseMax, actions.lowerMax, actions.raiseMax)
        }
        state.problems.forEach {
            Text(stringResource(it.message()), color = Palette.Red, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = actions.save,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(stringResource(R.string.band_save), style = MaterialTheme.typography.titleMedium)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            SyncLine(state.sync, locale)
            state.applied?.let { applied ->
                StatusLine(
                    FridgedIcons.Check,
                    Palette.Mint,
                    stringResource(R.string.band_applied_value, formatBand(applied, locale)),
                )
            }
            // While restoring, the applied band is still the old custom one.
            if (state.sync !is Sync.Restoring) {
                Text(
                    stringResource(if (state.custom != null) R.string.band_source_app else R.string.band_source_default),
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Muted,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                stringResource(R.string.band_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Faint,
                textAlign = TextAlign.Center,
            )
        }

        TextButton(
            onClick = actions.restore,
            enabled = state.canRestore,
            colors = ButtonDefaults.textButtonColors(contentColor = Palette.Lilac),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.band_restore), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun Stepper(
    @StringRes label: Int,
    dot: Color,
    value: Double,
    locale: Locale,
    canLower: Boolean,
    canRaise: Boolean,
    onLower: () -> Unit,
    onRaise: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        RoundButton("−", stringResource(R.string.band_lower, stringResource(label)), canLower, onLower)
        Text(
            String.format(locale, "%.1f°", value),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            // Fixed width keeps the buttons aligned between "8,0°" and "11,0°".
            modifier = Modifier.width(72.dp),
        )
        RoundButton("+", stringResource(R.string.band_raise, stringResource(label)), canRaise, onRaise)
    }
}

@Composable
private fun RoundButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = Palette.SurfaceHigh,
            contentColor = Palette.OnSurface,
            disabledContainerColor = Palette.Track,
            disabledContentColor = Palette.Faint,
        ),
        modifier = Modifier.size(44.dp).semantics { contentDescription = description },
    ) {
        Text(symbol, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun StatusLine(icon: ImageVector, color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun SyncLine(sync: Sync, locale: Locale) {
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).withZone(ZoneId.systemDefault())
    when (sync) {
        Sync.Idle -> Unit
        is Sync.Waiting -> StatusLine(FridgedIcons.Sliders, Palette.Peach, stringResource(R.string.band_waiting, formatBand(sync.target, locale)))
        is Sync.Restoring -> StatusLine(FridgedIcons.Sliders, Palette.Peach, stringResource(R.string.band_restoring))
        is Sync.Applied -> StatusLine(FridgedIcons.Check, Palette.Mint, stringResource(R.string.band_applied_at, time.format(sync.at)))
        Sync.Failed -> StatusLine(Icons.Filled.Warning, Palette.Red, stringResource(R.string.band_failed))
    }
}

@Composable
private fun Message(@StringRes text: Int, modifier: Modifier) {
    Box(modifier.fillMaxSize(), Alignment.Center) {
        Text(stringResource(text), textAlign = TextAlign.Center, color = Palette.Muted, modifier = Modifier.padding(24.dp))
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
