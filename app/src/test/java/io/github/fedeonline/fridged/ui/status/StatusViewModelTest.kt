package io.github.fedeonline.fridged.ui.status

import io.github.fedeonline.fridged.data.FridgeAccessDeniedException
import io.github.fedeonline.fridged.data.FridgeRepository
import io.github.fedeonline.fridged.data.fake.FakeData
import io.github.fedeonline.fridged.data.fake.FakeFridgeRepository
import io.github.fedeonline.fridged.model.Band
import io.github.fedeonline.fridged.model.Device
import io.github.fedeonline.fridged.model.Sample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class StatusViewModelTest {
    private val start = Instant.parse("2026-10-20T08:00:00Z")
    private val clock = MutableStateFlow(start)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repository: FridgeRepository) =
        StatusViewModel(repository, Device.SIMULATED, now = clock, startedAt = start)

    @Test
    fun `updates in real time and turns stale when the daemon stops`() = runTest {
        val repository = FakeFridgeRepository(FakeData.history(start, span = Duration.ofHours(1)))
        val vm = viewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertEquals(emptyList<Banner>(), (vm.uiState.value as StatusUiState.Ready).banners)

        // A new round arrives: shown immediately, still fresh.
        val next = start.plus(FakeData.ROUND)
        repository.simulateDaemonRound(next)
        clock.value = next
        assertEquals(next, (vm.uiState.value as StatusUiState.Ready).sample.time)

        // No more rounds: after 15 minutes the stale banner appears.
        clock.value = next.plus(Duration.ofMinutes(16))
        assertEquals(listOf(Banner.STALE), (vm.uiState.value as StatusUiState.Ready).banners)
    }

    @Test
    fun `access denied is reported as not authorized`() = runTest {
        val vm = viewModel(FailingRepository(FridgeAccessDeniedException()))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertEquals(StatusUiState.NotAuthorized, vm.uiState.value)
    }

    @Test
    fun `other errors are reported as unavailable`() = runTest {
        val vm = viewModel(FailingRepository(IllegalStateException("boom")))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertEquals(StatusUiState.Unavailable, vm.uiState.value)
    }

    @Test
    fun `missing status document`() = runTest {
        val vm = viewModel(FakeFridgeRepository(samples = emptyList()))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertEquals(StatusUiState.NoData, vm.uiState.value)
    }

    private class FailingRepository(private val error: Throwable) : FridgeRepository {
        override fun status(device: Device): Flow<Sample?> = flow { throw error }
        override suspend fun latestStatus(device: Device): Sample? = throw error
        override fun samples(device: Device, since: Instant): Flow<List<Sample>> = flowOf(emptyList())
        override fun settings(device: Device): Flow<Band?> = flowOf(null)
        override suspend fun setBand(device: Device, band: Band) = Unit
        override suspend fun clearBand(device: Device) = Unit
    }
}
