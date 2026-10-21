package io.github.fedeonline.fridged.ui.band

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fedeonline.fridged.data.FridgeAccessDeniedException
import io.github.fedeonline.fridged.data.FridgeRepository
import io.github.fedeonline.fridged.model.Band
import io.github.fedeonline.fridged.model.Device
import io.github.fedeonline.fridged.model.Sample
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

sealed interface BandUiState {
    data object Loading : BandUiState

    data object NotAuthorized : BandUiState

    data object Unavailable : BandUiState

    data class Ready(
        /** The band in the latest status: what the daemon applies. */
        val applied: Band?,
        /** The band in `settings/current`; null when the daemon uses its config file. */
        val custom: Band?,
        val draft: Band,
        val problems: Set<Band.Problem>,
        val sync: Sync,
    ) : BandUiState {
        val canSave: Boolean get() = problems.isEmpty() && draft != (custom ?: applied) && sync !is Sync.Restoring
        val canRestore: Boolean get() = custom != null && sync !is Sync.Restoring
        val canLowerMin: Boolean get() = stepDown(draft.min) >= Band.LOWEST
        val canRaiseMin: Boolean get() = stepUp(draft.min) <= Band.HIGHEST
        val canLowerMax: Boolean get() = stepDown(draft.max) >= Band.LOWEST
        val canRaiseMax: Boolean get() = stepUp(draft.max) <= Band.HIGHEST
    }
}

class BandViewModel(
    private val repository: FridgeRepository,
    private val device: Device,
    private val clock: () -> Instant = Instant::now,
) : ViewModel() {
    private val draft = MutableStateFlow<Band?>(null)
    private val sync = MutableStateFlow<Sync>(Sync.Idle)
    private var latestStatus: Sample? = null

    val uiState: StateFlow<BandUiState> =
        combine(repository.status(device), repository.settings(device), draft, sync) { status, settings, edited, s ->
            latestStatus = status
            val next = Sync.next(s, status, settings)
            if (next != s) sync.value = next
            val shown = edited ?: settings ?: status?.band ?: DEFAULT
            BandUiState.Ready(
                applied = status?.band,
                custom = settings,
                draft = shown,
                problems = Band.problems(shown.min, shown.max),
                sync = next,
            ) as BandUiState
        }
            .catch { e -> emit(if (e is FridgeAccessDeniedException) BandUiState.NotAuthorized else BandUiState.Unavailable) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BandUiState.Loading)

    fun lowerMin() = edit { it.copy(min = stepDown(it.min)) }
    fun raiseMin() = edit { it.copy(min = stepUp(it.min)) }
    fun lowerMax() = edit { it.copy(max = stepDown(it.max)) }
    fun raiseMax() = edit { it.copy(max = stepUp(it.max)) }

    /** Applies [change] unless it would take a bound outside 0–25 °C. */
    private fun edit(change: (Band) -> Band) {
        val ready = uiState.value as? BandUiState.Ready ?: return
        val edited = change(ready.draft)
        val range = Band.LOWEST..Band.HIGHEST
        if (edited.min in range && edited.max in range) draft.value = edited
    }

    fun save() {
        val ready = uiState.value as? BandUiState.Ready ?: return
        if (!ready.canSave) return
        val band = ready.draft
        // Waiting starts right away: the write is queued offline and the daemon's
        // confirmation is what matters.
        sync.value = Sync.next(Sync.Waiting(band), latestStatus, band)
        write { repository.setBand(device, band) }
    }

    fun restoreDefaults() {
        val ready = uiState.value as? BandUiState.Ready ?: return
        if (!ready.canRestore) return
        draft.value = null
        sync.value = Sync.Restoring(since = clock(), previous = ready.applied, lastSeen = latestStatus?.time)
        write { repository.clearBand(device) }
    }

    private fun write(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                sync.update { Sync.Failed }
            }
        }
    }

    private companion object {
        /** Shown before any data arrives; never written without an edit. */
        val DEFAULT = Band(3.0, 5.0)
    }
}
