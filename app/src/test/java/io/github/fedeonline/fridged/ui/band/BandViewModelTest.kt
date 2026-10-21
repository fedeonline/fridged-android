package io.github.fedeonline.fridged.ui.band

import io.github.fedeonline.fridged.data.fake.FakeData
import io.github.fedeonline.fridged.data.fake.FakeFridgeRepository
import io.github.fedeonline.fridged.model.Band
import io.github.fedeonline.fridged.model.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class BandViewModelTest {
    private val now = Instant.parse("2026-10-21T09:00:00Z")
    private val configBand = Band(3.0, 5.0)
    private val repository = FakeFridgeRepository(FakeData.history(now, span = Duration.ofHours(1), band = configBand), configBand = configBand)
    private lateinit var vm: BandViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = BandViewModel(repository, Device.SIMULATED) { now.plus(Duration.ofSeconds(30)) }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val ready get() = vm.uiState.value as BandUiState.Ready

    @Test
    fun `edit, save, then confirmed by the daemon`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertEquals(configBand, ready.draft)
        assertFalse("nothing changed yet", ready.canSave)

        vm.lowerMin()
        vm.lowerMax()
        assertEquals(Band(2.5, 4.5), ready.draft)
        assertTrue(ready.canSave)

        vm.save()
        assertEquals(Band(2.5, 4.5), repository.settings(Device.SIMULATED).first())
        assertEquals(Sync.Waiting(Band(2.5, 4.5)), ready.sync)

        val round = now.plus(Duration.ofMinutes(5))
        repository.simulateDaemonRound(round)
        assertEquals(Sync.Applied(Band(2.5, 4.5), round), ready.sync)
        assertEquals(Band(2.5, 4.5), ready.applied)
    }

    @Test
    fun `too narrow bands cannot be saved`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        repeat(4) { vm.raiseMin() } // 5.0 - 5.0
        assertEquals(setOf(Band.Problem.TOO_NARROW), ready.problems)
        assertFalse(ready.canSave)
    }

    @Test
    fun `restore deletes the settings and waits for the daemon`() = runTest {
        repository.setBand(Device.SIMULATED, Band(2.5, 4.5))
        repository.simulateDaemonRound(now.plus(Duration.ofMinutes(1)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertTrue(ready.canRestore)

        vm.restoreDefaults()
        assertEquals(null, repository.settings(Device.SIMULATED).first())
        assertTrue(ready.sync is Sync.Restoring)
        assertFalse(ready.canRestore)

        val round = now.plus(Duration.ofMinutes(6))
        repository.simulateDaemonRound(round)
        assertEquals(Sync.Applied(configBand, round), ready.sync)
    }

    @Test
    fun `steppers stop at the limits`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        repeat(10) { vm.lowerMin() }
        assertEquals(0.0, ready.draft.min, 0.0)
        assertFalse(ready.canLowerMin)
    }
}
