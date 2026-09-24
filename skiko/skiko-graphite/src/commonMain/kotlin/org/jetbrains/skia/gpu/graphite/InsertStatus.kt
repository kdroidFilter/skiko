package org.jetbrains.skia.gpu.graphite

import org.jetbrains.skiko.ExperimentalSkikoApi

/**
 * Result of [GraphiteContext.insertRecording].
 */
@ExperimentalSkikoApi
enum class InsertStatus {
    /** Everything was successfully added to the underlying command buffer. */
    SUCCESS,

    /** The recording or the insertion info is invalid; no command buffer changes were made. */
    INVALID_RECORDING,

    /** Promise image instantiation failed; no command buffer changes were made. */
    PROMISE_IMAGE_INSTANTIATION_FAILED,

    /** Internal failure; the command buffer was partially modified and the state is unrecoverable. */
    ADD_COMMANDS_FAILED,

    /** Shader pipeline compilation failed; the state is unrecoverable. */
    ASYNC_SHADER_COMPILES_FAILED,

    /**
     * The recording was not inserted in the order it was snapped from its [Recorder], e.g. because
     * a previous recording of the same recorder was never inserted. No command buffer changes were
     * made, but every later recording of that recorder will be rejected as well.
     */
    OUT_OF_ORDER_RECORDING,
}
