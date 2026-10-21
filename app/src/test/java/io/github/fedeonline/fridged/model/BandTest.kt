package io.github.fedeonline.fridged.model

import io.github.fedeonline.fridged.model.Band.Problem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BandTest {
    private fun problems(min: Double, max: Double) = Band.problems(min, max)

    @Test
    fun `valid bands`() {
        assertEquals(emptySet<Problem>(), problems(3.0, 5.0))
        assertEquals(emptySet<Problem>(), problems(0.0, 25.0))
        assertEquals(emptySet<Problem>(), problems(3.0, 3.5))
        assertEquals(emptySet<Problem>(), problems(24.5, 25.0))
        assertEquals(Band(3.0, 5.0), Band.validOrNull(3.0, 5.0))
    }

    @Test
    fun `out of range`() {
        assertEquals(setOf(Problem.MIN_OUT_OF_RANGE), problems(-0.5, 5.0))
        assertEquals(setOf(Problem.MAX_OUT_OF_RANGE), problems(3.0, 25.5))
        assertEquals(setOf(Problem.MIN_OUT_OF_RANGE, Problem.MAX_OUT_OF_RANGE), problems(-2.0, 26.0))
    }

    @Test
    fun `too narrow or inverted`() {
        assertEquals(setOf(Problem.TOO_NARROW), problems(3.0, 3.4))
        assertEquals(setOf(Problem.TOO_NARROW), problems(5.0, 3.0))
        assertEquals(setOf(Problem.TOO_NARROW, Problem.MAX_OUT_OF_RANGE), problems(24.8, 25.2))
        assertNull(Band.validOrNull(3.0, 3.4))
    }

    @Test
    fun `non finite values hide every other problem, like the daemon`() {
        assertEquals(setOf(Problem.NOT_FINITE), problems(Double.NaN, 5.0))
        assertEquals(setOf(Problem.NOT_FINITE), problems(3.0, Double.POSITIVE_INFINITY))
        assertEquals(setOf(Problem.NOT_FINITE), problems(Double.NEGATIVE_INFINITY, Double.NaN))
    }

    @Test
    fun `same double arithmetic as the daemon and the rules`() {
        // 4.1 - 3.6 is 0.49999999999999956 in IEEE 754: the daemon's check_band and the
        // security rules reject this band, so the app must too.
        assertEquals(setOf(Problem.TOO_NARROW), problems(3.6, 4.1))
        // Bounds on a 0.5 grid are exact in binary and never hit this.
        assertEquals(emptySet<Problem>(), problems(3.5, 4.0))
    }
}
