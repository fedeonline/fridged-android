package io.github.fedeonline.fridged.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import io.github.fedeonline.fridged.data.FridgeAccessDeniedException
import io.github.fedeonline.fridged.data.FridgeRepository
import io.github.fedeonline.fridged.model.Band
import io.github.fedeonline.fridged.model.Device
import io.github.fedeonline.fridged.model.Sample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Instant

/**
 * [FridgeRepository] on Cloud Firestore. Reads are live snapshot listeners, served from
 * the offline cache first (persistence is on by default on Android).
 */
class FirestoreFridgeRepository(private val db: FirebaseFirestore) : FridgeRepository {

    private fun device(device: Device) = db.collection("devices").document(device.id)
    private fun statusDoc(device: Device) = device(device).collection("status").document("current")
    private fun settingsDoc(device: Device): DocumentReference = device(device).collection("settings").document("current")

    override fun status(device: Device): Flow<Sample?> =
        statusDoc(device).snapshots()
            .map { snapshot -> snapshot.data?.let(FirestoreMapping::sample) }
            .mapAccessDenied()

    override fun samples(device: Device, since: Instant): Flow<List<Sample>> =
        device(device).collection("samples")
            .whereGreaterThanOrEqualTo("time", Timestamp(since))
            .orderBy("time")
            .snapshots()
            .map { snapshot -> snapshot.documents.mapNotNull { doc -> doc.data?.let(FirestoreMapping::sample) } }
            .mapAccessDenied()

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
}
