package io.github.fedeonline.fridged.ui.history

import io.github.fedeonline.fridged.data.fake.FakeData
import io.github.fedeonline.fridged.data.fake.FakeFridgeRepository
import io.github.fedeonline.fridged.model.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val now = Instant.parse("2026-10-20T08:00:00Z")

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `defaults to 24 hours and switches range`() = runTest {
        val vm = HistoryViewModel(FakeFridgeRepository(FakeData.history(now)), Device.SIMULATED, MutableStateFlow(now)) { now }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        val day = vm.uiState.value as HistoryUiState.Ready
        assertEquals(HistoryRange.DAY, day.range)
        assertEquals(24 * 12 + 1, day.samples.size)

        vm.select(HistoryRange.WEEK)
        val week = vm.uiState.value as HistoryUiState.Ready
        assertEquals(HistoryRange.WEEK, week.range)
        assertEquals(7 * 288 + 1, week.samples.size)
    }
}
