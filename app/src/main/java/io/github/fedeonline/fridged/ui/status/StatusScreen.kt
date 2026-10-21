package io.github.fedeonline.fridged.ui.status

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import io.github.fedeonline.fridged.ui.dial.Dial
import io.github.fedeonline.fridged.ui.dial.DialScale
import io.github.fedeonline.fridged.ui.format.formatBand
import io.github.fedeonline.fridged.ui.theme.FridgedIcons
import io.github.fedeonline.fridged.ui.theme.FridgedTheme
import io.github.fedeonline.fridged.ui.theme.Palette
import java.time.Duration
import java.time.Instant
import java.util.Locale

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
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.banners.forEach { BannerCard(it) }

        Dial(
            scale = DialScale.around(sample.band, sample.temperature),
            band = sample.band,
            needle = sample.temperature,
            modifier = Modifier.size(280.dp).align(Alignment.CenterHorizontally),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        sample.temperature?.let { String.format(locale, "%.1f", it) } ?: "—",
                        style = MaterialTheme.typography.displayLarge,
                    )
                    Text(
                        "°C",
                        style = MaterialTheme.typography.titleLarge,
                        color = Palette.Muted,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                    )
                }
                if (sample.temperature == null) {
                    Text(stringResource(R.string.status_no_reading), color = Palette.Red, style = MaterialTheme.typography.bodySmall)
                }
                Text(updatedAgo(state.age), style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
            }
        }

        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val on = sample.relay == RelayState.ON
            Tile(
                icon = FridgedIcons.Snowflake,
                accent = if (on) Palette.Blue else Palette.SurfaceHigh,
                onAccent = if (on) Palette.OnBlue else Palette.Muted,
                label = stringResource(R.string.status_compressor),
                value = stringResource(if (on) R.string.relay_on else R.string.relay_off).capitalized(locale),
                valueColor = if (on) Palette.Blue else Palette.OnSurface,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                Text(stringResource(sample.reason.label()), style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
                if (sample.heldByProtections) {
                    Text(
                        stringResource(
                            R.string.status_held,
                            stringResource(if (sample.wanted == RelayState.ON) R.string.relay_on else R.string.relay_off),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.Peach,
                    )
                }
            }
            Tile(
                icon = FridgedIcons.Sliders,
                accent = Palette.Mint,
                onAccent = Palette.OnMint,
                label = stringResource(R.string.status_band),
                value = sample.band?.let { formatBand(it, locale) } ?: stringResource(R.string.status_band_unknown),
                valueColor = Palette.Mint,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Palette.Surface)
                .clickable(onClick = onOpenHistory)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.status_recent), style = MaterialTheme.typography.labelMedium, color = Palette.Muted)
                Text(stringResource(R.string.status_open_history), style = MaterialTheme.typography.labelMedium, color = Palette.Lilac)
            }
            if (hasEnoughReadings(state.recent)) {
                TemperatureSparkline(
                    samples = state.recent,
                    now = state.now,
                    span = RECENT_SPAN,
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                )
            } else {
                Text(stringResource(R.string.status_recent_empty), style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
            }
        }
    }
}

@Composable
private fun Tile(
    icon: ImageVector,
    accent: Color,
    onAccent: Color,
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = onAccent, modifier = Modifier.size(20.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = Palette.Muted)
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor)
        extra()
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

/** Pastel containers with dark text: red for a fault, lilac for SAFE mode, peach for stale data. */
@Composable
private fun BannerCard(banner: Banner) {
    val (container, content, text) = when (banner) {
        Banner.OVERRUN_FAULT -> Triple(Palette.Red, Palette.OnRed, R.string.banner_overrun)
        Banner.SAFE_MODE -> Triple(Palette.Lilac, Palette.OnLilac, R.string.banner_safe)
        Banner.STALE -> Triple(Palette.Peach, Palette.OnPeach, R.string.banner_stale)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = content)
        Text(stringResource(text), color = content, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Message(@StringRes text: Int, modifier: Modifier) = Centered(modifier) {
    Text(stringResource(text), textAlign = TextAlign.Center, color = Palette.Muted, modifier = Modifier.padding(24.dp))
}

@Composable
private fun Centered(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

private fun String.capitalized(locale: Locale) = replaceFirstChar { it.titlecase(locale) }

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

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun StatusNormalPreview() = StatusPreview(previewState(previewHistory.last()))

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun StatusHeldPreview() = StatusPreview(
    previewState(previewHistory.last().copy(relay = RelayState.OFF, wanted = RelayState.ON, reason = Reason.ABOVE_MAX)),
)

@Preview(showBackground = true, heightDp = 1000)
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
