package org.jetbrains.skiko

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * Minimal UI dispatcher for Tao-driven rendering.
 * Uses a single-thread executor since Tao's event loop is managed separately.
 */
val MainUIDispatcher: CoroutineDispatcher by lazy {
    Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "Skiko-Tao-UI").apply { isDaemon = true }
    }.asCoroutineDispatcher()
}
