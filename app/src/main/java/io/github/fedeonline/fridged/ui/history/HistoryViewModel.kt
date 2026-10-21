package io.github.fedeonline.fridged.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fedeonline.fridged.data.FridgeAccessDeniedException
import io.github.fedeonline.fridged.data.FridgeRepository
import io.github.fedeonline.fridged.model.Device
import io.github.fedeonline.fridged.model.HistoryStats
import io.github.fedeonline.fridged.model.Sample
import io.github.fedeonline.fridged.ui.ticker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.Instant

enum class HistoryRange(val span: Duration, val labelEveryMinutes: Long, val labelPattern: String) {
    SIX_HOURS(Duration.ofHours(6), 60, "HH:mm"),
    DAY(Duration.ofHours(24), 4 * 60, "HH:mm"),
    WEEK(Duration.ofDays(7), 24 * 60, "EEE d"),
}

sealed interface HistoryUiState {
    val range: HistoryRange

    data class Loading(override val range: HistoryRange) : HistoryUiState

    data class NotAuthorized(override val range: HistoryRange) : HistoryUiState

    data class Unavailable(override val range: HistoryRange) : HistoryUiState

    data class Ready(
        override val range: HistoryRange,
        val end: Instant,
        /** Samples within the range, oldest first. */
        val samples: List<Sample>,
        val stats: HistoryStats,
    ) : HistoryUiState

    companion object {
        fun ready(range: HistoryRange, samples: List<Sample>, end: Instant): Ready {
            val inRange = samples.filter { it.time >= end.minus(range.span) }
            return Ready(range, end, inRange, HistoryStats.of(inRange))
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    repository: FridgeRepository,
    device: Device,
    now: Flow<Instant> = ticker(Duration.ofMinutes(1)),
    clock: () -> Instant = Instant::now,
) : ViewModel() {
    private val range = MutableStateFlow(HistoryRange.DAY)

    val uiState: StateFlow<HistoryUiState> = range
        .flatMapLatest { r ->
            combine(repository.samples(device, since = clock().minus(r.span)), now) { samples, end ->
                HistoryUiState.ready(r, samples, end) as HistoryUiState
            }
                .onStart { emit(HistoryUiState.Loading(r)) }
                .catch { e ->
                    emit(if (e is FridgeAccessDeniedException) HistoryUiState.NotAuthorized(r) else HistoryUiState.Unavailable(r))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState.Loading(range.value))

    fun select(range: HistoryRange) {
        this.range.value = range
    }
}
