package io.github.fedeonline.fridged.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fedeonline.fridged.data.FridgeAccessDeniedException
import io.github.fedeonline.fridged.data.FridgeRepository
import io.github.fedeonline.fridged.model.Device
import io.github.fedeonline.fridged.ui.ticker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

class StatusViewModel(
    repository: FridgeRepository,
    device: Device,
    now: Flow<Instant> = ticker(),
    startedAt: Instant = Instant.now(),
) : ViewModel() {

    val uiState: StateFlow<StatusUiState> =
        combine(
            repository.status(device),
            // One listener for the screen's lifetime: older samples are filtered out as time passes.
            repository.samples(device, since = startedAt.minus(RECENT_SPAN)),
            now,
            StatusUiState::from,
        )
            .catch { e -> emit(if (e is FridgeAccessDeniedException) StatusUiState.NotAuthorized else StatusUiState.Unavailable) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatusUiState.Loading)
}
