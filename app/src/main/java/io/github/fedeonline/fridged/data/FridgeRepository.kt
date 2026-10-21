package io.github.fedeonline.fridged.data

import io.github.fedeonline.fridged.model.Band
import io.github.fedeonline.fridged.model.Device
import io.github.fedeonline.fridged.model.Sample
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** The fridge documents in Firestore (docs/data-contract.md). */
interface FridgeRepository {
    /** The latest sample (`status/current`); null while there is none. */
    fun status(device: Device): Flow<Sample?>

    /** One read of `status/current`, from the server when reachable, else from the cache. */
    suspend fun latestStatus(device: Device): Sample?

    /** Samples with `time >= since`, oldest first. */
    fun samples(device: Device, since: Instant): Flow<List<Sample>>

    /** The band in `settings/current`; null when the document is missing and the daemon uses its config file. */
    fun settings(device: Device): Flow<Band?>

    /** Writes `settings/current`. [band] must be valid (see [Band.problems]). */
    suspend fun setBand(device: Device, band: Band)

    /** Deletes `settings/current`: the daemon goes back to the band in its config file. */
    suspend fun clearBand(device: Device)
}
