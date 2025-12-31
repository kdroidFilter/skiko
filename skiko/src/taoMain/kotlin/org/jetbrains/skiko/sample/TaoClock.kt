package org.jetbrains.skiko.sample

import io.github.kdroidfilter.taokt.tao.ControlFlow
import io.github.kdroidfilter.taokt.tao.TaoEvent
import io.github.kdroidfilter.taokt.tao.TaoEventHandler
import io.github.kdroidfilter.taokt.tao.TaoStartCause
import io.github.kdroidfilter.taokt.tao.TaoWindowEvent
import io.github.kdroidfilter.taokt.tao.Window
import io.github.kdroidfilter.taokt.tao.WindowBuilder
import io.github.kdroidfilter.taokt.tao.run
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.Paint
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoRenderDelegate
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.time.Duration.Companion.nanoseconds
import java.util.Calendar

/**
 * Minimal Tao + Skiko clock sample.
 *
 * Run with:
 *   ./gradlew :compileKotlinTao -Pskiko.tao.enabled=true
 *   ./gradlew :runTaoClock -Pskiko.tao.enabled=true
 */
fun main() {
    val layer = SkiaLayer(properties = org.jetbrains.skiko.SkiaLayerProperties(renderApi = GraphicsApi.METAL))
    layer.renderDelegate = ClockRenderer()

    run(object : TaoEventHandler {
        private var window: Window? = null

        override fun handleEvent(event: TaoEvent, app: io.github.kdroidfilter.taokt.tao.App): ControlFlow {
            when (event) {
                is TaoEvent.NewEvents -> {
                    if (event.cause == TaoStartCause.Init && window == null) {
                        val builder = WindowBuilder().apply {
                            setTitle("Skiko Tao Clock")
                            setInnerSize(io.github.kdroidfilter.taokt.tao.LogicalSize(360.0, 360.0))
                        }
                        window = app.createWindow(builder)
                        layer.attachTo(window!!)
                        window?.requestRedraw()
                    }
                }

                is TaoEvent.WindowEvent -> when (event.event) {
                    TaoWindowEvent.CloseRequested -> return ControlFlow.Exit
                    TaoWindowEvent.Destroyed -> {
                        layer.detach()
                        return ControlFlow.Exit
                    }
                    else -> {}
                }

                is TaoEvent.RedrawRequested -> {
                    layer.needRender()
                }

                TaoEvent.MainEventsCleared -> {
                    // Continuous animation: ask for another redraw
                    window?.requestRedraw()
                }

                else -> {}
            }
            return ControlFlow.Wait
        }
    })
}

private class ClockRenderer : SkikoRenderDelegate {
    private val facePaint = Paint().apply {
        isAntiAlias = true
        color = Color.makeRGB(32, 36, 48)
    }
    private val tickPaint = Paint().apply {
        isAntiAlias = true
        color = Color.makeRGB(200, 210, 230)
        strokeWidth = 2f
    }
    private val handPaint = Paint().apply {
        isAntiAlias = true
        strokeCap = org.jetbrains.skia.PaintStrokeCap.ROUND
    }

    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        canvas.clear(Color.makeRGB(15, 18, 26))

        val nowMillis = System.currentTimeMillis()
        val nowNano = nanoTime.nanoseconds.inWholeMilliseconds
        val blendedTime = (nowMillis + nowNano) / 2
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = blendedTime }

        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) * 0.45f

        // Face
        canvas.drawCircle(cx, cy, radius, facePaint)

        // Ticks
        repeat(60) { i ->
            val angle = Math.toRadians((i * 6 - 90).toDouble())
            val outerR = radius * 0.95f
            val innerR = if (i % 5 == 0) radius * 0.82f else radius * 0.88f
            val ox = cx + outerR * cos(angle).toFloat()
            val oy = cy + outerR * sin(angle).toFloat()
            val ix = cx + innerR * cos(angle).toFloat()
            val iy = cy + innerR * sin(angle).toFloat()
            canvas.drawLine(ix, iy, ox, oy, tickPaint)
        }

        val hours = calendar.get(java.util.Calendar.HOUR) + calendar.get(java.util.Calendar.MINUTE) / 60f
        val minutes = calendar.get(java.util.Calendar.MINUTE) + calendar.get(java.util.Calendar.SECOND) / 60f
        val seconds = calendar.get(java.util.Calendar.SECOND) + calendar.get(java.util.Calendar.MILLISECOND) / 1000f

        drawHand(canvas, cx, cy, hours / 12f, radius * 0.5f, Color.makeRGB(250, 204, 21), 6f)
        drawHand(canvas, cx, cy, minutes / 60f, radius * 0.7f, Color.makeRGB(90, 180, 255), 4f)
        drawHand(canvas, cx, cy, seconds / 60f, radius * 0.85f, Color.makeRGB(255, 95, 95), 2f)
    }

    private fun drawHand(canvas: Canvas, cx: Float, cy: Float, value: Float, length: Float, color: Int, stroke: Float) {
        val angle = Math.toRadians((value * 360f - 90f).toDouble())
        val x = cx + length * cos(angle).toFloat()
        val y = cy + length * sin(angle).toFloat()
        handPaint.color = color
        handPaint.strokeWidth = stroke
        canvas.drawLine(cx, cy, x, y, handPaint)
    }
}
