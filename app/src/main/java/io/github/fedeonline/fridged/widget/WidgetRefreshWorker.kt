package io.github.fedeonline.fridged.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.fedeonline.fridged.FridgedApplication
import io.github.fedeonline.fridged.data.FridgeAccessDeniedException
import kotlinx.coroutines.CancellationException
import java.time.Duration
import java.time.Instant

/** Reads `status/current` once and stores it in every widget's state: one document read per run. */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ids = GlanceAppWidgetManager(applicationContext).getGlanceIds(FridgeWidget::class.java)
        if (ids.isEmpty()) return Result.success()

        val container = (applicationContext as FridgedApplication).container
        val now = Instant.now()
        val data: WidgetData? = if (container.authRepository.currentUser.value == null) {
            WidgetData.SignedOut
        } else {
            try {
                WidgetData.of(container.fridgeRepository.latestStatus(container.deviceSelection.device.value), now)
            } catch (e: CancellationException) {
                throw e
            } catch (_: FridgeAccessDeniedException) {
                WidgetData.SignedOut
            } catch (_: Exception) {
                null
            }
        }

        ids.forEach { id ->
            updateAppWidgetState(applicationContext, id) { prefs ->
                if (data != null) {
                    WidgetData.write(data, prefs)
                } else {
                    // Keep the last status, but judge its age now: if the refreshes keep
                    // failing, the widget must turn stale instead of looking fresh forever.
                    (WidgetData.read(prefs) as? WidgetData.Status)?.let { WidgetData.write(it.copy(checkedAt = now), prefs) }
                }
            }
        }
        FridgeWidget().updateAll(applicationContext)
        return if (data == null) Result.retry() else Result.success()
    }

    companion object {
        private const val PERIODIC = "fridge-widget-refresh"
        private const val NOW = "fridge-widget-refresh-now"
        private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Every 15 minutes, the shortest period Android allows, while a widget exists. */
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<WidgetRefreshWorker>(Duration.ofMinutes(15)).setConstraints(network).build(),
            )
        }

        fun refreshNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                NOW,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build(),
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
        }
    }
}
