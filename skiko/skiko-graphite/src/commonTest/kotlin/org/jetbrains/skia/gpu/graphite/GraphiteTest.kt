package org.jetbrains.skia.gpu.graphite

import org.jetbrains.skia.impl.use
import org.jetbrains.skiko.ExperimentalSkikoApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalSkikoApi::class)
class GraphiteTest {
    @Test
    fun contextCanRecordAndSubmit() {
        withTestGraphiteContext { context ->
            context.makeRecorder().use { recorder ->
                recorder.snap().use { recording ->
                    assertEquals(InsertStatus.SUCCESS, context.insertRecording(recording))
                    context.submit(syncCpu = true)
                }
            }
        }
    }

    @Test
    fun skippedRecordingIsReportedAsOutOfOrder() {
        withTestGraphiteContext { context ->
            context.makeRecorder().use { recorder ->
                recorder.snap().use { assertEquals(InsertStatus.SUCCESS, context.insertRecording(it)) }
                recorder.snap().close()
                recorder.snap().use { assertEquals(InsertStatus.OUT_OF_ORDER_RECORDING, context.insertRecording(it)) }
            }
        }
    }

    @Test
    fun insertingClosedRecordingThrows() {
        withTestGraphiteContext { context ->
            context.makeRecorder().use { recorder ->
                val recording = recorder.snap()
                recording.close()
                assertFailsWith<IllegalArgumentException> { context.insertRecording(recording) }
            }
        }
    }
}

@OptIn(ExperimentalSkikoApi::class)
internal expect fun withTestGraphiteContext(block: (GraphiteContext) -> Unit)
