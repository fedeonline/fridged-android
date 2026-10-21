package io.github.fedeonline.fridged.ui.status

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.fedeonline.fridged.R
import io.github.fedeonline.fridged.data.fake.FakeData
import io.github.fedeonline.fridged.model.Mode
import io.github.fedeonline.fridged.model.Reason
import io.github.fedeonline.fridged.model.RelayState
import io.github.fedeonline.fridged.model.Sample
import io.github.fedeonline.fridged.ui.chart.TemperatureSparkline
import io.github.fedeonline.fridged.ui.chart.hasEnoughReadings
import io.github.fedeonline.fridged.ui.format.formatBand
import io.github.fedeonline.fridged.ui.format.formatCelsius
import io.github.fedeonline.fridged.ui.theme.FridgedTheme
import java.time.Duration
import java.time.Instant

@Composable
fun StatusScreen(viewModel: StatusViewModel, onOpenHistory: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatusContent(state, onOpenHistory, modifier)
}

@Composable
private fun StatusContent(state: StatusUiState, onOpenHistory: () -> Unit, modifier: Modifier = Modifier) {
    when (state) {
        StatusUiState.Loading -> Centered(modifier) { CircularProgressIndicator() }
        StatusUiState.NoData -> Message(R.string.status_no_data, modifier)
        StatusUiState.NotAuthorized -> Message(R.string.status_not_authorized, modifier)
        StatusUiState.Unavailable -> Message(R.string.status_unavailable, modifier)
        is StatusUiState.Ready -> Ready(state, onOpenHistory, modifier)
    }
}

@Composable
private fun Ready(state: StatusUiState.Ready, onOpenHistory: () -> Unit, modifier: Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val sample = state.sample
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.banners.forEach { BannerCard(it) }

        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = sample.temperature?.let { formatCelsius(it, locale) } ?: "—",
                style = MaterialTheme.typography.displayLarge,
            )
            if (sample.temperature == null) {
                Text(stringResource(R.string.status_no_reading), color = MaterialTheme.colorScheme.error)
            }
            Text(updatedAgo(state.age), style = MaterialTheme.typography.bodyMedium)
        }

        InfoCard(title = stringResource(R.string.status_compressor)) {
            Text(
                stringResource(if (sample.relay == RelayState.ON) R.string.relay_on else R.string.relay_off),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(stringResource(sample.reason.label()))
            if (sample.heldByProtections) {
                Text(
                    stringResource(
                        R.string.status_held,
                        stringResource(if (sample.wanted == RelayState.ON) R.string.relay_on else R.string.relay_off),
                    ),
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        InfoCard(title = stringResource(R.string.status_band)) {
            Text(
                sample.band?.let { formatBand(it, locale) } ?: stringResource(R.string.status_band_unknown),
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        InfoCard(title = stringResource(R.string.status_recent), onClick = onOpenHistory) {
            if (hasEnoughReadings(state.recent)) {
                TemperatureSparkline(
                    samples = state.recent,
                    now = state.now,
                    span = RECENT_SPAN,
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                )
            } else {
                Text(stringResource(R.string.status_recent_empty))
            }
        }
    }
}

@Composable
private fun updatedAgo(age: Duration): String {
    val minutes = age.toMinutes().toInt()
    val hours = age.toHours().toInt()
    return when {
        minutes < 1 -> stringResource(R.string.status_updated_now)
        hours < 1 -> pluralStringResource(R.plurals.status_updated_minutes, minutes, minutes)
        else -> pluralStringResource(R.plurals.status_updated_hours, hours, hours)
    }
}

@Composable
private fun BannerCard(banner: Banner) {
    val colors = when (banner) {
        Banner.SAFE_MODE -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Banner.OVERRUN_FAULT, Banner.STALE -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
    val text = when (banner) {
        Banner.OVERRUN_FAULT -> stringResource(R.string.banner_overrun)
        Banner.SAFE_MODE -> stringResource(R.string.banner_safe)
        Banner.STALE -> stringResource(R.string.banner_stale)
    }
    Card(colors = colors, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.Warning, contentDescription = null)
            Text(text)
        }
    }
}

@Composable
private fun InfoCard(title: String, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val body = @Composable {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
    if (onClick != null) Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) { body() }
    else Card(Modifier.fillMaxWidth()) { body() }
}

@Composable
private fun Message(@StringRes text: Int, modifier: Modifier) = Centered(modifier) {
    Text(stringResource(text), textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
}

@Composable
private fun Centered(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@StringRes
private fun Reason.label(): Int = when (this) {
    Reason.ABOVE_MAX -> R.string.reason_above_max
    Reason.AT_OR_BELOW_MIN -> R.string.reason_at_or_below_min
    Reason.IN_BAND -> R.string.reason_in_band
    Reason.STALE -> R.string.reason_stale
    Reason.SAFE_ON -> R.string.reason_safe_on
    Reason.SAFE_OFF -> R.string.reason_safe_off
    Reason.UNKNOWN -> R.string.reason_unknown
}

// Previews on fake data.

private val previewNow = Instant.parse("2026-10-20T08:00:00Z")
private val previewHistory = FakeData.history(previewNow, span = Duration.ofHours(6))

private fun previewState(status: Sample, now: Instant = previewNow) =
    StatusUiState.from(status, previewHistory, now)

@Composable
private fun StatusPreview(state: StatusUiState) {
    FridgedTheme { Surface { StatusContent(state, onOpenHistory = {}) } }
}

@Preview(showBackground = true)
@Composable
private fun StatusNormalPreview() = StatusPreview(previewState(previewHistory.last()))

@Preview(showBackground = true)
@Composable
private fun StatusHeldPreview() = StatusPreview(
    previewState(previewHistory.last().copy(relay = RelayState.OFF, wanted = RelayState.ON, reason = Reason.ABOVE_MAX)),
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun StatusAllBannersPreview() = StatusPreview(
    previewState(
        previewHistory.last().copy(
            temperature = null,
            mode = Mode.SAFE,
            reason = Reason.SAFE_ON,
            relay = RelayState.ON,
            wanted = RelayState.ON,
            overrunFault = true,
        ),
        now = previewNow.plus(Duration.ofMinutes(42)),
    ),
)

@Preview(showBackground = true)
@Composable
private fun StatusNoDataPreview() = StatusPreview(StatusUiState.NoData)
