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
     * a previous recording of the same recorder was skipped. No command buffer changes were made,
     * but later recordings of that recorder are rejected as well until the skipped one is inserted;
     * if it was closed, the recorder can no longer be used with this context.
     */
    OUT_OF_ORDER_RECORDING,
}
