package io.github.fedeonline.fridged.ui.status

import io.github.fedeonline.fridged.model.Mode
import io.github.fedeonline.fridged.model.Sample
import io.github.fedeonline.fridged.model.age
import io.github.fedeonline.fridged.model.isStale
import java.time.Duration
import java.time.Instant

/** Span of the mini chart on the Status screen. */
val RECENT_SPAN: Duration = Duration.ofHours(6)

enum class Banner {
    OVERRUN_FAULT,
    SAFE_MODE,
    STALE,
}

sealed interface StatusUiState {
    data object Loading : StatusUiState

    /** The daemon has never published a status for this device. */
    data object NoData : StatusUiState

    data object NotAuthorized : StatusUiState

    data object Unavailable : StatusUiState

    data class Ready(
        val now: Instant,
        val sample: Sample,
        val age: Duration,
        /** Most important first. */
        val banners: List<Banner>,
        /** Samples within [RECENT_SPAN] of now, oldest first. */
        val recent: List<Sample>,
    ) : StatusUiState

    companion object {
        fun from(status: Sample?, samples: List<Sample>, now: Instant): StatusUiState {
            if (status == null) return NoData
            val banners = buildList {
                if (status.overrunFault) add(Banner.OVERRUN_FAULT)
                if (status.mode == Mode.SAFE) add(Banner.SAFE_MODE)
                if (status.isStale(now)) add(Banner.STALE)
            }
            val since = now.minus(RECENT_SPAN)
            return Ready(
                now = now,
                sample = status,
                age = status.age(now),
                banners = banners,
                recent = samples.filter { it.time >= since },
            )
        }
    }
}
