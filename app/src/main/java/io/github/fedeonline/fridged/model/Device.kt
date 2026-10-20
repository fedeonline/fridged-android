package io.github.fedeonline.fridged.model

/** A fridge published by `fridged` under `devices/{id}/` in Firestore. */
enum class Device(val id: String) {
    /** The real fridge. */
    FRIDGE("fridge"),

    /** The simulated device written by `fridged --simulate`, used by debug builds. */
    SIMULATED("fridge-sim"),
    ;

    companion object {
        fun fromId(id: String): Device =
            entries.firstOrNull { it.id == id } ?: throw IllegalArgumentException("Unknown device id: $id")
    }
}
