package io.github.fedeonline.fridged.data.fake

import io.github.fedeonline.fridged.data.FridgeRepository
import io.github.fedeonline.fridged.model.Band
import io.github.fedeonline.fridged.model.Device
import io.github.fedeonline.fridged.model.Sample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * In-memory [FridgeRepository] for previews and tests. Holds a single device: the
 * [Device] arguments are ignored.
 */
class FakeFridgeRepository(
    samples: List<Sample> = FakeData.history(Instant.now()),
    settings: Band? = null,
    /** The band in the daemon's config file, applied when there is no settings document. */
    private val configBand: Band = FakeData.DEFAULT_BAND,
) : FridgeRepository {
    private val samples = MutableStateFlow(samples.sortedBy { it.time })
    private val settings = MutableStateFlow(settings)

    override fun status(device: Device): Flow<Sample?> = samples.map { it.lastOrNull() }

    override suspend fun latestStatus(device: Device): Sample? = samples.value.lastOrNull()

    override fun samples(device: Device, since: Instant): Flow<List<Sample>> =
        samples.map { all -> all.filter { it.time >= since } }

    override fun settings(device: Device): Flow<Band?> = settings

    override suspend fun setBand(device: Device, band: Band) {
        require(Band.problems(band.min, band.max).isEmpty()) { "Invalid band: $band" }
        settings.value = band
    }

    override suspend fun clearBand(device: Device) {
        settings.value = null
    }

    /**
     * What the daemon does in one upload round at [now]: publishes a new sample with the
     * band it applies (the settings document, else its config file).
     */
    fun simulateDaemonRound(now: Instant) {
        val last = samples.value.lastOrNull() ?: return
        val next = last.copy(
            time = now,
            uptimeMs = last.uptimeMs + (now.toEpochMilli() - last.time.toEpochMilli()),
            band = settings.value ?: configBand,
        )
        samples.value += next
    }
}
