package io.github.fedeonline.fridged.model

/**
 * The hysteresis band the daemon regulates to, in °C.
 *
 * Validation mirrors the daemon's `check_band` (docs/data-contract.md) and the security
 * rules, with the same double arithmetic, so all three agree on borderline values.
 */
data class Band(val min: Double, val max: Double) {

    enum class Problem {
        NOT_FINITE,
        MIN_OUT_OF_RANGE,
        MAX_OUT_OF_RANGE,
        TOO_NARROW,
    }

    companion object {
        const val LOWEST = 0.0
        const val HIGHEST = 25.0
        const val MIN_WIDTH = 0.5

        /** Every problem with the band [min]..[max]; empty when it is valid. */
        fun problems(min: Double, max: Double): Set<Problem> {
            if (!min.isFinite() || !max.isFinite()) return setOf(Problem.NOT_FINITE)
            return buildSet {
                if (min !in LOWEST..HIGHEST) add(Problem.MIN_OUT_OF_RANGE)
                if (max !in LOWEST..HIGHEST) add(Problem.MAX_OUT_OF_RANGE)
                if (max - min < MIN_WIDTH) add(Problem.TOO_NARROW)
            }
        }

        /** The band, or null when the daemon would reject it. */
        fun validOrNull(min: Double, max: Double): Band? =
            if (problems(min, max).isEmpty()) Band(min, max) else null
    }
}
