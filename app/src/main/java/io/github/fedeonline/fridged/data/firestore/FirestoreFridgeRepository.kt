package io.github.fedeonline.fridged.data.firestore

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import io.github.fedeonline.fridged.data.FridgeAccessDeniedException
import io.github.fedeonline.fridged.data.FridgeRepository
import io.github.fedeonline.fridged.data.SampleCoverage
import io.github.fedeonline.fridged.model.Band
import io.github.fedeonline.fridged.model.Device
import io.github.fedeonline.fridged.model.Sample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Instant

/**
 * [FridgeRepository] on Cloud Firestore. Reads are live snapshot listeners, served from
 * the offline cache first (persistence is on by default on Android).
 */
class FirestoreFridgeRepository(
    private val db: FirebaseFirestore,
    private val coverage: SampleCoverage,
) : FridgeRepository {

    private fun device(device: Device) = db.collection("devices").document(device.id)
    private fun statusDoc(device: Device) = device(device).collection("status").document("current")
    private fun settingsDoc(device: Device): DocumentReference = device(device).collection("settings").document("current")

    override fun status(device: Device): Flow<Sample?> =
        statusDoc(device).snapshots()
            .map { snapshot -> snapshot.data?.let(FirestoreMapping::sample) }
            .mapAccessDenied()

    /**
     * Cached samples plus a live listener on the server for the part the cache may miss
     * ([IncrementalFetch]): reopening a range costs only the new samples, not the whole
     * range again.
     */
    override fun samples(device: Device, since: Instant): Flow<List<Sample>> = flow {
        val collection = device(device).collection("samples")
        val covered = coverage.coveredSince(device)
        val cached = collection.inRange(since).get(Source.CACHE).await().toSamples()
        val serverStart = IncrementalFetch.serverStart(since, covered, cached.lastOrNull()?.time)
        val kept = cached.filter { it.time < serverStart }
        Log.d(TAG, "${device.id} since $since: ${cached.size} cached, server from $serverStart")

        var coverageUpdated = false
        emitAll(
            // Metadata changes included: when the server confirms what the cache already
            // had, that confirmation is the only event telling us the range is complete.
            collection.inRange(serverStart).snapshots(MetadataChanges.INCLUDE).map { snapshot ->
                if (!snapshot.metadata.isFromCache && !coverageUpdated) {
                    coverageUpdated = true
                    Log.d(TAG, "${device.id}: ${snapshot.size()} samples from the server")
                    IncrementalFetch.coverageAfter(since, serverStart, covered)?.let {
                        coverage.setCoveredSince(device, it)
                    }
                }
                kept + snapshot.toSamples()
            }.distinctUntilChanged(),
        )
    }.mapAccessDenied()

    private fun CollectionReference.inRange(since: Instant): Query =
        whereGreaterThanOrEqualTo("time", Timestamp(since)).orderBy("time")

    private fun QuerySnapshot.toSamples(): List<Sample> =
        documents.mapNotNull { doc -> doc.data?.let(FirestoreMapping::sample) }

    override fun settings(device: Device): Flow<Band?> =
        settingsDoc(device).snapshots()
            .map { snapshot -> snapshot.data?.let(FirestoreMapping::band) }
            .mapAccessDenied()

    override suspend fun setBand(device: Device, band: Band) {
        require(Band.problems(band.min, band.max).isEmpty()) { "Invalid band: $band" }
        settingsDoc(device).set(FirestoreMapping.settingsData(band)).await()
    }

    override suspend fun clearBand(device: Device) {
        settingsDoc(device).delete().await()
    }

    private fun <T> Flow<T>.mapAccessDenied(): Flow<T> = catch { e ->
        if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            throw FridgeAccessDeniedException(e)
        }
        throw e
    }

    private companion object {
        const val TAG = "FridgeRepository"
    }
}
