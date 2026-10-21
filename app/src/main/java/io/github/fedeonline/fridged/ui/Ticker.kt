package io.github.fedeonline.fridged.ui

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.Instant

/** The current time, now and then every [period]: keeps "N min ago" and staleness current. */
fun ticker(period: Duration = Duration.ofSeconds(30)): Flow<Instant> = flow {
    while (true) {
        emit(Instant.now())
        delay(period.toMillis())
    }
}
